package com.desafios.mtn.evaluationservice.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Entidad Interview - Representa una entrevista de admisión
 */
@Entity
@Table(name = "interviews", indexes = {
    @Index(name = "idx_interviews_application_id", columnList = "applicationId"),
    @Index(name = "idx_interviews_interviewer_id", columnList = "interviewerId"),
    @Index(name = "idx_interviews_status", columnList = "status"),
    @Index(name = "idx_interviews_scheduled_at", columnList = "scheduledAt"),
    @Index(name = "idx_interviews_type", columnList = "type"),
    @Index(name = "idx_interviews_scheduled_status", columnList = "scheduledAt, status")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Interview {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    @Column(name = "interviewer_id", nullable = false)
    private String interviewerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InterviewType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private InterviewStatus status = InterviewStatus.SCHEDULED;

    // Scheduling information
    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt;

    @Column(name = "duration_minutes")
    @Builder.Default
    private Integer durationMinutes = 60;

    @Column
    private String location;

    @Column(name = "meeting_link")
    private String meetingLink;

    @Column(name = "confirmation_code")
    private String confirmationCode;

    // Execution information
    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "rescheduled_at")
    private Instant rescheduledAt;

    // Results and feedback
    @Column(name = "overall_rating")
    private Integer overallRating; // 1-10 scale

    @Column(name = "recommendation")
    @Enumerated(EnumType.STRING)
    private Recommendation recommendation;

    @Column(name = "communication_skills")
    private Integer communicationSkills; // 1-10

    @Column(name = "academic_interest")
    private Integer academicInterest; // 1-10

    @Column(name = "family_alignment")
    private Integer familyAlignment; // 1-10

    @Column(name = "special_considerations")
    private Integer specialConsiderations; // 1-10

    // Notes and observations
    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "strengths", columnDefinition = "TEXT")
    private String strengths;

    @Column(name = "concerns", columnDefinition = "TEXT")
    private String concerns;

    @Column(name = "recommendations_text", columnDefinition = "TEXT")
    private String recommendationsText;

    // Scheduling metadata
    @Column(name = "reschedule_reason")
    private String rescheduleReason;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @Column(name = "reminder_sent")
    @Builder.Default
    private Boolean reminderSent = false;

    @Column(name = "confirmation_sent")
    @Builder.Default
    private Boolean confirmationSent = false;

    // Priority and urgency
    @Column
    @Builder.Default
    private Integer priority = 0;

    @Column(name = "urgent_flag")
    @Builder.Default
    private Boolean urgentFlag = false;

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

    public enum InterviewStatus {
        SCHEDULED("Programada", "Entrevista programada", false),
        CONFIRMED("Confirmada", "Entrevista confirmada por la familia", false),
        REMINDED("Recordatorio Enviado", "Recordatorio enviado", false),
        IN_PROGRESS("En Progreso", "Entrevista en desarrollo", true),
        COMPLETED("Completada", "Entrevista finalizada con resultado", false),
        RESCHEDULED("Reprogramada", "Entrevista reprogramada", false),
        CANCELLED("Cancelada", "Entrevista cancelada", false),
        NO_SHOW("No Asistió", "Familia no se presentó", false);

        private final String displayName;
        private final String description;
        private final boolean isActive;

        InterviewStatus(String displayName, String description, boolean isActive) {
            this.displayName = displayName;
            this.description = description;
            this.isActive = isActive;
        }

        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
        public boolean isActive() { return isActive; }
        public boolean isTerminal() { return this == COMPLETED || this == CANCELLED || this == NO_SHOW; }
        
        public boolean canTransitionTo(InterviewStatus target) {
            return switch (this) {
                case SCHEDULED -> target == CONFIRMED || target == REMINDED || target == IN_PROGRESS || 
                               target == RESCHEDULED || target == CANCELLED || target == NO_SHOW;
                case CONFIRMED -> target == REMINDED || target == IN_PROGRESS || 
                                target == RESCHEDULED || target == CANCELLED || target == NO_SHOW;
                case REMINDED -> target == IN_PROGRESS || target == RESCHEDULED || 
                               target == CANCELLED || target == NO_SHOW;
                case IN_PROGRESS -> target == COMPLETED || target == CANCELLED;
                case RESCHEDULED -> target == SCHEDULED || target == CANCELLED;
                case COMPLETED, CANCELLED, NO_SHOW -> false; // Terminal states
            };
        }
    }

    public enum InterviewType {
        DIRECTOR_INTERVIEW("Entrevista de Dirección", "Entrevista con director de ciclo", true),
        PSYCHOLOGICAL_INTERVIEW("Entrevista Psicológica", "Evaluación psicológica profesional", true),
        FAMILY_INTERVIEW("Entrevista Familiar", "Entrevista con toda la familia", false),
        STUDENT_INTERVIEW("Entrevista de Estudiante", "Entrevista individual con el estudiante", false),
        FOLLOW_UP_INTERVIEW("Entrevista de Seguimiento", "Entrevista de seguimiento post-evaluación", false);

        private final String displayName;
        private final String description;
        private final boolean requiresSpecialist;

        InterviewType(String displayName, String description, boolean requiresSpecialist) {
            this.displayName = displayName;
            this.description = description;
            this.requiresSpecialist = requiresSpecialist;
        }

        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
        public boolean requiresSpecialist() { return requiresSpecialist; }

        public int getDefaultDuration() {
            return switch (this) {
                case DIRECTOR_INTERVIEW -> 45;
                case PSYCHOLOGICAL_INTERVIEW -> 90;
                case FAMILY_INTERVIEW -> 60;
                case STUDENT_INTERVIEW -> 30;
                case FOLLOW_UP_INTERVIEW -> 30;
            };
        }
    }

    public enum Recommendation {
        HIGHLY_RECOMMENDED("Muy Recomendado", "Candidato altamente recomendado"),
        RECOMMENDED("Recomendado", "Candidato recomendado"),
        CONDITIONALLY_RECOMMENDED("Recomendado con Condiciones", "Recomendado con ciertas condiciones"),
        NOT_RECOMMENDED("No Recomendado", "Candidato no recomendado"),
        REQUIRES_FOLLOW_UP("Requiere Seguimiento", "Requiere entrevista adicional");

        private final String displayName;
        private final String description;

        Recommendation(String displayName, String description) {
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
     * Confirma la entrevista
     */
    public void confirm(String confirmedBy) {
        if (!status.canTransitionTo(InterviewStatus.CONFIRMED)) {
            throw new IllegalStateException(
                String.format("Cannot confirm interview in status %s", status));
        }

        this.status = InterviewStatus.CONFIRMED;
        this.confirmationSent = true;
        this.updatedBy = confirmedBy;
    }

    /**
     * Inicia la entrevista
     */
    public void start(String startedBy) {
        if (!status.canTransitionTo(InterviewStatus.IN_PROGRESS)) {
            throw new IllegalStateException(
                String.format("Cannot start interview in status %s", status));
        }

        this.status = InterviewStatus.IN_PROGRESS;
        this.startedAt = Instant.now();
        this.updatedBy = startedBy;
    }

    /**
     * Completa la entrevista con resultados
     */
    public void complete(Integer overallRating, Recommendation recommendation, String notes, 
                        Integer communication, Integer academic, Integer family, 
                        Integer special, String strengths, String concerns, 
                        String recommendationsText, String completedBy) {
        
        if (!status.canTransitionTo(InterviewStatus.COMPLETED)) {
            throw new IllegalStateException(
                String.format("Cannot complete interview in status %s", status));
        }

        this.status = InterviewStatus.COMPLETED;
        this.completedAt = Instant.now();
        this.overallRating = overallRating;
        this.recommendation = recommendation;
        this.notes = notes;
        this.communicationSkills = communication;
        this.academicInterest = academic;
        this.familyAlignment = family;
        this.specialConsiderations = special;
        this.strengths = strengths;
        this.concerns = concerns;
        this.recommendationsText = recommendationsText;
        this.updatedBy = completedBy;
    }

    /**
     * Reprograma la entrevista
     */
    public void reschedule(Instant newScheduledAt, String reason, String rescheduledBy) {
        if (status == InterviewStatus.COMPLETED || status == InterviewStatus.CANCELLED) {
            throw new IllegalStateException("Cannot reschedule completed or cancelled interview");
        }

        this.scheduledAt = newScheduledAt;
        this.status = InterviewStatus.RESCHEDULED;
        this.rescheduledAt = Instant.now();
        this.rescheduleReason = reason;
        this.reminderSent = false; // Reset reminder flag
        this.updatedBy = rescheduledBy;
    }

    /**
     * Cancela la entrevista
     */
    public void cancel(String reason, String cancelledBy) {
        if (status == InterviewStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel completed interview");
        }

        this.status = InterviewStatus.CANCELLED;
        this.cancelledAt = Instant.now();
        this.cancellationReason = reason;
        this.updatedBy = cancelledBy;
    }

    /**
     * Marca como no asistió
     */
    public void markNoShow(String updatedBy) {
        if (!status.canTransitionTo(InterviewStatus.NO_SHOW)) {
            throw new IllegalStateException(
                String.format("Cannot mark as no-show in status %s", status));
        }

        this.status = InterviewStatus.NO_SHOW;
        this.updatedBy = updatedBy;
    }

    /**
     * Envía recordatorio
     */
    public void sendReminder(String updatedBy) {
        if (status == InterviewStatus.SCHEDULED || status == InterviewStatus.CONFIRMED) {
            this.status = InterviewStatus.REMINDED;
            this.reminderSent = true;
            this.updatedBy = updatedBy;
        }
    }

    // ================================
    // QUERY METHODS
    // ================================

    /**
     * Verifica si la entrevista está activa
     */
    public boolean isActive() {
        return status.isActive();
    }

    /**
     * Verifica si la entrevista está completada
     */
    public boolean isCompleted() {
        return status == InterviewStatus.COMPLETED;
    }

    /**
     * Verifica si la entrevista está vencida
     */
    public boolean isOverdue() {
        return scheduledAt != null && 
               !isCompleted() && 
               Instant.now().isAfter(scheduledAt.plus(durationMinutes, ChronoUnit.MINUTES));
    }

    /**
     * Verifica si necesita recordatorio
     */
    public boolean needsReminder() {
        if (reminderSent || isCompleted() || status == InterviewStatus.CANCELLED) {
            return false;
        }
        
        // Enviar recordatorio 24 horas antes
        Instant reminderTime = scheduledAt.minus(24, ChronoUnit.HOURS);
        return Instant.now().isAfter(reminderTime);
    }

    /**
     * Obtiene los minutos hasta la entrevista
     */
    public long getMinutesUntilInterview() {
        if (scheduledAt == null) return 0;
        return ChronoUnit.MINUTES.between(Instant.now(), scheduledAt);
    }

    /**
     * Obtiene la duración de la entrevista en minutos
     */
    public int getInterviewDurationMinutes() {
        if (startedAt != null && completedAt != null) {
            return (int) ChronoUnit.MINUTES.between(startedAt, completedAt);
        }
        return durationMinutes != null ? durationMinutes : type.getDefaultDuration();
    }

    /**
     * Verifica si requiere seguimiento
     */
    public boolean requiresFollowUp() {
        return recommendation == Recommendation.REQUIRES_FOLLOW_UP ||
               recommendation == Recommendation.CONDITIONALLY_RECOMMENDED;
    }

    /**
     * Obtiene el puntaje promedio
     */
    public Double getAverageScore() {
        if (communicationSkills == null || academicInterest == null || 
            familyAlignment == null || specialConsiderations == null) {
            return null;
        }
        
        return (communicationSkills + academicInterest + familyAlignment + specialConsiderations) / 4.0;
    }

    /**
     * Obtiene el progreso de la entrevista como porcentaje
     */
    public int getProgressPercentage() {
        return switch (status) {
            case SCHEDULED -> 20;
            case CONFIRMED, REMINDED -> 40;
            case IN_PROGRESS -> 80;
            case COMPLETED -> 100;
            case RESCHEDULED -> 10;
            case CANCELLED, NO_SHOW -> 0;
        };
    }

    /**
     * Obtiene un resumen de la entrevista
     */
    public String getSummary() {
        return String.format("Interview[%s: %s %s - %s by %s]",
                id != null ? id.toString().substring(0, 8) : "new",
                type.getDisplayName(),
                scheduledAt != null ? scheduledAt.atZone(ZoneId.systemDefault()).toLocalDateTime() : "unscheduled",
                status.getDisplayName(),
                interviewerId != null ? interviewerId : "unassigned");
    }

    // ================================
    // FACTORY METHODS
    // ================================

    /**
     * Crea una nueva entrevista programada
     */
    public static Interview createScheduled(UUID applicationId, String interviewerId, 
                                          InterviewType type, Instant scheduledAt, 
                                          Integer durationMinutes, String location, 
                                          String createdBy) {
        return Interview.builder()
                .applicationId(applicationId)
                .interviewerId(interviewerId)
                .type(type)
                .scheduledAt(scheduledAt)
                .durationMinutes(durationMinutes != null ? durationMinutes : type.getDefaultDuration())
                .location(location)
                .status(InterviewStatus.SCHEDULED)
                .createdBy(createdBy)
                .updatedBy(createdBy)
                .build();
    }

    @Override
    public String toString() {
        return String.format("Interview{id=%s, app=%s, type=%s, status=%s, interviewer=%s, scheduled=%s}",
                id, applicationId, type, status, interviewerId, scheduledAt);
    }
}