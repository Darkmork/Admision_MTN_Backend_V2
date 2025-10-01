package com.desafios.mtn.inbox.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import java.time.Instant;
import java.util.UUID;

/**
 * Entidad que representa un evento en el Inbox para garantizar idempotencia
 * Cada evento procesado se registra para evitar procesamiento duplicado
 */
@Entity
@Table(name = "inbox_events",
       indexes = {
           @Index(name = "idx_event_id_unique", columnList = "eventId", unique = true),
           @Index(name = "idx_correlation_id", columnList = "correlationId"),
           @Index(name = "idx_event_type_status", columnList = "eventType,status"),
           @Index(name = "idx_received_at", columnList = "receivedAt"),
           @Index(name = "idx_status_retry", columnList = "status,retryCount"),
           @Index(name = "idx_next_retry", columnList = "nextRetryAt")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InboxEvent {

    @Id
    @GeneratedValue
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    /**
     * ID único del evento original - usado para idempotencia
     */
    @Column(nullable = false, unique = true, length = 100)
    private String eventId;

    /**
     * Tipo de evento
     */
    @Column(nullable = false, length = 100)
    private String eventType;

    /**
     * Versión del esquema del evento
     */
    @Column(nullable = false, length = 20)
    private String eventVersion;

    /**
     * ID de correlación para trazabilidad
     */
    @Column(length = 100)
    private String correlationId;

    /**
     * ID de causalidad
     */
    @Column(length = 100)
    private String causationId;

    /**
     * Payload completo del evento
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String payload;

    /**
     * Hash del payload para detección de cambios
     */
    @Column(nullable = false, length = 64)
    private String payloadHash;

    /**
     * Estado del procesamiento
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProcessingStatus status;

    /**
     * Servicio de origen del evento
     */
    @Column(nullable = false, length = 50)
    private String sourceService;

    /**
     * Servicio que procesa el evento
     */
    @Column(nullable = false, length = 50)
    private String targetService;

    /**
     * Handler específico que procesará el evento
     */
    @Column(length = 100)
    private String handlerName;

    /**
     * Timestamp original del evento
     */
    @Column(nullable = false)
    private Instant eventTimestamp;

    /**
     * Cuando se recibió el evento en el inbox
     */
    @Column(nullable = false, updatable = false)
    private Instant receivedAt;

    /**
     * Cuando se inició el procesamiento
     */
    private Instant processingStartedAt;

    /**
     * Cuando se completó el procesamiento
     */
    private Instant processingCompletedAt;

    /**
     * Número de intentos de procesamiento
     */
    @Column(nullable = false)
    private Integer retryCount;

    /**
     * Máximo número de reintentos permitidos
     */
    @Column(nullable = false)
    private Integer maxRetries;

    /**
     * Cuando realizar el siguiente reintento
     */
    private Instant nextRetryAt;

    /**
     * Mensaje de error del último intento
     */
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * Stack trace del último error
     */
    @Column(columnDefinition = "TEXT")
    private String errorStackTrace;

    /**
     * Resultado del procesamiento
     */
    @Column(columnDefinition = "TEXT")
    private String processingResult;

    /**
     * Metadatos adicionales del procesamiento
     */
    @Column(columnDefinition = "TEXT")
    private String processingMetadata;

    /**
     * Timestamps de auditoría
     */
    @Column(nullable = false)
    private Instant updatedAt;

    // ================================
    // MÉTODOS DE NEGOCIO
    // ================================

    public static InboxEvent createNew(
            String eventId,
            String eventType,
            String eventVersion,
            String correlationId,
            String causationId,
            String payload,
            String sourceService,
            String targetService,
            Instant eventTimestamp) {
        
        Instant now = Instant.now();
        
        return InboxEvent.builder()
            .eventId(eventId)
            .eventType(eventType)
            .eventVersion(eventVersion)
            .correlationId(correlationId)
            .causationId(causationId)
            .payload(payload)
            .payloadHash(calculateHash(payload))
            .status(ProcessingStatus.RECEIVED)
            .sourceService(sourceService)
            .targetService(targetService)
            .eventTimestamp(eventTimestamp)
            .receivedAt(now)
            .retryCount(0)
            .maxRetries(3)
            .updatedAt(now)
            .build();
    }

    public void startProcessing(String handlerName) {
        this.status = ProcessingStatus.PROCESSING;
        this.handlerName = handlerName;
        this.processingStartedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void completeSuccessfully(String result) {
        this.status = ProcessingStatus.COMPLETED;
        this.processingResult = result;
        this.processingCompletedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void markFailed(String errorMessage, String stackTrace) {
        this.retryCount++;
        this.errorMessage = errorMessage;
        this.errorStackTrace = stackTrace;
        this.updatedAt = Instant.now();

        if (this.retryCount >= this.maxRetries) {
            this.status = ProcessingStatus.FAILED;
        } else {
            this.status = ProcessingStatus.RETRY_SCHEDULED;
            this.nextRetryAt = calculateNextRetryTime();
        }
    }

    public void scheduleRetry() {
        this.status = ProcessingStatus.RETRY_SCHEDULED;
        this.nextRetryAt = calculateNextRetryTime();
        this.updatedAt = Instant.now();
    }

    public void markSkipped(String reason) {
        this.status = ProcessingStatus.SKIPPED;
        this.processingResult = reason;
        this.processingCompletedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void markDuplicateDetected() {
        this.status = ProcessingStatus.DUPLICATE_DETECTED;
        this.processingResult = "Duplicate event detected - already processed";
        this.processingCompletedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public boolean isReadyForRetry() {
        return status == ProcessingStatus.RETRY_SCHEDULED && 
               nextRetryAt != null && 
               nextRetryAt.isBefore(Instant.now());
    }

    public boolean isDuplicate(InboxEvent other) {
        return this.eventId.equals(other.eventId) || 
               this.payloadHash.equals(other.payloadHash);
    }

    public boolean canBeRetried() {
        return retryCount < maxRetries && 
               (status == ProcessingStatus.FAILED || status == ProcessingStatus.RETRY_SCHEDULED);
    }

    public long getProcessingDuration() {
        if (processingStartedAt == null) return 0;
        
        Instant endTime = processingCompletedAt != null ? 
            processingCompletedAt : Instant.now();
            
        return java.time.Duration.between(processingStartedAt, endTime).toMillis();
    }

    public long getAge() {
        return java.time.Duration.between(receivedAt, Instant.now()).toMillis();
    }

    private Instant calculateNextRetryTime() {
        // Exponential backoff: 2^retryCount minutes
        long delayMinutes = (long) Math.pow(2, retryCount);
        return Instant.now().plusSeconds(delayMinutes * 60);
    }

    private static String calculateHash(String payload) {
        // En producción usar algoritmo hash robusto (SHA-256)
        return String.valueOf(payload.hashCode());
    }

    // ================================
    // ENUMS
    // ================================

    public enum ProcessingStatus {
        /**
         * Evento recibido pero no procesado
         */
        RECEIVED,
        
        /**
         * Evento en proceso de ser procesado
         */
        PROCESSING,
        
        /**
         * Evento procesado exitosamente
         */
        COMPLETED,
        
        /**
         * Evento falló pero se reintentará
         */
        RETRY_SCHEDULED,
        
        /**
         * Evento falló después de todos los reintentos
         */
        FAILED,
        
        /**
         * Evento se saltó intencionalmente
         */
        SKIPPED,
        
        /**
         * Evento duplicado detectado
         */
        DUPLICATE_DETECTED
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (receivedAt == null) receivedAt = now;
        if (updatedAt == null) updatedAt = now;
        if (retryCount == null) retryCount = 0;
        if (maxRetries == null) maxRetries = 3;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}