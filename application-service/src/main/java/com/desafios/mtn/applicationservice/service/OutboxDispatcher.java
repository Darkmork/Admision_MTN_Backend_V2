package com.desafios.mtn.applicationservice.service;

import com.desafios.mtn.applicationservice.domain.OutboxEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Despachador de eventos del patrón Outbox
 * Procesa eventos pendientes en background de forma asíncrona
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "application.outbox.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxDispatcher {

    private final OutboxService outboxService;
    private final EventPublisher eventPublisher;

    // Configuración
    private static final int DEFAULT_BATCH_SIZE = 50;
    private static final int MAX_PROCESSING_MINUTES = 15;
    private static final int CLEANUP_DAYS = 7;

    // ================================
    // PROCESAMIENTO PRINCIPAL
    // ================================

    /**
     * Procesa eventos pendientes del Outbox
     * Se ejecuta cada 30 segundos
     */
    @Scheduled(fixedRate = 30, timeUnit = TimeUnit.SECONDS)
    public void processOutboxEvents() {
        try {
            log.debug("Starting outbox event processing");
            
            // Resetear eventos bloqueados en procesamiento
            int resetCount = outboxService.resetStaleProcessingEvents(MAX_PROCESSING_MINUTES);
            if (resetCount > 0) {
                log.warn("Reset {} stale processing events", resetCount);
            }

            // Procesar eventos críticos primero
            int criticalProcessed = processCriticalEvents();
            
            // Procesar eventos normales
            int normalProcessed = processRegularEvents();
            
            int totalProcessed = criticalProcessed + normalProcessed;
            if (totalProcessed > 0) {
                log.info("Processed {} outbox events (critical: {}, normal: {})", 
                        totalProcessed, criticalProcessed, normalProcessed);
            }
            
        } catch (Exception e) {
            log.error("Error during outbox event processing: {}", e.getMessage(), e);
        }
    }

    /**
     * Procesa eventos críticos con alta prioridad
     */
    private int processCriticalEvents() {
        List<OutboxEvent> criticalEvents = outboxService.findCriticalPendingEvents();
        if (criticalEvents.isEmpty()) {
            return 0;
        }

        log.info("Processing {} critical events", criticalEvents.size());
        
        int processedCount = 0;
        for (OutboxEvent event : criticalEvents) {
            if (processEvent(event)) {
                processedCount++;
            }
        }
        
        return processedCount;
    }

    /**
     * Procesa eventos regulares en lotes
     */
    private int processRegularEvents() {
        List<OutboxEvent> regularEvents = outboxService
            .findReadyForProcessingByPriority(OutboxEvent.EventPriority.NORMAL, DEFAULT_BATCH_SIZE);
        
        if (regularEvents.isEmpty()) {
            regularEvents = outboxService
                .findReadyForProcessingByPriority(OutboxEvent.EventPriority.LOW, DEFAULT_BATCH_SIZE);
        }
        
        if (regularEvents.isEmpty()) {
            return 0;
        }

        log.debug("Processing {} regular events", regularEvents.size());
        
        int processedCount = 0;
        for (OutboxEvent event : regularEvents) {
            if (processEvent(event)) {
                processedCount++;
            }
        }
        
        return processedCount;
    }

    /**
     * Procesa un evento individual
     */
    private boolean processEvent(OutboxEvent event) {
        try {
            // Marcar como en procesamiento
            event = outboxService.markAsProcessing(event.getId());
            
            // Publicar el evento
            eventPublisher.publishEvent(event);
            
            // Marcar como procesado
            outboxService.markAsProcessed(event.getId());
            
            log.debug("Successfully processed event: {}", event.getDebugInfo());
            return true;
            
        } catch (Exception e) {
            log.error("Failed to process event {}: {}", event.getDebugInfo(), e.getMessage(), e);
            
            // Marcar como fallido
            outboxService.markAsFailed(event.getId(), 
                e.getClass().getSimpleName(), 
                e.getMessage() != null ? e.getMessage().substring(0, Math.min(500, e.getMessage().length())) : "Unknown error");
            
            return false;
        }
    }

    // ================================
    // PROCESAMIENTO ASÍNCRONO
    // ================================

    /**
     * Procesa eventos de forma asíncrona en lotes grandes
     */
    @Async
    public CompletableFuture<Integer> processEventsBatchAsync(int batchSize) {
        log.info("Starting async batch processing with batch size: {}", batchSize);
        
        try {
            List<OutboxEvent> events = outboxService.findReadyForProcessing(batchSize);
            int processedCount = 0;
            
            for (OutboxEvent event : events) {
                if (processEvent(event)) {
                    processedCount++;
                }
            }
            
            log.info("Async batch processing completed: {} events processed", processedCount);
            return CompletableFuture.completedFuture(processedCount);
            
        } catch (Exception e) {
            log.error("Error during async batch processing: {}", e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Procesa eventos de un agregado específico de forma asíncrona
     */
    @Async
    public CompletableFuture<Integer> processAggregateEventsAsync(String aggregateType, java.util.UUID aggregateId) {
        log.info("Starting async processing for aggregate {}:{}", aggregateType, aggregateId);
        
        try {
            List<OutboxEvent> events = outboxService.findEventsByAggregate(aggregateType, aggregateId);
            int processedCount = 0;
            
            for (OutboxEvent event : events) {
                if (event.isReadyForProcessing() && processEvent(event)) {
                    processedCount++;
                }
            }
            
            log.info("Async aggregate processing completed: {} events processed for {}:{}", 
                    processedCount, aggregateType, aggregateId);
            return CompletableFuture.completedFuture(processedCount);
            
        } catch (Exception e) {
            log.error("Error during async aggregate processing: {}", e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    // ================================
    // MANTENIMIENTO Y LIMPIEZA
    // ================================

    /**
     * Limpia eventos antiguos procesados
     * Se ejecuta cada día a las 2:00 AM
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void cleanupOldEvents() {
        try {
            log.info("Starting cleanup of old outbox events");
            
            // Limpiar eventos procesados antiguos
            outboxService.cleanupOldProcessedEvents(CLEANUP_DAYS);
            
            // Limpiar eventos fallados muy antiguos (30 días)
            outboxService.cleanupOldFailedEvents(30);
            
            log.info("Completed cleanup of old outbox events");
            
        } catch (Exception e) {
            log.error("Error during outbox cleanup: {}", e.getMessage(), e);
        }
    }

    /**
     * Verifica la salud del sistema Outbox
     * Se ejecuta cada 5 minutos
     */
    @Scheduled(fixedRate = 5, timeUnit = TimeUnit.MINUTES)
    public void healthCheck() {
        try {
            OutboxService.OutboxHealthStatus health = outboxService.getHealthStatus();
            
            if (!health.isHealthy()) {
                log.warn("Outbox health check failed: {}", health.getSummary());
                
                // Resetear eventos bloqueados si hay demasiados
                if (health.staleProcessingEvents() > 0) {
                    int resetCount = outboxService.resetStaleProcessingEvents(MAX_PROCESSING_MINUTES);
                    log.info("Reset {} stale processing events during health check", resetCount);
                }
            } else {
                log.debug("Outbox health check passed: {}", health.getSummary());
            }
            
        } catch (Exception e) {
            log.error("Error during outbox health check: {}", e.getMessage(), e);
        }
    }

    // ================================
    // GESTIÓN DE EVENTOS PROBLEMÁTICOS
    // ================================

    /**
     * Reprocesa eventos fallidos
     * Se ejecuta cada hora
     */
    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.HOURS)
    public void reprocessFailedEvents() {
        try {
            log.debug("Starting failed events reprocessing");
            
            // Encontrar eventos con muchos reintentos pero que aún se pueden procesar
            List<OutboxEvent> highRetryEvents = outboxService.findHighRetryEvents(2);
            
            if (highRetryEvents.isEmpty()) {
                log.debug("No high retry events found for reprocessing");
                return;
            }
            
            int reprocessedCount = 0;
            for (OutboxEvent event : highRetryEvents) {
                if (event.shouldRetry() && processEvent(event)) {
                    reprocessedCount++;
                }
            }
            
            if (reprocessedCount > 0) {
                log.info("Reprocessed {} previously failed events", reprocessedCount);
            }
            
        } catch (Exception e) {
            log.error("Error during failed events reprocessing: {}", e.getMessage(), e);
        }
    }

    /**
     * Manejo manual de eventos específicos
     */
    public boolean reprocessEvent(java.util.UUID eventId) {
        try {
            log.info("Manual reprocessing of event: {}", eventId);
            
            var eventOpt = outboxService.findById(eventId);
            if (eventOpt.isEmpty()) {
                log.warn("Event not found for manual reprocessing: {}", eventId);
                return false;
            }
            
            OutboxEvent event = eventOpt.get();
            
            if (event.isProcessed()) {
                log.warn("Event already processed, skipping: {}", event.getDebugInfo());
                return false;
            }
            
            if (event.isPermanentlyFailed()) {
                log.warn("Event permanently failed, cannot reprocess: {}", event.getDebugInfo());
                return false;
            }
            
            return processEvent(event);
            
        } catch (Exception e) {
            log.error("Error during manual event reprocessing for {}: {}", eventId, e.getMessage(), e);
            return false;
        }
    }

    // ================================
    // ESTADÍSTICAS Y MONITOREO
    // ================================

    /**
     * Genera reporte de estadísticas del Outbox
     * Se ejecuta cada 15 minutos
     */
    @Scheduled(fixedRate = 15, timeUnit = TimeUnit.MINUTES)
    public void generateStatsReport() {
        try {
            long pendingCount = outboxService.countPendingEvents();
            long processingCount = outboxService.countProcessingEvents();
            long failedCount = outboxService.countFailedEvents();
            
            if (pendingCount > 0 || processingCount > 0 || failedCount > 0) {
                log.info("Outbox stats - Pending: {}, Processing: {}, Failed: {}", 
                        pendingCount, processingCount, failedCount);
            }
            
            // Log de advertencia si hay demasiados eventos pendientes
            if (pendingCount > 1000) {
                log.warn("High number of pending outbox events: {}", pendingCount);
            }
            
            if (failedCount > 100) {
                log.warn("High number of failed outbox events: {}", failedCount);
            }
            
        } catch (Exception e) {
            log.error("Error generating outbox stats report: {}", e.getMessage(), e);
        }
    }

    // ================================
    // MÉTODOS PÚBLICOS DE GESTIÓN
    // ================================

    /**
     * Fuerza el procesamiento inmediato de eventos pendientes
     */
    public int forceProcessPendingEvents() {
        log.info("Forcing immediate processing of pending events");
        
        try {
            // Procesar eventos críticos
            int criticalProcessed = processCriticalEvents();
            
            // Procesar eventos regulares en lote más grande
            List<OutboxEvent> events = outboxService.findReadyForProcessing(DEFAULT_BATCH_SIZE * 2);
            int regularProcessed = 0;
            
            for (OutboxEvent event : events) {
                if (processEvent(event)) {
                    regularProcessed++;
                }
            }
            
            int totalProcessed = criticalProcessed + regularProcessed;
            log.info("Forced processing completed: {} events processed", totalProcessed);
            
            return totalProcessed;
            
        } catch (Exception e) {
            log.error("Error during forced event processing: {}", e.getMessage(), e);
            return -1;
        }
    }

    /**
     * Pausa el procesamiento de eventos (para mantenimiento)
     */
    private volatile boolean processingPaused = false;
    
    public void pauseProcessing() {
        processingPaused = true;
        log.info("Outbox event processing paused");
    }
    
    public void resumeProcessing() {
        processingPaused = false;
        log.info("Outbox event processing resumed");
    }
    
    public boolean isProcessingPaused() {
        return processingPaused;
    }
}