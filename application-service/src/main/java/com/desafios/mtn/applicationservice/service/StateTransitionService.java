package com.desafios.mtn.applicationservice.service;

import com.desafios.mtn.applicationservice.domain.*;
import com.desafios.mtn.applicationservice.repository.ApplicationRepository;
import com.desafios.mtn.applicationservice.repository.OutboxEventRepository;
import com.desafios.mtn.applicationservice.repository.TransitionLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Servicio para gestión de transiciones de estado con auditoría completa
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class StateTransitionService {

    private final ApplicationRepository applicationRepository;
    private final TransitionLogRepository transitionLogRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final StateTransitionPolicy stateTransitionPolicy;

    // ================================
    // TRANSICIONES DE ESTADO PRINCIPALES
    // ================================

    /**
     * Ejecuta una transición de estado con auditoría completa
     */
    @Retryable(retryFor = ObjectOptimisticLockingFailureException.class,
               maxAttempts = 3, backoff = @Backoff(delay = 100))
    public Application transitionState(Application application, 
                                     ApplicationStatus targetStatus, 
                                     ReasonCode reasonCode, 
                                     String actorUserId, 
                                     String actorRole, 
                                     String comment) {
        return transitionState(application, targetStatus, reasonCode, actorUserId, actorRole, 
                             comment, null, null, null, null);
    }

    /**
     * Ejecuta una transición de estado con contexto completo
     */
    @Retryable(retryFor = ObjectOptimisticLockingFailureException.class,
               maxAttempts = 3, backoff = @Backoff(delay = 100))
    public Application transitionState(Application application, 
                                     ApplicationStatus targetStatus, 
                                     ReasonCode reasonCode, 
                                     String actorUserId, 
                                     String actorRole, 
                                     String comment,
                                     String idempotencyKey,
                                     Map<String, Object> transitionData,
                                     InetAddress ipAddress,
                                     String userAgent) {
        
        ApplicationStatus currentStatus = application.getStatus();
        UUID applicationId = application.getId();
        
        log.info("Attempting state transition for application {} from {} to {} with reason {}",
                applicationId, currentStatus, targetStatus, reasonCode);

        // Validar la transición
        StateTransitionPolicy.TransitionValidationResult validation = 
            stateTransitionPolicy.validateTransition(currentStatus, targetStatus, reasonCode, actorRole);
        
        if (!validation.isValid()) {
            log.warn("Invalid state transition attempted: {}", validation.getErrorMessage());
            throw new IllegalStateException(validation.getErrorMessage());
        }

        // Verificar idempotencia si se proporciona clave
        if (idempotencyKey != null && transitionLogRepository.existsByIdempotencyKey(idempotencyKey)) {
            log.info("Duplicate transition request detected with idempotency key: {}", idempotencyKey);
            // Devolver la aplicación en su estado actual
            return applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalStateException("Application not found: " + applicationId));
        }

        try {
            // Ejecutar la transición
            Application updatedApplication = executeTransition(
                application, targetStatus, reasonCode, actorUserId, actorRole, 
                comment, idempotencyKey, transitionData, ipAddress, userAgent);
            
            log.info("Successfully transitioned application {} from {} to {}",
                    applicationId, currentStatus, targetStatus);
            
            return updatedApplication;
            
        } catch (Exception e) {
            log.error("Failed to transition application {} from {} to {}: {}",
                     applicationId, currentStatus, targetStatus, e.getMessage(), e);
            throw new RuntimeException("State transition failed: " + e.getMessage(), e);
        }
    }

    /**
     * Ejecuta la transición de estado de forma atómica
     */
    private Application executeTransition(Application application, 
                                        ApplicationStatus targetStatus, 
                                        ReasonCode reasonCode, 
                                        String actorUserId, 
                                        String actorRole, 
                                        String comment,
                                        String idempotencyKey,
                                        Map<String, Object> transitionData,
                                        InetAddress ipAddress,
                                        String userAgent) {
        
        ApplicationStatus fromStatus = application.getStatus();
        UUID applicationId = application.getId();

        // 1. Crear log de transición ANTES de cambiar el estado
        TransitionLog transitionLog = TransitionLog.createFullTransition(
            applicationId, fromStatus, targetStatus, reasonCode,
            actorUserId, actorRole, comment, idempotencyKey, 
            transitionData, ipAddress, userAgent);
        
        transitionLogRepository.save(transitionLog);

        // 2. Cambiar el estado de la aplicación
        application.changeStatus(targetStatus);
        Application savedApplication = applicationRepository.save(application);

        // 3. Crear evento para el Outbox
        OutboxEvent stateChangeEvent = OutboxEvent.stateChanged(
            applicationId, fromStatus, targetStatus, reasonCode, actorUserId);
        outboxEventRepository.save(stateChangeEvent);

        // 4. Procesar efectos secundarios de la transición
        processTransitionSideEffects(savedApplication, transitionLog);

        return savedApplication;
    }

    // ================================
    // TRANSICIONES ESPECÍFICAS
    // ================================

    /**
     * Transición: DRAFT → PENDING (Envío de formulario)
     */
    public Application submitApplication(Application application, String submittedBy) {
        return transitionState(application, ApplicationStatus.PENDING, 
                             ReasonCode.FORM_SUBMITTED, submittedBy, "APODERADO",
                             "Formulario de postulación enviado");
    }

    /**
     * Transición: PENDING → UNDER_REVIEW (Documentos aprobados)
     */
    public Application approveDocuments(Application application, String reviewedBy) {
        return transitionState(application, ApplicationStatus.UNDER_REVIEW, 
                             ReasonCode.DOCS_APPROVED, reviewedBy, "ADMIN",
                             "Documentos revisados y aprobados");
    }

    /**
     * Transición: PENDING → DOCUMENTS_REQUESTED (Documentos faltantes)
     */
    public Application requestDocuments(Application application, String requestedBy, String reason) {
        return transitionState(application, ApplicationStatus.DOCUMENTS_REQUESTED, 
                             ReasonCode.DOCS_MISSING, requestedBy, "ADMIN", reason);
    }

    /**
     * Transición: UNDER_REVIEW → INTERVIEW_SCHEDULED (Evaluaciones completadas)
     */
    public Application scheduleInterview(Application application, String scheduledBy, 
                                       Map<String, Object> interviewData) {
        return transitionState(application, ApplicationStatus.INTERVIEW_SCHEDULED, 
                             ReasonCode.EVALS_COMPLETED, scheduledBy, "ADMIN",
                             "Entrevista programada con el director", null, interviewData, null, null);
    }

    /**
     * Transición: INTERVIEW_SCHEDULED → EXAM_SCHEDULED (Entrevista pasada)
     */
    public Application scheduleExam(Application application, String scheduledBy, 
                                  Map<String, Object> examData) {
        return transitionState(application, ApplicationStatus.EXAM_SCHEDULED, 
                             ReasonCode.INTERVIEW_PASSED, scheduledBy, "CYCLE_DIRECTOR",
                             "Entrevista aprobada, examen programado", null, examData, null, null);
    }

    /**
     * Transición: EXAM_SCHEDULED → APPROVED (Examen aprobado)
     */
    public Application approveApplication(Application application, String approvedBy, 
                                        Map<String, Object> evaluationResults) {
        return transitionState(application, ApplicationStatus.APPROVED, 
                             ReasonCode.EXAM_PASSED, approvedBy, "ADMIN",
                             "Examen aprobado, aplicación aceptada", null, evaluationResults, null, null);
    }

    /**
     * Transición: APPROVED → ENROLLED (Matrícula confirmada)
     */
    public Application confirmEnrollment(Application application, String confirmedBy) {
        return transitionState(application, ApplicationStatus.ENROLLED, 
                             ReasonCode.ENROLLMENT_CONFIRMED, confirmedBy, "ADMIN",
                             "Matrícula confirmada exitosamente");
    }

    /**
     * Transición: Cualquier estado → REJECTED (Rechazo)
     */
    public Application rejectApplication(Application application, String rejectedBy, 
                                       ReasonCode rejectionReason, String comment) {
        if (!rejectionReason.isNegative()) {
            throw new IllegalArgumentException("Rejection reason must be negative: " + rejectionReason);
        }
        
        return transitionState(application, ApplicationStatus.REJECTED, 
                             rejectionReason, rejectedBy, "ADMIN", comment);
    }

    /**
     * Transición: WAITLIST → APPROVED (Cupo disponible)
     */
    public Application promoteFromWaitlist(Application application, String promotedBy) {
        return transitionState(application, ApplicationStatus.APPROVED, 
                             ReasonCode.SLOT_AVAILABLE, promotedBy, "ADMIN",
                             "Cupo disponible, promovido de lista de espera");
    }

    // ================================
    // TRANSICIONES AUTOMÁTICAS DEL SISTEMA
    // ================================

    /**
     * Transición automática: APPROVED → EXPIRED (Expiración de matrícula)
     */
    public Application expireApproval(Application application) {
        return transitionState(application, ApplicationStatus.EXPIRED, 
                             ReasonCode.ENROLLMENT_EXPIRED, "SYSTEM", "SYSTEM",
                             "Plazo de matrícula expirado automáticamente");
    }

    /**
     * Transición automática: WAITLIST → EXPIRED (Lista de espera expirada)
     */
    public Application expireWaitlist(Application application) {
        return transitionState(application, ApplicationStatus.EXPIRED, 
                             ReasonCode.WAITLIST_EXPIRED, "SYSTEM", "SYSTEM",
                             "Tiempo en lista de espera expirado");
    }

    /**
     * Procesa expirations automáticas en lote
     */
    public int processAutomaticExpirations() {
        log.info("Processing automatic application expirations");
        
        int expiredCount = 0;
        
        // Expirar aprobaciones sin matrícula (30 días)
        Instant approvalCutoff = Instant.now().minusSeconds(30L * 24L * 3600L);
        List<Application> expiredApprovals = applicationRepository
            .findApplicationsExpiringApprovals(approvalCutoff);
        
        for (Application application : expiredApprovals) {
            try {
                expireApproval(application);
                expiredCount++;
            } catch (Exception e) {
                log.error("Failed to expire approval for application {}: {}", 
                         application.getId(), e.getMessage());
            }
        }
        
        log.info("Processed {} automatic expirations", expiredCount);
        return expiredCount;
    }

    // ================================
    // CONSULTAS Y VALIDACIONES
    // ================================

    /**
     * Verifica si una transición es válida
     */
    public boolean isValidTransition(ApplicationStatus fromState, ApplicationStatus toState, 
                                   ReasonCode reasonCode, String actorRole) {
        return stateTransitionPolicy.validateTransition(fromState, toState, reasonCode, actorRole)
                                   .isValid();
    }

    /**
     * Obtiene estados válidos desde un estado actual
     */
    public List<ApplicationStatus> getValidTargetStates(ApplicationStatus fromState) {
        return stateTransitionPolicy.getValidTargetStates(fromState).stream().toList();
    }

    /**
     * Obtiene códigos de razón válidos para una transición
     */
    public List<ReasonCode> getValidReasonCodes(ApplicationStatus fromState, ApplicationStatus toState) {
        return stateTransitionPolicy.getValidReasonCodes(fromState, toState).stream().toList();
    }

    /**
     * Obtiene el historial de transiciones de una aplicación
     */
    public List<TransitionLog> getTransitionHistory(UUID applicationId) {
        return transitionLogRepository.findByApplicationIdOrderByCreatedAtDesc(applicationId);
    }

    /**
     * Obtiene la transición más reciente de una aplicación
     */
    public Optional<TransitionLog> getLastTransition(UUID applicationId) {
        return transitionLogRepository.findFirstByApplicationIdOrderByCreatedAtDesc(applicationId);
    }

    // ================================
    // EFECTOS SECUNDARIOS
    // ================================

    /**
     * Procesa efectos secundarios de una transición
     */
    private void processTransitionSideEffects(Application application, TransitionLog transition) {
        ApplicationStatus newStatus = transition.getToState();
        
        // Crear eventos adicionales según el nuevo estado
        switch (newStatus) {
            case DOCUMENTS_REQUESTED -> scheduleDocumentReminderNotification(application);
            case INTERVIEW_SCHEDULED -> scheduleInterviewNotification(application);
            case EXAM_SCHEDULED -> scheduleExamNotification(application);
            case APPROVED -> scheduleEnrollmentReminderNotification(application);
            case ENROLLED -> createEnrollmentCompletedEvent(application);
            case REJECTED -> createRejectionNotificationEvent(application);
            default -> { /* No hay efectos secundarios específicos */ }
        }
    }

    /**
     * Programa notificación de documentos solicitados
     */
    private void scheduleDocumentReminderNotification(Application application) {
        Map<String, Object> payload = Map.of(
            "applicationId", application.getId(),
            "notificationType", "DOCUMENTS_REQUESTED",
            "recipientEmail", application.getPrimaryContactEmail(),
            "applicantName", application.getApplicantName()
        );
        
        OutboxEvent event = OutboxEvent.createScheduled(
            "Application", application.getId(),
            "DocumentReminderScheduled.v1", payload,
            Instant.now().plusSeconds(3600) // 1 hora después
        );
        
        outboxEventRepository.save(event);
    }

    /**
     * Programa notificación de entrevista
     */
    private void scheduleInterviewNotification(Application application) {
        Map<String, Object> payload = Map.of(
            "applicationId", application.getId(),
            "notificationType", "INTERVIEW_SCHEDULED",
            "recipientEmail", application.getPrimaryContactEmail(),
            "applicantName", application.getApplicantName()
        );
        
        OutboxEvent event = OutboxEvent.createScheduled(
            "Application", application.getId(),
            "InterviewNotificationScheduled.v1", payload,
            Instant.now().plusSeconds(1800) // 30 minutos después
        );
        
        outboxEventRepository.save(event);
    }

    /**
     * Programa notificación de examen
     */
    private void scheduleExamNotification(Application application) {
        Map<String, Object> payload = Map.of(
            "applicationId", application.getId(),
            "notificationType", "EXAM_SCHEDULED",
            "recipientEmail", application.getPrimaryContactEmail(),
            "applicantName", application.getApplicantName()
        );
        
        OutboxEvent event = OutboxEvent.createScheduled(
            "Application", application.getId(),
            "ExamNotificationScheduled.v1", payload,
            Instant.now().plusSeconds(1800) // 30 minutos después
        );
        
        outboxEventRepository.save(event);
    }

    /**
     * Programa recordatorio de matrícula
     */
    private void scheduleEnrollmentReminderNotification(Application application) {
        Map<String, Object> payload = Map.of(
            "applicationId", application.getId(),
            "notificationType", "ENROLLMENT_REMINDER",
            "recipientEmail", application.getPrimaryContactEmail(),
            "applicantName", application.getApplicantName()
        );
        
        // Recordatorio en 7 días
        OutboxEvent reminderEvent = OutboxEvent.createScheduled(
            "Application", application.getId(),
            "EnrollmentReminderScheduled.v1", payload,
            Instant.now().plusSeconds(7L * 24L * 3600L)
        );
        
        outboxEventRepository.save(reminderEvent);
    }

    /**
     * Crea evento de matrícula completada
     */
    private void createEnrollmentCompletedEvent(Application application) {
        Map<String, Object> payload = Map.of(
            "applicationId", application.getId(),
            "applicantName", application.getApplicantName(),
            "gradeApplied", application.getGradeApplied(),
            "enrolledAt", application.getEnrolledAt().toString()
        );
        
        OutboxEvent event = OutboxEvent.builder()
            .aggregateType("Application")
            .aggregateId(application.getId())
            .eventType("EnrollmentCompleted.v1")
            .payload(payload)
            .routingKey("application.enrollment.completed")
            .exchangeName("admission.events")
            .priority(OutboxEvent.EventPriority.HIGH)
            .build();
        
        outboxEventRepository.save(event);
    }

    /**
     * Crea evento de rechazo
     */
    private void createRejectionNotificationEvent(Application application) {
        Map<String, Object> payload = Map.of(
            "applicationId", application.getId(),
            "applicantName", application.getApplicantName(),
            "rejectedAt", application.getRejectedAt().toString()
        );
        
        OutboxEvent event = OutboxEvent.builder()
            .aggregateType("Application")
            .aggregateId(application.getId())
            .eventType("ApplicationRejected.v1")
            .payload(payload)
            .routingKey("application.rejected")
            .exchangeName("admission.events")
            .priority(OutboxEvent.EventPriority.HIGH)
            .build();
        
        outboxEventRepository.save(event);
    }
}