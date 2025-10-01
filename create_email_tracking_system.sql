-- Sistema de tracking de correos para apoderados
-- Permite rastrear apertura de correos y respuestas automáticas

-- Tabla para rastrear envíos de correos
CREATE TABLE IF NOT EXISTS email_notifications (
    id BIGSERIAL PRIMARY KEY,
    application_id BIGINT NOT NULL REFERENCES applications(id) ON DELETE CASCADE,
    recipient_email VARCHAR(255) NOT NULL,
    email_type VARCHAR(100) NOT NULL, -- 'INTERVIEW_SCHEDULED', 'INTERVIEW_REMINDER', 'ACCEPTANCE', 'REJECTION', etc.
    subject VARCHAR(500) NOT NULL,
    student_name VARCHAR(255) NOT NULL,
    student_gender VARCHAR(10) NOT NULL DEFAULT 'MALE', -- 'MALE', 'FEMALE' para personalización
    target_school VARCHAR(50) NOT NULL, -- 'MONTE_TABOR', 'NAZARET'
    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Tracking de apertura
    opened BOOLEAN DEFAULT FALSE,
    opened_at TIMESTAMP NULL,
    open_count INTEGER DEFAULT 0,
    tracking_token VARCHAR(255) UNIQUE NOT NULL, -- Token único para tracking
    
    -- Respuesta automática
    response_required BOOLEAN DEFAULT FALSE, -- Si requiere respuesta de confirmación
    responded BOOLEAN DEFAULT FALSE,
    response_value VARCHAR(50) NULL, -- 'ACCEPT', 'REJECT', 'RESCHEDULE'
    responded_at TIMESTAMP NULL,
    response_token VARCHAR(255) UNIQUE NULL, -- Token único para respuesta
    
    -- Metadatos
    interview_id BIGINT NULL REFERENCES interviews(id) ON DELETE SET NULL,
    additional_data JSONB DEFAULT '{}', -- Datos adicionales específicos del correo
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Índices para optimizar consultas
CREATE INDEX IF NOT EXISTS idx_email_notifications_application ON email_notifications(application_id);
CREATE INDEX IF NOT EXISTS idx_email_notifications_tracking_token ON email_notifications(tracking_token);
CREATE INDEX IF NOT EXISTS idx_email_notifications_response_token ON email_notifications(response_token);
CREATE INDEX IF NOT EXISTS idx_email_notifications_type ON email_notifications(email_type);
CREATE INDEX IF NOT EXISTS idx_email_notifications_sent_at ON email_notifications(sent_at);

-- Trigger para actualizar updated_at
CREATE OR REPLACE FUNCTION update_email_notifications_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_email_notifications_updated_at
    BEFORE UPDATE ON email_notifications
    FOR EACH ROW
    EXECUTE FUNCTION update_email_notifications_updated_at();

-- Tabla para log de eventos de correo (opcional para análisis detallado)
CREATE TABLE IF NOT EXISTS email_events (
    id BIGSERIAL PRIMARY KEY,
    email_notification_id BIGINT NOT NULL REFERENCES email_notifications(id) ON DELETE CASCADE,
    event_type VARCHAR(50) NOT NULL, -- 'SENT', 'OPENED', 'CLICKED', 'RESPONDED', 'BOUNCED', 'FAILED'
    ip_address INET NULL,
    user_agent TEXT NULL,
    additional_info JSONB DEFAULT '{}',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_email_events_notification ON email_events(email_notification_id);
CREATE INDEX IF NOT EXISTS idx_email_events_type ON email_events(event_type);
CREATE INDEX IF NOT EXISTS idx_email_events_created_at ON email_events(created_at);

-- Función para generar token único
CREATE OR REPLACE FUNCTION generate_unique_token(length INTEGER DEFAULT 32)
RETURNS TEXT AS $$
BEGIN
    RETURN encode(gen_random_bytes(length), 'hex');
END;
$$ LANGUAGE plpgsql;

-- Insertar algunos datos de ejemplo para testing
INSERT INTO email_notifications (
    application_id, 
    recipient_email, 
    email_type, 
    subject, 
    student_name, 
    student_gender,
    target_school,
    tracking_token,
    response_required,
    response_token
) VALUES 
(1, 'familia01@test.cl', 'INTERVIEW_SCHEDULED', 'Entrevista Programada - Ana María (Colegio Monte Tabor)', 'Ana María', 'FEMALE', 'MONTE_TABOR', generate_unique_token(), TRUE, generate_unique_token()),
(2, 'familia02@test.cl', 'INTERVIEW_SCHEDULED', 'Entrevista Programada - Juan Carlos (Colegio Monte Tabor)', 'Juan Carlos', 'MALE', 'MONTE_TABOR', generate_unique_token(), TRUE, generate_unique_token()),
(3, 'familia03@test.cl', 'INTERVIEW_REMINDER', 'Recordatorio de Entrevista - María Elena (Colegio Nazaret)', 'María Elena', 'FEMALE', 'NAZARET', generate_unique_token(), FALSE, NULL);

-- Verificar la creación
SELECT 'Email tracking system created successfully' as status;
SELECT COUNT(*) as email_notifications_count FROM email_notifications;
SELECT COUNT(*) as email_events_count FROM email_events;