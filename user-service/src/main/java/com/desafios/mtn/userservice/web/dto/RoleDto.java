// user-service/src/main/java/com/desafios/mtn/userservice/web/dto/RoleDto.java

package com.desafios.mtn.userservice.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Rol del sistema")
public class RoleDto {

    @Schema(description = "ID único del rol", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID id;

    @NotBlank(message = "Nombre del rol es obligatorio")
    @Size(max = 50, message = "Nombre del rol no puede exceder 50 caracteres")
    @Pattern(regexp = "^[A-Z][A-Z0-9_]*$", message = "Nombre del rol debe estar en mayúsculas y usar guiones bajos")
    @Schema(description = "Nombre único del rol", example = "TEACHER", required = true)
    private String name;

    @NotBlank(message = "Nombre de visualización es obligatorio")
    @Size(max = 100, message = "Nombre de visualización no puede exceder 100 caracteres")
    @Schema(description = "Nombre de visualización del rol", example = "Profesor", required = true)
    private String displayName;

    @Size(max = 500, message = "Descripción no puede exceder 500 caracteres")
    @Schema(description = "Descripción del rol", example = "Profesor con acceso a evaluaciones académicas")
    private String description;

    @Schema(description = "Categoría del rol", example = "EDUCATIONAL",
            allowableValues = {"SYSTEM", "EDUCATIONAL", "ADMINISTRATIVE", "FAMILY"})
    private String category;

    @Schema(description = "Indica si el rol está habilitado", example = "true")
    private Boolean enabled;

    @Schema(description = "Indica si es un rol del sistema", example = "true")
    private Boolean systemRole;

    @Schema(description = "Fecha de creación del rol")
    private Instant createdAt;

    @Schema(description = "Fecha de última actualización del rol")
    private Instant updatedAt;

    @Schema(description = "ID del usuario que creó este rol")
    private UUID createdBy;

    @Schema(description = "ID del usuario que actualizó este rol")
    private UUID updatedBy;

    @Schema(description = "Cantidad de usuarios con este rol", example = "15")
    private Long userCount;

    @Schema(description = "Indica si el rol se puede eliminar", example = "false")
    private Boolean deletable;

    // === DTOs ANIDADOS ===

    /**
     * DTO para crear un nuevo rol
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Datos para crear un nuevo rol")
    public static class CreateRoleRequest {

        @NotBlank(message = "Nombre del rol es obligatorio")
        @Size(max = 50)
        @Pattern(regexp = "^[A-Z][A-Z0-9_]*$", message = "Nombre debe estar en mayúsculas y usar guiones bajos")
        @Schema(description = "Nombre único del rol", example = "CUSTOM_EVALUATOR", required = true)
        private String name;

        @NotBlank(message = "Nombre de visualización es obligatorio")
        @Size(max = 100)
        @Schema(description = "Nombre de visualización del rol", example = "Evaluador Personalizado", required = true)
        private String displayName;

        @Size(max = 500)
        @Schema(description = "Descripción del rol", example = "Evaluador con permisos personalizados")
        private String description;

        @Schema(description = "Categoría del rol", example = "EDUCATIONAL",
                allowableValues = {"SYSTEM", "EDUCATIONAL", "ADMINISTRATIVE", "FAMILY"})
        @Builder.Default
        private String category = "EDUCATIONAL";

        @Schema(description = "Rol habilitado por defecto", example = "true")
        @Builder.Default
        private Boolean enabled = true;
    }

    /**
     * DTO para actualizar un rol existente
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Datos para actualizar un rol existente")
    public static class UpdateRoleRequest {

        @Size(max = 50)
        @Pattern(regexp = "^[A-Z][A-Z0-9_]*$", message = "Nombre debe estar en mayúsculas y usar guiones bajos")
        @Schema(description = "Nombre único del rol", example = "CUSTOM_EVALUATOR")
        private String name;

        @Size(max = 100)
        @Schema(description = "Nombre de visualización del rol", example = "Evaluador Personalizado")
        private String displayName;

        @Size(max = 500)
        @Schema(description = "Descripción del rol", example = "Evaluador con permisos personalizados")
        private String description;

        @Schema(description = "Categoría del rol", example = "EDUCATIONAL")
        private String category;
    }

    /**
     * DTO para cambio de estado del rol
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Datos para cambio de estado del rol")
    public static class ChangeRoleStatusRequest {

        @NotNull(message = "Estado es obligatorio")
        @Schema(description = "Nuevo estado del rol", example = "true", required = true)
        private Boolean enabled;

        @Size(max = 500)
        @Schema(description = "Razón del cambio de estado")
        private String reason;
    }

    /**
     * DTO para listado de roles
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Rol para listados")
    public static class RoleSummary {

        @Schema(description = "ID del rol")
        private UUID id;

        @Schema(description = "Nombre del rol")
        private String name;

        @Schema(description = "Nombre de visualización")
        private String displayName;

        @Schema(description = "Descripción corta")
        private String description;

        @Schema(description = "Categoría del rol")
        private String category;

        @Schema(description = "Rol habilitado")
        private Boolean enabled;

        @Schema(description = "Es rol del sistema")
        private Boolean systemRole;

        @Schema(description = "Cantidad de usuarios")
        private Long userCount;

        @Schema(description = "Se puede eliminar")
        private Boolean deletable;

        @Schema(description = "Fecha de creación")
        private Instant createdAt;
    }

    /**
     * DTO para estadísticas de roles
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Estadísticas de roles")
    public static class RoleStatistics {

        @Schema(description = "Total de roles")
        private Long totalRoles;

        @Schema(description = "Roles activos")
        private Long activeRoles;

        @Schema(description = "Roles del sistema")
        private Long systemRoles;

        @Schema(description = "Roles personalizados")
        private Long customRoles;

        @Schema(description = "Estadísticas por categoría")
        private java.util.Map<String, Long> rolesByCategory;

        @Schema(description = "Roles con usuarios asignados")
        private Long rolesWithUsers;

        @Schema(description = "Roles sin usuarios")
        private Long rolesWithoutUsers;
    }

    /**
     * DTO para información detallada del rol con usuarios
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Información detallada del rol")
    public static class RoleDetails {

        @Schema(description = "Información básica del rol")
        private RoleDto role;

        @Schema(description = "Cantidad total de usuarios")
        private Long totalUsers;

        @Schema(description = "Usuarios activos con este rol")
        private Long activeUsers;

        @Schema(description = "Últimos usuarios asignados")
        private java.util.List<UserDto.UserSummary> recentUsers;

        @Schema(description = "Fecha de última asignación")
        private Instant lastAssigned;

        @Schema(description = "Permisos asociados al rol")
        private java.util.Set<String> permissions;
    }

    /**
     * Constantes para roles del sistema
     */
    public static class SystemRoles {
        public static final String ADMIN = "ADMIN";
        public static final String TEACHER = "TEACHER";
        public static final String COORDINATOR = "COORDINATOR";
        public static final String PSYCHOLOGIST = "PSYCHOLOGIST";
        public static final String CYCLE_DIRECTOR = "CYCLE_DIRECTOR";
        public static final String APODERADO = "APODERADO";

        private SystemRoles() {} // Utility class
    }

    /**
     * Constantes para categorías de roles
     */
    public static class RoleCategories {
        public static final String SYSTEM = "SYSTEM";
        public static final String EDUCATIONAL = "EDUCATIONAL";
        public static final String ADMINISTRATIVE = "ADMINISTRATIVE";
        public static final String FAMILY = "FAMILY";

        private RoleCategories() {} // Utility class
    }
}