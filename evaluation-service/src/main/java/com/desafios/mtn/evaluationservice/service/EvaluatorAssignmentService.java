package com.desafios.mtn.evaluationservice.service;

import com.desafios.mtn.evaluationservice.domain.Evaluation.Subject;
import com.desafios.mtn.evaluationservice.domain.Evaluation.Level;
import com.desafios.mtn.evaluationservice.domain.Evaluation.EvaluationStatus;
import com.desafios.mtn.evaluationservice.repository.EvaluationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio especializado en la asignación de evaluadores
 * Implementa lógicas de balanceo de carga y disponibilidad
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EvaluatorAssignmentService {

    private final EvaluationRepository evaluationRepository;
    private final EvaluatorRegistry evaluatorRegistry;

    // ================================
    // ASSIGNMENT OPERATIONS
    // ================================

    /**
     * Encuentra el mejor evaluador disponible para una materia y nivel
     */
    public String findBestAvailableEvaluator(Subject subject, Level level) {
        return findBestAvailableEvaluator(subject, level, null);
    }

    /**
     * Encuentra el mejor evaluador disponible excluyendo uno específico
     */
    public String findBestAvailableEvaluator(Subject subject, Level level, String excludeEvaluatorId) {
        log.debug("Finding best evaluator for subject: {}, level: {}, excluding: {}", 
                 subject, level, excludeEvaluatorId);

        // Obtener evaluadores calificados para la materia y nivel
        List<EvaluatorInfo> qualifiedEvaluators = evaluatorRegistry.getQualifiedEvaluators(subject, level);
        
        if (qualifiedEvaluators.isEmpty()) {
            log.warn("No qualified evaluators found for subject: {} and level: {}", subject, level);
            return null;
        }

        // Filtrar evaluadores excluidos
        if (excludeEvaluatorId != null) {
            qualifiedEvaluators = qualifiedEvaluators.stream()
                .filter(evaluator -> !evaluator.getId().equals(excludeEvaluatorId))
                .collect(Collectors.toList());
        }

        if (qualifiedEvaluators.isEmpty()) {
            log.warn("No available evaluators after filtering exclusions");
            return null;
        }

        // Obtener carga de trabajo actual de evaluadores calificados
        Map<String, Integer> currentWorkload = getCurrentWorkload(
            qualifiedEvaluators.stream()
                .map(EvaluatorInfo::getId)
                .collect(Collectors.toList())
        );

        // Encontrar el evaluador con menor carga y mayor experiencia
        return qualifiedEvaluators.stream()
            .filter(evaluator -> isEvaluatorAvailable(evaluator))
            .min(Comparator
                .<EvaluatorInfo>comparingInt(e -> currentWorkload.getOrDefault(e.getId(), 0))
                .thenComparingInt(e -> -e.getExperienceLevel()) // Mayor experiencia primero
                .thenComparing(EvaluatorInfo::getId) // Desempate determinístico
            )
            .map(EvaluatorInfo::getId)
            .orElse(null);
    }

    /**
     * Valida que un evaluador esté disponible y calificado
     */
    public void validateEvaluatorAvailability(String evaluatorId, Subject subject, Level level) {
        EvaluatorInfo evaluator = evaluatorRegistry.getEvaluator(evaluatorId);
        
        if (evaluator == null) {
            throw new IllegalArgumentException("Evaluator not found: " + evaluatorId);
        }

        if (!evaluator.isActive()) {
            throw new IllegalStateException("Evaluator is not active: " + evaluatorId);
        }

        if (!evaluator.isQualifiedFor(subject, level)) {
            throw new IllegalStateException(
                String.format("Evaluator %s is not qualified for subject %s and level %s", 
                             evaluatorId, subject, level));
        }

        if (!isEvaluatorAvailable(evaluator)) {
            throw new IllegalStateException("Evaluator is not available: " + evaluatorId);
        }
    }

    /**
     * Redistribuye evaluaciones para balancear carga de trabajo
     */
    public List<String> redistributeWorkload(Subject subject) {
        log.info("Redistributing workload for subject: {}", subject);

        List<String> redistributionActions = new ArrayList<>();
        
        // Obtener evaluadores de la materia y su carga actual
        List<EvaluatorInfo> evaluators = evaluatorRegistry.getQualifiedEvaluators(subject, null);
        Map<String, Integer> workload = getCurrentWorkload(
            evaluators.stream().map(EvaluatorInfo::getId).collect(Collectors.toList())
        );

        if (workload.isEmpty()) {
            return redistributionActions;
        }

        // Calcular estadísticas de carga
        int totalWorkload = workload.values().stream().mapToInt(Integer::intValue).sum();
        double averageWorkload = (double) totalWorkload / workload.size();
        
        // Identificar evaluadores sobrecargados y subcargados
        List<Map.Entry<String, Integer>> overloaded = workload.entrySet().stream()
            .filter(entry -> entry.getValue() > averageWorkload * 1.5)
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .collect(Collectors.toList());

        List<Map.Entry<String, Integer>> underloaded = workload.entrySet().stream()
            .filter(entry -> entry.getValue() < averageWorkload * 0.5)
            .sorted(Map.Entry.comparingByValue())
            .collect(Collectors.toList());

        // Proceso de redistribución (simplificado)
        for (Map.Entry<String, Integer> overloadedEntry : overloaded) {
            for (Map.Entry<String, Integer> underloadedEntry : underloaded) {
                if (overloadedEntry.getValue() - underloadedEntry.getValue() > 2) {
                    // Aquí se implementaría la lógica de reasignación real
                    redistributionActions.add(
                        String.format("Consider reassigning evaluations from %s to %s", 
                                     overloadedEntry.getKey(), underloadedEntry.getKey())
                    );
                }
            }
        }

        log.info("Generated {} redistribution recommendations", redistributionActions.size());
        return redistributionActions;
    }

    // ================================
    // HELPER METHODS
    // ================================

    /**
     * Verifica si un evaluador está disponible
     */
    private boolean isEvaluatorAvailable(EvaluatorInfo evaluator) {
        if (!evaluator.isActive()) {
            return false;
        }

        // Verificar límite de evaluaciones activas
        int currentActiveCount = getCurrentActiveEvaluationCount(evaluator.getId());
        return currentActiveCount < evaluator.getMaxConcurrentEvaluations();
    }

    /**
     * Obtiene el conteo actual de evaluaciones activas para un evaluador
     */
    private int getCurrentActiveEvaluationCount(String evaluatorId) {
        List<EvaluationStatus> activeStatuses = Arrays.asList(
            EvaluationStatus.ASSIGNED, EvaluationStatus.IN_PROGRESS);
        
        Long count = evaluationRepository.countActiveEvaluationsByEvaluator(evaluatorId, activeStatuses);
        return count != null ? count.intValue() : 0;
    }

    /**
     * Obtiene la carga de trabajo actual para una lista de evaluadores
     */
    private Map<String, Integer> getCurrentWorkload(List<String> evaluatorIds) {
        List<EvaluationStatus> activeStatuses = Arrays.asList(
            EvaluationStatus.ASSIGNED, EvaluationStatus.IN_PROGRESS);

        Map<String, Integer> workload = new HashMap<>();
        
        for (String evaluatorId : evaluatorIds) {
            Long count = evaluationRepository.countActiveEvaluationsByEvaluator(evaluatorId, activeStatuses);
            workload.put(evaluatorId, count != null ? count.intValue() : 0);
        }

        return workload;
    }

    // ================================
    // DATA TRANSFER OBJECTS
    // ================================

    /**
     * Información básica de un evaluador
     */
    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class EvaluatorInfo {
        private String id;
        private String name;
        private String email;
        private boolean active;
        private List<Subject> subjects;
        private List<Level> levels;
        private int experienceLevel; // 1-10
        private int maxConcurrentEvaluations;
        private Map<String, Object> metadata;

        public boolean isQualifiedFor(Subject subject, Level level) {
            boolean subjectMatch = subjects.contains(subject) || subjects.contains(Subject.GENERAL);
            boolean levelMatch = level == null || levels.contains(level) || levels.contains(Level.ALL);
            return subjectMatch && levelMatch;
        }
    }

    /**
     * Métricas de asignación de evaluadores
     */
    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class AssignmentMetrics {
        private String evaluatorId;
        private int totalAssigned;
        private int currentActive;
        private int completed;
        private int cancelled;
        private double averageCompletionTime;
        private double successRate;
        private int slaBreaches;
    }
}

/**
 * Registro de evaluadores - interfaz para obtener información de evaluadores
 * En una implementación real, esto podría conectarse a un servicio externo
 */
@Service
@Slf4j
class EvaluatorRegistry {

    // Mock data - en implementación real vendría de base de datos o servicio externo
    private final Map<String, EvaluatorAssignmentService.EvaluatorInfo> evaluators;

    public EvaluatorRegistry() {
        this.evaluators = initializeMockEvaluators();
    }

    public EvaluatorAssignmentService.EvaluatorInfo getEvaluator(String evaluatorId) {
        return evaluators.get(evaluatorId);
    }

    public List<EvaluatorAssignmentService.EvaluatorInfo> getQualifiedEvaluators(Subject subject, Level level) {
        return evaluators.values().stream()
            .filter(evaluator -> evaluator.isActive())
            .filter(evaluator -> evaluator.isQualifiedFor(subject, level))
            .collect(Collectors.toList());
    }

    public List<EvaluatorAssignmentService.EvaluatorInfo> getAllActiveEvaluators() {
        return evaluators.values().stream()
            .filter(EvaluatorAssignmentService.EvaluatorInfo::isActive)
            .collect(Collectors.toList());
    }

    /**
     * Inicializa evaluadores mock para testing
     */
    private Map<String, EvaluatorAssignmentService.EvaluatorInfo> initializeMockEvaluators() {
        Map<String, EvaluatorAssignmentService.EvaluatorInfo> mockEvaluators = new HashMap<>();

        // Evaluadores de Matemáticas
        mockEvaluators.put("math-evaluator-1", 
            EvaluatorAssignmentService.EvaluatorInfo.builder()
                .id("math-evaluator-1")
                .name("María González")
                .email("maria.gonzalez@mtn.cl")
                .active(true)
                .subjects(Arrays.asList(Subject.MATHEMATICS))
                .levels(Arrays.asList(Level.BASIC, Level.HIGH_SCHOOL))
                .experienceLevel(8)
                .maxConcurrentEvaluations(5)
                .build()
        );

        mockEvaluators.put("math-evaluator-2", 
            EvaluatorAssignmentService.EvaluatorInfo.builder()
                .id("math-evaluator-2")
                .name("Carlos Rodríguez")
                .email("carlos.rodriguez@mtn.cl")
                .active(true)
                .subjects(Arrays.asList(Subject.MATHEMATICS))
                .levels(Arrays.asList(Level.HIGH_SCHOOL))
                .experienceLevel(9)
                .maxConcurrentEvaluations(4)
                .build()
        );

        // Evaluadores de Lenguaje
        mockEvaluators.put("lang-evaluator-1", 
            EvaluatorAssignmentService.EvaluatorInfo.builder()
                .id("lang-evaluator-1")
                .name("Ana Morales")
                .email("ana.morales@mtn.cl")
                .active(true)
                .subjects(Arrays.asList(Subject.LANGUAGE))
                .levels(Arrays.asList(Level.BASIC, Level.HIGH_SCHOOL))
                .experienceLevel(7)
                .maxConcurrentEvaluations(6)
                .build()
        );

        // Evaluador de Psicología
        mockEvaluators.put("psych-evaluator-1", 
            EvaluatorAssignmentService.EvaluatorInfo.builder()
                .id("psych-evaluator-1")
                .name("Dr. Roberto Silva")
                .email("roberto.silva@mtn.cl")
                .active(true)
                .subjects(Arrays.asList(Subject.PSYCHOLOGY))
                .levels(Arrays.asList(Level.ALL))
                .experienceLevel(10)
                .maxConcurrentEvaluations(3)
                .build()
        );

        // Evaluador General
        mockEvaluators.put("general-evaluator-1", 
            EvaluatorAssignmentService.EvaluatorInfo.builder()
                .id("general-evaluator-1")
                .name("Patricia Mendoza")
                .email("patricia.mendoza@mtn.cl")
                .active(true)
                .subjects(Arrays.asList(Subject.GENERAL))
                .levels(Arrays.asList(Level.PRESCHOOL, Level.BASIC))
                .experienceLevel(6)
                .maxConcurrentEvaluations(7)
                .build()
        );

        log.info("Initialized {} mock evaluators", mockEvaluators.size());
        return mockEvaluators;
    }
}