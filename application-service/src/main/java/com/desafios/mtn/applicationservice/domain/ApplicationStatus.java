package com.desafios.mtn.applicationservice.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Set;

/**
 * Estados posibles de una aplicación de admisión
 */
public enum ApplicationStatus {
    // Estados iniciales
    DRAFT("DRAFT", "Borrador", "Aplicación creada pero no enviada", false, false),
    PENDING("PENDING", "Pendiente", "Aplicación enviada, esperando revisión", false, false),
    
    // Estados de revisión
    DOCUMENTS_REQUESTED("DOCUMENTS_REQUESTED", "Documentos Solicitados", "Se requieren documentos adicionales", false, false),
    UNDER_REVIEW("UNDER_REVIEW", "En Revisión", "Aplicación bajo revisión administrativa", false, false),
    
    // Estados de evaluación
    INTERVIEW_SCHEDULED("INTERVIEW_SCHEDULED", "Entrevista Programada", "Entrevista con director programada", false, false),
    EXAM_SCHEDULED("EXAM_SCHEDULED", "Examen Programado", "Examen de admisión programado", false, false),
    
    // Estados finales
    APPROVED("APPROVED", "Aprobado", "Aplicación aprobada, pendiente matrícula", false, true),
    REJECTED("REJECTED", "Rechazado", "Aplicación rechazada", true, true),
    WAITLIST("WAITLIST", "Lista de Espera", "En lista de espera por cupos", false, true),
    ENROLLED("ENROLLED", "Matriculado", "Estudiante matriculado exitosamente", true, true),
    EXPIRED("EXPIRED", "Expirado", "Aplicación expirada por tiempo límite", true, true);

    private final String code;
    private final String displayName;
    private final String description;
    private final boolean isTerminal;
    private final boolean isDecisionMade;

    ApplicationStatus(String code, String displayName, String description, boolean isTerminal, boolean isDecisionMade) {
        this.code = code;
        this.displayName = displayName;
        this.description = description;
        this.isTerminal = isTerminal;
        this.isDecisionMade = isDecisionMade;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Indica si este estado es terminal (no permite más transiciones)
     */
    public boolean isTerminal() {
        return isTerminal;
    }

    /**
     * Indica si en este estado ya se tomó una decisión sobre la admisión
     */
    public boolean isDecisionMade() {
        return isDecisionMade;
    }

    /**
     * Estados que requieren acción del apoderado
     */
    public boolean requiresParentAction() {
        return this == DOCUMENTS_REQUESTED || this == APPROVED;
    }

    /**
     * Estados que requieren acción administrativa
     */
    public boolean requiresAdminAction() {
        return this == PENDING || this == UNDER_REVIEW || this == INTERVIEW_SCHEDULED || this == EXAM_SCHEDULED;
    }

    /**
     * Estados en los que se puede subir documentos
     */
    public boolean allowsDocumentUpload() {
        return this == DRAFT || this == PENDING || this == DOCUMENTS_REQUESTED || this == UNDER_REVIEW;
    }

    /**
     * Estados considerados "activos" (no terminales ni expirados)
     */
    public boolean isActive() {
        return !isTerminal() || this == APPROVED || this == WAITLIST;
    }

    /**
     * Estados que indican éxito en el proceso
     */
    public boolean isSuccessful() {
        return this == APPROVED || this == ENROLLED;
    }

    /**
     * Estados que indican fallo en el proceso
     */
    public boolean isRejected() {
        return this == REJECTED || this == EXPIRED;
    }

    /**
     * Obtiene los estados iniciales válidos
     */
    public static Set<ApplicationStatus> getInitialStates() {
        return Set.of(DRAFT);
    }

    /**
     * Obtiene los estados terminales
     */
    public static Set<ApplicationStatus> getTerminalStates() {
        return Set.of(REJECTED, ENROLLED, EXPIRED);
    }

    /**
     * Obtiene los estados que requieren documentos
     */
    public static Set<ApplicationStatus> getDocumentRequiredStates() {
        return Set.of(DRAFT, PENDING, DOCUMENTS_REQUESTED, UNDER_REVIEW);
    }

    /**
     * Obtiene los estados de evaluación
     */
    public static Set<ApplicationStatus> getEvaluationStates() {
        return Set.of(UNDER_REVIEW, INTERVIEW_SCHEDULED, EXAM_SCHEDULED);
    }

    /**
     * Obtiene los estados finales con decisión
     */
    public static Set<ApplicationStatus> getDecisionStates() {
        return Set.of(APPROVED, REJECTED, WAITLIST, ENROLLED, EXPIRED);
    }

    /**
     * Obtiene la prioridad del estado para ordenamiento
     * Estados más críticos tienen prioridad más alta
     */
    public int getPriority() {
        return switch (this) {
            case DOCUMENTS_REQUESTED -> 100;
            case APPROVED -> 90;
            case INTERVIEW_SCHEDULED, EXAM_SCHEDULED -> 80;
            case UNDER_REVIEW -> 70;
            case PENDING -> 60;
            case WAITLIST -> 50;
            case DRAFT -> 40;
            case ENROLLED -> 30;
            case EXPIRED -> 20;
            case REJECTED -> 10;
        };
    }

    /**
     * Obtiene el color asociado al estado (para UI)
     */
    public String getColorCode() {
        return switch (this) {
            case DRAFT -> "#6B7280"; // gray
            case PENDING -> "#F59E0B"; // amber
            case DOCUMENTS_REQUESTED -> "#EF4444"; // red
            case UNDER_REVIEW -> "#3B82F6"; // blue
            case INTERVIEW_SCHEDULED, EXAM_SCHEDULED -> "#8B5CF6"; // purple
            case APPROVED -> "#10B981"; // emerald
            case ENROLLED -> "#059669"; // emerald-dark
            case WAITLIST -> "#F97316"; // orange
            case REJECTED, EXPIRED -> "#DC2626"; // red-dark
        };
    }

    /**
     * Obtiene el ícono asociado al estado (nombre del ícono)
     */
    public String getIconName() {
        return switch (this) {
            case DRAFT -> "document-draft";
            case PENDING -> "clock";
            case DOCUMENTS_REQUESTED -> "document-plus";
            case UNDER_REVIEW -> "eye";
            case INTERVIEW_SCHEDULED -> "calendar-check";
            case EXAM_SCHEDULED -> "academic-cap";
            case APPROVED -> "check-circle";
            case ENROLLED -> "graduation-cap";
            case WAITLIST -> "hourglass";
            case REJECTED -> "x-circle";
            case EXPIRED -> "ban";
        };
    }

    /**
     * Parsea un string a ApplicationStatus
     */
    @JsonCreator
    public static ApplicationStatus fromString(String code) {
        if (code == null) {
            return null;
        }
        
        for (ApplicationStatus status : values()) {
            if (status.code.equalsIgnoreCase(code)) {
                return status;
            }
        }
        
        throw new IllegalArgumentException("Unknown ApplicationStatus: " + code);
    }

    /**
     * Verifica si este estado puede transicionar al estado destino
     * Esta es una validación básica, la lógica completa está en StateTransitionPolicy
     */
    public boolean canTransitionTo(ApplicationStatus targetStatus) {
        if (this == targetStatus) {
            return false; // No auto-transiciones
        }
        
        if (this.isTerminal() && targetStatus != APPROVED && targetStatus != EXPIRED) {
            return false; // Estados terminales generalmente no permiten transiciones
        }
        
        return switch (this) {
            case DRAFT -> targetStatus == PENDING;
            case PENDING -> targetStatus == UNDER_REVIEW || targetStatus == DOCUMENTS_REQUESTED;
            case DOCUMENTS_REQUESTED -> targetStatus == PENDING;
            case UNDER_REVIEW -> targetStatus == INTERVIEW_SCHEDULED || targetStatus == REJECTED;
            case INTERVIEW_SCHEDULED -> targetStatus == EXAM_SCHEDULED || targetStatus == REJECTED;
            case EXAM_SCHEDULED -> targetStatus == APPROVED || targetStatus == REJECTED || targetStatus == WAITLIST;
            case WAITLIST -> targetStatus == APPROVED || targetStatus == EXPIRED;
            case APPROVED -> targetStatus == ENROLLED || targetStatus == EXPIRED;
            case REJECTED, ENROLLED, EXPIRED -> false; // Estados terminales
        };
    }

    /**
     * Obtiene el próximo estado más común en el flujo normal
     */
    public ApplicationStatus getNextCommonState() {
        return switch (this) {
            case DRAFT -> PENDING;
            case PENDING -> UNDER_REVIEW;
            case DOCUMENTS_REQUESTED -> PENDING;
            case UNDER_REVIEW -> INTERVIEW_SCHEDULED;
            case INTERVIEW_SCHEDULED -> EXAM_SCHEDULED;
            case EXAM_SCHEDULED -> APPROVED;
            case APPROVED -> ENROLLED;
            case WAITLIST -> APPROVED;
            case REJECTED, ENROLLED, EXPIRED -> null; // Sin siguiente estado
        };
    }

    /**
     * Verifica si el estado requiere notificación automática
     */
    public boolean requiresNotification() {
        return this != DRAFT; // Todos excepto DRAFT requieren notificación
    }

    /**
     * Obtiene el mensaje de notificación por defecto
     */
    public String getDefaultNotificationMessage() {
        return switch (this) {
            case PENDING -> "Su postulación ha sido recibida y está en proceso de revisión.";
            case DOCUMENTS_REQUESTED -> "Se requieren documentos adicionales para continuar con su postulación.";
            case UNDER_REVIEW -> "Su postulación está siendo revisada por nuestro equipo académico.";
            case INTERVIEW_SCHEDULED -> "Se ha programado una entrevista como parte del proceso de admisión.";
            case EXAM_SCHEDULED -> "Se ha programado un examen de admisión.";
            case APPROVED -> "¡Felicitaciones! Su postulación ha sido aprobada.";
            case ENROLLED -> "La matrícula ha sido completada exitosamente.";
            case WAITLIST -> "Su postulación ha sido colocada en lista de espera.";
            case REJECTED -> "Lamentablemente, su postulación no ha sido aceptada.";
            case EXPIRED -> "Su postulación ha expirado por límite de tiempo.";
            case DRAFT -> ""; // No aplica
        };
    }

    @Override
    public String toString() {
        return String.format("%s (%s)", displayName, code);
    }
}