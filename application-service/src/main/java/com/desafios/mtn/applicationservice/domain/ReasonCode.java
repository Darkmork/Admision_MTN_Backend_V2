package com.desafios.mtn.applicationservice.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Códigos de razón para transiciones de estado en aplicaciones de admisión
 */
public enum ReasonCode {
    
    // ==========================================
    // CÓDIGOS PARA TRANSICIONES DE ENVÍO/RECEPCIÓN
    // ==========================================
    
    FORM_SUBMITTED("FORM_SUBMITTED", "Formulario Enviado", 
                   "El formulario de postulación fue enviado por el apoderado", 
                   TransitionType.POSITIVE),
    
    // ==========================================
    // CÓDIGOS PARA DOCUMENTOS
    // ==========================================
    
    DOCS_APPROVED("DOCS_APPROVED", "Documentos Aprobados", 
                  "Todos los documentos fueron revisados y aprobados", 
                  TransitionType.POSITIVE),
    
    DOCS_MISSING("DOCS_MISSING", "Documentos Faltantes", 
                 "Se requieren documentos adicionales para continuar", 
                 TransitionType.BLOCKING),
    
    DOCS_INVALID("DOCS_INVALID", "Documentos Inválidos", 
                 "Los documentos enviados no son válidos o están incompletos", 
                 TransitionType.BLOCKING),
    
    DOCS_UPLOADED("DOCS_UPLOADED", "Documentos Subidos", 
                  "Los documentos solicitados fueron subidos por el apoderado", 
                  TransitionType.POSITIVE),
    
    // ==========================================
    // CÓDIGOS PARA EVALUACIONES ACADÉMICAS
    // ==========================================
    
    EVALS_COMPLETED("EVALS_COMPLETED", "Evaluaciones Completadas", 
                    "Todas las evaluaciones académicas han sido completadas", 
                    TransitionType.POSITIVE),
    
    EVALS_PENDING("EVALS_PENDING", "Evaluaciones Pendientes", 
                  "Las evaluaciones académicas aún están pendientes", 
                  TransitionType.NEUTRAL),
    
    CRITERIA_NOT_MET("CRITERIA_NOT_MET", "Criterios No Cumplidos", 
                     "El postulante no cumple con los criterios académicos requeridos", 
                     TransitionType.NEGATIVE),
    
    ACADEMIC_EXCELLENCE("ACADEMIC_EXCELLENCE", "Excelencia Académica", 
                        "El postulante demostró excelencia académica", 
                        TransitionType.POSITIVE),
    
    // ==========================================
    // CÓDIGOS PARA ENTREVISTAS
    // ==========================================
    
    INTERVIEW_PASSED("INTERVIEW_PASSED", "Entrevista Aprobada", 
                     "La entrevista con el director fue exitosa", 
                     TransitionType.POSITIVE),
    
    INTERVIEW_FAILED("INTERVIEW_FAILED", "Entrevista No Aprobada", 
                     "La entrevista con el director no fue satisfactoria", 
                     TransitionType.NEGATIVE),
    
    INTERVIEW_SCHEDULED("INTERVIEW_SCHEDULED", "Entrevista Programada", 
                        "Se programó una entrevista con el director", 
                        TransitionType.NEUTRAL),
    
    INTERVIEW_NO_SHOW("INTERVIEW_NO_SHOW", "No Asistió a Entrevista", 
                      "El postulante no asistió a la entrevista programada", 
                      TransitionType.NEGATIVE),
    
    // ==========================================
    // CÓDIGOS PARA EXÁMENES
    // ==========================================
    
    EXAM_PASSED("EXAM_PASSED", "Examen Aprobado", 
                "El examen de admisión fue aprobado", 
                TransitionType.POSITIVE),
    
    EXAM_FAILED("EXAM_FAILED", "Examen Reprobado", 
                "El examen de admisión no fue aprobado", 
                TransitionType.NEGATIVE),
    
    EXAM_BORDERLINE("EXAM_BORDERLINE", "Examen Límite", 
                    "El resultado del examen está en el límite, requiere revisión adicional", 
                    TransitionType.NEUTRAL),
    
    EXAM_NO_SHOW("EXAM_NO_SHOW", "No Asistió a Examen", 
                 "El postulante no asistió al examen programado", 
                 TransitionType.NEGATIVE),
    
    // ==========================================
    // CÓDIGOS PARA CUPOS Y LISTA DE ESPERA
    // ==========================================
    
    SLOT_AVAILABLE("SLOT_AVAILABLE", "Cupo Disponible", 
                   "Se liberó un cupo y la aplicación puede ser aprobada", 
                   TransitionType.POSITIVE),
    
    SLOT_OPENED("SLOT_OPENED", "Cupo Abierto", 
                "Se abrió un nuevo cupo para este nivel", 
                TransitionType.POSITIVE),
    
    NO_SLOTS_AVAILABLE("NO_SLOTS_AVAILABLE", "Sin Cupos Disponibles", 
                       "No hay cupos disponibles, se coloca en lista de espera", 
                       TransitionType.NEUTRAL),
    
    WAITLIST_EXPIRED("WAITLIST_EXPIRED", "Lista de Espera Expirada", 
                     "El tiempo en lista de espera ha expirado", 
                     TransitionType.NEGATIVE),
    
    // ==========================================
    // CÓDIGOS PARA MATRÍCULA
    // ==========================================
    
    ENROLLMENT_CONFIRMED("ENROLLMENT_CONFIRMED", "Matrícula Confirmada", 
                         "La matrícula fue confirmada y completada", 
                         TransitionType.POSITIVE),
    
    ENROLLMENT_EXPIRED("ENROLLMENT_EXPIRED", "Matrícula Expirada", 
                       "El plazo para confirmar matrícula ha expirado", 
                       TransitionType.NEGATIVE),
    
    ENROLLMENT_DEADLINE_EXTENDED("ENROLLMENT_DEADLINE_EXTENDED", "Plazo de Matrícula Extendido", 
                                 "Se extendió el plazo para confirmar matrícula", 
                                 TransitionType.POSITIVE),
    
    // ==========================================
    // CÓDIGOS ADMINISTRATIVOS
    // ==========================================
    
    ADMIN_OVERRIDE("ADMIN_OVERRIDE", "Decisión Administrativa", 
                   "Decisión tomada por administración del colegio", 
                   TransitionType.NEUTRAL),
    
    ADMIN_REJECT("ADMIN_REJECT", "Rechazo Administrativo", 
                 "Aplicación rechazada por decisión administrativa", 
                 TransitionType.NEGATIVE),
    
    POLICY_VIOLATION("POLICY_VIOLATION", "Violación de Políticas", 
                     "La aplicación viola las políticas del colegio", 
                     TransitionType.NEGATIVE),
    
    DUPLICATE_APPLICATION("DUPLICATE_APPLICATION", "Aplicación Duplicada", 
                          "Se detectó una aplicación duplicada", 
                          TransitionType.NEGATIVE),
    
    // ==========================================
    // CÓDIGOS PARA PROCESOS AUTOMÁTICOS
    // ==========================================
    
    AUTO_EXPIRE("AUTO_EXPIRE", "Expiración Automática", 
                "La aplicación expiró automáticamente por tiempo límite", 
                TransitionType.NEGATIVE),
    
    SYSTEM_ERROR("SYSTEM_ERROR", "Error de Sistema", 
                 "Error del sistema durante el procesamiento", 
                 TransitionType.NEGATIVE),
    
    DATA_CORRECTION("DATA_CORRECTION", "Corrección de Datos", 
                    "Corrección realizada en los datos de la aplicación", 
                    TransitionType.NEUTRAL),
    
    // ==========================================
    // CÓDIGOS ESPECIALES
    // ==========================================
    
    SPECIAL_CONSIDERATION("SPECIAL_CONSIDERATION", "Consideración Especial", 
                          "Aplicación requiere consideración especial", 
                          TransitionType.NEUTRAL),
    
    MEDICAL_ACCOMMODATION("MEDICAL_ACCOMMODATION", "Acomodación Médica", 
                          "Se requieren acomodaciones por razones médicas", 
                          TransitionType.NEUTRAL),
    
    TRANSFER_STUDENT("TRANSFER_STUDENT", "Estudiante de Traslado", 
                     "Procesamiento especial para estudiante de traslado", 
                     TransitionType.NEUTRAL);

    private final String code;
    private final String displayName;
    private final String description;
    private final TransitionType type;

    ReasonCode(String code, String displayName, String description, TransitionType type) {
        this.code = code;
        this.displayName = displayName;
        this.description = description;
        this.type = type;
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

    public TransitionType getType() {
        return type;
    }

    /**
     * Indica si este código representa una acción positiva
     */
    public boolean isPositive() {
        return type == TransitionType.POSITIVE;
    }

    /**
     * Indica si este código representa una acción negativa
     */
    public boolean isNegative() {
        return type == TransitionType.NEGATIVE;
    }

    /**
     * Indica si este código representa una acción neutral
     */
    public boolean isNeutral() {
        return type == TransitionType.NEUTRAL;
    }

    /**
     * Indica si este código representa una acción que bloquea el progreso
     */
    public boolean isBlocking() {
        return type == TransitionType.BLOCKING;
    }

    /**
     * Obtiene el código de color para visualización
     */
    public String getColorCode() {
        return switch (type) {
            case POSITIVE -> "#10B981"; // green
            case NEGATIVE -> "#EF4444"; // red
            case NEUTRAL -> "#6B7280"; // gray
            case BLOCKING -> "#F59E0B"; // yellow
        };
    }

    /**
     * Obtiene el ícono asociado al tipo de transición
     */
    public String getIconName() {
        return switch (type) {
            case POSITIVE -> "check-circle";
            case NEGATIVE -> "x-circle";
            case NEUTRAL -> "info-circle";
            case BLOCKING -> "exclamation-triangle";
        };
    }

    /**
     * Verifica si este código es apropiado para una transición específica
     */
    public boolean isValidForTransition(ApplicationStatus fromState, ApplicationStatus toState) {
        // Validaciones específicas basadas en la transición
        return switch (this) {
            case FORM_SUBMITTED -> fromState == ApplicationStatus.DRAFT && toState == ApplicationStatus.PENDING;
            case DOCS_APPROVED -> fromState == ApplicationStatus.PENDING && toState == ApplicationStatus.UNDER_REVIEW;
            case DOCS_MISSING -> fromState == ApplicationStatus.PENDING && toState == ApplicationStatus.DOCUMENTS_REQUESTED;
            case DOCS_UPLOADED -> fromState == ApplicationStatus.DOCUMENTS_REQUESTED && toState == ApplicationStatus.PENDING;
            case EVALS_COMPLETED -> fromState == ApplicationStatus.UNDER_REVIEW && toState == ApplicationStatus.INTERVIEW_SCHEDULED;
            case INTERVIEW_PASSED -> fromState == ApplicationStatus.INTERVIEW_SCHEDULED && toState == ApplicationStatus.EXAM_SCHEDULED;
            case EXAM_PASSED -> fromState == ApplicationStatus.EXAM_SCHEDULED && toState == ApplicationStatus.APPROVED;
            case EXAM_FAILED, INTERVIEW_FAILED -> toState == ApplicationStatus.REJECTED;
            case EXAM_BORDERLINE -> fromState == ApplicationStatus.EXAM_SCHEDULED && toState == ApplicationStatus.WAITLIST;
            case SLOT_AVAILABLE -> fromState == ApplicationStatus.WAITLIST && toState == ApplicationStatus.APPROVED;
            case ENROLLMENT_CONFIRMED -> fromState == ApplicationStatus.APPROVED && toState == ApplicationStatus.ENROLLED;
            case WAITLIST_EXPIRED -> fromState == ApplicationStatus.WAITLIST && toState == ApplicationStatus.EXPIRED;
            case ENROLLMENT_EXPIRED -> fromState == ApplicationStatus.APPROVED && toState == ApplicationStatus.EXPIRED;
            default -> true; // Códigos administrativos y especiales son más flexibles
        };
    }

    /**
     * Obtiene la prioridad del código para ordenamiento
     */
    public int getPriority() {
        return switch (type) {
            case POSITIVE -> 100;
            case BLOCKING -> 80;
            case NEGATIVE -> 60;
            case NEUTRAL -> 40;
        };
    }

    /**
     * Parsea un string a ReasonCode
     */
    @JsonCreator
    public static ReasonCode fromString(String code) {
        if (code == null) {
            return null;
        }
        
        for (ReasonCode reasonCode : values()) {
            if (reasonCode.code.equalsIgnoreCase(code)) {
                return reasonCode;
            }
        }
        
        throw new IllegalArgumentException("Unknown ReasonCode: " + code);
    }

    @Override
    public String toString() {
        return String.format("%s (%s)", displayName, code);
    }

    /**
     * Tipo de transición
     */
    public enum TransitionType {
        POSITIVE("Positiva", "Progresa hacia aprobación"),
        NEGATIVE("Negativa", "Progresa hacia rechazo"),
        NEUTRAL("Neutral", "Cambio de estado sin dirección específica"),
        BLOCKING("Bloqueante", "Requiere acción antes de continuar");

        private final String displayName;
        private final String description;

        TransitionType(String displayName, String description) {
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
}