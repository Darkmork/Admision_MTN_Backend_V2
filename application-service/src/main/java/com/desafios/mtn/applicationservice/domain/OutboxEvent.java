package com.desafios.mtn.applicationservice.domain;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Entidad Outbox para patrón de eventos confiables
 * Garantiza que los eventos se publiquen de manera transaccional
 */
@Entity
@Table(name = "outbox", indexes = {
    @Index(name = "idx_outbox_processed", columnList = "processed"),
    @Index(name = "idx_outbox_created_at", columnList = "createdAt"),
    @Index(name = "idx_outbox_retry_count", columnList = "retryCount"),
    @Index(name = "idx_outbox_aggregate_id", columnList = "aggregateId"),
    @Index(name = "idx_outbox_type", columnList = "eventType"),
    @Index(name = "idx_outbox_scheduled_at", columnList = "scheduledAt"),
    @Index(name = "idx_outbox_processing", columnList = "processing, scheduledAt")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "aggregate_type", nullable = false)
    private String aggregateType; // "Application", "TransitionLog", etc.

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId; // ID de la entidad que genera el evento

    @Column(name = "event_type", nullable = false)
    private String eventType; // "ApplicationSubmitted.v1", "StateChanged.v1", etc.

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> payload; // Datos del evento en formato JSON

    // Control de procesamiento
    @Column(nullable = false)
    @Builder.Default
    private Boolean processed = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean processing = false; // Para evitar procesamiento concurrente

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "max_retries", nullable = false)
    @Builder.Default
    private Integer maxRetries = 5;

    // Timestamps
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "scheduled_at", nullable = false)
    @Builder.Default
    private Instant scheduledAt = Instant.now(); // Cuándo debe procesarse

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "last_retry_at")
    private Instant lastRetryAt;

    // Metadatos adicionales
    @Type(JsonType.class)
    @Column(name = "headers", columnDefinition = "jsonb")
    private Map<String, Object> headers; // Headers adicionales para el mensaje

    @Column(name = "routing_key")
    private String routingKey; // Clave de enrutamiento para RabbitMQ

    @Column(name = "exchange_name")
    private String exchangeName; // Exchange de RabbitMQ

    @Column(name = "correlation_id")
    private String correlationId; // ID de correlación para trazabilidad

    @Column(name = "causation_id")
    private String causationId; // ID del evento que causó este evento

    @Column(name = "idempotency_key")
    private String idempotencyKey; // Clave de idempotencia única

    // Error handling
    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "error_details", columnDefinition = "TEXT")
    private String errorDetails;

    // Versioning
    @Column(name = "event_version", nullable = false)
    @Builder.Default
    private String eventVersion = "1.0";

    // Prioridad del evento
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EventPriority priority = EventPriority.NORMAL;

    // ================================
    // BUSINESS METHODS
    // ================================

    /**
     * Verifica si el evento está listo para procesamiento
     */
    public boolean isReadyForProcessing() {
        return !processed && !processing && 
               scheduledAt.isBefore(Instant.now()) && 
               retryCount < maxRetries;
    }

    /**
     * Verifica si el evento ha fallado permanentemente
     */
    public boolean isPermanentlyFailed() {
        return !processed && retryCount >= maxRetries;
    }

    /**
     * Verifica si el evento está siendo procesado actualmente
     */
    public boolean isProcessing() {
        return Boolean.TRUE.equals(processing);
    }

    /**
     * Verifica si el evento ya fue procesado exitosamente
     */
    public boolean isProcessed() {
        return Boolean.TRUE.equals(processed);
    }

    /**
     * Marca el evento como en procesamiento
     */
    public void markAsProcessing() {
        this.processing = true;
        this.lastRetryAt = Instant.now();
    }

    /**
     * Marca el evento como procesado exitosamente
     */
    public void markAsProcessed() {
        this.processed = true;
        this.processing = false;
        this.processedAt = Instant.now();
        this.lastError = null;
        this.errorDetails = null;
    }

    /**
     * Marca el evento como fallido e incrementa el contador de reintentos
     */
    public void markAsFailed(String error, String details) {
        this.processing = false;
        this.retryCount++;
        this.lastError = error;
        this.errorDetails = details;
        this.lastRetryAt = Instant.now();
        
        // Programar siguiente reintento con backoff exponencial
        if (retryCount < maxRetries) {
            long delaySeconds = (long) Math.pow(2, retryCount) * 60; // 2^n minutos
            this.scheduledAt = Instant.now().plusSeconds(delaySeconds);
        }
    }

    /**
     * Resetea el estado de procesamiento (para casos excepcionales)
     */
    public void resetProcessingState() {
        this.processing = false;
    }

    /**
     * Verifica si debe reintentarse
     */
    public boolean shouldRetry() {
        return !processed && retryCount < maxRetries && !processing;
    }

    /**
     * Obtiene el tiempo hasta el próximo reintento
     */
    public long getSecondsUntilNextRetry() {
        if (!shouldRetry()) {
            return -1;
        }
        return Math.max(0, scheduledAt.getEpochSecond() - Instant.now().getEpochSecond());
    }

    /**
     * Obtiene la edad del evento en minutos
     */
    public long getAgeInMinutes() {
        if (createdAt == null) {
            return 0;
        }
        return java.time.Duration.between(createdAt, Instant.now()).toMinutes();
    }

    /**
     * Verifica si el evento es antiguo
     */
    public boolean isOld(int maxAgeInHours) {
        return getAgeInMinutes() > (maxAgeInHours * 60);
    }

    /**
     * Obtiene información de debug del evento
     */
    public String getDebugInfo() {
        return String.format(
            "OutboxEvent{id=%s, type=%s, aggregate=%s:%s, processed=%s, retries=%d/%d, age=%dm}",
            id != null ? id.toString().substring(0, 8) : "null",
            eventType,
            aggregateType,
            aggregateId != null ? aggregateId.toString().substring(0, 8) : "null",
            processed,
            retryCount,
            maxRetries,
            getAgeInMinutes()
        );
    }

    // ================================
    // FACTORY METHODS
    // ================================

    /**
     * Crea un evento de aplicación enviada
     */
    public static OutboxEvent applicationSubmitted(UUID applicationId, Map<String, Object> applicationData) {
        return OutboxEvent.builder()
                .aggregateType("Application")
                .aggregateId(applicationId)
                .eventType("ApplicationSubmitted.v1")
                .payload(applicationData)
                .routingKey("application.submitted")
                .exchangeName("admission.events")
                .priority(EventPriority.HIGH)
                .idempotencyKey("app-submitted-" + applicationId)
                .build();
    }

    /**
     * Crea un evento de cambio de estado
     */
    public static OutboxEvent stateChanged(UUID applicationId, ApplicationStatus fromState, 
                                          ApplicationStatus toState, ReasonCode reasonCode, 
                                          String actorUserId) {
        Map<String, Object> payload = Map.of(
            "applicationId", applicationId,
            "fromState", fromState.getCode(),
            "toState", toState.getCode(),
            "reasonCode", reasonCode.getCode(),
            "actorUserId", actorUserId,
            "timestamp", Instant.now().toString()
        );

        return OutboxEvent.builder()
                .aggregateType("Application")
                .aggregateId(applicationId)
                .eventType("StateChanged.v1")
                .payload(payload)
                .routingKey("application.state.changed")
                .exchangeName("admission.events")
                .priority(toState.isTerminal() ? EventPriority.HIGH : EventPriority.NORMAL)
                .idempotencyKey("state-change-" + applicationId + "-" + Instant.now().toEpochMilli())
                .build();
    }

    /**
     * Crea un evento de documento subido
     */
    public static OutboxEvent documentUploaded(UUID applicationId, UUID documentId, 
                                              AppDocument.DocumentType docType, String uploadedBy) {
        Map<String, Object> payload = Map.of(
            "applicationId", applicationId,
            "documentId", documentId,
            "documentType", docType.name(),
            "uploadedBy", uploadedBy,
            "timestamp", Instant.now().toString()
        );

        return OutboxEvent.builder()
                .aggregateType("AppDocument")
                .aggregateId(documentId)
                .eventType("DocumentUploaded.v1")
                .payload(payload)
                .routingKey("document.uploaded")
                .exchangeName("admission.events")
                .priority(EventPriority.NORMAL)
                .idempotencyKey("doc-uploaded-" + documentId)
                .build();
    }

    /**
     * Crea un evento de evaluación completada
     */
    public static OutboxEvent evaluationCompleted(UUID applicationId, String evaluationType, 
                                                  String evaluatorId, Map<String, Object> results) {
        Map<String, Object> payload = Map.of(
            "applicationId", applicationId,
            "evaluationType", evaluationType,
            "evaluatorId", evaluatorId,
            "results", results,
            "timestamp", Instant.now().toString()
        );

        return OutboxEvent.builder()
                .aggregateType("Application")
                .aggregateId(applicationId)
                .eventType("EvaluationCompleted.v1")
                .payload(payload)
                .routingKey("evaluation.completed")
                .exchangeName("admission.events")
                .priority(EventPriority.HIGH)
                .idempotencyKey("eval-completed-" + applicationId + "-" + evaluationType)
                .build();
    }

    /**
     * Crea un evento programado para el futuro
     */
    public static OutboxEvent createScheduled(String aggregateType, UUID aggregateId, 
                                            String eventType, Map<String, Object> payload,
                                            Instant scheduledAt) {
        return OutboxEvent.builder()
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .eventType(eventType)
                .payload(payload)
                .scheduledAt(scheduledAt)
                .routingKey(eventType.toLowerCase().replace(".", "-"))
                .exchangeName("admission.events")
                .priority(EventPriority.NORMAL)
                .build();
    }

    // ================================
    // ENUMS
    // ================================

    public enum EventPriority {
        LOW("Baja", 1),
        NORMAL("Normal", 2),
        HIGH("Alta", 3),
        CRITICAL("Crítica", 4);

        private final String displayName;
        private final int level;

        EventPriority(String displayName, int level) {
            this.displayName = displayName;
            this.level = level;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getLevel() {
            return level;
        }

        public boolean isHigherThan(EventPriority other) {
            return this.level > other.level;
        }
    }

    @Override
    public String toString() {
        return String.format("OutboxEvent{id=%s, type=%s, processed=%s, retries=%d}", 
                id, eventType, processed, retryCount);
    }
}