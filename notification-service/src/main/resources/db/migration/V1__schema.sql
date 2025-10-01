-- V1__schema.sql
-- Esquema inicial para notification-service

-- Extensión para UUID
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Tabla de plantillas
CREATE TABLE templates(
    id TEXT PRIMARY KEY,
    channel TEXT NOT NULL CHECK (channel IN ('email', 'sms')),
    subject TEXT,                   -- solo para email
    body_text TEXT,                 -- email/sms
    body_html TEXT,                 -- solo para email
    variables JSONB,                -- lista/nombres de variables requeridas
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by TEXT,
    active BOOLEAN NOT NULL DEFAULT true
);

-- Índices para templates
CREATE INDEX idx_templates_channel ON templates(channel);
CREATE INDEX idx_templates_active ON templates(active);
CREATE INDEX idx_templates_created_at ON templates(created_at);

-- Tabla de mensajes
CREATE TABLE messages(
    id UUID PRIMARY KEY,            -- message_id del evento
    channel TEXT NOT NULL CHECK (channel IN ('email', 'sms')),
    to_json JSONB NOT NULL,         -- destinos (array para email, string para sms)
    template_id TEXT,
    payload JSONB NOT NULL,         -- evento original completo
    status TEXT NOT NULL DEFAULT 'RECEIVED' CHECK (status IN ('RECEIVED', 'PROCESSING', 'SENT', 'FAILED', 'DLQ')),
    attempt_count INT NOT NULL DEFAULT 0,
    last_error TEXT,
    provider_message_id TEXT,       -- ID del proveedor (SMTP/SMS)
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    sent_at TIMESTAMPTZ,
    
    -- Campos adicionales para trazabilidad
    correlation_id TEXT,
    idempotency_key TEXT,
    source_service TEXT,            -- monolith, user-service, etc.
    
    -- Campos de contexto
    priority TEXT DEFAULT 'normal' CHECK (priority IN ('normal', 'high')),
    expires_at TIMESTAMPTZ,
    
    -- Foreign key a templates
    CONSTRAINT fk_messages_template FOREIGN KEY (template_id) REFERENCES templates(id)
);

-- Índices para messages
CREATE INDEX idx_messages_status ON messages(status);
CREATE INDEX idx_messages_created_at ON messages(created_at);
CREATE INDEX idx_messages_channel ON messages(channel);
CREATE INDEX idx_messages_template_id ON messages(template_id);
CREATE INDEX idx_messages_correlation_id ON messages(correlation_id);
CREATE INDEX idx_messages_idempotency_key ON messages(idempotency_key);
CREATE INDEX idx_messages_source_service ON messages(source_service);
CREATE INDEX idx_messages_priority ON messages(priority);
CREATE INDEX idx_messages_sent_at ON messages(sent_at) WHERE sent_at IS NOT NULL;

-- Tabla de intentos de entrega
CREATE TABLE delivery_attempts(
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    message_id UUID NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
    attempt_number INT NOT NULL,
    status TEXT NOT NULL CHECK (status IN ('SUCCESS', 'FAILED', 'TIMEOUT', 'REJECTED')),
    error_message TEXT,
    error_code TEXT,
    provider_response TEXT,         -- respuesta completa del proveedor
    duration_ms BIGINT,            -- tiempo de intento en milisegundos
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    -- Campos específicos por canal
    smtp_response_code INT,        -- para email
    sms_segments INT,              -- para SMS
    
    UNIQUE(message_id, attempt_number)
);

-- Índices para delivery_attempts
CREATE INDEX idx_delivery_attempts_message_id ON delivery_attempts(message_id);
CREATE INDEX idx_delivery_attempts_status ON delivery_attempts(status);
CREATE INDEX idx_delivery_attempts_created_at ON delivery_attempts(created_at);
CREATE INDEX idx_delivery_attempts_attempt_number ON delivery_attempts(attempt_number);

-- Tabla de rate limiting
CREATE TABLE rate_limits(
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    key_type TEXT NOT NULL,        -- 'email', 'sms', 'template', 'recipient'
    key_value TEXT NOT NULL,       -- valor específico
    window_start TIMESTAMPTZ NOT NULL,
    window_end TIMESTAMPTZ NOT NULL,
    count INT NOT NULL DEFAULT 0,
    limit_exceeded BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    UNIQUE(key_type, key_value, window_start)
);

-- Índices para rate_limits
CREATE INDEX idx_rate_limits_key_type_value ON rate_limits(key_type, key_value);
CREATE INDEX idx_rate_limits_window_end ON rate_limits(window_end);
CREATE INDEX idx_rate_limits_limit_exceeded ON rate_limits(limit_exceeded);

-- Función para actualizar updated_at automáticamente
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Triggers para updated_at
CREATE TRIGGER update_templates_updated_at BEFORE UPDATE ON templates
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_rate_limits_updated_at BEFORE UPDATE ON rate_limits
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Vista para estadísticas de mensajes
CREATE VIEW message_statistics AS
SELECT 
    channel,
    status,
    COUNT(*) as count,
    COUNT(*) FILTER (WHERE created_at >= NOW() - INTERVAL '1 hour') as count_last_hour,
    COUNT(*) FILTER (WHERE created_at >= NOW() - INTERVAL '24 hours') as count_last_day,
    AVG(attempt_count) as avg_attempts,
    MAX(attempt_count) as max_attempts
FROM messages 
GROUP BY channel, status;

-- Vista para estadísticas de templates
CREATE VIEW template_usage_statistics AS
SELECT 
    t.id,
    t.channel,
    t.subject,
    COUNT(m.id) as usage_count,
    COUNT(m.id) FILTER (WHERE m.created_at >= NOW() - INTERVAL '24 hours') as usage_last_day,
    COUNT(m.id) FILTER (WHERE m.status = 'SENT') as success_count,
    COUNT(m.id) FILTER (WHERE m.status = 'FAILED') as failure_count,
    CASE 
        WHEN COUNT(m.id) > 0 THEN 
            ROUND((COUNT(m.id) FILTER (WHERE m.status = 'SENT')::DECIMAL / COUNT(m.id)) * 100, 2)
        ELSE 0 
    END as success_rate_percentage
FROM templates t
LEFT JOIN messages m ON t.id = m.template_id
GROUP BY t.id, t.channel, t.subject;

-- Función para limpiar mensajes antiguos
CREATE OR REPLACE FUNCTION cleanup_old_messages(retention_days INT DEFAULT 30)
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
    cutoff_date TIMESTAMPTZ;
BEGIN
    cutoff_date := NOW() - (retention_days || ' days')::INTERVAL;
    
    -- Eliminar intentos de entrega primero (por FK)
    DELETE FROM delivery_attempts 
    WHERE message_id IN (
        SELECT id FROM messages 
        WHERE created_at < cutoff_date 
        AND status IN ('SENT', 'FAILED', 'DLQ')
    );
    
    -- Eliminar mensajes
    DELETE FROM messages 
    WHERE created_at < cutoff_date 
    AND status IN ('SENT', 'FAILED', 'DLQ');
    
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- Función para limpiar rate limits expirados
CREATE OR REPLACE FUNCTION cleanup_expired_rate_limits()
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    DELETE FROM rate_limits WHERE window_end < NOW() - INTERVAL '1 hour';
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- Función para obtener estadísticas de rendimiento
CREATE OR REPLACE FUNCTION get_performance_stats()
RETURNS TABLE(
    metric_name TEXT,
    metric_value NUMERIC,
    metric_unit TEXT
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        'avg_delivery_time_seconds' as metric_name,
        AVG(EXTRACT(EPOCH FROM (sent_at - created_at))) as metric_value,
        'seconds' as metric_unit
    FROM messages 
    WHERE sent_at IS NOT NULL AND created_at >= NOW() - INTERVAL '24 hours'
    
    UNION ALL
    
    SELECT 
        'success_rate_percentage' as metric_name,
        ROUND(
            (COUNT(*) FILTER (WHERE status = 'SENT')::DECIMAL / COUNT(*)) * 100, 
            2
        ) as metric_value,
        'percentage' as metric_unit
    FROM messages 
    WHERE created_at >= NOW() - INTERVAL '24 hours'
    
    UNION ALL
    
    SELECT 
        'messages_per_hour' as metric_name,
        COUNT(*)::NUMERIC / 24 as metric_value,
        'messages/hour' as metric_unit
    FROM messages 
    WHERE created_at >= NOW() - INTERVAL '24 hours';
END;
$$ LANGUAGE plpgsql;

-- Insertar plantillas predefinidas
INSERT INTO templates (id, channel, subject, body_text, body_html, variables, created_by) VALUES
-- Templates de Email
('user_account_created', 'email', 
 'Bienvenido al Sistema de Admisión MTN',
 'Estimado/a {{nombre}},

Su cuenta ha sido creada exitosamente en el Sistema de Admisión del Colegio Monte Tabor y Nazaret.

Credenciales de acceso:
Email: {{email}}
Contraseña temporal: {{password_temporal}}

Para acceder al sistema, visite: {{url_login}}
Es importante que cambie su contraseña en el primer acceso: {{url_cambiar_password}}

Saludos cordiales,
Sistema de Admisión MTN',
 '<html><body style="font-family: Arial, sans-serif;">
<h2>Bienvenido/a al Sistema de Admisión MTN</h2>
<p>Estimado/a <strong>{{nombre}}</strong>,</p>
<p>Su cuenta ha sido creada exitosamente en el Sistema de Admisión del Colegio Monte Tabor y Nazaret.</p>
<div style="background-color: #f0f0f0; padding: 15px; margin: 20px 0; border-radius: 5px;">
<h3>Credenciales de acceso:</h3>
<p><strong>Email:</strong> {{email}}</p>
<p><strong>Contraseña temporal:</strong> <code>{{password_temporal}}</code></p>
</div>
<p><a href="{{url_login}}" style="background-color: #007bff; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;">Acceder al Sistema</a></p>
<p><em>Es importante que cambie su contraseña en el primer acceso.</em></p>
<p>Saludos cordiales,<br><strong>Sistema de Admisión MTN</strong></p>
</body></html>',
 '["nombre", "email", "password_temporal", "url_login", "url_cambiar_password"]'::jsonb,
 'SYSTEM'),

('password_reset', 'email',
 'Contraseña reseteada - Sistema de Admisión MTN',
 'Estimado/a {{nombre}},

Su contraseña ha sido reseteada exitosamente.

Nueva contraseña temporal: {{password_temporal}}

Para acceder al sistema, visite: {{url_login}}
Fecha del reset: {{fecha_reset}}

Por favor, cambie su contraseña después de iniciar sesión.

Saludos cordiales,
Sistema de Admisión MTN',
 '<html><body style="font-family: Arial, sans-serif;">
<h2>Contraseña Reseteada</h2>
<p>Estimado/a <strong>{{nombre}}</strong>,</p>
<p>Su contraseña ha sido reseteada exitosamente.</p>
<div style="background-color: #fff3cd; padding: 15px; margin: 20px 0; border-radius: 5px; border-left: 5px solid #ffc107;">
<p><strong>Nueva contraseña temporal:</strong> <code>{{password_temporal}}</code></p>
</div>
<p><a href="{{url_login}}" style="background-color: #007bff; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;">Acceder al Sistema</a></p>
<p><small>Fecha del reset: {{fecha_reset}}</small></p>
<p><em>Por favor, cambie su contraseña después de iniciar sesión.</em></p>
</body></html>',
 '["nombre", "password_temporal", "url_login", "fecha_reset"]'::jsonb,
 'SYSTEM'),

('application_received', 'email',
 'Postulación recibida - {{nombre_estudiante}}',
 'Estimado apoderado,

Hemos recibido exitosamente la postulación para {{nombre_estudiante}}.

Número de seguimiento: {{numero_postulacion}}
Fecha de recepción: {{fecha_recepcion}}

Puede hacer seguimiento del estado de la postulación en: {{url_seguimiento}}

En los próximos días recibirá información sobre los siguientes pasos del proceso de admisión.

Saludos cordiales,
Colegio Monte Tabor y Nazaret',
 '<html><body style="font-family: Arial, sans-serif;">
<h2>Postulación Recibida</h2>
<p>Estimado apoderado,</p>
<p>Hemos recibido exitosamente la postulación para <strong>{{nombre_estudiante}}</strong>.</p>
<div style="background-color: #d4edda; padding: 15px; margin: 20px 0; border-radius: 5px; border-left: 5px solid #28a745;">
<p><strong>Número de seguimiento:</strong> {{numero_postulacion}}</p>
<p><strong>Fecha de recepción:</strong> {{fecha_recepcion}}</p>
</div>
<p><a href="{{url_seguimiento}}" style="background-color: #28a745; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;">Seguir Estado de Postulación</a></p>
<p>En los próximos días recibirá información sobre los siguientes pasos del proceso de admisión.</p>
<p>Saludos cordiales,<br><strong>Colegio Monte Tabor y Nazaret</strong></p>
</body></html>',
 '["nombre_estudiante", "numero_postulacion", "fecha_recepcion", "url_seguimiento"]'::jsonb,
 'SYSTEM');

-- Comentarios en las tablas
COMMENT ON TABLE templates IS 'Plantillas de notificaciones con soporte para variables Mustache/Handlebars';
COMMENT ON TABLE messages IS 'Mensajes de notificación enviados o por enviar';
COMMENT ON TABLE delivery_attempts IS 'Historial de intentos de entrega para cada mensaje';
COMMENT ON TABLE rate_limits IS 'Control de límites de tasa para prevenir spam';

COMMENT ON COLUMN templates.variables IS 'Array JSON de nombres de variables requeridas por la plantilla';
COMMENT ON COLUMN messages.to_json IS 'Destinos serializados - array para email, string para SMS';
COMMENT ON COLUMN messages.payload IS 'Evento original completo para auditoria y reprocessing';
COMMENT ON COLUMN delivery_attempts.provider_response IS 'Respuesta completa del proveedor para debugging';