package com.desafios.mtn.applicationservice.service;

import com.desafios.mtn.applicationservice.domain.OutboxEvent;
import com.desafios.mtn.applicationservice.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Servicio para gestión de eventos en el patrón Outbox
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OutboxService {

    private final OutboxEventRepository outboxRepository;

    // ================================
    // OPERACIONES CRUD
    // ================================

    /**
     * Crea un nuevo evento en el outbox
     */
    public OutboxEvent createEvent(String aggregateType, UUID aggregateId, String eventType,
                                 Object payload, String routingKey) {
        log.debug("Creating outbox event: {} for aggregate {}:{}", eventType, aggregateType, aggregateId);

        OutboxEvent event = OutboxEvent.builder()
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .eventType(eventType)
                .payload((java.util.Map<String, Object>) payload)
                .routingKey(routingKey)
                .exchangeName("admission.events")
                .build();

        OutboxEvent savedEvent = outboxRepository.save(event);
        log.debug("Created outbox event: {}", savedEvent.getDebugInfo());
        return savedEvent;
    }

    /**
     * Crea un evento programado
     */
    public OutboxEvent createScheduledEvent(String aggregateType, UUID aggregateId, String eventType,
                                          Object payload, String routingKey, Instant scheduledAt) {
        log.debug("Creating scheduled outbox event: {} for {} at {}", eventType, aggregateId, scheduledAt);

        OutboxEvent event = OutboxEvent.builder()
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .eventType(eventType)
                .payload((java.util.Map<String, Object>) payload)
                .routingKey(routingKey)
                .exchangeName("admission.events")
                .scheduledAt(scheduledAt)
                .build();

        OutboxEvent savedEvent = outboxRepository.save(event);
        log.debug("Created scheduled outbox event: {}", savedEvent.getDebugInfo());
        return savedEvent;
    }

    /**
     * Obtiene un evento por ID
     */
    @Transactional(readOnly = true)
    public Optional<OutboxEvent> findById(UUID eventId) {
        return outboxRepository.findById(eventId);
    }

    // ================================
    // PROCESAMIENTO DE EVENTOS
    // ================================

    /**
     * Obtiene eventos listos para procesamiento
     */
    @Transactional(readOnly = true)
    public List<OutboxEvent> findReadyForProcessing(int batchSize) {
        Pageable pageable = PageRequest.of(0, batchSize);
        return outboxRepository.findReadyForProcessing(Instant.now(), pageable);
    }

    /**
     * Obtiene eventos listos por prioridad
     */
    @Transactional(readOnly = true)
    public List<OutboxEvent> findReadyForProcessingByPriority(OutboxEvent.EventPriority priority, int batchSize) {
        Pageable pageable = PageRequest.of(0, batchSize);
        return outboxRepository.findReadyForProcessingByPriority(Instant.now(), priority, pageable);
    }

    /**
     * Marca un evento como en procesamiento
     */
    public OutboxEvent markAsProcessing(UUID eventId) {
        OutboxEvent event = outboxRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));

        if (event.isProcessed()) {
            log.warn("Attempting to process already processed event: {}", event.getDebugInfo());
            return event;
        }

        if (event.isProcessing()) {
            log.warn("Event is already being processed: {}", event.getDebugInfo());
            return event;
        }

        event.markAsProcessing();
        OutboxEvent savedEvent = outboxRepository.save(event);
        log.debug("Marked event as processing: {}", savedEvent.getDebugInfo());
        return savedEvent;
    }

    /**
     * Marca un evento como procesado exitosamente
     */
    public OutboxEvent markAsProcessed(UUID eventId) {
        OutboxEvent event = outboxRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));

        event.markAsProcessed();
        OutboxEvent savedEvent = outboxRepository.save(event);
        log.debug("Marked event as processed: {}", savedEvent.getDebugInfo());
        return savedEvent;
    }

    /**
     * Marca un evento como fallido
     */
    public OutboxEvent markAsFailed(UUID eventId, String error, String details) {
        OutboxEvent event = outboxRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));

        event.markAsFailed(error, details);
        OutboxEvent savedEvent = outboxRepository.save(event);
        
        if (savedEvent.isPermanentlyFailed()) {
            log.error("Event permanently failed after {} retries: {}", 
                     savedEvent.getRetryCount(), savedEvent.getDebugInfo());
        } else {
            log.warn("Event failed, scheduled for retry #{} at {}: {}", 
                    savedEvent.getRetryCount(), savedEvent.getScheduledAt(), savedEvent.getDebugInfo());
        }
        
        return savedEvent;
    }

    /**
     * Marca múltiples eventos como procesados en lote
     */
    public void markAsProcessedBatch(List<UUID> eventIds) {
        if (eventIds.isEmpty()) {
            return;
        }
        
        outboxRepository.markAsProcessedBatch(eventIds, Instant.now());
        log.debug("Marked {} events as processed in batch", eventIds.size());
    }

    // ================================
    // RECUPERACIÓN DE PROCESAMIENTO
    // ================================

    /**
     * Encuentra eventos que pueden estar bloqueados en procesamiento
     */
    @Transactional(readOnly = true)
    public List<OutboxEvent> findStaleProcessingEvents(int maxProcessingMinutes) {
        Instant staleCutoff = Instant.now().minusSeconds(maxProcessingMinutes * 60L);
        return outboxRepository.findStaleProcessingEvents(staleCutoff);
    }

    /**
     * Resetea eventos bloqueados en procesamiento
     */
    public int resetStaleProcessingEvents(int maxProcessingMinutes) {
        Instant staleCutoff = Instant.now().minusSeconds(maxProcessingMinutes * 60L);
        int resetCount = outboxRepository.resetStaleProcessingEvents(staleCutoff);
        
        if (resetCount > 0) {
            log.warn("Reset {} stale processing events (processing for more than {} minutes)", 
                    resetCount, maxProcessingMinutes);
        }
        
        return resetCount;
    }

    // ================================
    // CONSULTAS POR AGREGADO
    // ================================

    /**
     * Encuentra eventos de un agregado específico
     */
    @Transactional(readOnly = true)
    public List<OutboxEvent> findEventsByAggregate(String aggregateType, UUID aggregateId) {
        return outboxRepository.findByAggregateTypeAndAggregateId(aggregateType, aggregateId);
    }

    /**
     * Encuentra eventos más recientes de un agregado
     */
    @Transactional(readOnly = true)
    public List<OutboxEvent> findRecentEventsByAggregate(String aggregateType, UUID aggregateId) {
        return outboxRepository.findByAggregateTypeAndAggregateIdOrderByCreatedAtDesc(aggregateType, aggregateId);
    }

    // ================================
    // CONSULTAS POR TIPO DE EVENTO
    // ================================

    /**
     * Encuentra eventos por tipo
     */
    @Transactional(readOnly = true)
    public List<OutboxEvent> findEventsByType(String eventType) {
        return outboxRepository.findByEventType(eventType);
    }

    /**
     * Cuenta eventos por tipo
     */
    @Transactional(readOnly = true)
    public long countEventsByType(String eventType) {
        return outboxRepository.countByEventType(eventType);
    }

    // ================================
    // CONSULTAS DE IDEMPOTENCIA
    // ================================

    /**
     * Verifica si existe un evento con clave de idempotencia
     */
    @Transactional(readOnly = true)
    public boolean existsByIdempotencyKey(String idempotencyKey) {
        return outboxRepository.existsByIdempotencyKey(idempotencyKey);
    }

    /**
     * Encuentra evento por clave de idempotencia
     */
    @Transactional(readOnly = true)
    public Optional<OutboxEvent> findByIdempotencyKey(String idempotencyKey) {
        return outboxRepository.findByIdempotencyKey(idempotencyKey);
    }

    // ================================
    // ESTADÍSTICAS Y MONITOREO
    // ================================

    /**
     * Obtiene estadísticas generales de eventos
     */
    @Transactional(readOnly = true)
    public List<Object[]> getEventStatistics() {
        return outboxRepository.getEventStatistics();
    }

    /**
     * Obtiene estadísticas por tipo de agregado
     */
    @Transactional(readOnly = true)
    public List<Object[]> getAggregateTypeStatistics() {
        return outboxRepository.getAggregateTypeStatistics();
    }

    /**
     * Obtiene estadísticas por tipo de evento
     */
    @Transactional(readOnly = true)
    public List<Object[]> getEventTypeStatistics() {
        return outboxRepository.getEventTypeStatistics();
    }

    /**
     * Obtiene tiempo promedio de procesamiento
     */
    @Transactional(readOnly = true)
    public Optional<Double> getAverageProcessingTimeInMinutes() {
        return outboxRepository.getAverageProcessingTimeInMinutes();
    }

    /**
     * Cuenta eventos pendientes
     */
    @Transactional(readOnly = true)
    public long countPendingEvents() {
        return outboxRepository.countPendingEvents();
    }

    /**
     * Cuenta eventos fallados
     */
    @Transactional(readOnly = true)
    public long countFailedEvents() {
        return outboxRepository.countFailedEvents();
    }

    /**
     * Cuenta eventos siendo procesados
     */
    @Transactional(readOnly = true)
    public long countProcessingEvents() {
        return outboxRepository.countByProcessingTrue();
    }

    // ================================
    // EVENTOS PROGRAMADOS
    // ================================

    /**
     * Encuentra próximos eventos programados
     */
    @Transactional(readOnly = true)
    public List<OutboxEvent> findUpcomingScheduledEvents(int maxResults) {
        Pageable pageable = PageRequest.of(0, maxResults);
        return outboxRepository.findUpcomingScheduledEvents(Instant.now(), pageable);
    }

    /**
     * Encuentra eventos críticos pendientes
     */
    @Transactional(readOnly = true)
    public List<OutboxEvent> findCriticalPendingEvents() {
        return outboxRepository.findCriticalPendingEvents();
    }

    // ================================
    // OPERACIONES DE LIMPIEZA
    // ================================

    /**
     * Elimina eventos procesados antiguos
     */
    public void cleanupOldProcessedEvents(int daysOld) {
        Instant cutoffDate = Instant.now().minusSeconds(daysOld * 24L * 3600L);
        
        long oldEventCount = outboxRepository.countOldProcessedEvents(cutoffDate);
        if (oldEventCount == 0) {
            log.debug("No old processed events found for cleanup");
            return;
        }
        
        outboxRepository.deleteOldProcessedEvents(cutoffDate);
        log.info("Cleaned up {} old processed events (older than {} days)", oldEventCount, daysOld);
    }

    /**
     * Elimina eventos fallados antiguos
     */
    public void cleanupOldFailedEvents(int daysOld) {
        Instant cutoffDate = Instant.now().minusSeconds(daysOld * 24L * 3600L);
        outboxRepository.deleteOldFailedEvents(cutoffDate);
        log.info("Cleaned up old failed events (older than {} days)", daysOld);
    }

    // ================================
    // BÚSQUEDAS ESPECIALIZADAS
    // ================================

    /**
     * Busca eventos por texto en payload
     */
    @Transactional(readOnly = true)
    public List<OutboxEvent> searchEventsByPayload(String searchText) {
        return outboxRepository.searchByPayload(searchText);
    }

    /**
     * Encuentra eventos relacionados con una aplicación
     */
    @Transactional(readOnly = true)
    public List<OutboxEvent> findEventsRelatedToApplication(UUID applicationId) {
        return outboxRepository.findEventsRelatedToApplication(
            applicationId, applicationId.toString());
    }

    /**
     * Encuentra eventos con alta frecuencia de reintentos
     */
    @Transactional(readOnly = true)
    public List<OutboxEvent> findHighRetryEvents(int minRetries) {
        return outboxRepository.findHighRetryEvents(minRetries);
    }

    // ================================
    // MÉTODOS DE SALUD
    // ================================

    /**
     * Verifica la salud del sistema Outbox
     */
    @Transactional(readOnly = true)
    public OutboxHealthStatus getHealthStatus() {
        long pendingCount = countPendingEvents();
        long processingCount = countProcessingEvents();
        long failedCount = countFailedEvents();
        
        List<OutboxEvent> staleEvents = findStaleProcessingEvents(15); // 15 minutos
        List<OutboxEvent> criticalEvents = findCriticalPendingEvents();
        
        boolean isHealthy = staleEvents.isEmpty() && 
                           criticalEvents.isEmpty() && 
                           processingCount < 100 && 
                           failedCount < 50;
        
        return new OutboxHealthStatus(
            isHealthy,
            pendingCount,
            processingCount,
            failedCount,
            staleEvents.size(),
            criticalEvents.size()
        );
    }

    // ================================
    // INNER CLASSES
    // ================================

    public record OutboxHealthStatus(
        boolean isHealthy,
        long pendingEvents,
        long processingEvents,
        long failedEvents,
        long staleProcessingEvents,
        long criticalPendingEvents
    ) {
        public String getSummary() {
            return String.format(
                "Outbox Health: %s - Pending: %d, Processing: %d, Failed: %d, Stale: %d, Critical: %d",
                isHealthy ? "HEALTHY" : "UNHEALTHY",
                pendingEvents, processingEvents, failedEvents, staleProcessingEvents, criticalPendingEvents
            );
        }
    }
}