// user-service/src/main/java/com/desafios/mtn/userservice/migration/MonolithUser.java

package com.desafios.mtn.userservice.migration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Representación de un usuario en el sistema monolítico
 * Utilizado durante el proceso de migración de datos
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonolithUser {
    
    private Long id;
    private String email;
    private String username;
    private String firstName;
    private String lastName;
    private String rut;
    private String phone;
    private String role;
    private String educationalLevel;
    private String subject;
    private Boolean enabled;
    private Boolean emailVerified;
    private Instant lastLoginAt;
    private Instant createdAt;
    private Instant updatedAt;
    
    // Campos adicionales que podrían existir en el monolito
    private String passwordHash;
    private Integer loginAttempts;
    private Boolean accountLocked;
    private Instant lockedAt;
    
    /**
     * Valida que el usuario del monolito tenga los campos requeridos
     */
    public boolean isValid() {
        return email != null && !email.trim().isEmpty() &&
               firstName != null && !firstName.trim().isEmpty() &&
               lastName != null && !lastName.trim().isEmpty() &&
               role != null && !role.trim().isEmpty();
    }
    
    /**
     * Obtiene el nombre completo del usuario
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }
    
    /**
     * Verifica si el usuario está activo
     */
    public boolean isActive() {
        return enabled != null && enabled && 
               (accountLocked == null || !accountLocked);
    }
}