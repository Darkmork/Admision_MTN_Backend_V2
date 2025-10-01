// user-service/src/main/java/com/desafios/mtn/userservice/event/OutboxEventProcessor.java

package com.desafios.mtn.userservice.event;

import com.desafios.mtn.userservice.repository.DomainEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Procesador de eventos Outbox que envía eventos a RabbitMQ
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "user-service.events.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxEventProcessor {

    private final DomainEventRepository domainEventRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    
    private static final String USER_EVENTS_EXCHANGE = "user-events";
    private static final int BATCH_SIZE = 50;
    private static final int MAX_RETRIES = 3;

    /**
     * Procesa eventos pendientes cada 5 segundos
     */
    @Scheduled(fixedDelayString = "${user-service.events.processing-interval:5000}")
    @Transactional
    public void processOutboxEvents() {
        log.debug("Iniciando procesamiento de eventos Outbox");
        
        try {
            Pageable pageable = PageRequest.of(0, BATCH_SIZE);
            List<DomainEvent> unprocessedEvents = domainEventRepository
                    .findUnprocessedEvents(pageable)
                    .getContent();
            
            if (unprocessedEvents.isEmpty()) {
                log.debug("No hay eventos pendientes para procesar");
                return;
            }
            
            log.info("Procesando {} eventos pendientes", unprocessedEvents.size());
            
            int successCount = 0;
            int failureCount = 0;
            
            for (DomainEvent event : unprocessedEvents) {
                try {
                    boolean success = processEvent(event);
                    if (success) {
                        event.markAsProcessed();
                        domainEventRepository.save(event);
                        successCount++;
                        log.debug("Evento procesado exitosamente: {}", event.getId());
                    } else {
                        failureCount++;
                        log.warn("Falló el procesamiento del evento: {}", event.getId());
                    }
                } catch (Exception e) {
                    failureCount++;
                    log.error("Error procesando evento {}: {}", event.getId(), e.getMessage(), e);
                    
                    // Crear evento de reintento si no se ha superado el máximo
                    if (shouldCreateRetryEvent(event)) {
                        createRetryEvent(event, e.getMessage());
                    }
                }
            }
            
            log.info("Procesamiento completado - Exitosos: {}, Fallidos: {}", successCount, failureCount);
            
        } catch (Exception e) {
            log.error("Error en el procesamiento batch de eventos Outbox", e);
        }
    }

    /**
     * Procesa un evento individual enviándolo a RabbitMQ
     */
    private boolean processEvent(DomainEvent event) {
        try {
            String routingKey = determineRoutingKey(event);
            Message message = createRabbitMessage(event);
            
            log.debug("Enviando evento {} a RabbitMQ con routing key: {}", event.getId(), routingKey);
            
            rabbitTemplate.send(USER_EVENTS_EXCHANGE, routingKey, message);
            
            log.debug("Evento {} enviado exitosamente a RabbitMQ", event.getId());
            return true;
            
        } catch (Exception e) {
            log.error("Error enviando evento {} a RabbitMQ: {}", event.getId(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * Determina la routing key basada en el tipo de evento
     */
    private String determineRoutingKey(DomainEvent event) {
        String aggregateType = event.getAggregateType().toLowerCase();
        String eventType = event.getEventType().toLowerCase();
        return String.format("%s.%s", aggregateType, eventType);
    }

    /**
     * Crea un mensaje de RabbitMQ a partir de un evento de dominio
     */
    private Message createRabbitMessage(DomainEvent event) throws Exception {
        // Crear el payload del mensaje
        EventMessage eventMessage = EventMessage.builder()
                .eventId(event.getId())
                .aggregateType(event.getAggregateType())
                .aggregateId(event.getAggregateId())
                .eventType(event.getEventType())
                .eventData(event.getEventData())
                .eventVersion(event.getEventVersion())
                .occurredAt(event.getOccurredAt())
                .correlationId(event.getCorrelationId())
                .causationId(event.getCausationId())
                .userId(event.getUserId())
                .build();
        
        String messageBody = objectMapper.writeValueAsString(eventMessage);
        
        return MessageBuilder
                .withBody(messageBody.getBytes())
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setHeader("eventId", event.getId().toString())
                .setHeader("eventType", event.getEventType())
                .setHeader("aggregateType", event.getAggregateType())
                .setHeader("aggregateId", event.getAggregateId().toString())
                .setHeader("correlationId", event.getCorrelationId() != null ? event.getCorrelationId().toString() : null)
                .setHeader("userId", event.getUserId() != null ? event.getUserId().toString() : null)
                .setTimestamp(java.util.Date.from(event.getOccurredAt()))
                .build();
    }

    /**
     * Verifica si se debe crear un evento de reintento
     */
    private boolean shouldCreateRetryEvent(DomainEvent event) {
        // No crear reintentos para eventos que ya son reintentos múltiples
        if (event.getEventType().endsWith("_RETRY")) {
            int retryCount = countRetryLevel(event.getEventType());
            return retryCount < MAX_RETRIES;
        }
        return true; // Crear primer reintento para eventos originales
    }

    /**
     * Cuenta el nivel de reintento basado en el nombre del evento
     */
    private int countRetryLevel(String eventType) {
        if (!eventType.contains("_RETRY")) {
            return 0;
        }
        // Contar ocurrencias de "_RETRY" para determinar el nivel
        return (eventType.length() - eventType.replace("_RETRY", "").length()) / "_RETRY".length();
    }

    /**
     * Crea un evento de reintento
     */
    private void createRetryEvent(DomainEvent originalEvent, String errorMessage) {
        try {
            DomainEvent retryEvent = originalEvent.createRetryEvent();
            
            // Agregar información del error al evento data
            retryEvent.getEventData().put("retryReason", errorMessage);
            retryEvent.getEventData().put("originalEventId", originalEvent.getId().toString());
            retryEvent.getEventData().put("retryAttempt", countRetryLevel(retryEvent.getEventType()));
            
            domainEventRepository.save(retryEvent);
            
            log.info("Evento de reintento creado: {} para evento original: {}", 
                    retryEvent.getId(), originalEvent.getId());
            
        } catch (Exception e) {
            log.error("Error creando evento de reintento para evento {}: {}", 
                    originalEvent.getId(), e.getMessage(), e);
        }
    }

    /**
     * Limpia eventos procesados antiguos
     */
    @Scheduled(cron = "${user-service.events.cleanup-cron:0 0 2 * * ?}") // 2 AM diario
    @Transactional
    public void cleanupOldProcessedEvents() {
        log.info("Iniciando limpieza de eventos procesados antiguos");
        
        try {
            // Eliminar eventos procesados más antiguos que 30 días
            Instant thirtyDaysAgo = Instant.now().minusSeconds(30 * 24 * 60 * 60);
            int deletedCount = domainEventRepository.deleteOldProcessedEvents(thirtyDaysAgo);
            
            log.info("Eliminados {} eventos procesados antiguos", deletedCount);
            
        } catch (Exception e) {
            log.error("Error durante la limpieza de eventos antiguos", e);
        }
    }

    /**
     * Limpia eventos expirados no procesados
     */
    @Scheduled(cron = "${user-service.events.expired-cleanup-cron:0 30 2 * * ?}") // 2:30 AM diario
    @Transactional
    public void cleanupExpiredEvents() {
        log.info("Iniciando limpieza de eventos expirados");
        
        try {
            // Eliminar eventos no procesados más antiguos que 7 días
            Instant sevenDaysAgo = Instant.now().minusSeconds(7 * 24 * 60 * 60);
            int deletedCount = domainEventRepository.deleteExpiredEvents(sevenDaysAgo);
            
            log.info("Eliminados {} eventos expirados no procesados", deletedCount);
            
        } catch (Exception e) {
            log.error("Error durante la limpieza de eventos expirados", e);
        }
    }

    /**
     * Obtiene estadísticas del procesador
     */
    public OutboxProcessorStats getProcessorStats() {
        long unprocessedEvents = domainEventRepository.countUnprocessedEvents();
        long processedLastHour = domainEventRepository.countProcessedEventsSince(Instant.now().minusSeconds(3600));
        
        // Obtener métricas de rendimiento
        Object[] performanceMetrics = domainEventRepository.getProcessingPerformanceMetrics();
        
        return OutboxProcessorStats.builder()
                .unprocessedEvents(unprocessedEvents)
                .processedLastHour(processedLastHour)
                .averageProcessingTimeSeconds(performanceMetrics != null && performanceMetrics[0] != null ? 
                        ((Number) performanceMetrics[0]).doubleValue() : 0.0)
                .build();
    }

    /**
     * Mensaje de evento para enviar a RabbitMQ
     */
    private static class EventMessage {
        public final UUID eventId;
        public final String aggregateType;
        public final UUID aggregateId;
        public final String eventType;
        public final Object eventData;
        public final Integer eventVersion;
        public final Instant occurredAt;
        public final UUID correlationId;
        public final UUID causationId;
        public final UUID userId;
        
        private EventMessage(EventMessageBuilder builder) {
            this.eventId = builder.eventId;
            this.aggregateType = builder.aggregateType;
            this.aggregateId = builder.aggregateId;
            this.eventType = builder.eventType;
            this.eventData = builder.eventData;
            this.eventVersion = builder.eventVersion;
            this.occurredAt = builder.occurredAt;
            this.correlationId = builder.correlationId;
            this.causationId = builder.causationId;
            this.userId = builder.userId;
        }
        
        public static EventMessageBuilder builder() {
            return new EventMessageBuilder();
        }
        
        public static class EventMessageBuilder {
            private UUID eventId;
            private String aggregateType;
            private UUID aggregateId;
            private String eventType;
            private Object eventData;
            private Integer eventVersion;
            private Instant occurredAt;
            private UUID correlationId;
            private UUID causationId;
            private UUID userId;
            
            public EventMessageBuilder eventId(UUID eventId) { this.eventId = eventId; return this; }
            public EventMessageBuilder aggregateType(String aggregateType) { this.aggregateType = aggregateType; return this; }
            public EventMessageBuilder aggregateId(UUID aggregateId) { this.aggregateId = aggregateId; return this; }
            public EventMessageBuilder eventType(String eventType) { this.eventType = eventType; return this; }
            public EventMessageBuilder eventData(Object eventData) { this.eventData = eventData; return this; }
            public EventMessageBuilder eventVersion(Integer eventVersion) { this.eventVersion = eventVersion; return this; }
            public EventMessageBuilder occurredAt(Instant occurredAt) { this.occurredAt = occurredAt; return this; }
            public EventMessageBuilder correlationId(UUID correlationId) { this.correlationId = correlationId; return this; }
            public EventMessageBuilder causationId(UUID causationId) { this.causationId = causationId; return this; }
            public EventMessageBuilder userId(UUID userId) { this.userId = userId; return this; }
            
            public EventMessage build() { return new EventMessage(this); }
        }
    }

    /**
     * Estadísticas del procesador Outbox
     */
    public static class OutboxProcessorStats {
        public final long unprocessedEvents;
        public final long processedLastHour;
        public final double averageProcessingTimeSeconds;
        
        private OutboxProcessorStats(OutboxProcessorStatsBuilder builder) {
            this.unprocessedEvents = builder.unprocessedEvents;
            this.processedLastHour = builder.processedLastHour;
            this.averageProcessingTimeSeconds = builder.averageProcessingTimeSeconds;
        }
        
        public static OutboxProcessorStatsBuilder builder() {
            return new OutboxProcessorStatsBuilder();
        }
        
        public static class OutboxProcessorStatsBuilder {
            private long unprocessedEvents;
            private long processedLastHour;
            private double averageProcessingTimeSeconds;
            
            public OutboxProcessorStatsBuilder unprocessedEvents(long unprocessedEvents) {
                this.unprocessedEvents = unprocessedEvents;
                return this;
            }
            
            public OutboxProcessorStatsBuilder processedLastHour(long processedLastHour) {
                this.processedLastHour = processedLastHour;
                return this;
            }
            
            public OutboxProcessorStatsBuilder averageProcessingTimeSeconds(double averageProcessingTimeSeconds) {
                this.averageProcessingTimeSeconds = averageProcessingTimeSeconds;
                return this;
            }
            
            public OutboxProcessorStats build() {
                return new OutboxProcessorStats(this);
            }
        }
    }
}