package com.desafios.mtn.evaluationservice.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Entidad Evaluation - Representa una evaluación académica o psicológica
 */
@Entity
@Table(name = "evaluations", indexes = {
    @Index(name = "idx_evaluations_application_id", columnList = "applicationId"),
    @Index(name = "idx_evaluations_evaluator_id", columnList = "evaluatorId"),
    @Index(name = "idx_evaluations_status", columnList = "status"),
    @Index(name = "idx_evaluations_subject", columnList = "subject"),
    @Index(name = "idx_evaluations_evaluator_status", columnList = "evaluatorId, status"),
    @Index(name = "idx_evaluations_application_status", columnList = "applicationId, status"),
    @Index(name = "idx_evaluations_sla_exceeded", columnList = "slaExceeded")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Evaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    @Column(name = "evaluator_id", nullable = false)
    private String evaluatorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Subject subject;

    @Enumerated(EnumType.STRING)
    @Column
    private Level level;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EvaluationStatus status = EvaluationStatus.PENDING;

    // Timing information
    @Column(name = "assigned_at")
    private Instant assignedAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    // Scoring information
    @Column(name = "total_score", precision = 5, scale = 2)
    private BigDecimal totalScore;

    @Column(name = "max_score", precision = 5, scale = 2)
    private BigDecimal maxScore;

    @Column(precision = 5, scale = 2)
    private BigDecimal percentage;

    @Column
    private Boolean passed;

    // Assignment metadata
    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_reason")
    private AssignmentReason assignmentReason;

    @Column(name = "previous_evaluator_id")
    private String previousEvaluatorId;

    @Column
    @Builder.Default
    private Integer priority = 0;

    // SLA and timing
    @Column(name = "expected_completion_at")
    private Instant expectedCompletionAt;

    @Column(name = "sla_exceeded")
    @Builder.Default
    private Boolean slaExceeded = false;

    @Column(name = "processing_time_minutes")
    private Integer processingTimeMinutes;

    // Notes and comments
    @Column(columnDefinition = "TEXT")
    private String notes;

    // Audit fields
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_by", nullable = false)
    @Builder.Default
    private String createdBy = "SYSTEM";

    @Column(name = "updated_by", nullable = false)
    @Builder.Default
    private String updatedBy = "SYSTEM";

    // Optimistic locking
    @Version
    @Column(nullable = false)
    @Builder.Default
    private Integer version = 0;

    // ================================
    // ENUMS
    // ================================

    public enum EvaluationStatus {
        PENDING("Pendiente", "Evaluación creada pero no asignada", false),
        ASSIGNED("Asignada", "Asignada a evaluador pero no iniciada", true),
        IN_PROGRESS("En Progreso", "Evaluación siendo realizada", true),
        COMPLETED("Completada", "Evaluación finalizada con resultado", false),
        CANCELLED("Cancelada", "Evaluación cancelada", false);

        private final String displayName;
        private final String description;
        private final boolean isActive;

        EvaluationStatus(String displayName, String description, boolean isActive) {
            this.displayName = displayName;
            this.description = description;
            this.isActive = isActive;
        }

        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
        public boolean isActive() { return isActive; }
        public boolean isTerminal() { return this == COMPLETED || this == CANCELLED; }
        public boolean canTransitionTo(EvaluationStatus target) {
            return switch (this) {
                case PENDING -> target == ASSIGNED || target == CANCELLED;
                case ASSIGNED -> target == IN_PROGRESS || target == CANCELLED;
                case IN_PROGRESS -> target == COMPLETED || target == CANCELLED;
                case COMPLETED, CANCELLED -> false; // Terminal states
            };
        }
    }

    public enum Subject {
        MATHEMATICS("Matemáticas", "Evaluación de competencias matemáticas"),
        LANGUAGE("Lenguaje", "Evaluación de competencias lingüísticas"),
        ENGLISH("Inglés", "Evaluación de competencias en inglés"),
        PSYCHOLOGY("Psicología", "Evaluación psicológica y emocional"),
        GENERAL("General", "Evaluación general de competencias"),
        SCIENCE("Ciencias", "Evaluación de competencias científicas"),
        HISTORY("Historia", "Evaluación de competencias en historia");

        private final String displayName;
        private final String description;

        Subject(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }

        public boolean requiresSpecialist() {
            return this != GENERAL;
        }

        public Level[] getCompatibleLevels() {
            return switch (this) {
                case PSYCHOLOGY -> new Level[]{Level.ALL};
                case GENERAL -> Level.values();
                case MATHEMATICS, LANGUAGE, ENGLISH -> new Level[]{Level.BASIC, Level.HIGH_SCHOOL};
                case SCIENCE, HISTORY -> new Level[]{Level.HIGH_SCHOOL};
            };
        }
    }

    public enum Level {
        PRESCHOOL("Preescolar", "Educación Preescolar", 3, 5),
        BASIC("Básica", "Educación Básica", 6, 13),
        HIGH_SCHOOL("Media", "Educación Media", 14, 18),
        ALL("Todos", "Todos los niveles", 3, 18);

        private final String displayName;
        private final String description;
        private final int minAge;
        private final int maxAge;

        Level(String displayName, String description, int minAge, int maxAge) {
            this.displayName = displayName;
            this.description = description;
            this.minAge = minAge;
            this.maxAge = maxAge;
        }

        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
        public int getMinAge() { return minAge; }
        public int getMaxAge() { return maxAge; }

        public boolean isCompatibleWithAge(int age) {
            return age >= minAge && age <= maxAge;
        }
    }

    public enum AssignmentReason {
        AUTO_ASSIGNED("Asignación Automática", "Asignado automáticamente por el sistema"),
        MANUAL_ASSIGNED("Asignación Manual", "Asignado manualmente por un administrador"),
        REASSIGNED("Reasignado", "Reasignado desde otro evaluador"),
        LOAD_BALANCING("Balance de Carga", "Asignado por balanceo de carga"),
        SPECIALIST_REQUIRED("Especialista Requerido", "Asignado por requerimiento de especialista");

        private final String displayName;
        private final String description;

        AssignmentReason(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }

    // ================================
    // BUSINESS METHODS
    // ================================

    /**
     * Asigna la evaluación a un evaluador
     */
    public void assign(String evaluatorId, AssignmentReason reason, String assignedBy, Instant expectedCompletion) {
        if (!status.canTransitionTo(EvaluationStatus.ASSIGNED)) {
            throw new IllegalStateException(
                String.format("Cannot assign evaluation in status %s", status));
        }

        this.evaluatorId = evaluatorId;
        this.status = EvaluationStatus.ASSIGNED;
        this.assignedAt = Instant.now();
        this.assignmentReason = reason;
        this.expectedCompletionAt = expectedCompletion;
        this.updatedBy = assignedBy;
    }

    /**
     * Reasigna la evaluación a otro evaluador
     */
    public void reassign(String newEvaluatorId, String reason, String reassignedBy, Instant expectedCompletion) {
        if (status == EvaluationStatus.COMPLETED || status == EvaluationStatus.CANCELLED) {
            throw new IllegalStateException("Cannot reassign completed or cancelled evaluation");
        }

        this.previousEvaluatorId = this.evaluatorId;
        this.evaluatorId = newEvaluatorId;
        this.assignmentReason = AssignmentReason.REASSIGNED;
        this.expectedCompletionAt = expectedCompletion;
        this.updatedBy = reassignedBy;
        this.notes = (this.notes != null ? this.notes + "\n" : "") + 
                    String.format("Reasignado de %s a %s. Razón: %s", 
                                 previousEvaluatorId, newEvaluatorId, reason);
    }

    /**
     * Inicia la evaluación
     */
    public void start(String startedBy) {
        if (!status.canTransitionTo(EvaluationStatus.IN_PROGRESS)) {
            throw new IllegalStateException(
                String.format("Cannot start evaluation in status %s", status));
        }

        this.status = EvaluationStatus.IN_PROGRESS;
        this.startedAt = Instant.now();
        this.updatedBy = startedBy;
    }

    /**
     * Completa la evaluación con puntajes
     */
    public void complete(BigDecimal totalScore, BigDecimal maxScore, String completedBy, String notes) {
        if (!status.canTransitionTo(EvaluationStatus.COMPLETED)) {
            throw new IllegalStateException(
                String.format("Cannot complete evaluation in status %s", status));
        }

        this.status = EvaluationStatus.COMPLETED;
        this.completedAt = Instant.now();
        this.totalScore = totalScore;
        this.maxScore = maxScore;
        
        // Calcular porcentaje y estado de aprobación
        if (maxScore != null && maxScore.compareTo(BigDecimal.ZERO) > 0) {
            this.percentage = totalScore.divide(maxScore, 2, BigDecimal.ROUND_HALF_UP)
                                      .multiply(BigDecimal.valueOf(100));
            this.passed = this.percentage.compareTo(BigDecimal.valueOf(60)) >= 0; // 60% mínimo
        }

        // Calcular tiempo de procesamiento
        if (assignedAt != null) {
            this.processingTimeMinutes = (int) ChronoUnit.MINUTES.between(assignedAt, completedAt);
        }

        // Verificar SLA
        if (expectedCompletionAt != null && completedAt.isAfter(expectedCompletionAt)) {
            this.slaExceeded = true;
        }

        this.notes = notes;
        this.updatedBy = completedBy;
    }

    /**
     * Cancela la evaluación
     */
    public void cancel(String reason, String cancelledBy) {
        if (status == EvaluationStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel completed evaluation");
        }

        this.status = EvaluationStatus.CANCELLED;
        this.cancelledAt = Instant.now();
        this.updatedBy = cancelledBy;
        this.notes = (this.notes != null ? this.notes + "\n" : "") + 
                    String.format("Cancelada. Razón: %s", reason);
    }

    // ================================
    // QUERY METHODS
    // ================================

    /**
     * Verifica si la evaluación está activa (asignada o en progreso)
     */
    public boolean isActive() {
        return status.isActive();
    }

    /**
     * Verifica si la evaluación está completada
     */
    public boolean isCompleted() {
        return status == EvaluationStatus.COMPLETED;
    }

    /**
     * Verifica si la evaluación está vencida
     */
    public boolean isOverdue() {
        return expectedCompletionAt != null && 
               !isCompleted() && 
               Instant.now().isAfter(expectedCompletionAt);
    }

    /**
     * Obtiene los días transcurridos desde la asignación
     */
    public long getDaysSinceAssignment() {
        if (assignedAt == null) return 0;
        return ChronoUnit.DAYS.between(assignedAt, Instant.now());
    }

    /**
     * Obtiene las horas transcurridas desde el inicio
     */
    public long getHoursSinceStarted() {
        if (startedAt == null) return 0;
        return ChronoUnit.HOURS.between(startedAt, Instant.now());
    }

    /**
     * Verifica si requiere atención urgente
     */
    public boolean requiresUrgentAttention() {
        return isOverdue() || 
               (status == EvaluationStatus.ASSIGNED && getDaysSinceAssignment() > 2) ||
               (status == EvaluationStatus.IN_PROGRESS && getHoursSinceStarted() > 48);
    }

    /**
     * Obtiene el progreso de la evaluación como porcentaje
     */
    public int getProgressPercentage() {
        return switch (status) {
            case PENDING -> 0;
            case ASSIGNED -> 25;
            case IN_PROGRESS -> 75;
            case COMPLETED -> 100;
            case CANCELLED -> 0;
        };
    }

    /**
     * Obtiene un resumen de la evaluación
     */
    public String getSummary() {
        return String.format("Evaluation[%s: %s %s - %s by %s]",
                id != null ? id.toString().substring(0, 8) : "new",
                subject.getDisplayName(),
                level != null ? level.getDisplayName() : "N/A",
                status.getDisplayName(),
                evaluatorId != null ? evaluatorId : "unassigned");
    }

    /**
     * Obtiene información para métricas
     */
    public String getMetricsInfo() {
        return String.format("subject=%s,level=%s,status=%s,evaluator=%s,sla_exceeded=%s",
                subject, level, status, evaluatorId, slaExceeded);
    }

    // ================================
    // FACTORY METHODS
    // ================================

    /**
     * Crea una nueva evaluación pendiente
     */
    public static Evaluation createPending(UUID applicationId, Subject subject, Level level, String createdBy) {
        return Evaluation.builder()
                .applicationId(applicationId)
                .subject(subject)
                .level(level)
                .status(EvaluationStatus.PENDING)
                .createdBy(createdBy)
                .updatedBy(createdBy)
                .build();
    }

    /**
     * Crea una evaluación con asignación inmediata
     */
    public static Evaluation createAndAssign(UUID applicationId, Subject subject, Level level,
                                           String evaluatorId, AssignmentReason reason,
                                           String createdBy, Instant expectedCompletion) {
        Evaluation evaluation = createPending(applicationId, subject, level, createdBy);
        evaluation.assign(evaluatorId, reason, createdBy, expectedCompletion);
        return evaluation;
    }

    @Override
    public String toString() {
        return String.format("Evaluation{id=%s, app=%s, subject=%s, level=%s, status=%s, evaluator=%s}",
                id, applicationId, subject, level, status, evaluatorId);
    }
}