package com.desafios.mtn.applicationservice.domain;

import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Política inmutable de transiciones de estado para aplicaciones
 * Define todas las transiciones válidas y sus códigos de razón permitidos
 */
@Component
public class StateTransitionPolicy {

    /**
     * Mapa inmutable de transiciones válidas
     * Key: Estado origen
     * Value: Mapa de estados destino válidos y sus códigos de razón permitidos
     */
    private static final Map<ApplicationStatus, Map<ApplicationStatus, Set<ReasonCode>>> VALID_TRANSITIONS;

    static {
        Map<ApplicationStatus, Map<ApplicationStatus, Set<ReasonCode>>> transitions = new HashMap<>();

        // ==========================================
        // DRAFT → Estados válidos
        // ==========================================
        transitions.put(ApplicationStatus.DRAFT, Map.of(
            ApplicationStatus.PENDING, Set.of(ReasonCode.FORM_SUBMITTED)
        ));

        // ==========================================
        // PENDING → Estados válidos
        // ==========================================
        transitions.put(ApplicationStatus.PENDING, Map.of(
            ApplicationStatus.UNDER_REVIEW, Set.of(
                ReasonCode.DOCS_APPROVED,
                ReasonCode.ADMIN_OVERRIDE
            ),
            ApplicationStatus.DOCUMENTS_REQUESTED, Set.of(
                ReasonCode.DOCS_MISSING,
                ReasonCode.DOCS_INVALID
            ),
            ApplicationStatus.REJECTED, Set.of(
                ReasonCode.ADMIN_REJECT,
                ReasonCode.POLICY_VIOLATION,
                ReasonCode.DUPLICATE_APPLICATION
            )
        ));

        // ==========================================
        // DOCUMENTS_REQUESTED → Estados válidos
        // ==========================================
        transitions.put(ApplicationStatus.DOCUMENTS_REQUESTED, Map.of(
            ApplicationStatus.PENDING, Set.of(
                ReasonCode.DOCS_UPLOADED
            ),
            ApplicationStatus.EXPIRED, Set.of(
                ReasonCode.AUTO_EXPIRE
            )
        ));

        // ==========================================
        // UNDER_REVIEW → Estados válidos
        // ==========================================
        transitions.put(ApplicationStatus.UNDER_REVIEW, Map.of(
            ApplicationStatus.INTERVIEW_SCHEDULED, Set.of(
                ReasonCode.EVALS_COMPLETED,
                ReasonCode.ACADEMIC_EXCELLENCE
            ),
            ApplicationStatus.EXAM_SCHEDULED, Set.of(
                ReasonCode.EVALS_COMPLETED,
                ReasonCode.SPECIAL_CONSIDERATION,
                ReasonCode.TRANSFER_STUDENT
            ),
            ApplicationStatus.DOCUMENTS_REQUESTED, Set.of(
                ReasonCode.DOCS_MISSING,
                ReasonCode.DOCS_INVALID
            ),
            ApplicationStatus.APPROVED, Set.of(
                ReasonCode.ADMIN_OVERRIDE,
                ReasonCode.ACADEMIC_EXCELLENCE
            ),
            ApplicationStatus.REJECTED, Set.of(
                ReasonCode.CRITERIA_NOT_MET,
                ReasonCode.ADMIN_REJECT,
                ReasonCode.POLICY_VIOLATION
            )
        ));

        // ==========================================
        // INTERVIEW_SCHEDULED → Estados válidos
        // ==========================================
        transitions.put(ApplicationStatus.INTERVIEW_SCHEDULED, Map.of(
            ApplicationStatus.EXAM_SCHEDULED, Set.of(
                ReasonCode.INTERVIEW_PASSED,
                ReasonCode.INTERVIEW_SCHEDULED
            ),
            ApplicationStatus.APPROVED, Set.of(
                ReasonCode.INTERVIEW_PASSED,
                ReasonCode.ADMIN_OVERRIDE,
                ReasonCode.SPECIAL_CONSIDERATION
            ),
            ApplicationStatus.REJECTED, Set.of(
                ReasonCode.INTERVIEW_FAILED,
                ReasonCode.INTERVIEW_NO_SHOW,
                ReasonCode.ADMIN_REJECT
            ),
            ApplicationStatus.WAITLIST, Set.of(
                ReasonCode.INTERVIEW_PASSED,
                ReasonCode.NO_SLOTS_AVAILABLE
            )
        ));

        // ==========================================
        // EXAM_SCHEDULED → Estados válidos
        // ==========================================
        transitions.put(ApplicationStatus.EXAM_SCHEDULED, Map.of(
            ApplicationStatus.APPROVED, Set.of(
                ReasonCode.EXAM_PASSED,
                ReasonCode.ACADEMIC_EXCELLENCE,
                ReasonCode.ADMIN_OVERRIDE
            ),
            ApplicationStatus.REJECTED, Set.of(
                ReasonCode.EXAM_FAILED,
                ReasonCode.EXAM_NO_SHOW,
                ReasonCode.CRITERIA_NOT_MET,
                ReasonCode.ADMIN_REJECT
            ),
            ApplicationStatus.WAITLIST, Set.of(
                ReasonCode.EXAM_BORDERLINE,
                ReasonCode.NO_SLOTS_AVAILABLE,
                ReasonCode.EXAM_PASSED
            ),
            ApplicationStatus.INTERVIEW_SCHEDULED, Set.of(
                ReasonCode.EXAM_BORDERLINE,
                ReasonCode.DATA_CORRECTION
            )
        ));

        // ==========================================
        // APPROVED → Estados válidos
        // ==========================================
        transitions.put(ApplicationStatus.APPROVED, Map.of(
            ApplicationStatus.ENROLLED, Set.of(
                ReasonCode.ENROLLMENT_CONFIRMED
            ),
            ApplicationStatus.EXPIRED, Set.of(
                ReasonCode.ENROLLMENT_EXPIRED,
                ReasonCode.AUTO_EXPIRE
            ),
            ApplicationStatus.WAITLIST, Set.of(
                ReasonCode.DATA_CORRECTION,
                ReasonCode.ADMIN_OVERRIDE
            )
        ));

        // ==========================================
        // WAITLIST → Estados válidos
        // ==========================================
        transitions.put(ApplicationStatus.WAITLIST, Map.of(
            ApplicationStatus.APPROVED, Set.of(
                ReasonCode.SLOT_AVAILABLE,
                ReasonCode.SLOT_OPENED,
                ReasonCode.ADMIN_OVERRIDE
            ),
            ApplicationStatus.EXPIRED, Set.of(
                ReasonCode.WAITLIST_EXPIRED,
                ReasonCode.AUTO_EXPIRE
            )
        ));

        // ==========================================
        // Estados terminales - Sin transiciones normales
        // ==========================================
        // REJECTED: Solo permite correcciones administrativas excepcionales
        transitions.put(ApplicationStatus.REJECTED, Map.of(
            ApplicationStatus.PENDING, Set.of(
                ReasonCode.ADMIN_OVERRIDE,
                ReasonCode.DATA_CORRECTION
            )
        ));

        // ENROLLED: Estado final exitoso, sin transiciones
        transitions.put(ApplicationStatus.ENROLLED, Map.of());

        // EXPIRED: Estado final por tiempo, permite reactivación excepcional
        transitions.put(ApplicationStatus.EXPIRED, Map.of(
            ApplicationStatus.PENDING, Set.of(
                ReasonCode.ADMIN_OVERRIDE,
                ReasonCode.ENROLLMENT_DEADLINE_EXTENDED
            )
        ));

        // Hacer el mapa inmutable
        VALID_TRANSITIONS = Collections.unmodifiableMap(transitions);
    }

    /**
     * Verifica si una transición es válida
     */
    public boolean isValidTransition(ApplicationStatus fromState, ApplicationStatus toState, ReasonCode reasonCode) {
        if (fromState == null || toState == null || reasonCode == null) {
            return false;
        }

        if (fromState == toState) {
            return false; // No auto-transiciones
        }

        Map<ApplicationStatus, Set<ReasonCode>> validTargets = VALID_TRANSITIONS.get(fromState);
        if (validTargets == null) {
            return false;
        }

        Set<ReasonCode> validReasons = validTargets.get(toState);
        if (validReasons == null) {
            return false;
        }

        return validReasons.contains(reasonCode);
    }

    /**
     * Obtiene todos los estados válidos desde un estado origen
     */
    public Set<ApplicationStatus> getValidTargetStates(ApplicationStatus fromState) {
        Map<ApplicationStatus, Set<ReasonCode>> validTargets = VALID_TRANSITIONS.get(fromState);
        return validTargets != null ? Set.copyOf(validTargets.keySet()) : Set.of();
    }

    /**
     * Obtiene todos los códigos de razón válidos para una transición específica
     */
    public Set<ReasonCode> getValidReasonCodes(ApplicationStatus fromState, ApplicationStatus toState) {
        Map<ApplicationStatus, Set<ReasonCode>> validTargets = VALID_TRANSITIONS.get(fromState);
        if (validTargets == null) {
            return Set.of();
        }

        Set<ReasonCode> validReasons = validTargets.get(toState);
        return validReasons != null ? Set.copyOf(validReasons) : Set.of();
    }

    /**
     * Obtiene todas las transiciones válidas desde un estado
     */
    public Map<ApplicationStatus, Set<ReasonCode>> getValidTransitionsFrom(ApplicationStatus fromState) {
        Map<ApplicationStatus, Set<ReasonCode>> validTargets = VALID_TRANSITIONS.get(fromState);
        return validTargets != null ? Map.copyOf(validTargets) : Map.of();
    }

    /**
     * Obtiene todas las transiciones definidas en el sistema
     */
    public Map<ApplicationStatus, Map<ApplicationStatus, Set<ReasonCode>>> getAllValidTransitions() {
        return VALID_TRANSITIONS;
    }

    /**
     * Verifica si un estado es terminal (no permite transiciones normales)
     */
    public boolean isTerminalState(ApplicationStatus state) {
        Map<ApplicationStatus, Set<ReasonCode>> transitions = VALID_TRANSITIONS.get(state);
        return transitions == null || transitions.isEmpty();
    }

    /**
     * Obtiene el flujo normal más común para un estado
     */
    public Optional<ApplicationStatus> getPreferredNextState(ApplicationStatus fromState) {
        return switch (fromState) {
            case DRAFT -> Optional.of(ApplicationStatus.PENDING);
            case PENDING -> Optional.of(ApplicationStatus.UNDER_REVIEW);
            case DOCUMENTS_REQUESTED -> Optional.of(ApplicationStatus.PENDING);
            case UNDER_REVIEW -> Optional.of(ApplicationStatus.INTERVIEW_SCHEDULED);
            case INTERVIEW_SCHEDULED -> Optional.of(ApplicationStatus.EXAM_SCHEDULED);
            case EXAM_SCHEDULED -> Optional.of(ApplicationStatus.APPROVED);
            case APPROVED -> Optional.of(ApplicationStatus.ENROLLED);
            case WAITLIST -> Optional.of(ApplicationStatus.APPROVED);
            case REJECTED, ENROLLED, EXPIRED -> Optional.empty(); // Estados terminales
        };
    }

    /**
     * Obtiene el código de razón más común para una transición
     */
    public Optional<ReasonCode> getPreferredReasonCode(ApplicationStatus fromState, ApplicationStatus toState) {
        Set<ReasonCode> validReasons = getValidReasonCodes(fromState, toState);
        
        if (validReasons.isEmpty()) {
            return Optional.empty();
        }

        // Devolver el código más común o prioritario
        return switch (fromState.name() + "->" + toState.name()) {
            case "DRAFT->PENDING" -> Optional.of(ReasonCode.FORM_SUBMITTED);
            case "PENDING->UNDER_REVIEW" -> Optional.of(ReasonCode.DOCS_APPROVED);
            case "PENDING->DOCUMENTS_REQUESTED" -> Optional.of(ReasonCode.DOCS_MISSING);
            case "DOCUMENTS_REQUESTED->PENDING" -> Optional.of(ReasonCode.DOCS_UPLOADED);
            case "UNDER_REVIEW->INTERVIEW_SCHEDULED" -> Optional.of(ReasonCode.EVALS_COMPLETED);
            case "INTERVIEW_SCHEDULED->EXAM_SCHEDULED" -> Optional.of(ReasonCode.INTERVIEW_PASSED);
            case "EXAM_SCHEDULED->APPROVED" -> Optional.of(ReasonCode.EXAM_PASSED);
            case "APPROVED->ENROLLED" -> Optional.of(ReasonCode.ENROLLMENT_CONFIRMED);
            case "WAITLIST->APPROVED" -> Optional.of(ReasonCode.SLOT_AVAILABLE);
            default -> validReasons.stream().findFirst(); // Primer código válido
        };
    }

    /**
     * Valida una transición con contexto adicional
     */
    public TransitionValidationResult validateTransition(
            ApplicationStatus fromState, 
            ApplicationStatus toState, 
            ReasonCode reasonCode, 
            String actorRole) {
        
        if (!isValidTransition(fromState, toState, reasonCode)) {
            return TransitionValidationResult.invalid(
                String.format("Transición %s -> %s con razón %s no está permitida", 
                    fromState, toState, reasonCode)
            );
        }

        // Validaciones adicionales basadas en el rol del actor
        if (requiresAdminRole(reasonCode) && !"ADMIN".equals(actorRole)) {
            return TransitionValidationResult.invalid(
                String.format("El código de razón %s requiere rol de administrador", reasonCode)
            );
        }

        return TransitionValidationResult.valid();
    }

    /**
     * Verifica si un código de razón requiere rol administrativo
     */
    private boolean requiresAdminRole(ReasonCode reasonCode) {
        return switch (reasonCode) {
            case ADMIN_OVERRIDE, ADMIN_REJECT, POLICY_VIOLATION, 
                 DATA_CORRECTION, ENROLLMENT_DEADLINE_EXTENDED -> true;
            default -> false;
        };
    }

    /**
     * Obtiene estadísticas de las transiciones definidas
     */
    public TransitionStatistics getTransitionStatistics() {
        int totalStates = ApplicationStatus.values().length;
        int statesWithTransitions = VALID_TRANSITIONS.size();
        int totalTransitions = VALID_TRANSITIONS.values().stream()
            .mapToInt(Map::size)
            .sum();
        int totalReasonCodes = VALID_TRANSITIONS.values().stream()
            .flatMap(m -> m.values().stream())
            .mapToInt(Set::size)
            .sum();

        return new TransitionStatistics(
            totalStates,
            statesWithTransitions,
            totalTransitions,
            totalReasonCodes
        );
    }

    // ================================
    // INNER CLASSES
    // ================================

    /**
     * Resultado de validación de transición
     */
    public static class TransitionValidationResult {
        private final boolean valid;
        private final String errorMessage;

        private TransitionValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public static TransitionValidationResult valid() {
            return new TransitionValidationResult(true, null);
        }

        public static TransitionValidationResult invalid(String message) {
            return new TransitionValidationResult(false, message);
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        @Override
        public String toString() {
            return valid ? "VALID" : "INVALID: " + errorMessage;
        }
    }

    /**
     * Estadísticas del sistema de transiciones
     */
    public record TransitionStatistics(
        int totalStates,
        int statesWithTransitions,
        int totalTransitions,
        int totalReasonCodes
    ) {
        @Override
        public String toString() {
            return String.format(
                "TransitionStatistics{states=%d, with_transitions=%d, transitions=%d, reason_codes=%d}",
                totalStates, statesWithTransitions, totalTransitions, totalReasonCodes
            );
        }
    }
}