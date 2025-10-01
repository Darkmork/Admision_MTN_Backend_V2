package com.desafios.mtn.schemaregistry.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Validador de JSON Schema optimizado con caché
 * Utiliza NetworkNT JSON Schema Validator para validación robusta
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JsonSchemaValidator {

    private final JsonSchemaFactory jsonSchemaFactory = 
        JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
    
    // Caché de esquemas compilados para mejor performance
    private final ConcurrentHashMap<String, JsonSchema> schemaCache = new ConcurrentHashMap<>();

    // ================================
    // VALIDACIÓN PRINCIPAL
    // ================================

    /**
     * Valida un payload JSON contra un esquema JSON Schema
     */
    public ValidationResult validate(JsonNode schemaNode, JsonNode dataNode) {
        try {
            String schemaKey = generateSchemaKey(schemaNode);
            JsonSchema schema = getOrCompileSchema(schemaKey, schemaNode);
            
            Set<ValidationMessage> errors = schema.validate(dataNode);
            
            if (errors.isEmpty()) {
                return ValidationResult.success();
            } else {
                return ValidationResult.failure(errors);
            }
            
        } catch (Exception e) {
            log.error("Error during JSON Schema validation: {}", e.getMessage(), e);
            return ValidationResult.error("Validation error: " + e.getMessage());
        }
    }

    /**
     * Valida un payload JSON contra un esquema como string
     */
    public ValidationResult validate(String schemaJson, String dataJson) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = 
                new com.fasterxml.jackson.databind.ObjectMapper();
            
            JsonNode schemaNode = objectMapper.readTree(schemaJson);
            JsonNode dataNode = objectMapper.readTree(dataJson);
            
            return validate(schemaNode, dataNode);
            
        } catch (Exception e) {
            log.error("Error parsing JSON for validation: {}", e.getMessage(), e);
            return ValidationResult.error("JSON parsing error: " + e.getMessage());
        }
    }

    /**
     * Verifica si un JSON Node es un esquema JSON válido
     */
    public boolean isValidJsonSchema(JsonNode schemaNode) {
        try {
            JsonSchema schema = jsonSchemaFactory.getSchema(schemaNode);
            return schema != null;
        } catch (Exception e) {
            log.debug("Invalid JSON Schema: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Verifica si un string es un esquema JSON válido
     */
    public boolean isValidJsonSchema(String schemaJson) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = 
                new com.fasterxml.jackson.databind.ObjectMapper();
            JsonNode schemaNode = objectMapper.readTree(schemaJson);
            return isValidJsonSchema(schemaNode);
        } catch (Exception e) {
            return false;
        }
    }

    // ================================
    // GESTIÓN DE CACHÉ
    // ================================

    private JsonSchema getOrCompileSchema(String schemaKey, JsonNode schemaNode) {
        return schemaCache.computeIfAbsent(schemaKey, key -> {
            try {
                JsonSchema schema = jsonSchemaFactory.getSchema(schemaNode);
                log.debug("Compiled and cached schema: {}", schemaKey);
                return schema;
            } catch (Exception e) {
                log.error("Error compiling schema {}: {}", schemaKey, e.getMessage());
                throw new RuntimeException("Failed to compile schema", e);
            }
        });
    }

    private String generateSchemaKey(JsonNode schemaNode) {
        // Generar clave única basada en el contenido del schema
        return String.valueOf(schemaNode.hashCode());
    }

    /**
     * Limpia el caché de esquemas compilados
     */
    public void clearCache() {
        schemaCache.clear();
        log.info("JSON Schema cache cleared");
    }

    /**
     * Obtiene estadísticas del caché
     */
    public CacheStatistics getCacheStatistics() {
        return CacheStatistics.builder()
            .cacheSize(schemaCache.size())
            .cachedSchemas(schemaCache.keySet().size())
            .build();
    }

    // ================================
    // VALIDACIONES ESPECÍFICAS
    // ================================

    /**
     * Valida que un evento tiene la estructura base requerida
     */
    public ValidationResult validateBaseEventStructure(JsonNode eventNode) {
        // Schema base para todos los eventos del sistema
        String baseEventSchema = """
            {
                "$schema": "http://json-schema.org/draft-07/schema#",
                "type": "object",
                "required": ["eventId", "eventType", "eventVersion", "timestamp", "source", "data"],
                "properties": {
                    "eventId": {
                        "type": "string",
                        "format": "uuid",
                        "description": "Unique identifier for this event instance"
                    },
                    "eventType": {
                        "type": "string",
                        "description": "Type of event"
                    },
                    "eventVersion": {
                        "type": "string",
                        "pattern": "^\\\\d+\\\\.\\\\d+\\\\.\\\\d+$",
                        "description": "Semantic version of the event schema"
                    },
                    "timestamp": {
                        "type": "string",
                        "format": "date-time",
                        "description": "When the event occurred"
                    },
                    "source": {
                        "type": "string",
                        "description": "Source service that generated the event"
                    },
                    "correlationId": {
                        "type": "string",
                        "format": "uuid",
                        "description": "Correlation ID for tracing"
                    },
                    "causationId": {
                        "type": "string",
                        "format": "uuid", 
                        "description": "ID of the event/command that caused this event"
                    },
                    "data": {
                        "type": "object",
                        "description": "Event payload data"
                    },
                    "metadata": {
                        "type": "object",
                        "description": "Additional metadata"
                    }
                }
            }
            """;
        
        return validate(baseEventSchema, eventNode.toString());
    }

    // ================================
    // RESULT CLASSES
    // ================================

    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class ValidationResult {
        private boolean isValid;
        private Set<ValidationMessage> errors;
        private String errorMessage;
        private ValidationStatus status;

        public static ValidationResult success() {
            return ValidationResult.builder()
                .isValid(true)
                .status(ValidationStatus.SUCCESS)
                .build();
        }

        public static ValidationResult failure(Set<ValidationMessage> errors) {
            StringBuilder errorMsg = new StringBuilder("Validation errors:\n");
            for (ValidationMessage error : errors) {
                errorMsg.append("- ").append(error.getMessage()).append("\n");
            }
            
            return ValidationResult.builder()
                .isValid(false)
                .errors(errors)
                .errorMessage(errorMsg.toString())
                .status(ValidationStatus.VALIDATION_FAILED)
                .build();
        }

        public static ValidationResult error(String errorMessage) {
            return ValidationResult.builder()
                .isValid(false)
                .errorMessage(errorMessage)
                .status(ValidationStatus.ERROR)
                .build();
        }

        public boolean hasErrors() {
            return errors != null && !errors.isEmpty();
        }

        public int getErrorCount() {
            return errors != null ? errors.size() : 0;
        }
    }

    public enum ValidationStatus {
        SUCCESS,
        VALIDATION_FAILED,
        ERROR
    }

    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class CacheStatistics {
        private int cacheSize;
        private int cachedSchemas;
    }
}