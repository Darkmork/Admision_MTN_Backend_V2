// user-service/src/main/java/com/desafios/mtn/userservice/web/dto/UserDto.java

package com.desafios.mtn.userservice.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Usuario del sistema")
public class UserDto {

    @Schema(description = "ID único del usuario", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID id;

    @NotBlank(message = "Email es obligatorio")
    @Email(message = "Email debe tener formato válido")
    @Size(max = 255, message = "Email no puede exceder 255 caracteres")
    @Schema(description = "Email del usuario", example = "jorge.gangale@mtn.cl", required = true)
    private String email;

    @Size(max = 100, message = "Username no puede exceder 100 caracteres")
    @Schema(description = "Nombre de usuario único", example = "jorge.gangale")
    private String username;

    @NotBlank(message = "Nombre es obligatorio")
    @Size(max = 100, message = "Nombre no puede exceder 100 caracteres")
    @Schema(description = "Nombre del usuario", example = "Jorge", required = true)
    private String firstName;

    @NotBlank(message = "Apellido es obligatorio")
    @Size(max = 100, message = "Apellido no puede exceder 100 caracteres")
    @Schema(description = "Apellido del usuario", example = "Gangale", required = true)
    private String lastName;

    @Size(max = 12, message = "RUT no puede exceder 12 caracteres")
    @Pattern(regexp = "^\\d{1,2}\\.?\\d{3}\\.?\\d{3}-?[0-9kK]$", 
             message = "RUT debe tener formato válido (ej: 12.345.678-9)")
    @Schema(description = "RUT chileno del usuario", example = "12.345.678-9")
    private String rut;

    @Size(max = 20, message = "Teléfono no puede exceder 20 caracteres")
    @Schema(description = "Teléfono del usuario", example = "+56912345678")
    private String phone;

    @Schema(description = "Nivel educativo del usuario", example = "ALL_LEVELS")
    private String educationalLevel;

    @Schema(description = "Materia especializada del usuario", example = "MATHEMATICS")
    private String subject;

    @Schema(description = "Indica si el usuario está habilitado", example = "true")
    private Boolean enabled;

    @Schema(description = "Indica si el email está verificado", example = "true")
    private Boolean emailVerified;

    @Schema(description = "Indica si la cuenta no está expirada", example = "true")
    private Boolean accountNonExpired;

    @Schema(description = "Indica si las credenciales no están expiradas", example = "true")
    private Boolean credentialsNonExpired;

    @Schema(description = "Indica si la cuenta no está bloqueada", example = "true")
    private Boolean accountNonLocked;

    @Schema(description = "Fecha del último login")
    private Instant lastLoginAt;

    @Schema(description = "Número de intentos de login fallidos", example = "0")
    private Integer loginAttempts;

    @Schema(description = "Fecha de creación del usuario")
    private Instant createdAt;

    @Schema(description = "Fecha de última actualización del usuario")
    private Instant updatedAt;

    @Schema(description = "ID del usuario que creó este registro")
    private UUID createdBy;

    @Schema(description = "ID del usuario que actualizó este registro")
    private UUID updatedBy;

    @Schema(description = "Roles asignados al usuario")
    private Set<String> roles;

    @Schema(description = "Información detallada de roles")
    private Set<RoleDto> roleDetails;

    @Schema(description = "Nombre completo del usuario (solo lectura)")
    private String fullName;

    @Schema(description = "Indica si el usuario está activo (solo lectura)")
    private Boolean active;

    // === DTOs ANIDADOS ===

    /**
     * DTO para crear un nuevo usuario
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Datos para crear un nuevo usuario")
    public static class CreateUserRequest {

        @NotBlank(message = "Email es obligatorio")
        @Email(message = "Email debe tener formato válido")
        @Size(max = 255)
        @Schema(description = "Email del usuario", example = "nuevo.usuario@mtn.cl", required = true)
        private String email;

        @Size(max = 100)
        @Schema(description = "Nombre de usuario único", example = "nuevo.usuario")
        private String username;

        @Size(max = 255)
        @Schema(description = "Contraseña del usuario (opcional, se puede generar automáticamente)")
        private String password;

        @NotBlank(message = "Nombre es obligatorio")
        @Size(max = 100)
        @Schema(description = "Nombre del usuario", example = "Nuevo", required = true)
        private String firstName;

        @NotBlank(message = "Apellido es obligatorio")
        @Size(max = 100)
        @Schema(description = "Apellido del usuario", example = "Usuario", required = true)
        private String lastName;

        @Size(max = 12)
        @Pattern(regexp = "^\\d{1,2}\\.?\\d{3}\\.?\\d{3}-?[0-9kK]$", 
                 message = "RUT debe tener formato válido")
        @Schema(description = "RUT chileno del usuario", example = "98.765.432-1")
        private String rut;

        @Size(max = 20)
        @Schema(description = "Teléfono del usuario", example = "+56987654321")
        private String phone;

        @Schema(description = "Nivel educativo", example = "BASIC", 
                allowableValues = {"PRESCHOOL", "BASIC", "HIGH_SCHOOL", "ALL_LEVELS"})
        private String educationalLevel;

        @Schema(description = "Materia especializada", example = "MATHEMATICS",
                allowableValues = {"GENERAL", "LANGUAGE", "MATHEMATICS", "ENGLISH", "SCIENCE", "HISTORY", "ARTS", "PHYSICAL_EDUCATION", "TECHNOLOGY", "RELIGION", "ALL_SUBJECTS"})
        private String subject;

        @Schema(description = "Roles a asignar al usuario", 
                example = "[\"TEACHER\", \"COORDINATOR\"]")
        private Set<String> roles;

        @Schema(description = "Usuario habilitado por defecto", example = "true")
        @Builder.Default
        private Boolean enabled = true;

        @Schema(description = "Email verificado automáticamente para staff", example = "false")
        @Builder.Default
        private Boolean emailVerified = false;
    }

    /**
     * DTO para actualizar un usuario existente
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Datos para actualizar un usuario existente")
    public static class UpdateUserRequest {

        @Email(message = "Email debe tener formato válido")
        @Size(max = 255)
        @Schema(description = "Email del usuario", example = "usuario.actualizado@mtn.cl")
        private String email;

        @Size(max = 100)
        @Schema(description = "Nombre de usuario único", example = "usuario.actualizado")
        private String username;

        @Size(max = 100)
        @Schema(description = "Nombre del usuario", example = "Usuario")
        private String firstName;

        @Size(max = 100)
        @Schema(description = "Apellido del usuario", example = "Actualizado")
        private String lastName;

        @Size(max = 12)
        @Pattern(regexp = "^\\d{1,2}\\.?\\d{3}\\.?\\d{3}-?[0-9kK]$", 
                 message = "RUT debe tener formato válido")
        @Schema(description = "RUT chileno del usuario", example = "11.222.333-4")
        private String rut;

        @Size(max = 20)
        @Schema(description = "Teléfono del usuario", example = "+56911222333")
        private String phone;

        @Schema(description = "Nivel educativo", example = "HIGH_SCHOOL")
        private String educationalLevel;

        @Schema(description = "Materia especializada", example = "ENGLISH")
        private String subject;
    }

    /**
     * DTO para cambio de roles
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Datos para cambio de roles de usuario")
    public static class ChangeRolesRequest {

        @NotEmpty(message = "Se debe especificar al menos un rol")
        @Schema(description = "Nuevos roles para el usuario", 
                example = "[\"TEACHER\", \"COORDINATOR\"]", required = true)
        private Set<String> roles;

        @Schema(description = "Operación a realizar", example = "REPLACE",
                allowableValues = {"ASSIGN", "REMOVE", "REPLACE"})
        @Builder.Default
        private String operation = "REPLACE";

        @Size(max = 500)
        @Schema(description = "Comentarios sobre el cambio de roles")
        private String comments;
    }

    /**
     * DTO para cambio de estado
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Datos para cambio de estado de usuario")
    public static class ChangeStatusRequest {

        @NotNull(message = "Estado es obligatorio")
        @Schema(description = "Nuevo estado del usuario", example = "true", required = true)
        private Boolean enabled;

        @Size(max = 500)
        @Schema(description = "Razón del cambio de estado")
        private String reason;
    }

    /**
     * DTO para respuesta de perfil propio
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Perfil del usuario autenticado")
    public static class UserProfile {

        @Schema(description = "ID del usuario")
        private UUID id;

        @Schema(description = "Email del usuario")
        private String email;

        @Schema(description = "Nombre completo del usuario")
        private String fullName;

        @Schema(description = "Nombre del usuario")
        private String firstName;

        @Schema(description = "Apellido del usuario")
        private String lastName;

        @Schema(description = "RUT del usuario")
        private String rut;

        @Schema(description = "Teléfono del usuario")
        private String phone;

        @Schema(description = "Nivel educativo")
        private String educationalLevel;

        @Schema(description = "Materia especializada")
        private String subject;

        @Schema(description = "Roles del usuario")
        private Set<String> roles;

        @Schema(description = "Información detallada de roles")
        private Set<RoleDto> roleDetails;

        @Schema(description = "Fecha de último login")
        private Instant lastLoginAt;

        @Schema(description = "Email verificado")
        private Boolean emailVerified;

        @Schema(description = "Cuenta activa")
        private Boolean active;
    }

    /**
     * DTO para listado paginado
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Usuario para listados")
    public static class UserSummary {

        @Schema(description = "ID del usuario")
        private UUID id;

        @Schema(description = "Email del usuario")
        private String email;

        @Schema(description = "Nombre completo del usuario")
        private String fullName;

        @Schema(description = "RUT del usuario")
        private String rut;

        @Schema(description = "Teléfono del usuario")
        private String phone;

        @Schema(description = "Roles principales del usuario")
        private Set<String> roles;

        @Schema(description = "Usuario habilitado")
        private Boolean enabled;

        @Schema(description = "Email verificado")
        private Boolean emailVerified;

        @Schema(description = "Cuenta activa")
        private Boolean active;

        @Schema(description = "Último login")
        private Instant lastLoginAt;

        @Schema(description = "Fecha de creación")
        private Instant createdAt;
    }

    /**
     * DTO para estadísticas
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Estadísticas de usuarios")
    public static class UserStatistics {

        @Schema(description = "Total de usuarios")
        private Long totalUsers;

        @Schema(description = "Usuarios activos")
        private Long activeUsers;

        @Schema(description = "Usuarios con email verificado")
        private Long verifiedUsers;

        @Schema(description = "Usuarios por rol")
        private java.util.Map<String, Long> usersByRole;

        @Schema(description = "Usuarios creados últimos 30 días")
        private Long recentUsers;

        @Schema(description = "Usuarios con login reciente (últimos 30 días)")
        private Long recentLogins;
    }

    /**
     * DTO para actualizar perfil propio
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Datos para actualizar perfil propio")
    public static class UpdateProfileRequest {

        @Size(max = 100)
        @Schema(description = "Nombre del usuario", example = "Juan Carlos")
        private String firstName;

        @Size(max = 100)
        @Schema(description = "Apellido del usuario", example = "González")
        private String lastName;

        @Size(max = 20)
        @Schema(description = "Teléfono del usuario", example = "+56912345678")
        private String phone;

        @Schema(description = "Nivel educativo", example = "HIGH_SCHOOL")
        private String educationalLevel;

        @Schema(description = "Materia especializada", example = "MATHEMATICS")
        private String subject;
    }

    /**
     * DTO para cambio de contraseña
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Datos para cambio de contraseña")
    public static class ChangePasswordRequest {

        @NotBlank(message = "Contraseña actual es obligatoria")
        @Schema(description = "Contraseña actual", required = true)
        private String currentPassword;

        @NotBlank(message = "Nueva contraseña es obligatoria")
        @Size(min = 8, max = 100, message = "Nueva contraseña debe tener entre 8 y 100 caracteres")
        @Schema(description = "Nueva contraseña", required = true)
        private String newPassword;

        @NotBlank(message = "Confirmación de contraseña es obligatoria")
        @Schema(description = "Confirmación de nueva contraseña", required = true)
        private String confirmPassword;
    }

    /**
     * DTO para verificar contraseña actual
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Datos para verificar contraseña")
    public static class VerifyPasswordRequest {

        @NotBlank(message = "Contraseña actual es obligatoria")
        @Schema(description = "Contraseña actual a verificar", required = true)
        private String currentPassword;
    }

    /**
     * DTO para actividad del usuario
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Actividad del usuario")
    public static class UserActivity {

        @Schema(description = "Último login del usuario")
        private Instant lastLogin;

        @Schema(description = "Número total de logins")
        private Long totalLogins;

        @Schema(description = "Logins en el período consultado")
        private Long loginsInPeriod;

        @Schema(description = "Fecha de primer login")
        private Instant firstLogin;

        @Schema(description = "Promedio de logins por día")
        private Double averageLoginsPerDay;

        @Schema(description = "Días activos en el período")
        private Long activeDays;

        @Schema(description = "Actividades recientes")
        private java.util.List<ActivityEntry> recentActivities;
    }

    /**
     * DTO para entrada de actividad
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Entrada de actividad")
    public static class ActivityEntry {

        @Schema(description = "Tipo de actividad", example = "LOGIN")
        private String activityType;

        @Schema(description = "Descripción de la actividad", example = "Usuario inició sesión")
        private String description;

        @Schema(description = "Fecha y hora de la actividad")
        private Instant timestamp;

        @Schema(description = "Dirección IP")
        private String ipAddress;

        @Schema(description = "User Agent")
        private String userAgent;
    }

    /**
     * DTO para preferencias del usuario
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Preferencias del usuario")
    public static class UserPreferences {

        @Schema(description = "Idioma preferido", example = "es")
        private String language;

        @Schema(description = "Zona horaria", example = "America/Santiago")
        private String timezone;

        @Schema(description = "Recibir notificaciones por email", example = "true")
        @Builder.Default
        private Boolean emailNotifications = true;

        @Schema(description = "Recibir notificaciones push", example = "true")
        @Builder.Default
        private Boolean pushNotifications = true;

        @Schema(description = "Formato de fecha preferido", example = "dd/MM/yyyy")
        private String dateFormat;

        @Schema(description = "Configuraciones adicionales")
        private java.util.Map<String, Object> additionalSettings;
    }

    /**
     * DTO para información de login
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Información de login")
    public static class LoginInfo {

        @Schema(description = "Dirección IP del cliente")
        private String ipAddress;

        @Schema(description = "User Agent del navegador")
        private String userAgent;

        @Schema(description = "Información del dispositivo")
        private String deviceInfo;

        @Schema(description = "Ubicación geográfica")
        private String location;
    }

    /**
     * DTO para solicitud de eliminación de cuenta
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Solicitud de eliminación de cuenta")
    public static class DeleteAccountRequest {

        @NotBlank(message = "Contraseña actual es obligatoria")
        @Schema(description = "Contraseña actual para confirmar", required = true)
        private String currentPassword;

        @NotBlank(message = "Confirmación es obligatoria")
        @Schema(description = "Texto de confirmación: 'ELIMINAR MI CUENTA'", required = true)
        private String confirmationText;

        @Size(max = 500)
        @Schema(description = "Razón para eliminar la cuenta")
        private String reason;

        @Schema(description = "Solicitar copia de datos antes de eliminar", example = "true")
        @Builder.Default
        private Boolean requestDataExport = false;
    }
}