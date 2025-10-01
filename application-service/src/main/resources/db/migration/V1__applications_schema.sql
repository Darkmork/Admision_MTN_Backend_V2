-- V1__applications_schema.sql
-- Schema inicial para application-service - Dominio de Admisiones

-- Extensión para UUID
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Tabla principal de aplicaciones/postulaciones
CREATE TABLE applications (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  created_by TEXT NOT NULL,                    -- user id del apoderado o admin que creó
  status TEXT NOT NULL DEFAULT 'DRAFT',       -- ApplicationStatus enum
  grade_applied TEXT,                          -- grado al que postula (opcional)
  applicant JSONB,                            -- datos mínimos del postulante (nombre, rut, fecha_nacimiento)
  
  -- Campos de auditoría y concurrencia
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  version INT NOT NULL DEFAULT 0,             -- para optimistic locking @Version
  
  -- Campos adicionales de metadatos
  source_channel TEXT DEFAULT 'WEB',          -- WEB, API, MIGRATION
  external_reference TEXT,                    -- referencia externa si aplica
  notes TEXT,                                 -- notas administrativas
  
  -- Campos de contexto familiar
  family_context JSONB,                       -- datos familiares agregados (opcional)
  special_needs BOOLEAN DEFAULT FALSE,        -- indicador de necesidades especiales
  
  -- Timestamps de estados críticos
  submitted_at TIMESTAMPTZ,                   -- cuando se envió (DRAFT -> PENDING)
  approved_at TIMESTAMPTZ,                    -- cuando se aprobó
  rejected_at TIMESTAMPTZ,                    -- cuando se rechazó
  enrolled_at TIMESTAMPTZ                     -- cuando se matriculó
);

-- Índices para applications
CREATE INDEX idx_applications_status ON applications(status);
CREATE INDEX idx_applications_created_at ON applications(created_at);
CREATE INDEX idx_applications_created_by ON applications(created_by);
CREATE INDEX idx_applications_submitted_at ON applications(submitted_at) WHERE submitted_at IS NOT NULL;
CREATE INDEX idx_applications_grade_applied ON applications(grade_applied) WHERE grade_applied IS NOT NULL;
CREATE INDEX idx_applications_special_needs ON applications(special_needs) WHERE special_needs = TRUE;

-- Índices JSONB para búsquedas en applicant
CREATE INDEX idx_applications_applicant_gin ON applications USING GIN(applicant);

-- Log de transiciones de estado
CREATE TABLE transition_log (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  application_id UUID NOT NULL REFERENCES applications(id) ON DELETE CASCADE,
  from_state TEXT NOT NULL,                   -- estado anterior
  to_state TEXT NOT NULL,                     -- estado nuevo
  reason_code TEXT NOT NULL,                  -- ReasonCode enum
  actor_user_id TEXT NOT NULL,               -- quién ejecutó la transición
  actor_role TEXT,                           -- rol del actor
  comment TEXT,                              -- comentario opcional
  
  -- Metadatos de la transición
  idempotency_key TEXT,                      -- clave de idempotencia única
  transition_data JSONB,                     -- datos adicionales de la transición
  automated BOOLEAN DEFAULT FALSE,           -- si fue automática o manual
  
  -- Auditoría
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  
  -- Context adicional
  ip_address INET,                           -- IP del actor
  user_agent TEXT                            -- User agent si aplica
);

-- Índices para transition_log
CREATE INDEX idx_transition_log_application_id ON transition_log(application_id);
CREATE INDEX idx_transition_log_created_at ON transition_log(created_at DESC);
CREATE INDEX idx_transition_log_from_to_state ON transition_log(from_state, to_state);
CREATE INDEX idx_transition_log_reason_code ON transition_log(reason_code);
CREATE INDEX idx_transition_log_actor_user_id ON transition_log(actor_user_id);

-- Índice único para idempotencia (solo para claves no nulas)
CREATE UNIQUE INDEX idx_transition_log_idempotency_key 
  ON transition_log(idempotency_key) 
  WHERE idempotency_key IS NOT NULL;

-- Metadatos de documentos (binarios permanecen en monolito)
CREATE TABLE app_documents (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  application_id UUID NOT NULL REFERENCES applications(id) ON DELETE CASCADE,
  external_id TEXT NOT NULL,                 -- ID del binario en monolito o file-service
  doc_type TEXT NOT NULL,                    -- CERTIFICATE, ID_COPY, PHOTO, MEDICAL_REPORT, etc.
  filename TEXT NOT NULL,                    -- nombre original del archivo
  mime_type TEXT,                           -- application/pdf, image/jpeg, etc.
  size_bytes BIGINT,                        -- tamaño en bytes
  checksum TEXT,                            -- hash para integridad
  
  -- Estados del documento
  status TEXT DEFAULT 'UPLOADED',           -- UPLOADED, APPROVED, REJECTED, EXPIRED
  review_status TEXT DEFAULT 'PENDING',     -- PENDING, IN_REVIEW, APPROVED, REJECTED
  rejection_reason TEXT,                    -- razón de rechazo si aplica
  
  -- Auditoría
  uploaded_by TEXT NOT NULL,                -- quien subió el documento
  reviewed_by TEXT,                         -- quien revisó el documento
  uploaded_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  reviewed_at TIMESTAMPTZ,                  -- cuando se revisó
  
  -- Metadatos adicionales
  document_category TEXT,                   -- ACADEMIC, PERSONAL, MEDICAL, FINANCIAL
  required BOOLEAN DEFAULT TRUE,            -- si es requerido o opcional
  expiry_date DATE,                        -- fecha de vencimiento si aplica
  
  -- Versioning de documentos
  version_number INT DEFAULT 1,            -- número de versión del documento
  replaces_document_id UUID REFERENCES app_documents(id), -- documento que reemplaza
  
  -- Context
  upload_source TEXT DEFAULT 'WEB'         -- WEB, API, ADMIN_UPLOAD
);

-- Índices para app_documents
CREATE INDEX idx_app_documents_application_id ON app_documents(application_id);
CREATE INDEX idx_app_documents_external_id ON app_documents(external_id);
CREATE INDEX idx_app_documents_doc_type ON app_documents(doc_type);
CREATE INDEX idx_app_documents_status ON app_documents(status);
CREATE INDEX idx_app_documents_review_status ON app_documents(review_status);
CREATE INDEX idx_app_documents_uploaded_at ON app_documents(uploaded_at);
CREATE INDEX idx_app_documents_required ON app_documents(required) WHERE required = TRUE;
CREATE INDEX idx_app_documents_expiry_date ON app_documents(expiry_date) WHERE expiry_date IS NOT NULL;

-- Índice único para evitar duplicados por external_id y application_id
CREATE UNIQUE INDEX idx_app_documents_external_app_unique 
  ON app_documents(application_id, external_id);

-- Función para actualizar updated_at automáticamente
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Trigger para actualizar updated_at en applications
CREATE TRIGGER update_applications_updated_at 
    BEFORE UPDATE ON applications
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Vista para estadísticas de aplicaciones por estado
CREATE VIEW application_statistics AS
SELECT 
    status,
    COUNT(*) as count,
    COUNT(*) FILTER (WHERE created_at >= NOW() - INTERVAL '1 day') as count_last_day,
    COUNT(*) FILTER (WHERE created_at >= NOW() - INTERVAL '7 days') as count_last_week,
    COUNT(*) FILTER (WHERE created_at >= NOW() - INTERVAL '30 days') as count_last_month,
    AVG(EXTRACT(EPOCH FROM (updated_at - created_at))) as avg_time_in_status_seconds,
    MIN(created_at) as earliest_created,
    MAX(created_at) as latest_created
FROM applications 
GROUP BY status;

-- Vista para estadísticas de transiciones
CREATE VIEW transition_statistics AS
SELECT 
    from_state,
    to_state,
    reason_code,
    COUNT(*) as transition_count,
    COUNT(*) FILTER (WHERE created_at >= NOW() - INTERVAL '7 days') as count_last_week,
    AVG(EXTRACT(EPOCH FROM (created_at - (
        SELECT MAX(tl2.created_at) 
        FROM transition_log tl2 
        WHERE tl2.application_id = transition_log.application_id 
        AND tl2.created_at < transition_log.created_at
    )))) as avg_time_between_transitions_seconds,
    COUNT(DISTINCT actor_user_id) as unique_actors
FROM transition_log 
GROUP BY from_state, to_state, reason_code;

-- Vista para documentos pendientes de revisión
CREATE VIEW pending_document_reviews AS
SELECT 
    ad.id,
    ad.application_id,
    ad.doc_type,
    ad.filename,
    ad.uploaded_at,
    ad.uploaded_by,
    a.status as application_status,
    a.grade_applied,
    EXTRACT(EPOCH FROM (NOW() - ad.uploaded_at))/3600 as hours_pending
FROM app_documents ad
JOIN applications a ON ad.application_id = a.id
WHERE ad.review_status = 'PENDING'
ORDER BY ad.uploaded_at ASC;

-- Función para limpiar logs de transición antiguos
CREATE OR REPLACE FUNCTION cleanup_old_transition_logs(retention_days INT DEFAULT 365)
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
    cutoff_date TIMESTAMPTZ;
BEGIN
    cutoff_date := NOW() - (retention_days || ' days')::INTERVAL;
    
    DELETE FROM transition_log 
    WHERE created_at < cutoff_date
    AND to_state IN ('REJECTED', 'ENROLLED', 'EXPIRED'); -- Solo estados terminales
    
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- Función para obtener el estado actual más reciente de una aplicación
CREATE OR REPLACE FUNCTION get_application_current_state(app_id UUID)
RETURNS TEXT AS $$
DECLARE
    current_state TEXT;
BEGIN
    SELECT status INTO current_state
    FROM applications
    WHERE id = app_id;
    
    RETURN current_state;
END;
$$ LANGUAGE plpgsql;

-- Función para validar transiciones de estado
CREATE OR REPLACE FUNCTION is_valid_state_transition(from_state TEXT, to_state TEXT)
RETURNS BOOLEAN AS $$
BEGIN
    -- Esta función será usada por el código Java, pero incluimos lógica básica aquí
    RETURN CASE 
        WHEN from_state = 'DRAFT' AND to_state = 'PENDING' THEN TRUE
        WHEN from_state = 'PENDING' AND to_state IN ('UNDER_REVIEW', 'DOCUMENTS_REQUESTED') THEN TRUE
        WHEN from_state = 'DOCUMENTS_REQUESTED' AND to_state = 'PENDING' THEN TRUE
        WHEN from_state = 'UNDER_REVIEW' AND to_state IN ('INTERVIEW_SCHEDULED', 'REJECTED') THEN TRUE
        WHEN from_state = 'INTERVIEW_SCHEDULED' AND to_state IN ('EXAM_SCHEDULED', 'REJECTED') THEN TRUE
        WHEN from_state = 'EXAM_SCHEDULED' AND to_state IN ('APPROVED', 'REJECTED', 'WAITLIST') THEN TRUE
        WHEN from_state = 'WAITLIST' AND to_state IN ('APPROVED', 'EXPIRED') THEN TRUE
        WHEN from_state = 'APPROVED' AND to_state IN ('ENROLLED', 'EXPIRED') THEN TRUE
        ELSE FALSE
    END;
END;
$$ LANGUAGE plpgsql;

-- Comentarios en tablas para documentación
COMMENT ON TABLE applications IS 'Tabla principal de postulaciones/aplicaciones de admisión';
COMMENT ON TABLE transition_log IS 'Log de auditoría de todas las transiciones de estado';
COMMENT ON TABLE app_documents IS 'Metadatos de documentos adjuntos (binarios en sistema externo)';

COMMENT ON COLUMN applications.version IS 'Campo para optimistic locking JPA @Version';
COMMENT ON COLUMN applications.applicant IS 'Datos JSONB del postulante: {nombre, rut, fecha_nacimiento, etc}';
COMMENT ON COLUMN applications.family_context IS 'Contexto familiar JSONB: padres, apoderados, situación socioeconómica';
COMMENT ON COLUMN transition_log.idempotency_key IS 'Clave única para prevenir transiciones duplicadas';
COMMENT ON COLUMN app_documents.external_id IS 'ID del archivo binario en el sistema externo (monolito)';

-- Insertar datos de ejemplo para desarrollo/testing (solo en perfil local)
-- Estos datos se pueden eliminar en producción

-- Ejemplo de aplicación en estado DRAFT
INSERT INTO applications (id, created_by, status, grade_applied, applicant, family_context, notes) VALUES
(
    '550e8400-e29b-41d4-a716-446655440001',
    'apoderado-test-001',
    'DRAFT',
    'KINDER',
    '{"nombre": "Juan Pérez", "rut": "12345678-9", "fecha_nacimiento": "2018-05-15"}',
    '{"padre": {"nombre": "Carlos Pérez", "telefono": "+56912345678"}, "madre": {"nombre": "María González", "telefono": "+56987654321"}}',
    'Aplicación de prueba creada durante migración'
);

-- Ejemplo de aplicación en estado PENDING
INSERT INTO applications (id, created_by, status, grade_applied, applicant, submitted_at, notes) VALUES
(
    '550e8400-e29b-41d4-a716-446655440002',
    'apoderado-test-002',
    'PENDING',
    'PRIMERO_BASICO',
    '{"nombre": "Ana Silva", "rut": "98765432-1", "fecha_nacimiento": "2017-08-22"}',
    NOW() - INTERVAL '2 days',
    'Aplicación enviada hace 2 días'
);

-- Ejemplo de transición para la aplicación PENDING
INSERT INTO transition_log (application_id, from_state, to_state, reason_code, actor_user_id, actor_role, comment) VALUES
(
    '550e8400-e29b-41d4-a716-446655440002',
    'DRAFT',
    'PENDING',
    'FORM_SUBMITTED',
    'apoderado-test-002',
    'APODERADO',
    'Formulario completado y enviado por el apoderado'
);

-- Ejemplo de documento
INSERT INTO app_documents (application_id, external_id, doc_type, filename, mime_type, size_bytes, uploaded_by) VALUES
(
    '550e8400-e29b-41d4-a716-446655440002',
    'monolito-doc-12345',
    'BIRTH_CERTIFICATE',
    'certificado_nacimiento_ana_silva.pdf',
    'application/pdf',
    245760,
    'apoderado-test-002'
);