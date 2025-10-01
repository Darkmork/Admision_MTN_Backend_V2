package com.desafios.mtn.evaluationservice.repository;

import com.desafios.mtn.evaluationservice.domain.Evaluation;
import com.desafios.mtn.evaluationservice.domain.Evaluation.EvaluationStatus;
import com.desafios.mtn.evaluationservice.domain.Evaluation.Subject;
import com.desafios.mtn.evaluationservice.domain.Evaluation.Level;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio para la entidad Evaluation
 * Proporciona operaciones de acceso a datos y consultas especializadas
 */
@Repository
public interface EvaluationRepository extends JpaRepository<Evaluation, UUID> {

    // ================================
    // BASIC QUERIES
    // ================================

    /**
     * Busca evaluaciones por applicationId
     */
    List<Evaluation> findByApplicationId(UUID applicationId);

    /**
     * Busca evaluaciones asignadas a un evaluador específico
     */
    List<Evaluation> findByEvaluatorId(String evaluatorId);

    /**
     * Busca evaluaciones por estado
     */
    List<Evaluation> findByStatus(EvaluationStatus status);

    /**
     * Busca evaluaciones por materia
     */
    List<Evaluation> findBySubject(Subject subject);

    /**
     * Busca evaluaciones por nivel educativo
     */
    List<Evaluation> findByLevel(Level level);

    // ================================
    // COMBINED QUERIES
    // ================================

    /**
     * Busca evaluaciones de un evaluador en un estado específico
     */
    List<Evaluation> findByEvaluatorIdAndStatus(String evaluatorId, EvaluationStatus status);

    /**
     * Busca evaluaciones de una aplicación en estados específicos
     */
    List<Evaluation> findByApplicationIdAndStatusIn(UUID applicationId, List<EvaluationStatus> statuses);

    /**
     * Busca evaluaciones por materia y estado
     */
    List<Evaluation> findBySubjectAndStatus(Subject subject, EvaluationStatus status);

    /**
     * Busca evaluaciones por evaluador, materia y estado
     */
    List<Evaluation> findByEvaluatorIdAndSubjectAndStatus(String evaluatorId, Subject subject, EvaluationStatus status);

    // ================================
    // TIME-BASED QUERIES
    // ================================

    /**
     * Busca evaluaciones vencidas (SLA excedido)
     */
    @Query("SELECT e FROM Evaluation e WHERE e.expectedCompletionAt < :now AND e.status IN :activeStatuses")
    List<Evaluation> findOverdueEvaluations(@Param("now") Instant now, @Param("activeStatuses") List<EvaluationStatus> activeStatuses);

    /**
     * Busca evaluaciones que requieren atención urgente
     */
    @Query("""
        SELECT e FROM Evaluation e 
        WHERE (e.expectedCompletionAt < :now AND e.status IN :activeStatuses) 
           OR (e.status = 'ASSIGNED' AND e.assignedAt < :twoDaysAgo)
           OR (e.status = 'IN_PROGRESS' AND e.startedAt < :twoDaysAgo)
        ORDER BY e.priority DESC, e.assignedAt ASC
    """)
    List<Evaluation> findUrgentEvaluations(
        @Param("now") Instant now, 
        @Param("twoDaysAgo") Instant twoDaysAgo, 
        @Param("activeStatuses") List<EvaluationStatus> activeStatuses
    );

    /**
     * Busca evaluaciones asignadas antes de una fecha específica
     */
    List<Evaluation> findByStatusAndAssignedAtBefore(EvaluationStatus status, Instant cutoffDate);

    /**
     * Busca evaluaciones completadas en un rango de fechas
     */
    List<Evaluation> findByStatusAndCompletedAtBetween(EvaluationStatus status, Instant start, Instant end);

    // ================================
    // WORKLOAD QUERIES
    // ================================

    /**
     * Cuenta evaluaciones activas por evaluador
     */
    @Query("SELECT COUNT(e) FROM Evaluation e WHERE e.evaluatorId = :evaluatorId AND e.status IN :activeStatuses")
    Long countActiveEvaluationsByEvaluator(@Param("evaluatorId") String evaluatorId, @Param("activeStatuses") List<EvaluationStatus> activeStatuses);

    /**
     * Busca carga de trabajo por evaluador
     */
    @Query("""
        SELECT e.evaluatorId, COUNT(e) as workload 
        FROM Evaluation e 
        WHERE e.status IN :activeStatuses 
        GROUP BY e.evaluatorId 
        ORDER BY workload ASC
    """)
    List<Object[]> findWorkloadDistribution(@Param("activeStatuses") List<EvaluationStatus> activeStatuses);

    /**
     * Busca evaluadores disponibles por materia (menor carga de trabajo)
     */
    @Query("""
        SELECT e.evaluatorId, COUNT(e) as workload 
        FROM Evaluation e 
        WHERE e.subject = :subject AND e.status IN :activeStatuses 
        GROUP BY e.evaluatorId 
        ORDER BY workload ASC
    """)
    List<Object[]> findAvailableEvaluatorsBySubject(@Param("subject") Subject subject, @Param("activeStatuses") List<EvaluationStatus> activeStatuses);

    // ================================
    // COMPLETION AND METRICS QUERIES
    // ================================

    /**
     * Verifica si todas las evaluaciones de una aplicación están completadas
     */
    @Query("SELECT COUNT(e) FROM Evaluation e WHERE e.applicationId = :applicationId AND e.status != 'COMPLETED' AND e.status != 'CANCELLED'")
    Long countIncompleteEvaluationsByApplication(@Param("applicationId") UUID applicationId);

    /**
     * Busca evaluaciones completadas de una aplicación
     */
    @Query("SELECT e FROM Evaluation e WHERE e.applicationId = :applicationId AND e.status = 'COMPLETED' ORDER BY e.completedAt DESC")
    List<Evaluation> findCompletedEvaluationsByApplication(@Param("applicationId") UUID applicationId);

    /**
     * Busca estadísticas de evaluaciones por período
     */
    @Query("""
        SELECT 
            e.status,
            COUNT(e) as count,
            AVG(e.processingTimeMinutes) as avgProcessingTime,
            COUNT(CASE WHEN e.slaExceeded = true THEN 1 END) as slaBreaches
        FROM Evaluation e 
        WHERE e.createdAt BETWEEN :start AND :end 
        GROUP BY e.status
    """)
    List<Object[]> findEvaluationStatistics(@Param("start") Instant start, @Param("end") Instant end);

    /**
     * Busca métricas de performance por evaluador
     */
    @Query("""
        SELECT 
            e.evaluatorId,
            COUNT(e) as totalEvaluations,
            AVG(e.processingTimeMinutes) as avgProcessingTime,
            COUNT(CASE WHEN e.passed = true THEN 1 END) as passedEvaluations,
            COUNT(CASE WHEN e.slaExceeded = true THEN 1 END) as slaBreaches
        FROM Evaluation e 
        WHERE e.status = 'COMPLETED' 
          AND e.completedAt BETWEEN :start AND :end
        GROUP BY e.evaluatorId
        ORDER BY totalEvaluations DESC
    """)
    List<Object[]> findEvaluatorPerformanceMetrics(@Param("start") Instant start, @Param("end") Instant end);

    // ================================
    // BUSINESS LOGIC QUERIES
    // ================================

    /**
     * Busca la próxima evaluación pendiente para asignación automática
     */
    @Query("""
        SELECT e FROM Evaluation e 
        WHERE e.status = 'PENDING' 
        ORDER BY e.priority DESC, e.createdAt ASC
    """)
    Optional<Evaluation> findNextPendingEvaluation();

    /**
     * Busca evaluaciones que pueden ser reasignadas (asignadas pero no iniciadas por mucho tiempo)
     */
    @Query("""
        SELECT e FROM Evaluation e 
        WHERE e.status = 'ASSIGNED' 
          AND e.assignedAt < :cutoffTime
        ORDER BY e.assignedAt ASC
    """)
    List<Evaluation> findEvaluationsForReassignment(@Param("cutoffTime") Instant cutoffTime);

    /**
     * Verifica si existe una evaluación activa para una aplicación y materia específica
     */
    @Query("""
        SELECT COUNT(e) > 0 FROM Evaluation e 
        WHERE e.applicationId = :applicationId 
          AND e.subject = :subject 
          AND e.status IN :activeStatuses
    """)
    boolean existsActiveEvaluationByApplicationAndSubject(
        @Param("applicationId") UUID applicationId, 
        @Param("subject") Subject subject, 
        @Param("activeStatuses") List<EvaluationStatus> activeStatuses
    );

    /**
     * Busca la última evaluación de una aplicación para una materia específica
     */
    @Query("""
        SELECT e FROM Evaluation e 
        WHERE e.applicationId = :applicationId 
          AND e.subject = :subject 
        ORDER BY e.createdAt DESC
    """)
    Optional<Evaluation> findLatestEvaluationByApplicationAndSubject(@Param("applicationId") UUID applicationId, @Param("subject") Subject subject);

    // ================================
    // SLA AND MONITORING QUERIES
    // ================================

    /**
     * Cuenta evaluaciones con SLA excedido
     */
    @Query("SELECT COUNT(e) FROM Evaluation e WHERE e.slaExceeded = true AND e.createdAt BETWEEN :start AND :end")
    Long countSlaExceededEvaluations(@Param("start") Instant start, @Param("end") Instant end);

    /**
     * Busca evaluaciones en riesgo de exceder SLA
     */
    @Query("""
        SELECT e FROM Evaluation e 
        WHERE e.status IN :activeStatuses 
          AND e.expectedCompletionAt BETWEEN :now AND :warningThreshold
        ORDER BY e.expectedCompletionAt ASC
    """)
    List<Evaluation> findEvaluationsAtRisk(
        @Param("now") Instant now, 
        @Param("warningThreshold") Instant warningThreshold, 
        @Param("activeStatuses") List<EvaluationStatus> activeStatuses
    );

    // ================================
    // CUSTOM DELETE OPERATIONS
    // ================================

    /**
     * Elimina evaluaciones canceladas antiguas (para limpieza)
     */
    @Query("DELETE FROM Evaluation e WHERE e.status = 'CANCELLED' AND e.cancelledAt < :cutoffDate")
    int deleteCancelledEvaluationsBefore(@Param("cutoffDate") Instant cutoffDate);
}