-- V1__Create_initial_schema.sql
-- Creación del esquema inicial para user-service

-- Extensión para UUID
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Tabla de roles
CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(50) NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    category VARCHAR(50) NOT NULL DEFAULT 'EDUCATIONAL',
    enabled BOOLEAN NOT NULL DEFAULT true,
    system_role BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by UUID,
    updated_by UUID,
    
    CONSTRAINT roles_name_format CHECK (name ~ '^[A-Z][A-Z0-9_]*$'),
    CONSTRAINT roles_category_check CHECK (category IN ('SYSTEM', 'EDUCATIONAL', 'ADMINISTRATIVE', 'FAMILY'))
);

-- Índices para tabla roles
CREATE INDEX idx_roles_name ON roles(name);
CREATE INDEX idx_roles_category ON roles(category);
CREATE INDEX idx_roles_enabled ON roles(enabled);
CREATE INDEX idx_roles_system_role ON roles(system_role);

-- Tabla de usuarios
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) NOT NULL UNIQUE,
    username VARCHAR(100),
    password_hash VARCHAR(255),
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    rut VARCHAR(12),
    phone VARCHAR(20),
    educational_level VARCHAR(50),
    subject VARCHAR(50),
    enabled BOOLEAN NOT NULL DEFAULT true,
    email_verified BOOLEAN NOT NULL DEFAULT false,
    account_non_expired BOOLEAN NOT NULL DEFAULT true,
    credentials_non_expired BOOLEAN NOT NULL DEFAULT true,
    account_non_locked BOOLEAN NOT NULL DEFAULT true,
    last_login_at TIMESTAMP WITH TIME ZONE,
    login_attempts INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by UUID,
    updated_by UUID,
    
    -- Constraints
    CONSTRAINT users_email_format CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'),
    CONSTRAINT users_rut_format CHECK (rut IS NULL OR rut ~ '^\d{1,2}\.?\d{3}\.?\d{3}-?[0-9kK]$'),
    CONSTRAINT users_phone_format CHECK (phone IS NULL OR phone ~ '^\+?[1-9]\d{1,14}$'),
    CONSTRAINT users_educational_level_check CHECK (educational_level IS NULL OR educational_level IN ('PRESCHOOL', 'BASIC', 'HIGH_SCHOOL', 'ALL_LEVELS')),
    CONSTRAINT users_subject_check CHECK (subject IS NULL OR subject IN ('GENERAL', 'LANGUAGE', 'MATHEMATICS', 'ENGLISH', 'SCIENCE', 'HISTORY', 'ARTS', 'PHYSICAL_EDUCATION', 'TECHNOLOGY', 'RELIGION', 'ALL_SUBJECTS'))
);

-- Índices para tabla users
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_rut ON users(rut);
CREATE INDEX idx_users_enabled ON users(enabled);
CREATE INDEX idx_users_email_verified ON users(email_verified);
CREATE INDEX idx_users_educational_level ON users(educational_level);
CREATE INDEX idx_users_subject ON users(subject);
CREATE INDEX idx_users_created_at ON users(created_at);
CREATE INDEX idx_users_last_login_at ON users(last_login_at);

-- Tabla intermedia para relación many-to-many entre users y roles
CREATE TABLE user_roles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    role_id UUID NOT NULL,
    assigned_by UUID,
    assigned_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP WITH TIME ZONE,
    active BOOLEAN NOT NULL DEFAULT true,
    comments VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    
    -- Foreign Keys
    CONSTRAINT fk_user_roles_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role_id FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_assigned_by FOREIGN KEY (assigned_by) REFERENCES users(id),
    
    -- Unique constraint para evitar duplicados activos
    CONSTRAINT uk_user_roles_user_role_active UNIQUE (user_id, role_id, active)
);

-- Índices para tabla user_roles
CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX idx_user_roles_role_id ON user_roles(role_id);
CREATE INDEX idx_user_roles_active ON user_roles(active);
CREATE INDEX idx_user_roles_assigned_at ON user_roles(assigned_at);
CREATE INDEX idx_user_roles_expires_at ON user_roles(expires_at);

-- Tabla para auditoría de eventos (para implementación futura del Outbox pattern)
CREATE TABLE domain_events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    event_data JSONB NOT NULL,
    event_version INTEGER NOT NULL DEFAULT 1,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    processed_at TIMESTAMP WITH TIME ZONE,
    processed BOOLEAN NOT NULL DEFAULT false,
    correlation_id UUID,
    causation_id UUID,
    user_id UUID,
    
    -- Foreign Keys
    CONSTRAINT fk_domain_events_user_id FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Índices para tabla domain_events
CREATE INDEX idx_domain_events_aggregate_type ON domain_events(aggregate_type);
CREATE INDEX idx_domain_events_aggregate_id ON domain_events(aggregate_id);
CREATE INDEX idx_domain_events_event_type ON domain_events(event_type);
CREATE INDEX idx_domain_events_occurred_at ON domain_events(occurred_at);
CREATE INDEX idx_domain_events_processed ON domain_events(processed);
CREATE INDEX idx_domain_events_correlation_id ON domain_events(correlation_id);

-- Tabla para idempotencia
CREATE TABLE idempotency_keys (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    idempotency_key VARCHAR(255) NOT NULL UNIQUE,
    request_hash VARCHAR(64) NOT NULL,
    response_status INTEGER,
    response_body TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (NOW() + INTERVAL '24 hours'),
    
    CONSTRAINT idempotency_keys_key_format CHECK (length(idempotency_key) <= 255)
);

-- Índices para tabla idempotency_keys
CREATE INDEX idx_idempotency_keys_key ON idempotency_keys(idempotency_key);
CREATE INDEX idx_idempotency_keys_expires_at ON idempotency_keys(expires_at);

-- Tabla para actividad de usuarios (login tracking)
CREATE TABLE user_activities (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    activity_type VARCHAR(50) NOT NULL,
    description VARCHAR(255),
    ip_address INET,
    user_agent TEXT,
    device_info VARCHAR(255),
    location VARCHAR(100),
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    session_id VARCHAR(100),
    
    -- Foreign Keys
    CONSTRAINT fk_user_activities_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Índices para tabla user_activities
CREATE INDEX idx_user_activities_user_id ON user_activities(user_id);
CREATE INDEX idx_user_activities_activity_type ON user_activities(activity_type);
CREATE INDEX idx_user_activities_occurred_at ON user_activities(occurred_at);
CREATE INDEX idx_user_activities_session_id ON user_activities(session_id);

-- Función para actualizar updated_at automáticamente
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Triggers para actualizar updated_at
CREATE TRIGGER update_roles_updated_at BEFORE UPDATE ON roles
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_user_roles_updated_at BEFORE UPDATE ON user_roles
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Función para limpiar claves de idempotencia expiradas
CREATE OR REPLACE FUNCTION cleanup_expired_idempotency_keys()
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    DELETE FROM idempotency_keys WHERE expires_at < NOW();
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- Comentarios en las tablas
COMMENT ON TABLE roles IS 'Roles del sistema para control de acceso basado en roles (RBAC)';
COMMENT ON TABLE users IS 'Usuarios del sistema de admisión escolar';
COMMENT ON TABLE user_roles IS 'Relación many-to-many entre usuarios y roles con información de asignación';
COMMENT ON TABLE domain_events IS 'Eventos de dominio para implementación del patrón Outbox';
COMMENT ON TABLE idempotency_keys IS 'Claves de idempotencia para operaciones POST idempotentes';
COMMENT ON TABLE user_activities IS 'Registro de actividades de usuarios para auditoría y monitoreo';

-- Comentarios en columnas importantes
COMMENT ON COLUMN users.rut IS 'RUT chileno con formato validado (ej: 12.345.678-9)';
COMMENT ON COLUMN users.educational_level IS 'Nivel educativo: PRESCHOOL, BASIC, HIGH_SCHOOL, ALL_LEVELS';
COMMENT ON COLUMN users.subject IS 'Materia de especialización del profesor';
COMMENT ON COLUMN user_roles.expires_at IS 'Fecha de expiración del rol (NULL = permanente)';
COMMENT ON COLUMN domain_events.event_data IS 'Datos del evento en formato JSON';
COMMENT ON COLUMN idempotency_keys.request_hash IS 'Hash SHA-256 del cuerpo de la petición para validación';