package com.desafios.mtn.events.envelope;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Metadatos enriquecidos para eventos del sistema MTN
 * Incluye información técnica, de negocio y operacional
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EventMetadata {

    // ================================
    // INFORMACIÓN TÉCNICA
    // ================================

    /**
     * Esquema utilizado para el evento
     */
    private String schemaRegistry;

    /**
     * Checksum del payload para integridad
     */
    private String checksumSha256;

    /**
     * Tamaño del payload en bytes
     */
    private Long payloadSize;

    /**
     * Codificación del payload
     */
    @Builder.Default
    private String encoding = "UTF-8";

    /**
     * Formato del payload
     */
    @Builder.Default
    private String contentType = "application/json";

    // ================================
    // INFORMACIÓN DE PROCESAMIENTO
    // ================================

    /**
     * Prioridad del evento (1=máxima, 10=mínima)
     */
    @Builder.Default
    private Integer priority = 5;

    /**
     * TTL del evento en segundos
     */
    private Long timeToLiveSeconds;

    /**
     * Número máximo de reintentos permitidos
     */
    @Builder.Default
    private Integer maxRetries = 3;

    /**
     * Estrategia de retry (EXPONENTIAL, LINEAR, FIXED)
     */
    @Builder.Default
    private String retryStrategy = "EXPONENTIAL";

    /**
     * Delay inicial para retry en milisegundos
     */
    @Builder.Default
    private Long initialRetryDelay = 1000L;

    // ================================
    // INFORMACIÓN DE ENRUTAMIENTO
    // ================================

    /**
     * Exchange de RabbitMQ donde se publicó
     */
    private String exchange;

    /**
     * Routing key utilizada
     */
    private String routingKey;

    /**
     * Queue donde fue entregado
     */
    private String queue;

    /**
     * Partición en caso de uso con Kafka
     */
    private Integer partition;

    /**
     * Offset en caso de uso con Kafka
     */
    private Long offset;

    // ================================
    // INFORMACIÓN DE NEGOCIO
    // ================================

    /**
     * Dominio de negocio al que pertenece el evento
     */
    private String businessDomain;

    /**
     * Tipo de operación de negocio
     */
    private String businessOperation;

    /**
     * Entidad de negocio principal afectada
     */
    private String businessEntity;

    /**
     * ID de la entidad de negocio
     */
    private String businessEntityId;

    /**
     * Categoría del evento (COMMAND, QUERY, NOTIFICATION, etc.)
     */
    private String eventCategory;

    // ================================
    // INFORMACIÓN DE CUMPLIMIENTO
    // ================================

    /**
     * Clasificación de datos según privacidad
     */
    private String dataClassification;

    /**
     * Si el evento contiene PII
     */
    @Builder.Default
    private Boolean containsPii = false;

    /**
     * Período de retención de datos
     */
    private String retentionPeriod;

    /**
     * Regulaciones aplicables (GDPR, etc.)
     */
    private String[] applicableRegulations;

    // ================================
    // INFORMACIÓN DE CALIDAD
    // ================================

    /**
     * Nivel de confianza en la calidad del evento (0.0-1.0)
     */
    @Builder.Default
    private Double qualityScore = 1.0;

    /**
     * Fuente de los datos (USER_INPUT, SYSTEM_GENERATED, EXTERNAL_API, etc.)
     */
    private String dataSource;

    /**
     * Nivel de completitud de los datos (0.0-1.0)
     */
    @Builder.Default
    private Double completenessScore = 1.0;

    /**
     * Validaciones aplicadas
     */
    private String[] validationsApplied;

    // ================================
    // CONTEXTO OPERACIONAL
    // ================================

    /**
     * Ambiente donde se generó el evento
     */
    private String environment;

    /**
     * Región del datacenter
     */
    private String region;

    /**
     * Zona de disponibilidad
     */
    private String availabilityZone;

    /**
     * Instancia del servicio que generó el evento
     */
    private String serviceInstance;

    /**
     * Versión del software que generó el evento
     */
    private String softwareVersion;

    // ================================
    // PROPIEDADES PERSONALIZADAS
    // ================================

    /**
     * Propiedades adicionales específicas del evento
     */
    @Builder.Default
    private Map<String, Object> customProperties = new HashMap<>();

    // ================================
    // FACTORY METHODS
    // ================================

    /**
     * Crea metadatos con valores por defecto
     */
    public static EventMetadata createDefault() {
        return EventMetadata.builder()
            .priority(5)
            .maxRetries(3)
            .retryStrategy("EXPONENTIAL")
            .initialRetryDelay(1000L)
            .encoding("UTF-8")
            .contentType("application/json")
            .containsPii(false)
            .qualityScore(1.0)
            .completenessScore(1.0)
            .customProperties(new HashMap<>())
            .build();
    }

    /**
     * Crea metadatos para comando
     */
    public static EventMetadata createForCommand(
            String businessDomain, 
            String businessEntity, 
            String businessEntityId) {
        
        return createDefault()
            .toBuilder()
            .eventCategory("COMMAND")
            .businessDomain(businessDomain)
            .businessEntity(businessEntity)
            .businessEntityId(businessEntityId)
            .priority(3) // Comandos tienen prioridad alta
            .build();
    }

    /**
     * Crea metadatos para evento de dominio
     */
    public static EventMetadata createForDomainEvent(
            String businessDomain, 
            String businessEntity, 
            String businessEntityId) {
        
        return createDefault()
            .toBuilder()
            .eventCategory("DOMAIN_EVENT")
            .businessDomain(businessDomain)
            .businessEntity(businessEntity)
            .businessEntityId(businessEntityId)
            .priority(5) // Eventos de dominio prioridad normal
            .build();
    }

    /**
     * Crea metadatos para notificación
     */
    public static EventMetadata createForNotification(int priority) {
        return createDefault()
            .toBuilder()
            .eventCategory("NOTIFICATION")
            .priority(priority)
            .timeToLiveSeconds(3600L) // Notificaciones expiran en 1 hora
            .build();
    }

    /**
     * Crea metadatos para evento con PII
     */
    public static EventMetadata createWithPii(
            String dataClassification, 
            String retentionPeriod,
            String[] regulations) {
        
        return createDefault()
            .toBuilder()
            .containsPii(true)
            .dataClassification(dataClassification)
            .retentionPeriod(retentionPeriod)
            .applicableRegulations(regulations)
            .build();
    }

    // ================================
    // UTILITY METHODS
    // ================================

    /**
     * Añade una propiedad personalizada
     */
    public EventMetadata addCustomProperty(String key, Object value) {
        if (this.customProperties == null) {
            this.customProperties = new HashMap<>();
        }
        this.customProperties.put(key, value);
        return this;
    }

    /**
     * Obtiene una propiedad personalizada
     */
    @SuppressWarnings("unchecked")
    public <T> T getCustomProperty(String key, Class<T> type) {
        if (customProperties == null) {
            return null;
        }
        Object value = customProperties.get(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }

    /**
     * Verifica si tiene una propiedad personalizada
     */
    public boolean hasCustomProperty(String key) {
        return customProperties != null && customProperties.containsKey(key);
    }

    /**
     * Actualiza información de routing (RabbitMQ)
     */
    public EventMetadata withRouting(String exchange, String routingKey, String queue) {
        this.exchange = exchange;
        this.routingKey = routingKey;
        this.queue = queue;
        return this;
    }

    /**
     * Actualiza información de Kafka
     */
    public EventMetadata withKafka(String topic, Integer partition, Long offset) {
        this.addCustomProperty("kafka.topic", topic);
        this.partition = partition;
        this.offset = offset;
        return this;
    }

    /**
     * Marca el evento como crítico
     */
    public EventMetadata asCritical() {
        this.priority = 1;
        this.maxRetries = 5;
        this.timeToLiveSeconds = 86400L; // 24 horas
        return this;
    }

    /**
     * Marca el evento como de baja prioridad
     */
    public EventMetadata asLowPriority() {
        this.priority = 8;
        this.maxRetries = 1;
        this.timeToLiveSeconds = 3600L; // 1 hora
        return this;
    }

    /**
     * Configura para evento batch
     */
    public EventMetadata asBatchEvent() {
        this.priority = 9;
        this.maxRetries = 1;
        this.retryStrategy = "FIXED";
        this.addCustomProperty("processing.type", "BATCH");
        return this;
    }

    /**
     * Configura para evento en tiempo real
     */
    public EventMetadata asRealTime() {
        this.priority = 2;
        this.maxRetries = 2;
        this.timeToLiveSeconds = 300L; // 5 minutos
        this.addCustomProperty("processing.type", "REAL_TIME");
        return this;
    }

    /**
     * Verifica si el evento está expirado
     */
    public boolean isExpired() {
        if (timeToLiveSeconds == null) {
            return false;
        }
        
        Long createdAt = getCustomProperty("created_at", Long.class);
        if (createdAt == null) {
            return false;
        }
        
        long currentTime = Instant.now().getEpochSecond();
        return (currentTime - createdAt) > timeToLiveSeconds;
    }

    /**
     * Calcula el score de calidad combinado
     */
    public double calculateOverallQualityScore() {
        double base = qualityScore != null ? qualityScore : 1.0;
        double completeness = completenessScore != null ? completenessScore : 1.0;
        
        // Score combinado ponderado
        return (base * 0.7) + (completeness * 0.3);
    }

    /**
     * Verifica si el evento requiere procesamiento especial
     */
    public boolean requiresSpecialProcessing() {
        return containsPii || 
               priority <= 2 || 
               dataClassification != null ||
               hasCustomProperty("special_processing");
    }
}