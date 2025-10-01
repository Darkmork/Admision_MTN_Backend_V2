// user-service/src/main/java/com/desafios/mtn/userservice/migration/MigrationResult.java

package com.desafios.mtn.userservice.migration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Resultado de la operación de migración de datos
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MigrationResult {
    
    private Instant startTime;
    private Instant endTime;
    private boolean success;
    private String errorMessage;
    
    private int migratedUsers;
    private int migratedRoleAssignments;
    private int skippedUsers;
    private int errorCount;
    
    @Builder.Default
    private List<String> warnings = new ArrayList<>();
    
    @Builder.Default
    private List<String> errors = new ArrayList<>();
    
    /**
     * Obtiene la duración de la migración en milisegundos
     */
    public long getDurationMs() {
        if (startTime == null || endTime == null) {
            return 0;
        }
        return Duration.between(startTime, endTime).toMillis();
    }
    
    /**
     * Obtiene la duración de la migración en segundos
     */
    public long getDurationSeconds() {
        return getDurationMs() / 1000;
    }
    
    /**
     * Obtiene el total de registros procesados
     */
    public int getTotalProcessed() {
        return migratedUsers + skippedUsers;
    }
    
    /**
     * Obtiene la tasa de éxito como porcentaje
     */
    public double getSuccessRate() {
        int total = getTotalProcessed();
        if (total == 0) {
            return 0.0;
        }
        return (double) migratedUsers / total * 100.0;
    }
    
    /**
     * Verifica si hubo advertencias durante la migración
     */
    public boolean hasWarnings() {
        return warnings != null && !warnings.isEmpty();
    }
    
    /**
     * Verifica si hubo errores durante la migración
     */
    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }
    
    /**
     * Agrega una advertencia al resultado
     */
    public void addWarning(String warning) {
        if (warnings == null) {
            warnings = new ArrayList<>();
        }
        warnings.add(warning);
    }
    
    /**
     * Agrega un error al resultado
     */
    public void addError(String error) {
        if (errors == null) {
            errors = new ArrayList<>();
        }
        errors.add(error);
        errorCount++;
    }
    
    /**
     * Genera un resumen textual del resultado
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        
        summary.append("=== RESUMEN DE MIGRACIÓN ===\n");
        summary.append(String.format("Estado: %s\n", success ? "EXITOSA" : "FALLIDA"));
        summary.append(String.format("Duración: %d ms (%d segundos)\n", getDurationMs(), getDurationSeconds()));
        summary.append(String.format("Usuarios migrados: %d\n", migratedUsers));
        summary.append(String.format("Asignaciones de roles: %d\n", migratedRoleAssignments));
        summary.append(String.format("Usuarios omitidos: %d\n", skippedUsers));
        summary.append(String.format("Errores: %d\n", errorCount));
        summary.append(String.format("Advertencias: %d\n", warnings != null ? warnings.size() : 0));
        summary.append(String.format("Tasa de éxito: %.2f%%\n", getSuccessRate()));
        
        if (hasWarnings()) {
            summary.append("\nADVERTENCIAS:\n");
            for (String warning : warnings) {
                summary.append("- ").append(warning).append("\n");
            }
        }
        
        if (hasErrors()) {
            summary.append("\nERRORES:\n");
            for (String error : errors) {
                summary.append("- ").append(error).append("\n");
            }
        }
        
        if (errorMessage != null) {
            summary.append("\nError principal: ").append(errorMessage).append("\n");
        }
        
        return summary.toString();
    }
    
    /**
     * Verifica si la migración fue exitosa sin errores críticos
     */
    public boolean isFullySuccessful() {
        return success && errorCount == 0;
    }
    
    /**
     * Verifica si la migración fue parcialmente exitosa (con advertencias pero sin errores críticos)
     */
    public boolean isPartiallySuccessful() {
        return success && errorCount == 0 && hasWarnings();
    }
}