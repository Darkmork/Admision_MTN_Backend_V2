// user-service/src/main/java/com/desafios/mtn/userservice/event/EventPublisher.java

package com.desafios.mtn.userservice.event;

import com.desafios.mtn.userservice.repository.DomainEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Servicio para publicar eventos de dominio usando el patrón Outbox
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventPublisher {

    private final DomainEventRepository domainEventRepository;
    private final ObjectMapper objectMapper;

    /**
     * Publica un evento de usuario creado
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publishUserCreated(UserEvent.UserCreated event) {
        publishEvent(
            UserEvent.AggregateTypes.USER,
            event.getUserId(),
            UserEvent.EventTypes.USER_CREATED,
            convertToMap(event),
            event.getCorrelationId(),
            event.getUserId()
        );
    }

    /**
     * Publica un evento de usuario actualizado
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publishUserUpdated(UserEvent.UserUpdated event) {
        publishEvent(
            UserEvent.AggregateTypes.USER,
            event.getUserId(),
            UserEvent.EventTypes.USER_UPDATED,
            convertToMap(event),
            event.getCorrelationId(),
            event.getUserId()
        );
    }

    /**
     * Publica un evento de usuario eliminado
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publishUserDeleted(UserEvent.UserDeleted event) {
        publishEvent(
            UserEvent.AggregateTypes.USER,
            event.getUserId(),
            UserEvent.EventTypes.USER_DELETED,
            convertToMap(event),
            event.getCorrelationId(),
            event.getUserId()
        );
    }

    /**
     * Publica un evento de roles cambiados
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publishUserRolesChanged(UserEvent.UserRolesChanged event) {
        publishEvent(
            UserEvent.AggregateTypes.USER,
            event.getUserId(),
            UserEvent.EventTypes.USER_ROLES_CHANGED,
            convertToMap(event),
            event.getCorrelationId(),
            event.getUserId()
        );
    }

    /**
     * Publica un evento de estado cambiado
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publishUserStatusChanged(UserEvent.UserStatusChanged event) {
        publishEvent(
            UserEvent.AggregateTypes.USER,
            event.getUserId(),
            UserEvent.EventTypes.USER_STATUS_CHANGED,
            convertToMap(event),
            event.getCorrelationId(),
            event.getUserId()
        );
    }

    /**
     * Publica un evento de login
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publishUserLoggedIn(UserEvent.UserLoggedIn event) {
        publishEvent(
            UserEvent.AggregateTypes.USER,
            event.getUserId(),
            UserEvent.EventTypes.USER_LOGGED_IN,
            convertToMap(event),
            event.getCorrelationId(),
            event.getUserId()
        );
    }

    /**
     * Publica un evento de contraseña cambiada
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publishUserPasswordChanged(UserEvent.UserPasswordChanged event) {
        publishEvent(
            UserEvent.AggregateTypes.USER,
            event.getUserId(),
            UserEvent.EventTypes.USER_PASSWORD_CHANGED,
            convertToMap(event),
            event.getCorrelationId(),
            event.getUserId()
        );
    }

    /**
     * Publica un evento de email verificado
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publishUserEmailVerified(UserEvent.UserEmailVerified event) {
        publishEvent(
            UserEvent.AggregateTypes.USER,
            event.getUserId(),
            UserEvent.EventTypes.USER_EMAIL_VERIFIED,
            convertToMap(event),
            event.getCorrelationId(),
            event.getUserId()
        );
    }

    /**
     * Publica un evento genérico
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publishEvent(String aggregateType, UUID aggregateId, String eventType, 
                           Map<String, Object> eventData, UUID correlationId, UUID userId) {
        
        log.debug("Publicando evento: {} para agregado {} con ID {}", eventType, aggregateType, aggregateId);
        
        try {
            // Verificar duplicados recientes para evitar eventos duplicados
            if (isRecentDuplicateEvent(aggregateId, eventType)) {
                log.debug("Evento duplicado detectado y omitido: {} para agregado {}", eventType, aggregateId);
                return;
            }
            
            // Crear el evento de dominio
            DomainEvent domainEvent = DomainEvent.builder()
                    .aggregateType(aggregateType)
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .eventData(eventData)
                    .eventVersion(1)
                    .correlationId(correlationId)
                    .userId(userId)
                    .build();
            
            // Guardar en la tabla de eventos (Outbox)
            domainEventRepository.save(domainEvent);
            
            log.info("Evento publicado exitosamente: {} para agregado {} con ID {}", 
                    eventType, aggregateType, aggregateId);
            
        } catch (Exception e) {
            log.error("Error publicando evento: {} para agregado {} con ID {}", 
                    eventType, aggregateType, aggregateId, e);
            throw new EventPublishingException("Error publicando evento", e);
        }
    }

    /**
     * Publica un evento con causación (para cadenas de eventos)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publishEventWithCausation(String aggregateType, UUID aggregateId, String eventType,
                                        Map<String, Object> eventData, UUID correlationId, 
                                        UUID causationId, UUID userId) {
        
        log.debug("Publicando evento causado: {} para agregado {} con ID {}", eventType, aggregateType, aggregateId);
        
        try {
            DomainEvent domainEvent = DomainEvent.builder()
                    .aggregateType(aggregateType)
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .eventData(eventData)
                    .eventVersion(1)
                    .correlationId(correlationId)
                    .causationId(causationId)
                    .userId(userId)
                    .build();
            
            domainEventRepository.save(domainEvent);
            
            log.info("Evento causado publicado exitosamente: {} para agregado {} con ID {}", 
                    eventType, aggregateType, aggregateId);
            
        } catch (Exception e) {
            log.error("Error publicando evento causado: {} para agregado {} con ID {}", 
                    eventType, aggregateType, aggregateId, e);
            throw new EventPublishingException("Error publicando evento causado", e);
        }
    }

    /**
     * Convierte un objeto de evento a un mapa para almacenar como JSON
     */
    private Map<String, Object> convertToMap(Object event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            log.error("Error convirtiendo evento a mapa: {}", event.getClass().getSimpleName(), e);
            // Fallback: crear mapa básico con información mínima
            Map<String, Object> fallbackMap = new HashMap<>();
            fallbackMap.put("eventClass", event.getClass().getSimpleName());
            fallbackMap.put("timestamp", Instant.now().toString());
            fallbackMap.put("conversionError", e.getMessage());
            return fallbackMap;
        }
    }

    /**
     * Verifica si ya existe un evento reciente similar para evitar duplicados
     */
    private boolean isRecentDuplicateEvent(UUID aggregateId, String eventType) {
        // Verificar en los últimos 5 minutos
        Instant fiveMinutesAgo = Instant.now().minusSeconds(300);
        return domainEventRepository.existsRecentEvent(aggregateId, eventType, fiveMinutesAgo);
    }

    /**
     * Obtiene estadísticas de eventos publicados
     */
    public EventPublishingStats getPublishingStats() {
        long totalEvents = domainEventRepository.count();
        long unprocessedEvents = domainEventRepository.countUnprocessedEvents();
        long processedLastHour = domainEventRepository.countProcessedEventsSince(Instant.now().minusSeconds(3600));
        
        return EventPublishingStats.builder()
                .totalEvents(totalEvents)
                .unprocessedEvents(unprocessedEvents)
                .processedLastHour(processedLastHour)
                .build();
    }

    /**
     * Estadísticas de publicación de eventos
     */
    public static class EventPublishingStats {
        public final long totalEvents;
        public final long unprocessedEvents;
        public final long processedLastHour;
        
        private EventPublishingStats(EventPublishingStatsBuilder builder) {
            this.totalEvents = builder.totalEvents;
            this.unprocessedEvents = builder.unprocessedEvents;
            this.processedLastHour = builder.processedLastHour;
        }
        
        public static EventPublishingStatsBuilder builder() {
            return new EventPublishingStatsBuilder();
        }
        
        public static class EventPublishingStatsBuilder {
            private long totalEvents;
            private long unprocessedEvents;
            private long processedLastHour;
            
            public EventPublishingStatsBuilder totalEvents(long totalEvents) {
                this.totalEvents = totalEvents;
                return this;
            }
            
            public EventPublishingStatsBuilder unprocessedEvents(long unprocessedEvents) {
                this.unprocessedEvents = unprocessedEvents;
                return this;
            }
            
            public EventPublishingStatsBuilder processedLastHour(long processedLastHour) {
                this.processedLastHour = processedLastHour;
                return this;
            }
            
            public EventPublishingStats build() {
                return new EventPublishingStats(this);
            }
        }
    }

    /**
     * Excepción para errores de publicación de eventos
     */
    public static class EventPublishingException extends RuntimeException {
        public EventPublishingException(String message) {
            super(message);
        }
        
        public EventPublishingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}