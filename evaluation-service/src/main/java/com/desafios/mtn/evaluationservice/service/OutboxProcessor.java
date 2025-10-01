package com.desafios.mtn.evaluationservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Procesador del Outbox Pattern - Publica eventos pendientes a RabbitMQ de forma confiable
 * Implementa polling de eventos con retry logic y manejo de errores
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxProcessor {

    private final JdbcTemplate jdbcTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Value("${outbox.processor.batch-size:50}")
    private int batchSize;

    @Value("${outbox.processor.enabled:true}")
    private boolean processorEnabled;

    @Value("${outbox.processor.exchange:evaluations.events}")
    private String defaultExchange;

    // ================================
    // SCHEDULED PROCESSING
    // ================================

    /**
     * Procesa eventos pendientes cada 5 segundos
     */
    @Scheduled(fixedDelay = 5000) // 5 segundos
    @Transactional
    public void processOutboxEvents() {
        if (!processorEnabled) {
            return;
        }

        try {
            List<OutboxEvent> pendingEvents = fetchPendingEvents();
            
            if (pendingEvents.isEmpty()) {
                log.debug("No pending outbox events to process");
                return;
            }

            log.info("Processing {} outbox events", pendingEvents.size());
            
            int successCount = 0;
            int failureCount = 0;

            for (OutboxEvent event : pendingEvents) {
                if (processEvent(event)) {
                    successCount++;
                } else {
                    failureCount++;
                }
            }

            log.info("Outbox processing completed: {} successful, {} failed", 
                    successCount, failureCount);

        } catch (Exception e) {
            log.error("Error during outbox processing", e);
        }
    }

    /**
     * Procesa eventos con retry que han fallado anteriormente
     */
    @Scheduled(fixedDelay = 60000) // 1 minuto
    @Transactional
    public void processRetryEvents() {
        if (!processorEnabled) {
            return;
        }

        try {
            List<OutboxEvent> retryEvents = fetchRetryEvents();
            
            if (retryEvents.isEmpty()) {
                log.debug("No retry outbox events to process");
                return;
            }

            log.info("Processing {} retry outbox events", retryEvents.size());
            
            for (OutboxEvent event : retryEvents) {
                processEvent(event);
            }

        } catch (Exception e) {
            log.error("Error during retry processing", e);
        }
    }

    /**
     * Limpieza de eventos procesados antiguos - cada hora
     */
    @Scheduled(fixedDelay = 3600000) // 1 hora
    @Transactional
    public void cleanupProcessedEvents() {
        if (!processorEnabled) {
            return;
        }

        try {
            int deletedCount = jdbcTemplate.update(
                "SELECT cleanup_processed_events(?)", 7); // 7 días de retención
            
            if (deletedCount > 0) {
                log.info("Cleaned up {} old processed events", deletedCount);
            }

        } catch (Exception e) {
            log.error("Error during cleanup", e);
        }
    }

    /**
     * Reset de eventos en estado de procesamiento estancados - cada 15 minutos
     */
    @Scheduled(fixedDelay = 900000) // 15 minutos
    @Transactional
    public void resetStaleProcessingEvents() {
        if (!processorEnabled) {
            return;
        }

        try {
            int resetCount = jdbcTemplate.queryForObject(
                "SELECT reset_stale_processing_events(?)", Integer.class, 15); // 15 minutos
            
            if (resetCount > 0) {
                log.warn("Reset {} stale processing events", resetCount);
            }

        } catch (Exception e) {
            log.error("Error during stale event reset", e);
        }
    }

    // ================================
    // CORE PROCESSING LOGIC
    // ================================

    /**
     * Procesa un evento individual
     */
    private boolean processEvent(OutboxEvent event) {
        try {
            log.debug("Processing outbox event: {} - {}", event.getId(), event.getEventType());

            // Marcar como en procesamiento
            markEventProcessing(event.getId());

            // Publicar a RabbitMQ
            publishToRabbitMQ(event);

            // Marcar como procesado exitosamente
            markEventProcessed(event.getId());

            log.debug("Successfully processed event: {}", event.getId());
            return true;

        } catch (Exception e) {
            log.error("Failed to process event {}: {}", event.getId(), e.getMessage(), e);
            
            // Marcar como fallido
            markEventFailed(event.getId(), e.getMessage(), getStackTrace(e));
            return false;
        }
    }

    /**
     * Publica evento a RabbitMQ
     */
    private void publishToRabbitMQ(OutboxEvent event) throws Exception {
        String exchange = event.getExchangeName() != null ? event.getExchangeName() : defaultExchange;
        String routingKey = event.getRoutingKey();

        // Deserializar payload
        Map<String, Object> payload = objectMapper.readValue(event.getPayload(), Map.class);

        // Agregar headers si existen
        if (event.getHeaders() != null) {
            Map<String, Object> headers = objectMapper.readValue(event.getHeaders(), Map.class);
            // Aquí se pueden agregar headers específicos de RabbitMQ si es necesario
        }

        // Publicar mensaje
        rabbitTemplate.convertAndSend(exchange, routingKey, payload);

        log.debug("Published message to exchange: {}, routing key: {}", exchange, routingKey);
    }

    // ================================
    // DATABASE OPERATIONS
    // ================================

    /**
     * Obtiene eventos pendientes para procesar
     */
    private List<OutboxEvent> fetchPendingEvents() {
        String sql = """
            SELECT id, aggregate_type, aggregate_id, event_type, event_version, payload,
                   routing_key, exchange_name, idempotency_key, correlation_id, causation_id,
                   retry_count, max_retries, created_at, scheduled_at, headers
            FROM outbox 
            WHERE processed = false 
              AND processing = false 
              AND scheduled_at <= NOW()
              AND retry_count < max_retries
            ORDER BY priority DESC, created_at ASC
            LIMIT ?
            """;

        return jdbcTemplate.query(sql, this::mapOutboxEvent, batchSize);
    }

    /**
     * Obtiene eventos para retry
     */
    private List<OutboxEvent> fetchRetryEvents() {
        String sql = """
            SELECT id, aggregate_type, aggregate_id, event_type, event_version, payload,
                   routing_key, exchange_name, idempotency_key, correlation_id, causation_id,
                   retry_count, max_retries, created_at, scheduled_at, headers
            FROM outbox 
            WHERE processed = false 
              AND processing = false 
              AND scheduled_at <= NOW()
              AND retry_count > 0 
              AND retry_count < max_retries
            ORDER BY scheduled_at ASC
            LIMIT ?
            """;

        return jdbcTemplate.query(sql, this::mapOutboxEvent, batchSize / 2);
    }

    /**
     * Marca un evento como en procesamiento
     */
    private void markEventProcessing(UUID eventId) {
        jdbcTemplate.update(
            "UPDATE outbox SET processing = true, last_retry_at = NOW() WHERE id = ?", 
            eventId);
    }

    /**
     * Marca un evento como procesado exitosamente
     */
    private void markEventProcessed(UUID eventId) {
        Boolean result = jdbcTemplate.queryForObject(
            "SELECT mark_event_processed(?)", Boolean.class, eventId);
        
        if (Boolean.TRUE.equals(result)) {
            log.debug("Event {} marked as processed", eventId);
        }
    }

    /**
     * Marca un evento como fallido
     */
    private void markEventFailed(UUID eventId, String error, String errorDetails) {
        Boolean result = jdbcTemplate.queryForObject(
            "SELECT mark_event_failed(?, ?, ?)", Boolean.class, eventId, error, errorDetails);
        
        if (Boolean.TRUE.equals(result)) {
            log.debug("Event {} marked as failed", eventId);
        }
    }

    /**
     * Mapea ResultSet a OutboxEvent
     */
    private OutboxEvent mapOutboxEvent(ResultSet rs, int rowNum) throws SQLException {
        return OutboxEvent.builder()
            .id(UUID.fromString(rs.getString("id")))
            .aggregateType(rs.getString("aggregate_type"))
            .aggregateId(UUID.fromString(rs.getString("aggregate_id")))
            .eventType(rs.getString("event_type"))
            .eventVersion(rs.getString("event_version"))
            .payload(rs.getString("payload"))
            .routingKey(rs.getString("routing_key"))
            .exchangeName(rs.getString("exchange_name"))
            .idempotencyKey(rs.getString("idempotency_key"))
            .correlationId(rs.getString("correlation_id") != null ? 
                          UUID.fromString(rs.getString("correlation_id")) : null)
            .causationId(rs.getString("causation_id") != null ? 
                        UUID.fromString(rs.getString("causation_id")) : null)
            .retryCount(rs.getInt("retry_count"))
            .maxRetries(rs.getInt("max_retries"))
            .createdAt(rs.getTimestamp("created_at").toInstant())
            .scheduledAt(rs.getTimestamp("scheduled_at").toInstant())
            .headers(rs.getString("headers"))
            .build();
    }

    // ================================
    // MONITORING AND MANAGEMENT
    // ================================

    /**
     * Obtiene estadísticas del outbox
     */
    public OutboxStatistics getOutboxStatistics() {
        String sql = """
            SELECT 
                COUNT(*) as total_events,
                COUNT(CASE WHEN processed = false THEN 1 END) as pending_events,
                COUNT(CASE WHEN processing = true THEN 1 END) as processing_events,
                COUNT(CASE WHEN processed = false AND retry_count >= max_retries THEN 1 END) as failed_events,
                COUNT(CASE WHEN processing = true AND last_retry_at < NOW() - INTERVAL '15 minutes' THEN 1 END) as stale_processing_events,
                MIN(created_at) as oldest_pending_event,
                MAX(processed_at) as last_processed_event
            FROM outbox
            """;

        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> 
            OutboxStatistics.builder()
                .totalEvents(rs.getLong("total_events"))
                .pendingEvents(rs.getLong("pending_events"))
                .processingEvents(rs.getLong("processing_events"))
                .failedEvents(rs.getLong("failed_events"))
                .staleProcessingEvents(rs.getLong("stale_processing_events"))
                .oldestPendingEvent(rs.getTimestamp("oldest_pending_event") != null ? 
                                   rs.getTimestamp("oldest_pending_event").toInstant() : null)
                .lastProcessedEvent(rs.getTimestamp("last_processed_event") != null ? 
                                   rs.getTimestamp("last_processed_event").toInstant() : null)
                .build());
    }

    /**
     * Fuerza el procesamiento inmediato de eventos pendientes
     */
    @Transactional
    public ProcessingResult forceProcessPendingEvents() {
        log.info("Force processing of pending outbox events triggered");
        
        List<OutboxEvent> pendingEvents = fetchPendingEvents();
        int successCount = 0;
        int failureCount = 0;

        for (OutboxEvent event : pendingEvents) {
            if (processEvent(event)) {
                successCount++;
            } else {
                failureCount++;
            }
        }

        return ProcessingResult.builder()
            .totalProcessed(pendingEvents.size())
            .successful(successCount)
            .failed(failureCount)
            .timestamp(Instant.now())
            .build();
    }

    // ================================
    // CONFIGURATION
    // ================================

    public void setProcessorEnabled(boolean enabled) {
        this.processorEnabled = enabled;
        log.info("Outbox processor {}", enabled ? "enabled" : "disabled");
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = Math.max(1, Math.min(batchSize, 100)); // Entre 1 y 100
        log.info("Outbox processor batch size set to: {}", this.batchSize);
    }

    // ================================
    // HELPER METHODS
    // ================================

    private String getStackTrace(Throwable t) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        t.printStackTrace(pw);
        return sw.toString();
    }

    // ================================
    // DATA TRANSFER OBJECTS
    // ================================

    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class OutboxEvent {
        private UUID id;
        private String aggregateType;
        private UUID aggregateId;
        private String eventType;
        private String eventVersion;
        private String payload;
        private String routingKey;
        private String exchangeName;
        private String idempotencyKey;
        private UUID correlationId;
        private UUID causationId;
        private Integer retryCount;
        private Integer maxRetries;
        private Instant createdAt;
        private Instant scheduledAt;
        private String headers;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class OutboxStatistics {
        private Long totalEvents;
        private Long pendingEvents;
        private Long processingEvents;
        private Long failedEvents;
        private Long staleProcessingEvents;
        private Instant oldestPendingEvent;
        private Instant lastProcessedEvent;
        private Double processingRate;
        private Double errorRate;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class ProcessingResult {
        private Integer totalProcessed;
        private Integer successful;
        private Integer failed;
        private Instant timestamp;
        private String message;
    }
}