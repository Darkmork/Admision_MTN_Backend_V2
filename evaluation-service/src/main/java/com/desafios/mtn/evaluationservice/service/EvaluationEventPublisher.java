package com.desafios.mtn.evaluationservice.service;

import com.desafios.mtn.evaluationservice.domain.Evaluation;
import com.desafios.mtn.evaluationservice.domain.Interview;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Servicio para publicar eventos de evaluación usando el patrón Outbox
 * Garantiza la publicación confiable de eventos de dominio
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class EvaluationEventPublisher {

    private final JdbcTemplate jdbcTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.events.enabled:true}")
    private boolean eventsEnabled;

    @Value("${app.events.use-outbox:true}")
    private boolean useOutbox;

    // ================================
    // EVALUATION EVENTS
    // ================================

    /**
     * Publica evento de evaluación creada
     */
    public void publishEvaluationCreated(Evaluation evaluation) {
        if (!eventsEnabled) return;

        Map<String, Object> payload = createEvaluationEventPayload(evaluation);
        payload.put("event_type", "evaluation_created");

        publishEvent(
            "Evaluation",
            evaluation.getId(),
            "EvaluationCreated.v1",
            payload,
            "evaluation.created",
            generateIdempotencyKey("created", evaluation.getId()),
            null,
            1 // High priority
        );
    }

    /**
     * Publica evento de evaluación asignada
     */
    public void publishEvaluationAssigned(Evaluation evaluation) {
        if (!eventsEnabled) return;

        Map<String, Object> payload = createEvaluationEventPayload(evaluation);
        payload.put("event_type", "evaluation_assigned");
        payload.put("assigned_at", evaluation.getAssignedAt());
        payload.put("expected_completion_at", evaluation.getExpectedCompletionAt());

        publishEvent(
            "Evaluation",
            evaluation.getId(),
            "EvaluationAssigned.v1",
            payload,
            "evaluation.assigned",
            generateIdempotencyKey("assigned", evaluation.getId()),
            null,
            1 // High priority
        );

        // También publicar a outbox usando función SQL específica
        publishEvaluationAssignedToOutbox(evaluation);
    }

    /**
     * Publica evento de evaluación reasignada
     */
    public void publishEvaluationReassigned(Evaluation evaluation) {
        if (!eventsEnabled) return;

        Map<String, Object> payload = createEvaluationEventPayload(evaluation);
        payload.put("event_type", "evaluation_reassigned");
        payload.put("previous_evaluator_id", evaluation.getPreviousEvaluatorId());
        payload.put("assignment_reason", evaluation.getAssignmentReason());

        publishEvent(
            "Evaluation",
            evaluation.getId(),
            "EvaluationReassigned.v1",
            payload,
            "evaluation.reassigned",
            generateIdempotencyKey("reassigned", evaluation.getId()),
            null,
            1 // High priority
        );
    }

    /**
     * Publica evento de evaluación iniciada
     */
    public void publishEvaluationStarted(Evaluation evaluation) {
        if (!eventsEnabled) return;

        Map<String, Object> payload = createEvaluationEventPayload(evaluation);
        payload.put("event_type", "evaluation_started");
        payload.put("started_at", evaluation.getStartedAt());

        publishEvent(
            "Evaluation",
            evaluation.getId(),
            "EvaluationStarted.v1",
            payload,
            "evaluation.started",
            generateIdempotencyKey("started", evaluation.getId()),
            null,
            1 // High priority
        );
    }

    /**
     * Publica evento de evaluación completada
     */
    public void publishEvaluationCompleted(Evaluation evaluation) {
        if (!eventsEnabled) return;

        Map<String, Object> payload = createEvaluationEventPayload(evaluation);
        payload.put("event_type", "evaluation_completed");
        payload.put("completed_at", evaluation.getCompletedAt());
        payload.put("total_score", evaluation.getTotalScore());
        payload.put("max_score", evaluation.getMaxScore());
        payload.put("percentage", evaluation.getPercentage());
        payload.put("passed", evaluation.getPassed());
        payload.put("processing_time_minutes", evaluation.getProcessingTimeMinutes());
        payload.put("sla_exceeded", evaluation.getSlaExceeded());

        publishEvent(
            "Evaluation",
            evaluation.getId(),
            "EvaluationCompleted.v1",
            payload,
            "evaluation.completed",
            generateIdempotencyKey("completed", evaluation.getId()),
            null,
            2 // Critical priority
        );

        // También publicar a outbox usando función SQL específica
        publishEvaluationCompletedToOutbox(evaluation);
    }

    /**
     * Publica evento de evaluación cancelada
     */
    public void publishEvaluationCancelled(Evaluation evaluation) {
        if (!eventsEnabled) return;

        Map<String, Object> payload = createEvaluationEventPayload(evaluation);
        payload.put("event_type", "evaluation_cancelled");
        payload.put("cancelled_at", evaluation.getCancelledAt());

        publishEvent(
            "Evaluation",
            evaluation.getId(),
            "EvaluationCancelled.v1",
            payload,
            "evaluation.cancelled",
            generateIdempotencyKey("cancelled", evaluation.getId()),
            null,
            1 // High priority
        );
    }

    /**
     * Publica evento de SLA excedido
     */
    public void publishEvaluationSlaExceeded(Evaluation evaluation) {
        if (!eventsEnabled) return;

        Map<String, Object> payload = createEvaluationEventPayload(evaluation);
        payload.put("event_type", "evaluation_sla_exceeded");
        payload.put("days_overdue", evaluation.getDaysSinceAssignment());

        publishEvent(
            "Evaluation",
            evaluation.getId(),
            "EvaluationSlaExceeded.v1",
            payload,
            "evaluation.sla_exceeded",
            generateIdempotencyKey("sla_exceeded", evaluation.getId()),
            null,
            2 // Critical priority
        );
    }

    /**
     * Publica evento de todas las evaluaciones de una aplicación completadas
     */
    public void publishApplicationEvaluationsCompleted(EvaluationService.ApplicationEvaluationResult result) {
        if (!eventsEnabled) return;

        Map<String, Object> payload = new HashMap<>();
        payload.put("event_type", "application_evaluations_completed");
        payload.put("application_id", result.getApplicationId());
        payload.put("overall_passed", result.isOverallPassed());
        payload.put("average_score", result.getAverageScore());
        payload.put("completed_count", result.getCompletedCount());
        payload.put("evaluation_ids", result.getEvaluations().stream()
            .map(Evaluation::getId).toArray());
        payload.put("timestamp", Instant.now());

        publishEvent(
            "Application",
            result.getApplicationId(),
            "ApplicationEvaluationsCompleted.v1",
            payload,
            "application.evaluations_completed",
            generateIdempotencyKey("app_evaluations_completed", result.getApplicationId()),
            null,
            2 // Critical priority
        );

        // También publicar a outbox usando función SQL específica
        publishApplicationEvaluationsCompletedToOutbox(result);
    }

    // ================================
    // INTERVIEW EVENTS
    // ================================

    /**
     * Publica evento de entrevista programada
     */
    public void publishInterviewScheduled(Interview interview) {
        if (!eventsEnabled) return;

        Map<String, Object> payload = createInterviewEventPayload(interview);
        payload.put("event_type", "interview_scheduled");
        payload.put("scheduled_at", interview.getScheduledAt());
        payload.put("duration_minutes", interview.getDurationMinutes());
        payload.put("location", interview.getLocation());

        publishEvent(
            "Interview",
            interview.getId(),
            "InterviewScheduled.v1",
            payload,
            "interview.scheduled",
            generateIdempotencyKey("scheduled", interview.getId()),
            null,
            1 // High priority
        );

        // También publicar a outbox usando función SQL específica
        publishInterviewScheduledToOutbox(interview);
    }

    /**
     * Publica evento de entrevista completada
     */
    public void publishInterviewCompleted(Interview interview) {
        if (!eventsEnabled) return;

        Map<String, Object> payload = createInterviewEventPayload(interview);
        payload.put("event_type", "interview_completed");
        payload.put("completed_at", interview.getCompletedAt());
        payload.put("overall_rating", interview.getOverallRating());
        payload.put("recommendation", interview.getRecommendation());

        publishEvent(
            "Interview",
            interview.getId(),
            "InterviewCompleted.v1",
            payload,
            "interview.completed",
            generateIdempotencyKey("completed", interview.getId()),
            null,
            2 // Critical priority
        );
    }

    // ================================
    // CORE EVENT PUBLISHING
    // ================================

    /**
     * Publica un evento usando Outbox pattern o directamente a RabbitMQ
     */
    private void publishEvent(String aggregateType, UUID aggregateId, String eventType,
                             Map<String, Object> payload, String routingKey,
                             String idempotencyKey, UUID correlationId, int priority) {
        
        try {
            if (useOutbox) {
                publishToOutbox(aggregateType, aggregateId, eventType, payload, 
                              routingKey, idempotencyKey, correlationId, priority);
            } else {
                publishDirectly(eventType, payload, routingKey);
            }

            log.debug("Published event: {} for aggregate: {}", eventType, aggregateId);

        } catch (Exception e) {
            log.error("Failed to publish event: {} for aggregate: {}", 
                     eventType, aggregateId, e);
            throw new RuntimeException("Event publication failed", e);
        }
    }

    /**
     * Publica evento al outbox para procesamiento confiable
     */
    private void publishToOutbox(String aggregateType, UUID aggregateId, String eventType,
                                Map<String, Object> payload, String routingKey,
                                String idempotencyKey, UUID correlationId, int priority) {
        
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);
            
            jdbcTemplate.update(
                "SELECT publish_event(?, ?, ?, ?::jsonb, ?, ?, ?, ?, ?, ?::jsonb)",
                aggregateType,
                aggregateId,
                eventType,
                payloadJson,
                routingKey,
                idempotencyKey,
                correlationId,
                null, // causation_id
                priority,
                null  // headers
            );

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize event payload", e);
        }
    }

    /**
     * Publica evento directamente a RabbitMQ
     */
    private void publishDirectly(String eventType, Map<String, Object> payload, String routingKey) {
        rabbitTemplate.convertAndSend("evaluations.events", routingKey, payload);
    }

    // ================================
    // SQL OUTBOX FUNCTIONS
    // ================================

    /**
     * Usa la función SQL específica para publicar evento de evaluación asignada
     */
    private void publishEvaluationAssignedToOutbox(Evaluation evaluation) {
        try {
            jdbcTemplate.update(
                "SELECT publish_evaluation_assigned_event(?, ?, ?, ?, ?, ?, ?)",
                evaluation.getId(),
                evaluation.getApplicationId(),
                evaluation.getEvaluatorId(),
                evaluation.getSubject().name(),
                evaluation.getLevel() != null ? evaluation.getLevel().name() : null,
                evaluation.getAssignedAt(),
                null // correlation_id
            );
        } catch (Exception e) {
            log.warn("Failed to publish to outbox using SQL function: {}", e.getMessage());
        }
    }

    /**
     * Usa la función SQL específica para publicar evento de evaluación completada
     */
    private void publishEvaluationCompletedToOutbox(Evaluation evaluation) {
        try {
            jdbcTemplate.update(
                "SELECT publish_evaluation_completed_event(?, ?, ?, ?, ?, ?, ?, ?, ?)",
                evaluation.getId(),
                evaluation.getApplicationId(),
                evaluation.getEvaluatorId(),
                evaluation.getSubject().name(),
                evaluation.getTotalScore(),
                evaluation.getPercentage(),
                evaluation.getPassed(),
                evaluation.getCompletedAt(),
                null // correlation_id
            );
        } catch (Exception e) {
            log.warn("Failed to publish to outbox using SQL function: {}", e.getMessage());
        }
    }

    /**
     * Usa la función SQL específica para publicar evento de entrevista programada
     */
    private void publishInterviewScheduledToOutbox(Interview interview) {
        try {
            jdbcTemplate.update(
                "SELECT publish_interview_scheduled_event(?, ?, ?, ?, ?, ?, ?)",
                interview.getId(),
                interview.getApplicationId(),
                interview.getInterviewerId(),
                interview.getScheduledAt(),
                interview.getDurationMinutes(),
                interview.getLocation(),
                null // correlation_id
            );
        } catch (Exception e) {
            log.warn("Failed to publish to outbox using SQL function: {}", e.getMessage());
        }
    }

    /**
     * Usa la función SQL específica para publicar evento de evaluaciones de aplicación completadas
     */
    private void publishApplicationEvaluationsCompletedToOutbox(EvaluationService.ApplicationEvaluationResult result) {
        try {
            UUID[] evaluationIds = result.getEvaluations().stream()
                .map(Evaluation::getId)
                .toArray(UUID[]::new);

            jdbcTemplate.update(
                "SELECT publish_evaluations_completed_event(?, ?, ?, ?)",
                result.getApplicationId(),
                evaluationIds,
                result.isOverallPassed(),
                null // correlation_id
            );
        } catch (Exception e) {
            log.warn("Failed to publish to outbox using SQL function: {}", e.getMessage());
        }
    }

    // ================================
    // HELPER METHODS
    // ================================

    /**
     * Crea payload base para eventos de evaluación
     */
    private Map<String, Object> createEvaluationEventPayload(Evaluation evaluation) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("evaluation_id", evaluation.getId());
        payload.put("application_id", evaluation.getApplicationId());
        payload.put("evaluator_id", evaluation.getEvaluatorId());
        payload.put("subject", evaluation.getSubject());
        payload.put("level", evaluation.getLevel());
        payload.put("status", evaluation.getStatus());
        payload.put("priority", evaluation.getPriority());
        payload.put("timestamp", Instant.now());
        return payload;
    }

    /**
     * Crea payload base para eventos de entrevista
     */
    private Map<String, Object> createInterviewEventPayload(Interview interview) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("interview_id", interview.getId());
        payload.put("application_id", interview.getApplicationId());
        payload.put("interviewer_id", interview.getInterviewerId());
        payload.put("type", interview.getType());
        payload.put("status", interview.getStatus());
        payload.put("timestamp", Instant.now());
        return payload;
    }

    /**
     * Genera clave de idempotencia para el evento
     */
    private String generateIdempotencyKey(String eventType, UUID entityId) {
        return String.format("%s-%s", eventType, entityId.toString());
    }

    // ================================
    // CONFIGURATION METHODS
    // ================================

    /**
     * Habilita/deshabilita la publicación de eventos
     */
    public void setEventsEnabled(boolean enabled) {
        this.eventsEnabled = enabled;
        log.info("Event publishing {}", enabled ? "enabled" : "disabled");
    }

    /**
     * Configura el uso del patrón outbox
     */
    public void setUseOutbox(boolean useOutbox) {
        this.useOutbox = useOutbox;
        log.info("Outbox pattern {}", useOutbox ? "enabled" : "disabled");
    }

    /**
     * Obtiene estado de configuración de eventos
     */
    public Map<String, Object> getEventConfiguration() {
        Map<String, Object> config = new HashMap<>();
        config.put("events_enabled", eventsEnabled);
        config.put("use_outbox", useOutbox);
        config.put("rabbit_template_configured", rabbitTemplate != null);
        config.put("jdbc_template_configured", jdbcTemplate != null);
        return config;
    }
}