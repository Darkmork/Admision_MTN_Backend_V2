// user-service/src/main/java/com/desafios/mtn/userservice/event/DomainEvent.java

package com.desafios.mtn.userservice.event;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Entidad para almacenar eventos de dominio (Outbox pattern)
 */
@Entity
@Table(name = "domain_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class DomainEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "aggregate_type", nullable = false, length = 100)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "event_data", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> eventData;

    @Column(name = "event_version", nullable = false)
    @Builder.Default
    private Integer eventVersion = 1;

    @Column(name = "occurred_at", nullable = false)
    @Builder.Default
    private Instant occurredAt = Instant.now();

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "processed", nullable = false)
    @Builder.Default
    private Boolean processed = false;

    @Column(name = "correlation_id")
    private UUID correlationId;

    @Column(name = "causation_id")
    private UUID causationId;

    @Column(name = "user_id")
    private UUID userId;

    // Métodos de utilidad

    /**
     * Marca el evento como procesado
     */
    public void markAsProcessed() {
        this.processed = true;
        this.processedAt = Instant.now();
    }

    /**
     * Verifica si el evento ha sido procesado
     */
    public boolean isProcessed() {
        return processed != null && processed;
    }

    /**
     * Verifica si el evento ha expirado (más de 24 horas sin procesar)
     */
    public boolean isExpired() {
        if (isProcessed()) {
            return false;
        }
        
        Instant expirationTime = occurredAt.plusSeconds(24 * 60 * 60); // 24 horas
        return Instant.now().isAfter(expirationTime);
    }

    /**
     * Obtiene el tiempo transcurrido desde que ocurrió el evento
     */
    public long getAgeInSeconds() {
        return Instant.now().getEpochSecond() - occurredAt.getEpochSecond();
    }

    /**
     * Obtiene el tiempo que tardó en procesarse el evento
     */
    public Long getProcessingTimeInSeconds() {
        if (processedAt == null) {
            return null;
        }
        return processedAt.getEpochSecond() - occurredAt.getEpochSecond();
    }

    /**
     * Verifica si este evento es de un tipo específico
     */
    public boolean isEventType(String type) {
        return eventType != null && eventType.equals(type);
    }

    /**
     * Verifica si este evento pertenece a un agregado específico
     */
    public boolean isAggregateType(String type) {
        return aggregateType != null && aggregateType.equals(type);
    }

    /**
     * Crea una copia del evento para reintento
     */
    public DomainEvent createRetryEvent() {
        return DomainEvent.builder()
                .aggregateType(this.aggregateType)
                .aggregateId(this.aggregateId)
                .eventType(this.eventType + "_RETRY")
                .eventData(this.eventData)
                .eventVersion(this.eventVersion + 1)
                .correlationId(this.correlationId)
                .causationId(this.id) // El evento original es la causa del reintento
                .userId(this.userId)
                .build();
    }

    @Override
    public String toString() {
        return String.format("DomainEvent{id=%s, aggregateType='%s', eventType='%s', aggregateId=%s, processed=%s, occurredAt=%s}", 
                id, aggregateType, eventType, aggregateId, processed, occurredAt);
    }
}