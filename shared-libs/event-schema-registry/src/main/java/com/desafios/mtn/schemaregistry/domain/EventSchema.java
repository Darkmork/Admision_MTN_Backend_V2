package com.desafios.mtn.schemaregistry.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import java.time.Instant;
import java.util.UUID;

/**
 * Entidad que representa un esquema de evento en el registro
 * Soporta versionado y evolución de esquemas con compatibilidad
 */
@Entity
@Table(name = "event_schemas", 
       indexes = {
           @Index(name = "idx_event_type_version", columnList = "eventType,version", unique = true),
           @Index(name = "idx_event_type_active", columnList = "eventType,isActive"),
           @Index(name = "idx_compatibility_mode", columnList = "compatibilityMode"),
           @Index(name = "idx_created_at", columnList = "createdAt")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventSchema {

    @Id
    @GeneratedValue
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    /**
     * Tipo de evento (ej: "ApplicationSubmitted", "EvaluationCompleted")
     */
    @Column(nullable = false, length = 100)
    private String eventType;

    /**
     * Versión semántica del esquema (ej: "1.0.0", "2.1.0")
     */
    @Column(nullable = false, length = 20)
    private String version;

    /**
     * Esquema JSON Schema completo
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String jsonSchema;

    /**
     * Hash del esquema para detección de cambios
     */
    @Column(nullable = false, length = 64)
    private String schemaHash;

    /**
     * Modo de compatibilidad para evolución
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CompatibilityMode compatibilityMode;

    /**
     * Si esta versión está activa para nuevos eventos
     */
    @Column(nullable = false)
    private Boolean isActive;

    /**
     * Si esta versión está deprecada
     */
    @Column(nullable = false)
    private Boolean isDeprecated;

    /**
     * Fecha de deprecación (si aplica)
     */
    private Instant deprecatedAt;

    /**
     * Descripción de los cambios en esta versión
     */
    @Column(columnDefinition = "TEXT")
    private String changeDescription;

    /**
     * Ejemplos de payloads válidos
     */
    @Column(columnDefinition = "TEXT")
    private String examples;

    /**
     * Servicio que registró este esquema
     */
    @Column(nullable = false, length = 50)
    private String registeredBy;

    /**
     * Metadatos adicionales
     */
    @Column(columnDefinition = "TEXT")
    private String metadata;

    /**
     * Timestamps de auditoría
     */
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    private String createdBy;
    private String updatedBy;

    // ================================
    // MÉTODOS DE NEGOCIO
    // ================================

    public static EventSchema createNew(
            String eventType, 
            String version, 
            String jsonSchema, 
            CompatibilityMode compatibilityMode,
            String registeredBy,
            String createdBy) {
        
        Instant now = Instant.now();
        
        return EventSchema.builder()
            .eventType(eventType)
            .version(version)
            .jsonSchema(jsonSchema)
            .schemaHash(calculateHash(jsonSchema))
            .compatibilityMode(compatibilityMode)
            .isActive(true)
            .isDeprecated(false)
            .registeredBy(registeredBy)
            .createdBy(createdBy)
            .updatedBy(createdBy)
            .createdAt(now)
            .updatedAt(now)
            .build();
    }

    public void deprecate(String reason, String updatedBy) {
        this.isDeprecated = true;
        this.isActive = false;
        this.deprecatedAt = Instant.now();
        this.changeDescription = "DEPRECATED: " + reason;
        this.updatedBy = updatedBy;
        this.updatedAt = Instant.now();
    }

    public void activate(String updatedBy) {
        this.isActive = true;
        this.updatedBy = updatedBy;
        this.updatedAt = Instant.now();
    }

    public void deactivate(String updatedBy) {
        this.isActive = false;
        this.updatedBy = updatedBy;
        this.updatedAt = Instant.now();
    }

    public boolean isCompatibleWith(EventSchema otherSchema) {
        if (!this.eventType.equals(otherSchema.eventType)) {
            return false;
        }

        return switch (this.compatibilityMode) {
            case BACKWARD -> canReadOldData(otherSchema);
            case FORWARD -> canWriteNewData(otherSchema);
            case FULL -> canReadOldData(otherSchema) && canWriteNewData(otherSchema);
            case NONE -> this.version.equals(otherSchema.version);
        };
    }

    private boolean canReadOldData(EventSchema oldSchema) {
        // Lógica simplificada - en producción se haría análisis completo del schema
        return compareVersions(this.version, oldSchema.version) >= 0;
    }

    private boolean canWriteNewData(EventSchema newSchema) {
        // Lógica simplificada - en producción se haría análisis completo del schema
        return compareVersions(newSchema.version, this.version) >= 0;
    }

    private int compareVersions(String version1, String version2) {
        String[] v1Parts = version1.split("\\.");
        String[] v2Parts = version2.split("\\.");
        
        int maxLength = Math.max(v1Parts.length, v2Parts.length);
        
        for (int i = 0; i < maxLength; i++) {
            int v1Part = i < v1Parts.length ? Integer.parseInt(v1Parts[i]) : 0;
            int v2Part = i < v2Parts.length ? Integer.parseInt(v2Parts[i]) : 0;
            
            if (v1Part != v2Part) {
                return Integer.compare(v1Part, v2Part);
            }
        }
        
        return 0;
    }

    private static String calculateHash(String jsonSchema) {
        // En producción usar algoritmo hash robusto (SHA-256)
        return String.valueOf(jsonSchema.hashCode());
    }

    // ================================
    // ENUMS
    // ================================

    public enum CompatibilityMode {
        /**
         * Nuevos esquemas pueden leer datos escritos con esquemas anteriores
         */
        BACKWARD,
        
        /**
         * Datos escritos con nuevos esquemas pueden ser leídos por esquemas anteriores
         */
        FORWARD,
        
        /**
         * Compatibilidad completa en ambas direcciones
         */
        FULL,
        
        /**
         * Sin garantías de compatibilidad
         */
        NONE
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (isActive == null) isActive = true;
        if (isDeprecated == null) isDeprecated = false;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}