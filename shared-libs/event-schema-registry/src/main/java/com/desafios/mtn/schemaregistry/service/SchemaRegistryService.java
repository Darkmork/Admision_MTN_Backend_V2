package com.desafios.mtn.schemaregistry.service;

import com.desafios.mtn.schemaregistry.domain.EventSchema;
import com.desafios.mtn.schemaregistry.domain.EventSchema.CompatibilityMode;
import com.desafios.mtn.schemaregistry.repository.EventSchemaRepository;
import com.desafios.mtn.schemaregistry.exception.SchemaRegistryException;
import com.desafios.mtn.schemaregistry.exception.SchemaValidationException;
import com.desafios.mtn.schemaregistry.exception.IncompatibleSchemaException;
import com.desafios.mtn.schemaregistry.validation.JsonSchemaValidator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Servicio principal del Schema Registry
 * Gestiona registro, versionado y validación de esquemas de eventos
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SchemaRegistryService {

    private final EventSchemaRepository schemaRepository;
    private final JsonSchemaValidator jsonSchemaValidator;
    private final SchemaCompatibilityChecker compatibilityChecker;
    private final ObjectMapper objectMapper;

    // ================================
    // REGISTRO DE ESQUEMAS
    // ================================

    /**
     * Registra un nuevo esquema de evento
     */
    public EventSchema registerSchema(RegisterSchemaRequest request) {
        log.info("Registering new schema: {} version {}", request.getEventType(), request.getVersion());
        
        // Validar que el esquema JSON es válido
        validateJsonSchema(request.getJsonSchema());
        
        // Verificar que no existe esta combinación exacta
        Optional<EventSchema> existingSchema = schemaRepository.findByEventTypeAndVersion(
            request.getEventType(), request.getVersion());
        
        if (existingSchema.isPresent()) {
            throw new SchemaRegistryException(
                "Schema already exists for event type '" + request.getEventType() + 
                "' version '" + request.getVersion() + "'");
        }
        
        // Verificar compatibilidad con esquemas existentes
        checkCompatibilityWithExistingSchemas(
            request.getEventType(), 
            request.getJsonSchema(), 
            request.getCompatibilityMode()
        );
        
        // Crear y guardar el nuevo esquema
        EventSchema newSchema = EventSchema.createNew(
            request.getEventType(),
            request.getVersion(),
            request.getJsonSchema(),
            request.getCompatibilityMode(),
            request.getRegisteredBy(),
            request.getCreatedBy()
        );
        
        if (request.getChangeDescription() != null) {
            newSchema.setChangeDescription(request.getChangeDescription());
        }
        
        if (request.getExamples() != null) {
            newSchema.setExamples(request.getExamples());
        }
        
        if (request.getMetadata() != null) {
            newSchema.setMetadata(request.getMetadata());
        }
        
        EventSchema savedSchema = schemaRepository.save(newSchema);
        
        // Si es la primera versión, activarla automáticamente
        if (isFirstVersionForEventType(request.getEventType())) {
            activateSchema(savedSchema.getId(), request.getCreatedBy());
        }
        
        log.info("Schema registered successfully: {} (ID: {})", 
                request.getEventType(), savedSchema.getId());
        
        return savedSchema;
    }

    /**
     * Actualiza un esquema existente (solo metadatos, no el JSON Schema)
     */
    public EventSchema updateSchemaMetadata(UUID schemaId, UpdateSchemaMetadataRequest request) {
        EventSchema schema = getSchemaById(schemaId);
        
        if (request.getChangeDescription() != null) {
            schema.setChangeDescription(request.getChangeDescription());
        }
        
        if (request.getExamples() != null) {
            schema.setExamples(request.getExamples());
        }
        
        if (request.getMetadata() != null) {
            schema.setMetadata(request.getMetadata());
        }
        
        schema.setUpdatedBy(request.getUpdatedBy());
        schema.setUpdatedAt(Instant.now());
        
        return schemaRepository.save(schema);
    }

    // ================================
    // GESTIÓN DE VERSIONES
    // ================================

    /**
     * Activa una versión específica de esquema
     */
    public void activateSchema(UUID schemaId, String updatedBy) {
        EventSchema schema = getSchemaById(schemaId);
        
        // Desactivar otras versiones activas del mismo evento
        List<EventSchema> activeSchemas = schemaRepository.findByEventTypeAndIsActive(
            schema.getEventType(), true);
        
        for (EventSchema activeSchema : activeSchemas) {
            if (!activeSchema.getId().equals(schemaId)) {
                activeSchema.deactivate(updatedBy);
                schemaRepository.save(activeSchema);
            }
        }
        
        // Activar el esquema solicitado
        schema.activate(updatedBy);
        schemaRepository.save(schema);
        
        log.info("Activated schema: {} version {} (ID: {})", 
                schema.getEventType(), schema.getVersion(), schemaId);
    }

    /**
     * Depreca una versión de esquema
     */
    public void deprecateSchema(UUID schemaId, String reason, String updatedBy) {
        EventSchema schema = getSchemaById(schemaId);
        
        schema.deprecate(reason, updatedBy);
        schemaRepository.save(schema);
        
        log.info("Deprecated schema: {} version {} (ID: {}) - Reason: {}", 
                schema.getEventType(), schema.getVersion(), schemaId, reason);
    }

    // ================================
    // VALIDACIÓN DE EVENTOS
    // ================================

    /**
     * Valida un payload de evento contra su esquema activo
     */
    public ValidationResult validateEvent(String eventType, String eventPayload) {
        return validateEvent(eventType, null, eventPayload);
    }

    /**
     * Valida un payload de evento contra una versión específica de esquema
     */
    public ValidationResult validateEvent(String eventType, String version, String eventPayload) {
        EventSchema schema;
        
        if (version != null) {
            schema = schemaRepository.findByEventTypeAndVersion(eventType, version)
                .orElseThrow(() -> new SchemaRegistryException(
                    "No schema found for event type '" + eventType + "' version '" + version + "'"));
        } else {
            schema = schemaRepository.findByEventTypeAndIsActive(eventType, true)
                .stream().findFirst()
                .orElseThrow(() -> new SchemaRegistryException(
                    "No active schema found for event type '" + eventType + "'"));
        }
        
        return validateEventAgainstSchema(eventPayload, schema);
    }

    /**
     * Valida un payload contra un esquema específico
     */
    private ValidationResult validateEventAgainstSchema(String eventPayload, EventSchema schema) {
        try {
            JsonNode payloadNode = objectMapper.readTree(eventPayload);
            JsonNode schemaNode = objectMapper.readTree(schema.getJsonSchema());
            
            com.networknt.schema.ValidationMessage violations = 
                jsonSchemaValidator.validate(schemaNode, payloadNode);
            
            if (violations.isEmpty()) {
                return ValidationResult.success(schema);
            } else {
                return ValidationResult.failure(schema, violations.toString());
            }
            
        } catch (Exception e) {
            log.error("Error validating event payload against schema {}: {}", 
                     schema.getId(), e.getMessage());
            return ValidationResult.failure(schema, "Validation error: " + e.getMessage());
        }
    }

    // ================================
    // CONSULTAS
    // ================================

    /**
     * Obtiene un esquema por ID
     */
    public EventSchema getSchemaById(UUID schemaId) {
        return schemaRepository.findById(schemaId)
            .orElseThrow(() -> new SchemaRegistryException(
                "Schema not found with ID: " + schemaId));
    }

    /**
     * Obtiene el esquema activo para un tipo de evento
     */
    public Optional<EventSchema> getActiveSchema(String eventType) {
        return schemaRepository.findByEventTypeAndIsActive(eventType, true)
            .stream().findFirst();
    }

    /**
     * Obtiene todas las versiones de un tipo de evento
     */
    public List<EventSchema> getAllVersions(String eventType) {
        return schemaRepository.findByEventTypeOrderByCreatedAtDesc(eventType);
    }

    /**
     * Obtiene todos los tipos de evento registrados
     */
    public List<String> getAllEventTypes() {
        return schemaRepository.findDistinctEventTypes();
    }

    /**
     * Obtiene esquemas por compatibilidad
     */
    public List<EventSchema> getSchemasByCompatibility(CompatibilityMode compatibility) {
        return schemaRepository.findByCompatibilityMode(compatibility);
    }

    /**
     * Obtiene esquemas deprecados
     */
    public List<EventSchema> getDeprecatedSchemas() {
        return schemaRepository.findByIsDeprecatedTrue();
    }

    // ================================
    // VALIDACIÓN Y COMPATIBILIDAD
    // ================================

    private void validateJsonSchema(String jsonSchema) {
        try {
            JsonNode schemaNode = objectMapper.readTree(jsonSchema);
            
            // Validar que es un JSON Schema válido
            if (!jsonSchemaValidator.isValidJsonSchema(schemaNode)) {
                throw new SchemaValidationException("Invalid JSON Schema format");
            }
            
        } catch (Exception e) {
            throw new SchemaValidationException("Invalid JSON Schema: " + e.getMessage());
        }
    }

    private void checkCompatibilityWithExistingSchemas(
            String eventType, 
            String newJsonSchema, 
            CompatibilityMode compatibilityMode) {
        
        List<EventSchema> existingSchemas = schemaRepository.findByEventType(eventType);
        
        if (existingSchemas.isEmpty()) {
            return; // Primera versión, no hay que verificar compatibilidad
        }
        
        for (EventSchema existingSchema : existingSchemas) {
            if (!compatibilityChecker.isCompatible(
                    existingSchema.getJsonSchema(), 
                    newJsonSchema, 
                    compatibilityMode)) {
                
                throw new IncompatibleSchemaException(
                    "New schema is not compatible with existing version " + 
                    existingSchema.getVersion() + " under " + compatibilityMode + " compatibility mode");
            }
        }
    }

    private boolean isFirstVersionForEventType(String eventType) {
        return schemaRepository.countByEventType(eventType) == 1;
    }

    // ================================
    // DTOs
    // ================================

    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class RegisterSchemaRequest {
        private String eventType;
        private String version;
        private String jsonSchema;
        private CompatibilityMode compatibilityMode;
        private String changeDescription;
        private String examples;
        private String metadata;
        private String registeredBy;
        private String createdBy;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class UpdateSchemaMetadataRequest {
        private String changeDescription;
        private String examples;
        private String metadata;
        private String updatedBy;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class ValidationResult {
        private boolean isValid;
        private EventSchema schema;
        private String errorMessage;
        private Instant validatedAt;

        public static ValidationResult success(EventSchema schema) {
            return ValidationResult.builder()
                .isValid(true)
                .schema(schema)
                .validatedAt(Instant.now())
                .build();
        }

        public static ValidationResult failure(EventSchema schema, String errorMessage) {
            return ValidationResult.builder()
                .isValid(false)
                .schema(schema)
                .errorMessage(errorMessage)
                .validatedAt(Instant.now())
                .build();
        }
    }
}