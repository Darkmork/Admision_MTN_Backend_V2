package com.desafios.mtn.notificationservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Propiedades de configuración para el notification-service
 */
@Component
@ConfigurationProperties(prefix = "notification")
@Data
public class NotificationProperties {

    private Email email = new Email();
    private Sms sms = new Sms();
    private Templates templates = new Templates();
    private Retry retry = new Retry();
    private Dlq dlq = new Dlq();

    @Data
    public static class Email {
        private boolean enabled = true;
        private boolean mockMode = true;
        private String fromName = "Sistema de Admisión MTN";
        private String fromAddress = "noreply@mtn.cl";
        private String replyTo = "soporte@mtn.cl";
        private int maxRecipients = 50;
        private long maxAttachmentSize = 10485760; // 10MB
        private RateLimit rateLimit = new RateLimit();
    }

    @Data
    public static class Sms {
        private boolean enabled = true;
        private boolean mockMode = true;
        private String provider = "twilio";
        private RateLimit rateLimit = new RateLimit();
        private Twilio twilio = new Twilio();
    }

    @Data
    public static class RateLimit {
        private boolean enabled = true;
        private int maxPerMinute = 60;
        private int maxPerHour = 500;
    }

    @Data
    public static class Twilio {
        private String accountSid;
        private String authToken;
        private String fromNumber;
    }

    @Data
    public static class Templates {
        private Cache cache = new Cache();
        private Reload reload = new Reload();
    }

    @Data
    public static class Cache {
        private boolean enabled = true;
        private int ttlMinutes = 30;
        private int maxSize = 100;
    }

    @Data
    public static class Reload {
        private boolean enabled = true;
        private int intervalSeconds = 300;
    }

    @Data
    public static class Retry {
        private int maxAttempts = 5;
        private long initialDelay = 1000;
        private long maxDelay = 300000;
        private double multiplier = 2.0;
    }

    @Data
    public static class Dlq {
        private boolean enabled = true;
        private int retentionDays = 30;
        private int alertThreshold = 10;
    }
}