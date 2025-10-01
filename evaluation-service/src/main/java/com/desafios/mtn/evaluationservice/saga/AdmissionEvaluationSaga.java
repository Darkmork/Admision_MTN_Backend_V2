package com.desafios.mtn.evaluationservice.saga;

import com.desafios.mtn.evaluationservice.service.EvaluationService;
import com.desafios.mtn.evaluationservice.service.InterviewService;
import com.desafios.mtn.evaluationservice.service.EvaluationEventPublisher;
import com.desafios.mtn.evaluationservice.domain.Evaluation;
import com.desafios.mtn.evaluationservice.domain.Interview;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.*;

/**
 * Saga para coordinar el proceso completo de evaluación de admisión
 * Implementa el patrón Saga para manejar transacciones distribuidas
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdmissionEvaluationSaga {

    private final EvaluationService evaluationService;
    private final InterviewService interviewService;
    private final EvaluationEventPublisher evaluationEventPublisher;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Value("${application.saga.application-service-url}")
    private String applicationServiceUrl;

    @Value("${application.saga.fallback-enabled:true}")
    private boolean fallbackEnabled;

    // ================================
    // SAGA EVENT HANDLERS
    // ================================

    /**
     * Inicia la saga cuando se completan todas las evaluaciones de una aplicación
     */
    @RabbitListener(queues = "saga.evaluations-completed.queue")
    @Transactional
    public void handleEvaluationsCompleted(String message) {
        try {
            log.info("Starting admission evaluation saga for completed evaluations");
            
            Map<String, Object> eventData = objectMapper.readValue(message, Map.class);
            UUID applicationId = UUID.fromString((String) eventData.get("application_id"));
            Boolean overallPassed = (Boolean) eventData.get("overall_passed");
            Double averageScore = (Double) eventData.get("average_score");
            
            SagaState sagaState = SagaState.builder()
                .sagaId(UUID.randomUUID())
                .applicationId(applicationId)
                .currentStep(SagaStep.EVALUATIONS_COMPLETED)
                .overallPassed(overallPassed)
                .averageScore(averageScore)
                .startedAt(Instant.now())
                .build();

            log.info("Saga {} started for application {} - Passed: {}, Score: {}", 
                    sagaState.getSagaId(), applicationId, overallPassed, averageScore);

            if (overallPassed) {
                proceedWithApprovalProcess(sagaState);
            } else {
                proceedWithRejectionProcess(sagaState);
            }

        } catch (Exception e) {
            log.error("Error in evaluations completed saga", e);
            if (fallbackEnabled) {
                handleSagaFailure(null, SagaStep.EVALUATIONS_COMPLETED, e);
            } else {
                throw new RuntimeException("Saga execution failed", e);
            }
        }
    }

    /**
     * Maneja la finalización de entrevistas para continuar la saga
     */
    @RabbitListener(queues = "saga.interviews-completed.queue")
    @Transactional
    public void handleInterviewsCompleted(String message) {
        try {
            log.info("Processing interviews completed in saga");
            
            Map<String, Object> eventData = objectMapper.readValue(message, Map.class);
            UUID applicationId = UUID.fromString((String) eventData.get("application_id"));
            
            // Obtener resultados de entrevistas
            List<Interview> completedInterviews = interviewService.getInterviewsByApplication(applicationId)
                .stream()
                .filter(Interview::isCompleted)
                .toList();

            if (completedInterviews.isEmpty()) {
                log.warn("No completed interviews found for application {}", applicationId);
                return;
            }

            // Evaluar resultados de entrevistas
            boolean interviewsPassed = evaluateInterviewResults(completedInterviews);
            
            SagaState sagaState = SagaState.builder()
                .sagaId(UUID.randomUUID())
                .applicationId(applicationId)
                .currentStep(SagaStep.INTERVIEWS_COMPLETED)
                .interviewsPassed(interviewsPassed)
                .startedAt(Instant.now())
                .build();

            if (interviewsPassed) {
                proceedWithFinalApproval(sagaState);
            } else {
                proceedWithConditionalApproval(sagaState);
            }

        } catch (Exception e) {
            log.error("Error in interviews completed saga", e);
            if (fallbackEnabled) {
                handleSagaFailure(null, SagaStep.INTERVIEWS_COMPLETED, e);
            }
        }
    }

    // ================================
    // SAGA ORCHESTRATION METHODS
    // ================================

    /**
     * Procede con el proceso de aprobación después de evaluaciones exitosas
     */
    private void proceedWithApprovalProcess(SagaState sagaState) {
        log.info("Proceeding with approval process for application {}", 
                sagaState.getApplicationId());

        try {
            // 1. Actualizar estado de aplicación a "EN_EVALUACION_FINAL"
            updateApplicationStatus(sagaState.getApplicationId(), "INTERVIEW_SCHEDULED");

            // 2. Verificar si necesita entrevistas adicionales
            boolean needsAdditionalInterviews = checkIfNeedsAdditionalInterviews(sagaState.getApplicationId());

            if (needsAdditionalInterviews) {
                // 3. Programar entrevistas finales
                scheduleRequiredInterviews(sagaState.getApplicationId());
                sagaState.setCurrentStep(SagaStep.SCHEDULING_INTERVIEWS);
            } else {
                // 4. Proceder directamente con aprobación final
                proceedWithFinalApproval(sagaState);
            }

            log.info("Approval process initiated for application {}", sagaState.getApplicationId());

        } catch (Exception e) {
            log.error("Error in approval process for application {}", 
                     sagaState.getApplicationId(), e);
            
            if (fallbackEnabled) {
                handleSagaFailure(sagaState, SagaStep.APPROVAL_PROCESS, e);
            }
        }
    }

    /**
     * Procede con el proceso de rechazo
     */
    private void proceedWithRejectionProcess(SagaState sagaState) {
        log.info("Proceeding with rejection process for application {}", 
                sagaState.getApplicationId());

        try {
            // 1. Actualizar estado a rechazado
            updateApplicationStatus(sagaState.getApplicationId(), "REJECTED");

            // 2. Cancelar entrevistas pendientes
            cancelPendingInterviews(sagaState.getApplicationId());

            // 3. Enviar notificación de rechazo
            sendRejectionNotification(sagaState.getApplicationId());

            sagaState.setCurrentStep(SagaStep.COMPLETED);
            sagaState.setCompletedAt(Instant.now());

            log.info("Rejection process completed for application {}", sagaState.getApplicationId());

        } catch (Exception e) {
            log.error("Error in rejection process for application {}", 
                     sagaState.getApplicationId(), e);
            
            if (fallbackEnabled) {
                handleSagaFailure(sagaState, SagaStep.REJECTION_PROCESS, e);
            }
        }
    }

    /**
     * Procede con la aprobación final
     */
    private void proceedWithFinalApproval(SagaState sagaState) {
        log.info("Proceeding with final approval for application {}", 
                sagaState.getApplicationId());

        try {
            // 1. Verificar que todos los requisitos estén cumplidos
            boolean allRequirementsMet = verifyAllRequirements(sagaState.getApplicationId());

            if (allRequirementsMet) {
                // 2. Actualizar estado a aprobado
                updateApplicationStatus(sagaState.getApplicationId(), "APPROVED");

                // 3. Reservar cupo si está disponible
                boolean cupoReserved = reserveStudentSlot(sagaState.getApplicationId());

                if (cupoReserved) {
                    // 4. Enviar notificación de aprobación
                    sendApprovalNotification(sagaState.getApplicationId());
                    sagaState.setCurrentStep(SagaStep.COMPLETED);
                } else {
                    // 5. Poner en lista de espera
                    updateApplicationStatus(sagaState.getApplicationId(), "WAITLIST");
                    sendWaitlistNotification(sagaState.getApplicationId());
                    sagaState.setCurrentStep(SagaStep.WAITLISTED);
                }
            } else {
                // Falta algún requisito - aprobación condicional
                proceedWithConditionalApproval(sagaState);
            }

            sagaState.setCompletedAt(Instant.now());
            log.info("Final approval process completed for application {}", 
                    sagaState.getApplicationId());

        } catch (Exception e) {
            log.error("Error in final approval for application {}", 
                     sagaState.getApplicationId(), e);
            
            if (fallbackEnabled) {
                handleSagaFailure(sagaState, SagaStep.FINAL_APPROVAL, e);
            }
        }
    }

    /**
     * Procede con aprobación condicional
     */
    private void proceedWithConditionalApproval(SagaState sagaState) {
        log.info("Proceeding with conditional approval for application {}", 
                sagaState.getApplicationId());

        try {
            // 1. Identificar requisitos faltantes
            List<String> missingRequirements = identifyMissingRequirements(sagaState.getApplicationId());

            // 2. Actualizar estado
            updateApplicationStatus(sagaState.getApplicationId(), "CONDITIONALLY_APPROVED");

            // 3. Programar seguimientos adicionales si es necesario
            scheduleFollowUpInterviews(sagaState.getApplicationId(), missingRequirements);

            // 4. Notificar aprobación condicional
            sendConditionalApprovalNotification(sagaState.getApplicationId(), missingRequirements);

            sagaState.setCurrentStep(SagaStep.CONDITIONAL_APPROVAL);
            sagaState.setCompletedAt(Instant.now());

            log.info("Conditional approval completed for application {}", sagaState.getApplicationId());

        } catch (Exception e) {
            log.error("Error in conditional approval for application {}", 
                     sagaState.getApplicationId(), e);
            
            if (fallbackEnabled) {
                handleSagaFailure(sagaState, SagaStep.CONDITIONAL_APPROVAL, e);
            }
        }
    }

    // ================================
    // HELPER METHODS
    // ================================

    /**
     * Actualiza el estado de la aplicación en el servicio principal
     */
    private void updateApplicationStatus(UUID applicationId, String newStatus) {
        try {
            String url = applicationServiceUrl + "/api/applications/" + applicationId + "/status";
            
            Map<String, Object> statusUpdate = Map.of(
                "status", newStatus,
                "updated_by", "EVALUATION_SAGA",
                "updated_at", Instant.now()
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(statusUpdate, headers);
            
            restTemplate.exchange(url, HttpMethod.PUT, request, Void.class);
            
            log.debug("Updated application {} status to {}", applicationId, newStatus);

        } catch (Exception e) {
            log.error("Failed to update application status for {}: {}", applicationId, e.getMessage());
            throw new RuntimeException("Status update failed", e);
        }
    }

    /**
     * Evalúa los resultados de las entrevistas
     */
    private boolean evaluateInterviewResults(List<Interview> interviews) {
        if (interviews.isEmpty()) {
            return false;
        }

        // Criterios de evaluación:
        // - Al menos una entrevista debe ser HIGHLY_RECOMMENDED o RECOMMENDED
        // - No debe haber ninguna NOT_RECOMMENDED
        // - Rating promedio debe ser >= 6

        boolean hasPositiveRecommendation = interviews.stream()
            .anyMatch(i -> i.getRecommendation() == Interview.Recommendation.HIGHLY_RECOMMENDED || 
                          i.getRecommendation() == Interview.Recommendation.RECOMMENDED);

        boolean hasNoNegativeRecommendation = interviews.stream()
            .noneMatch(i -> i.getRecommendation() == Interview.Recommendation.NOT_RECOMMENDED);

        double averageRating = interviews.stream()
            .filter(i -> i.getOverallRating() != null)
            .mapToInt(Interview::getOverallRating)
            .average()
            .orElse(0.0);

        boolean ratingAcceptable = averageRating >= 6.0;

        log.debug("Interview evaluation: positive={}, noNegative={}, avgRating={}", 
                 hasPositiveRecommendation, hasNoNegativeRecommendation, averageRating);

        return hasPositiveRecommendation && hasNoNegativeRecommendation && ratingAcceptable;
    }

    /**
     * Verifica si necesita entrevistas adicionales
     */
    private boolean checkIfNeedsAdditionalInterviews(UUID applicationId) {
        List<Interview> existingInterviews = interviewService.getInterviewsByApplication(applicationId);
        
        // Verificar si tiene entrevista de dirección
        boolean hasDirectorInterview = existingInterviews.stream()
            .anyMatch(i -> i.getType() == Interview.InterviewType.DIRECTOR_INTERVIEW && 
                          i.isCompleted());

        // Si no tiene entrevista de dirección completada, la necesita
        return !hasDirectorInterview;
    }

    /**
     * Programa entrevistas requeridas
     */
    private void scheduleRequiredInterviews(UUID applicationId) {
        log.info("Scheduling required interviews for application {}", applicationId);
        
        // Programar entrevista de dirección si no existe
        if (!interviewService.hasActiveInterview(applicationId, Interview.InterviewType.DIRECTOR_INTERVIEW)) {
            Instant scheduledAt = Instant.now().plus(java.time.Duration.ofDays(3));
            
            interviewService.scheduleInterview(
                applicationId,
                "director-1", // En producción esto sería dinámico
                Interview.InterviewType.DIRECTOR_INTERVIEW,
                scheduledAt,
                45,
                "Oficina de Dirección",
                "SAGA_SYSTEM"
            );
        }
    }

    private void cancelPendingInterviews(UUID applicationId) {
        List<Interview> pendingInterviews = interviewService.getInterviewsByApplication(applicationId)
            .stream()
            .filter(i -> !i.getStatus().isTerminal())
            .toList();

        for (Interview interview : pendingInterviews) {
            interviewService.cancelInterview(
                interview.getId(), 
                "Application rejected - cancelling pending interviews", 
                "SAGA_SYSTEM"
            );
        }
    }

    private boolean verifyAllRequirements(UUID applicationId) {
        // Verificar que todas las evaluaciones estén completadas
        boolean evaluationsCompleted = evaluationService.areAllEvaluationsCompleted(applicationId);
        
        // Verificar que las entrevistas requeridas estén completadas
        List<Interview> interviews = interviewService.getInterviewsByApplication(applicationId);
        boolean requiredInterviewsCompleted = interviews.stream()
            .anyMatch(i -> i.getType() == Interview.InterviewType.DIRECTOR_INTERVIEW && 
                          i.isCompleted());

        return evaluationsCompleted && requiredInterviewsCompleted;
    }

    private boolean reserveStudentSlot(UUID applicationId) {
        // En una implementación real, esto consultaría el servicio de cupos
        // Por ahora retornamos true (cupo disponible)
        return true;
    }

    private List<String> identifyMissingRequirements(UUID applicationId) {
        List<String> missing = new ArrayList<>();
        
        if (!evaluationService.areAllEvaluationsCompleted(applicationId)) {
            missing.add("Evaluaciones académicas pendientes");
        }
        
        List<Interview> interviews = interviewService.getInterviewsByApplication(applicationId);
        boolean hasDirectorInterview = interviews.stream()
            .anyMatch(i -> i.getType() == Interview.InterviewType.DIRECTOR_INTERVIEW && 
                          i.isCompleted());
        
        if (!hasDirectorInterview) {
            missing.add("Entrevista de dirección pendiente");
        }
        
        return missing;
    }

    private void scheduleFollowUpInterviews(UUID applicationId, List<String> reasons) {
        log.info("Scheduling follow-up interviews for application {} due to: {}", 
                applicationId, reasons);
        
        Instant followUpTime = Instant.now().plus(java.time.Duration.ofDays(7));
        
        interviewService.scheduleInterview(
            applicationId,
            "coordinator-1",
            Interview.InterviewType.FOLLOW_UP_INTERVIEW,
            followUpTime,
            30,
            "Sala de Coordinación",
            "SAGA_SYSTEM"
        );
    }

    // Notification methods (simplified - in real implementation would use notification service)
    private void sendApprovalNotification(UUID applicationId) {
        log.info("Sending approval notification for application {}", applicationId);
    }

    private void sendRejectionNotification(UUID applicationId) {
        log.info("Sending rejection notification for application {}", applicationId);
    }

    private void sendWaitlistNotification(UUID applicationId) {
        log.info("Sending waitlist notification for application {}", applicationId);
    }

    private void sendConditionalApprovalNotification(UUID applicationId, List<String> requirements) {
        log.info("Sending conditional approval notification for application {}: {}", 
                applicationId, requirements);
    }

    /**
     * Maneja fallos en la saga con rollback y compensación
     */
    private void handleSagaFailure(SagaState sagaState, SagaStep failedStep, Exception error) {
        log.error("Saga failure at step {} for application {}: {}", 
                 failedStep, 
                 sagaState != null ? sagaState.getApplicationId() : "unknown", 
                 error.getMessage());

        if (sagaState != null) {
            // Implementar acciones de compensación según el paso que falló
            switch (failedStep) {
                case APPROVAL_PROCESS -> {
                    // Revertir cambios de estado si es posible
                    try {
                        updateApplicationStatus(sagaState.getApplicationId(), "UNDER_REVIEW");
                    } catch (Exception e) {
                        log.error("Failed to rollback application status", e);
                    }
                }
                case SCHEDULING_INTERVIEWS -> {
                    // Cancelar entrevistas que se hayan programado
                    cancelPendingInterviews(sagaState.getApplicationId());
                }
                // Agregar más casos según sea necesario
            }
        }

        // Notificar fallo para intervención manual
        log.error("Manual intervention required for failed saga");
    }

    // ================================
    // DATA TRANSFER OBJECTS
    // ================================

    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    private static class SagaState {
        private UUID sagaId;
        private UUID applicationId;
        private SagaStep currentStep;
        private Boolean overallPassed;
        private Boolean interviewsPassed;
        private Double averageScore;
        private Instant startedAt;
        private Instant completedAt;
        private String failureReason;
        private Map<String, Object> metadata;
    }

    private enum SagaStep {
        EVALUATIONS_COMPLETED,
        APPROVAL_PROCESS,
        REJECTION_PROCESS,
        SCHEDULING_INTERVIEWS,
        INTERVIEWS_COMPLETED,
        FINAL_APPROVAL,
        CONDITIONAL_APPROVAL,
        WAITLISTED,
        COMPLETED,
        FAILED
    }
}