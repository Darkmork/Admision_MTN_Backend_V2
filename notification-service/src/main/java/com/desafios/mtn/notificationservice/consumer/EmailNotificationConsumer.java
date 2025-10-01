package com.desafios.mtn.notificationservice.consumer;

import com.desafios.mtn.notificationservice.config.RabbitTopologyConfig;
import com.desafios.mtn.notificationservice.service.NotificationProcessingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

/**
 * Consumidor de eventos de notificación por email
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationConsumer {

    private final NotificationProcessingService notificationProcessingService;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    private final Tracer tracer;

    /**
     * Procesa eventos de EmailRequested desde la cola principal
     */
    @RabbitListener(queues = RabbitTopologyConfig.EMAIL_QUEUE)
    public void handleEmailRequested(@Payload Map<String, Object> eventPayload, 
                                   Message message,
                                   @Header(value = "eventId", required = false) String eventId,
                                   @Header(value = "eventType", required = false) String eventType,
                                   @Header(value = "correlationId", required = false) String correlationId,
                                   @Header(value = "idempotencyKey", required = false) String idempotencyKey) {
        
        String messageId = extractMessageId(eventPayload);
        
        // Crear span de tracing
        var span = tracer.nextSpan()
                .name("email.notification.process")
                .tag("message.id", messageId != null ? messageId.substring(0, 8) : "unknown")
                .tag("event.type", eventType != null ? eventType : "EmailRequested.v1")
                .tag("channel", "email")
                .start();

        try (var ws = tracer.withSpanInScope(span)) {
            log.info("📧 Processing email notification: messageId={}, correlationId={}, idempotencyKey={}", 
                    messageId != null ? messageId.substring(0, 8) : "unknown", 
                    correlationId, idempotencyKey);

            // Registrar métricas
            meterRegistry.counter("notifications.received.total", 
                    "channel", "email", "queue", "main").increment();

            // Procesar notificación
            boolean success = notificationProcessingService.processEmailNotification(eventPayload);

            if (success) {
                log.info("✅ Email notification processed successfully: {}", 
                        messageId != null ? messageId.substring(0, 8) : "unknown");
                        
                meterRegistry.counter("notifications.processed.total", 
                        "channel", "email", "status", "success").increment();
                span.tag("processing.status", "success");
            } else {
                log.warn("⚠️ Email notification processing failed: {}", 
                        messageId != null ? messageId.substring(0, 8) : "unknown");
                        
                meterRegistry.counter("notifications.processed.total", 
                        "channel", "email", "status", "failed").increment();
                span.tag("processing.status", "failed");
                
                // Lanzar excepción para activar retry
                throw new NotificationProcessingException("Email processing failed");
            }

        } catch (Exception e) {
            log.error("❌ Error processing email notification {}: {}", 
                     messageId != null ? messageId.substring(0, 8) : "unknown", e.getMessage(), e);
                     
            meterRegistry.counter("notifications.processing.errors.total", 
                    "channel", "email", "error.type", e.getClass().getSimpleName()).increment();
            
            span.tag("error", true);
            span.tag("error.message", e.getMessage());
            
            // Re-lanzar para activar el sistema de retry de RabbitMQ
            throw new NotificationProcessingException("Failed to process email notification", e);
            
        } finally {
            span.end();
        }
    }

    /**
     * Procesa eventos desde las colas de retry nivel 1
     */
    @RabbitListener(queues = RabbitTopologyConfig.EMAIL_RETRY_QUEUE_1)
    public void handleEmailRetry1(@Payload Map<String, Object> eventPayload, Message message) {
        handleEmailRetry(eventPayload, message, 1);
    }

    /**
     * Procesa eventos desde las colas de retry nivel 2
     */
    @RabbitListener(queues = RabbitTopologyConfig.EMAIL_RETRY_QUEUE_2)
    public void handleEmailRetry2(@Payload Map<String, Object> eventPayload, Message message) {
        handleEmailRetry(eventPayload, message, 2);
    }

    /**
     * Procesa eventos desde las colas de retry nivel 3
     */
    @RabbitListener(queues = RabbitTopologyConfig.EMAIL_RETRY_QUEUE_3)
    public void handleEmailRetry3(@Payload Map<String, Object> eventPayload, Message message) {
        handleEmailRetry(eventPayload, message, 3);
    }

    /**
     * Procesa eventos desde las colas de retry nivel 4
     */
    @RabbitListener(queues = RabbitTopologyConfig.EMAIL_RETRY_QUEUE_4)
    public void handleEmailRetry4(@Payload Map<String, Object> eventPayload, Message message) {
        handleEmailRetry(eventPayload, message, 4);
    }

    /**
     * Procesa eventos desde las colas de retry nivel 5 (último intento)
     */
    @RabbitListener(queues = RabbitTopologyConfig.EMAIL_RETRY_QUEUE_5)
    public void handleEmailRetry5(@Payload Map<String, Object> eventPayload, Message message) {
        handleEmailRetry(eventPayload, message, 5);
    }

    /**
     * Maneja eventos en Dead Letter Queue para monitoreo
     */
    @RabbitListener(queues = RabbitTopologyConfig.EMAIL_DLQ)
    public void handleEmailDlq(@Payload Map<String, Object> eventPayload, Message message) {
        String messageId = extractMessageId(eventPayload);
        
        log.error("💀 Email notification reached DLQ: messageId={}, attempts=MAX", 
                 messageId != null ? messageId.substring(0, 8) : "unknown");

        meterRegistry.counter("notifications.dlq.total", "channel", "email").increment();

        // Registrar en base de datos para auditoría
        try {
            notificationProcessingService.handleDlqMessage("email", eventPayload, "Max retry attempts exceeded");
        } catch (Exception e) {
            log.error("Error handling DLQ message: {}", e.getMessage(), e);
        }
    }

    /**
     * Maneja reintentos genéricos
     */
    private void handleEmailRetry(Map<String, Object> eventPayload, Message message, int retryLevel) {
        String messageId = extractMessageId(eventPayload);
        
        var span = tracer.nextSpan()
                .name("email.notification.retry")
                .tag("message.id", messageId != null ? messageId.substring(0, 8) : "unknown")
                .tag("retry.level", String.valueOf(retryLevel))
                .tag("channel", "email")
                .start();

        try (var ws = tracer.withSpanInScope(span)) {
            log.info("🔄 Processing email retry level {}: messageId={}", 
                    retryLevel, messageId != null ? messageId.substring(0, 8) : "unknown");

            meterRegistry.counter("notifications.retry.attempts.total", 
                    "channel", "email", "level", String.valueOf(retryLevel)).increment();

            // Intentar procesar nuevamente
            boolean success = notificationProcessingService.processEmailNotification(eventPayload);

            if (success) {
                log.info("✅ Email retry level {} succeeded: {}", retryLevel, 
                        messageId != null ? messageId.substring(0, 8) : "unknown");
                        
                meterRegistry.counter("notifications.retry.success.total", 
                        "channel", "email", "level", String.valueOf(retryLevel)).increment();
                        
                span.tag("retry.status", "success");
            } else {
                log.warn("⚠️ Email retry level {} failed: {}", retryLevel, 
                        messageId != null ? messageId.substring(0, 8) : "unknown");
                        
                meterRegistry.counter("notifications.retry.failed.total", 
                        "channel", "email", "level", String.valueOf(retryLevel)).increment();
                        
                span.tag("retry.status", "failed");
                
                // Lanzar excepción para continuar con el siguiente nivel de retry
                throw new NotificationProcessingException("Email retry level " + retryLevel + " failed");
            }

        } catch (Exception e) {
            log.error("❌ Error in email retry level {}: messageId={}, error={}", 
                     retryLevel, messageId != null ? messageId.substring(0, 8) : "unknown", e.getMessage());
                     
            meterRegistry.counter("notifications.retry.errors.total", 
                    "channel", "email", "level", String.valueOf(retryLevel)).increment();
                    
            span.tag("error", true);
            span.tag("error.message", e.getMessage());
            span.tag("retry.status", "error");
            
            // Re-lanzar para activar el siguiente nivel de retry
            throw new NotificationProcessingException("Retry level " + retryLevel + " failed", e);
            
        } finally {
            span.end();
        }
    }

    /**
     * Extrae el message ID del payload del evento
     */
    private String extractMessageId(Map<String, Object> eventPayload) {
        try {
            return (String) eventPayload.get("message_id");
        } catch (Exception e) {
            log.warn("Could not extract message_id from event payload: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Obtiene información de debug del mensaje RabbitMQ
     */
    private String getMessageDebugInfo(Message message) {
        try {
            return String.format("routingKey=%s, deliveryTag=%d, redelivered=%s, timestamp=%s",
                    message.getMessageProperties().getReceivedRoutingKey(),
                    message.getMessageProperties().getDeliveryTag(),
                    message.getMessageProperties().isRedelivered(),
                    message.getMessageProperties().getTimestamp() != null ? 
                            Instant.ofEpochMilli(message.getMessageProperties().getTimestamp().getTime()) : "null");
        } catch (Exception e) {
            return "debug-info-unavailable";
        }
    }

    /**
     * Excepción para errores de procesamiento de notificaciones
     */
    public static class NotificationProcessingException extends RuntimeException {
        public NotificationProcessingException(String message) {
            super(message);
        }

        public NotificationProcessingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}