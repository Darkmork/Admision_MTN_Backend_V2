package com.desafios.mtn.evaluationservice.service;

import com.desafios.mtn.evaluationservice.domain.Evaluation;
import com.desafios.mtn.evaluationservice.domain.Evaluation.*;
import com.desafios.mtn.evaluationservice.repository.EvaluationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio de dominio para la gestión de evaluaciones académicas y psicológicas
 * Implementa la lógica de negocio para el proceso completo de evaluaciones
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class EvaluationService {

    private final EvaluationRepository evaluationRepository;
    private final EvaluatorAssignmentService evaluatorAssignmentService;
    private final EvaluationEventPublisher evaluationEventPublisher;
    private final EvaluationSlaService evaluationSlaService;

    // ================================
    // CORE EVALUATION OPERATIONS
    // ================================

    /**
     * Crea una nueva evaluación pendiente
     */
    public Evaluation createEvaluation(UUID applicationId, Subject subject, Level level, 
                                     Integer priority, String createdBy) {
        log.info("Creating evaluation for application {} - Subject: {}, Level: {}", 
                applicationId, subject, level);

        // Validar que no exista una evaluación activa para la misma aplicación y materia
        if (hasActiveEvaluation(applicationId, subject)) {
            throw new IllegalStateException(
                String.format("Active evaluation already exists for application %s and subject %s", 
                             applicationId, subject));
        }

        Evaluation evaluation = Evaluation.createPending(applicationId, subject, level, createdBy);
        if (priority != null) {
            evaluation.setPriority(priority);
        }

        evaluation = evaluationRepository.save(evaluation);
        log.info("Created evaluation: {}", evaluation.getSummary());

        // Publicar evento de evaluación creada
        evaluationEventPublisher.publishEvaluationCreated(evaluation);

        return evaluation;
    }

    /**
     * Asigna una evaluación a un evaluador específico
     */
    public Evaluation assignEvaluation(UUID evaluationId, String evaluatorId, 
                                     AssignmentReason reason, String assignedBy) {
        log.info("Assigning evaluation {} to evaluator {}", evaluationId, evaluatorId);

        Evaluation evaluation = getEvaluationById(evaluationId);
        
        // Validar que el evaluador esté disponible y calificado
        evaluatorAssignmentService.validateEvaluatorAvailability(evaluatorId, evaluation.getSubject(), evaluation.getLevel());

        // Calcular tiempo esperado de completación basado en SLA
        Instant expectedCompletion = evaluationSlaService.calculateExpectedCompletion(
            evaluation.getSubject(), evaluation.getLevel(), evaluation.getPriority());

        evaluation.assign(evaluatorId, reason, assignedBy, expectedCompletion);
        evaluation = evaluationRepository.save(evaluation);

        log.info("Assigned evaluation: {}", evaluation.getSummary());

        // Publicar evento de asignación
        evaluationEventPublisher.publishEvaluationAssigned(evaluation);

        return evaluation;
    }

    /**
     * Asignación automática de evaluaciones pendientes
     */
    public List<Evaluation> assignPendingEvaluationsAutomatically() {
        log.info("Starting automatic assignment of pending evaluations");

        List<Evaluation> assignedEvaluations = new ArrayList<>();
        
        // Buscar evaluaciones pendientes ordenadas por prioridad
        List<Evaluation> pendingEvaluations = evaluationRepository.findByStatus(EvaluationStatus.PENDING);
        
        for (Evaluation evaluation : pendingEvaluations) {
            try {
                String bestEvaluator = evaluatorAssignmentService.findBestAvailableEvaluator(
                    evaluation.getSubject(), evaluation.getLevel());
                
                if (bestEvaluator != null) {
                    assignEvaluation(evaluation.getId(), bestEvaluator, 
                                   AssignmentReason.AUTO_ASSIGNED, "SYSTEM");
                    assignedEvaluations.add(evaluation);
                }
            } catch (Exception e) {
                log.warn("Failed to auto-assign evaluation {}: {}", 
                        evaluation.getId(), e.getMessage());
            }
        }

        log.info("Auto-assigned {} evaluations", assignedEvaluations.size());
        return assignedEvaluations;
    }

    /**
     * Reasigna una evaluación a otro evaluador
     */
    public Evaluation reassignEvaluation(UUID evaluationId, String newEvaluatorId, 
                                       String reason, String reassignedBy) {
        log.info("Reassigning evaluation {} to new evaluator {}", evaluationId, newEvaluatorId);

        Evaluation evaluation = getEvaluationById(evaluationId);

        // Validar que el nuevo evaluador esté disponible
        evaluatorAssignmentService.validateEvaluatorAvailability(newEvaluatorId, 
                                                               evaluation.getSubject(), 
                                                               evaluation.getLevel());

        // Calcular nueva fecha esperada
        Instant expectedCompletion = evaluationSlaService.calculateExpectedCompletion(
            evaluation.getSubject(), evaluation.getLevel(), evaluation.getPriority());

        evaluation.reassign(newEvaluatorId, reason, reassignedBy, expectedCompletion);
        evaluation = evaluationRepository.save(evaluation);

        log.info("Reassigned evaluation: {}", evaluation.getSummary());

        // Publicar evento de reasignación
        evaluationEventPublisher.publishEvaluationReassigned(evaluation);

        return evaluation;
    }

    /**
     * Inicia una evaluación
     */
    public Evaluation startEvaluation(UUID evaluationId, String startedBy) {
        log.info("Starting evaluation {}", evaluationId);

        Evaluation evaluation = getEvaluationById(evaluationId);
        evaluation.start(startedBy);
        evaluation = evaluationRepository.save(evaluation);

        log.info("Started evaluation: {}", evaluation.getSummary());

        // Publicar evento de inicio
        evaluationEventPublisher.publishEvaluationStarted(evaluation);

        return evaluation;
    }

    /**
     * Completa una evaluación con resultados
     */
    public Evaluation completeEvaluation(UUID evaluationId, BigDecimal totalScore, 
                                       BigDecimal maxScore, String notes, String completedBy) {
        log.info("Completing evaluation {} with score {}/{}", evaluationId, totalScore, maxScore);

        Evaluation evaluation = getEvaluationById(evaluationId);
        evaluation.complete(totalScore, maxScore, completedBy, notes);
        evaluation = evaluationRepository.save(evaluation);

        log.info("Completed evaluation: {} - Passed: {}, Percentage: {}%", 
                evaluation.getSummary(), evaluation.getPassed(), evaluation.getPercentage());

        // Publicar evento de completación
        evaluationEventPublisher.publishEvaluationCompleted(evaluation);

        // Verificar si todas las evaluaciones de la aplicación están completadas
        checkAndNotifyApplicationEvaluationsCompleted(evaluation.getApplicationId());

        return evaluation;
    }

    /**
     * Cancela una evaluación
     */
    public Evaluation cancelEvaluation(UUID evaluationId, String reason, String cancelledBy) {
        log.info("Cancelling evaluation {}: {}", evaluationId, reason);

        Evaluation evaluation = getEvaluationById(evaluationId);
        evaluation.cancel(reason, cancelledBy);
        evaluation = evaluationRepository.save(evaluation);

        log.info("Cancelled evaluation: {}", evaluation.getSummary());

        // Publicar evento de cancelación
        evaluationEventPublisher.publishEvaluationCancelled(evaluation);

        return evaluation;
    }

    // ================================
    // QUERY OPERATIONS
    // ================================

    /**
     * Obtiene una evaluación por ID
     */
    @Transactional(readOnly = true)
    public Evaluation getEvaluationById(UUID evaluationId) {
        return evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new IllegalArgumentException(
                    "Evaluation not found with id: " + evaluationId));
    }

    /**
     * Obtiene evaluaciones de una aplicación
     */
    @Transactional(readOnly = true)
    public List<Evaluation> getEvaluationsByApplication(UUID applicationId) {
        return evaluationRepository.findByApplicationId(applicationId);
    }

    /**
     * Obtiene evaluaciones asignadas a un evaluador
     */
    @Transactional(readOnly = true)
    public List<Evaluation> getEvaluationsByEvaluator(String evaluatorId) {
        return evaluationRepository.findByEvaluatorId(evaluatorId);
    }

    /**
     * Obtiene evaluaciones activas de un evaluador
     */
    @Transactional(readOnly = true)
    public List<Evaluation> getActiveEvaluationsByEvaluator(String evaluatorId) {
        List<EvaluationStatus> activeStatuses = Arrays.asList(
            EvaluationStatus.ASSIGNED, EvaluationStatus.IN_PROGRESS);
        return evaluationRepository.findByEvaluatorIdAndStatusIn(evaluatorId, activeStatuses);
    }

    /**
     * Obtiene evaluaciones por estado
     */
    @Transactional(readOnly = true)
    public List<Evaluation> getEvaluationsByStatus(EvaluationStatus status) {
        return evaluationRepository.findByStatus(status);
    }

    /**
     * Obtiene evaluaciones vencidas
     */
    @Transactional(readOnly = true)
    public List<Evaluation> getOverdueEvaluations() {
        List<EvaluationStatus> activeStatuses = Arrays.asList(
            EvaluationStatus.ASSIGNED, EvaluationStatus.IN_PROGRESS);
        return evaluationRepository.findOverdueEvaluations(Instant.now(), activeStatuses);
    }

    /**
     * Obtiene evaluaciones que requieren atención urgente
     */
    @Transactional(readOnly = true)
    public List<Evaluation> getUrgentEvaluations() {
        Instant now = Instant.now();
        Instant twoDaysAgo = now.minus(2, ChronoUnit.DAYS);
        List<EvaluationStatus> activeStatuses = Arrays.asList(
            EvaluationStatus.ASSIGNED, EvaluationStatus.IN_PROGRESS);
        
        return evaluationRepository.findUrgentEvaluations(now, twoDaysAgo, activeStatuses);
    }

    // ================================
    // BUSINESS LOGIC OPERATIONS
    // ================================

    /**
     * Verifica si existe una evaluación activa para una aplicación y materia
     */
    @Transactional(readOnly = true)
    public boolean hasActiveEvaluation(UUID applicationId, Subject subject) {
        List<EvaluationStatus> activeStatuses = Arrays.asList(
            EvaluationStatus.PENDING, EvaluationStatus.ASSIGNED, EvaluationStatus.IN_PROGRESS);
        return evaluationRepository.existsActiveEvaluationByApplicationAndSubject(
            applicationId, subject, activeStatuses);
    }

    /**
     * Verifica si todas las evaluaciones de una aplicación están completadas
     */
    @Transactional(readOnly = true)
    public boolean areAllEvaluationsCompleted(UUID applicationId) {
        Long incompleteCount = evaluationRepository.countIncompleteEvaluationsByApplication(applicationId);
        return incompleteCount == 0;
    }

    /**
     * Obtiene el resultado general de las evaluaciones de una aplicación
     */
    @Transactional(readOnly = true)
    public ApplicationEvaluationResult getApplicationEvaluationResult(UUID applicationId) {
        List<Evaluation> completedEvaluations = evaluationRepository
            .findCompletedEvaluationsByApplication(applicationId);

        if (completedEvaluations.isEmpty()) {
            return ApplicationEvaluationResult.builder()
                .applicationId(applicationId)
                .completed(false)
                .overallPassed(false)
                .evaluations(Collections.emptyList())
                .build();
        }

        boolean allCompleted = areAllEvaluationsCompleted(applicationId);
        boolean overallPassed = completedEvaluations.stream()
            .allMatch(eval -> eval.getPassed() != null && eval.getPassed());

        Double averageScore = completedEvaluations.stream()
            .filter(eval -> eval.getPercentage() != null)
            .mapToDouble(eval -> eval.getPercentage().doubleValue())
            .average()
            .orElse(0.0);

        return ApplicationEvaluationResult.builder()
            .applicationId(applicationId)
            .completed(allCompleted)
            .overallPassed(overallPassed)
            .averageScore(averageScore)
            .evaluations(completedEvaluations)
            .completedCount(completedEvaluations.size())
            .build();
    }

    /**
     * Calcula la carga de trabajo de los evaluadores
     */
    @Transactional(readOnly = true)
    public Map<String, Integer> getEvaluatorWorkload() {
        List<EvaluationStatus> activeStatuses = Arrays.asList(
            EvaluationStatus.ASSIGNED, EvaluationStatus.IN_PROGRESS);
        
        List<Object[]> workloadData = evaluationRepository.findWorkloadDistribution(activeStatuses);
        
        return workloadData.stream()
            .collect(Collectors.toMap(
                row -> (String) row[0],  // evaluatorId
                row -> ((Long) row[1]).intValue()  // workload count
            ));
    }

    // ================================
    // MAINTENANCE AND MONITORING
    // ================================

    /**
     * Procesa evaluaciones vencidas y las marca con SLA excedido
     */
    @Transactional
    public int processOverdueEvaluations() {
        log.info("Processing overdue evaluations");

        List<Evaluation> overdueEvaluations = getOverdueEvaluations();
        int processedCount = 0;

        for (Evaluation evaluation : overdueEvaluations) {
            if (!evaluation.getSlaExceeded()) {
                evaluation.setSlaExceeded(true);
                evaluationRepository.save(evaluation);
                processedCount++;

                // Publicar evento de SLA excedido
                evaluationEventPublisher.publishEvaluationSlaExceeded(evaluation);
            }
        }

        log.info("Processed {} overdue evaluations", processedCount);
        return processedCount;
    }

    /**
     * Reasigna automáticamente evaluaciones estancadas
     */
    @Transactional
    public List<Evaluation> reassignStaleEvaluations() {
        log.info("Checking for stale evaluations to reassign");

        List<Evaluation> reassignedEvaluations = new ArrayList<>();
        Instant cutoffTime = Instant.now().minus(3, ChronoUnit.DAYS); // 3 días sin progreso

        List<Evaluation> staleEvaluations = evaluationRepository
            .findEvaluationsForReassignment(cutoffTime);

        for (Evaluation evaluation : staleEvaluations) {
            try {
                String newEvaluator = evaluatorAssignmentService.findBestAvailableEvaluator(
                    evaluation.getSubject(), evaluation.getLevel(), evaluation.getEvaluatorId());

                if (newEvaluator != null) {
                    reassignEvaluation(evaluation.getId(), newEvaluator, 
                                     "Automatic reassignment due to inactivity", "SYSTEM");
                    reassignedEvaluations.add(evaluation);
                }
            } catch (Exception e) {
                log.warn("Failed to reassign stale evaluation {}: {}", 
                        evaluation.getId(), e.getMessage());
            }
        }

        log.info("Reassigned {} stale evaluations", reassignedEvaluations.size());
        return reassignedEvaluations;
    }

    // ================================
    // HELPER METHODS
    // ================================

    /**
     * Verifica y notifica si todas las evaluaciones de una aplicación están completadas
     */
    private void checkAndNotifyApplicationEvaluationsCompleted(UUID applicationId) {
        if (areAllEvaluationsCompleted(applicationId)) {
            ApplicationEvaluationResult result = getApplicationEvaluationResult(applicationId);
            evaluationEventPublisher.publishApplicationEvaluationsCompleted(result);
        }
    }

    // ================================
    // DATA TRANSFER OBJECTS
    // ================================

    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class ApplicationEvaluationResult {
        private UUID applicationId;
        private boolean completed;
        private boolean overallPassed;
        private Double averageScore;
        private List<Evaluation> evaluations;
        private int completedCount;
        private Instant completedAt;
    }

    /**
     * Estadísticas de evaluaciones
     */
    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class EvaluationStatistics {
        private int totalEvaluations;
        private int pendingEvaluations;
        private int assignedEvaluations;
        private int inProgressEvaluations;
        private int completedEvaluations;
        private int cancelledEvaluations;
        private int overdueEvaluations;
        private int slaExceededEvaluations;
        private Double averageProcessingTime;
        private Double averageScore;
        private Double passRate;
    }

    /**
     * Obtiene estadísticas generales de evaluaciones
     */
    @Transactional(readOnly = true)
    public EvaluationStatistics getEvaluationStatistics(Instant start, Instant end) {
        List<Object[]> stats = evaluationRepository.findEvaluationStatistics(start, end);
        
        // Procesar estadísticas y construir resultado
        EvaluationStatistics.EvaluationStatisticsBuilder builder = EvaluationStatistics.builder();
        
        int total = 0;
        for (Object[] row : stats) {
            EvaluationStatus status = (EvaluationStatus) row[0];
            Long count = (Long) row[1];
            Double avgTime = (Double) row[2];
            Long slaBreaches = (Long) row[3];
            
            total += count.intValue();
            
            switch (status) {
                case PENDING -> builder.pendingEvaluations(count.intValue());
                case ASSIGNED -> builder.assignedEvaluations(count.intValue());
                case IN_PROGRESS -> builder.inProgressEvaluations(count.intValue());
                case COMPLETED -> {
                    builder.completedEvaluations(count.intValue());
                    builder.averageProcessingTime(avgTime);
                }
                case CANCELLED -> builder.cancelledEvaluations(count.intValue());
            }
        }

        return builder.totalEvaluations(total).build();
    }
}