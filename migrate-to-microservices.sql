-- ======================================================
-- MIGRACIÓN DE DATOS DEL MONOLITO A MICROSERVICIOS
-- Sistema de Admisión MTN - Migración completa de datos
-- ======================================================

-- Este script migra datos del monolito a las bases de datos de los microservicios
-- IMPORTANTE: Ejecutar este script cuando los microservicios estén funcionando

BEGIN;

-- ======================================================
-- 1. MIGRACIÓN DE USUARIOS (User Service)
-- ======================================================

-- Conectar a la base de datos del user-service
\c users_db

-- Crear tabla de usuarios en el microservicio si no existe
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    rut VARCHAR(20) UNIQUE,
    phone VARCHAR(20),
    role VARCHAR(50) NOT NULL,
    educational_level VARCHAR(50),
    subject VARCHAR(100),
    email_verified BOOLEAN DEFAULT false,
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

-- Crear tabla de roles
CREATE TABLE IF NOT EXISTS roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL,
    description TEXT,
    permissions TEXT[],
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insertar roles estándar
INSERT INTO roles (name, description, permissions) VALUES 
('ADMIN', 'Administrator role with full access', ARRAY['*']),
('COORDINATOR', 'Academic coordinator', ARRAY['applications:read', 'applications:write', 'evaluations:read']),
('TEACHER', 'Teacher role for evaluations', ARRAY['evaluations:read', 'evaluations:write']),
('PSYCHOLOGIST', 'Psychologist for psychological evaluations', ARRAY['evaluations:read', 'evaluations:write', 'interviews:read']),
('CYCLE_DIRECTOR', 'Cycle director for interviews', ARRAY['interviews:read', 'interviews:write', 'evaluations:read']),
('APODERADO', 'Family/Guardian role', ARRAY['applications:read', 'applications:write', 'documents:upload'])
ON CONFLICT (name) DO NOTHING;

-- Migrar usuarios del monolito
INSERT INTO users (first_name, last_name, email, password, rut, phone, role, educational_level, subject, email_verified, active, created_at)
SELECT 
    first_name,
    last_name,
    email,
    password,
    rut,
    phone,
    role,
    educational_level,
    subject,
    email_verified,
    active,
    created_at
FROM dblink('host=localhost port=5432 dbname=Admisión_MTN_DB user=admin password=admin123',
    'SELECT first_name, last_name, email, password, rut, phone, role, educational_level, subject, email_verified, active, created_at FROM users')
    AS monolith_users(first_name VARCHAR, last_name VARCHAR, email VARCHAR, password VARCHAR, rut VARCHAR, phone VARCHAR, 
                     role VARCHAR, educational_level VARCHAR, subject VARCHAR, email_verified BOOLEAN, active BOOLEAN, created_at TIMESTAMP)
ON CONFLICT (email) DO UPDATE SET
    first_name = EXCLUDED.first_name,
    last_name = EXCLUDED.last_name,
    rut = EXCLUDED.rut,
    phone = EXCLUDED.phone,
    role = EXCLUDED.role,
    educational_level = EXCLUDED.educational_level,
    subject = EXCLUDED.subject,
    email_verified = EXCLUDED.email_verified,
    active = EXCLUDED.active,
    updated_at = CURRENT_TIMESTAMP;

-- ======================================================
-- 2. MIGRACIÓN DE APLICACIONES (Application Service)
-- ======================================================

-- Conectar a la base de datos del application-service
\c applications_db

-- Crear tablas necesarias para el application-service
CREATE TABLE IF NOT EXISTS applications (
    id BIGSERIAL PRIMARY KEY,
    student_name VARCHAR(255) NOT NULL,
    student_rut VARCHAR(20) UNIQUE,
    birth_date DATE,
    grade_level VARCHAR(50),
    target_school VARCHAR(50),
    applicant_email VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    submission_date TIMESTAMP,
    last_modified TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_modified_by VARCHAR(255),
    
    -- Special categories
    is_employee_child BOOLEAN DEFAULT false,
    employee_parent_name VARCHAR(255),
    is_alumni_child BOOLEAN DEFAULT false,
    alumni_parent_year INTEGER,
    is_inclusion_student BOOLEAN DEFAULT false,
    inclusion_type VARCHAR(100),
    inclusion_notes TEXT,
    
    -- Document status
    documents_complete BOOLEAN DEFAULT false,
    required_documents_count INTEGER DEFAULT 0,
    uploaded_documents_count INTEGER DEFAULT 0,
    
    -- Contact information
    primary_phone VARCHAR(20),
    secondary_phone VARCHAR(20),
    address TEXT,
    city VARCHAR(100),
    region VARCHAR(100),
    
    -- Academic information
    previous_school VARCHAR(255),
    last_year_gpa DECIMAL(3,2),
    academic_strengths TEXT,
    extracurricular_activities TEXT,
    
    -- Medical information
    has_special_needs BOOLEAN DEFAULT false,
    special_needs_description TEXT,
    has_medical_conditions BOOLEAN DEFAULT false,
    medical_conditions_description TEXT,
    
    -- Application metadata
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

-- Crear tabla de parents
CREATE TABLE IF NOT EXISTS parents (
    id BIGSERIAL PRIMARY KEY,
    application_id BIGINT REFERENCES applications(id),
    full_name VARCHAR(255) NOT NULL,
    rut VARCHAR(20),
    email VARCHAR(255),
    phone VARCHAR(20),
    relationship VARCHAR(50), -- FATHER, MOTHER
    occupation VARCHAR(255),
    work_place VARCHAR(255),
    education_level VARCHAR(100),
    birth_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Crear tabla de documentos
CREATE TABLE IF NOT EXISTS app_documents (
    id BIGSERIAL PRIMARY KEY,
    application_id BIGINT REFERENCES applications(id),
    document_type VARCHAR(100) NOT NULL,
    file_name VARCHAR(255),
    file_path VARCHAR(500),
    file_size BIGINT,
    uploaded BOOLEAN DEFAULT false,
    upload_date TIMESTAMP,
    status VARCHAR(50) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Crear tabla de outbox para eventos
CREATE TABLE IF NOT EXISTS outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type VARCHAR(255) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    event_data JSONB NOT NULL,
    occurred_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP,
    processed BOOLEAN DEFAULT false,
    version INTEGER DEFAULT 1
);

-- Migrar aplicaciones del monolito
INSERT INTO applications (
    id, student_name, student_rut, birth_date, grade_level, target_school, 
    applicant_email, status, submission_date, last_modified, last_modified_by,
    is_employee_child, employee_parent_name, is_alumni_child, alumni_parent_year,
    is_inclusion_student, inclusion_type, inclusion_notes,
    documents_complete, primary_phone, address, city,
    has_special_needs, special_needs_description, created_at, updated_at
)
SELECT 
    a.id,
    CONCAT(s.first_name, ' ', s.last_name) as student_name,
    s.rut as student_rut,
    s.birth_date,
    CASE 
        WHEN s.grade = 'PRE_K' THEN 'PRE_K'
        WHEN s.grade = 'KINDER' THEN 'KINDER'
        ELSE COALESCE(s.grade, 'UNKNOWN')
    END as grade_level,
    CASE 
        WHEN s.target_school = 'MONTE_TABOR' THEN 'MONTE_TABOR'
        WHEN s.target_school = 'NAZARET' THEN 'NAZARET'
        ELSE 'MONTE_TABOR'
    END as target_school,
    u.email as applicant_email,
    a.status,
    a.submission_date,
    a.updated_at,
    'MIGRATION_SCRIPT',
    COALESCE(s.is_employee_child, false),
    s.employee_parent_name,
    COALESCE(s.is_alumni_child, false),
    s.alumni_parent_year,
    COALESCE(s.is_inclusion_student, false),
    s.inclusion_type,
    s.inclusion_notes,
    false, -- documents_complete - will be calculated later
    COALESCE(u.phone, '+56900000000'),
    'Dirección migrada',
    'Santiago',
    false,
    NULL,
    a.created_at,
    a.updated_at
FROM dblink('host=localhost port=5432 dbname=Admisión_MTN_DB user=admin password=admin123',
    'SELECT a.id, a.status, a.submission_date, a.created_at, a.updated_at, 
            s.first_name, s.last_name, s.rut, s.birth_date, s.grade, s.target_school,
            s.is_employee_child, s.employee_parent_name, s.is_alumni_child, s.alumni_parent_year,
            s.is_inclusion_student, s.inclusion_type, s.inclusion_notes,
            u.email, u.phone
     FROM applications a 
     JOIN students s ON a.student_id = s.id 
     JOIN users u ON a.applicant_user_id = u.id')
    AS monolith_data(id BIGINT, status VARCHAR, submission_date TIMESTAMP, created_at TIMESTAMP, updated_at TIMESTAMP,
                    first_name VARCHAR, last_name VARCHAR, rut VARCHAR, birth_date DATE, grade VARCHAR, target_school VARCHAR,
                    is_employee_child BOOLEAN, employee_parent_name VARCHAR, is_alumni_child BOOLEAN, alumni_parent_year INTEGER,
                    is_inclusion_student BOOLEAN, inclusion_type VARCHAR, inclusion_notes TEXT,
                    email VARCHAR, phone VARCHAR) a
    JOIN monolith_data u ON true -- Join with the same data for user info
ON CONFLICT (id) DO UPDATE SET
    student_name = EXCLUDED.student_name,
    status = EXCLUDED.status,
    submission_date = EXCLUDED.submission_date,
    updated_at = CURRENT_TIMESTAMP;

-- Migrar padres/madres
INSERT INTO parents (application_id, full_name, rut, email, phone, relationship, occupation, education_level, birth_date)
SELECT 
    a.id as application_id,
    CONCAT(p.first_name, ' ', p.last_name) as full_name,
    p.rut,
    p.email,
    p.phone,
    'FATHER',
    p.occupation,
    p.education_level,
    p.birth_date
FROM dblink('host=localhost port=5432 dbname=Admisión_MTN_DB user=admin password=admin123',
    'SELECT a.id, p.first_name, p.last_name, p.rut, p.email, p.phone, p.occupation, p.education_level, p.birth_date
     FROM applications a 
     JOIN parents p ON a.father_id = p.id')
    AS father_data(application_id BIGINT, first_name VARCHAR, last_name VARCHAR, rut VARCHAR, 
                   email VARCHAR, phone VARCHAR, occupation VARCHAR, education_level VARCHAR, birth_date DATE) a
UNION ALL
SELECT 
    a.id as application_id,
    CONCAT(p.first_name, ' ', p.last_name) as full_name,
    p.rut,
    p.email,
    p.phone,
    'MOTHER',
    p.occupation,
    p.education_level,
    p.birth_date
FROM dblink('host=localhost port=5432 dbname=Admisión_MTN_DB user=admin password=admin123',
    'SELECT a.id, p.first_name, p.last_name, p.rut, p.email, p.phone, p.occupation, p.education_level, p.birth_date
     FROM applications a 
     JOIN parents p ON a.mother_id = p.id')
    AS mother_data(application_id BIGINT, first_name VARCHAR, last_name VARCHAR, rut VARCHAR, 
                   email VARCHAR, phone VARCHAR, occupation VARCHAR, education_level VARCHAR, birth_date DATE) a
ON CONFLICT DO NOTHING;

-- ======================================================
-- 3. MIGRACIÓN DE EVALUACIONES (Evaluation Service)
-- ======================================================

-- Conectar a la base de datos del evaluation-service
\c evaluations_db

-- Crear tablas para el evaluation-service
CREATE TABLE IF NOT EXISTS evaluations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id UUID NOT NULL,
    evaluator_id VARCHAR(255),
    subject VARCHAR(100) NOT NULL,
    level VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    
    -- Timing
    assigned_at TIMESTAMP,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    expected_completion_at TIMESTAMP,
    
    -- Scoring
    total_score DECIMAL(5,2),
    max_score DECIMAL(5,2),
    percentage DECIMAL(5,2),
    passed BOOLEAN,
    
    -- Assignment
    assignment_reason VARCHAR(100),
    previous_evaluator_id VARCHAR(255),
    priority INTEGER DEFAULT 0,
    
    -- SLA tracking
    sla_exceeded BOOLEAN DEFAULT false,
    processing_time_minutes INTEGER,
    
    -- Notes and metadata
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

-- Crear tabla de entrevistas
CREATE TABLE IF NOT EXISTS interviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id UUID NOT NULL,
    interviewer_id VARCHAR(255),
    interview_type VARCHAR(50) NOT NULL, -- PSYCHOLOGICAL, ACADEMIC, DIRECTOR
    status VARCHAR(50) NOT NULL DEFAULT 'SCHEDULED',
    
    -- Timing
    scheduled_date DATE,
    scheduled_time TIME,
    duration_minutes INTEGER DEFAULT 60,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    
    -- Results
    overall_rating INTEGER CHECK (overall_rating BETWEEN 1 AND 10),
    recommendation VARCHAR(100),
    notes TEXT,
    
    -- Metadata
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

-- Crear tabla de outbox para eventos
CREATE TABLE IF NOT EXISTS outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type VARCHAR(255) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    event_data JSONB NOT NULL,
    occurred_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP,
    processed BOOLEAN DEFAULT false,
    version INTEGER DEFAULT 1
);

-- Migrar evaluaciones del monolito
INSERT INTO evaluations (
    id, application_id, evaluator_id, subject, level, status,
    assigned_at, started_at, completed_at, total_score, max_score,
    percentage, passed, notes, created_at, updated_at
)
SELECT 
    gen_random_uuid() as id,
    e.application_id::UUID,
    COALESCE(e.evaluator_email, 'unassigned'),
    CASE 
        WHEN e.evaluation_type = 'ACADEMIC_MATHEMATICS' THEN 'MATHEMATICS'
        WHEN e.evaluation_type = 'ACADEMIC_LANGUAGE' THEN 'LANGUAGE'
        WHEN e.evaluation_type = 'PSYCHOLOGICAL' THEN 'PSYCHOLOGY'
        ELSE 'GENERAL'
    END as subject,
    CASE 
        WHEN e.level = 'PRESCHOOL' THEN 'PRESCHOOL'
        WHEN e.level = 'BASIC' THEN 'BASIC'
        WHEN e.level = 'HIGH_SCHOOL' THEN 'HIGH_SCHOOL'
        ELSE 'BASIC'
    END as level,
    CASE 
        WHEN e.status = 'COMPLETED' THEN 'COMPLETED'
        WHEN e.status = 'IN_PROGRESS' THEN 'IN_PROGRESS'
        WHEN e.status = 'ASSIGNED' THEN 'ASSIGNED'
        ELSE 'PENDING'
    END as status,
    e.assigned_date as assigned_at,
    e.started_date as started_at,
    e.completed_date as completed_at,
    e.score as total_score,
    100.0 as max_score,
    CASE WHEN e.score IS NOT NULL THEN (e.score * 100.0 / 100.0) ELSE NULL END as percentage,
    CASE WHEN e.score IS NOT NULL THEN (e.score >= 60.0) ELSE NULL END as passed,
    e.notes,
    e.created_at,
    e.updated_at
FROM dblink('host=localhost port=5432 dbname=Admisión_MTN_DB user=admin password=admin123',
    'SELECT application_id, evaluator_email, evaluation_type, level, status,
            assigned_date, started_date, completed_date, score, notes, created_at, updated_at
     FROM evaluations')
    AS monolith_evaluations(application_id BIGINT, evaluator_email VARCHAR, evaluation_type VARCHAR, level VARCHAR, 
                           status VARCHAR, assigned_date TIMESTAMP, started_date TIMESTAMP, completed_date TIMESTAMP,
                           score DECIMAL, notes TEXT, created_at TIMESTAMP, updated_at TIMESTAMP) e
ON CONFLICT DO NOTHING;

-- Migrar entrevistas del monolito
INSERT INTO interviews (
    id, application_id, interviewer_id, interview_type, status,
    scheduled_date, scheduled_time, duration_minutes, started_at, completed_at,
    overall_rating, recommendation, notes, created_at, updated_at
)
SELECT 
    gen_random_uuid() as id,
    i.application_id::UUID,
    COALESCE(i.interviewer_email, 'unassigned'),
    CASE 
        WHEN i.interview_type = 'DIRECTOR' THEN 'DIRECTOR'
        WHEN i.interview_type = 'PSYCHOLOGICAL' THEN 'PSYCHOLOGICAL'
        ELSE 'ACADEMIC'
    END as interview_type,
    CASE 
        WHEN i.status = 'COMPLETED' THEN 'COMPLETED'
        WHEN i.status = 'IN_PROGRESS' THEN 'IN_PROGRESS'
        WHEN i.status = 'SCHEDULED' THEN 'SCHEDULED'
        ELSE 'PENDING'
    END as status,
    i.scheduled_date::DATE,
    i.scheduled_time::TIME,
    COALESCE(i.duration, 60),
    i.started_at,
    i.completed_at,
    i.rating,
    i.recommendation,
    i.notes,
    i.created_at,
    i.updated_at
FROM dblink('host=localhost port=5432 dbname=Admisión_MTN_DB user=admin password=admin123',
    'SELECT application_id, interviewer_email, interview_type, status,
            scheduled_date, scheduled_time, duration, started_at, completed_at,
            rating, recommendation, notes, created_at, updated_at
     FROM interviews')
    AS monolith_interviews(application_id BIGINT, interviewer_email VARCHAR, interview_type VARCHAR, status VARCHAR,
                          scheduled_date TIMESTAMP, scheduled_time TIMESTAMP, duration INTEGER, started_at TIMESTAMP,
                          completed_at TIMESTAMP, rating INTEGER, recommendation VARCHAR, notes TEXT, 
                          created_at TIMESTAMP, updated_at TIMESTAMP) i
ON CONFLICT DO NOTHING;

-- ======================================================
-- 4. VERIFICACIÓN DE MIGRACIÓN
-- ======================================================

-- Verificar conteos de datos migrados
\echo '=========================================='
\echo 'VERIFICACIÓN DE MIGRACIÓN COMPLETADA'
\echo '=========================================='

\c users_db
SELECT 'USERS' as table_name, COUNT(*) as migrated_records FROM users;

\c applications_db  
SELECT 'APPLICATIONS' as table_name, COUNT(*) as migrated_records FROM applications;
SELECT 'PARENTS' as table_name, COUNT(*) as migrated_records FROM parents;

\c evaluations_db
SELECT 'EVALUATIONS' as table_name, COUNT(*) as migrated_records FROM evaluations;
SELECT 'INTERVIEWS' as table_name, COUNT(*) as migrated_records FROM interviews;

-- ======================================================
-- 5. CONFIGURAR SECUENCIAS
-- ======================================================

-- Actualizar secuencias para evitar conflictos de ID
\c users_db
SELECT setval('users_id_seq', COALESCE((SELECT MAX(id) FROM users), 1));

\c applications_db
SELECT setval('applications_id_seq', COALESCE((SELECT MAX(id) FROM applications), 1));
SELECT setval('parents_id_seq', COALESCE((SELECT MAX(id) FROM parents), 1));

\echo '=========================================='
\echo 'MIGRACIÓN COMPLETADA EXITOSAMENTE'
\echo '=========================================='
\echo 'Los datos han sido migrados del monolito a los microservicios.'
\echo 'Verifique que los microservicios estén funcionando correctamente.'

COMMIT;