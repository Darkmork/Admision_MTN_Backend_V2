package com.desafios.mtn.notificationservice.service;

import com.desafios.mtn.notificationservice.config.NotificationProperties;
import com.desafios.mtn.notificationservice.domain.DeliveryAttempt;
import com.desafios.mtn.notificationservice.domain.Message;
import com.desafios.mtn.notificationservice.service.TemplateRenderingService.RenderedTemplate;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Servicio para envío de emails
 */
@Service
@ConditionalOnProperty(name = "notification.email.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final NotificationProperties notificationProperties;
    private final MeterRegistry meterRegistry;
    private final RateLimitingService rateLimitingService;

    /**
     * Envía un email renderizado
     */
    public DeliveryAttempt sendEmail(Message message, RenderedTemplate renderedTemplate, int attemptNumber) {
        String messageId = message.getId().toString();
        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            log.info("Sending email for message {} (attempt {})", messageId.substring(0, 8), attemptNumber);
            
            // Verificar rate limiting
            if (!rateLimitingService.isAllowed("email", message.getPrimaryRecipient())) {
                throw new EmailDeliveryException("Rate limit exceeded for recipient: " + message.getMaskedRecipient());
            }
            
            // Obtener destinatarios
            @SuppressWarnings("unchecked")
            List<String> recipients = (List<String>) message.getToJson();
            
            if (recipients.isEmpty()) {
                throw new EmailDeliveryException("No recipients specified");
            }
            
            // Verificar mock mode
            if (notificationProperties.getEmail().isMockMode()) {
                return handleMockEmail(message, renderedTemplate, attemptNumber, sample);
            }
            
            // Envío real
            return sendRealEmail(message, renderedTemplate, recipients, attemptNumber, sample);
            
        } catch (EmailDeliveryException e) {
            log.error("Email delivery failed for message {}: {}", messageId.substring(0, 8), e.getMessage());
            return createFailedAttempt(message.getId(), attemptNumber, e.getMessage(), sample);
            
        } catch (Exception e) {
            log.error("Unexpected error sending email for message {}: {}", messageId.substring(0, 8), e.getMessage(), e);
            return createFailedAttempt(message.getId(), attemptNumber, "Unexpected error: " + e.getMessage(), sample);
        }
    }

    /**
     * Maneja el envío simulado en modo mock
     */
    private DeliveryAttempt handleMockEmail(Message message, RenderedTemplate renderedTemplate, 
                                          int attemptNumber, Timer.Sample sample) {
        
        @SuppressWarnings("unchecked")
        List<String> recipients = (List<String>) message.getToJson();
        
        // Simular tiempo de envío
        try {
            Thread.sleep(100 + (int)(Math.random() * 200)); // 100-300ms
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Log detallado del email simulado
        log.info("📧 MOCK EMAIL SENT:");
        log.info("  📬 To: {}", String.join(", ", recipients));
        log.info("  📋 Subject: {}", renderedTemplate.getSubject());
        log.info("  📝 Template: {}", renderedTemplate.getTemplateId());
        log.info("  🔗 Message ID: {}", message.getId().toString().substring(0, 8));
        log.info("  📄 Body Text: {}", truncateForLog(renderedTemplate.getBodyText()));
        
        if (renderedTemplate.getBodyHtml() != null) {
            log.info("  🌐 Body HTML: {}", truncateForLog(renderedTemplate.getBodyHtml()));
        }
        
        // Registrar métricas
        long duration = sample.stop(Timer.builder("notification.send.duration")
                .tag("channel", "email")
                .tag("provider", "mock")
                .register(meterRegistry));
                
        meterRegistry.counter("notifications.sent.total", 
                "channel", "email", "provider", "mock").increment();
        
        // Generar ID de proveedor simulado
        String providerMessageId = "mock-smtp-" + UUID.randomUUID().toString().substring(0, 8);
        
        return DeliveryAttempt.createSuccessfulEmailAttempt(
                message.getId(), attemptNumber, duration / 1_000_000, 250, 
                "250 2.0.0 Message accepted for delivery (MOCK MODE)");
    }

    /**
     * Envía email real usando JavaMailSender
     */
    private DeliveryAttempt sendRealEmail(Message message, RenderedTemplate renderedTemplate,
                                        List<String> recipients, int attemptNumber, Timer.Sample sample) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            
            // Configurar remitente
            helper.setFrom(notificationProperties.getEmail().getFromAddress(), 
                          notificationProperties.getEmail().getFromName());
            
            // Configurar reply-to si está disponible
            if (notificationProperties.getEmail().getReplyTo() != null) {
                helper.setReplyTo(notificationProperties.getEmail().getReplyTo());
            }
            
            // Configurar destinatarios
            if (recipients.size() > notificationProperties.getEmail().getMaxRecipients()) {
                throw new EmailDeliveryException(
                    String.format("Too many recipients: %d (max: %d)", 
                                 recipients.size(), notificationProperties.getEmail().getMaxRecipients()));
            }
            
            helper.setTo(recipients.toArray(new String[0]));
            helper.setSubject(renderedTemplate.getSubject());
            
            // Configurar contenido
            if (renderedTemplate.getBodyHtml() != null && renderedTemplate.getBodyText() != null) {
                // Email multipart con HTML y texto
                helper.setText(renderedTemplate.getBodyText(), renderedTemplate.getBodyHtml());
            } else if (renderedTemplate.getBodyHtml() != null) {
                // Solo HTML
                helper.setText(renderedTemplate.getBodyHtml(), true);
            } else {
                // Solo texto
                helper.setText(renderedTemplate.getBodyText(), false);
            }
            
            // Headers personalizados para tracking
            mimeMessage.setHeader("X-Message-ID", message.getId().toString());
            mimeMessage.setHeader("X-Template-ID", renderedTemplate.getTemplateId());
            mimeMessage.setHeader("X-MTN-System", "Admission-Notifications");
            
            if (message.getCorrelationId() != null) {
                mimeMessage.setHeader("X-Correlation-ID", message.getCorrelationId());
            }
            
            // Enviar
            long startTime = System.nanoTime();
            mailSender.send(mimeMessage);
            long duration = System.nanoTime() - startTime;
            
            // Registrar métricas
            sample.stop(Timer.builder("notification.send.duration")
                    .tag("channel", "email")
                    .tag("provider", "smtp")
                    .register(meterRegistry));
                    
            meterRegistry.counter("notifications.sent.total", 
                    "channel", "email", "provider", "smtp").increment();
            
            log.info("Email sent successfully for message {} to {} recipients", 
                    message.getId().toString().substring(0, 8), recipients.size());
                    
            // Simular respuesta SMTP exitosa
            String providerMessageId = generateProviderMessageId();
            
            return DeliveryAttempt.createSuccessfulEmailAttempt(
                    message.getId(), attemptNumber, duration / 1_000_000, 250, 
                    "250 2.0.0 Message queued for delivery");
                    
        } catch (MessagingException e) {
            log.error("SMTP messaging error for message {}: {}", 
                     message.getId().toString().substring(0, 8), e.getMessage());
            return createFailedAttempt(message.getId(), attemptNumber, 
                                     "SMTP error: " + e.getMessage(), sample);
                                     
        } catch (MailException e) {
            log.error("Mail delivery error for message {}: {}", 
                     message.getId().toString().substring(0, 8), e.getMessage());
            return createFailedAttempt(message.getId(), attemptNumber, 
                                     "Mail delivery error: " + e.getMessage(), sample);
        }
    }

    /**
     * Valida un email antes del envío
     */
    public void validateEmail(Message message, RenderedTemplate renderedTemplate) {
        if (message.getToJson() == null) {
            throw new EmailDeliveryException("No recipients specified");
        }
        
        @SuppressWarnings("unchecked")
        List<String> recipients = (List<String>) message.getToJson();
        
        if (recipients.isEmpty()) {
            throw new EmailDeliveryException("Recipients list is empty");
        }
        
        // Validar formato de emails
        for (String email : recipients) {
            if (!isValidEmail(email)) {
                throw new EmailDeliveryException("Invalid email format: " + email);
            }
        }
        
        if (renderedTemplate.getSubject() == null || renderedTemplate.getSubject().trim().isEmpty()) {
            throw new EmailDeliveryException("Email subject is required");
        }
        
        if (renderedTemplate.getBodyText() == null && renderedTemplate.getBodyHtml() == null) {
            throw new EmailDeliveryException("Email body is required");
        }
    }

    /**
     * Verifica si un email tiene formato válido
     */
    private boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        
        // Validación básica de formato de email
        String emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        return email.matches(emailRegex);
    }

    /**
     * Genera un ID de proveedor simulado
     */
    private String generateProviderMessageId() {
        return "smtp-" + System.currentTimeMillis() + "-" + 
               UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Crea un intento fallido
     */
    private DeliveryAttempt createFailedAttempt(UUID messageId, int attemptNumber, 
                                               String errorMessage, Timer.Sample sample) {
        long duration = sample.stop(Timer.builder("notification.send.duration")
                .tag("channel", "email")
                .tag("provider", "smtp")
                .tag("status", "failed")
                .register(meterRegistry));
                
        meterRegistry.counter("notifications.failed.total", 
                "channel", "email", "reason", "delivery_error").increment();
        
        return DeliveryAttempt.createFailedAttempt(
                messageId, attemptNumber, duration / 1_000_000, 
                "EMAIL_DELIVERY_ERROR", errorMessage, null);
    }

    /**
     * Trunca texto para logs
     */
    private String truncateForLog(String text) {
        if (text == null) return "null";
        if (text.length() <= 200) return text;
        return text.substring(0, 200) + "...";
    }

    /**
     * Obtiene estadísticas del servicio de email
     */
    public EmailServiceStats getStats() {
        // Estas métricas serían obtenidas del MeterRegistry
        return EmailServiceStats.builder()
                .totalSent(0L) // Obtener de métricas
                .totalFailed(0L) // Obtener de métricas
                .averageDeliveryTime(0.0) // Obtener de métricas
                .mockModeEnabled(notificationProperties.getEmail().isMockMode())
                .rateLimitEnabled(notificationProperties.getEmail().getRateLimit().isEnabled())
                .maxRecipientsPerEmail(notificationProperties.getEmail().getMaxRecipients())
                .build();
    }

    // ======================
    // CLASSES & EXCEPTIONS
    // ======================

    /**
     * Excepción para errores de entrega de email
     */
    public static class EmailDeliveryException extends RuntimeException {
        public EmailDeliveryException(String message) {
            super(message);
        }

        public EmailDeliveryException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Estadísticas del servicio de email
     */
    public static class EmailServiceStats {
        private final long totalSent;
        private final long totalFailed;
        private final double averageDeliveryTime;
        private final boolean mockModeEnabled;
        private final boolean rateLimitEnabled;
        private final int maxRecipientsPerEmail;

        private EmailServiceStats(EmailServiceStatsBuilder builder) {
            this.totalSent = builder.totalSent;
            this.totalFailed = builder.totalFailed;
            this.averageDeliveryTime = builder.averageDeliveryTime;
            this.mockModeEnabled = builder.mockModeEnabled;
            this.rateLimitEnabled = builder.rateLimitEnabled;
            this.maxRecipientsPerEmail = builder.maxRecipientsPerEmail;
        }

        public static EmailServiceStatsBuilder builder() {
            return new EmailServiceStatsBuilder();
        }

        // Getters
        public long getTotalSent() { return totalSent; }
        public long getTotalFailed() { return totalFailed; }
        public double getAverageDeliveryTime() { return averageDeliveryTime; }
        public boolean isMockModeEnabled() { return mockModeEnabled; }
        public boolean isRateLimitEnabled() { return rateLimitEnabled; }
        public int getMaxRecipientsPerEmail() { return maxRecipientsPerEmail; }

        public static class EmailServiceStatsBuilder {
            private long totalSent;
            private long totalFailed;
            private double averageDeliveryTime;
            private boolean mockModeEnabled;
            private boolean rateLimitEnabled;
            private int maxRecipientsPerEmail;

            public EmailServiceStatsBuilder totalSent(long totalSent) {
                this.totalSent = totalSent;
                return this;
            }

            public EmailServiceStatsBuilder totalFailed(long totalFailed) {
                this.totalFailed = totalFailed;
                return this;
            }

            public EmailServiceStatsBuilder averageDeliveryTime(double averageDeliveryTime) {
                this.averageDeliveryTime = averageDeliveryTime;
                return this;
            }

            public EmailServiceStatsBuilder mockModeEnabled(boolean mockModeEnabled) {
                this.mockModeEnabled = mockModeEnabled;
                return this;
            }

            public EmailServiceStatsBuilder rateLimitEnabled(boolean rateLimitEnabled) {
                this.rateLimitEnabled = rateLimitEnabled;
                return this;
            }

            public EmailServiceStatsBuilder maxRecipientsPerEmail(int maxRecipientsPerEmail) {
                this.maxRecipientsPerEmail = maxRecipientsPerEmail;
                return this;
            }

            public EmailServiceStats build() {
                return new EmailServiceStats(this);
            }
        }
    }
}