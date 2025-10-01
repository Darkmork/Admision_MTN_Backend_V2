// user-service/src/main/java/com/desafios/mtn/userservice/repository/DomainEventRepository.java

package com.desafios.mtn.userservice.repository;

import com.desafios.mtn.userservice.event.DomainEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface DomainEventRepository extends JpaRepository<DomainEvent, UUID> {

    /**
     * Encuentra eventos no procesados ordenados por fecha de ocurrencia
     */
    @Query("SELECT e FROM DomainEvent e WHERE e.processed = false ORDER BY e.occurredAt ASC")
    List<DomainEvent> findUnprocessedEvents();

    /**
     * Encuentra eventos no procesados con límite para procesamiento por lotes
     */
    @Query("SELECT e FROM DomainEvent e WHERE e.processed = false ORDER BY e.occurredAt ASC")
    Page<DomainEvent> findUnprocessedEvents(Pageable pageable);

    /**
     * Encuentra eventos por tipo de agregado
     */
    @Query("SELECT e FROM DomainEvent e WHERE e.aggregateType = :aggregateType ORDER BY e.occurredAt DESC")
    List<DomainEvent> findByAggregateType(@Param("aggregateType") String aggregateType);

    /**
     * Encuentra eventos por ID de agregado
     */
    @Query("SELECT e FROM DomainEvent e WHERE e.aggregateId = :aggregateId ORDER BY e.occurredAt DESC")
    List<DomainEvent> findByAggregateId(@Param("aggregateId") UUID aggregateId);

    /**
     * Encuentra eventos por tipo de evento
     */
    @Query("SELECT e FROM DomainEvent e WHERE e.eventType = :eventType ORDER BY e.occurredAt DESC")
    List<DomainEvent> findByEventType(@Param("eventType") String eventType);

    /**
     * Encuentra eventos por correlation ID
     */
    @Query("SELECT e FROM DomainEvent e WHERE e.correlationId = :correlationId ORDER BY e.occurredAt ASC")
    List<DomainEvent> findByCorrelationId(@Param("correlationId") UUID correlationId);

    /**
     * Encuentra eventos por usuario
     */
    @Query("SELECT e FROM DomainEvent e WHERE e.userId = :userId ORDER BY e.occurredAt DESC")
    List<DomainEvent> findByUserId(@Param("userId") UUID userId);

    /**
     * Encuentra eventos en un rango de fechas
     */
    @Query("SELECT e FROM DomainEvent e WHERE e.occurredAt BETWEEN :startTime AND :endTime ORDER BY e.occurredAt DESC")
    List<DomainEvent> findEventsInTimeRange(@Param("startTime") Instant startTime, @Param("endTime") Instant endTime);

    /**
     * Encuentra eventos expirados (más de 24 horas sin procesar)
     */
    @Query("SELECT e FROM DomainEvent e WHERE e.processed = false AND e.occurredAt < :expiredBefore")
    List<DomainEvent> findExpiredEvents(@Param("expiredBefore") Instant expiredBefore);

    /**
     * Cuenta eventos no procesados
     */
    @Query("SELECT COUNT(e) FROM DomainEvent e WHERE e.processed = false")
    long countUnprocessedEvents();

    /**
     * Cuenta eventos por tipo de agregado
     */
    @Query("SELECT COUNT(e) FROM DomainEvent e WHERE e.aggregateType = :aggregateType")
    long countByAggregateType(@Param("aggregateType") String aggregateType);

    /**
     * Cuenta eventos procesados en las últimas N horas
     */
    @Query("SELECT COUNT(e) FROM DomainEvent e WHERE e.processed = true AND e.processedAt > :since")
    long countProcessedEventsSince(@Param("since") Instant since);

    /**
     * Marca eventos como procesados por IDs
     */
    @Modifying
    @Query("UPDATE DomainEvent e SET e.processed = true, e.processedAt = :processedAt WHERE e.id IN :eventIds")
    int markEventsAsProcessed(@Param("eventIds") List<UUID> eventIds, @Param("processedAt") Instant processedAt);

    /**
     * Elimina eventos procesados más antiguos que la fecha especificada
     */
    @Modifying
    @Query("DELETE FROM DomainEvent e WHERE e.processed = true AND e.processedAt < :olderThan")
    int deleteOldProcessedEvents(@Param("olderThan") Instant olderThan);

    /**
     * Elimina eventos expirados no procesados
     */
    @Modifying
    @Query("DELETE FROM DomainEvent e WHERE e.processed = false AND e.occurredAt < :expiredBefore")
    int deleteExpiredEvents(@Param("expiredBefore") Instant expiredBefore);

    /**
     * Obtiene estadísticas de eventos por tipo
     */
    @Query("SELECT e.eventType, COUNT(e) as count FROM DomainEvent e GROUP BY e.eventType ORDER BY count DESC")
    List<Object[]> getEventTypeStatistics();

    /**
     * Obtiene estadísticas de eventos por estado de procesamiento
     */
    @Query("SELECT e.processed, COUNT(e) as count FROM DomainEvent e GROUP BY e.processed")
    List<Object[]> getProcessingStatistics();

    /**
     * Encuentra eventos relacionados (mismo correlation ID o causation chain)
     */
    @Query("SELECT e FROM DomainEvent e WHERE e.correlationId = :correlationId OR " +
           "e.causationId IN (SELECT e2.id FROM DomainEvent e2 WHERE e2.correlationId = :correlationId) " +
           "ORDER BY e.occurredAt ASC")
    List<DomainEvent> findRelatedEvents(@Param("correlationId") UUID correlationId);

    /**
     * Encuentra el último evento para un agregado específico
     */
    @Query("SELECT e FROM DomainEvent e WHERE e.aggregateId = :aggregateId ORDER BY e.occurredAt DESC LIMIT 1")
    DomainEvent findLatestEventForAggregate(@Param("aggregateId") UUID aggregateId);

    /**
     * Verifica si existe un evento específico para prevenir duplicados
     */
    @Query("SELECT COUNT(e) > 0 FROM DomainEvent e WHERE e.aggregateId = :aggregateId AND " +
           "e.eventType = :eventType AND e.occurredAt > :since")
    boolean existsRecentEvent(@Param("aggregateId") UUID aggregateId, 
                             @Param("eventType") String eventType, 
                             @Param("since") Instant since);

    /**
     * Encuentra eventos que necesitan reintento (fallidos pero no expirados)
     */
    @Query("SELECT e FROM DomainEvent e WHERE e.processed = false AND " +
           "e.eventType LIKE '%_RETRY' AND e.occurredAt > :notExpiredBefore " +
           "ORDER BY e.occurredAt ASC")
    List<DomainEvent> findEventsForRetry(@Param("notExpiredBefore") Instant notExpiredBefore);

    /**
     * Obtiene métricas de rendimiento de procesamiento
     */
    @Query("SELECT AVG(EXTRACT(EPOCH FROM (e.processedAt - e.occurredAt))) as avgProcessingTimeSeconds, " +
           "MIN(EXTRACT(EPOCH FROM (e.processedAt - e.occurredAt))) as minProcessingTimeSeconds, " +
           "MAX(EXTRACT(EPOCH FROM (e.processedAt - e.occurredAt))) as maxProcessingTimeSeconds " +
           "FROM DomainEvent e WHERE e.processed = true AND e.processedAt IS NOT NULL")
    Object[] getProcessingPerformanceMetrics();
}