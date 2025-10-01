// src/main/java/com/desafios/mtn/userservice/outbox/OutboxService.java

package com.desafios.mtn.userservice.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    /**
     * Publica un evento de email solicitado
     */
    @Transactional
    public void publishEmailRequested(EmailRequestedEvent event) {
        log.debug("Publicando EmailRequested para: {}", maskEmail(event.getTo().get(0)));
        
        // Verificar idempotencia
        if (isDuplicate(event.getIdempotencyKey())) {
            log.info("Evento duplicado detectado y omitido: {}", event.getIdempotencyKey());
            return;
        }
        
        try {
            Map<String, Object> payload = convertToMap(event);
            
            OutboxEntity outboxEvent = OutboxEntity.builder()
                    .aggregateType(event.getAggregateType())
                    .aggregateId(event.getAggregateId())
                    .type(OutboxEntity.EventTypes.EMAIL_REQUESTED_V1)
                    .payload(payload)
                    .idempotencyKey(event.getIdempotencyKey())
                    .build();
            
            outboxRepository.save(outboxEvent);
            
            log.info("EmailRequested publicado exitosamente - ID: {} para: {}", 
                    outboxEvent.getId(), maskEmail(event.getTo().get(0)));
            
        } catch (Exception e) {
            log.error("Error publicando EmailRequested: {}", e.getMessage(), e);
            throw new OutboxException("Error publicando evento de email", e);
        }
    }

    /**
     * Publica un evento de SMS solicitado
     */
    @Transactional
    public void publishSmsRequested(SmsRequestedEvent event) {
        log.debug("Publicando SmsRequested para: {}", maskPhone(event.getTo()));
        
        // Verificar idempotencia
        if (isDuplicate(event.getIdempotencyKey())) {
            log.info("Evento duplicado detectado y omitido: {}", event.getIdempotencyKey());
            return;
        }
        
        try {
            Map<String, Object> payload = convertToMap(event);
            
            OutboxEntity outboxEvent = OutboxEntity.builder()
                    .aggregateType(event.getAggregateType())
                    .aggregateId(event.getAggregateId())
                    .type(OutboxEntity.EventTypes.SMS_REQUESTED_V1)
                    .payload(payload)
                    .idempotencyKey(event.getIdempotencyKey())
                    .build();
            
            outboxRepository.save(outboxEvent);
            
            log.info("SmsRequested publicado exitosamente - ID: {} para: {}", 
                    outboxEvent.getId(), maskPhone(event.getTo()));
            
        } catch (Exception e) {
            log.error("Error publicando SmsRequested: {}", e.getMessage(), e);
            throw new OutboxException("Error publicando evento de SMS", e);
        }
    }

    /**
     * Obtiene eventos no procesados para el dispatcher
     */
    public List<OutboxEntity> getUnprocessedEvents(int batchSize) {
        return outboxRepository.findUnprocessedEventsOrderByCreated()
                .stream()
                .limit(batchSize)
                .toList();
    }

    /**
     * Marca eventos como procesados
     */
    @Transactional
    public void markEventsAsProcessed(List<UUID> eventIds) {
        Instant now = Instant.now();
        int updatedCount = outboxRepository.markEventsAsProcessed(eventIds, now);
        log.debug("Marcados {} eventos como procesados", updatedCount);
    }

    /**
     * Limpia eventos procesados antiguos
     */
    @Transactional
    public int cleanupOldProcessedEvents() {
        Instant sevenDaysAgo = Instant.now().minusSeconds(7 * 24 * 60 * 60);
        int deletedCount = outboxRepository.deleteOldProcessedEvents(sevenDaysAgo);
        log.info("Eliminados {} eventos procesados antiguos", deletedCount);
        return deletedCount;
    }

    /**
     * Obtiene estadísticas del outbox
     */
    public OutboxStatistics getStatistics() {
        long unprocessedCount = outboxRepository.countUnprocessedEvents();
        long processedLastHour = outboxRepository.countProcessedEventsSince(
                Instant.now().minusSeconds(3600));
        
        List<Object[]> typeStats = outboxRepository.getEventTypeStatistics();
        Map<String, Long> eventsByType = new HashMap<>();
        for (Object[] stat : typeStats) {
            eventsByType.put((String) stat[0], (Long) stat[1]);
        }
        
        Object[] processingMetrics = outboxRepository.getProcessingTimeMetrics();
        double avgProcessingTime = processingMetrics != null && processingMetrics[0] != null 
                ? ((Number) processingMetrics[0]).doubleValue() : 0.0;
        
        return OutboxStatistics.builder()
                .unprocessedEvents(unprocessedCount)
                .processedLastHour(processedLastHour)
                .eventsByType(eventsByType)
                .averageProcessingTimeSeconds(avgProcessingTime)
                .build();
    }

    /**
     * Verifica si existe un evento duplicado basado en la clave de idempotencia
     */
    private boolean isDuplicate(String idempotencyKey) {
        if (idempotencyKey == null) {
            return false;
        }
        
        // Verificar en los últimos 5 minutos
        Instant fiveMinutesAgo = Instant.now().minusSeconds(300);
        return outboxRepository.existsRecentByIdempotencyKey(idempotencyKey, fiveMinutesAgo);
    }

    /**
     * Convierte un evento a mapa para almacenar como JSON
     */
    private Map<String, Object> convertToMap(Object event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            log.error("Error convirtiendo evento a mapa", e);
            throw new OutboxException("Error serializando evento", e);
        }
    }

    /**
     * Enmascara email para logs
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        String[] parts = email.split("@");
        return parts[0].substring(0, Math.min(2, parts[0].length())) + "***@" + parts[1];
    }

    /**
     * Enmascara teléfono para logs
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) {
            return "***";
        }
        return "***" + phone.substring(phone.length() - 4);
    }

    /**
     * Estadísticas del Outbox
     */
    public static class OutboxStatistics {
        public final long unprocessedEvents;
        public final long processedLastHour;
        public final Map<String, Long> eventsByType;
        public final double averageProcessingTimeSeconds;
        
        private OutboxStatistics(OutboxStatisticsBuilder builder) {
            this.unprocessedEvents = builder.unprocessedEvents;
            this.processedLastHour = builder.processedLastHour;
            this.eventsByType = builder.eventsByType;
            this.averageProcessingTimeSeconds = builder.averageProcessingTimeSeconds;
        }
        
        public static OutboxStatisticsBuilder builder() {
            return new OutboxStatisticsBuilder();
        }
        
        public static class OutboxStatisticsBuilder {
            private long unprocessedEvents;
            private long processedLastHour;
            private Map<String, Long> eventsByType = new HashMap<>();
            private double averageProcessingTimeSeconds;
            
            public OutboxStatisticsBuilder unprocessedEvents(long unprocessedEvents) {
                this.unprocessedEvents = unprocessedEvents;
                return this;
            }
            
            public OutboxStatisticsBuilder processedLastHour(long processedLastHour) {
                this.processedLastHour = processedLastHour;
                return this;
            }
            
            public OutboxStatisticsBuilder eventsByType(Map<String, Long> eventsByType) {
                this.eventsByType = eventsByType;
                return this;
            }
            
            public OutboxStatisticsBuilder averageProcessingTimeSeconds(double averageProcessingTimeSeconds) {
                this.averageProcessingTimeSeconds = averageProcessingTimeSeconds;
                return this;
            }
            
            public OutboxStatistics build() {
                return new OutboxStatistics(this);
            }
        }
    }

    /**
     * Excepción para errores del Outbox
     */
    public static class OutboxException extends RuntimeException {
        public OutboxException(String message) {
            super(message);
        }
        
        public OutboxException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}