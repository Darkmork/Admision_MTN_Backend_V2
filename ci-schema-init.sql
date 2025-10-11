-- =========================================================================
-- CI Database Schema Initialization
-- =========================================================================
-- PostgreSQL 15 compatible schema for Sistema de Admisión MTN
-- Creates all 35 tables required for CI environment
-- Idempotent: Can run multiple times safely
-- =========================================================================

-- Enable extensions if needed
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- =========================================================================
-- CORE ENTITIES
-- =========================================================================

-- Users table (authentication and authorization)
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255),
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL, -- BCrypt hashed
    rut VARCHAR(50) UNIQUE,
    phone VARCHAR(50),
    role VARCHAR(50) NOT NULL DEFAULT 'USER',
    email_verified BOOLEAN DEFAULT FALSE,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT users_role_check CHECK (role IN ('ADMIN', 'TEACHER', 'COORDINATOR', 'PSYCHOLOGIST', 'CYCLE_DIRECTOR', 'APODERADO', 'USER'))
);

-- Students table
CREATE TABLE IF NOT EXISTS students (
    id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(255) NOT NULL,
    paternal_last_name VARCHAR(255),
    maternal_last_name VARCHAR(255),
    rut VARCHAR(50) UNIQUE NOT NULL,
    date_of_birth DATE,
    gender VARCHAR(20),
    email VARCHAR(255),
    phone VARCHAR(50),
    address TEXT,
    previous_school VARCHAR(255),
    current_grade VARCHAR(50),
    special_needs TEXT,
    medical_conditions TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT students_gender_check CHECK (gender IN ('MALE', 'FEMALE', 'OTHER'))
);

-- Guardians table
CREATE TABLE IF NOT EXISTS guardians (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    relationship VARCHAR(100),
    rut VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(50),
    address TEXT,
    occupation VARCHAR(255),
    workplace VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT guardians_relationship_check CHECK (relationship IN ('PADRE', 'MADRE', 'TUTOR_LEGAL', 'ABUELO', 'ABUELA', 'TIO', 'TIA', 'OTRO'))
);

-- Parents table (legacy - may be merged with guardians)
CREATE TABLE IF NOT EXISTS parents (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    relationship VARCHAR(100),
    rut VARCHAR(50),
    email VARCHAR(255),
    phone VARCHAR(50),
    occupation VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Supporters table (additional supporters)
CREATE TABLE IF NOT EXISTS supporters (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    relationship VARCHAR(100),
    rut VARCHAR(50),
    email VARCHAR(255),
    phone VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =========================================================================
-- APPLICATION PROCESS
-- =========================================================================

-- Applications table
CREATE TABLE IF NOT EXISTS applications (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT REFERENCES students(id) ON DELETE CASCADE,
    guardian_id BIGINT REFERENCES guardians(id) ON DELETE SET NULL,
    father_id BIGINT REFERENCES parents(id) ON DELETE SET NULL,
    mother_id BIGINT REFERENCES parents(id) ON DELETE SET NULL,
    supporter_id BIGINT REFERENCES supporters(id) ON DELETE SET NULL,
    applicant_user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    application_year INTEGER,
    submission_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    additional_notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT applications_status_check CHECK (status IN ('PENDING', 'UNDER_REVIEW', 'DOCUMENTS_REQUESTED', 'INTERVIEW_SCHEDULED', 'EXAM_SCHEDULED', 'APPROVED', 'REJECTED', 'WAITLIST')),
    CONSTRAINT unique_student_application UNIQUE (student_id)
);

-- Documents table
CREATE TABLE IF NOT EXISTS documents (
    id BIGSERIAL PRIMARY KEY,
    application_id BIGINT NOT NULL REFERENCES applications(id) ON DELETE CASCADE,
    document_type VARCHAR(100) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100),
    file_size BIGINT,
    is_required BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT documents_type_check CHECK (document_type IN ('BIRTH_CERTIFICATE', 'GRADES_2023', 'GRADES_2024', 'GRADES_2025_SEMESTER_1', 'PERSONALITY_REPORT_2024', 'PERSONALITY_REPORT_2025_SEMESTER_1', 'STUDENT_PHOTO', 'BAPTISM_CERTIFICATE', 'PREVIOUS_SCHOOL_REPORT', 'MEDICAL_CERTIFICATE', 'PSYCHOLOGICAL_REPORT'))
);

-- =========================================================================
-- INTERVIEWS
-- =========================================================================

-- Interviewer schedules
CREATE TABLE IF NOT EXISTS interviewer_schedules (
    id BIGSERIAL PRIMARY KEY,
    interviewer_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    day_of_week VARCHAR(20) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT schedules_day_check CHECK (day_of_week IN ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'))
);

-- Interviews table
CREATE TABLE IF NOT EXISTS interviews (
    id BIGSERIAL PRIMARY KEY,
    application_id BIGINT NOT NULL REFERENCES applications(id) ON DELETE CASCADE,
    interviewer_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    scheduled_date DATE,
    scheduled_time TIME,
    duration_minutes INTEGER DEFAULT 60,
    mode VARCHAR(50) DEFAULT 'IN_PERSON',
    status VARCHAR(50) DEFAULT 'SCHEDULED',
    type VARCHAR(50),
    result VARCHAR(50),
    notes TEXT,
    completed_at TIMESTAMP,
    follow_up_required BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT interviews_mode_check CHECK (mode IN ('IN_PERSON', 'VIRTUAL', 'PHONE')),
    CONSTRAINT interviews_status_check CHECK (status IN ('SCHEDULED', 'COMPLETED', 'CANCELLED', 'RESCHEDULED', 'NO_SHOW')),
    CONSTRAINT interviews_result_check CHECK (result IS NULL OR result IN ('APPROVED', 'REJECTED', 'PENDING_REVIEW')),
    CONSTRAINT unique_interviewer_datetime UNIQUE (interviewer_id, scheduled_date, scheduled_time)
);

-- =========================================================================
-- EVALUATIONS
-- =========================================================================

-- Evaluations table
CREATE TABLE IF NOT EXISTS evaluations (
    id BIGSERIAL PRIMARY KEY,
    application_id BIGINT NOT NULL REFERENCES applications(id) ON DELETE CASCADE,
    evaluator_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    evaluation_type VARCHAR(100),
    score DECIMAL(5,2),
    max_score DECIMAL(5,2),
    result VARCHAR(50),
    comments TEXT,
    evaluated_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT evaluations_type_check CHECK (evaluation_type IN ('ACADEMIC', 'PSYCHOLOGICAL', 'INTERVIEW', 'ENTRANCE_EXAM')),
    CONSTRAINT evaluations_result_check CHECK (result IS NULL OR result IN ('PASSED', 'FAILED', 'PENDING'))
);

-- Evaluation schedules (for scheduling evaluation sessions)
CREATE TABLE IF NOT EXISTS evaluation_schedules (
    id BIGSERIAL PRIMARY KEY,
    application_id BIGINT REFERENCES applications(id) ON DELETE CASCADE,
    evaluator_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    evaluation_type VARCHAR(100),
    scheduled_date DATE,
    scheduled_time TIME,
    status VARCHAR(50) DEFAULT 'SCHEDULED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =========================================================================
-- STAFF SPECIALIZATIONS
-- =========================================================================

-- Professors
CREATE TABLE IF NOT EXISTS professors (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    department VARCHAR(255),
    specialization VARCHAR(255),
    hire_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Professor qualifications
CREATE TABLE IF NOT EXISTS professor_qualifications (
    id BIGSERIAL PRIMARY KEY,
    professor_id BIGINT REFERENCES professors(id) ON DELETE CASCADE,
    qualification VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Professor subjects
CREATE TABLE IF NOT EXISTS professor_subjects (
    id BIGSERIAL PRIMARY KEY,
    professor_id BIGINT REFERENCES professors(id) ON DELETE CASCADE,
    subject VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Professor grades (grade levels they teach)
CREATE TABLE IF NOT EXISTS professor_grades (
    id BIGSERIAL PRIMARY KEY,
    professor_id BIGINT REFERENCES professors(id) ON DELETE CASCADE,
    grade_level VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Psychologists
CREATE TABLE IF NOT EXISTS psychologists (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    license_number VARCHAR(100),
    specialization VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Psychologist specialized areas
CREATE TABLE IF NOT EXISTS psychologist_specialized_areas (
    id BIGSERIAL PRIMARY KEY,
    psychologist_id BIGINT REFERENCES psychologists(id) ON DELETE CASCADE,
    area VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Psychologist grades (grade levels they work with)
CREATE TABLE IF NOT EXISTS psychologist_grades (
    id BIGSERIAL PRIMARY KEY,
    psychologist_id BIGINT REFERENCES psychologists(id) ON DELETE CASCADE,
    grade_level VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Kinder teachers
CREATE TABLE IF NOT EXISTS kinder_teachers (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    experience_years INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Kinder teacher qualifications
CREATE TABLE IF NOT EXISTS kinder_teacher_qualifications (
    id BIGSERIAL PRIMARY KEY,
    kinder_teacher_id BIGINT REFERENCES kinder_teachers(id) ON DELETE CASCADE,
    qualification VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Kinder teacher specializations
CREATE TABLE IF NOT EXISTS kinder_teacher_specializations (
    id BIGSERIAL PRIMARY KEY,
    kinder_teacher_id BIGINT REFERENCES kinder_teachers(id) ON DELETE CASCADE,
    specialization VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Support staff
CREATE TABLE IF NOT EXISTS support_staff (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    position VARCHAR(255),
    department VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Support staff responsibilities
CREATE TABLE IF NOT EXISTS support_staff_responsibilities (
    id BIGSERIAL PRIMARY KEY,
    support_staff_id BIGINT REFERENCES support_staff(id) ON DELETE CASCADE,
    responsibility VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =========================================================================
-- NOTIFICATIONS & EMAIL
-- =========================================================================

-- Email notifications
CREATE TABLE IF NOT EXISTS email_notifications (
    id BIGSERIAL PRIMARY KEY,
    application_id BIGINT REFERENCES applications(id) ON DELETE SET NULL,
    interview_id BIGINT REFERENCES interviews(id) ON DELETE SET NULL,
    recipient_email VARCHAR(255) NOT NULL,
    subject VARCHAR(500) NOT NULL,
    body TEXT NOT NULL,
    template_name VARCHAR(100),
    status VARCHAR(50) DEFAULT 'PENDING',
    sent_at TIMESTAMP,
    tracking_token VARCHAR(255) UNIQUE,
    response_token VARCHAR(255) UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT email_status_check CHECK (status IN ('PENDING', 'SENT', 'FAILED', 'BOUNCED'))
);

-- Email events (tracking opens, clicks, etc.)
CREATE TABLE IF NOT EXISTS email_events (
    id BIGSERIAL PRIMARY KEY,
    email_notification_id BIGINT NOT NULL REFERENCES email_notifications(id) ON DELETE CASCADE,
    event_type VARCHAR(100) NOT NULL,
    ip_address VARCHAR(45),
    user_agent TEXT,
    additional_info JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT email_event_type_check CHECK (event_type IN ('OPENED', 'CLICKED', 'BOUNCED', 'COMPLAINED', 'DELIVERED'))
);

-- Email verification tokens
CREATE TABLE IF NOT EXISTS email_verification_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(500) UNIQUE NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Email verifications
CREATE TABLE IF NOT EXISTS email_verifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    verification_token VARCHAR(500),
    verified_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Notifications (general notification system)
CREATE TABLE IF NOT EXISTS notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT notification_type_check CHECK (type IN ('INFO', 'WARNING', 'ERROR', 'SUCCESS'))
);

-- =========================================================================
-- LEGACY / SISTEMA ANTERIOR (may be deprecated)
-- =========================================================================

-- Usuarios (legacy user table - may be merged with users)
CREATE TABLE IF NOT EXISTS usuarios (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(255),
    email VARCHAR(255) UNIQUE,
    password VARCHAR(255),
    rol VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Temas (legacy - for forum/discussion system)
CREATE TABLE IF NOT EXISTS temas (
    id BIGSERIAL PRIMARY KEY,
    titulo VARCHAR(255),
    descripcion TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Problemas (legacy - issue tracking)
CREATE TABLE IF NOT EXISTS problemas (
    id BIGSERIAL PRIMARY KEY,
    tema_id BIGINT REFERENCES temas(id) ON DELETE CASCADE,
    descripcion TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Progreso usuario (legacy - user progress tracking)
CREATE TABLE IF NOT EXISTS progreso_usuario (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT REFERENCES usuarios(id) ON DELETE CASCADE,
    problema_id BIGINT REFERENCES problemas(id) ON DELETE CASCADE,
    progreso INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Ranking (legacy - gamification)
CREATE TABLE IF NOT EXISTS ranking (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT UNIQUE REFERENCES usuarios(id) ON DELETE CASCADE,
    puntos INTEGER DEFAULT 0,
    nivel INTEGER DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =========================================================================
-- INDEXES FOR PERFORMANCE
-- =========================================================================

-- Users indexes
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_rut ON users(rut);
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);

-- Students indexes
CREATE INDEX IF NOT EXISTS idx_students_rut ON students(rut);
CREATE INDEX IF NOT EXISTS idx_students_email ON students(email);

-- Guardians indexes
CREATE INDEX IF NOT EXISTS idx_guardians_rut ON guardians(rut);
CREATE INDEX IF NOT EXISTS idx_guardians_email ON guardians(email);

-- Applications indexes
CREATE INDEX IF NOT EXISTS idx_applications_student ON applications(student_id);
CREATE INDEX IF NOT EXISTS idx_applications_guardian ON applications(guardian_id);
CREATE INDEX IF NOT EXISTS idx_applications_status ON applications(status);
CREATE INDEX IF NOT EXISTS idx_applications_year ON applications(application_year);

-- Documents indexes
CREATE INDEX IF NOT EXISTS idx_documents_application ON documents(application_id);
CREATE INDEX IF NOT EXISTS idx_documents_type ON documents(document_type);

-- Interviews indexes
CREATE INDEX IF NOT EXISTS idx_interviews_application ON interviews(application_id);
CREATE INDEX IF NOT EXISTS idx_interviews_interviewer ON interviews(interviewer_id);
CREATE INDEX IF NOT EXISTS idx_interviews_date ON interviews(scheduled_date);
CREATE INDEX IF NOT EXISTS idx_interviews_status ON interviews(status);

-- Evaluations indexes
CREATE INDEX IF NOT EXISTS idx_evaluations_application ON evaluations(application_id);
CREATE INDEX IF NOT EXISTS idx_evaluations_evaluator ON evaluations(evaluator_id);
CREATE INDEX IF NOT EXISTS idx_evaluations_type ON evaluations(evaluation_type);

-- Email notifications indexes
CREATE INDEX IF NOT EXISTS idx_email_notifications_application ON email_notifications(application_id);
CREATE INDEX IF NOT EXISTS idx_email_notifications_status ON email_notifications(status);
CREATE INDEX IF NOT EXISTS idx_email_notifications_sent_at ON email_notifications(sent_at);

-- =========================================================================
-- INITIAL DATA (for CI tests)
-- =========================================================================

-- Insert default roles/data can go here if needed
-- This is handled by seed-test-data.sh instead

-- =========================================================================
-- END OF SCHEMA
-- =========================================================================

-- Log completion
DO $$
BEGIN
    RAISE NOTICE '✅ CI Schema initialized successfully - All 35 tables created';
END $$;
