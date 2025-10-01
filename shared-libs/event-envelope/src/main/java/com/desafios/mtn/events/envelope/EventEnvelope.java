package com.desafios.mtn.events.envelope;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.InstantDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Envelope estandarizado para todos los eventos en el sistema MTN
 * Garantiza estructura consistente y trazabilidad completa
 * 
 * @param <T> Tipo del payload del evento
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EventEnvelope<T> {

    // ================================
    // IDENTIFICACIÓN DEL EVENTO
    // ================================

    /**
     * ID único del evento - UUID v4
     */
    @NotBlank(message = "Event ID is required")
    private String eventId;

    /**
     * Tipo de evento - debe seguir convención PascalCase
     */
    @NotBlank(message = "Event type is required")
    private String eventType;

    /**
     * Versión del esquema del evento - formato semántico x.y.z
     */
    @NotBlank(message = "Event version is required")
    @Pattern(regexp = "^\\d+\\.\\d+\\.\\d+$", message = "Event version must follow semantic versioning (x.y.z)")
    private String eventVersion;

    // ================================
    // TIMESTAMPS Y ORIGEN
    // ================================

    /**
     * Timestamp cuando ocurrió el evento
     */
    @NotNull(message = "Event timestamp is required")
    @JsonSerialize(using = InstantSerializer.class)
    @JsonDeserialize(using = InstantDeserializer.class)
    private Instant timestamp;

    /**
     * Servicio que originó el evento
     */
    @NotBlank(message = "Source service is required")
    private String source;

    /**
     * Versión del servicio que generó el evento
     */
    private String sourceVersion;

    // ================================
    // TRAZABILIDAD DISTRIBUIDA
    // ================================

    /**
     * ID de correlación para seguimiento de transacciones distribuidas
     */
    private String correlationId;

    /**
     * ID del evento/comando que causó este evento
     */
    private String causationId;

    /**
     * ID único de la sesión de usuario (si aplica)
     */
    private String sessionId;

    /**
     * ID de tracing distribuido (OpenTelemetry/Jaeger)
     */
    private String traceId;

    /**
     * ID de span para tracing granular
     */
    private String spanId;

    // ================================
    // DATOS DEL EVENTO
    // ================================

    /**
     * Payload principal del evento
     */
    @NotNull(message = "Event data is required")
    @Valid
    private T data;

    /**
     * Metadatos adicionales del evento
     */
    private EventMetadata metadata;

    // ================================
    // INFORMACIÓN DE CONTEXTO
    // ================================

    /**
     * Contexto del usuario que generó el evento
     */
    private UserContext userContext;

    /**
     * Contexto técnico del sistema
     */
    private SystemContext systemContext;

    // ================================
    // FACTORY METHODS
    // ================================

    /**
     * Crea un nuevo evento con estructura completa
     */
    public static <T> EventEnvelope<T> create(
            String eventType,
            String eventVersion,
            T data,
            String source) {
        
        return EventEnvelope.<T>builder()
            .eventId(generateEventId())
            .eventType(eventType)
            .eventVersion(eventVersion)
            .timestamp(Instant.now())
            .source(source)
            .data(data)
            .correlationId(generateCorrelationId())
            .metadata(EventMetadata.createDefault())
            .systemContext(SystemContext.current())
            .build();
    }

    /**
     * Crea un evento como respuesta a otro evento (preserva correlación)
     */
    public static <T> EventEnvelope<T> createResponse(
            String eventType,
            String eventVersion,
            T data,
            String source,
            EventEnvelope<?> originalEvent) {
        
        return EventEnvelope.<T>builder()
            .eventId(generateEventId())
            .eventType(eventType)
            .eventVersion(eventVersion)
            .timestamp(Instant.now())
            .source(source)
            .data(data)
            .correlationId(originalEvent.getCorrelationId())
            .causationId(originalEvent.getEventId())
            .sessionId(originalEvent.getSessionId())
            .traceId(originalEvent.getTraceId())
            .metadata(EventMetadata.createDefault())
            .userContext(originalEvent.getUserContext())
            .systemContext(SystemContext.current())
            .build();
    }

    /**
     * Crea un evento enriquecido con contexto completo
     */
    public static <T> EventEnvelope<T> createEnriched(
            String eventType,
            String eventVersion,
            T data,
            String source,
            String correlationId,
            UserContext userContext) {
        
        return EventEnvelope.<T>builder()
            .eventId(generateEventId())
            .eventType(eventType)
            .eventVersion(eventVersion)
            .timestamp(Instant.now())
            .source(source)
            .data(data)
            .correlationId(correlationId)
            .metadata(EventMetadata.createDefault())
            .userContext(userContext)
            .systemContext(SystemContext.current())
            .build();
    }

    // ================================
    // UTILITY METHODS
    // ================================

    /**
     * Verifica si el evento es del tipo especificado
     */
    public boolean isEventType(String eventType) {
        return this.eventType != null && this.eventType.equals(eventType);
    }

    /**
     * Verifica si el evento proviene del servicio especificado
     */
    public boolean isFromSource(String source) {
        return this.source != null && this.source.equals(source);
    }

    /**
     * Verifica si el evento tiene correlación
     */
    public boolean hasCorrelation() {
        return correlationId != null && !correlationId.isBlank();
    }

    /**
     * Verifica si el evento tiene causación (es respuesta a otro evento)
     */
    public boolean hasCausation() {
        return causationId != null && !causationId.isBlank();
    }

    /**
     * Calcula la edad del evento en milisegundos
     */
    public long getAgeInMillis() {
        return java.time.Duration.between(timestamp, Instant.now()).toMillis();
    }

    /**
     * Verifica si el evento es antiguo (más de x minutos)
     */
    public boolean isStale(int maxAgeMinutes) {
        return getAgeInMillis() > (maxAgeMinutes * 60 * 1000L);
    }

    /**
     * Crea una copia con nuevos datos pero preservando metadatos
     */
    public <U> EventEnvelope<U> withNewData(U newData) {
        return EventEnvelope.<U>builder()
            .eventId(this.eventId)
            .eventType(this.eventType)
            .eventVersion(this.eventVersion)
            .timestamp(this.timestamp)
            .source(this.source)
            .sourceVersion(this.sourceVersion)
            .correlationId(this.correlationId)
            .causationId(this.causationId)
            .sessionId(this.sessionId)
            .traceId(this.traceId)
            .spanId(this.spanId)
            .data(newData)
            .metadata(this.metadata)
            .userContext(this.userContext)
            .systemContext(this.systemContext)
            .build();
    }

    /**
     * Enriquece el evento con contexto adicional
     */
    public EventEnvelope<T> enrichWith(UserContext userContext, String sessionId) {
        this.userContext = userContext;
        this.sessionId = sessionId;
        return this;
    }

    /**
     * Enriquece el evento con información de tracing
     */
    public EventEnvelope<T> enrichWithTracing(String traceId, String spanId) {
        this.traceId = traceId;
        this.spanId = spanId;
        return this;
    }

    /**
     * Añade metadatos personalizados
     */
    public EventEnvelope<T> addMetadata(String key, Object value) {
        if (this.metadata == null) {
            this.metadata = EventMetadata.createDefault();
        }
        this.metadata.addCustomProperty(key, value);
        return this;
    }

    // ================================
    // PRIVATE UTILITIES
    // ================================

    private static String generateEventId() {
        return UUID.randomUUID().toString();
    }

    private static String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }

    // ================================
    // BUILDER CUSTOMIZATION
    // ================================

    public static class EventEnvelopeBuilder<T> {
        
        public EventEnvelopeBuilder<T> withGeneratedIds() {
            return this.eventId(generateEventId())
                      .correlationId(generateCorrelationId());
        }

        public EventEnvelopeBuilder<T> withCurrentTimestamp() {
            return this.timestamp(Instant.now());
        }

        public EventEnvelopeBuilder<T> withDefaultMetadata() {
            return this.metadata(EventMetadata.createDefault())
                      .systemContext(SystemContext.current());
        }

        public EventEnvelopeBuilder<T> withTracing(String traceId, String spanId) {
            return this.traceId(traceId).spanId(spanId);
        }

        public EventEnvelopeBuilder<T> withUser(UserContext userContext, String sessionId) {
            return this.userContext(userContext).sessionId(sessionId);
        }
    }
    
    // ================================
    // VALIDATION
    // ================================

    /**
     * Valida que el evento tenga la estructura mínima requerida
     */
    public void validate() {
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalStateException("Event ID is required");
        }
        if (eventType == null || eventType.isBlank()) {
            throw new IllegalStateException("Event type is required");
        }
        if (eventVersion == null || eventVersion.isBlank()) {
            throw new IllegalStateException("Event version is required");
        }
        if (timestamp == null) {
            throw new IllegalStateException("Event timestamp is required");
        }
        if (source == null || source.isBlank()) {
            throw new IllegalStateException("Event source is required");
        }
        if (data == null) {
            throw new IllegalStateException("Event data is required");
        }
    }

    /**
     * Valida y retorna el evento (fluent interface)
     */
    public EventEnvelope<T> validated() {
        validate();
        return this;
    }
}