-- V99__create_outbox.sql
-- Estructura base Outbox para user-service

CREATE TABLE IF NOT EXISTS outbox(
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    aggregate_type TEXT NOT NULL,
    aggregate_id TEXT NOT NULL,
    type TEXT NOT NULL,             -- EmailRequested.v1 | SmsRequested.v1
    payload JSONB NOT NULL,
    idempotency_key TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    processed_at TIMESTAMPTZ,
    
    -- Índices para performance
    CONSTRAINT unique_idempotency_key UNIQUE(idempotency_key)
);

-- Índices para consultas comunes
CREATE INDEX idx_outbox_processed ON outbox(processed_at) WHERE processed_at IS NULL;
CREATE INDEX idx_outbox_created_at ON outbox(created_at);
CREATE INDEX idx_outbox_type ON outbox(type);
CREATE INDEX idx_outbox_aggregate ON outbox(aggregate_type, aggregate_id);

-- Función para limpiar eventos procesados antiguos (> 7 días)
CREATE OR REPLACE FUNCTION cleanup_processed_outbox_events()
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    DELETE FROM outbox 
    WHERE processed_at IS NOT NULL 
    AND processed_at < NOW() - INTERVAL '7 days';
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

COMMENT ON TABLE outbox IS 'Patrón Outbox para eventos de notificaciones - garantiza entrega at-least-once';
COMMENT ON COLUMN outbox.type IS 'Tipo de evento: EmailRequested.v1 | SmsRequested.v1';
COMMENT ON COLUMN outbox.payload IS 'Payload completo del evento en formato JSON';
COMMENT ON COLUMN outbox.idempotency_key IS 'Clave de idempotencia para evitar duplicados';