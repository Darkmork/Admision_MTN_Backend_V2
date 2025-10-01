package com.desafios.mtn.applicationservice.repository;

import com.desafios.mtn.applicationservice.domain.ApplicationStatus;
import com.desafios.mtn.applicationservice.domain.ReasonCode;
import com.desafios.mtn.applicationservice.domain.TransitionLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository para TransitionLog con consultas de auditoría y análisis
 */
@Repository
public interface TransitionLogRepository extends JpaRepository<TransitionLog, UUID> {

    // ================================
    // CONSULTAS POR APLICACIÓN
    // ================================

    /**
     * Obtiene el historial completo de transiciones de una aplicación
     */
    List<TransitionLog> findByApplicationIdOrderByCreatedAtDesc(UUID applicationId);

    /**
     * Obtiene el historial de transiciones con paginación
     */
    Page<TransitionLog> findByApplicationIdOrderByCreatedAtDesc(UUID applicationId, Pageable pageable);

    /**
     * Obtiene la transición más reciente de una aplicación
     */
    Optional<TransitionLog> findFirstByApplicationIdOrderByCreatedAtDesc(UUID applicationId);

    /**
     * Cuenta transiciones de una aplicación
     */
    long countByApplicationId(UUID applicationId);

    /**
     * Encuentra transiciones específicas de una aplicación
     */
    List<TransitionLog> findByApplicationIdAndToState(UUID applicationId, ApplicationStatus toState);

    // ================================
    // CONSULTAS POR ESTADO
    // ================================

    /**
     * Encuentra transiciones hacia un estado específico
     */
    List<TransitionLog> findByToState(ApplicationStatus toState);

    /**
     * Encuentra transiciones desde un estado específico
     */
    List<TransitionLog> findByFromState(ApplicationStatus fromState);

    /**
     * Encuentra transiciones entre estados específicos
     */
    List<TransitionLog> findByFromStateAndToState(ApplicationStatus fromState, ApplicationStatus toState);

    /**
     * Cuenta transiciones por estado destino
     */
    long countByToState(ApplicationStatus toState);

    /**
     * Encuentra transiciones recientes a un estado
     */
    List<TransitionLog> findByToStateAndCreatedAtAfter(ApplicationStatus toState, Instant after);

    // ================================
    // CONSULTAS POR ACTOR
    // ================================

    /**
     * Encuentra transiciones realizadas por un usuario
     */
    List<TransitionLog> findByActorUserId(String actorUserId);

    /**
     * Encuentra transiciones realizadas por un rol
     */
    List<TransitionLog> findByActorRole(String actorRole);

    /**
     * Encuentra transiciones por usuario y rol
     */
    List<TransitionLog> findByActorUserIdAndActorRole(String actorUserId, String actorRole);

    /**
     * Encuentra transiciones automáticas
     */
    List<TransitionLog> findByAutomatedTrue();

    /**
     * Encuentra transiciones manuales
     */
    List<TransitionLog> findByAutomatedFalse();

    // ================================
    // CONSULTAS POR REASON CODE
    // ================================

    /**
     * Encuentra transiciones por código de razón
     */
    List<TransitionLog> findByReasonCode(ReasonCode reasonCode);

    /**
     * Cuenta transiciones por código de razón
     */
    long countByReasonCode(ReasonCode reasonCode);

    /**
     * Encuentra transiciones con códigos positivos
     */
    @Query("SELECT t FROM TransitionLog t WHERE t.reasonCode IN :positiveCodes")
    List<TransitionLog> findPositiveTransitions(@Param("positiveCodes") List<ReasonCode> positiveCodes);

    /**
     * Encuentra transiciones con códigos negativos
     */
    @Query("SELECT t FROM TransitionLog t WHERE t.reasonCode IN :negativeCodes")
    List<TransitionLog> findNegativeTransitions(@Param("negativeCodes") List<ReasonCode> negativeCodes);

    // ================================
    // CONSULTAS TEMPORALES
    // ================================

    /**
     * Encuentra transiciones en un rango de fechas
     */
    List<TransitionLog> findByCreatedAtBetween(Instant startDate, Instant endDate);

    /**
     * Encuentra transiciones recientes
     */
    List<TransitionLog> findByCreatedAtAfter(Instant after);

    /**
     * Encuentra transiciones de hoy
     */
    @Query("SELECT t FROM TransitionLog t WHERE DATE(t.createdAt) = CURRENT_DATE")
    List<TransitionLog> findTodaysTransitions();

    /**
     * Encuentra transiciones de la última semana
     */
    @Query("SELECT t FROM TransitionLog t WHERE t.createdAt >= :weekAgo ORDER BY t.createdAt DESC")
    List<TransitionLog> findLastWeekTransitions(@Param("weekAgo") Instant weekAgo);

    // ================================
    // CONSULTAS AVANZADAS
    // ================================

    /**
     * Encuentra transiciones duplicadas por clave de idempotencia
     */
    List<TransitionLog> findByIdempotencyKey(String idempotencyKey);

    /**
     * Verifica si existe una transición con clave de idempotencia
     */
    boolean existsByIdempotencyKey(String idempotencyKey);

    /**
     * Encuentra transiciones con comentarios
     */
    @Query("SELECT t FROM TransitionLog t WHERE t.comment IS NOT NULL AND LENGTH(t.comment) > 0")
    List<TransitionLog> findTransitionsWithComments();

    /**
     * Encuentra transiciones por dirección IP
     */
    List<TransitionLog> findByIpAddress(java.net.InetAddress ipAddress);

    /**
     * Busca transiciones por texto en comentarios
     */
    @Query("SELECT t FROM TransitionLog t WHERE LOWER(t.comment) LIKE LOWER(CONCAT('%', :searchText, '%'))")
    List<TransitionLog> searchByComment(@Param("searchText") String searchText);

    // ================================
    // ESTADÍSTICAS Y ANÁLISIS
    // ================================

    /**
     * Obtiene estadísticas por estado destino
     */
    @Query("SELECT t.toState, COUNT(t) FROM TransitionLog t GROUP BY t.toState ORDER BY COUNT(t) DESC")
    List<Object[]> getTransitionStatisticsByToState();

    /**
     * Obtiene estadísticas por código de razón
     */
    @Query("SELECT t.reasonCode, COUNT(t) FROM TransitionLog t GROUP BY t.reasonCode ORDER BY COUNT(t) DESC")
    List<Object[]> getTransitionStatisticsByReasonCode();

    /**
     * Obtiene estadísticas por actor (rol)
     */
    @Query("SELECT t.actorRole, COUNT(t) FROM TransitionLog t WHERE t.actorRole IS NOT NULL GROUP BY t.actorRole ORDER BY COUNT(t) DESC")
    List<Object[]> getTransitionStatisticsByRole();

    /**
     * Obtiene estadísticas diarias de transiciones
     */
    @Query(value = "SELECT DATE(created_at) as day, COUNT(*) FROM transition_log WHERE created_at >= :startDate GROUP BY DATE(created_at) ORDER BY day DESC", 
           nativeQuery = true)
    List<Object[]> getDailyTransitionStatistics(@Param("startDate") Instant startDate);

    /**
     * Obtiene las transiciones más comunes
     */
    @Query("SELECT t.fromState, t.toState, COUNT(t) as transition_count FROM TransitionLog t GROUP BY t.fromState, t.toState ORDER BY COUNT(t) DESC")
    List<Object[]> getMostCommonTransitions();

    /**
     * Calcula tiempo promedio entre transiciones por aplicación
     */
    @Query(value = """
        WITH transition_times AS (
            SELECT 
                application_id,
                created_at,
                LAG(created_at) OVER (PARTITION BY application_id ORDER BY created_at) as prev_created_at
            FROM transition_log
        )
        SELECT 
            AVG(EXTRACT(EPOCH FROM (created_at - prev_created_at))/3600) as avg_hours_between_transitions
        FROM transition_times 
        WHERE prev_created_at IS NOT NULL
        """, nativeQuery = true)
    Optional<Double> getAverageTimeBetweenTransitions();

    // ================================
    // CONSULTAS PARA NOTIFICACIONES
    // ================================

    /**
     * Encuentra transiciones que requieren notificación
     */
    @Query("SELECT t FROM TransitionLog t WHERE t.toState IN ('PENDING', 'DOCUMENTS_REQUESTED', 'UNDER_REVIEW', 'INTERVIEW_SCHEDULED', 'EXAM_SCHEDULED', 'APPROVED', 'REJECTED', 'WAITLIST', 'ENROLLED', 'EXPIRED')")
    List<TransitionLog> findTransitionsRequiringNotification();

    /**
     * Encuentra transiciones de alta prioridad para notificación
     */
    @Query("SELECT t FROM TransitionLog t WHERE t.toState IN ('APPROVED', 'ENROLLED', 'REJECTED', 'EXPIRED')")
    List<TransitionLog> findHighPriorityTransitions();

    /**
     * Encuentra transiciones recientes sin procesar para notificaciones
     */
    @Query("SELECT t FROM TransitionLog t WHERE t.createdAt >= :since AND t.toState != 'DRAFT' ORDER BY t.createdAt DESC")
    List<TransitionLog> findRecentTransitionsForNotification(@Param("since") Instant since);

    // ================================
    // CONSULTAS DE AUDITORÍA
    // ================================

    /**
     * Encuentra transiciones sospechosas (muchas en poco tiempo)
     */
    @Query(value = """
        SELECT application_id, COUNT(*) as transition_count
        FROM transition_log 
        WHERE created_at >= :since
        GROUP BY application_id 
        HAVING COUNT(*) > :threshold
        ORDER BY COUNT(*) DESC
        """, nativeQuery = true)
    List<Object[]> findSuspiciousTransitionActivity(@Param("since") Instant since, @Param("threshold") int threshold);

    /**
     * Encuentra transiciones por usuario en un período
     */
    @Query("SELECT COUNT(t) FROM TransitionLog t WHERE t.actorUserId = :userId AND t.createdAt >= :since")
    long countUserTransitionsSince(@Param("userId") String userId, @Param("since") Instant since);

    /**
     * Encuentra fallos de transición (transiciones negativas)
     */
    @Query("SELECT t FROM TransitionLog t WHERE t.toState IN ('REJECTED', 'EXPIRED') ORDER BY t.createdAt DESC")
    List<TransitionLog> findFailedTransitions();

    /**
     * Obtiene el historial completo de una aplicación con metadatos
     */
    @Query("SELECT t FROM TransitionLog t WHERE t.applicationId = :applicationId ORDER BY t.createdAt ASC")
    List<TransitionLog> findCompleteTransitionHistory(@Param("applicationId") UUID applicationId);

    // ================================
    // CONSULTAS DE LIMPIEZA
    // ================================

    /**
     * Encuentra transiciones antiguas para archivado
     */
    @Query("SELECT t FROM TransitionLog t WHERE t.createdAt < :cutoffDate ORDER BY t.createdAt ASC")
    List<TransitionLog> findOldTransitionsForArchival(@Param("cutoffDate") Instant cutoffDate, Pageable pageable);

    /**
     * Cuenta transiciones antiguas
     */
    @Query("SELECT COUNT(t) FROM TransitionLog t WHERE t.createdAt < :cutoffDate")
    long countOldTransitions(@Param("cutoffDate") Instant cutoffDate);
}