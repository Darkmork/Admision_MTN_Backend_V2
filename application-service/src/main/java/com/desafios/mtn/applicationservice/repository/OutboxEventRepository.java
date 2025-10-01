package com.desafios.mtn.applicationservice.repository;

import com.desafios.mtn.applicationservice.domain.OutboxEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository para OutboxEvent con operaciones especializadas para el patrón Outbox
 */
@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    // ================================
    // CONSULTAS DE PROCESAMIENTO
    // ================================

    /**
     * Encuentra eventos listos para procesamiento
     */
    @Query("SELECT e FROM OutboxEvent e WHERE e.processed = false AND e.processing = false AND e.scheduledAt <= :now AND e.retryCount < e.maxRetries ORDER BY e.priority DESC, e.createdAt ASC")
    List<OutboxEvent> findReadyForProcessing(@Param("now") Instant now, Pageable pageable);

    /**
     * Encuentra eventos listos para procesamiento por prioridad
     */
    @Query("SELECT e FROM OutboxEvent e WHERE e.processed = false AND e.processing = false AND e.scheduledAt <= :now AND e.retryCount < e.maxRetries AND e.priority = :priority ORDER BY e.createdAt ASC")
    List<OutboxEvent> findReadyForProcessingByPriority(@Param("now") Instant now, 
                                                      @Param("priority") OutboxEvent.EventPriority priority,
                                                      Pageable pageable);

    /**
     * Encuentra eventos que están siendo procesados por mucho tiempo (posible lock muerto)
     */
    @Query("SELECT e FROM OutboxEvent e WHERE e.processing = true AND e.lastRetryAt < :staleCutoff")
    List<OutboxEvent> findStaleProcessingEvents(@Param("staleCutoff") Instant staleCutoff);

    /**
     * Encuentra eventos fallados permanentemente
     */
    List<OutboxEvent> findByProcessedFalseAndRetryCountGreaterThanEqual(Integer maxRetries);

    /**
     * Encuentra eventos por estado de procesamiento
     */
    List<OutboxEvent> findByProcessedAndProcessing(Boolean processed, Boolean processing);

    // ================================
    // CONSULTAS POR AGREGADO
    // ================================

    /**
     * Encuentra eventos por tipo de agregado
     */
    List<OutboxEvent> findByAggregateType(String aggregateType);

    /**
     * Encuentra eventos por ID de agregado
     */
    List<OutboxEvent> findByAggregateId(UUID aggregateId);

    /**
     * Encuentra eventos de un agregado específico
     */
    List<OutboxEvent> findByAggregateTypeAndAggregateId(String aggregateType, UUID aggregateId);

    /**
     * Encuentra eventos más recientes de un agregado
     */
    List<OutboxEvent> findByAggregateTypeAndAggregateIdOrderByCreatedAtDesc(String aggregateType, UUID aggregateId);

    // ================================
    // CONSULTAS POR TIPO DE EVENTO
    // ================================

    /**
     * Encuentra eventos por tipo
     */
    List<OutboxEvent> findByEventType(String eventType);

    /**
     * Encuentra eventos por múltiples tipos
     */
    List<OutboxEvent> findByEventTypeIn(List<String> eventTypes);

    /**
     * Cuenta eventos por tipo
     */
    long countByEventType(String eventType);

    // ================================
    // CONSULTAS TEMPORALES
    // ================================

    /**
     * Encuentra eventos creados en un rango de fechas
     */
    List<OutboxEvent> findByCreatedAtBetween(Instant startDate, Instant endDate);

    /**
     * Encuentra eventos programados antes de una fecha
     */
    List<OutboxEvent> findByScheduledAtBefore(Instant scheduledAt);

    /**
     * Encuentra eventos procesados en un rango de fechas
     */
    List<OutboxEvent> findByProcessedAtBetween(Instant startDate, Instant endDate);

    /**
     * Encuentra eventos antiguos procesados (para limpieza)
     */
    @Query("SELECT e FROM OutboxEvent e WHERE e.processed = true AND e.processedAt < :cutoffDate ORDER BY e.processedAt ASC")
    List<OutboxEvent> findOldProcessedEvents(@Param("cutoffDate") Instant cutoffDate, Pageable pageable);

    // ================================
    // CONSULTAS POR IDEMPOTENCIA
    // ================================

    /**
     * Encuentra evento por clave de idempotencia
     */
    Optional<OutboxEvent> findByIdempotencyKey(String idempotencyKey);

    /**
     * Verifica si existe un evento con clave de idempotencia
     */
    boolean existsByIdempotencyKey(String idempotencyKey);

    /**
     * Encuentra eventos duplicados por clave de idempotencia
     */
    @Query("SELECT e.idempotencyKey, COUNT(e) FROM OutboxEvent e WHERE e.idempotencyKey IS NOT NULL GROUP BY e.idempotencyKey HAVING COUNT(e) > 1")
    List<Object[]> findDuplicateIdempotencyKeys();

    // ================================
    // CONSULTAS DE CORRELACIÓN
    // ================================

    /**
     * Encuentra eventos por ID de correlación
     */
    List<OutboxEvent> findByCorrelationId(String correlationId);

    /**
     * Encuentra eventos por ID de causalidad
     */
    List<OutboxEvent> findByCausationId(String causationId);

    /**
     * Encuentra cadena de eventos relacionados
     */
    @Query("SELECT e FROM OutboxEvent e WHERE e.correlationId = :correlationId OR e.causationId = :correlationId ORDER BY e.createdAt ASC")
    List<OutboxEvent> findEventChain(@Param("correlationId") String correlationId);

    // ================================
    // ESTADÍSTICAS Y MONITOREO
    // ================================

    /**
     * Cuenta eventos por estado
     */
    @Query("SELECT 'processed', COUNT(e) FROM OutboxEvent e WHERE e.processed = true UNION ALL SELECT 'pending', COUNT(e) FROM OutboxEvent e WHERE e.processed = false AND e.retryCount < e.maxRetries UNION ALL SELECT 'failed', COUNT(e) FROM OutboxEvent e WHERE e.processed = false AND e.retryCount >= e.maxRetries")
    List<Object[]> getEventStatistics();

    /**
     * Cuenta eventos por tipo de agregado
     */
    @Query("SELECT e.aggregateType, COUNT(e) FROM OutboxEvent e GROUP BY e.aggregateType ORDER BY COUNT(e) DESC")
    List<Object[]> getAggregateTypeStatistics();

    /**
     * Cuenta eventos por tipo de evento
     */
    @Query("SELECT e.eventType, COUNT(e) FROM OutboxEvent e GROUP BY e.eventType ORDER BY COUNT(e) DESC")
    List<Object[]> getEventTypeStatistics();

    /**
     * Cuenta eventos por prioridad
     */
    @Query("SELECT e.priority, COUNT(e) FROM OutboxEvent e GROUP BY e.priority")
    List<Object[]> getPriorityStatistics();

    /**
     * Obtiene estadísticas de reintentos
     */
    @Query("SELECT e.retryCount, COUNT(e) FROM OutboxEvent e WHERE e.retryCount > 0 GROUP BY e.retryCount ORDER BY e.retryCount")
    List<Object[]> getRetryStatistics();

    /**
     * Calcula tiempo promedio de procesamiento
     */
    @Query("SELECT AVG(EXTRACT(EPOCH FROM (e.processedAt - e.createdAt))/60) FROM OutboxEvent e WHERE e.processedAt IS NOT NULL")
    Optional<Double> getAverageProcessingTimeInMinutes();

    // ================================
    // CONSULTAS DE RENDIMIENTO
    // ================================

    /**
     * Cuenta eventos pendientes
     */
    @Query("SELECT COUNT(e) FROM OutboxEvent e WHERE e.processed = false AND e.retryCount < e.maxRetries")
    long countPendingEvents();

    /**
     * Cuenta eventos fallados
     */
    @Query("SELECT COUNT(e) FROM OutboxEvent e WHERE e.processed = false AND e.retryCount >= e.maxRetries")
    long countFailedEvents();

    /**
     * Cuenta eventos siendo procesados
     */
    long countByProcessingTrue();

    /**
     * Encuentra eventos con alta frecuencia de reintentos
     */
    @Query("SELECT e FROM OutboxEvent e WHERE e.retryCount >= :threshold ORDER BY e.retryCount DESC")
    List<OutboxEvent> findHighRetryEvents(@Param("threshold") int threshold);

    // ================================
    // OPERACIONES DE BATCH
    // ================================

    /**
     * Marca eventos como procesados en batch
     */
    @Modifying
    @Transactional
    @Query("UPDATE OutboxEvent e SET e.processed = true, e.processing = false, e.processedAt = :now WHERE e.id IN :ids")
    void markAsProcessedBatch(@Param("ids") List<UUID> ids, @Param("now") Instant now);

    /**
     * Resetea estado de procesamiento para eventos stale
     */
    @Modifying
    @Transactional
    @Query("UPDATE OutboxEvent e SET e.processing = false WHERE e.processing = true AND e.lastRetryAt < :staleCutoff")
    int resetStaleProcessingEvents(@Param("staleCutoff") Instant staleCutoff);

    /**
     * Incrementa contador de reintentos
     */
    @Modifying
    @Transactional
    @Query("UPDATE OutboxEvent e SET e.retryCount = e.retryCount + 1, e.processing = false, e.lastRetryAt = :now, e.scheduledAt = :nextRetryAt, e.lastError = :error WHERE e.id = :id")
    void incrementRetryCount(@Param("id") UUID id, @Param("now") Instant now, 
                           @Param("nextRetryAt") Instant nextRetryAt, @Param("error") String error);

    // ================================
    // OPERACIONES DE LIMPIEZA
    // ================================

    /**
     * Elimina eventos procesados antiguos
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM OutboxEvent e WHERE e.processed = true AND e.processedAt < :cutoffDate")
    void deleteOldProcessedEvents(@Param("cutoffDate") Instant cutoffDate);

    /**
     * Cuenta eventos antiguos procesados
     */
    @Query("SELECT COUNT(e) FROM OutboxEvent e WHERE e.processed = true AND e.processedAt < :cutoffDate")
    long countOldProcessedEvents(@Param("cutoffDate") Instant cutoffDate);

    /**
     * Elimina eventos fallados antiguos
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM OutboxEvent e WHERE e.processed = false AND e.retryCount >= e.maxRetries AND e.createdAt < :cutoffDate")
    void deleteOldFailedEvents(@Param("cutoffDate") Instant cutoffDate);

    // ================================
    // CONSULTAS ESPECIALIZADAS
    // ================================

    /**
     * Encuentra próximos eventos programados
     */
    @Query("SELECT e FROM OutboxEvent e WHERE e.processed = false AND e.processing = false AND e.scheduledAt > :now AND e.retryCount < e.maxRetries ORDER BY e.scheduledAt ASC")
    List<OutboxEvent> findUpcomingScheduledEvents(@Param("now") Instant now, Pageable pageable);

    /**
     * Encuentra eventos críticos pendientes
     */
    @Query("SELECT e FROM OutboxEvent e WHERE e.processed = false AND e.priority = 'CRITICAL' AND e.retryCount < e.maxRetries ORDER BY e.createdAt ASC")
    List<OutboxEvent> findCriticalPendingEvents();

    /**
     * Busca eventos por texto en payload
     */
    @Query(value = "SELECT * FROM outbox WHERE payload::text ILIKE %:searchText%", nativeQuery = true)
    List<OutboxEvent> searchByPayload(@Param("searchText") String searchText);

    /**
     * Encuentra eventos relacionados con una aplicación específica
     */
    @Query(value = "SELECT * FROM outbox WHERE aggregate_id = :applicationId OR payload::text LIKE %:applicationIdStr%", nativeQuery = true)
    List<OutboxEvent> findEventsRelatedToApplication(@Param("applicationId") UUID applicationId, 
                                                   @Param("applicationIdStr") String applicationIdStr);
}