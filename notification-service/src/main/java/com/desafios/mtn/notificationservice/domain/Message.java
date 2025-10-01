package com.desafios.mtn.notificationservice.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Entidad Message para el manejo de mensajes de notificación
 */
@Entity
@Table(name = "messages", indexes = {
    @Index(name = "idx_messages_status", columnList = "status"),
    @Index(name = "idx_messages_created_at", columnList = "created_at"),
    @Index(name = "idx_messages_channel", columnList = "channel"),
    @Index(name = "idx_messages_template_id", columnList = "template_id"),
    @Index(name = "idx_messages_correlation_id", columnList = "correlation_id"),
    @Index(name = "idx_messages_idempotency_key", columnList = "idempotency_key"),
    @Index(name = "idx_messages_source_service", columnList = "source_service"),
    @Index(name = "idx_messages_priority", columnList = "priority")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Message {

    @Id
    @EqualsAndHashCode.Include
    private UUID id; // message_id del evento

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationChannel channel;

    @Type(JsonType.class)
    @Column(name = "to_json", columnDefinition = "jsonb", nullable = false)
    private Object toJson; // List<String> para email, String para SMS

    @Column(name = "template_id")
    private String templateId;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> payload; // evento original completo

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private MessageStatus status = MessageStatus.RECEIVED;

    @Column(name = "attempt_count", nullable = false)
    @Builder.Default
    private Integer attemptCount = 0;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "provider_message_id")
    private String providerMessageId;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "sent_at")
    private Instant sentAt;

    // Campos adicionales para trazabilidad
    @Column(name = "correlation_id")
    private String correlationId;

    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @Column(name = "source_service")
    private String sourceService;

    // Campos de contexto
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private MessagePriority priority = MessagePriority.normal;

    @Column(name = "expires_at")
    private Instant expiresAt;

    // Relación con Template (opcional, puede ser null para contenido directo)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", insertable = false, updatable = false)
    private Template template;

    // ======================
    // ENUMS
    // ======================

    public enum NotificationChannel {
        email, sms
    }

    public enum MessageStatus {
        RECEIVED,    // Recibido, esperando procesamiento
        PROCESSING,  // En proceso de envío
        SENT,        // Enviado exitosamente
        FAILED,      // Fallo en el envío (reintentará)
        DLQ          // En Dead Letter Queue (fallo definitivo)
    }

    public enum MessagePriority {
        normal, high
    }

    // ======================
    // BUSINESS METHODS
    // ======================

    /**
     * Verifica si el mensaje es de email
     */
    @JsonIgnore
    public boolean isEmailMessage() {
        return channel == NotificationChannel.email;
    }

    /**
     * Verifica si el mensaje es de SMS
     */
    @JsonIgnore
    public boolean isSmsMessage() {
        return channel == NotificationChannel.sms;
    }

    /**
     * Verifica si el mensaje está en estado final
     */
    @JsonIgnore
    public boolean isInFinalState() {
        return status == MessageStatus.SENT || status == MessageStatus.DLQ;
    }

    /**
     * Verifica si el mensaje puede ser reintentado
     */
    @JsonIgnore
    public boolean canBeRetried() {
        return status == MessageStatus.FAILED && 
               attemptCount < getMaxAttempts() && 
               !isExpired();
    }

    /**
     * Verifica si el mensaje ha expirado
     */
    @JsonIgnore
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    /**
     * Verifica si es de alta prioridad
     */
    @JsonIgnore
    public boolean isHighPriority() {
        return priority == MessagePriority.high;
    }

    /**
     * Obtiene los destinatarios como lista (para compatibilidad)
     */
    @SuppressWarnings("unchecked")
    @JsonIgnore
    public List<String> getRecipients() {
        if (isEmailMessage() && toJson instanceof List) {
            return (List<String>) toJson;
        } else if (isSmsMessage() && toJson instanceof String) {
            return List.of((String) toJson);
        }
        return List.of();
    }

    /**
     * Obtiene el primer destinatario
     */
    @JsonIgnore
    public String getPrimaryRecipient() {
        List<String> recipients = getRecipients();
        return recipients.isEmpty() ? null : recipients.get(0);
    }

    /**
     * Obtiene el número máximo de intentos según el tipo
     */
    @JsonIgnore
    public int getMaxAttempts() {
        // SMS suele ser más inmediato, email puede tener más reintentos
        return isSmsMessage() ? 3 : 5;
    }

    /**
     * Incrementa el contador de intentos
     */
    public void incrementAttemptCount() {
        this.attemptCount = (this.attemptCount == null ? 0 : this.attemptCount) + 1;
    }

    /**
     * Marca el mensaje como enviado
     */
    public void markAsSent(String providerMessageId) {
        this.status = MessageStatus.SENT;
        this.sentAt = Instant.now();
        this.providerMessageId = providerMessageId;
        this.lastError = null;
    }

    /**
     * Marca el mensaje como fallido
     */
    public void markAsFailed(String error) {
        this.status = MessageStatus.FAILED;
        this.lastError = error;
        incrementAttemptCount();
    }

    /**
     * Marca el mensaje como DLQ
     */
    public void markAsDlq(String finalError) {
        this.status = MessageStatus.DLQ;
        this.lastError = finalError;
    }

    /**
     * Marca el mensaje como en procesamiento
     */
    public void markAsProcessing() {
        this.status = MessageStatus.PROCESSING;
    }

    /**
     * Calcula el tiempo de vida del mensaje en segundos
     */
    @JsonIgnore
    public long getAgeInSeconds() {
        return java.time.Duration.between(createdAt, Instant.now()).toSeconds();
    }

    /**
     * Calcula el tiempo desde el último intento en segundos
     */
    @JsonIgnore
    public long getTimeSinceLastAttemptInSeconds() {
        if (sentAt != null) {
            return java.time.Duration.between(sentAt, Instant.now()).toSeconds();
        }
        return getAgeInSeconds();
    }

    /**
     * Obtiene una representación enmascarada del destinatario para logs
     */
    @JsonIgnore
    public String getMaskedRecipient() {
        String primary = getPrimaryRecipient();
        if (primary == null) return "***";
        
        if (isEmailMessage()) {
            // maria@example.com -> ma***@example.com
            int atIndex = primary.indexOf('@');
            if (atIndex > 2) {
                return primary.substring(0, 2) + "***" + primary.substring(atIndex);
            }
        } else if (isSmsMessage()) {
            // +56 9 1234 5678 -> ***5678
            if (primary.length() > 4) {
                return "***" + primary.substring(primary.length() - 4);
            }
        }
        
        return "***";
    }

    /**
     * Obtiene información resumida para logs
     */
    @JsonIgnore
    public String getLogSummary() {
        return String.format("%s[%s] to=%s template=%s status=%s attempts=%d",
                channel.name().toUpperCase(),
                id.toString().substring(0, 8),
                getMaskedRecipient(),
                templateId != null ? templateId : "direct",
                status,
                attemptCount);
    }

    // ======================
    // FACTORY METHODS
    // ======================

    /**
     * Crea un mensaje de email desde un evento
     */
    public static Message fromEmailEvent(Map<String, Object> eventPayload) {
        UUID messageId = UUID.fromString((String) eventPayload.get("message_id"));
        
        return Message.builder()
                .id(messageId)
                .channel(NotificationChannel.email)
                .toJson(eventPayload.get("to"))
                .templateId((String) eventPayload.get("template_id"))
                .payload(eventPayload)
                .correlationId((String) eventPayload.get("correlation_id"))
                .idempotencyKey((String) eventPayload.get("idempotency_key"))
                .sourceService(determineSourceService(eventPayload))
                .priority(determinePriority(eventPayload))
                .expiresAt(determineExpiration(eventPayload))
                .build();
    }

    /**
     * Crea un mensaje de SMS desde un evento
     */
    public static Message fromSmsEvent(Map<String, Object> eventPayload) {
        UUID messageId = UUID.fromString((String) eventPayload.get("message_id"));
        
        return Message.builder()
                .id(messageId)
                .channel(NotificationChannel.sms)
                .toJson(eventPayload.get("to"))
                .payload(eventPayload)
                .correlationId((String) eventPayload.get("correlation_id"))
                .idempotencyKey((String) eventPayload.get("idempotency_key"))
                .sourceService(determineSourceService(eventPayload))
                .priority(determinePriority(eventPayload))
                .expiresAt(determineExpiration(eventPayload))
                .build();
    }

    // ======================
    // UTILITY METHODS
    // ======================

    private static String determineSourceService(Map<String, Object> payload) {
        return (String) payload.getOrDefault("aggregate_type", "unknown");
    }

    private static MessagePriority determinePriority(Map<String, Object> payload) {
        String priority = (String) payload.get("priority");
        return "high".equals(priority) ? MessagePriority.high : MessagePriority.normal;
    }

    private static Instant determineExpiration(Map<String, Object> payload) {
        // Por defecto, los mensajes expiran en 24 horas
        return Instant.now().plusSeconds(24 * 60 * 60);
    }
}