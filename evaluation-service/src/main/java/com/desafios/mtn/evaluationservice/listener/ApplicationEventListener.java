package com.desafios.mtn.evaluationservice.listener;

import com.desafios.mtn.evaluationservice.service.EvaluationService;
import com.desafios.mtn.evaluationservice.service.InterviewService;
import com.desafios.mtn.evaluationservice.domain.Evaluation.Subject;
import com.desafios.mtn.evaluationservice.domain.Evaluation.Level;
import com.desafios.mtn.evaluationservice.domain.Interview.InterviewType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Listener para eventos de aplicaciones
 * Maneja la creación de evaluaciones y entrevistas cuando se reciben eventos de aplicaciones
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ApplicationEventListener {

    private final EvaluationService evaluationService;
    private final InterviewService interviewService;
    private final ObjectMapper objectMapper;

    /**
     * Maneja eventos de aplicación recibida
     * Crea evaluaciones requeridas basadas en el nivel educativo del estudiante
     */
    @RabbitListener(queues = "evaluations.application-received.queue")
    @Transactional
    public void handleApplicationReceived(@Payload String message, 
                                        @Header Map<String, Object> headers) {
        try {
            log.info("Received application received event");
            log.debug("Message: {}", message);

            JsonNode eventData = objectMapper.readTree(message);
            
            UUID applicationId = UUID.fromString(eventData.get("application_id").asText());
            String levelStr = eventData.get("educational_level").asText();
            int studentAge = eventData.get("student_age").asInt();

            Level level = Level.valueOf(levelStr);

            log.info("Processing application {} for level {} (student age: {})", 
                    applicationId, level, studentAge);

            // Crear evaluaciones requeridas según el nivel
            createRequiredEvaluations(applicationId, level);

            // Programar entrevista de dirección si es necesario
            scheduleDirectorInterviewIfNeeded(applicationId, level);

            log.info("Successfully processed application received event for {}", applicationId);

        } catch (Exception e) {
            log.error("Error processing application received event", e);
            throw new RuntimeException("Failed to process application event", e);
        }
    }

    /**
     * Maneja eventos de documentos completados
     * Activa evaluaciones cuando todos los documentos están listos
     */
    @RabbitListener(queues = "evaluations.documents-completed.queue")
    @Transactional
    public void handleDocumentsCompleted(@Payload String message, 
                                       @Header Map<String, Object> headers) {
        try {
            log.info("Received documents completed event");
            
            JsonNode eventData = objectMapper.readTree(message);
            UUID applicationId = UUID.fromString(eventData.get("application_id").asText());

            log.info("Documents completed for application {}, triggering evaluation assignments", 
                    applicationId);

            // Asignar evaluaciones pendientes automáticamente
            evaluationService.assignPendingEvaluationsAutomatically();

            log.info("Successfully processed documents completed event for {}", applicationId);

        } catch (Exception e) {
            log.error("Error processing documents completed event", e);
            throw new RuntimeException("Failed to process documents completed event", e);
        }
    }

    /**
     * Maneja eventos de aplicación aprobada por evaluaciones
     * Programa entrevistas finales
     */
    @RabbitListener(queues = "evaluations.application-approved.queue")
    @Transactional
    public void handleApplicationApproved(@Payload String message, 
                                        @Header Map<String, Object> headers) {
        try {
            log.info("Received application approved event");
            
            JsonNode eventData = objectMapper.readTree(message);
            UUID applicationId = UUID.fromString(eventData.get("application_id").asText());

            log.info("Application {} approved, scheduling final interviews", applicationId);

            // Programar entrevista familiar si no existe
            scheduleFamilyInterviewIfNeeded(applicationId);

            log.info("Successfully processed application approved event for {}", applicationId);

        } catch (Exception e) {
            log.error("Error processing application approved event", e);
            throw new RuntimeException("Failed to process application approved event", e);
        }
    }

    // ================================
    // HELPER METHODS
    // ================================

    /**
     * Crea evaluaciones requeridas según el nivel educativo
     */
    private void createRequiredEvaluations(UUID applicationId, Level level) {
        log.debug("Creating required evaluations for level: {}", level);

        switch (level) {
            case PRESCHOOL -> {
                // Preescolar: Evaluación general y psicológica
                evaluationService.createEvaluation(applicationId, Subject.GENERAL, level, 1, "SYSTEM");
                evaluationService.createEvaluation(applicationId, Subject.PSYCHOLOGY, level, 1, "SYSTEM");
                log.info("Created GENERAL and PSYCHOLOGY evaluations for PRESCHOOL level");
            }
            case BASIC -> {
                // Básica: Matemáticas, Lenguaje y Psicología
                evaluationService.createEvaluation(applicationId, Subject.MATHEMATICS, level, 1, "SYSTEM");
                evaluationService.createEvaluation(applicationId, Subject.LANGUAGE, level, 1, "SYSTEM");
                evaluationService.createEvaluation(applicationId, Subject.PSYCHOLOGY, level, 1, "SYSTEM");
                log.info("Created MATHEMATICS, LANGUAGE, and PSYCHOLOGY evaluations for BASIC level");
            }
            case HIGH_SCHOOL -> {
                // Media: Matemáticas, Lenguaje, Ciencias, Historia y Psicología
                evaluationService.createEvaluation(applicationId, Subject.MATHEMATICS, level, 1, "SYSTEM");
                evaluationService.createEvaluation(applicationId, Subject.LANGUAGE, level, 1, "SYSTEM");
                evaluationService.createEvaluation(applicationId, Subject.SCIENCE, level, 1, "SYSTEM");
                evaluationService.createEvaluation(applicationId, Subject.HISTORY, level, 1, "SYSTEM");
                evaluationService.createEvaluation(applicationId, Subject.PSYCHOLOGY, level, 1, "SYSTEM");
                log.info("Created comprehensive evaluations for HIGH_SCHOOL level");
            }
            case ALL -> {
                // Caso especial - crear evaluación general
                evaluationService.createEvaluation(applicationId, Subject.GENERAL, level, 1, "SYSTEM");
                evaluationService.createEvaluation(applicationId, Subject.PSYCHOLOGY, level, 1, "SYSTEM");
                log.info("Created GENERAL and PSYCHOLOGY evaluations for ALL level");
            }
        }
    }

    /**
     * Programa entrevista de dirección si es necesario
     */
    private void scheduleDirectorInterviewIfNeeded(UUID applicationId, Level level) {
        // Verificar si ya existe una entrevista de dirección
        if (!interviewService.hasActiveInterview(applicationId, InterviewType.DIRECTOR_INTERVIEW)) {
            
            // Para todos los niveles se requiere entrevista de dirección
            // Programar para dentro de 3-5 días laborales
            Instant scheduledAt = Instant.now().plus(java.time.Duration.ofDays(4));
            
            try {
                // Buscar director disponible (esto sería más sofisticado en producción)
                String directorId = findAvailableDirector(level);
                
                if (directorId != null) {
                    interviewService.scheduleInterview(
                        applicationId, 
                        directorId, 
                        InterviewType.DIRECTOR_INTERVIEW,
                        scheduledAt,
                        45, // 45 minutos
                        "Oficina de Dirección",
                        "SYSTEM"
                    );
                    
                    log.info("Scheduled director interview for application {}", applicationId);
                }
            } catch (Exception e) {
                log.warn("Could not schedule director interview for application {}: {}", 
                        applicationId, e.getMessage());
            }
        }
    }

    /**
     * Programa entrevista familiar cuando la aplicación está aprobada
     */
    private void scheduleFamilyInterviewIfNeeded(UUID applicationId) {
        if (!interviewService.hasActiveInterview(applicationId, InterviewType.FAMILY_INTERVIEW)) {
            
            // Programar entrevista familiar para dentro de 1-2 días
            Instant scheduledAt = Instant.now().plus(java.time.Duration.ofDays(2));
            
            try {
                String interviewerId = findAvailableInterviewer(InterviewType.FAMILY_INTERVIEW);
                
                if (interviewerId != null) {
                    interviewService.scheduleInterview(
                        applicationId,
                        interviewerId,
                        InterviewType.FAMILY_INTERVIEW,
                        scheduledAt,
                        60, // 1 hora
                        "Sala de Entrevistas",
                        "SYSTEM"
                    );
                    
                    log.info("Scheduled family interview for application {}", applicationId);
                }
            } catch (Exception e) {
                log.warn("Could not schedule family interview for application {}: {}", 
                        applicationId, e.getMessage());
            }
        }
    }

    /**
     * Busca director disponible según el nivel (mock implementation)
     */
    private String findAvailableDirector(Level level) {
        // En una implementación real, esto consultaría un servicio de usuarios
        // o base de datos para encontrar directores disponibles
        return switch (level) {
            case PRESCHOOL -> "director-preescolar-1";
            case BASIC -> "director-basica-1";
            case HIGH_SCHOOL -> "director-media-1";
            default -> "director-general-1";
        };
    }

    /**
     * Busca entrevistador disponible según el tipo de entrevista
     */
    private String findAvailableInterviewer(InterviewType type) {
        // Mock implementation
        return switch (type) {
            case DIRECTOR_INTERVIEW -> "director-1";
            case PSYCHOLOGICAL_INTERVIEW -> "psicologo-1";
            case FAMILY_INTERVIEW -> "orientador-1";
            case STUDENT_INTERVIEW -> "profesor-1";
            case FOLLOW_UP_INTERVIEW -> "coordinador-1";
        };
    }
}