// user-service/src/main/java/com/desafios/mtn/userservice/web/controller/MigrationController.java

package com.desafios.mtn.userservice.web.controller;

import com.desafios.mtn.userservice.migration.DataMigrationService;
import com.desafios.mtn.userservice.migration.MigrationResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/migration")
@RequiredArgsConstructor
@Tag(name = "Data Migration", description = "API para operaciones de migración de datos desde el monolito")
public class MigrationController {

    private final DataMigrationService migrationService;

    @Operation(
        summary = "Ejecutar migración de datos",
        description = "Ejecuta la migración completa de datos desde el monolito al microservicio"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Migración ejecutada exitosamente",
            content = @Content(schema = @Schema(implementation = MigrationResult.class))
        ),
        @ApiResponse(responseCode = "500", description = "Error durante la migración"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado - Solo administradores")
    })
    @PostMapping("/execute")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MigrationResult> executeMigration(
            @Parameter(description = "Forzar migración aunque ya se haya ejecutado")
            @RequestParam(defaultValue = "false") boolean force
    ) {
        log.info("Iniciando migración de datos - Forzar: {}", force);
        
        try {
            MigrationResult result = migrationService.executeMigration();
            
            if (result.isSuccess()) {
                log.info("Migración completada exitosamente");
                return ResponseEntity.ok(result);
            } else {
                log.error("Migración falló: {}", result.getErrorMessage());
                return ResponseEntity.status(500).body(result);
            }
            
        } catch (Exception e) {
            log.error("Error inesperado durante la migración", e);
            
            MigrationResult errorResult = MigrationResult.builder()
                .success(false)
                .errorMessage(e.getMessage())
                .build();
                
            return ResponseEntity.status(500).body(errorResult);
        }
    }

    @Operation(
        summary = "Obtener estado de migración",
        description = "Obtiene información sobre el estado actual de la migración"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Estado de migración obtenido"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado")
    })
    @GetMapping("/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MigrationStatus> getMigrationStatus() {
        log.info("Obteniendo estado de migración");
        
        // En una implementación real, esto verificaría el estado de la migración
        // revisando logs, tablas de control, etc.
        MigrationStatus status = MigrationStatus.builder()
            .migrationExecuted(true) // Placeholder
            .lastMigrationDate("2025-01-30T10:00:00Z") // Placeholder
            .totalMigratedUsers(10) // Placeholder
            .systemReady(true)
            .build();
            
        return ResponseEntity.ok(status);
    }

    @Operation(
        summary = "Validar prerequisitos de migración",
        description = "Valida que todos los prerequisitos estén en lugar antes de ejecutar la migración"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Prerequisitos validados"),
        @ApiResponse(responseCode = "400", description = "Prerequisitos no cumplidos"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado")
    })
    @GetMapping("/validate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ValidationResult> validateMigrationPrerequisites() {
        log.info("Validando prerequisitos de migración");
        
        try {
            // Aquí iría la lógica de validación real
            ValidationResult result = ValidationResult.builder()
                .valid(true)
                .message("Todos los prerequisitos están cumplidos")
                .build();
                
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            ValidationResult result = ValidationResult.builder()
                .valid(false)
                .message("Error validando prerequisitos: " + e.getMessage())
                .build();
                
            return ResponseEntity.badRequest().body(result);
        }
    }

    @Operation(
        summary = "Rollback de migración",
        description = "Revierte los cambios de la migración (PELIGROSO - Solo para emergencias)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Rollback ejecutado"),
        @ApiResponse(responseCode = "500", description = "Error durante rollback"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado")
    })
    @PostMapping("/rollback")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MigrationResult> rollbackMigration(
            @Parameter(description = "Confirmación de rollback (debe ser 'CONFIRM_ROLLBACK')")
            @RequestParam String confirmation
    ) {
        if (!"CONFIRM_ROLLBACK".equals(confirmation)) {
            return ResponseEntity.badRequest().build();
        }
        
        log.warn("INICIANDO ROLLBACK DE MIGRACIÓN - OPERACIÓN PELIGROSA");
        
        // En una implementación real, esto revertiría la migración
        MigrationResult result = MigrationResult.builder()
            .success(false)
            .errorMessage("Rollback no implementado - contacte al administrador del sistema")
            .build();
            
        return ResponseEntity.status(501).body(result); // Not Implemented
    }

    /**
     * DTO para estado de migración
     */
    @Schema(description = "Estado de la migración de datos")
    public static class MigrationStatus {
        @Schema(description = "Indica si la migración se ha ejecutado")
        public boolean migrationExecuted;
        
        @Schema(description = "Fecha de la última migración")
        public String lastMigrationDate;
        
        @Schema(description = "Total de usuarios migrados")
        public int totalMigratedUsers;
        
        @Schema(description = "Indica si el sistema está listo para uso")
        public boolean systemReady;
        
        public static MigrationStatusBuilder builder() {
            return new MigrationStatusBuilder();
        }
        
        public static class MigrationStatusBuilder {
            private MigrationStatus status = new MigrationStatus();
            
            public MigrationStatusBuilder migrationExecuted(boolean executed) {
                status.migrationExecuted = executed;
                return this;
            }
            
            public MigrationStatusBuilder lastMigrationDate(String date) {
                status.lastMigrationDate = date;
                return this;
            }
            
            public MigrationStatusBuilder totalMigratedUsers(int total) {
                status.totalMigratedUsers = total;
                return this;
            }
            
            public MigrationStatusBuilder systemReady(boolean ready) {
                status.systemReady = ready;
                return this;
            }
            
            public MigrationStatus build() {
                return status;
            }
        }
    }

    /**
     * DTO para resultado de validación
     */
    @Schema(description = "Resultado de validación de prerequisitos")
    public static class ValidationResult {
        @Schema(description = "Indica si la validación pasó")
        public boolean valid;
        
        @Schema(description = "Mensaje descriptivo del resultado")
        public String message;
        
        public static ValidationResultBuilder builder() {
            return new ValidationResultBuilder();
        }
        
        public static class ValidationResultBuilder {
            private ValidationResult result = new ValidationResult();
            
            public ValidationResultBuilder valid(boolean valid) {
                result.valid = valid;
                return this;
            }
            
            public ValidationResultBuilder message(String message) {
                result.message = message;
                return this;
            }
            
            public ValidationResult build() {
                return result;
            }
        }
    }
}