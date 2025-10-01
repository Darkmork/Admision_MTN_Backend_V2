-- ============================================================================
-- Outbox Pattern Implementation for Evaluation Service
-- Garantiza publicación confiable de eventos de dominio
-- ============================================================================

CREATE TABLE outbox (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    aggregate_type TEXT NOT NULL,              -- Evaluation, Interview, Score
    aggregate_id UUID NOT NULL,                -- ID de la entidad que genera el evento
    
    -- Event metadata
    event_type TEXT NOT NULL,                  -- EvaluationAssigned.v1, InterviewScheduled.v1, etc.
    event_version TEXT NOT NULL DEFAULT '1.0',
    payload JSONB NOT NULL,                    -- Datos del evento
    
    -- Routing information
    routing_key TEXT,                          -- Clave de routing para RabbitMQ
    exchange_name TEXT DEFAULT 'evaluations.events',
    
    -- Idempotency and deduplication
    idempotency_key TEXT,                      -- Clave única para prevenir duplicados
    correlation_id UUID,                       -- ID de correlación para trazas
    causation_id UUID,                         -- ID del evento que causó este evento
    
    -- Processing control
    processed BOOLEAN NOT NULL DEFAULT FALSE,
    processing BOOLEAN NOT NULL DEFAULT FALSE, -- Flag para evitar procesamiento concurrente
    retry_count INTEGER NOT NULL DEFAULT 0,
    max_retries INTEGER NOT NULL DEFAULT 5,
    
    -- Timing information
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    scheduled_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), -- Cuándo debe procesarse
    processed_at TIMESTAMPTZ,
    last_retry_at TIMESTAMPTZ,
    
    -- Error handling
    last_error TEXT,
    error_details TEXT,
    
    -- Priority handling
    priority INTEGER NOT NULL DEFAULT 0,      -- 0=normal, 1=high, 2=critical
    
    -- Headers adicionales para el mensaje
    headers JSONB,
    
    -- Constraint para evitar duplicados por idempotency key
    CONSTRAINT unique_idempotency_key UNIQUE(idempotency_key)
);

-- ============================================================================
-- INDEXES FOR OUTBOX TABLE
-- ============================================================================

-- Primary indexes for processing
CREATE INDEX idx_outbox_processing ON outbox(processed, processing, scheduled_at) 
    WHERE processed = FALSE AND processing = FALSE;

CREATE INDEX idx_outbox_retry ON outbox(processed, retry_count, scheduled_at) 
    WHERE processed = FALSE AND retry_count < max_retries;

CREATE INDEX idx_outbox_failed ON outbox(processed, retry_count) 
    WHERE processed = FALSE AND retry_count >= max_retries;

-- Indexes for monitoring and cleanup
CREATE INDEX idx_outbox_created_at ON outbox(created_at);
CREATE INDEX idx_outbox_processed_at ON outbox(processed_at) WHERE processed = TRUE;
CREATE INDEX idx_outbox_aggregate ON outbox(aggregate_type, aggregate_id);
CREATE INDEX idx_outbox_event_type ON outbox(event_type);
CREATE INDEX idx_outbox_priority ON outbox(priority, created_at) WHERE processed = FALSE;

-- Index for correlation and causation tracking
CREATE INDEX idx_outbox_correlation ON outbox(correlation_id) WHERE correlation_id IS NOT NULL;
CREATE INDEX idx_outbox_causation ON outbox(causation_id) WHERE causation_id IS NOT NULL;

-- ============================================================================
-- FUNCTIONS FOR OUTBOX MANAGEMENT
-- ============================================================================

-- Function to publish an event to the outbox
CREATE OR REPLACE FUNCTION publish_event(
    p_aggregate_type TEXT,
    p_aggregate_id UUID,
    p_event_type TEXT,
    p_payload JSONB,
    p_routing_key TEXT DEFAULT NULL,
    p_idempotency_key TEXT DEFAULT NULL,
    p_correlation_id UUID DEFAULT NULL,
    p_causation_id UUID DEFAULT NULL,
    p_priority INTEGER DEFAULT 0,
    p_headers JSONB DEFAULT NULL
) RETURNS UUID AS $$
DECLARE
    event_id UUID;
BEGIN
    INSERT INTO outbox (
        aggregate_type,
        aggregate_id,
        event_type,
        payload,
        routing_key,
        idempotency_key,
        correlation_id,
        causation_id,
        priority,
        headers
    ) VALUES (
        p_aggregate_type,
        p_aggregate_id,
        p_event_type,
        p_payload,
        COALESCE(p_routing_key, LOWER(REPLACE(p_event_type, '.', '_'))),
        p_idempotency_key,
        p_correlation_id,
        p_causation_id,
        p_priority,
        p_headers
    ) RETURNING id INTO event_id;
    
    RETURN event_id;
END;
$$ LANGUAGE plpgsql;

-- Function to mark event as processed
CREATE OR REPLACE FUNCTION mark_event_processed(p_event_id UUID) RETURNS BOOLEAN AS $$
BEGIN
    UPDATE outbox 
    SET processed = TRUE,
        processing = FALSE,
        processed_at = NOW(),
        last_error = NULL,
        error_details = NULL
    WHERE id = p_event_id;
    
    RETURN FOUND;
END;
$$ LANGUAGE plpgsql;

-- Function to mark event as failed
CREATE OR REPLACE FUNCTION mark_event_failed(
    p_event_id UUID,
    p_error TEXT,
    p_error_details TEXT DEFAULT NULL
) RETURNS BOOLEAN AS $$
DECLARE
    current_retry_count INTEGER;
    next_scheduled_at TIMESTAMPTZ;
BEGIN
    -- Get current retry count and calculate next retry time
    SELECT retry_count INTO current_retry_count
    FROM outbox WHERE id = p_event_id;
    
    -- Exponential backoff: 2^retry_count minutes
    next_scheduled_at := NOW() + (POWER(2, current_retry_count) || ' minutes')::INTERVAL;
    
    UPDATE outbox 
    SET processing = FALSE,
        retry_count = retry_count + 1,
        last_retry_at = NOW(),
        last_error = p_error,
        error_details = p_error_details,
        scheduled_at = CASE 
            WHEN retry_count + 1 < max_retries THEN next_scheduled_at
            ELSE scheduled_at 
        END
    WHERE id = p_event_id;
    
    RETURN FOUND;
END;
$$ LANGUAGE plpgsql;

-- Function to reset stale processing events
CREATE OR REPLACE FUNCTION reset_stale_processing_events(p_max_processing_minutes INTEGER DEFAULT 15) RETURNS INTEGER AS $$
DECLARE
    reset_count INTEGER;
BEGIN
    UPDATE outbox 
    SET processing = FALSE,
        last_error = 'Processing timeout - reset by cleanup job'
    WHERE processing = TRUE 
      AND last_retry_at < NOW() - (p_max_processing_minutes || ' minutes')::INTERVAL;
    
    GET DIAGNOSTICS reset_count = ROW_COUNT;
    RETURN reset_count;
END;
$$ LANGUAGE plpgsql;

-- Function to cleanup old processed events
CREATE OR REPLACE FUNCTION cleanup_processed_events(p_retention_days INTEGER DEFAULT 7) RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    DELETE FROM outbox 
    WHERE processed = TRUE 
      AND processed_at < NOW() - (p_retention_days || ' days')::INTERVAL;
    
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- SAMPLE EVENT TEMPLATES
-- ============================================================================

-- Function to publish evaluation assigned event
CREATE OR REPLACE FUNCTION publish_evaluation_assigned_event(
    p_evaluation_id UUID,
    p_application_id UUID,
    p_evaluator_id TEXT,
    p_subject TEXT,
    p_level TEXT,
    p_assigned_at TIMESTAMPTZ,
    p_correlation_id UUID DEFAULT NULL
) RETURNS UUID AS $$
DECLARE
    event_payload JSONB;
    event_id UUID;
BEGIN
    event_payload := jsonb_build_object(
        'evaluation_id', p_evaluation_id,
        'application_id', p_application_id,
        'evaluator_id', p_evaluator_id,
        'subject', p_subject,
        'level', p_level,
        'assigned_at', p_assigned_at,
        'timestamp', NOW()
    );
    
    SELECT publish_event(
        'Evaluation',
        p_evaluation_id,
        'EvaluationAssigned.v1',
        event_payload,
        'evaluation.assigned',
        'eval-assigned-' || p_evaluation_id::text,
        p_correlation_id,
        NULL,
        1 -- High priority
    ) INTO event_id;
    
    RETURN event_id;
END;
$$ LANGUAGE plpgsql;

-- Function to publish evaluation completed event
CREATE OR REPLACE FUNCTION publish_evaluation_completed_event(
    p_evaluation_id UUID,
    p_application_id UUID,
    p_evaluator_id TEXT,
    p_subject TEXT,
    p_total_score DECIMAL,
    p_percentage DECIMAL,
    p_passed BOOLEAN,
    p_completed_at TIMESTAMPTZ,
    p_correlation_id UUID DEFAULT NULL
) RETURNS UUID AS $$
DECLARE
    event_payload JSONB;
    event_id UUID;
BEGIN
    event_payload := jsonb_build_object(
        'evaluation_id', p_evaluation_id,
        'application_id', p_application_id,
        'evaluator_id', p_evaluator_id,
        'subject', p_subject,
        'total_score', p_total_score,
        'percentage', p_percentage,
        'passed', p_passed,
        'completed_at', p_completed_at,
        'timestamp', NOW()
    );
    
    SELECT publish_event(
        'Evaluation',
        p_evaluation_id,
        'EvaluationCompleted.v1',
        event_payload,
        'evaluation.completed',
        'eval-completed-' || p_evaluation_id::text,
        p_correlation_id,
        NULL,
        2 -- Critical priority
    ) INTO event_id;
    
    RETURN event_id;
END;
$$ LANGUAGE plpgsql;

-- Function to publish interview scheduled event
CREATE OR REPLACE FUNCTION publish_interview_scheduled_event(
    p_interview_id UUID,
    p_application_id UUID,
    p_interviewer_id TEXT,
    p_scheduled_at TIMESTAMPTZ,
    p_duration_minutes INTEGER,
    p_location TEXT,
    p_correlation_id UUID DEFAULT NULL
) RETURNS UUID AS $$
DECLARE
    event_payload JSONB;
    event_id UUID;
BEGIN
    event_payload := jsonb_build_object(
        'interview_id', p_interview_id,
        'application_id', p_application_id,
        'interviewer_id', p_interviewer_id,
        'scheduled_at', p_scheduled_at,
        'duration_minutes', p_duration_minutes,
        'location', p_location,
        'timestamp', NOW()
    );
    
    SELECT publish_event(
        'Interview',
        p_interview_id,
        'InterviewScheduled.v1',
        event_payload,
        'interview.scheduled',
        'interview-scheduled-' || p_interview_id::text,
        p_correlation_id,
        NULL,
        1 -- High priority
    ) INTO event_id;
    
    RETURN event_id;
END;
$$ LANGUAGE plpgsql;

-- Function to publish evaluations completed (all for application) event
CREATE OR REPLACE FUNCTION publish_evaluations_completed_event(
    p_application_id UUID,
    p_evaluation_ids UUID[],
    p_overall_passed BOOLEAN,
    p_correlation_id UUID DEFAULT NULL
) RETURNS UUID AS $$
DECLARE
    event_payload JSONB;
    event_id UUID;
BEGIN
    event_payload := jsonb_build_object(
        'application_id', p_application_id,
        'evaluation_ids', p_evaluation_ids,
        'overall_passed', p_overall_passed,
        'completed_count', array_length(p_evaluation_ids, 1),
        'timestamp', NOW()
    );
    
    SELECT publish_event(
        'Application',
        p_application_id,
        'EvaluationsCompleted.v1',
        event_payload,
        'evaluations.completed',
        'evals-completed-' || p_application_id::text,
        p_correlation_id,
        NULL,
        2 -- Critical priority for saga coordination
    ) INTO event_id;
    
    RETURN event_id;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- VIEWS FOR MONITORING
-- ============================================================================

-- View for outbox health monitoring
CREATE VIEW outbox_health AS
SELECT 
    COUNT(*) as total_events,
    COUNT(CASE WHEN processed = FALSE THEN 1 END) as pending_events,
    COUNT(CASE WHEN processing = TRUE THEN 1 END) as processing_events,
    COUNT(CASE WHEN processed = FALSE AND retry_count >= max_retries THEN 1 END) as failed_events,
    COUNT(CASE WHEN processing = TRUE AND last_retry_at < NOW() - INTERVAL '15 minutes' THEN 1 END) as stale_processing_events,
    MIN(created_at) as oldest_pending_event,
    MAX(processed_at) as last_processed_event
FROM outbox;

-- View for event statistics by type
CREATE VIEW outbox_event_stats AS
SELECT 
    event_type,
    COUNT(*) as total_count,
    COUNT(CASE WHEN processed = TRUE THEN 1 END) as processed_count,
    COUNT(CASE WHEN processed = FALSE THEN 1 END) as pending_count,
    COUNT(CASE WHEN retry_count > 0 THEN 1 END) as retry_count,
    AVG(EXTRACT(EPOCH FROM (processed_at - created_at))) as avg_processing_seconds,
    MAX(retry_count) as max_retries_used
FROM outbox
GROUP BY event_type
ORDER BY total_count DESC;

-- View for failed events analysis
CREATE VIEW outbox_failed_events AS
SELECT 
    id,
    aggregate_type,
    aggregate_id,
    event_type,
    retry_count,
    max_retries,
    last_error,
    created_at,
    last_retry_at,
    scheduled_at
FROM outbox
WHERE processed = FALSE AND retry_count >= max_retries
ORDER BY created_at DESC;

-- ============================================================================
-- SCHEDULED MAINTENANCE FUNCTIONS
-- ============================================================================

-- Function to run regular outbox maintenance
CREATE OR REPLACE FUNCTION outbox_maintenance() RETURNS TEXT AS $$
DECLARE
    reset_count INTEGER;
    cleanup_count INTEGER;
    result TEXT;
BEGIN
    -- Reset stale processing events
    SELECT reset_stale_processing_events(15) INTO reset_count;
    
    -- Cleanup old processed events (older than 7 days)
    SELECT cleanup_processed_events(7) INTO cleanup_count;
    
    result := format('Outbox maintenance completed: %s stale events reset, %s old events cleaned up', 
                     reset_count, cleanup_count);
    
    RETURN result;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- COMMENTS FOR DOCUMENTATION
-- ============================================================================

COMMENT ON TABLE outbox IS 'Tabla Outbox para publicación confiable de eventos de dominio';
COMMENT ON COLUMN outbox.aggregate_type IS 'Tipo de entidad que genera el evento (Evaluation, Interview, etc.)';
COMMENT ON COLUMN outbox.aggregate_id IS 'ID de la entidad específica';
COMMENT ON COLUMN outbox.event_type IS 'Tipo de evento con versión (ej: EvaluationCompleted.v1)';
COMMENT ON COLUMN outbox.idempotency_key IS 'Clave única para prevenir eventos duplicados';
COMMENT ON COLUMN outbox.correlation_id IS 'ID para correlacionar eventos relacionados';
COMMENT ON COLUMN outbox.causation_id IS 'ID del evento que causó este evento';
COMMENT ON COLUMN outbox.processing IS 'Flag para evitar procesamiento concurrente del mismo evento';
COMMENT ON COLUMN outbox.priority IS 'Prioridad del evento: 0=normal, 1=alto, 2=crítico';

COMMENT ON FUNCTION publish_event IS 'Función para publicar eventos al outbox con todos los metadatos';
COMMENT ON FUNCTION mark_event_processed IS 'Marca un evento como procesado exitosamente';
COMMENT ON FUNCTION mark_event_failed IS 'Marca un evento como fallido e incrementa retry con backoff exponencial';
COMMENT ON FUNCTION reset_stale_processing_events IS 'Resetea eventos que quedaron en estado "processing" por mucho tiempo';
COMMENT ON FUNCTION cleanup_processed_events IS 'Elimina eventos procesados antiguos para mantener tamaño de tabla';

COMMENT ON VIEW outbox_health IS 'Vista para monitoreo de salud del sistema outbox';
COMMENT ON VIEW outbox_event_stats IS 'Estadísticas de eventos por tipo';
COMMENT ON VIEW outbox_failed_events IS 'Eventos que fallaron permanentemente para revisión manual';