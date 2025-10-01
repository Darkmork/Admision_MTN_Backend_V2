package com.desafios.mtn.inbox.service;

import com.desafios.mtn.inbox.domain.InboxEvent;
import com.desafios.mtn.inbox.domain.InboxEvent.ProcessingStatus;
import com.desafios.mtn.inbox.repository.InboxEventRepository;
import com.desafios.mtn.inbox.handler.EventHandler;
import com.desafios.mtn.inbox.handler.EventHandlerRegistry;
import com.desafios.mtn.inbox.exception.InboxException;
import com.desafios.mtn.inbox.exception.DuplicateEventException;
import com.desafios.mtn.inbox.exception.HandlerNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Procesador principal del Inbox que garantiza procesamiento idempotente
 * Maneja recepción, deduplicación, procesamiento y reintentos
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InboxEventProcessor {

    private final InboxEventRepository inboxEventRepository;
    private final EventHandlerRegistry handlerRegistry;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    // ================================
    // PROCESAMIENTO DE EVENTOS
    // ================================

    /**
     * Procesa un evento entrante de forma idempotente
     */
    @Transactional
    public ProcessingResult processIncomingEvent(IncomingEvent incomingEvent) {
        log.debug("Processing incoming event: {} ({})", 
                 incomingEvent.getEventType(), incomingEvent.getEventId());

        try {
            // 1. Verificar si el evento ya existe (idempotencia)
            Optional<InboxEvent> existingEvent = 
                inboxEventRepository.findByEventId(incomingEvent.getEventId());
            
            if (existingEvent.isPresent()) {
                return handleDuplicateEvent(existingEvent.get(), incomingEvent);
            }

            // 2. Crear nuevo registro en el inbox
            InboxEvent inboxEvent = createInboxEvent(incomingEvent);
            inboxEvent = inboxEventRepository.save(inboxEvent);

            // 3. Encontrar y ejecutar handler apropiado
            EventHandler<?> handler = handlerRegistry.getHandler(incomingEvent.getEventType());
            
            if (handler == null) {
                inboxEvent.markSkipped("No handler found for event type: " + incomingEvent.getEventType());
                inboxEventRepository.save(inboxEvent);
                
                log.warn("No handler found for event type: {}", incomingEvent.getEventType());
                return ProcessingResult.skipped("No handler available");
            }

            // 4. Procesar el evento
            return processEventWithHandler(inboxEvent, handler, incomingEvent);

        } catch (Exception e) {
            log.error("Error processing incoming event {}: {}", 
                     incomingEvent.getEventId(), e.getMessage(), e);
            return ProcessingResult.failed("Processing error: " + e.getMessage());
        }
    }

    /**
     * Procesa eventos pendientes de reintento
     */
    @Scheduled(fixedDelay = 30000) // Cada 30 segundos
    @Transactional
    public void processRetryableEvents() {
        List<InboxEvent> retryableEvents = 
            inboxEventRepository.findRetryableEvents(Instant.now());
        
        if (!retryableEvents.isEmpty()) {
            log.info("Processing {} retryable events", retryableEvents.size());
            
            for (InboxEvent event : retryableEvents) {
                try {
                    retryEventProcessing(event);
                } catch (Exception e) {
                    log.error("Error retrying event {}: {}", event.getEventId(), e.getMessage());
                }
            }
        }
    }

    /**
     * Limpia eventos antiguos ya procesados
     */
    @Scheduled(fixedRate = 3600000) // Cada hora
    @Transactional
    public void cleanupOldEvents() {
        Instant cutoffTime = Instant.now().minusSeconds(7 * 24 * 3600); // 7 días
        
        List<ProcessingStatus> completedStatuses = List.of(
            ProcessingStatus.COMPLETED,
            ProcessingStatus.SKIPPED,
            ProcessingStatus.DUPLICATE_DETECTED
        );
        
        int deletedCount = inboxEventRepository.deleteOldEvents(cutoffTime, completedStatuses);
        
        if (deletedCount > 0) {
            log.info("Cleaned up {} old inbox events", deletedCount);
        }
    }

    // ================================
    // PROCESAMIENTO INTERNO
    // ================================

    private ProcessingResult handleDuplicateEvent(InboxEvent existingEvent, IncomingEvent incomingEvent) {
        log.debug("Duplicate event detected: {} - Status: {}", 
                 incomingEvent.getEventId(), existingEvent.getStatus());
        
        switch (existingEvent.getStatus()) {
            case COMPLETED:
                return ProcessingResult.duplicate("Already processed successfully");
                
            case PROCESSING:
                return ProcessingResult.duplicate("Currently being processed");
                
            case FAILED:
                // Reintento manual de evento fallado
                log.info("Retrying failed event: {}", incomingEvent.getEventId());
                return retryFailedEvent(existingEvent, incomingEvent);
                
            case RETRY_SCHEDULED:
                return ProcessingResult.duplicate("Scheduled for retry");
                
            default:
                existingEvent.markDuplicateDetected();
                inboxEventRepository.save(existingEvent);
                return ProcessingResult.duplicate("Duplicate detected");
        }
    }

    private ProcessingResult processEventWithHandler(
            InboxEvent inboxEvent, 
            EventHandler<?> handler, 
            IncomingEvent incomingEvent) {
        
        inboxEvent.startProcessing(handler.getClass().getSimpleName());
        inboxEventRepository.save(inboxEvent);

        try {
            // Verificación adicional de idempotencia usando el servicio especializado
            String idempotencyKey = idempotencyService.generateKey(incomingEvent);
            
            if (idempotencyService.isAlreadyProcessed(idempotencyKey)) {
                inboxEvent.markDuplicateDetected();
                inboxEventRepository.save(inboxEvent);
                
                log.warn("Event already processed based on idempotency key: {}", idempotencyKey);
                return ProcessingResult.duplicate("Already processed (idempotency check)");
            }

            // Procesar el evento
            Object eventData = parseEventData(incomingEvent.getPayload(), handler.getEventDataType());
            Object result = handler.handle(eventData, incomingEvent.toEventContext());

            // Marcar como procesado con éxito
            String resultJson = objectMapper.writeValueAsString(result);
            inboxEvent.completeSuccessfully(resultJson);
            inboxEventRepository.save(inboxEvent);

            // Registrar en el servicio de idempotencia
            idempotencyService.markAsProcessed(idempotencyKey, resultJson);

            log.info("Successfully processed event: {} in {}ms", 
                    incomingEvent.getEventId(), inboxEvent.getProcessingDuration());
            
            return ProcessingResult.success(result);

        } catch (Exception e) {
            // Marcar como fallado
            inboxEvent.markFailed(e.getMessage(), getStackTrace(e));
            inboxEventRepository.save(inboxEvent);

            log.error("Failed to process event {}: {}", 
                     incomingEvent.getEventId(), e.getMessage(), e);
            
            return ProcessingResult.failed("Handler execution failed: " + e.getMessage());
        }
    }

    private ProcessingResult retryEventProcessing(InboxEvent inboxEvent) {
        try {
            EventHandler<?> handler = handlerRegistry.getHandler(inboxEvent.getEventType());
            
            if (handler == null) {
                throw new HandlerNotFoundException("Handler not found for: " + inboxEvent.getEventType());
            }

            IncomingEvent retryEvent = IncomingEvent.fromInboxEvent(inboxEvent);
            return processEventWithHandler(inboxEvent, handler, retryEvent);

        } catch (Exception e) {
            inboxEvent.markFailed(e.getMessage(), getStackTrace(e));
            inboxEventRepository.save(inboxEvent);
            
            return ProcessingResult.failed("Retry failed: " + e.getMessage());
        }
    }

    private ProcessingResult retryFailedEvent(InboxEvent existingEvent, IncomingEvent incomingEvent) {
        if (!existingEvent.canBeRetried()) {
            return ProcessingResult.failed("Event exceeded maximum retry attempts");
        }
        
        existingEvent.scheduleRetry();
        inboxEventRepository.save(existingEvent);
        
        return ProcessingResult.scheduled("Scheduled for retry");
    }

    private InboxEvent createInboxEvent(IncomingEvent incomingEvent) {
        return InboxEvent.createNew(
            incomingEvent.getEventId(),
            incomingEvent.getEventType(),
            incomingEvent.getEventVersion(),
            incomingEvent.getCorrelationId(),
            incomingEvent.getCausationId(),
            incomingEvent.getPayload(),
            incomingEvent.getSourceService(),
            incomingEvent.getTargetService(),
            incomingEvent.getEventTimestamp()
        );
    }

    private Object parseEventData(String payload, Class<?> eventDataType) {
        try {
            return objectMapper.readValue(payload, eventDataType);
        } catch (Exception e) {
            throw new InboxException("Failed to parse event data: " + e.getMessage(), e);
        }
    }

    private String getStackTrace(Throwable throwable) {
        java.io.StringWriter sw = new java.io.StringWriter();
        throwable.printStackTrace(new java.io.PrintWriter(sw));
        return sw.toString();
    }

    // ================================
    // CONSULTAS Y ESTADÍSTICAS
    // ================================

    public InboxStatistics getStatistics() {
        return InboxStatistics.builder()
            .totalEvents(inboxEventRepository.count())
            .completedEvents(inboxEventRepository.countByStatus(ProcessingStatus.COMPLETED))
            .failedEvents(inboxEventRepository.countByStatus(ProcessingStatus.FAILED))
            .retryScheduledEvents(inboxEventRepository.countByStatus(ProcessingStatus.RETRY_SCHEDULED))
            .duplicateEvents(inboxEventRepository.countByStatus(ProcessingStatus.DUPLICATE_DETECTED))
            .averageProcessingTime(inboxEventRepository.getAverageProcessingTime())
            .build();
    }

    public List<InboxEvent> getEventsByStatus(ProcessingStatus status, int limit) {
        return inboxEventRepository.findByStatusOrderByReceivedAtDesc(status, 
            org.springframework.data.domain.PageRequest.of(0, limit)).getContent();
    }

    public Optional<InboxEvent> getEventById(String eventId) {
        return inboxEventRepository.findByEventId(eventId);
    }

    // ================================
    // RESULT CLASSES
    // ================================

    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class ProcessingResult {
        private ProcessingResultStatus status;
        private String message;
        private Object result;
        private Instant processedAt;

        public static ProcessingResult success(Object result) {
            return ProcessingResult.builder()
                .status(ProcessingResultStatus.SUCCESS)
                .result(result)
                .processedAt(Instant.now())
                .build();
        }

        public static ProcessingResult failed(String message) {
            return ProcessingResult.builder()
                .status(ProcessingResultStatus.FAILED)
                .message(message)
                .processedAt(Instant.now())
                .build();
        }

        public static ProcessingResult duplicate(String message) {
            return ProcessingResult.builder()
                .status(ProcessingResultStatus.DUPLICATE)
                .message(message)
                .processedAt(Instant.now())
                .build();
        }

        public static ProcessingResult skipped(String message) {
            return ProcessingResult.builder()
                .status(ProcessingResultStatus.SKIPPED)
                .message(message)
                .processedAt(Instant.now())
                .build();
        }

        public static ProcessingResult scheduled(String message) {
            return ProcessingResult.builder()
                .status(ProcessingResultStatus.SCHEDULED)
                .message(message)
                .processedAt(Instant.now())
                .build();
        }
    }

    public enum ProcessingResultStatus {
        SUCCESS, FAILED, DUPLICATE, SKIPPED, SCHEDULED
    }

    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class InboxStatistics {
        private long totalEvents;
        private long completedEvents;
        private long failedEvents;
        private long retryScheduledEvents;
        private long duplicateEvents;
        private double averageProcessingTime;
        private double successRate;
        private double duplicationRate;

        @lombok.Builder.Default
        private Instant generatedAt = Instant.now();

        public double getSuccessRate() {
            return totalEvents > 0 ? (double) completedEvents / totalEvents * 100 : 0.0;
        }

        public double getDuplicationRate() {
            return totalEvents > 0 ? (double) duplicateEvents / totalEvents * 100 : 0.0;
        }
    }
}