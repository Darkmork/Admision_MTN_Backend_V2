package com.desafios.mtn.evaluationservice.repository;

import com.desafios.mtn.evaluationservice.domain.Interview;
import com.desafios.mtn.evaluationservice.domain.Interview.InterviewStatus;
import com.desafios.mtn.evaluationservice.domain.Interview.InterviewType;
import com.desafios.mtn.evaluationservice.domain.Interview.Recommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio para la entidad Interview
 * Proporciona operaciones de acceso a datos y consultas especializadas
 */
@Repository
public interface InterviewRepository extends JpaRepository<Interview, UUID> {

    // ================================
    // BASIC QUERIES
    // ================================

    /**
     * Busca entrevistas por applicationId
     */
    List<Interview> findByApplicationId(UUID applicationId);

    /**
     * Busca entrevistas asignadas a un entrevistador específico
     */
    List<Interview> findByInterviewerId(String interviewerId);

    /**
     * Busca entrevistas por estado
     */
    List<Interview> findByStatus(InterviewStatus status);

    /**
     * Busca entrevistas por tipo
     */
    List<Interview> findByType(InterviewType type);

    // ================================
    // COMBINED QUERIES
    // ================================

    /**
     * Busca entrevistas de un entrevistador en un estado específico
     */
    List<Interview> findByInterviewerIdAndStatus(String interviewerId, InterviewStatus status);

    /**
     * Busca entrevistas de una aplicación en estados específicos
     */
    List<Interview> findByApplicationIdAndStatusIn(UUID applicationId, List<InterviewStatus> statuses);

    /**
     * Busca entrevistas por tipo y estado
     */
    List<Interview> findByTypeAndStatus(InterviewType type, InterviewStatus status);

    /**
     * Busca entrevistas por entrevistador, tipo y estado
     */
    List<Interview> findByInterviewerIdAndTypeAndStatus(String interviewerId, InterviewType type, InterviewStatus status);

    // ================================
    // SCHEDULING QUERIES
    // ================================

    /**
     * Busca entrevistas programadas en un rango de fechas
     */
    List<Interview> findByScheduledAtBetween(Instant start, Instant end);

    /**
     * Busca entrevistas programadas para un día específico por entrevistador
     */
    @Query("""
        SELECT i FROM Interview i 
        WHERE i.interviewerId = :interviewerId 
          AND i.scheduledAt BETWEEN :dayStart AND :dayEnd 
          AND i.status NOT IN ('CANCELLED', 'NO_SHOW')
        ORDER BY i.scheduledAt ASC
    """)
    List<Interview> findByInterviewerAndDay(@Param("interviewerId") String interviewerId, 
                                           @Param("dayStart") Instant dayStart, 
                                           @Param("dayEnd") Instant dayEnd);

    /**
     * Busca conflictos de horario para un entrevistador
     */
    @Query("""
        SELECT i FROM Interview i 
        WHERE i.interviewerId = :interviewerId 
          AND i.status NOT IN ('CANCELLED', 'NO_SHOW', 'COMPLETED')
          AND (
            (i.scheduledAt <= :proposedStart AND :proposedStart < i.scheduledAt + FUNCTION('INTERVAL', i.durationMinutes, 'MINUTE')) OR
            (i.scheduledAt < :proposedEnd AND :proposedEnd <= i.scheduledAt + FUNCTION('INTERVAL', i.durationMinutes, 'MINUTE')) OR
            (:proposedStart <= i.scheduledAt AND i.scheduledAt + FUNCTION('INTERVAL', i.durationMinutes, 'MINUTE') <= :proposedEnd)
          )
    """)
    List<Interview> findSchedulingConflicts(@Param("interviewerId") String interviewerId, 
                                           @Param("proposedStart") Instant proposedStart, 
                                           @Param("proposedEnd") Instant proposedEnd);

    /**
     * Busca entrevistas vencidas (que debieron haber ocurrido)
     */
    @Query("""
        SELECT i FROM Interview i 
        WHERE i.scheduledAt < :now 
          AND i.status IN ('SCHEDULED', 'CONFIRMED', 'REMINDED')
        ORDER BY i.scheduledAt ASC
    """)
    List<Interview> findOverdueInterviews(@Param("now") Instant now);

    // ================================
    // REMINDER AND NOTIFICATION QUERIES
    // ================================

    /**
     * Busca entrevistas que necesitan recordatorio
     */
    @Query("""
        SELECT i FROM Interview i 
        WHERE i.reminderSent = false 
          AND i.status IN ('SCHEDULED', 'CONFIRMED')
          AND i.scheduledAt BETWEEN :now AND :reminderWindow
        ORDER BY i.scheduledAt ASC
    """)
    List<Interview> findInterviewsNeedingReminder(@Param("now") Instant now, 
                                                 @Param("reminderWindow") Instant reminderWindow);

    /**
     * Busca entrevistas que necesitan confirmación
     */
    @Query("""
        SELECT i FROM Interview i 
        WHERE i.confirmationSent = false 
          AND i.status = 'SCHEDULED'
          AND i.scheduledAt > :now
        ORDER BY i.scheduledAt ASC
    """)
    List<Interview> findInterviewsNeedingConfirmation(@Param("now") Instant now);

    // ================================
    // WORKLOAD QUERIES
    // ================================

    /**
     * Cuenta entrevistas activas por entrevistador
     */
    @Query("""
        SELECT COUNT(i) FROM Interview i 
        WHERE i.interviewerId = :interviewerId 
          AND i.status IN :activeStatuses
    """)
    Long countActiveInterviewsByInterviewer(@Param("interviewerId") String interviewerId, 
                                          @Param("activeStatuses") List<InterviewStatus> activeStatuses);

    /**
     * Busca carga de trabajo por entrevistador en un período
     */
    @Query("""
        SELECT i.interviewerId, COUNT(i) as workload 
        FROM Interview i 
        WHERE i.scheduledAt BETWEEN :start AND :end 
          AND i.status NOT IN ('CANCELLED', 'NO_SHOW') 
        GROUP BY i.interviewerId 
        ORDER BY workload ASC
    """)
    List<Object[]> findInterviewerWorkload(@Param("start") Instant start, @Param("end") Instant end);

    /**
     * Busca disponibilidad de entrevistadores por tipo
     */
    @Query("""
        SELECT i.interviewerId, COUNT(i) as currentLoad 
        FROM Interview i 
        WHERE i.type = :type 
          AND i.scheduledAt BETWEEN :start AND :end
          AND i.status NOT IN ('CANCELLED', 'NO_SHOW', 'COMPLETED')
        GROUP BY i.interviewerId 
        ORDER BY currentLoad ASC
    """)
    List<Object[]> findAvailableInterviewersByType(@Param("type") InterviewType type, 
                                                   @Param("start") Instant start, 
                                                   @Param("end") Instant end);

    // ================================
    // COMPLETION AND RESULTS QUERIES
    // ================================

    /**
     * Busca entrevistas completadas de una aplicación
     */
    @Query("""
        SELECT i FROM Interview i 
        WHERE i.applicationId = :applicationId 
          AND i.status = 'COMPLETED' 
        ORDER BY i.completedAt DESC
    """)
    List<Interview> findCompletedInterviewsByApplication(@Param("applicationId") UUID applicationId);

    /**
     * Verifica si todas las entrevistas requeridas están completadas para una aplicación
     */
    @Query("""
        SELECT COUNT(i) FROM Interview i 
        WHERE i.applicationId = :applicationId 
          AND i.status NOT IN ('COMPLETED', 'CANCELLED')
    """)
    Long countIncompleteInterviewsByApplication(@Param("applicationId") UUID applicationId);

    /**
     * Busca entrevistas por recomendación
     */
    List<Interview> findByRecommendation(Recommendation recommendation);

    /**
     * Busca entrevistas que requieren seguimiento
     */
    @Query("""
        SELECT i FROM Interview i 
        WHERE i.status = 'COMPLETED' 
          AND i.recommendation IN ('REQUIRES_FOLLOW_UP', 'CONDITIONALLY_RECOMMENDED')
        ORDER BY i.completedAt DESC
    """)
    List<Interview> findInterviewsRequiringFollowUp();

    // ================================
    // STATISTICS AND REPORTING QUERIES
    // ================================

    /**
     * Busca estadísticas de entrevistas por período
     */
    @Query("""
        SELECT 
            i.status,
            COUNT(i) as count,
            AVG(CASE WHEN i.startedAt IS NOT NULL AND i.completedAt IS NOT NULL 
                THEN EXTRACT(EPOCH FROM (i.completedAt - i.startedAt))/60 
                ELSE NULL END) as avgDurationMinutes
        FROM Interview i 
        WHERE i.createdAt BETWEEN :start AND :end 
        GROUP BY i.status
    """)
    List<Object[]> findInterviewStatistics(@Param("start") Instant start, @Param("end") Instant end);

    /**
     * Busca métricas de performance por entrevistador
     */
    @Query("""
        SELECT 
            i.interviewerId,
            COUNT(i) as totalInterviews,
            AVG(i.overallRating) as avgRating,
            COUNT(CASE WHEN i.recommendation IN ('HIGHLY_RECOMMENDED', 'RECOMMENDED') THEN 1 END) as positiveRecommendations,
            COUNT(CASE WHEN i.status = 'NO_SHOW' THEN 1 END) as noShows
        FROM Interview i 
        WHERE i.status = 'COMPLETED' 
          AND i.completedAt BETWEEN :start AND :end
        GROUP BY i.interviewerId
        ORDER BY totalInterviews DESC
    """)
    List<Object[]> findInterviewerPerformanceMetrics(@Param("start") Instant start, @Param("end") Instant end);

    /**
     * Busca distribución de recomendaciones
     */
    @Query("""
        SELECT i.recommendation, COUNT(i) as count 
        FROM Interview i 
        WHERE i.status = 'COMPLETED' 
          AND i.completedAt BETWEEN :start AND :end 
        GROUP BY i.recommendation
        ORDER BY count DESC
    """)
    List<Object[]> findRecommendationDistribution(@Param("start") Instant start, @Param("end") Instant end);

    // ================================
    // BUSINESS LOGIC QUERIES
    // ================================

    /**
     * Verifica si existe una entrevista activa para una aplicación y tipo específico
     */
    @Query("""
        SELECT COUNT(i) > 0 FROM Interview i 
        WHERE i.applicationId = :applicationId 
          AND i.type = :type 
          AND i.status NOT IN ('COMPLETED', 'CANCELLED', 'NO_SHOW')
    """)
    boolean existsActiveInterviewByApplicationAndType(@Param("applicationId") UUID applicationId, 
                                                     @Param("type") InterviewType type);

    /**
     * Busca la última entrevista de una aplicación para un tipo específico
     */
    @Query("""
        SELECT i FROM Interview i 
        WHERE i.applicationId = :applicationId 
          AND i.type = :type 
        ORDER BY i.createdAt DESC
    """)
    Optional<Interview> findLatestInterviewByApplicationAndType(@Param("applicationId") UUID applicationId, 
                                                               @Param("type") InterviewType type);

    /**
     * Busca entrevistas que pueden ser reprogramadas automáticamente
     */
    @Query("""
        SELECT i FROM Interview i 
        WHERE i.status = 'SCHEDULED' 
          AND i.scheduledAt < :cutoffTime
          AND i.rescheduledAt IS NULL
        ORDER BY i.scheduledAt ASC
    """)
    List<Interview> findInterviewsForAutomaticRescheduling(@Param("cutoffTime") Instant cutoffTime);

    // ================================
    // CALENDAR AND AVAILABILITY QUERIES
    // ================================

    /**
     * Busca slots libres para un entrevistador en un día
     */
    @Query("""
        SELECT i.scheduledAt, i.durationMinutes 
        FROM Interview i 
        WHERE i.interviewerId = :interviewerId 
          AND i.scheduledAt BETWEEN :dayStart AND :dayEnd 
          AND i.status NOT IN ('CANCELLED', 'NO_SHOW')
        ORDER BY i.scheduledAt ASC
    """)
    List<Object[]> findInterviewerScheduleForDay(@Param("interviewerId") String interviewerId, 
                                                 @Param("dayStart") Instant dayStart, 
                                                 @Param("dayEnd") Instant dayEnd);

    /**
     * Busca próximas entrevistas para dashboard
     */
    @Query("""
        SELECT i FROM Interview i 
        WHERE i.interviewerId = :interviewerId 
          AND i.scheduledAt >= :now 
          AND i.status NOT IN ('COMPLETED', 'CANCELLED', 'NO_SHOW')
        ORDER BY i.scheduledAt ASC
    """)
    List<Interview> findUpcomingInterviewsForInterviewer(@Param("interviewerId") String interviewerId, 
                                                        @Param("now") Instant now);

    // ================================
    // CLEANUP QUERIES
    // ================================

    /**
     * Elimina entrevistas canceladas antiguas
     */
    @Query("DELETE FROM Interview i WHERE i.status = 'CANCELLED' AND i.cancelledAt < :cutoffDate")
    int deleteCancelledInterviewsBefore(@Param("cutoffDate") Instant cutoffDate);

    /**
     * Busca entrevistas sin actividad reciente para limpieza
     */
    @Query("""
        SELECT i FROM Interview i 
        WHERE i.status IN ('SCHEDULED', 'CONFIRMED') 
          AND i.scheduledAt < :staleDate
          AND i.updatedAt < :lastActivityDate
    """)
    List<Interview> findStaleInterviews(@Param("staleDate") Instant staleDate, 
                                       @Param("lastActivityDate") Instant lastActivityDate);
}