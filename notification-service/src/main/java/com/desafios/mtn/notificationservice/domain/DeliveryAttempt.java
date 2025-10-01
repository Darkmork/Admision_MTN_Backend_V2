package com.desafios.mtn.notificationservice.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Entidad DeliveryAttempt para el historial de intentos de entrega
 */
@Entity
@Table(name = "delivery_attempts", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"message_id", "attempt_number"}),
       indexes = {
    @Index(name = "idx_delivery_attempts_message_id", columnList = "message_id"),
    @Index(name = "idx_delivery_attempts_status", columnList = "status"),
    @Index(name = "idx_delivery_attempts_created_at", columnList = "created_at"),
    @Index(name = "idx_delivery_attempts_attempt_number", columnList = "attempt_number")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class DeliveryAttempt {

    @Id
    @GeneratedValue
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "message_id", nullable = false)
    private UUID messageId;

    @Column(name = "attempt_number", nullable = false)
    private Integer attemptNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryStatus status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "error_code")
    private String errorCode;

    @Column(name = "provider_response", columnDefinition = "TEXT")
    private String providerResponse;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    // Campos específicos por canal
    @Column(name = "smtp_response_code")
    private Integer smtpResponseCode;

    @Column(name = "sms_segments")
    private Integer smsSegments;

    // Relación con Message
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", insertable = false, updatable = false)
    private Message message;

    // ======================
    // ENUMS
    // ======================

    public enum DeliveryStatus {
        SUCCESS,   // Entrega exitosa
        FAILED,    // Fallo en la entrega (puede reintentarse)
        TIMEOUT,   // Timeout en la conexión
        REJECTED   // Rechazado por el proveedor (no reintentar)
    }

    // ======================
    // BUSINESS METHODS
    // ======================

    /**
     * Verifica si el intento fue exitoso
     */
    public boolean wasSuccessful() {
        return status == DeliveryStatus.SUCCESS;
    }

    /**
     * Verifica si el intento falló
     */
    public boolean wasFailed() {
        return status == DeliveryStatus.FAILED;
    }

    /**
     * Verifica si hubo timeout
     */
    public boolean wasTimeout() {
        return status == DeliveryStatus.TIMEOUT;
    }

    /**
     * Verifica si fue rechazado
     */
    public boolean wasRejected() {
        return status == DeliveryStatus.REJECTED;
    }

    /**
     * Verifica si debe reintentarse (no fue exitoso ni rechazado)
     */
    public boolean shouldRetry() {
        return status != DeliveryStatus.SUCCESS && status != DeliveryStatus.REJECTED;
    }

    /**
     * Obtiene la duración en segundos
     */
    public Double getDurationInSeconds() {
        return durationMs != null ? durationMs / 1000.0 : null;
    }

    /**
     * Verifica si es un intento de email
     */
    public boolean isEmailAttempt() {
        return smtpResponseCode != null;
    }

    /**
     * Verifica si es un intento de SMS
     */
    public boolean isSmsAttempt() {
        return smsSegments != null;
    }

    /**
     * Obtiene un resumen del error para logs
     */
    public String getErrorSummary() {
        if (errorMessage == null) return "No error";
        
        // Truncar mensaje si es muy largo
        String summary = errorMessage.length() > 100 ? 
                errorMessage.substring(0, 100) + "..." : errorMessage;
                
        if (errorCode != null) {
            summary = String.format("[%s] %s", errorCode, summary);
        }
        
        return summary;
    }

    /**
     * Verifica si el error indica un problema temporal
     */
    public boolean isTemporaryError() {
        if (status == DeliveryStatus.TIMEOUT) return true;
        if (status == DeliveryStatus.REJECTED) return false;
        
        // Para SMTP, ciertos códigos indican errores temporales
        if (smtpResponseCode != null) {
            return smtpResponseCode >= 400 && smtpResponseCode < 500; // 4xx = temporal
        }
        
        // Para errores genéricos, considerar temporal si no es explícitamente permanente
        if (errorMessage != null) {
            String error = errorMessage.toLowerCase();
            return !error.contains("invalid") && 
                   !error.contains("banned") && 
                   !error.contains("blocked") &&
                   !error.contains("not found") &&
                   !error.contains("unauthorized");
        }
        
        return true; // Por defecto, asumir temporal
    }

    /**
     * Obtiene información resumida para logs
     */
    public String getLogSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Attempt #").append(attemptNumber)
          .append(" [").append(status).append("]");
          
        if (durationMs != null) {
            sb.append(" (").append(durationMs).append("ms)");
        }
        
        if (status != DeliveryStatus.SUCCESS && errorCode != null) {
            sb.append(" - ").append(errorCode);
        }
        
        return sb.toString();
    }

    // ======================
    // FACTORY METHODS
    // ======================

    /**
     * Crea un intento exitoso para email
     */
    public static DeliveryAttempt createSuccessfulEmailAttempt(
            UUID messageId, int attemptNumber, long durationMs, 
            int smtpCode, String providerResponse) {
        return DeliveryAttempt.builder()
                .messageId(messageId)
                .attemptNumber(attemptNumber)
                .status(DeliveryStatus.SUCCESS)
                .durationMs(durationMs)
                .smtpResponseCode(smtpCode)
                .providerResponse(providerResponse)
                .build();
    }

    /**
     * Crea un intento exitoso para SMS
     */
    public static DeliveryAttempt createSuccessfulSmsAttempt(
            UUID messageId, int attemptNumber, long durationMs, 
            int segments, String providerResponse) {
        return DeliveryAttempt.builder()
                .messageId(messageId)
                .attemptNumber(attemptNumber)
                .status(DeliveryStatus.SUCCESS)
                .durationMs(durationMs)
                .smsSegments(segments)
                .providerResponse(providerResponse)
                .build();
    }

    /**
     * Crea un intento fallido
     */
    public static DeliveryAttempt createFailedAttempt(
            UUID messageId, int attemptNumber, long durationMs,
            String errorCode, String errorMessage, String providerResponse) {
        
        DeliveryStatus status = determineStatusFromError(errorCode, errorMessage);
        
        return DeliveryAttempt.builder()
                .messageId(messageId)
                .attemptNumber(attemptNumber)
                .status(status)
                .durationMs(durationMs)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .providerResponse(providerResponse)
                .build();
    }

    /**
     * Crea un intento con timeout
     */
    public static DeliveryAttempt createTimeoutAttempt(
            UUID messageId, int attemptNumber, long durationMs) {
        return DeliveryAttempt.builder()
                .messageId(messageId)
                .attemptNumber(attemptNumber)
                .status(DeliveryStatus.TIMEOUT)
                .durationMs(durationMs)
                .errorCode("TIMEOUT")
                .errorMessage("Request timed out after " + durationMs + "ms")
                .build();
    }

    // ======================
    // UTILITY METHODS
    // ======================

    /**
     * Determina el estado basado en el error
     */
    private static DeliveryStatus determineStatusFromError(String errorCode, String errorMessage) {
        if ("TIMEOUT".equals(errorCode)) {
            return DeliveryStatus.TIMEOUT;
        }
        
        if (errorMessage != null) {
            String error = errorMessage.toLowerCase();
            if (error.contains("invalid email") || 
                error.contains("invalid recipient") ||
                error.contains("blocked") ||
                error.contains("banned") ||
                error.contains("unauthorized")) {
                return DeliveryStatus.REJECTED;
            }
        }
        
        return DeliveryStatus.FAILED;
    }

    /**
     * Obtiene la descripción del estado
     */
    public String getStatusDescription() {
        switch (status) {
            case SUCCESS: return "Entregado exitosamente";
            case FAILED: return "Fallo en la entrega";
            case TIMEOUT: return "Timeout en la conexión";
            case REJECTED: return "Rechazado por el proveedor";
            default: return "Estado desconocido";
        }
    }

    /**
     * Verifica si este intento requiere análisis adicional
     */
    public boolean requiresAnalysis() {
        // Requiere análisis si falló múltiples veces o fue rechazado
        return status == DeliveryStatus.REJECTED || 
               (status == DeliveryStatus.FAILED && attemptNumber > 3);
    }
}