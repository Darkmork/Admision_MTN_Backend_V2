// src/main/java/com/desafios/mtn/userservice/outbox/OutboxEntity.java

package com.desafios.mtn.userservice.outbox;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "outbox")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class OutboxEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "aggregate_type", nullable = false)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private String aggregateId;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> payload;

    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "processed_at")
    private Instant processedAt;

    // Métodos de utilidad

    /**
     * Marca el evento como procesado
     */
    public void markAsProcessed() {
        this.processedAt = Instant.now();
    }

    /**
     * Verifica si el evento ha sido procesado
     */
    public boolean isProcessed() {
        return processedAt != null;
    }

    /**
     * Verifica si el evento es de tipo email
     */
    public boolean isEmailEvent() {
        return "EmailRequested.v1".equals(type);
    }

    /**
     * Verifica si el evento es de tipo SMS
     */
    public boolean isSmsEvent() {
        return "SmsRequested.v1".equals(type);
    }

    /**
     * Obtiene la edad del evento en segundos
     */
    public long getAgeInSeconds() {
        return Instant.now().getEpochSecond() - createdAt.getEpochSecond();
    }

    /**
     * Verifica si el evento es reciente (menos de 5 minutos)
     */
    public boolean isRecentForIdempotency() {
        return getAgeInSeconds() < 300; // 5 minutos
    }

    /**
     * Constantes para tipos de evento
     */
    public static class EventTypes {
        public static final String EMAIL_REQUESTED_V1 = "EmailRequested.v1";
        public static final String SMS_REQUESTED_V1 = "SmsRequested.v1";
    }

    /**
     * Constantes para tipos de agregado
     */
    public static class AggregateTypes {
        public static final String USER = "USER";
        public static final String ROLE = "ROLE";
        public static final String SYSTEM = "SYSTEM";
    }
}