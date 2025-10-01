package com.desafios.mtn.evaluationservice.controller;

import com.desafios.mtn.evaluationservice.service.OutboxProcessor;
import com.desafios.mtn.evaluationservice.service.EvaluationService;
import com.desafios.mtn.evaluationservice.service.InterviewService;
import com.desafios.mtn.evaluationservice.service.EvaluationSlaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controlador para operaciones de gestión y administración del sistema
 */
@RestController
@RequestMapping("/api/management")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Management", description = "API para operaciones de gestión y administración")
public class ManagementController {

    private final OutboxProcessor outboxProcessor;
    private final EvaluationService evaluationService;
    private final InterviewService interviewService;
    private final EvaluationSlaService evaluationSlaService;

    // ================================
    // OUTBOX MANAGEMENT
    // ================================

    @Operation(summary = "Obtener estadísticas del outbox")
    @GetMapping("/outbox/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OutboxProcessor.OutboxStatistics> getOutboxStatistics() {
        OutboxProcessor.OutboxStatistics statistics = outboxProcessor.getOutboxStatistics();
        return ResponseEntity.ok(statistics);
    }

    @Operation(summary = "Forzar procesamiento de eventos pendientes del outbox")
    @PostMapping("/outbox/process")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OutboxProcessor.ProcessingResult> forceProcessOutboxEvents() {
        log.info("Admin triggered force processing of outbox events");
        
        OutboxProcessor.ProcessingResult result = outboxProcessor.forceProcessPendingEvents();
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Configurar procesador del outbox")
    @PostMapping("/outbox/configure")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> configureOutboxProcessor(
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) Integer batchSize) {
        
        Map<String, Object> changes = new HashMap<>();
        
        if (enabled != null) {
            outboxProcessor.setProcessorEnabled(enabled);
            changes.put("enabled", enabled);
        }
        
        if (batchSize != null) {
            outboxProcessor.setBatchSize(batchSize);
            changes.put("batch_size", batchSize);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Outbox processor configuration updated");
        response.put("changes", changes);
        response.put("timestamp", Instant.now());
        
        return ResponseEntity.ok(response);
    }

    // ================================
    // EVALUATION MANAGEMENT
    // ================================

    @Operation(summary = "Procesar evaluaciones vencidas")
    @PostMapping("/evaluations/process-overdue")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> processOverdueEvaluations() {
        log.info("Admin triggered processing of overdue evaluations");
        
        int processedCount = evaluationService.processOverdueEvaluations();
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Processed overdue evaluations");
        response.put("processed_count", processedCount);
        response.put("timestamp", Instant.now());
        
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Reasignar evaluaciones estancadas")
    @PostMapping("/evaluations/reassign-stale")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> reassignStaleEvaluations() {
        log.info("Admin triggered reassignment of stale evaluations");
        
        List<com.desafios.mtn.evaluationservice.domain.Evaluation> reassignedEvaluations = 
            evaluationService.reassignStaleEvaluations();
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Reassigned stale evaluations");
        response.put("reassigned_count", reassignedEvaluations.size());
        response.put("timestamp", Instant.now());
        
        return ResponseEntity.ok(response);
    }

    // ================================
    // INTERVIEW MANAGEMENT
    // ================================

    @Operation(summary = "Procesar recordatorios automáticos de entrevistas")
    @PostMapping("/interviews/process-reminders")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> processInterviewReminders() {
        log.info("Admin triggered processing of interview reminders");
        
        int remindersSent = interviewService.processAutomaticReminders();
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Processed interview reminders");
        response.put("reminders_sent", remindersSent);
        response.put("timestamp", Instant.now());
        
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Procesar entrevistas vencidas")
    @PostMapping("/interviews/process-overdue")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> processOverdueInterviews() {
        log.info("Admin triggered processing of overdue interviews");
        
        int processedCount = interviewService.processOverdueInterviews();
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Processed overdue interviews");
        response.put("processed_count", processedCount);
        response.put("timestamp", Instant.now());
        
        return ResponseEntity.ok(response);
    }

    // ================================
    // SYSTEM HEALTH AND STATUS
    // ================================

    @Operation(summary = "Obtener estado general del sistema")
    @GetMapping("/system/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    public ResponseEntity<Map<String, Object>> getSystemStatus() {
        Map<String, Object> status = new HashMap<>();
        
        // Estadísticas de evaluaciones
        Map<String, Object> evaluationStats = new HashMap<>();
        evaluationStats.put("pending", evaluationService.getEvaluationsByStatus(
            com.desafios.mtn.evaluationservice.domain.Evaluation.EvaluationStatus.PENDING).size());
        evaluationStats.put("assigned", evaluationService.getEvaluationsByStatus(
            com.desafios.mtn.evaluationservice.domain.Evaluation.EvaluationStatus.ASSIGNED).size());
        evaluationStats.put("in_progress", evaluationService.getEvaluationsByStatus(
            com.desafios.mtn.evaluationservice.domain.Evaluation.EvaluationStatus.IN_PROGRESS).size());
        evaluationStats.put("overdue", evaluationService.getOverdueEvaluations().size());
        evaluationStats.put("urgent", evaluationService.getUrgentEvaluations().size());
        
        // Estadísticas de entrevistas
        Map<String, Object> interviewStats = new HashMap<>();
        interviewStats.put("scheduled", interviewService.getInterviewsByStatus(
            com.desafios.mtn.evaluationservice.domain.Interview.InterviewStatus.SCHEDULED).size());
        interviewStats.put("confirmed", interviewService.getInterviewsByStatus(
            com.desafios.mtn.evaluationservice.domain.Interview.InterviewStatus.CONFIRMED).size());
        interviewStats.put("completed", interviewService.getInterviewsByStatus(
            com.desafios.mtn.evaluationservice.domain.Interview.InterviewStatus.COMPLETED).size());
        interviewStats.put("overdue", interviewService.getOverdueInterviews().size());
        interviewStats.put("need_reminder", interviewService.getInterviewsNeedingReminder().size());
        
        // Estadísticas del outbox
        OutboxProcessor.OutboxStatistics outboxStats = outboxProcessor.getOutboxStatistics();
        
        // Carga de trabajo de evaluadores
        Map<String, Integer> workload = evaluationService.getEvaluatorWorkload();
        
        status.put("evaluations", evaluationStats);
        status.put("interviews", interviewStats);
        status.put("outbox", outboxStats);
        status.put("evaluator_workload", workload);
        status.put("timestamp", Instant.now());
        
        return ResponseEntity.ok(status);
    }

    @Operation(summary = "Obtener configuración de SLAs")
    @GetMapping("/sla/configuration")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    public ResponseEntity<Map<String, Object>> getSlaConfiguration() {
        Map<String, Object> configuration = evaluationSlaService.getSlaConfiguration();
        return ResponseEntity.ok(configuration);
    }

    @Operation(summary = "Actualizar configuración de SLA por defecto")
    @PostMapping("/sla/configure")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> configureSla(
            @RequestParam Integer defaultHours) {
        
        log.info("Admin updating default SLA hours to {}", defaultHours);
        
        evaluationSlaService.updateDefaultSlaHours(defaultHours);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "SLA configuration updated");
        response.put("new_default_hours", defaultHours);
        response.put("timestamp", Instant.now());
        
        return ResponseEntity.ok(response);
    }

    // ================================
    // MAINTENANCE OPERATIONS
    // ================================

    @Operation(summary = "Ejecutar mantenimiento general del sistema")
    @PostMapping("/maintenance/run")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> runSystemMaintenance() {
        log.info("Admin triggered system maintenance");
        
        Map<String, Object> results = new HashMap<>();
        
        // Procesar evaluaciones vencidas
        int overdueEvaluations = evaluationService.processOverdueEvaluations();
        results.put("overdue_evaluations_processed", overdueEvaluations);
        
        // Reasignar evaluaciones estancadas
        List<com.desafios.mtn.evaluationservice.domain.Evaluation> reassignedEvaluations = 
            evaluationService.reassignStaleEvaluations();
        results.put("stale_evaluations_reassigned", reassignedEvaluations.size());
        
        // Procesar recordatorios de entrevistas
        int remindersSent = interviewService.processAutomaticReminders();
        results.put("interview_reminders_sent", remindersSent);
        
        // Procesar entrevistas vencidas
        int overdueInterviews = interviewService.processOverdueInterviews();
        results.put("overdue_interviews_processed", overdueInterviews);
        
        // Procesar eventos del outbox
        OutboxProcessor.ProcessingResult outboxResult = outboxProcessor.forceProcessPendingEvents();
        results.put("outbox_events_processed", outboxResult.getTotalProcessed());
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "System maintenance completed");
        response.put("results", results);
        response.put("timestamp", Instant.now());
        
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Obtener métricas del sistema para monitoreo")
    @GetMapping("/metrics")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    public ResponseEntity<Map<String, Object>> getSystemMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        // Métricas de tiempo
        Instant now = Instant.now();
        
        // Evaluaciones por estado
        Map<String, Long> evaluationsByStatus = new HashMap<>();
        for (com.desafios.mtn.evaluationservice.domain.Evaluation.EvaluationStatus status : 
             com.desafios.mtn.evaluationservice.domain.Evaluation.EvaluationStatus.values()) {
            evaluationsByStatus.put(status.name(), 
                (long) evaluationService.getEvaluationsByStatus(status).size());
        }
        
        // Entrevistas por estado
        Map<String, Long> interviewsByStatus = new HashMap<>();
        for (com.desafios.mtn.evaluationservice.domain.Interview.InterviewStatus status : 
             com.desafios.mtn.evaluationservice.domain.Interview.InterviewStatus.values()) {
            interviewsByStatus.put(status.name(), 
                (long) interviewService.getInterviewsByStatus(status).size());
        }
        
        metrics.put("evaluations_by_status", evaluationsByStatus);
        metrics.put("interviews_by_status", interviewsByStatus);
        metrics.put("evaluator_workload", evaluationService.getEvaluatorWorkload());
        metrics.put("outbox_statistics", outboxProcessor.getOutboxStatistics());
        metrics.put("generated_at", now);
        
        return ResponseEntity.ok(metrics);
    }

    // ================================
    // CONFIGURATION ENDPOINTS
    // ================================

    @Operation(summary = "Obtener configuración completa del sistema")
    @GetMapping("/configuration")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getSystemConfiguration() {
        Map<String, Object> configuration = new HashMap<>();
        
        configuration.put("sla", evaluationSlaService.getSlaConfiguration());
        configuration.put("timestamp", Instant.now());
        
        return ResponseEntity.ok(configuration);
    }

    @Operation(summary = "Verificar estado de salud de todos los componentes")
    @GetMapping("/health")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    public ResponseEntity<Map<String, Object>> getHealthCheck() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            // Verificar conexión a base de datos
            evaluationService.getEvaluationsByStatus(
                com.desafios.mtn.evaluationservice.domain.Evaluation.EvaluationStatus.PENDING);
            health.put("database", "UP");
        } catch (Exception e) {
            health.put("database", "DOWN");
            health.put("database_error", e.getMessage());
        }
        
        try {
            // Verificar outbox
            outboxProcessor.getOutboxStatistics();
            health.put("outbox", "UP");
        } catch (Exception e) {
            health.put("outbox", "DOWN");
            health.put("outbox_error", e.getMessage());
        }
        
        health.put("service", "UP");
        health.put("timestamp", Instant.now());
        
        return ResponseEntity.ok(health);
    }
}