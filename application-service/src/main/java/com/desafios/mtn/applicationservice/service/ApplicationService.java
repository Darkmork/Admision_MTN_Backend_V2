package com.desafios.mtn.applicationservice.service;

import com.desafios.mtn.applicationservice.domain.Application;
import com.desafios.mtn.applicationservice.domain.ApplicationStatus;
import com.desafios.mtn.applicationservice.domain.OutboxEvent;
import com.desafios.mtn.applicationservice.repository.ApplicationRepository;
import com.desafios.mtn.applicationservice.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Servicio principal para gestión de aplicaciones de admisión
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final StateTransitionService stateTransitionService;

    // ================================
    // OPERACIONES CRUD
    // ================================

    /**
     * Crea una nueva aplicación en estado DRAFT
     */
    public Application createApplication(String createdBy, String gradeApplied, 
                                       Map<String, Object> applicantData, 
                                       Map<String, Object> familyContext) {
        log.info("Creating new application for user: {} grade: {}", createdBy, gradeApplied);
        
        // Verificar si el usuario ya tiene una aplicación activa
        if (hasActiveApplication(createdBy)) {
            throw new IllegalStateException(
                String.format("User %s already has an active application", createdBy));
        }

        Application application = Application.createWithApplicantData(
            createdBy, gradeApplied, applicantData, familyContext);

        Application savedApplication = applicationRepository.save(application);
        
        log.info("Created application: {}", savedApplication.getSummary());
        return savedApplication;
    }

    /**
     * Obtiene una aplicación por ID
     */
    @Transactional(readOnly = true)
    public Optional<Application> findById(UUID applicationId) {
        return applicationRepository.findById(applicationId);
    }

    /**
     * Obtiene una aplicación por ID con validación
     */
    @Transactional(readOnly = true)
    public Application getById(UUID applicationId) {
        return applicationRepository.findById(applicationId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Application not found with ID: " + applicationId));
    }

    /**
     * Actualiza datos de una aplicación (solo si está en DRAFT)
     */
    @Retryable(retryFor = ObjectOptimisticLockingFailureException.class,
               maxAttempts = 3, backoff = @Backoff(delay = 100))
    public Application updateApplication(UUID applicationId, 
                                       Map<String, Object> applicantData, 
                                       Map<String, Object> familyContext, 
                                       String notes) {
        log.info("Updating application: {}", applicationId);
        
        Application application = getById(applicationId);
        
        if (!application.isDraft()) {
            throw new IllegalStateException(
                String.format("Cannot update application %s in state %s", 
                    applicationId, application.getStatus()));
        }

        // Actualizar datos
        if (applicantData != null) {
            application.setApplicant(applicantData);
        }
        if (familyContext != null) {
            application.setFamilyContext(familyContext);
        }
        if (notes != null) {
            application.setNotes(notes);
        }

        Application savedApplication = applicationRepository.save(application);
        log.info("Updated application: {}", savedApplication.getSummary());
        return savedApplication;
    }

    /**
     * Envía una aplicación (DRAFT → PENDING)
     */
    @Retryable(retryFor = ObjectOptimisticLockingFailureException.class,
               maxAttempts = 3, backoff = @Backoff(delay = 100))
    public Application submitApplication(UUID applicationId, String submittedBy) {
        log.info("Submitting application: {} by user: {}", applicationId, submittedBy);
        
        Application application = getById(applicationId);
        
        if (!application.isDraft()) {
            throw new IllegalStateException(
                String.format("Cannot submit application %s in state %s", 
                    applicationId, application.getStatus()));
        }

        // Validar que la aplicación esté completa
        validateApplicationCompleteness(application);

        // Realizar transición de estado
        application = stateTransitionService.transitionState(
            application, ApplicationStatus.PENDING, 
            com.desafios.mtn.applicationservice.domain.ReasonCode.FORM_SUBMITTED,
            submittedBy, "APODERADO", 
            "Formulario de postulación enviado");

        // Crear evento de aplicación enviada
        OutboxEvent event = OutboxEvent.applicationSubmitted(
            application.getId(), 
            createApplicationEventData(application));
        outboxEventRepository.save(event);

        log.info("Submitted application: {}", application.getSummary());
        return application;
    }

    // ================================
    // CONSULTAS Y BÚSQUEDAS
    // ================================

    /**
     * Obtiene aplicaciones de un usuario
     */
    @Transactional(readOnly = true)
    public List<Application> findUserApplications(String userId) {
        return applicationRepository.findByCreatedBy(userId);
    }

    /**
     * Obtiene aplicaciones de un usuario con paginación
     */
    @Transactional(readOnly = true)
    public Page<Application> findUserApplications(String userId, Pageable pageable) {
        return applicationRepository.findByCreatedBy(userId, pageable);
    }

    /**
     * Obtiene la aplicación activa de un usuario
     */
    @Transactional(readOnly = true)
    public Optional<Application> findActiveUserApplication(String userId) {
        return applicationRepository.findActiveApplicationByUser(userId);
    }

    /**
     * Verifica si un usuario tiene una aplicación activa
     */
    @Transactional(readOnly = true)
    public boolean hasActiveApplication(String userId) {
        return applicationRepository.hasActiveApplication(userId);
    }

    /**
     * Busca aplicaciones por estado
     */
    @Transactional(readOnly = true)
    public List<Application> findByStatus(ApplicationStatus status) {
        return applicationRepository.findByStatus(status);
    }

    /**
     * Busca aplicaciones por estado con paginación
     */
    @Transactional(readOnly = true)
    public Page<Application> findByStatus(ApplicationStatus status, Pageable pageable) {
        return applicationRepository.findByStatus(status, pageable);
    }

    /**
     * Busca aplicaciones por múltiples estados
     */
    @Transactional(readOnly = true)
    public List<Application> findByStatusIn(List<ApplicationStatus> statuses) {
        return applicationRepository.findByStatusIn(statuses);
    }

    /**
     * Busca aplicaciones por grado
     */
    @Transactional(readOnly = true)
    public List<Application> findByGrade(String grade) {
        return applicationRepository.findByGradeApplied(grade);
    }

    /**
     * Busca aplicaciones activas por grado
     */
    @Transactional(readOnly = true)
    public List<Application> findActiveApplicationsByGrade(String grade) {
        return applicationRepository.findActiveApplicationsByGrade(grade);
    }

    /**
     * Busca aplicaciones que requieren acción del apoderado
     */
    @Transactional(readOnly = true)
    public List<Application> findApplicationsRequiringParentAction() {
        return applicationRepository.findApplicationsRequiringParentAction();
    }

    /**
     * Busca aplicaciones que requieren acción administrativa
     */
    @Transactional(readOnly = true)
    public List<Application> findApplicationsRequiringAdminAction() {
        return applicationRepository.findApplicationsRequiringAdminAction();
    }

    /**
     * Busca aplicaciones por texto
     */
    @Transactional(readOnly = true)
    public List<Application> searchApplications(String searchText) {
        return applicationRepository.searchApplicationsByText(searchText);
    }

    // ================================
    // OPERACIONES DE VALIDACIÓN
    // ================================

    /**
     * Valida que una aplicación esté completa para envío
     */
    private void validateApplicationCompleteness(Application application) {
        if (application.getApplicant() == null || application.getApplicant().isEmpty()) {
            throw new IllegalStateException("Applicant data is required");
        }

        if (application.getFamilyContext() == null || application.getFamilyContext().isEmpty()) {
            throw new IllegalStateException("Family context data is required");
        }

        // Validar campos requeridos del postulante
        Map<String, Object> applicant = application.getApplicant();
        validateRequiredField(applicant, "nombre", "Applicant name is required");
        validateRequiredField(applicant, "rut", "Applicant RUT is required");
        validateRequiredField(applicant, "fecha_nacimiento", "Applicant birth date is required");

        // Validar datos familiares
        Map<String, Object> familyContext = application.getFamilyContext();
        if (!familyContext.containsKey("padre") && !familyContext.containsKey("madre")) {
            throw new IllegalStateException("At least one parent's information is required");
        }
    }

    /**
     * Valida que un campo requerido esté presente y no vacío
     */
    private void validateRequiredField(Map<String, Object> data, String field, String errorMessage) {
        Object value = data.get(field);
        if (value == null || (value instanceof String && ((String) value).trim().isEmpty())) {
            throw new IllegalStateException(errorMessage);
        }
    }

    /**
     * Verifica si hay aplicaciones duplicadas por RUT
     */
    @Transactional(readOnly = true)
    public List<Application> findDuplicateApplications(String applicantRut, UUID excludeId) {
        return applicationRepository.findDuplicateApplicationsByRut(applicantRut, excludeId);
    }

    // ================================
    // ESTADÍSTICAS Y REPORTES
    // ================================

    /**
     * Obtiene estadísticas por estado
     */
    @Transactional(readOnly = true)
    public List<Object[]> getStatusStatistics() {
        return applicationRepository.getStatusStatistics();
    }

    /**
     * Obtiene estadísticas por grado
     */
    @Transactional(readOnly = true)
    public List<Object[]> getGradeStatistics() {
        return applicationRepository.getGradeStatistics();
    }

    /**
     * Obtiene estadísticas mensuales
     */
    @Transactional(readOnly = true)
    public List<Object[]> getMonthlyStatistics() {
        return applicationRepository.getMonthlyCreationStatistics();
    }

    /**
     * Cuenta aplicaciones por estado
     */
    @Transactional(readOnly = true)
    public long countByStatus(ApplicationStatus status) {
        return applicationRepository.countByStatus(status);
    }

    /**
     * Cuenta aplicaciones por grado y estado
     */
    @Transactional(readOnly = true)
    public long countByGradeAndStatus(String grade, ApplicationStatus status) {
        return applicationRepository.countByGradeAndStatus(grade, status);
    }

    // ================================
    // OPERACIONES DE MANTENIMIENTO
    // ================================

    /**
     * Encuentra aplicaciones sin actividad reciente
     */
    @Transactional(readOnly = true)
    public List<Application> findStaleApplications(int daysWithoutActivity) {
        Instant cutoffDate = Instant.now().minusSeconds(daysWithoutActivity * 24L * 3600L);
        return applicationRepository.findStaleApplications(cutoffDate);
    }

    /**
     * Encuentra aplicaciones con aprobaciones que expiran pronto
     */
    @Transactional(readOnly = true)
    public List<Application> findApplicationsExpiringApprovals(int daysUntilExpiration) {
        Instant cutoffDate = Instant.now().minusSeconds(daysUntilExpiration * 24L * 3600L);
        return applicationRepository.findApplicationsExpiringApprovals(cutoffDate);
    }

    /**
     * Encuentra aplicaciones pendientes antiguas
     */
    @Transactional(readOnly = true)
    public long countOldPendingApplications(int maxDays) {
        Instant cutoffDate = Instant.now().minusSeconds(maxDays * 24L * 3600L);
        return applicationRepository.countOldPendingApplications(cutoffDate);
    }

    // ================================
    // MÉTODOS AUXILIARES
    // ================================

    /**
     * Crea datos de evento para aplicación
     */
    private Map<String, Object> createApplicationEventData(Application application) {
        return Map.of(
            "applicationId", application.getId(),
            "createdBy", application.getCreatedBy(),
            "status", application.getStatus().getCode(),
            "gradeApplied", application.getGradeApplied(),
            "applicantName", application.getApplicantName(),
            "applicantRut", application.getApplicantRut(),
            "submittedAt", application.getSubmittedAt() != null ? 
                application.getSubmittedAt().toString() : null,
            "timestamp", Instant.now().toString()
        );
    }

    /**
     * Refrescar una entidad desde la base de datos (para manejar optimistic locking)
     */
    public Application refresh(Application application) {
        return applicationRepository.findById(application.getId())
            .orElseThrow(() -> new IllegalStateException("Application no longer exists"));
    }
}