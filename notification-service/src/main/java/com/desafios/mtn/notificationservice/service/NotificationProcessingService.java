package com.desafios.mtn.notificationservice.service;

import com.desafios.mtn.notificationservice.domain.DeliveryAttempt;
import com.desafios.mtn.notificationservice.domain.Message;
import com.desafios.mtn.notificationservice.domain.Template;
import com.desafios.mtn.notificationservice.repository.DeliveryAttemptRepository;
import com.desafios.mtn.notificationservice.repository.MessageRepository;
import com.desafios.mtn.notificationservice.repository.TemplateRepository;
import com.desafios.mtn.notificationservice.service.TemplateRenderingService.RenderedTemplate;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Servicio principal para procesamiento de notificaciones
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationProcessingService {

    private final MessageRepository messageRepository;
    private final TemplateRepository templateRepository;
    private final DeliveryAttemptRepository deliveryAttemptRepository;
    
    private final TemplateRenderingService templateRenderingService;
    private final EmailService emailService;
    private final SmsService smsService;
    private final RateLimitingService rateLimitingService;
    
    private final MeterRegistry meterRegistry;

    /**
     * Procesa una notificación de email
     */
    @Transactional
    public boolean processEmailNotification(Map<String, Object> eventPayload) {
        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            // Validar payload
            if (!isValidEmailPayload(eventPayload)) {
                throw new NotificationProcessingException("Invalid email event payload");
            }

            // Verificar idempotencia
            String idempotencyKey = (String) eventPayload.get("idempotency_key");
            if (idempotencyKey != null && messageRepository.existsByIdempotencyKey(idempotencyKey)) {
                log.info("Duplicate email notification ignored: idempotencyKey={}", idempotencyKey);
                return true; // Ya procesado exitosamente
            }

            // Crear o recuperar mensaje
            Message message = getOrCreateMessage(eventPayload, Template.NotificationChannel.email);
            
            // Determinar número de intento
            int attemptNumber = message.getAttemptCount() + 1;
            
            log.info("Processing email notification: messageId={}, attempt={}", 
                    message.getId().toString().substring(0, 8), attemptNumber);

            // Marcar como en procesamiento
            message.markAsProcessing();
            messageRepository.save(message);

            // Renderizar template o usar contenido directo
            RenderedTemplate renderedTemplate = renderEmailTemplate(eventPayload);

            // Validar email antes del envío
            emailService.validateEmail(message, renderedTemplate);

            // Enviar email
            DeliveryAttempt attempt = emailService.sendEmail(message, renderedTemplate, attemptNumber);
            
            // Guardar intento de entrega
            deliveryAttemptRepository.save(attempt);

            // Actualizar mensaje según el resultado
            if (attempt.wasSuccessful()) {
                message.markAsSent(attempt.getProviderResponse());
                
                // Registrar uso en rate limiting
                rateLimitingService.recordUsage("email", message.getPrimaryRecipient(), 
                                               (String) eventPayload.get("template_id"));
                                               
                log.info("✅ Email notification sent successfully: messageId={}, to={}", 
                        message.getId().toString().substring(0, 8), message.getMaskedRecipient());
                        
            } else {
                message.markAsFailed(attempt.getErrorMessage());
                log.warn("⚠️ Email notification failed: messageId={}, error={}", 
                        message.getId().toString().substring(0, 8), attempt.getErrorSummary());
            }

            // Guardar mensaje actualizado
            messageRepository.save(message);

            // Registrar métricas
            sample.stop(Timer.builder("notification.processing.duration")
                    .tag("channel", "email")
                    .tag("status", attempt.wasSuccessful() ? "success" : "failed")
                    .register(meterRegistry));

            return attempt.wasSuccessful();

        } catch (Exception e) {
            log.error("Error processing email notification: {}", e.getMessage(), e);
            
            sample.stop(Timer.builder("notification.processing.duration")
                    .tag("channel", "email")
                    .tag("status", "error")
                    .register(meterRegistry));
                    
            meterRegistry.counter("notification.processing.errors.total", 
                    "channel", "email").increment();
            
            return false;
        }
    }

    /**
     * Procesa una notificación de SMS
     */
    @Transactional
    public boolean processSmsNotification(Map<String, Object> eventPayload) {
        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            // Validar payload
            if (!isValidSmsPayload(eventPayload)) {
                throw new NotificationProcessingException("Invalid SMS event payload");
            }

            // Verificar idempotencia
            String idempotencyKey = (String) eventPayload.get("idempotency_key");
            if (idempotencyKey != null && messageRepository.existsByIdempotencyKey(idempotencyKey)) {
                log.info("Duplicate SMS notification ignored: idempotencyKey={}", idempotencyKey);
                return true; // Ya procesado exitosamente
            }

            // Crear o recuperar mensaje
            Message message = getOrCreateMessage(eventPayload, Template.NotificationChannel.sms);
            
            // Determinar número de intento
            int attemptNumber = message.getAttemptCount() + 1;
            
            log.info("Processing SMS notification: messageId={}, attempt={}", 
                    message.getId().toString().substring(0, 8), attemptNumber);

            // Marcar como en procesamiento
            message.markAsProcessing();
            messageRepository.save(message);

            // Renderizar contenido (SMS siempre usa contenido directo)
            RenderedTemplate renderedTemplate = renderSmsContent(eventPayload);

            // Validar SMS antes del envío
            smsService.validateSms(message, renderedTemplate);

            // Enviar SMS
            DeliveryAttempt attempt = smsService.sendSms(message, renderedTemplate, attemptNumber);
            
            // Guardar intento de entrega
            deliveryAttemptRepository.save(attempt);

            // Actualizar mensaje según el resultado
            if (attempt.wasSuccessful()) {
                message.markAsSent(attempt.getProviderResponse());
                
                // Registrar uso en rate limiting
                rateLimitingService.recordUsage("sms", (String) eventPayload.get("to"), null);
                                               
                log.info("✅ SMS notification sent successfully: messageId={}, to={}", 
                        message.getId().toString().substring(0, 8), message.getMaskedRecipient());
                        
            } else {
                message.markAsFailed(attempt.getErrorMessage());
                log.warn("⚠️ SMS notification failed: messageId={}, error={}", 
                        message.getId().toString().substring(0, 8), attempt.getErrorSummary());
            }

            // Guardar mensaje actualizado
            messageRepository.save(message);

            // Registrar métricas
            sample.stop(Timer.builder("notification.processing.duration")
                    .tag("channel", "sms")
                    .tag("status", attempt.wasSuccessful() ? "success" : "failed")
                    .register(meterRegistry));

            return attempt.wasSuccessful();

        } catch (Exception e) {
            log.error("Error processing SMS notification: {}", e.getMessage(), e);
            
            sample.stop(Timer.builder("notification.processing.duration")
                    .tag("channel", "sms")
                    .tag("status", "error")
                    .register(meterRegistry));
                    
            meterRegistry.counter("notification.processing.errors.total", 
                    "channel", "sms").increment();
            
            return false;
        }
    }

    /**
     * Maneja mensajes que llegan a DLQ
     */
    @Transactional
    public void handleDlqMessage(String channel, Map<String, Object> eventPayload, String reason) {
        try {
            String messageId = (String) eventPayload.get("message_id");
            if (messageId == null) {
                log.warn("DLQ message without message_id: channel={}", channel);
                return;
            }

            UUID messageUuid = UUID.fromString(messageId);
            Optional<Message> messageOpt = messageRepository.findById(messageUuid);

            if (messageOpt.isPresent()) {
                Message message = messageOpt.get();
                message.markAsDlq(reason);
                messageRepository.save(message);
                
                log.info("Message marked as DLQ: messageId={}, channel={}, reason={}", 
                        messageId.substring(0, 8), channel, reason);
            } else {
                // Crear mensaje directamente en estado DLQ
                Message dlqMessage = "email".equals(channel) ? 
                        Message.fromEmailEvent(eventPayload) : 
                        Message.fromSmsEvent(eventPayload);
                dlqMessage.markAsDlq(reason);
                messageRepository.save(dlqMessage);
                
                log.info("New DLQ message created: messageId={}, channel={}, reason={}", 
                        messageId.substring(0, 8), channel, reason);
            }
            
            meterRegistry.counter("notifications.dlq.processed.total", "channel", channel).increment();
            
        } catch (Exception e) {
            log.error("Error handling DLQ message for channel {}: {}", channel, e.getMessage(), e);
        }
    }

    /**
     * Obtiene o crea un mensaje desde el evento
     */
    private Message getOrCreateMessage(Map<String, Object> eventPayload, Template.NotificationChannel channel) {
        String messageId = (String) eventPayload.get("message_id");
        UUID messageUuid = UUID.fromString(messageId);

        return messageRepository.findById(messageUuid).orElseGet(() -> {
            Message message = channel == Template.NotificationChannel.email ? 
                    Message.fromEmailEvent(eventPayload) : 
                    Message.fromSmsEvent(eventPayload);
            return messageRepository.save(message);
        });
    }

    /**
     * Renderiza template de email
     */
    private RenderedTemplate renderEmailTemplate(Map<String, Object> eventPayload) {
        String templateId = (String) eventPayload.get("template_id");
        
        if (templateId != null) {
            // Usar template
            Optional<Template> templateOpt = templateRepository.findByIdAndActiveTrue(templateId);
            if (templateOpt.isEmpty()) {
                throw new NotificationProcessingException("Email template not found: " + templateId);
            }
            
            Template template = templateOpt.get();
            @SuppressWarnings("unchecked")
            Map<String, Object> variables = (Map<String, Object>) eventPayload.get("variables");
            
            return templateRenderingService.renderTemplate(template, variables);
        } else {
            // Contenido directo
            return RenderedTemplate.builder()
                    .templateId("direct")
                    .channel(Template.NotificationChannel.email)
                    .subject((String) eventPayload.get("subject"))
                    .bodyText((String) eventPayload.get("body_text"))
                    .bodyHtml((String) eventPayload.get("body_html"))
                    .build();
        }
    }

    /**
     * Renderiza contenido de SMS
     */
    private RenderedTemplate renderSmsContent(Map<String, Object> eventPayload) {
        return RenderedTemplate.builder()
                .templateId("direct")
                .channel(Template.NotificationChannel.sms)
                .bodyText((String) eventPayload.get("text"))
                .build();
    }

    /**
     * Valida payload de evento de email
     */
    private boolean isValidEmailPayload(Map<String, Object> eventPayload) {
        return eventPayload.containsKey("message_id") &&
               eventPayload.containsKey("to") &&
               eventPayload.containsKey("subject") &&
               (eventPayload.containsKey("template_id") || 
                eventPayload.containsKey("body_text") || 
                eventPayload.containsKey("body_html"));
    }

    /**
     * Valida payload de evento de SMS
     */
    private boolean isValidSmsPayload(Map<String, Object> eventPayload) {
        return eventPayload.containsKey("message_id") &&
               eventPayload.containsKey("to") &&
               eventPayload.containsKey("text");
    }

    /**
     * Obtiene estadísticas de procesamiento
     */
    public ProcessingStats getProcessingStats() {
        long totalMessages = messageRepository.count();
        long sentMessages = messageRepository.countByStatus(Message.MessageStatus.SENT);
        long failedMessages = messageRepository.countByStatus(Message.MessageStatus.FAILED);
        long dlqMessages = messageRepository.countByStatus(Message.MessageStatus.DLQ);
        
        Double avgDeliveryTime = messageRepository.getAverageDeliveryTimeInSeconds();
        
        return ProcessingStats.builder()
                .totalMessages(totalMessages)
                .sentMessages(sentMessages)
                .failedMessages(failedMessages)
                .dlqMessages(dlqMessages)
                .averageDeliveryTimeSeconds(avgDeliveryTime != null ? avgDeliveryTime : 0.0)
                .successRate(totalMessages > 0 ? (double) sentMessages / totalMessages * 100 : 0.0)
                .build();
    }

    // ======================
    // CLASSES & EXCEPTIONS
    // ======================

    /**
     * Excepción para errores de procesamiento
     */
    public static class NotificationProcessingException extends RuntimeException {
        public NotificationProcessingException(String message) {
            super(message);
        }

        public NotificationProcessingException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Estadísticas de procesamiento
     */
    public static class ProcessingStats {
        private final long totalMessages;
        private final long sentMessages;
        private final long failedMessages;
        private final long dlqMessages;
        private final double averageDeliveryTimeSeconds;
        private final double successRate;

        private ProcessingStats(ProcessingStatsBuilder builder) {
            this.totalMessages = builder.totalMessages;
            this.sentMessages = builder.sentMessages;
            this.failedMessages = builder.failedMessages;
            this.dlqMessages = builder.dlqMessages;
            this.averageDeliveryTimeSeconds = builder.averageDeliveryTimeSeconds;
            this.successRate = builder.successRate;
        }

        public static ProcessingStatsBuilder builder() {
            return new ProcessingStatsBuilder();
        }

        // Getters
        public long getTotalMessages() { return totalMessages; }
        public long getSentMessages() { return sentMessages; }
        public long getFailedMessages() { return failedMessages; }
        public long getDlqMessages() { return dlqMessages; }
        public double getAverageDeliveryTimeSeconds() { return averageDeliveryTimeSeconds; }
        public double getSuccessRate() { return successRate; }

        public static class ProcessingStatsBuilder {
            private long totalMessages;
            private long sentMessages;
            private long failedMessages;
            private long dlqMessages;
            private double averageDeliveryTimeSeconds;
            private double successRate;

            public ProcessingStatsBuilder totalMessages(long totalMessages) {
                this.totalMessages = totalMessages;
                return this;
            }

            public ProcessingStatsBuilder sentMessages(long sentMessages) {
                this.sentMessages = sentMessages;
                return this;
            }

            public ProcessingStatsBuilder failedMessages(long failedMessages) {
                this.failedMessages = failedMessages;
                return this;
            }

            public ProcessingStatsBuilder dlqMessages(long dlqMessages) {
                this.dlqMessages = dlqMessages;
                return this;
            }

            public ProcessingStatsBuilder averageDeliveryTimeSeconds(double averageDeliveryTimeSeconds) {
                this.averageDeliveryTimeSeconds = averageDeliveryTimeSeconds;
                return this;
            }

            public ProcessingStatsBuilder successRate(double successRate) {
                this.successRate = successRate;
                return this;
            }

            public ProcessingStats build() {
                return new ProcessingStats(this);
            }
        }
    }
}