package com.desafios.mtn.evaluationservice.controller;

import com.desafios.mtn.evaluationservice.domain.Evaluation;
import com.desafios.mtn.evaluationservice.domain.Evaluation.*;
import com.desafios.mtn.evaluationservice.service.EvaluationService;
import com.desafios.mtn.evaluationservice.service.EvaluationSlaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controlador REST para la gestión de evaluaciones académicas y psicológicas
 */
@RestController
@RequestMapping("/api/evaluations")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Evaluations", description = "API para gestión de evaluaciones académicas y psicológicas")
public class EvaluationController {

    private final EvaluationService evaluationService;
    private final EvaluationSlaService evaluationSlaService;

    // ================================
    // EVALUATION CRUD OPERATIONS
    // ================================

    @Operation(summary = "Crear nueva evaluación", description = "Crea una nueva evaluación para una aplicación")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Evaluación creada exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
        @ApiResponse(responseCode = "409", description = "Ya existe una evaluación activa para esta aplicación y materia")
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR', 'TEACHER')")
    public ResponseEntity<EvaluationResponse> createEvaluation(
            @Valid @RequestBody CreateEvaluationRequest request) {
        
        log.info("Creating evaluation for application {} - Subject: {}, Level: {}", 
                request.getApplicationId(), request.getSubject(), request.getLevel());

        Evaluation evaluation = evaluationService.createEvaluation(
            request.getApplicationId(),
            request.getSubject(),
            request.getLevel(),
            request.getPriority(),
            getCurrentUser()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(EvaluationResponse.fromEvaluation(evaluation));
    }

    @Operation(summary = "Obtener evaluación por ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR', 'TEACHER', 'PSYCHOLOGIST')")
    public ResponseEntity<EvaluationResponse> getEvaluation(
            @Parameter(description = "ID de la evaluación") @PathVariable UUID id) {
        
        Evaluation evaluation = evaluationService.getEvaluationById(id);
        return ResponseEntity.ok(EvaluationResponse.fromEvaluation(evaluation));
    }

    @Operation(summary = "Obtener evaluaciones por aplicación")
    @GetMapping("/application/{applicationId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR', 'TEACHER', 'PSYCHOLOGIST')")
    public ResponseEntity<List<EvaluationResponse>> getEvaluationsByApplication(
            @Parameter(description = "ID de la aplicación") @PathVariable UUID applicationId) {
        
        List<Evaluation> evaluations = evaluationService.getEvaluationsByApplication(applicationId);
        List<EvaluationResponse> responses = evaluations.stream()
            .map(EvaluationResponse::fromEvaluation)
            .toList();
        
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "Obtener evaluaciones asignadas al evaluador actual")
    @GetMapping("/my-evaluations")
    @PreAuthorize("hasAnyRole('TEACHER', 'PSYCHOLOGIST')")
    public ResponseEntity<List<EvaluationResponse>> getMyEvaluations() {
        
        String currentUser = getCurrentUser();
        List<Evaluation> evaluations = evaluationService.getEvaluationsByEvaluator(currentUser);
        List<EvaluationResponse> responses = evaluations.stream()
            .map(EvaluationResponse::fromEvaluation)
            .toList();
        
        return ResponseEntity.ok(responses);
    }

    // ================================
    // EVALUATION ASSIGNMENT
    // ================================

    @Operation(summary = "Asignar evaluación a evaluador")
    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    public ResponseEntity<EvaluationResponse> assignEvaluation(
            @PathVariable UUID id,
            @Valid @RequestBody AssignEvaluationRequest request) {
        
        log.info("Assigning evaluation {} to evaluator {}", id, request.getEvaluatorId());

        Evaluation evaluation = evaluationService.assignEvaluation(
            id,
            request.getEvaluatorId(),
            request.getReason(),
            getCurrentUser()
        );

        return ResponseEntity.ok(EvaluationResponse.fromEvaluation(evaluation));
    }

    @Operation(summary = "Reasignar evaluación a otro evaluador")
    @PostMapping("/{id}/reassign")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    public ResponseEntity<EvaluationResponse> reassignEvaluation(
            @PathVariable UUID id,
            @Valid @RequestBody ReassignEvaluationRequest request) {
        
        log.info("Reassigning evaluation {} to evaluator {}", id, request.getNewEvaluatorId());

        Evaluation evaluation = evaluationService.reassignEvaluation(
            id,
            request.getNewEvaluatorId(),
            request.getReason(),
            getCurrentUser()
        );

        return ResponseEntity.ok(EvaluationResponse.fromEvaluation(evaluation));
    }

    @Operation(summary = "Asignar evaluaciones pendientes automáticamente")
    @PostMapping("/auto-assign")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    public ResponseEntity<AutoAssignmentResponse> autoAssignEvaluations() {
        
        log.info("Starting automatic assignment of pending evaluations");

        List<Evaluation> assignedEvaluations = evaluationService.assignPendingEvaluationsAutomatically();
        
        return ResponseEntity.ok(AutoAssignmentResponse.builder()
            .assignedCount(assignedEvaluations.size())
            .assignedEvaluations(assignedEvaluations.stream()
                .map(EvaluationResponse::fromEvaluation)
                .toList())
            .timestamp(Instant.now())
            .build());
    }

    // ================================
    // EVALUATION EXECUTION
    // ================================

    @Operation(summary = "Iniciar evaluación")
    @PostMapping("/{id}/start")
    @PreAuthorize("hasAnyRole('TEACHER', 'PSYCHOLOGIST')")
    public ResponseEntity<EvaluationResponse> startEvaluation(@PathVariable UUID id) {
        
        log.info("Starting evaluation {}", id);

        Evaluation evaluation = evaluationService.startEvaluation(id, getCurrentUser());
        return ResponseEntity.ok(EvaluationResponse.fromEvaluation(evaluation));
    }

    @Operation(summary = "Completar evaluación con resultados")
    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('TEACHER', 'PSYCHOLOGIST')")
    public ResponseEntity<EvaluationResponse> completeEvaluation(
            @PathVariable UUID id,
            @Valid @RequestBody CompleteEvaluationRequest request) {
        
        log.info("Completing evaluation {} with score {}/{}", 
                id, request.getTotalScore(), request.getMaxScore());

        Evaluation evaluation = evaluationService.completeEvaluation(
            id,
            request.getTotalScore(),
            request.getMaxScore(),
            request.getNotes(),
            getCurrentUser()
        );

        return ResponseEntity.ok(EvaluationResponse.fromEvaluation(evaluation));
    }

    @Operation(summary = "Cancelar evaluación")
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR', 'TEACHER', 'PSYCHOLOGIST')")
    public ResponseEntity<EvaluationResponse> cancelEvaluation(
            @PathVariable UUID id,
            @Valid @RequestBody CancelEvaluationRequest request) {
        
        log.info("Cancelling evaluation {}: {}", id, request.getReason());

        Evaluation evaluation = evaluationService.cancelEvaluation(
            id,
            request.getReason(),
            getCurrentUser()
        );

        return ResponseEntity.ok(EvaluationResponse.fromEvaluation(evaluation));
    }

    // ================================
    // QUERIES AND MONITORING
    // ================================

    @Operation(summary = "Obtener evaluaciones por estado")
    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    public ResponseEntity<List<EvaluationResponse>> getEvaluationsByStatus(
            @PathVariable EvaluationStatus status) {
        
        List<Evaluation> evaluations = evaluationService.getEvaluationsByStatus(status);
        List<EvaluationResponse> responses = evaluations.stream()
            .map(EvaluationResponse::fromEvaluation)
            .toList();
        
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "Obtener evaluaciones vencidas")
    @GetMapping("/overdue")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    public ResponseEntity<List<EvaluationResponse>> getOverdueEvaluations() {
        
        List<Evaluation> evaluations = evaluationService.getOverdueEvaluations();
        List<EvaluationResponse> responses = evaluations.stream()
            .map(EvaluationResponse::fromEvaluation)
            .toList();
        
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "Obtener evaluaciones urgentes")
    @GetMapping("/urgent")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    public ResponseEntity<List<EvaluationResponse>> getUrgentEvaluations() {
        
        List<Evaluation> evaluations = evaluationService.getUrgentEvaluations();
        List<EvaluationResponse> responses = evaluations.stream()
            .map(EvaluationResponse::fromEvaluation)
            .toList();
        
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "Obtener carga de trabajo de evaluadores")
    @GetMapping("/workload")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    public ResponseEntity<Map<String, Integer>> getEvaluatorWorkload() {
        
        Map<String, Integer> workload = evaluationService.getEvaluatorWorkload();
        return ResponseEntity.ok(workload);
    }

    @Operation(summary = "Obtener resultado general de evaluaciones de una aplicación")
    @GetMapping("/application/{applicationId}/result")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR', 'TEACHER', 'PSYCHOLOGIST')")
    public ResponseEntity<EvaluationService.ApplicationEvaluationResult> getApplicationResult(
            @PathVariable UUID applicationId) {
        
        EvaluationService.ApplicationEvaluationResult result = 
            evaluationService.getApplicationEvaluationResult(applicationId);
        
        return ResponseEntity.ok(result);
    }

    // ================================
    // SLA MANAGEMENT
    // ================================

    @Operation(summary = "Analizar estado de SLA de una evaluación")
    @GetMapping("/{id}/sla-status")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR', 'TEACHER', 'PSYCHOLOGIST')")
    public ResponseEntity<EvaluationSlaService.SlaStatus> getSlaStatus(@PathVariable UUID id) {
        
        Evaluation evaluation = evaluationService.getEvaluationById(id);
        EvaluationSlaService.SlaStatus slaStatus = evaluationSlaService.analyzeSlaStatus(
            evaluation.getAssignedAt(),
            evaluation.getExpectedCompletionAt(),
            evaluation.getCompletedAt()
        );
        
        return ResponseEntity.ok(slaStatus);
    }

    @Operation(summary = "Obtener recomendaciones de SLA")
    @GetMapping("/{id}/sla-recommendations")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    public ResponseEntity<EvaluationSlaService.SlaRecommendations> getSlaRecommendations(
            @PathVariable UUID id) {
        
        Evaluation evaluation = evaluationService.getEvaluationById(id);
        EvaluationSlaService.SlaStatus slaStatus = evaluationSlaService.analyzeSlaStatus(
            evaluation.getAssignedAt(),
            evaluation.getExpectedCompletionAt(),
            evaluation.getCompletedAt()
        );
        
        EvaluationSlaService.SlaRecommendations recommendations = 
            evaluationSlaService.getSlaRecommendations(
                evaluation.getSubject(),
                evaluation.getLevel(),
                slaStatus.getStatus()
            );
        
        return ResponseEntity.ok(recommendations);
    }

    // ================================
    // STATISTICS AND REPORTING
    // ================================

    @Operation(summary = "Obtener estadísticas de evaluaciones")
    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    public ResponseEntity<EvaluationService.EvaluationStatistics> getEvaluationStatistics(
            @RequestParam(required = false, defaultValue = "30") int days) {
        
        Instant end = Instant.now();
        Instant start = end.minus(java.time.Duration.ofDays(days));
        
        EvaluationService.EvaluationStatistics statistics = 
            evaluationService.getEvaluationStatistics(start, end);
        
        return ResponseEntity.ok(statistics);
    }

    // ================================
    // HELPER METHODS
    // ================================

    private String getCurrentUser() {
        // En una implementación real, esto extraería el usuario del SecurityContext
        return "current-user";
    }

    // ================================
    // DTOs
    // ================================

    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class CreateEvaluationRequest {
        private UUID applicationId;
        private Subject subject;
        private Level level;
        private Integer priority;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class AssignEvaluationRequest {
        private String evaluatorId;
        private AssignmentReason reason;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class ReassignEvaluationRequest {
        private String newEvaluatorId;
        private String reason;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class CompleteEvaluationRequest {
        private BigDecimal totalScore;
        private BigDecimal maxScore;
        private String notes;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class CancelEvaluationRequest {
        private String reason;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class EvaluationResponse {
        private UUID id;
        private UUID applicationId;
        private String evaluatorId;
        private Subject subject;
        private Level level;
        private EvaluationStatus status;
        private Instant assignedAt;
        private Instant startedAt;
        private Instant completedAt;
        private Instant cancelledAt;
        private BigDecimal totalScore;
        private BigDecimal maxScore;
        private BigDecimal percentage;
        private Boolean passed;
        private AssignmentReason assignmentReason;
        private String previousEvaluatorId;
        private Integer priority;
        private Instant expectedCompletionAt;
        private Boolean slaExceeded;
        private Integer processingTimeMinutes;
        private String notes;
        private Instant createdAt;
        private Instant updatedAt;
        private String createdBy;
        private String updatedBy;

        public static EvaluationResponse fromEvaluation(Evaluation evaluation) {
            return EvaluationResponse.builder()
                .id(evaluation.getId())
                .applicationId(evaluation.getApplicationId())
                .evaluatorId(evaluation.getEvaluatorId())
                .subject(evaluation.getSubject())
                .level(evaluation.getLevel())
                .status(evaluation.getStatus())
                .assignedAt(evaluation.getAssignedAt())
                .startedAt(evaluation.getStartedAt())
                .completedAt(evaluation.getCompletedAt())
                .cancelledAt(evaluation.getCancelledAt())
                .totalScore(evaluation.getTotalScore())
                .maxScore(evaluation.getMaxScore())
                .percentage(evaluation.getPercentage())
                .passed(evaluation.getPassed())
                .assignmentReason(evaluation.getAssignmentReason())
                .previousEvaluatorId(evaluation.getPreviousEvaluatorId())
                .priority(evaluation.getPriority())
                .expectedCompletionAt(evaluation.getExpectedCompletionAt())
                .slaExceeded(evaluation.getSlaExceeded())
                .processingTimeMinutes(evaluation.getProcessingTimeMinutes())
                .notes(evaluation.getNotes())
                .createdAt(evaluation.getCreatedAt())
                .updatedAt(evaluation.getUpdatedAt())
                .createdBy(evaluation.getCreatedBy())
                .updatedBy(evaluation.getUpdatedBy())
                .build();
        }
    }

    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class AutoAssignmentResponse {
        private Integer assignedCount;
        private List<EvaluationResponse> assignedEvaluations;
        private Instant timestamp;
    }
}