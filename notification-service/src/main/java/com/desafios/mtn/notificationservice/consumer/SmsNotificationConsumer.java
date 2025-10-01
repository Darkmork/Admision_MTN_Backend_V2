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
 * Consumidor de eventos de notificación por SMS
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SmsNotificationConsumer {

    private final NotificationProcessingService notificationProcessingService;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    private final Tracer tracer;

    /**
     * Procesa eventos de SmsRequested desde la cola principal
     */
    @RabbitListener(queues = RabbitTopologyConfig.SMS_QUEUE)
    public void handleSmsRequested(@Payload Map<String, Object> eventPayload, 
                                 Message message,
                                 @Header(value = "eventId", required = false) String eventId,
                                 @Header(value = "eventType", required = false) String eventType,
                                 @Header(value = "correlationId", required = false) String correlationId,
                                 @Header(value = "idempotencyKey", required = false) String idempotencyKey) {
        
        String messageId = extractMessageId(eventPayload);
        
        // Crear span de tracing
        var span = tracer.nextSpan()
                .name("sms.notification.process")
                .tag("message.id", messageId != null ? messageId.substring(0, 8) : "unknown")
                .tag("event.type", eventType != null ? eventType : "SmsRequested.v1")
                .tag("channel", "sms")
                .start();

        try (var ws = tracer.withSpanInScope(span)) {
            log.info("📱 Processing SMS notification: messageId={}, correlationId={}, idempotencyKey={}", 
                    messageId != null ? messageId.substring(0, 8) : "unknown", 
                    correlationId, idempotencyKey);

            // Registrar métricas
            meterRegistry.counter("notifications.received.total", 
                    "channel", "sms", "queue", "main").increment();

            // Procesar notificación
            boolean success = notificationProcessingService.processSmsNotification(eventPayload);

            if (success) {
                log.info("✅ SMS notification processed successfully: {}", 
                        messageId != null ? messageId.substring(0, 8) : "unknown");
                        
                meterRegistry.counter("notifications.processed.total", 
                        "channel", "sms", "status", "success").increment();
                span.tag("processing.status", "success");
            } else {
                log.warn("⚠️ SMS notification processing failed: {}", 
                        messageId != null ? messageId.substring(0, 8) : "unknown");
                        
                meterRegistry.counter("notifications.processed.total", 
                        "channel", "sms", "status", "failed").increment();
                span.tag("processing.status", "failed");
                
                // Lanzar excepción para activar retry
                throw new NotificationProcessingException("SMS processing failed");
            }

        } catch (Exception e) {
            log.error("❌ Error processing SMS notification {}: {}", 
                     messageId != null ? messageId.substring(0, 8) : "unknown", e.getMessage(), e);
                     
            meterRegistry.counter("notifications.processing.errors.total", 
                    "channel", "sms", "error.type", e.getClass().getSimpleName()).increment();
            
            span.tag("error", true);
            span.tag("error.message", e.getMessage());
            
            // Re-lanzar para activar el sistema de retry de RabbitMQ
            throw new NotificationProcessingException("Failed to process SMS notification", e);
            
        } finally {
            span.end();
        }
    }

    /**
     * Procesa eventos desde las colas de retry nivel 1
     */
    @RabbitListener(queues = RabbitTopologyConfig.SMS_RETRY_QUEUE_1)
    public void handleSmsRetry1(@Payload Map<String, Object> eventPayload, Message message) {
        handleSmsRetry(eventPayload, message, 1);
    }

    /**
     * Procesa eventos desde las colas de retry nivel 2
     */
    @RabbitListener(queues = RabbitTopologyConfig.SMS_RETRY_QUEUE_2)
    public void handleSmsRetry2(@Payload Map<String, Object> eventPayload, Message message) {
        handleSmsRetry(eventPayload, message, 2);
    }

    /**
     * Procesa eventos desde las colas de retry nivel 3
     */
    @RabbitListener(queues = RabbitTopologyConfig.SMS_RETRY_QUEUE_3)
    public void handleSmsRetry3(@Payload Map<String, Object> eventPayload, Message message) {
        handleSmsRetry(eventPayload, message, 3);
    }

    /**
     * Procesa eventos desde las colas de retry nivel 4
     */
    @RabbitListener(queues = RabbitTopologyConfig.SMS_RETRY_QUEUE_4)
    public void handleSmsRetry4(@Payload Map<String, Object> eventPayload, Message message) {
        handleSmsRetry(eventPayload, message, 4);
    }

    /**
     * Procesa eventos desde las colas de retry nivel 5 (último intento)
     */
    @RabbitListener(queues = RabbitTopologyConfig.SMS_RETRY_QUEUE_5)
    public void handleSmsRetry5(@Payload Map<String, Object> eventPayload, Message message) {
        handleSmsRetry(eventPayload, message, 5);
    }

    /**
     * Maneja eventos en Dead Letter Queue para monitoreo
     */
    @RabbitListener(queues = RabbitTopologyConfig.SMS_DLQ)
    public void handleSmsDlq(@Payload Map<String, Object> eventPayload, Message message) {
        String messageId = extractMessageId(eventPayload);
        String phoneNumber = extractPhoneNumber(eventPayload);
        
        log.error("💀 SMS notification reached DLQ: messageId={}, to={}, attempts=MAX", 
                 messageId != null ? messageId.substring(0, 8) : "unknown",
                 maskPhoneNumber(phoneNumber));

        meterRegistry.counter("notifications.dlq.total", "channel", "sms").increment();

        // Registrar en base de datos para auditoría
        try {
            notificationProcessingService.handleDlqMessage("sms", eventPayload, "Max retry attempts exceeded");
        } catch (Exception e) {
            log.error("Error handling SMS DLQ message: {}", e.getMessage(), e);
        }
    }

    /**
     * Maneja reintentos genéricos
     */
    private void handleSmsRetry(Map<String, Object> eventPayload, Message message, int retryLevel) {
        String messageId = extractMessageId(eventPayload);
        
        var span = tracer.nextSpan()
                .name("sms.notification.retry")
                .tag("message.id", messageId != null ? messageId.substring(0, 8) : "unknown")
                .tag("retry.level", String.valueOf(retryLevel))
                .tag("channel", "sms")
                .start();

        try (var ws = tracer.withSpanInScope(span)) {
            log.info("🔄 Processing SMS retry level {}: messageId={}", 
                    retryLevel, messageId != null ? messageId.substring(0, 8) : "unknown");

            meterRegistry.counter("notifications.retry.attempts.total", 
                    "channel", "sms", "level", String.valueOf(retryLevel)).increment();

            // Intentar procesar nuevamente
            boolean success = notificationProcessingService.processSmsNotification(eventPayload);

            if (success) {
                log.info("✅ SMS retry level {} succeeded: {}", retryLevel, 
                        messageId != null ? messageId.substring(0, 8) : "unknown");
                        
                meterRegistry.counter("notifications.retry.success.total", 
                        "channel", "sms", "level", String.valueOf(retryLevel)).increment();
                        
                span.tag("retry.status", "success");
            } else {
                log.warn("⚠️ SMS retry level {} failed: {}", retryLevel, 
                        messageId != null ? messageId.substring(0, 8) : "unknown");
                        
                meterRegistry.counter("notifications.retry.failed.total", 
                        "channel", "sms", "level", String.valueOf(retryLevel)).increment();
                        
                span.tag("retry.status", "failed");
                
                // Lanzar excepción para continuar con el siguiente nivel de retry
                throw new NotificationProcessingException("SMS retry level " + retryLevel + " failed");
            }

        } catch (Exception e) {
            log.error("❌ Error in SMS retry level {}: messageId={}, error={}", 
                     retryLevel, messageId != null ? messageId.substring(0, 8) : "unknown", e.getMessage());
                     
            meterRegistry.counter("notifications.retry.errors.total", 
                    "channel", "sms", "level", String.valueOf(retryLevel)).increment();
                    
            span.tag("error", true);
            span.tag("error.message", e.getMessage());
            span.tag("retry.status", "error");
            
            // Re-lanzar para activar el siguiente nivel de retry
            throw new NotificationProcessingException("SMS retry level " + retryLevel + " failed", e);
            
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
            log.warn("Could not extract message_id from SMS event payload: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extrae el número de teléfono del payload del evento
     */
    private String extractPhoneNumber(Map<String, Object> eventPayload) {
        try {
            return (String) eventPayload.get("to");
        } catch (Exception e) {
            log.warn("Could not extract phone number from SMS event payload: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Enmascara número de teléfono para logs de seguridad
     */
    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 4) {
            return "***";
        }
        return "***" + phoneNumber.substring(phoneNumber.length() - 4);
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
     * Valida el payload de evento SMS
     */
    private boolean isValidSmsPayload(Map<String, Object> eventPayload) {
        try {
            return eventPayload.containsKey("message_id") &&
                   eventPayload.containsKey("to") &&
                   eventPayload.containsKey("text") &&
                   eventPayload.get("to") instanceof String &&
                   eventPayload.get("text") instanceof String;
        } catch (Exception e) {
            log.warn("Error validating SMS payload: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Obtiene estadísticas específicas para SMS
     */
    private void recordSmsSpecificMetrics(Map<String, Object> eventPayload) {
        try {
            String text = (String) eventPayload.get("text");
            if (text != null) {
                int length = text.length();
                
                // Registrar distribución de longitud de SMS
                if (length <= 160) {
                    meterRegistry.counter("sms.length.distribution", "range", "single").increment();
                } else if (length <= 306) {
                    meterRegistry.counter("sms.length.distribution", "range", "double").increment();
                } else {
                    meterRegistry.counter("sms.length.distribution", "range", "multiple").increment();
                }
                
                // Registrar longitud promedio
                meterRegistry.gauge("sms.text.length", length);
            }
        } catch (Exception e) {
            log.debug("Could not record SMS specific metrics: {}", e.getMessage());
        }
    }

    /**
     * Excepción para errores de procesamiento de notificaciones SMS
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