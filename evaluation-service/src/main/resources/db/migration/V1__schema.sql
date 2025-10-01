-- ============================================================================
-- Evaluation Service Database Schema
-- Sistema de Admisión Monte Tabor y Nazaret
-- ============================================================================

-- Enable necessary extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- ============================================================================
-- EVALUATIONS TABLE
-- ============================================================================
CREATE TABLE evaluations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    application_id UUID NOT NULL,
    evaluator_id TEXT NOT NULL,
    subject TEXT NOT NULL,                    -- MATHEMATICS, LANGUAGE, PSYCHOLOGY, GENERAL, etc.
    level TEXT,                               -- HIGH_SCHOOL, BASIC, PRESCHOOL
    status TEXT NOT NULL,                     -- PENDING, ASSIGNED, IN_PROGRESS, COMPLETED, CANCELLED
    assigned_at TIMESTAMPTZ,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    notes TEXT,
    
    -- Scoring summary
    total_score DECIMAL(5,2),                 -- Puntaje total obtenido
    max_score DECIMAL(5,2),                   -- Puntaje máximo posible
    percentage DECIMAL(5,2),                  -- Porcentaje obtenido
    passed BOOLEAN,                           -- Si aprobó la evaluación
    
    -- Assignment metadata
    assignment_reason TEXT,                   -- AUTO_ASSIGNED, MANUAL_ASSIGNED, REASSIGNED
    previous_evaluator_id TEXT,               -- En caso de reasignación
    priority INTEGER DEFAULT 0,              -- Prioridad de la evaluación (0=normal, 1=alta, etc.)
    
    -- Timing and SLA
    expected_completion_at TIMESTAMPTZ,       -- Fecha esperada de completado
    sla_exceeded BOOLEAN DEFAULT FALSE,       -- Si se excedió el SLA
    processing_time_minutes INTEGER,          -- Tiempo total de procesamiento
    
    -- Audit fields
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by TEXT NOT NULL DEFAULT 'SYSTEM',
    updated_by TEXT NOT NULL DEFAULT 'SYSTEM',
    
    -- Optimistic locking
    version INTEGER NOT NULL DEFAULT 0
);

-- Indexes for evaluations
CREATE INDEX idx_evaluations_application_id ON evaluations(application_id);
CREATE INDEX idx_evaluations_evaluator_id ON evaluations(evaluator_id);
CREATE INDEX idx_evaluations_status ON evaluations(status);
CREATE INDEX idx_evaluations_subject ON evaluations(subject);
CREATE INDEX idx_evaluations_level ON evaluations(level);
CREATE INDEX idx_evaluations_evaluator_status ON evaluations(evaluator_id, status);
CREATE INDEX idx_evaluations_application_status ON evaluations(application_id, status);
CREATE INDEX idx_evaluations_assigned_at ON evaluations(assigned_at);
CREATE INDEX idx_evaluations_completed_at ON evaluations(completed_at);
CREATE INDEX idx_evaluations_expected_completion ON evaluations(expected_completion_at) WHERE status IN ('ASSIGNED', 'IN_PROGRESS');
CREATE INDEX idx_evaluations_sla_exceeded ON evaluations(sla_exceeded) WHERE sla_exceeded = TRUE;

-- Partial index for active evaluations
CREATE INDEX idx_evaluations_active ON evaluations(evaluator_id, assigned_at) 
    WHERE status IN ('ASSIGNED', 'IN_PROGRESS');

-- ============================================================================
-- RUBRICS TABLE
-- ============================================================================
CREATE TABLE rubrics (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name TEXT NOT NULL,
    subject TEXT NOT NULL,
    level TEXT,                               -- HIGH_SCHOOL, BASIC, PRESCHOOL, ALL
    description TEXT,
    
    -- Rubric configuration
    items JSONB NOT NULL,                     -- [{id, label, description, max_score, weight}, ...]
    total_max_score DECIMAL(5,2) NOT NULL,    -- Suma total de puntajes máximos
    passing_score DECIMAL(5,2),               -- Puntaje mínimo para aprobar
    passing_percentage DECIMAL(5,2),          -- Porcentaje mínimo para aprobar
    
    -- Metadata
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    version_number INTEGER NOT NULL DEFAULT 1,
    replaces_rubric_id UUID,                  -- ID de la rúbrica que reemplaza
    
    -- Audit fields
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by TEXT NOT NULL,
    updated_by TEXT NOT NULL
);

-- Indexes for rubrics
CREATE INDEX idx_rubrics_subject ON rubrics(subject);
CREATE INDEX idx_rubrics_level ON rubrics(level);
CREATE INDEX idx_rubrics_active ON rubrics(is_active) WHERE is_active = TRUE;
CREATE INDEX idx_rubrics_subject_level ON rubrics(subject, level) WHERE is_active = TRUE;

-- Unique constraint for active rubrics
CREATE UNIQUE INDEX idx_rubrics_unique_active 
    ON rubrics(subject, level) 
    WHERE is_active = TRUE AND replaces_rubric_id IS NULL;

-- ============================================================================
-- SCORES TABLE
-- ============================================================================
CREATE TABLE scores (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    evaluation_id UUID NOT NULL REFERENCES evaluations(id) ON DELETE CASCADE,
    rubric_id UUID NOT NULL REFERENCES rubrics(id),
    rubric_item_id TEXT NOT NULL,             -- ID del item dentro de la rúbrica
    
    -- Score data
    value DECIMAL(5,2) NOT NULL,              -- Puntaje otorgado
    max_value DECIMAL(5,2) NOT NULL,          -- Puntaje máximo posible para este item
    weight DECIMAL(3,2) DEFAULT 1.0,         -- Peso del item en la evaluación total
    
    -- Additional data
    comment TEXT,                             -- Comentario específico del evaluador
    rubric_item_data JSONB,                   -- Copia de los datos del item de la rúbrica
    
    -- Audit fields
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by TEXT NOT NULL,
    updated_by TEXT NOT NULL,
    
    -- Ensure one score per item per evaluation
    CONSTRAINT scores_unique_item_per_evaluation UNIQUE(evaluation_id, rubric_item_id)
);

-- Indexes for scores
CREATE INDEX idx_scores_evaluation_id ON scores(evaluation_id);
CREATE INDEX idx_scores_rubric_id ON scores(rubric_id);
CREATE INDEX idx_scores_value ON scores(value);

-- ============================================================================
-- INTERVIEWS TABLE
-- ============================================================================
CREATE TABLE interviews (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    application_id UUID NOT NULL,
    interviewer_id TEXT NOT NULL,
    
    -- Scheduling information
    scheduled_at TIMESTAMPTZ NOT NULL,
    duration_minutes INTEGER NOT NULL DEFAULT 45,
    buffer_minutes INTEGER DEFAULT 15,       -- Buffer time before/after
    location TEXT,                           -- Physical or virtual location
    meeting_url TEXT,                        -- Virtual meeting URL
    meeting_id TEXT,                         -- Meeting ID for reference
    
    -- Status and lifecycle
    status TEXT NOT NULL,                    -- SCHEDULED, CONFIRMED, COMPLETED, NO_SHOW, CANCELLED, RESCHEDULED
    confirmed_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    
    -- Interview content
    interview_type TEXT DEFAULT 'GENERAL',  -- GENERAL, ACADEMIC, PSYCHOLOGICAL, FAMILY
    questions JSONB,                         -- Preguntas preparadas
    responses JSONB,                         -- Respuestas/notas del entrevistador
    
    -- Scoring
    overall_score DECIMAL(5,2),              -- Puntaje general de la entrevista
    max_score DECIMAL(5,2) DEFAULT 100,      -- Puntaje máximo
    passed BOOLEAN,                          -- Si aprobó la entrevista
    
    -- Notes and observations
    notes TEXT,                              -- Notas generales
    private_notes TEXT,                      -- Notas privadas del entrevistador
    recommendation TEXT,                     -- Recomendación final
    
    -- Rescheduling history
    original_scheduled_at TIMESTAMPTZ,       -- Fecha original si fue reprogramada
    reschedule_reason TEXT,                  -- Motivo de reprogramación
    reschedule_count INTEGER DEFAULT 0,      -- Número de reprogramaciones
    
    -- Notifications
    reminder_sent_at TIMESTAMPTZ,            -- Cuándo se envió el recordatorio
    confirmation_sent_at TIMESTAMPTZ,        -- Cuándo se envió la confirmación
    
    -- Audit fields
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by TEXT NOT NULL DEFAULT 'SYSTEM',
    updated_by TEXT NOT NULL DEFAULT 'SYSTEM',
    
    -- Optimistic locking
    version INTEGER NOT NULL DEFAULT 0
);

-- Indexes for interviews
CREATE INDEX idx_interviews_application_id ON interviews(application_id);
CREATE INDEX idx_interviews_interviewer_id ON interviews(interviewer_id);
CREATE INDEX idx_interviews_status ON interviews(status);
CREATE INDEX idx_interviews_scheduled_at ON interviews(scheduled_at);
CREATE INDEX idx_interviews_interviewer_date ON interviews(interviewer_id, scheduled_at);
CREATE INDEX idx_interviews_confirmed_at ON interviews(confirmed_at);
CREATE INDEX idx_interviews_completed_at ON interviews(completed_at);

-- Index for preventing double booking
CREATE INDEX idx_interviews_interviewer_schedule ON interviews(interviewer_id, scheduled_at, duration_minutes, buffer_minutes)
    WHERE status IN ('SCHEDULED', 'CONFIRMED');

-- ============================================================================
-- EVALUATOR PROFILES TABLE (Cache/Reference)
-- ============================================================================
CREATE TABLE evaluator_profiles (
    evaluator_id TEXT PRIMARY KEY,
    full_name TEXT NOT NULL,
    email TEXT NOT NULL,
    
    -- Specialization
    subjects TEXT[] NOT NULL,                 -- Array of subjects they can evaluate
    levels TEXT[] NOT NULL,                   -- Array of levels they can handle
    max_concurrent_evaluations INTEGER DEFAULT 10,
    
    -- Availability and load balancing
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    current_load INTEGER DEFAULT 0,          -- Current number of active evaluations
    last_assignment_at TIMESTAMPTZ,          -- For round-robin
    priority_weight DECIMAL(3,2) DEFAULT 1.0, -- Weight for assignment algorithm
    
    -- Interview capabilities
    can_conduct_interviews BOOLEAN DEFAULT FALSE,
    interview_types TEXT[],                   -- Types of interviews they can conduct
    max_interviews_per_day INTEGER DEFAULT 8,
    
    -- Working schedule (for interview scheduling)
    working_hours_start TIME DEFAULT '08:00',
    working_hours_end TIME DEFAULT '18:00',
    working_days TEXT[] DEFAULT ARRAY['MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY'],
    
    -- Statistics
    total_evaluations_completed INTEGER DEFAULT 0,
    total_interviews_completed INTEGER DEFAULT 0,
    average_evaluation_score DECIMAL(5,2),
    average_processing_time_minutes INTEGER,
    
    -- Audit fields
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    synced_at TIMESTAMPTZ,                   -- Last sync with user service
    
    -- Optimistic locking
    version INTEGER NOT NULL DEFAULT 0
);

-- Indexes for evaluator_profiles
CREATE INDEX idx_evaluator_profiles_active ON evaluator_profiles(is_active) WHERE is_active = TRUE;
CREATE INDEX idx_evaluator_profiles_subjects ON evaluator_profiles USING GIN(subjects);
CREATE INDEX idx_evaluator_profiles_levels ON evaluator_profiles USING GIN(levels);
CREATE INDEX idx_evaluator_profiles_load ON evaluator_profiles(current_load, is_active);
CREATE INDEX idx_evaluator_profiles_last_assignment ON evaluator_profiles(last_assignment_at) WHERE is_active = TRUE;
CREATE INDEX idx_evaluator_profiles_interviews ON evaluator_profiles(can_conduct_interviews, is_active);

-- ============================================================================
-- TRIGGERS FOR AUTOMATIC UPDATES
-- ============================================================================

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Apply triggers to all tables with updated_at
CREATE TRIGGER update_evaluations_updated_at BEFORE UPDATE ON evaluations
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_rubrics_updated_at BEFORE UPDATE ON rubrics
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_scores_updated_at BEFORE UPDATE ON scores
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_interviews_updated_at BEFORE UPDATE ON interviews
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_evaluator_profiles_updated_at BEFORE UPDATE ON evaluator_profiles
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- FUNCTIONS FOR EVALUATION METRICS
-- ============================================================================

-- Function to calculate evaluation processing time
CREATE OR REPLACE FUNCTION calculate_evaluation_processing_time()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.status = 'COMPLETED' AND OLD.status != 'COMPLETED' AND NEW.assigned_at IS NOT NULL THEN
        NEW.processing_time_minutes = EXTRACT(EPOCH FROM (NEW.completed_at - NEW.assigned_at)) / 60;
    END IF;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER calculate_evaluation_time BEFORE UPDATE ON evaluations
    FOR EACH ROW EXECUTE FUNCTION calculate_evaluation_processing_time();

-- Function to update evaluator load
CREATE OR REPLACE FUNCTION update_evaluator_load()
RETURNS TRIGGER AS $$
BEGIN
    -- Update current load when evaluation status changes
    IF TG_OP = 'UPDATE' THEN
        -- If evaluation becomes active (ASSIGNED or IN_PROGRESS)
        IF NEW.status IN ('ASSIGNED', 'IN_PROGRESS') AND OLD.status NOT IN ('ASSIGNED', 'IN_PROGRESS') THEN
            UPDATE evaluator_profiles 
            SET current_load = current_load + 1,
                last_assignment_at = CASE WHEN NEW.status = 'ASSIGNED' THEN NEW.assigned_at ELSE last_assignment_at END
            WHERE evaluator_id = NEW.evaluator_id;
        END IF;
        
        -- If evaluation becomes inactive (COMPLETED, CANCELLED)
        IF OLD.status IN ('ASSIGNED', 'IN_PROGRESS') AND NEW.status NOT IN ('ASSIGNED', 'IN_PROGRESS') THEN
            UPDATE evaluator_profiles 
            SET current_load = GREATEST(current_load - 1, 0),
                total_evaluations_completed = CASE WHEN NEW.status = 'COMPLETED' 
                                                 THEN total_evaluations_completed + 1 
                                                 ELSE total_evaluations_completed END
            WHERE evaluator_id = NEW.evaluator_id;
        END IF;
    END IF;
    
    RETURN COALESCE(NEW, OLD);
END;
$$ language 'plpgsql';

CREATE TRIGGER update_evaluator_load_trigger AFTER UPDATE ON evaluations
    FOR EACH ROW EXECUTE FUNCTION update_evaluator_load();

-- ============================================================================
-- CONSTRAINTS AND VALIDATION
-- ============================================================================

-- Check constraints for valid enum values
ALTER TABLE evaluations ADD CONSTRAINT check_evaluation_status 
    CHECK (status IN ('PENDING', 'ASSIGNED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'));

ALTER TABLE evaluations ADD CONSTRAINT check_evaluation_subject 
    CHECK (subject IN ('MATHEMATICS', 'LANGUAGE', 'ENGLISH', 'PSYCHOLOGY', 'GENERAL', 'SCIENCE', 'HISTORY'));

ALTER TABLE evaluations ADD CONSTRAINT check_evaluation_level 
    CHECK (level IS NULL OR level IN ('PRESCHOOL', 'BASIC', 'HIGH_SCHOOL', 'ALL'));

ALTER TABLE interviews ADD CONSTRAINT check_interview_status 
    CHECK (status IN ('SCHEDULED', 'CONFIRMED', 'COMPLETED', 'NO_SHOW', 'CANCELLED', 'RESCHEDULED'));

ALTER TABLE interviews ADD CONSTRAINT check_interview_type 
    CHECK (interview_type IN ('GENERAL', 'ACADEMIC', 'PSYCHOLOGICAL', 'FAMILY', 'DIRECTOR'));

-- Check that completed evaluations have completion date
ALTER TABLE evaluations ADD CONSTRAINT check_completed_evaluation_date
    CHECK ((status = 'COMPLETED' AND completed_at IS NOT NULL) OR status != 'COMPLETED');

-- Check that assigned evaluations have assignment date
ALTER TABLE evaluations ADD CONSTRAINT check_assigned_evaluation_date
    CHECK ((status IN ('ASSIGNED', 'IN_PROGRESS', 'COMPLETED') AND assigned_at IS NOT NULL) OR 
           status NOT IN ('ASSIGNED', 'IN_PROGRESS', 'COMPLETED'));

-- Check that scores are within valid range
ALTER TABLE scores ADD CONSTRAINT check_score_range
    CHECK (value >= 0 AND value <= max_value);

-- Check that percentages are valid
ALTER TABLE evaluations ADD CONSTRAINT check_percentage_range
    CHECK (percentage IS NULL OR (percentage >= 0 AND percentage <= 100));

-- Check that interview duration is reasonable
ALTER TABLE interviews ADD CONSTRAINT check_interview_duration
    CHECK (duration_minutes > 0 AND duration_minutes <= 240);

-- ============================================================================
-- INITIAL DATA
-- ============================================================================

-- Insert default rubrics for different subjects and levels
INSERT INTO rubrics (name, subject, level, description, items, total_max_score, passing_score, passing_percentage, created_by, updated_by) VALUES
-- Mathematics rubrics
('Matemáticas Educación Media', 'MATHEMATICS', 'HIGH_SCHOOL', 'Rúbrica de evaluación de Matemáticas para Educación Media',
 '[
   {"id": "algebra", "label": "Álgebra", "description": "Resolución de ecuaciones y sistemas", "max_score": 25, "weight": 1.0},
   {"id": "geometry", "label": "Geometría", "description": "Cálculo de áreas, perímetros y volúmenes", "max_score": 25, "weight": 1.0},
   {"id": "functions", "label": "Funciones", "description": "Análisis de funciones y gráficos", "max_score": 25, "weight": 1.0},
   {"id": "statistics", "label": "Estadística", "description": "Análisis de datos y probabilidades", "max_score": 25, "weight": 1.0}
 ]'::jsonb, 100, 60, 60, 'SYSTEM', 'SYSTEM'),

('Matemáticas Educación Básica', 'MATHEMATICS', 'BASIC', 'Rúbrica de evaluación de Matemáticas para Educación Básica',
 '[
   {"id": "arithmetic", "label": "Aritmética", "description": "Operaciones básicas y fracciones", "max_score": 30, "weight": 1.0},
   {"id": "geometry_basic", "label": "Geometría Básica", "description": "Figuras geométricas y medidas", "max_score": 30, "weight": 1.0},
   {"id": "problem_solving", "label": "Resolución de Problemas", "description": "Aplicación de matemáticas en contextos", "max_score": 40, "weight": 1.0}
 ]'::jsonb, 100, 50, 50, 'SYSTEM', 'SYSTEM'),

-- Language rubrics
('Lenguaje Educación Media', 'LANGUAGE', 'HIGH_SCHOOL', 'Rúbrica de evaluación de Lenguaje para Educación Media',
 '[
   {"id": "reading_comprehension", "label": "Comprensión Lectora", "description": "Análisis y comprensión de textos", "max_score": 30, "weight": 1.0},
   {"id": "writing", "label": "Producción de Textos", "description": "Redacción y estructura textual", "max_score": 30, "weight": 1.0},
   {"id": "grammar", "label": "Gramática y Ortografía", "description": "Uso correcto del idioma", "max_score": 20, "weight": 1.0},
   {"id": "vocabulary", "label": "Vocabulario", "description": "Riqueza y precisión léxica", "max_score": 20, "weight": 1.0}
 ]'::jsonb, 100, 60, 60, 'SYSTEM', 'SYSTEM'),

-- Psychology rubrics
('Evaluación Psicológica General', 'PSYCHOLOGY', 'ALL', 'Rúbrica de evaluación psicológica para todos los niveles',
 '[
   {"id": "emotional_maturity", "label": "Madurez Emocional", "description": "Capacidad de gestión emocional", "max_score": 25, "weight": 1.0},
   {"id": "social_skills", "label": "Habilidades Sociales", "description": "Interacción con pares y adultos", "max_score": 25, "weight": 1.0},
   {"id": "attention", "label": "Capacidad de Atención", "description": "Concentración y seguimiento de instrucciones", "max_score": 25, "weight": 1.0},
   {"id": "adaptation", "label": "Adaptabilidad", "description": "Capacidad de adaptación a nuevos entornos", "max_score": 25, "weight": 1.0}
 ]'::jsonb, 100, 70, 70, 'SYSTEM', 'SYSTEM');

-- Insert sample evaluator profiles (these would normally be synced from user service)
INSERT INTO evaluator_profiles (evaluator_id, full_name, email, subjects, levels, max_concurrent_evaluations, can_conduct_interviews, interview_types, created_by) VALUES
('prof-math-001', 'María González', 'maria.gonzalez@mtn.cl', 
 ARRAY['MATHEMATICS'], ARRAY['HIGH_SCHOOL', 'BASIC'], 8, FALSE, NULL, 'SYSTEM'),

('prof-lang-001', 'Carlos Silva', 'carlos.silva@mtn.cl', 
 ARRAY['LANGUAGE'], ARRAY['HIGH_SCHOOL', 'BASIC'], 10, FALSE, NULL, 'SYSTEM'),

('prof-psyc-001', 'Ana Morales', 'ana.morales@mtn.cl', 
 ARRAY['PSYCHOLOGY'], ARRAY['ALL'], 6, TRUE, ARRAY['PSYCHOLOGICAL', 'GENERAL'], 'SYSTEM'),

('director-001', 'Roberto Díaz', 'roberto.diaz@mtn.cl', 
 ARRAY['GENERAL'], ARRAY['ALL'], 3, TRUE, ARRAY['DIRECTOR', 'GENERAL'], 'SYSTEM');

-- ============================================================================
-- VIEWS FOR REPORTING AND ANALYTICS
-- ============================================================================

-- View for evaluation metrics
CREATE VIEW evaluation_metrics AS
SELECT 
    e.subject,
    e.level,
    e.status,
    COUNT(*) as total_evaluations,
    AVG(e.processing_time_minutes) as avg_processing_time_minutes,
    AVG(e.percentage) as avg_percentage,
    COUNT(CASE WHEN e.passed = TRUE THEN 1 END) as passed_count,
    COUNT(CASE WHEN e.sla_exceeded = TRUE THEN 1 END) as sla_exceeded_count,
    MIN(e.created_at) as first_evaluation,
    MAX(e.completed_at) as last_completed
FROM evaluations e
GROUP BY e.subject, e.level, e.status;

-- View for evaluator workload
CREATE VIEW evaluator_workload AS
SELECT 
    ep.evaluator_id,
    ep.full_name,
    ep.current_load,
    ep.max_concurrent_evaluations,
    ROUND((ep.current_load::decimal / ep.max_concurrent_evaluations) * 100, 2) as utilization_percentage,
    COUNT(e.id) as total_assigned_evaluations,
    COUNT(CASE WHEN e.status = 'COMPLETED' THEN 1 END) as completed_evaluations,
    AVG(e.processing_time_minutes) as avg_processing_time
FROM evaluator_profiles ep
LEFT JOIN evaluations e ON ep.evaluator_id = e.evaluator_id
WHERE ep.is_active = TRUE
GROUP BY ep.evaluator_id, ep.full_name, ep.current_load, ep.max_concurrent_evaluations;

-- View for interview statistics
CREATE VIEW interview_statistics AS
SELECT 
    i.interviewer_id,
    i.status,
    i.interview_type,
    COUNT(*) as total_interviews,
    AVG(i.duration_minutes) as avg_duration_minutes,
    AVG(i.overall_score) as avg_score,
    COUNT(CASE WHEN i.status = 'NO_SHOW' THEN 1 END) as no_show_count,
    COUNT(CASE WHEN i.reschedule_count > 0 THEN 1 END) as rescheduled_count,
    DATE_TRUNC('month', i.scheduled_at) as month
FROM interviews i
GROUP BY i.interviewer_id, i.status, i.interview_type, DATE_TRUNC('month', i.scheduled_at);

-- ============================================================================
-- COMMENTS FOR DOCUMENTATION
-- ============================================================================

COMMENT ON TABLE evaluations IS 'Evaluaciones académicas y psicológicas de postulantes';
COMMENT ON COLUMN evaluations.application_id IS 'ID de la aplicación en application-service';
COMMENT ON COLUMN evaluations.evaluator_id IS 'ID del evaluador asignado';
COMMENT ON COLUMN evaluations.subject IS 'Materia de la evaluación (MATHEMATICS, LANGUAGE, etc.)';
COMMENT ON COLUMN evaluations.processing_time_minutes IS 'Tiempo total desde asignación hasta completado';
COMMENT ON COLUMN evaluations.sla_exceeded IS 'Indica si se excedió el SLA de tiempo';

COMMENT ON TABLE interviews IS 'Entrevistas programadas con directores y evaluadores';
COMMENT ON COLUMN interviews.buffer_minutes IS 'Tiempo de buffer para evitar conflictos de agenda';
COMMENT ON COLUMN interviews.reschedule_count IS 'Número de veces que se ha reprogramado';

COMMENT ON TABLE rubrics IS 'Rúbricas de evaluación por materia y nivel';
COMMENT ON COLUMN rubrics.items IS 'Items de evaluación en formato JSON con puntajes y pesos';

COMMENT ON TABLE scores IS 'Puntajes individuales por item de rúbrica';

COMMENT ON TABLE evaluator_profiles IS 'Perfiles de evaluadores sincronizados desde user-service';
COMMENT ON COLUMN evaluator_profiles.current_load IS 'Número actual de evaluaciones activas';
COMMENT ON COLUMN evaluator_profiles.priority_weight IS 'Peso para algoritmo de asignación round-robin';

COMMENT ON VIEW evaluation_metrics IS 'Vista con métricas agregadas de evaluaciones';
COMMENT ON VIEW evaluator_workload IS 'Vista con carga de trabajo de evaluadores';
COMMENT ON VIEW interview_statistics IS 'Vista con estadísticas de entrevistas';