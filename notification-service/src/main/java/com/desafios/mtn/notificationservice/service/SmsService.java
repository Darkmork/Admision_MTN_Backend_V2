package com.desafios.mtn.notificationservice.service;

import com.desafios.mtn.notificationservice.config.NotificationProperties;
import com.desafios.mtn.notificationservice.domain.DeliveryAttempt;
import com.desafios.mtn.notificationservice.domain.Message;
import com.desafios.mtn.notificationservice.service.TemplateRenderingService.RenderedTemplate;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.MessageCreator;
import com.twilio.type.PhoneNumber;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Servicio para envío de SMS usando Twilio
 */
@Service
@ConditionalOnProperty(name = "notification.sms.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class SmsService {

    private final NotificationProperties notificationProperties;
    private final MeterRegistry meterRegistry;
    private final RateLimitingService rateLimitingService;

    // Patrón para validar números chilenos
    private static final Pattern CHILEAN_PHONE_PATTERN = Pattern.compile("^\\+56\\s?9\\s?\\d{4}\\s?\\d{4}$");

    @PostConstruct
    public void initTwilio() {
        if (!notificationProperties.getSms().isMockMode()) {
            String accountSid = notificationProperties.getSms().getTwilio().getAccountSid();
            String authToken = notificationProperties.getSms().getTwilio().getAuthToken();
            
            if (accountSid != null && authToken != null && !accountSid.trim().isEmpty() && !authToken.trim().isEmpty()) {
                Twilio.init(accountSid, authToken);
                log.info("Twilio initialized successfully");
            } else {
                log.warn("Twilio credentials not configured, SMS will only work in mock mode");
            }
        }
    }

    /**
     * Envía un SMS renderizado
     */
    public DeliveryAttempt sendSms(Message message, RenderedTemplate renderedTemplate, int attemptNumber) {
        String messageId = message.getId().toString();
        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            log.info("Sending SMS for message {} (attempt {})", messageId.substring(0, 8), attemptNumber);
            
            // Obtener número de teléfono
            String phoneNumber = (String) message.getToJson();
            
            // Validar número
            if (!isValidChileanPhoneNumber(phoneNumber)) {
                throw new SmsDeliveryException("Invalid Chilean phone number: " + phoneNumber);
            }
            
            // Verificar rate limiting
            if (!rateLimitingService.isAllowed("sms", phoneNumber)) {
                throw new SmsDeliveryException("Rate limit exceeded for number: " + maskPhoneNumber(phoneNumber));
            }
            
            // Validar longitud del mensaje
            String text = renderedTemplate.getBodyText();
            if (text.length() > 160) {
                log.warn("SMS text length {} exceeds 160 characters for message {}", 
                        text.length(), messageId.substring(0, 8));
                text = truncateSmsText(text);
            }
            
            // Verificar mock mode
            if (notificationProperties.getSms().isMockMode()) {
                return handleMockSms(message, phoneNumber, text, attemptNumber, sample);
            }
            
            // Envío real
            return sendRealSms(message, phoneNumber, text, attemptNumber, sample);
            
        } catch (SmsDeliveryException e) {
            log.error("SMS delivery failed for message {}: {}", messageId.substring(0, 8), e.getMessage());
            return createFailedAttempt(message.getId(), attemptNumber, e.getMessage(), sample);
            
        } catch (Exception e) {
            log.error("Unexpected error sending SMS for message {}: {}", messageId.substring(0, 8), e.getMessage(), e);
            return createFailedAttempt(message.getId(), attemptNumber, "Unexpected error: " + e.getMessage(), sample);
        }
    }

    /**
     * Maneja el envío simulado en modo mock
     */
    private DeliveryAttempt handleMockSms(Message message, String phoneNumber, String text, 
                                         int attemptNumber, Timer.Sample sample) {
        
        // Simular tiempo de envío
        try {
            Thread.sleep(50 + (int)(Math.random() * 100)); // 50-150ms
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Calcular número de segmentos
        int segments = calculateSmsSegments(text);
        
        // Log detallado del SMS simulado
        log.info("📱 MOCK SMS SENT:");
        log.info("  📞 To: {}", maskPhoneNumber(phoneNumber));
        log.info("  📝 Text: {}", text);
        log.info("  📏 Length: {} chars, {} segments", text.length(), segments);
        log.info("  🔗 Message ID: {}", message.getId().toString().substring(0, 8));
        
        // Registrar métricas
        long duration = sample.stop(Timer.builder("notification.send.duration")
                .tag("channel", "sms")
                .tag("provider", "mock")
                .register(meterRegistry));
                
        meterRegistry.counter("notifications.sent.total", 
                "channel", "sms", "provider", "mock").increment();
        
        return DeliveryAttempt.createSuccessfulSmsAttempt(
                message.getId(), attemptNumber, duration / 1_000_000, segments,
                "SMS delivered successfully (MOCK MODE)");
    }

    /**
     * Envía SMS real usando Twilio
     */
    private DeliveryAttempt sendRealSms(Message message, String phoneNumber, String text, 
                                       int attemptNumber, Timer.Sample sample) {
        try {
            String fromNumber = notificationProperties.getSms().getTwilio().getFromNumber();
            if (fromNumber == null || fromNumber.trim().isEmpty()) {
                throw new SmsDeliveryException("Twilio from number not configured");
            }
            
            // Normalizar números para Twilio
            PhoneNumber to = new PhoneNumber(normalizePhoneForTwilio(phoneNumber));
            PhoneNumber from = new PhoneNumber(fromNumber);
            
            // Crear mensaje
            MessageCreator messageCreator = com.twilio.rest.api.v2010.account.Message.creator(to, from, text);
            
            // Configurar callback URLs si están disponibles
            // messageCreator.setStatusCallback(URI.create("https://your-domain.com/sms/status"));
            
            long startTime = System.nanoTime();
            com.twilio.rest.api.v2010.account.Message twilioMessage = messageCreator.create();
            long duration = System.nanoTime() - startTime;
            
            // Registrar métricas
            sample.stop(Timer.builder("notification.send.duration")
                    .tag("channel", "sms")
                    .tag("provider", "twilio")
                    .register(meterRegistry));
                    
            meterRegistry.counter("notifications.sent.total", 
                    "channel", "sms", "provider", "twilio").increment();
            
            log.info("SMS sent successfully for message {} via Twilio (SID: {})", 
                    message.getId().toString().substring(0, 8), twilioMessage.getSid());
            
            int segments = calculateSmsSegments(text);
            
            return DeliveryAttempt.createSuccessfulSmsAttempt(
                    message.getId(), attemptNumber, duration / 1_000_000, segments,
                    String.format("Twilio SID: %s, Status: %s", 
                                 twilioMessage.getSid(), twilioMessage.getStatus()));
                                 
        } catch (com.twilio.exception.TwilioException e) {
            log.error("Twilio SMS error for message {}: {}", 
                     message.getId().toString().substring(0, 8), e.getMessage());
            return createFailedAttempt(message.getId(), attemptNumber, 
                                     "Twilio error: " + e.getMessage(), sample);
                                     
        } catch (Exception e) {
            log.error("SMS delivery error for message {}: {}", 
                     message.getId().toString().substring(0, 8), e.getMessage());
            return createFailedAttempt(message.getId(), attemptNumber, 
                                     "SMS delivery error: " + e.getMessage(), sample);
        }
    }

    /**
     * Valida un SMS antes del envío
     */
    public void validateSms(Message message, RenderedTemplate renderedTemplate) {
        if (message.getToJson() == null) {
            throw new SmsDeliveryException("No phone number specified");
        }
        
        String phoneNumber = (String) message.getToJson();
        
        if (!isValidChileanPhoneNumber(phoneNumber)) {
            throw new SmsDeliveryException("Invalid Chilean phone number: " + phoneNumber);
        }
        
        if (renderedTemplate.getBodyText() == null || renderedTemplate.getBodyText().trim().isEmpty()) {
            throw new SmsDeliveryException("SMS text is required");
        }
        
        if (renderedTemplate.getBodyText().length() > 160) {
            log.warn("SMS text length {} exceeds 160 characters, will be truncated", 
                    renderedTemplate.getBodyText().length());
        }
    }

    /**
     * Verifica si un número de teléfono chileno es válido
     */
    private boolean isValidChileanPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }
        
        // Limpiar espacios y guiones
        String cleaned = phoneNumber.replaceAll("\\s|-", "");
        
        // Verificar formato básico
        return CHILEAN_PHONE_PATTERN.matcher(phoneNumber).matches() ||
               cleaned.matches("^\\+569\\d{8}$") ||
               cleaned.matches("^569\\d{8}$") ||
               cleaned.matches("^9\\d{8}$");
    }

    /**
     * Normaliza número de teléfono para Twilio
     */
    private String normalizePhoneForTwilio(String phoneNumber) {
        // Limpiar número
        String cleaned = phoneNumber.replaceAll("[^0-9+]", "");
        
        // Si ya tiene +56, usar tal como está
        if (cleaned.startsWith("+56")) {
            return cleaned;
        }
        
        // Si empieza con 56, agregar +
        if (cleaned.startsWith("56") && cleaned.length() == 11) {
            return "+" + cleaned;
        }
        
        // Si es un número de 9 dígitos que empieza con 9, agregar +56
        if (cleaned.startsWith("9") && cleaned.length() == 9) {
            return "+56" + cleaned;
        }
        
        // Si es un número de 8 dígitos, asumir que le falta el 9 inicial
        if (cleaned.length() == 8) {
            return "+569" + cleaned;
        }
        
        return phoneNumber; // Devolver original si no se puede normalizar
    }

    /**
     * Calcula el número de segmentos SMS
     */
    private int calculateSmsSegments(String text) {
        if (text == null) return 0;
        
        int length = text.length();
        if (length <= 160) return 1;
        if (length <= 306) return 2;
        if (length <= 459) return 3;
        
        // Para mensajes muy largos, calcular segmentos
        return (int) Math.ceil(length / 153.0);
    }

    /**
     * Trunca texto SMS manteniendo palabras completas cuando es posible
     */
    private String truncateSmsText(String text) {
        if (text.length() <= 160) {
            return text;
        }
        
        // Intentar truncar en un espacio
        int lastSpace = text.lastIndexOf(' ', 157);
        if (lastSpace > 140) { // Solo si no queda muy corto
            return text.substring(0, lastSpace) + "...";
        }
        
        // Truncar directamente
        return text.substring(0, 157) + "...";
    }

    /**
     * Enmascara número de teléfono para logs
     */
    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 4) {
            return "***";
        }
        return "***" + phoneNumber.substring(phoneNumber.length() - 4);
    }

    /**
     * Crea un intento fallido
     */
    private DeliveryAttempt createFailedAttempt(UUID messageId, int attemptNumber, 
                                               String errorMessage, Timer.Sample sample) {
        long duration = sample.stop(Timer.builder("notification.send.duration")
                .tag("channel", "sms")
                .tag("provider", "twilio")
                .tag("status", "failed")
                .register(meterRegistry));
                
        meterRegistry.counter("notifications.failed.total", 
                "channel", "sms", "reason", "delivery_error").increment();
        
        return DeliveryAttempt.createFailedAttempt(
                messageId, attemptNumber, duration / 1_000_000, 
                "SMS_DELIVERY_ERROR", errorMessage, null);
    }

    /**
     * Obtiene estadísticas del servicio de SMS
     */
    public SmsServiceStats getStats() {
        return SmsServiceStats.builder()
                .totalSent(0L) // Obtener de métricas
                .totalFailed(0L) // Obtener de métricas
                .averageDeliveryTime(0.0) // Obtener de métricas
                .mockModeEnabled(notificationProperties.getSms().isMockMode())
                .rateLimitEnabled(notificationProperties.getSms().getRateLimit().isEnabled())
                .provider(notificationProperties.getSms().getProvider())
                .build();
    }

    // ======================
    // CLASSES & EXCEPTIONS
    // ======================

    /**
     * Excepción para errores de entrega de SMS
     */
    public static class SmsDeliveryException extends RuntimeException {
        public SmsDeliveryException(String message) {
            super(message);
        }

        public SmsDeliveryException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Estadísticas del servicio de SMS
     */
    public static class SmsServiceStats {
        private final long totalSent;
        private final long totalFailed;
        private final double averageDeliveryTime;
        private final boolean mockModeEnabled;
        private final boolean rateLimitEnabled;
        private final String provider;

        private SmsServiceStats(SmsServiceStatsBuilder builder) {
            this.totalSent = builder.totalSent;
            this.totalFailed = builder.totalFailed;
            this.averageDeliveryTime = builder.averageDeliveryTime;
            this.mockModeEnabled = builder.mockModeEnabled;
            this.rateLimitEnabled = builder.rateLimitEnabled;
            this.provider = builder.provider;
        }

        public static SmsServiceStatsBuilder builder() {
            return new SmsServiceStatsBuilder();
        }

        // Getters
        public long getTotalSent() { return totalSent; }
        public long getTotalFailed() { return totalFailed; }
        public double getAverageDeliveryTime() { return averageDeliveryTime; }
        public boolean isMockModeEnabled() { return mockModeEnabled; }
        public boolean isRateLimitEnabled() { return rateLimitEnabled; }
        public String getProvider() { return provider; }

        public static class SmsServiceStatsBuilder {
            private long totalSent;
            private long totalFailed;
            private double averageDeliveryTime;
            private boolean mockModeEnabled;
            private boolean rateLimitEnabled;
            private String provider;

            public SmsServiceStatsBuilder totalSent(long totalSent) {
                this.totalSent = totalSent;
                return this;
            }

            public SmsServiceStatsBuilder totalFailed(long totalFailed) {
                this.totalFailed = totalFailed;
                return this;
            }

            public SmsServiceStatsBuilder averageDeliveryTime(double averageDeliveryTime) {
                this.averageDeliveryTime = averageDeliveryTime;
                return this;
            }

            public SmsServiceStatsBuilder mockModeEnabled(boolean mockModeEnabled) {
                this.mockModeEnabled = mockModeEnabled;
                return this;
            }

            public SmsServiceStatsBuilder rateLimitEnabled(boolean rateLimitEnabled) {
                this.rateLimitEnabled = rateLimitEnabled;
                return this;
            }

            public SmsServiceStatsBuilder provider(String provider) {
                this.provider = provider;
                return this;
            }

            public SmsServiceStats build() {
                return new SmsServiceStats(this);
            }
        }
    }
}