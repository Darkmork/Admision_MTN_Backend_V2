package com.desafios.mtn.evaluationservice.service;

import com.desafios.mtn.evaluationservice.domain.Evaluation.Subject;
import com.desafios.mtn.evaluationservice.domain.Evaluation.Level;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

/**
 * Servicio para gestión de SLAs (Service Level Agreements) de evaluaciones
 * Define y calcula tiempos esperados de completación basados en tipo, nivel y prioridad
 */
@Service
@Slf4j
public class EvaluationSlaService {

    @Value("${evaluation.sla.default-hours:48}")
    private int defaultSlaHours;

    @Value("${evaluation.sla.high-priority-multiplier:0.5}")
    private double highPriorityMultiplier;

    @Value("${evaluation.sla.low-priority-multiplier:1.5}")
    private double lowPriorityMultiplier;

    // SLA base por materia (en horas)
    private final Map<Subject, Integer> subjectSlaHours = Map.of(
        Subject.MATHEMATICS, 72,        // 3 días
        Subject.LANGUAGE, 72,           // 3 días  
        Subject.ENGLISH, 48,            // 2 días
        Subject.SCIENCE, 48,            // 2 días
        Subject.HISTORY, 48,            // 2 días
        Subject.PSYCHOLOGY, 120,        // 5 días (más complejo)
        Subject.GENERAL, 24             // 1 día (más simple)
    );

    // Modificadores por nivel educativo
    private final Map<Level, Double> levelMultipliers = Map.of(
        Level.PRESCHOOL, 0.8,          // 20% menos tiempo
        Level.BASIC, 1.0,              // Tiempo base
        Level.HIGH_SCHOOL, 1.2,        // 20% más tiempo
        Level.ALL, 1.0                 // Tiempo base
    );

    // ================================
    // CORE SLA OPERATIONS
    // ================================

    /**
     * Calcula el tiempo esperado de completación para una evaluación
     */
    public Instant calculateExpectedCompletion(Subject subject, Level level, Integer priority) {
        return calculateExpectedCompletion(subject, level, priority, Instant.now());
    }

    /**
     * Calcula el tiempo esperado de completación desde una fecha específica
     */
    public Instant calculateExpectedCompletion(Subject subject, Level level, 
                                             Integer priority, Instant fromTime) {
        
        log.debug("Calculating SLA for subject: {}, level: {}, priority: {}", 
                 subject, level, priority);

        // Obtener SLA base para la materia
        int baseSlaHours = subjectSlaHours.getOrDefault(subject, defaultSlaHours);

        // Aplicar modificador por nivel
        double levelMultiplier = levelMultipliers.getOrDefault(level, 1.0);

        // Aplicar modificador por prioridad
        double priorityMultiplier = getPriorityMultiplier(priority);

        // Calcular horas finales
        long finalHours = Math.round(baseSlaHours * levelMultiplier * priorityMultiplier);

        // Ajustar por horario laboral (opcional)
        finalHours = adjustForBusinessHours(finalHours);

        Instant expectedCompletion = fromTime.plus(finalHours, ChronoUnit.HOURS);

        log.debug("Calculated SLA: {} hours from {} = {}", 
                 finalHours, fromTime, expectedCompletion);

        return expectedCompletion;
    }

    /**
     * Verifica si una evaluación está en riesgo de exceder el SLA
     */
    public boolean isAtRisk(Instant expectedCompletion, double riskThreshold) {
        if (expectedCompletion == null) {
            return false;
        }

        Instant now = Instant.now();
        long hoursUntilDeadline = ChronoUnit.HOURS.between(now, expectedCompletion);
        
        // Calcular SLA total para determinar el umbral
        long totalSlaHours = ChronoUnit.HOURS.between(
            expectedCompletion.minus(defaultSlaHours, ChronoUnit.HOURS), 
            expectedCompletion
        );

        long riskThresholdHours = Math.round(totalSlaHours * riskThreshold);

        return hoursUntilDeadline <= riskThresholdHours;
    }

    /**
     * Verifica si una evaluación está en riesgo (80% del tiempo transcurrido)
     */
    public boolean isAtRisk(Instant expectedCompletion) {
        return isAtRisk(expectedCompletion, 0.2); // 20% del tiempo restante
    }

    /**
     * Calcula el porcentaje de SLA transcurrido
     */
    public double calculateSlaProgress(Instant startTime, Instant expectedCompletion) {
        if (startTime == null || expectedCompletion == null) {
            return 0.0;
        }

        Instant now = Instant.now();
        
        if (now.isBefore(startTime)) {
            return 0.0;
        }

        if (now.isAfter(expectedCompletion)) {
            return 1.0; // 100% excedido
        }

        long totalSlaSeconds = ChronoUnit.SECONDS.between(startTime, expectedCompletion);
        long elapsedSeconds = ChronoUnit.SECONDS.between(startTime, now);

        return (double) elapsedSeconds / totalSlaSeconds;
    }

    // ================================
    // SLA ANALYSIS
    // ================================

    /**
     * Analiza el estado del SLA de una evaluación
     */
    public SlaStatus analyzeSlaStatus(Instant startTime, Instant expectedCompletion, 
                                    Instant completedAt) {
        
        if (startTime == null || expectedCompletion == null) {
            return SlaStatus.builder()
                .status(SlaStatusType.UNKNOWN)
                .message("Insufficient data for SLA analysis")
                .build();
        }

        Instant referenceTime = completedAt != null ? completedAt : Instant.now();
        
        if (referenceTime.isBefore(expectedCompletion)) {
            double progress = calculateSlaProgress(startTime, expectedCompletion);
            
            if (progress < 0.5) {
                return SlaStatus.builder()
                    .status(SlaStatusType.ON_TRACK)
                    .progress(progress)
                    .message("Evaluation is on track")
                    .build();
            } else if (progress < 0.8) {
                return SlaStatus.builder()
                    .status(SlaStatusType.AT_RISK)
                    .progress(progress)
                    .message("Evaluation is at risk of missing SLA")
                    .build();
            } else {
                return SlaStatus.builder()
                    .status(SlaStatusType.CRITICAL)
                    .progress(progress)
                    .message("Evaluation is in critical state - SLA about to be breached")
                    .build();
            }
        } else {
            long hoursOverdue = ChronoUnit.HOURS.between(expectedCompletion, referenceTime);
            return SlaStatus.builder()
                .status(SlaStatusType.BREACHED)
                .progress(1.0)
                .hoursOverdue(hoursOverdue)
                .message(String.format("SLA breached by %d hours", hoursOverdue))
                .build();
        }
    }

    /**
     * Obtiene recomendaciones para mejorar el cumplimiento del SLA
     */
    public SlaRecommendations getSlaRecommendations(Subject subject, Level level, 
                                                   SlaStatusType currentStatus) {
        
        SlaRecommendations.SlaRecommendationsBuilder builder = SlaRecommendations.builder();
        builder.subject(subject).level(level).currentStatus(currentStatus);

        switch (currentStatus) {
            case ON_TRACK -> {
                builder.addRecommendation("Evaluation is progressing well - maintain current pace");
                builder.priority("LOW");
            }
            case AT_RISK -> {
                builder.addRecommendation("Consider prioritizing this evaluation");
                builder.addRecommendation("Check if evaluator needs additional resources");
                builder.priority("MEDIUM");
            }
            case CRITICAL -> {
                builder.addRecommendation("URGENT: Evaluation needs immediate attention");
                builder.addRecommendation("Consider reassigning to available evaluator");
                builder.addRecommendation("Escalate to supervisor if necessary");
                builder.priority("HIGH");
            }
            case BREACHED -> {
                builder.addRecommendation("SLA already breached - complete as soon as possible");
                builder.addRecommendation("Document reasons for delay");
                builder.addRecommendation("Review process to prevent future breaches");
                builder.priority("CRITICAL");
            }
            default -> {
                builder.addRecommendation("Unable to provide specific recommendations");
                builder.priority("UNKNOWN");
            }
        }

        return builder.build();
    }

    // ================================
    // HELPER METHODS
    // ================================

    /**
     * Obtiene el multiplicador basado en la prioridad
     */
    private double getPriorityMultiplier(Integer priority) {
        if (priority == null) {
            return 1.0; // Prioridad normal
        }

        return switch (priority) {
            case 2 -> highPriorityMultiplier;      // Alta prioridad - menos tiempo
            case 1 -> 0.75;                       // Prioridad media-alta
            case 0 -> 1.0;                        // Prioridad normal
            case -1 -> lowPriorityMultiplier;     // Baja prioridad - más tiempo
            default -> 1.0;
        };
    }

    /**
     * Ajusta las horas considerando horario laboral
     */
    private long adjustForBusinessHours(long hours) {
        // Implementación simplificada: por cada día laboral (8 horas), 
        // agregar 16 horas adicionales para horario no laboral
        
        if (hours <= 8) {
            return hours; // Dentro del primer día laboral
        }

        long businessDays = hours / 8;
        long remainingHours = hours % 8;
        
        // Cada día laboral se extiende a 24 horas reales
        return (businessDays * 24) + remainingHours;
    }

    // ================================
    // CONFIGURATION
    // ================================

    /**
     * Obtiene la configuración actual de SLAs
     */
    public Map<String, Object> getSlaConfiguration() {
        Map<String, Object> config = new HashMap<>();
        config.put("default_sla_hours", defaultSlaHours);
        config.put("high_priority_multiplier", highPriorityMultiplier);
        config.put("low_priority_multiplier", lowPriorityMultiplier);
        config.put("subject_sla_hours", subjectSlaHours);
        config.put("level_multipliers", levelMultipliers);
        return config;
    }

    /**
     * Actualiza configuración de SLA (para testing o configuración dinámica)
     */
    public void updateDefaultSlaHours(int hours) {
        this.defaultSlaHours = hours;
        log.info("Updated default SLA hours to: {}", hours);
    }

    // ================================
    // DATA TRANSFER OBJECTS
    // ================================

    public enum SlaStatusType {
        ON_TRACK,    // Todo va bien
        AT_RISK,     // En riesgo de incumplir
        CRITICAL,    // Estado crítico
        BREACHED,    // SLA incumplido
        UNKNOWN      // No se puede determinar
    }

    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class SlaStatus {
        private SlaStatusType status;
        private double progress;      // 0.0 a 1.0+
        private Long hoursOverdue;   // Solo para BREACHED
        private String message;
        private Instant analyzedAt;

        @lombok.Builder.Default
        private Instant analyzedAt_default = Instant.now();
    }

    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class SlaRecommendations {
        private Subject subject;
        private Level level;
        private SlaStatusType currentStatus;
        private String priority;
        
        @lombok.Singular
        private java.util.List<String> recommendations;
        
        @lombok.Builder.Default
        private Instant generatedAt = Instant.now();
    }

    /**
     * Métricas de SLA para reporting
     */
    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class SlaMetrics {
        private int totalEvaluations;
        private int onTrackCount;
        private int atRiskCount;
        private int criticalCount;
        private int breachedCount;
        private double averageCompletionTime;
        private double slaComplianceRate;
        private Map<Subject, Double> complianceBySubject;
        private Map<Level, Double> complianceByLevel;
    }
}