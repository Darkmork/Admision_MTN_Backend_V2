package com.desafios.mtn.applicationservice.domain;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.net.InetAddress;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Log de auditoría para transiciones de estado en aplicaciones
 */
@Entity
@Table(name = "transition_log", indexes = {
    @Index(name = "idx_transition_log_application_id", columnList = "applicationId"),
    @Index(name = "idx_transition_log_created_at", columnList = "createdAt"),
    @Index(name = "idx_transition_log_from_to_state", columnList = "fromState, toState"),
    @Index(name = "idx_transition_log_reason_code", columnList = "reasonCode"),
    @Index(name = "idx_transition_log_actor_user_id", columnList = "actorUserId")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class TransitionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_state", nullable = false)
    private ApplicationStatus fromState;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_state", nullable = false)
    private ApplicationStatus toState;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_code", nullable = false)
    private ReasonCode reasonCode;

    @Column(name = "actor_user_id", nullable = false)
    private String actorUserId; // ID del usuario que ejecutó la transición

    @Column(name = "actor_role")
    private String actorRole; // Rol del usuario (ADMIN, APODERADO, etc.)

    @Column(columnDefinition = "TEXT")
    private String comment; // Comentario opcional sobre la transición

    // Metadatos de la transición
    @Column(name = "idempotency_key")
    private String idempotencyKey; // Clave única para prevenir duplicados

    @Type(JsonType.class)
    @Column(name = "transition_data", columnDefinition = "jsonb")
    private Map<String, Object> transitionData; // Datos adicionales de la transición

    @Column
    @Builder.Default
    private Boolean automated = false; // Si fue transición automática o manual

    // Auditoría
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // Context adicional
    @Column(name = "ip_address")
    private InetAddress ipAddress; // IP del actor

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent; // User agent si aplica

    // Relación opcional con la aplicación (para queries)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", insertable = false, updatable = false)
    private Application application;

    // ================================
    // BUSINESS METHODS
    // ================================

    /**
     * Verifica si la transición representa un progreso positivo
     */
    public boolean isPositiveTransition() {
        return reasonCode.isPositive();
    }

    /**
     * Verifica si la transición representa un progreso negativo
     */
    public boolean isNegativeTransition() {
        return reasonCode.isNegative();
    }

    /**
     * Verifica si la transición fue automática
     */
    public boolean isAutomated() {
        return Boolean.TRUE.equals(automated);
    }

    /**
     * Verifica si la transición fue manual
     */
    public boolean isManual() {
        return !isAutomated();
    }

    /**
     * Verifica si la transición llevó a un estado terminal
     */
    public boolean isTerminalTransition() {
        return toState.isTerminal();
    }

    /**
     * Verifica si la transición fue un avance en el flujo normal
     */
    public boolean isProgressTransition() {
        return toState.ordinal() > fromState.ordinal() && !toState.isRejected();
    }

    /**
     * Verifica si la transición fue un retroceso
     */
    public boolean isRegressionTransition() {
        return toState.ordinal() < fromState.ordinal() && !isNegativeTransition();
    }

    /**
     * Obtiene la dirección de la transición
     */
    public TransitionDirection getDirection() {
        if (isProgressTransition()) {
            return TransitionDirection.FORWARD;
        } else if (isRegressionTransition()) {
            return TransitionDirection.BACKWARD;
        } else if (isNegativeTransition()) {
            return TransitionDirection.NEGATIVE;
        } else {
            return TransitionDirection.LATERAL;
        }
    }

    /**
     * Obtiene un resumen legible de la transición
     */
    public String getTransitionSummary() {
        return String.format("%s → %s (%s)", 
                fromState.getDisplayName(), 
                toState.getDisplayName(), 
                reasonCode.getDisplayName());
    }

    /**
     * Obtiene información del actor de forma enmascarada para logs
     */
    public String getMaskedActorInfo() {
        if (actorUserId == null) {
            return "unknown";
        }
        
        String masked = actorUserId.length() > 8 ? 
                actorUserId.substring(0, 4) + "***" + actorUserId.substring(actorUserId.length() - 4) :
                actorUserId.substring(0, Math.min(4, actorUserId.length())) + "***";
                
        return actorRole != null ? masked + " (" + actorRole + ")" : masked;
    }

    /**
     * Verifica si la transición requiere notificación
     */
    public boolean requiresNotification() {
        // Transiciones que normalmente requieren notificar al apoderado
        return switch (toState) {
            case PENDING, DOCUMENTS_REQUESTED, UNDER_REVIEW, 
                 INTERVIEW_SCHEDULED, EXAM_SCHEDULED, 
                 APPROVED, REJECTED, WAITLIST, 
                 ENROLLED, EXPIRED -> true;
            case DRAFT -> false;
        };
    }

    /**
     * Obtiene la prioridad de notificación
     */
    public NotificationPriority getNotificationPriority() {
        return switch (toState) {
            case APPROVED, ENROLLED -> NotificationPriority.HIGH;
            case REJECTED, EXPIRED -> NotificationPriority.HIGH;
            case DOCUMENTS_REQUESTED -> NotificationPriority.MEDIUM;
            case INTERVIEW_SCHEDULED, EXAM_SCHEDULED -> NotificationPriority.MEDIUM;
            case PENDING, UNDER_REVIEW, WAITLIST -> NotificationPriority.LOW;
            case DRAFT -> NotificationPriority.NONE;
        };
    }

    /**
     * Obtiene datos específicos de la transición
     */
    public Object getTransitionDataValue(String key) {
        return transitionData != null ? transitionData.get(key) : null;
    }

    /**
     * Verifica si tiene datos de transición específicos
     */
    public boolean hasTransitionData(String key) {
        return transitionData != null && transitionData.containsKey(key);
    }

    /**
     * Calcula el tiempo transcurrido desde la transición
     */
    public long getAgeInMinutes() {
        if (createdAt == null) {
            return 0;
        }
        return java.time.Duration.between(createdAt, Instant.now()).toMinutes();
    }

    /**
     * Verifica si la transición es reciente
     */
    public boolean isRecent(int maxMinutes) {
        return getAgeInMinutes() <= maxMinutes;
    }

    /**
     * Obtiene información de contexto para debugging
     */
    public String getContextInfo() {
        StringBuilder context = new StringBuilder();
        context.append("TransitionLog{");
        context.append("app=").append(applicationId.toString().substring(0, 8));
        context.append(", ").append(fromState).append("->").append(toState);
        context.append(", reason=").append(reasonCode);
        context.append(", actor=").append(getMaskedActorInfo());
        if (automated != null && automated) {
            context.append(", automated");
        }
        if (ipAddress != null) {
            context.append(", ip=").append(ipAddress.getHostAddress());
        }
        context.append("}");
        return context.toString();
    }

    // ================================
    // FACTORY METHODS
    // ================================

    /**
     * Crea un log de transición básico
     */
    public static TransitionLog createTransition(
            UUID applicationId,
            ApplicationStatus fromState,
            ApplicationStatus toState,
            ReasonCode reasonCode,
            String actorUserId,
            String actorRole) {
        
        return TransitionLog.builder()
                .applicationId(applicationId)
                .fromState(fromState)
                .toState(toState)
                .reasonCode(reasonCode)
                .actorUserId(actorUserId)
                .actorRole(actorRole)
                .automated(false)
                .build();
    }

    /**
     * Crea un log de transición con comentario
     */
    public static TransitionLog createTransitionWithComment(
            UUID applicationId,
            ApplicationStatus fromState,
            ApplicationStatus toState,
            ReasonCode reasonCode,
            String actorUserId,
            String actorRole,
            String comment) {
        
        return TransitionLog.builder()
                .applicationId(applicationId)
                .fromState(fromState)
                .toState(toState)
                .reasonCode(reasonCode)
                .actorUserId(actorUserId)
                .actorRole(actorRole)
                .comment(comment)
                .automated(false)
                .build();
    }

    /**
     * Crea un log de transición automática
     */
    public static TransitionLog createAutomatedTransition(
            UUID applicationId,
            ApplicationStatus fromState,
            ApplicationStatus toState,
            ReasonCode reasonCode,
            String systemUserId) {
        
        return TransitionLog.builder()
                .applicationId(applicationId)
                .fromState(fromState)
                .toState(toState)
                .reasonCode(reasonCode)
                .actorUserId(systemUserId)
                .actorRole("SYSTEM")
                .automated(true)
                .comment("Transición automática del sistema")
                .build();
    }

    /**
     * Crea un log con datos de transición completos
     */
    public static TransitionLog createFullTransition(
            UUID applicationId,
            ApplicationStatus fromState,
            ApplicationStatus toState,
            ReasonCode reasonCode,
            String actorUserId,
            String actorRole,
            String comment,
            String idempotencyKey,
            Map<String, Object> transitionData,
            InetAddress ipAddress,
            String userAgent) {
        
        return TransitionLog.builder()
                .applicationId(applicationId)
                .fromState(fromState)
                .toState(toState)
                .reasonCode(reasonCode)
                .actorUserId(actorUserId)
                .actorRole(actorRole)
                .comment(comment)
                .idempotencyKey(idempotencyKey)
                .transitionData(transitionData)
                .automated(false)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();
    }

    // ================================
    // INNER CLASSES
    // ================================

    /**
     * Dirección de la transición de estado
     */
    public enum TransitionDirection {
        FORWARD("Avance", "Progreso hacia aprobación"),
        BACKWARD("Retroceso", "Regreso a estado anterior"),
        NEGATIVE("Negativa", "Progreso hacia rechazo"),
        LATERAL("Lateral", "Cambio sin dirección específica");

        private final String displayName;
        private final String description;

        TransitionDirection(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Prioridad de notificación
     */
    public enum NotificationPriority {
        NONE("Sin notificación"),
        LOW("Baja prioridad"),
        MEDIUM("Prioridad media"),
        HIGH("Alta prioridad");

        private final String description;

        NotificationPriority(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    @Override
    public String toString() {
        return String.format("TransitionLog{id=%s, app=%s, %s->%s, reason=%s, actor=%s}",
                id != null ? id.toString().substring(0, 8) : "null",
                applicationId != null ? applicationId.toString().substring(0, 8) : "null",
                fromState, toState, reasonCode, getMaskedActorInfo());
    }
}