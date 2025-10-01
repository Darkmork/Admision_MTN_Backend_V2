-- V2__outbox.sql
-- Tabla Outbox para patrón de eventos confiables

-- Tabla principal de outbox para eventos de dominio
CREATE TABLE outbox (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    aggregate_type TEXT NOT NULL,              -- Application
    aggregate_id TEXT NOT NULL,                -- UUID de la aplicación
    type TEXT NOT NULL,                        -- ApplicationSubmitted.v1, StateChanged.v1
    payload JSONB NOT NULL,                    -- evento completo serializado
    
    -- Idempotencia y control de duplicados
    idempotency_key TEXT,                      -- clave única para evitar duplicados
    
    -- Auditoría y timestamps
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    processed_at TIMESTAMPTZ,                 -- cuando se publicó exitosamente
    
    -- Estado del evento
    status TEXT DEFAULT 'PENDING',            -- PENDING, PROCESSED, FAILED, DEAD_LETTER
    attempts INTEGER DEFAULT 0,               -- número de intentos de publicación
    last_error TEXT,                          -- último error encontrado
    next_retry_at TIMESTAMPTZ,               -- próximo intento programado
    
    -- Metadatos adicionales
    event_version TEXT DEFAULT '1',           -- versión del esquema del evento
    correlation_id TEXT,                      -- ID de correlación para trazabilidad
    causation_id TEXT,                        -- ID del evento que causó este evento
    
    -- Context del evento
    source_service TEXT DEFAULT 'application-service',
    tenant_id TEXT,                           -- para multi-tenancy futuro
    
    -- Partitioning hint
    partition_key TEXT GENERATED ALWAYS AS (DATE_TRUNC('day', created_at)::TEXT) STORED
);

-- Índices para outbox
CREATE INDEX idx_outbox_status ON outbox(status);
CREATE INDEX idx_outbox_created_at ON outbox(created_at);
CREATE INDEX idx_outbox_aggregate_type_id ON outbox(aggregate_type, aggregate_id);
CREATE INDEX idx_outbox_type ON outbox(type);
CREATE INDEX idx_outbox_next_retry_at ON outbox(next_retry_at) WHERE next_retry_at IS NOT NULL;
CREATE INDEX idx_outbox_partition_key ON outbox(partition_key);

-- Índice único para idempotencia (solo para claves no nulas)
CREATE UNIQUE INDEX idx_outbox_idempotency_key 
    ON outbox(idempotency_key) 
    WHERE idempotency_key IS NOT NULL;

-- Índice compuesto para el dispatcher
CREATE INDEX idx_outbox_dispatcher 
    ON outbox(status, created_at) 
    WHERE status IN ('PENDING', 'FAILED');

-- Tabla de métricas de outbox (opcional, para observabilidad avanzada)
CREATE TABLE outbox_metrics (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    metric_name TEXT NOT NULL,                -- events_published, events_failed, avg_processing_time
    metric_value NUMERIC NOT NULL,
    metric_type TEXT NOT NULL,                -- COUNTER, GAUGE, HISTOGRAM
    labels JSONB,                             -- etiquetas adicionales
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    -- Agregación temporal
    time_bucket TEXT NOT NULL,                -- '1h', '1d', etc.
    bucket_start TIMESTAMPTZ NOT NULL,
    bucket_end TIMESTAMPTZ NOT NULL
);

-- Índices para métricas
CREATE INDEX idx_outbox_metrics_name_time ON outbox_metrics(metric_name, recorded_at);
CREATE INDEX idx_outbox_metrics_bucket ON outbox_metrics(time_bucket, bucket_start);

-- Vista para eventos pendientes de procesamiento
CREATE VIEW pending_outbox_events AS
SELECT 
    id,
    aggregate_type,
    aggregate_id,
    type,
    payload,
    created_at,
    attempts,
    last_error,
    next_retry_at,
    EXTRACT(EPOCH FROM (NOW() - created_at))/60 as age_minutes
FROM outbox
WHERE status = 'PENDING' 
   OR (status = 'FAILED' AND (next_retry_at IS NULL OR next_retry_at <= NOW()))
ORDER BY created_at ASC;

-- Vista para estadísticas de outbox
CREATE VIEW outbox_statistics AS
SELECT 
    type as event_type,
    status,
    COUNT(*) as count,
    COUNT(*) FILTER (WHERE created_at >= NOW() - INTERVAL '1 hour') as count_last_hour,
    COUNT(*) FILTER (WHERE created_at >= NOW() - INTERVAL '24 hours') as count_last_day,
    AVG(attempts) as avg_attempts,
    MAX(attempts) as max_attempts,
    AVG(EXTRACT(EPOCH FROM (processed_at - created_at))) FILTER (WHERE processed_at IS NOT NULL) as avg_processing_seconds
FROM outbox
GROUP BY type, status;

-- Función para limpiar eventos procesados antiguos
CREATE OR REPLACE FUNCTION cleanup_processed_outbox_events(retention_hours INT DEFAULT 168) -- 7 días
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
    cutoff_date TIMESTAMPTZ;
BEGIN
    cutoff_date := NOW() - (retention_hours || ' hours')::INTERVAL;
    
    -- Solo eliminar eventos procesados exitosamente
    DELETE FROM outbox 
    WHERE status = 'PROCESSED' 
    AND processed_at < cutoff_date;
    
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- Función para obtener próximos eventos a procesar
CREATE OR REPLACE FUNCTION get_next_outbox_batch(batch_size INT DEFAULT 10)
RETURNS SETOF outbox AS $$
BEGIN
    RETURN QUERY
    SELECT *
    FROM outbox
    WHERE (status = 'PENDING' 
           OR (status = 'FAILED' AND (next_retry_at IS NULL OR next_retry_at <= NOW())))
    AND attempts < 5  -- máximo 5 intentos
    ORDER BY created_at ASC
    LIMIT batch_size
    FOR UPDATE SKIP LOCKED;  -- evitar bloqueos
END;
$$ LANGUAGE plpgsql;

-- Función para marcar evento como procesado
CREATE OR REPLACE FUNCTION mark_outbox_event_processed(event_id UUID)
RETURNS BOOLEAN AS $$
BEGIN
    UPDATE outbox 
    SET status = 'PROCESSED', 
        processed_at = NOW()
    WHERE id = event_id 
    AND status IN ('PENDING', 'FAILED');
    
    RETURN FOUND;
END;
$$ LANGUAGE plpgsql;

-- Función para marcar evento como fallido con retry
CREATE OR REPLACE FUNCTION mark_outbox_event_failed(
    event_id UUID, 
    error_message TEXT,
    retry_delay_seconds INT DEFAULT 60
)
RETURNS BOOLEAN AS $$
BEGIN
    UPDATE outbox 
    SET status = 'FAILED',
        attempts = attempts + 1,
        last_error = error_message,
        next_retry_at = NOW() + (retry_delay_seconds || ' seconds')::INTERVAL
    WHERE id = event_id;
    
    -- Si excede el máximo de intentos, marcar como dead letter
    UPDATE outbox 
    SET status = 'DEAD_LETTER',
        next_retry_at = NULL
    WHERE id = event_id 
    AND attempts >= 5;
    
    RETURN FOUND;
END;
$$ LANGUAGE plpgsql;

-- Función para reintentar eventos fallidos manualmente
CREATE OR REPLACE FUNCTION retry_failed_outbox_events()
RETURNS INTEGER AS $$
DECLARE
    updated_count INTEGER;
BEGIN
    UPDATE outbox 
    SET status = 'PENDING',
        next_retry_at = NULL,
        last_error = NULL
    WHERE status = 'FAILED'
    AND attempts < 5
    AND (next_retry_at IS NULL OR next_retry_at <= NOW());
    
    GET DIAGNOSTICS updated_count = ROW_COUNT;
    
    RETURN updated_count;
END;
$$ LANGUAGE plpgsql;

-- Función para insertar evento con validaciones
CREATE OR REPLACE FUNCTION insert_outbox_event(
    p_aggregate_type TEXT,
    p_aggregate_id TEXT,
    p_type TEXT,
    p_payload JSONB,
    p_idempotency_key TEXT DEFAULT NULL,
    p_correlation_id TEXT DEFAULT NULL
)
RETURNS UUID AS $$
DECLARE
    event_id UUID;
BEGIN
    -- Validar que el payload sea JSON válido
    IF p_payload IS NULL OR p_payload = '{}'::JSONB THEN
        RAISE EXCEPTION 'Payload cannot be null or empty';
    END IF;
    
    -- Insertar evento
    INSERT INTO outbox (
        aggregate_type,
        aggregate_id,
        type,
        payload,
        idempotency_key,
        correlation_id
    ) VALUES (
        p_aggregate_type,
        p_aggregate_id,
        p_type,
        p_payload,
        p_idempotency_key,
        p_correlation_id
    ) RETURNING id INTO event_id;
    
    RETURN event_id;
END;
$$ LANGUAGE plpgsql;

-- Trigger para actualizar métricas cuando se procesa un evento
CREATE OR REPLACE FUNCTION update_outbox_metrics()
RETURNS TRIGGER AS $$
BEGIN
    -- Solo procesar cuando cambia a PROCESSED
    IF NEW.status = 'PROCESSED' AND OLD.status != 'PROCESSED' THEN
        -- Incrementar contador de eventos procesados
        INSERT INTO outbox_metrics (
            metric_name,
            metric_value,
            metric_type,
            labels,
            time_bucket,
            bucket_start,
            bucket_end
        ) VALUES (
            'events_processed_total',
            1,
            'COUNTER',
            jsonb_build_object('event_type', NEW.type, 'aggregate_type', NEW.aggregate_type),
            '1h',
            DATE_TRUNC('hour', NOW()),
            DATE_TRUNC('hour', NOW()) + INTERVAL '1 hour'
        ) ON CONFLICT DO NOTHING; -- evitar duplicados en caso de reprocessing
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER outbox_metrics_trigger
    AFTER UPDATE ON outbox
    FOR EACH ROW
    EXECUTE FUNCTION update_outbox_metrics();

-- Comentarios para documentación
COMMENT ON TABLE outbox IS 'Tabla Outbox para publicación confiable de eventos de dominio';
COMMENT ON COLUMN outbox.aggregate_type IS 'Tipo de agregado que generó el evento (Application)';
COMMENT ON COLUMN outbox.aggregate_id IS 'ID único del agregado específico';
COMMENT ON COLUMN outbox.type IS 'Tipo de evento: ApplicationSubmitted.v1, StateChanged.v1';
COMMENT ON COLUMN outbox.payload IS 'Evento completo serializado en JSON';
COMMENT ON COLUMN outbox.idempotency_key IS 'Clave única para evitar eventos duplicados';
COMMENT ON COLUMN outbox.status IS 'Estado del evento: PENDING, PROCESSED, FAILED, DEAD_LETTER';

COMMENT ON FUNCTION cleanup_processed_outbox_events(INT) IS 'Limpia eventos procesados más antiguos que el tiempo especificado';
COMMENT ON FUNCTION get_next_outbox_batch(INT) IS 'Obtiene el próximo lote de eventos para procesar con bloqueo optimista';
COMMENT ON FUNCTION mark_outbox_event_processed(UUID) IS 'Marca un evento como procesado exitosamente';
COMMENT ON FUNCTION mark_outbox_event_failed(UUID, TEXT, INT) IS 'Marca un evento como fallido y programa retry';

-- Datos iniciales para testing/desarrollo
-- (Se pueden eliminar en producción)

-- Ejemplo de evento ApplicationSubmitted
INSERT INTO outbox (
    aggregate_type,
    aggregate_id, 
    type,
    payload,
    idempotency_key,
    correlation_id
) VALUES (
    'Application',
    '550e8400-e29b-41d4-a716-446655440002',
    'ApplicationSubmitted.v1',
    '{
        "schema": "admissions.application.submitted.v1",
        "event_id": "' || uuid_generate_v4() || '",
        "application_id": "550e8400-e29b-41d4-a716-446655440002",
        "created_by": "apoderado-test-002",
        "status": "PENDING",
        "grade_applied": "PRIMERO_BASICO",
        "submitted_at": "' || NOW()::TEXT || '"
    }'::JSONB,
    'app-submitted-550e8400-e29b-41d4-a716-446655440002-' || EXTRACT(EPOCH FROM NOW())::TEXT,
    'test-correlation-' || uuid_generate_v4()
);

-- Ejemplo de evento StateChanged
INSERT INTO outbox (
    aggregate_type,
    aggregate_id,
    type, 
    payload,
    correlation_id
) VALUES (
    'Application',
    '550e8400-e29b-41d4-a716-446655440002',
    'StateChanged.v1',
    '{
        "schema": "admissions.application.state_changed.v1",
        "event_id": "' || uuid_generate_v4() || '",
        "application_id": "550e8400-e29b-41d4-a716-446655440002",
        "from_state": "DRAFT",
        "to_state": "PENDING",
        "reason_code": "FORM_SUBMITTED",
        "actor_user_id": "apoderado-test-002",
        "actor_role": "APODERADO",
        "comment": "Formulario enviado automáticamente",
        "occurred_at": "' || NOW()::TEXT || '"
    }'::JSONB,
    'test-correlation-' || uuid_generate_v4()
);