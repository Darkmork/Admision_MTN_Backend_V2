package com.desafios.mtn.applicationservice.service;

import com.desafios.mtn.applicationservice.domain.OutboxEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Servicio para publicar eventos a RabbitMQ desde el patrón Outbox
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "application.events.enabled", havingValue = "true", matchIfMissing = true)
public class EventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    // ================================
    // PUBLICACIÓN DE EVENTOS
    // ================================

    /**
     * Publica un evento del Outbox a RabbitMQ
     */
    public void publishEvent(OutboxEvent event) {
        try {
            String exchange = event.getExchangeName() != null ? event.getExchangeName() : "admission.events";
            String routingKey = event.getRoutingKey() != null ? event.getRoutingKey() : event.getEventType().toLowerCase();
            
            Message message = createMessage(event);
            
            log.debug("Publishing event {} to exchange {} with routing key {}", 
                     event.getEventType(), exchange, routingKey);
            
            rabbitTemplate.send(exchange, routingKey, message);
            
            log.info("Successfully published event: {} ({})", 
                    event.getEventType(), event.getId());
            
        } catch (Exception e) {
            log.error("Failed to publish event {} ({}): {}", 
                     event.getEventType(), event.getId(), e.getMessage(), e);
            throw new EventPublishingException("Failed to publish event: " + e.getMessage(), e);
        }
    }

    /**
     * Crea un mensaje AMQP a partir de un evento del Outbox
     */
    private Message createMessage(OutboxEvent event) throws Exception {
        // Crear el payload del mensaje
        Map<String, Object> messagePayload = new HashMap<>();
        messagePayload.put("eventId", event.getId());
        messagePayload.put("eventType", event.getEventType());
        messagePayload.put("eventVersion", event.getEventVersion());
        messagePayload.put("aggregateType", event.getAggregateType());
        messagePayload.put("aggregateId", event.getAggregateId());
        messagePayload.put("timestamp", event.getCreatedAt().toString());
        messagePayload.put("data", event.getPayload());
        
        // Agregar metadatos adicionales si están presentes
        if (event.getCorrelationId() != null) {
            messagePayload.put("correlationId", event.getCorrelationId());
        }
        if (event.getCausationId() != null) {
            messagePayload.put("causationId", event.getCausationId());
        }
        
        String jsonPayload = objectMapper.writeValueAsString(messagePayload);
        
        // Crear el mensaje con headers
        Message message = MessageBuilder.withBody(jsonPayload.getBytes())
                .setContentType("application/json")
                .setHeader("eventId", event.getId().toString())
                .setHeader("eventType", event.getEventType())
                .setHeader("eventVersion", event.getEventVersion())
                .setHeader("aggregateType", event.getAggregateType())
                .setHeader("aggregateId", event.getAggregateId().toString())
                .setHeader("timestamp", event.getCreatedAt().toString())
                .setHeader("priority", event.getPriority().getLevel())
                .build();
        
        // Agregar headers personalizados si están presentes
        if (event.getHeaders() != null) {
            event.getHeaders().forEach((key, value) -> 
                message.getMessageProperties().setHeader("custom." + key, value));
        }
        
        // Agregar headers de correlación
        if (event.getCorrelationId() != null) {
            message.getMessageProperties().setCorrelationId(event.getCorrelationId());
        }
        
        return message;
    }

    // ================================
    // EVENTOS ESPECÍFICOS DEL DOMINIO
    // ================================

    /**
     * Publica evento de aplicación enviada
     */
    public void publishApplicationSubmitted(UUID applicationId, Map<String, Object> applicationData) {
        try {
            Map<String, Object> payload = Map.of(
                "applicationId", applicationId,
                "timestamp", Instant.now().toString(),
                "data", applicationData
            );
            
            Message message = MessageBuilder.withBody(objectMapper.writeValueAsBytes(payload))
                    .setContentType("application/json")
                    .setHeader("eventType", "ApplicationSubmitted.v1")
                    .setHeader("aggregateType", "Application")
                    .setHeader("aggregateId", applicationId.toString())
                    .build();
            
            rabbitTemplate.send("admission.events", "application.submitted", message);
            log.info("Published ApplicationSubmitted event for application: {}", applicationId);
            
        } catch (Exception e) {
            log.error("Failed to publish ApplicationSubmitted event: {}", e.getMessage(), e);
            throw new EventPublishingException("Failed to publish ApplicationSubmitted event", e);
        }
    }

    /**
     * Publica evento de cambio de estado
     */
    public void publishStateChanged(UUID applicationId, String fromState, String toState, 
                                  String reasonCode, String actorUserId) {
        try {
            Map<String, Object> payload = Map.of(
                "applicationId", applicationId,
                "fromState", fromState,
                "toState", toState,
                "reasonCode", reasonCode,
                "actorUserId", actorUserId,
                "timestamp", Instant.now().toString()
            );
            
            Message message = MessageBuilder.withBody(objectMapper.writeValueAsBytes(payload))
                    .setContentType("application/json")
                    .setHeader("eventType", "StateChanged.v1")
                    .setHeader("aggregateType", "Application")
                    .setHeader("aggregateId", applicationId.toString())
                    .setHeader("fromState", fromState)
                    .setHeader("toState", toState)
                    .build();
            
            rabbitTemplate.send("admission.events", "application.state.changed", message);
            log.info("Published StateChanged event: {} -> {} for application: {}", 
                    fromState, toState, applicationId);
            
        } catch (Exception e) {
            log.error("Failed to publish StateChanged event: {}", e.getMessage(), e);
            throw new EventPublishingException("Failed to publish StateChanged event", e);
        }
    }

    /**
     * Publica evento de documento subido
     */
    public void publishDocumentUploaded(UUID applicationId, UUID documentId, 
                                      String documentType, String uploadedBy) {
        try {
            Map<String, Object> payload = Map.of(
                "applicationId", applicationId,
                "documentId", documentId,
                "documentType", documentType,
                "uploadedBy", uploadedBy,
                "timestamp", Instant.now().toString()
            );
            
            Message message = MessageBuilder.withBody(objectMapper.writeValueAsBytes(payload))
                    .setContentType("application/json")
                    .setHeader("eventType", "DocumentUploaded.v1")
                    .setHeader("aggregateType", "AppDocument")
                    .setHeader("aggregateId", documentId.toString())
                    .setHeader("applicationId", applicationId.toString())
                    .build();
            
            rabbitTemplate.send("admission.events", "document.uploaded", message);
            log.info("Published DocumentUploaded event for document: {} (app: {})", 
                    documentId, applicationId);
            
        } catch (Exception e) {
            log.error("Failed to publish DocumentUploaded event: {}", e.getMessage(), e);
            throw new EventPublishingException("Failed to publish DocumentUploaded event", e);
        }
    }

    /**
     * Publica evento de evaluación completada
     */
    public void publishEvaluationCompleted(UUID applicationId, String evaluationType, 
                                         String evaluatorId, Map<String, Object> results) {
        try {
            Map<String, Object> payload = Map.of(
                "applicationId", applicationId,
                "evaluationType", evaluationType,
                "evaluatorId", evaluatorId,
                "results", results,
                "timestamp", Instant.now().toString()
            );
            
            Message message = MessageBuilder.withBody(objectMapper.writeValueAsBytes(payload))
                    .setContentType("application/json")
                    .setHeader("eventType", "EvaluationCompleted.v1")
                    .setHeader("aggregateType", "Application")
                    .setHeader("aggregateId", applicationId.toString())
                    .setHeader("evaluationType", evaluationType)
                    .build();
            
            rabbitTemplate.send("admission.events", "evaluation.completed", message);
            log.info("Published EvaluationCompleted event: {} for application: {}", 
                    evaluationType, applicationId);
            
        } catch (Exception e) {
            log.error("Failed to publish EvaluationCompleted event: {}", e.getMessage(), e);
            throw new EventPublishingException("Failed to publish EvaluationCompleted event", e);
        }
    }

    // ================================
    // EVENTOS DE NOTIFICACIÓN
    // ================================

    /**
     * Publica evento de notificación programada
     */
    public void publishNotificationScheduled(String notificationType, UUID applicationId, 
                                           String recipientEmail, Map<String, Object> templateData) {
        try {
            Map<String, Object> payload = Map.of(
                "notificationType", notificationType,
                "applicationId", applicationId,
                "recipientEmail", recipientEmail,
                "templateData", templateData,
                "timestamp", Instant.now().toString()
            );
            
            Message message = MessageBuilder.withBody(objectMapper.writeValueAsBytes(payload))
                    .setContentType("application/json")
                    .setHeader("eventType", "NotificationScheduled.v1")
                    .setHeader("aggregateType", "Application")
                    .setHeader("aggregateId", applicationId.toString())
                    .setHeader("notificationType", notificationType)
                    .setHeader("recipientEmail", recipientEmail)
                    .build();
            
            rabbitTemplate.send("admission.events", "notification.scheduled", message);
            log.info("Published NotificationScheduled event: {} for application: {}", 
                    notificationType, applicationId);
            
        } catch (Exception e) {
            log.error("Failed to publish NotificationScheduled event: {}", e.getMessage(), e);
            throw new EventPublishingException("Failed to publish NotificationScheduled event", e);
        }
    }

    // ================================
    // UTILIDADES Y VALIDACIÓN
    // ================================

    /**
     * Verifica la conectividad con RabbitMQ
     */
    public boolean testConnectivity() {
        try {
            // Intentar una operación simple para verificar conexión
            rabbitTemplate.execute(channel -> {
                channel.queueDeclarePassive("test.connectivity.queue");
                return true;
            });
            return true;
        } catch (Exception e) {
            log.warn("RabbitMQ connectivity test failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Obtiene estadísticas de la cola
     */
    public Map<String, Object> getQueueStatistics(String queueName) {
        try {
            return rabbitTemplate.execute(channel -> {
                var response = channel.queueDeclare(queueName, true, false, false, null);
                Map<String, Object> stats = new HashMap<>();
                stats.put("queueName", queueName);
                stats.put("messageCount", response.getMessageCount());
                stats.put("consumerCount", response.getConsumerCount());
                return stats;
            });
        } catch (Exception e) {
            log.error("Failed to get queue statistics for {}: {}", queueName, e.getMessage());
            return Map.of("error", e.getMessage());
        }
    }

    // ================================
    // EXCEPCIONES PERSONALIZADAS
    // ================================

    public static class EventPublishingException extends RuntimeException {
        public EventPublishingException(String message) {
            super(message);
        }
        
        public EventPublishingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}