// src/main/java/com/desafios/mtn/userservice/outbox/OutboxRepository.java

package com.desafios.mtn.userservice.outbox;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OutboxRepository extends JpaRepository<OutboxEntity, UUID> {

    /**
     * Encuentra eventos no procesados ordenados por fecha de creación
     */
    @Query("SELECT o FROM OutboxEntity o WHERE o.processedAt IS NULL ORDER BY o.createdAt ASC")
    List<OutboxEntity> findUnprocessedEventsOrderByCreated();

    /**
     * Encuentra eventos no procesados con paginación
     */
    @Query("SELECT o FROM OutboxEntity o WHERE o.processedAt IS NULL ORDER BY o.createdAt ASC")
    Page<OutboxEntity> findUnprocessedEvents(Pageable pageable);

    /**
     * Cuenta eventos no procesados
     */
    @Query("SELECT COUNT(o) FROM OutboxEntity o WHERE o.processedAt IS NULL")
    long countUnprocessedEvents();

    /**
     * Encuentra evento por clave de idempotencia para verificar duplicados
     */
    Optional<OutboxEntity> findByIdempotencyKey(String idempotencyKey);

    /**
     * Verifica si existe un evento reciente con la misma clave de idempotencia
     */
    @Query("SELECT COUNT(o) > 0 FROM OutboxEntity o WHERE o.idempotencyKey = :idempotencyKey " +
           "AND o.createdAt > :since")
    boolean existsRecentByIdempotencyKey(@Param("idempotencyKey") String idempotencyKey, 
                                        @Param("since") Instant since);

    /**
     * Encuentra eventos por tipo
     */
    List<OutboxEntity> findByTypeOrderByCreatedAtAsc(String type);

    /**
     * Encuentra eventos por agregado
     */
    List<OutboxEntity> findByAggregateTypeAndAggregateIdOrderByCreatedAtAsc(
            String aggregateType, String aggregateId);

    /**
     * Marca eventos como procesados por IDs
     */
    @Modifying
    @Query("UPDATE OutboxEntity o SET o.processedAt = :processedAt WHERE o.id IN :eventIds")
    int markEventsAsProcessed(@Param("eventIds") List<UUID> eventIds, 
                             @Param("processedAt") Instant processedAt);

    /**
     * Elimina eventos procesados más antiguos que la fecha especificada
     */
    @Modifying
    @Query("DELETE FROM OutboxEntity o WHERE o.processedAt IS NOT NULL AND o.processedAt < :olderThan")
    int deleteOldProcessedEvents(@Param("olderThan") Instant olderThan);

    /**
     * Encuentra eventos procesados en un rango de tiempo
     */
    @Query("SELECT o FROM OutboxEntity o WHERE o.processedAt BETWEEN :startTime AND :endTime " +
           "ORDER BY o.processedAt DESC")
    List<OutboxEntity> findProcessedEventsInTimeRange(@Param("startTime") Instant startTime, 
                                                     @Param("endTime") Instant endTime);

    /**
     * Cuenta eventos procesados desde una fecha
     */
    @Query("SELECT COUNT(o) FROM OutboxEntity o WHERE o.processedAt IS NOT NULL AND o.processedAt > :since")
    long countProcessedEventsSince(@Param("since") Instant since);

    /**
     * Obtiene estadísticas de eventos por tipo
     */
    @Query("SELECT o.type, COUNT(o) as count FROM OutboxEntity o GROUP BY o.type ORDER BY count DESC")
    List<Object[]> getEventTypeStatistics();

    /**
     * Obtiene estadísticas de procesamiento
     */
    @Query("SELECT " +
           "COUNT(CASE WHEN o.processedAt IS NULL THEN 1 END) as unprocessed, " +
           "COUNT(CASE WHEN o.processedAt IS NOT NULL THEN 1 END) as processed " +
           "FROM OutboxEntity o")
    Object[] getProcessingStatistics();

    /**
     * Encuentra eventos antiguos no procesados (posibles fallos)
     */
    @Query("SELECT o FROM OutboxEntity o WHERE o.processedAt IS NULL AND o.createdAt < :olderThan " +
           "ORDER BY o.createdAt ASC")
    List<OutboxEntity> findOldUnprocessedEvents(@Param("olderThan") Instant olderThan);

    /**
     * Obtiene métricas de tiempo de procesamiento
     */
    @Query("SELECT " +
           "AVG(EXTRACT(EPOCH FROM (o.processedAt - o.createdAt))) as avgProcessingTimeSeconds, " +
           "MIN(EXTRACT(EPOCH FROM (o.processedAt - o.createdAt))) as minProcessingTimeSeconds, " +
           "MAX(EXTRACT(EPOCH FROM (o.processedAt - o.createdAt))) as maxProcessingTimeSeconds " +
           "FROM OutboxEntity o WHERE o.processedAt IS NOT NULL")
    Object[] getProcessingTimeMetrics();
}