// user-service/src/main/java/com/desafios/mtn/userservice/migration/DataMigrationService.java

package com.desafios.mtn.userservice.migration;

import com.desafios.mtn.userservice.domain.Role;
import com.desafios.mtn.userservice.domain.User;
import com.desafios.mtn.userservice.domain.UserRole;
import com.desafios.mtn.userservice.repository.RoleRepository;
import com.desafios.mtn.userservice.repository.UserRepository;
import com.desafios.mtn.userservice.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataMigrationService {

    private final Environment environment;
    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    
    private static final String MIGRATION_PROPERTY = "user-service.migration.enabled";
    private static final String MONOLITH_DB_PROPERTY = "user-service.migration.monolith-db-url";

    /**
     * Ejecuta la migración de datos automáticamente al iniciar la aplicación
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (isMigrationEnabled()) {
            log.info("Migración automática habilitada, iniciando proceso...");
            try {
                executeMigration();
            } catch (Exception e) {
                log.error("Error durante la migración automática", e);
                // En producción, podrías querer fallar el startup o enviar alertas
            }
        } else {
            log.info("Migración automática deshabilitada");
        }
    }

    /**
     * Ejecuta la migración completa de datos desde el monolito
     */
    @Transactional
    public MigrationResult executeMigration() {
        log.info("=== INICIANDO MIGRACIÓN DE DATOS ===");
        
        MigrationResult result = new MigrationResult();
        result.setStartTime(Instant.now());
        
        try {
            // Paso 1: Verificar conexiones y prerequisitos
            validatePrerequisites();
            
            // Paso 2: Migrar usuarios
            result.setMigratedUsers(migrateUsers());
            
            // Paso 3: Migrar asignaciones de roles
            result.setMigratedRoleAssignments(migrateUserRoles());
            
            // Paso 4: Verificar integridad de datos
            validateDataIntegrity();
            
            // Paso 5: Registrar evento de migración completada
            recordMigrationEvent(result);
            
            result.setEndTime(Instant.now());
            result.setSuccess(true);
            
            log.info("=== MIGRACIÓN COMPLETADA EXITOSAMENTE ===");
            log.info("Usuarios migrados: {}", result.getMigratedUsers());
            log.info("Asignaciones de roles: {}", result.getMigratedRoleAssignments());
            log.info("Duración: {} ms", result.getDurationMs());
            
        } catch (Exception e) {
            result.setEndTime(Instant.now());
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            
            log.error("=== MIGRACIÓN FALLIDA ===", e);
            throw new MigrationException("Migración fallida: " + e.getMessage(), e);
        }
        
        return result;
    }

    /**
     * Verifica prerequisitos para la migración
     */
    private void validatePrerequisites() {
        log.info("Validando prerequisitos de migración...");
        
        // Verificar que existan los roles del sistema
        List<Role> systemRoles = roleRepository.findBySystemRoleTrue();
        if (systemRoles.size() < 6) {
            throw new MigrationException("Roles del sistema incompletos. Se requieren al menos 6 roles.");
        }
        
        // Verificar conexión a base de datos
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        } catch (DataAccessException e) {
            throw new MigrationException("No se puede conectar a la base de datos", e);
        }
        
        log.info("Prerequisitos validados correctamente");
    }

    /**
     * Migra usuarios desde el monolito o fuente de datos
     */
    private int migrateUsers() {
        log.info("Iniciando migración de usuarios...");
        
        List<MonolithUser> monolithUsers = fetchMonolithUsers();
        int migratedCount = 0;
        
        for (MonolithUser monolithUser : monolithUsers) {
            try {
                User user = convertToUser(monolithUser);
                
                // Verificar si el usuario ya existe
                Optional<User> existingUser = userRepository.findByEmailIgnoreCase(user.getEmail());
                
                if (existingUser.isPresent()) {
                    // Actualizar usuario existente
                    User existing = existingUser.get();
                    updateUserFromMonolith(existing, monolithUser);
                    userRepository.save(existing);
                    log.debug("Usuario actualizado: {}", existing.getEmail());
                } else {
                    // Crear nuevo usuario
                    userRepository.save(user);
                    log.debug("Usuario creado: {}", user.getEmail());
                }
                
                migratedCount++;
                
            } catch (Exception e) {
                log.error("Error migrando usuario {}: {}", monolithUser.getEmail(), e.getMessage());
                // Continuar con otros usuarios en lugar de fallar completamente
            }
        }
        
        log.info("Migración de usuarios completada: {} usuarios procesados", migratedCount);
        return migratedCount;
    }

    /**
     * Migra asignaciones de roles
     */
    private int migrateUserRoles() {
        log.info("Iniciando migración de asignaciones de roles...");
        
        List<MonolithUser> monolithUsers = fetchMonolithUsers();
        Map<String, Role> roleMap = createRoleMap();
        int migratedCount = 0;
        
        for (MonolithUser monolithUser : monolithUsers) {
            try {
                Optional<User> userOpt = userRepository.findByEmailIgnoreCase(monolithUser.getEmail());
                if (userOpt.isEmpty()) {
                    log.warn("Usuario no encontrado para asignación de rol: {}", monolithUser.getEmail());
                    continue;
                }
                
                User user = userOpt.get();
                Role role = roleMap.get(monolithUser.getRole());
                
                if (role == null) {
                    log.warn("Rol no encontrado: {} para usuario {}", monolithUser.getRole(), user.getEmail());
                    continue;
                }
                
                // Verificar si ya existe la asignación
                boolean exists = userRoleRepository.existsByUserIdAndRoleIdAndActiveTrue(user.getId(), role.getId());
                
                if (!exists) {
                    UserRole userRole = UserRole.builder()
                            .user(user)
                            .role(role)
                            .assignedAt(monolithUser.getCreatedAt())
                            .active(true)
                            .comments("Migrado desde monolito")
                            .build();
                    
                    userRoleRepository.save(userRole);
                    migratedCount++;
                    log.debug("Rol {} asignado a usuario {}", role.getName(), user.getEmail());
                }
                
            } catch (Exception e) {
                log.error("Error asignando rol para usuario {}: {}", monolithUser.getEmail(), e.getMessage());
            }
        }
        
        log.info("Migración de roles completada: {} asignaciones procesadas", migratedCount);
        return migratedCount;
    }

    /**
     * Obtiene usuarios del monolito (simulado para demostración)
     * En producción, esto se conectaría a la base de datos real del monolito
     */
    private List<MonolithUser> fetchMonolithUsers() {
        // Simulación de datos del monolito
        // En producción, esto haría una consulta a la base de datos del monolito
        List<MonolithUser> users = new ArrayList<>();
        
        // Datos de ejemplo que simularían venir del monolito
        users.add(MonolithUser.builder()
                .id(1L)
                .email("jorge.gangale@mtn.cl")
                .firstName("Jorge")
                .lastName("Gangale")
                .rut("12345678-9")
                .phone("+56912345678")
                .role("ADMIN")
                .educationalLevel("ALL_LEVELS")
                .subject("ALL_SUBJECTS")
                .enabled(true)
                .emailVerified(true)
                .createdAt(Instant.now().minusSeconds(2592000)) // 30 días atrás
                .lastLoginAt(Instant.now().minusSeconds(86400)) // 1 día atrás
                .build());
        
        // Agregar más usuarios de ejemplo...
        users.addAll(createExampleUsers());
        
        log.info("Obtenidos {} usuarios del monolito", users.size());
        return users;
    }

    /**
     * Convierte un usuario del monolito al modelo del microservicio
     */
    private User convertToUser(MonolithUser monolithUser) {
        return User.builder()
                .email(monolithUser.getEmail())
                .firstName(monolithUser.getFirstName())
                .lastName(monolithUser.getLastName())
                .rut(monolithUser.getRut())
                .phone(monolithUser.getPhone())
                .educationalLevel(monolithUser.getEducationalLevel())
                .subject(monolithUser.getSubject())
                .enabled(monolithUser.getEnabled())
                .emailVerified(monolithUser.getEmailVerified())
                .lastLoginAt(monolithUser.getLastLoginAt())
                .accountNonExpired(true)
                .credentialsNonExpired(true)
                .accountNonLocked(true)
                .loginAttempts(0)
                .build();
    }

    /**
     * Actualiza un usuario existente con datos del monolito
     */
    private void updateUserFromMonolith(User existing, MonolithUser monolithUser) {
        existing.setFirstName(monolithUser.getFirstName());
        existing.setLastName(monolithUser.getLastName());
        existing.setRut(monolithUser.getRut());
        existing.setPhone(monolithUser.getPhone());
        existing.setEducationalLevel(monolithUser.getEducationalLevel());
        existing.setSubject(monolithUser.getSubject());
        existing.setEnabled(monolithUser.getEnabled());
        existing.setEmailVerified(monolithUser.getEmailVerified());
        
        if (monolithUser.getLastLoginAt() != null && 
            (existing.getLastLoginAt() == null || monolithUser.getLastLoginAt().isAfter(existing.getLastLoginAt()))) {
            existing.setLastLoginAt(monolithUser.getLastLoginAt());
        }
    }

    /**
     * Crea un mapa de roles para búsqueda rápida
     */
    private Map<String, Role> createRoleMap() {
        List<Role> roles = roleRepository.findAll();
        Map<String, Role> roleMap = new HashMap<>();
        
        for (Role role : roles) {
            roleMap.put(role.getName(), role);
        }
        
        return roleMap;
    }

    /**
     * Valida la integridad de los datos migrados
     */
    private void validateDataIntegrity() {
        log.info("Validando integridad de datos...");
        
        // Verificar que no hay user_roles huérfanos
        long orphanedUserRoles = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM user_roles ur " +
            "LEFT JOIN users u ON ur.user_id = u.id " +
            "LEFT JOIN roles r ON ur.role_id = r.id " +
            "WHERE u.id IS NULL OR r.id IS NULL",
            Long.class
        );
        
        if (orphanedUserRoles > 0) {
            throw new MigrationException("Encontradas " + orphanedUserRoles + " asignaciones de rol huérfanas");
        }
        
        // Verificar que todos los usuarios tienen al menos un rol
        long usersWithoutRoles = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM users u " +
            "LEFT JOIN user_roles ur ON u.id = ur.user_id AND ur.active = true " +
            "WHERE ur.user_id IS NULL AND u.email != 'system@mtn.cl'",
            Long.class
        );
        
        if (usersWithoutRoles > 0) {
            log.warn("Encontrados {} usuarios sin roles asignados", usersWithoutRoles);
        }
        
        log.info("Validación de integridad completada");
    }

    /**
     * Registra un evento de migración completada
     */
    private void recordMigrationEvent(MigrationResult result) {
        try {
            jdbcTemplate.update(
                "INSERT INTO domain_events (aggregate_type, aggregate_id, event_type, event_data, correlation_id) " +
                "VALUES (?, ?, ?, ?::jsonb, ?)",
                "USER_MIGRATION",
                UUID.randomUUID(),
                "MIGRATION_COMPLETED",
                String.format("{\"migratedUsers\": %d, \"migratedRoleAssignments\": %d, \"durationMs\": %d, \"timestamp\": \"%s\"}", 
                    result.getMigratedUsers(), result.getMigratedRoleAssignments(), 
                    result.getDurationMs(), result.getEndTime()),
                UUID.randomUUID()
            );
        } catch (Exception e) {
            log.warn("No se pudo registrar evento de migración: {}", e.getMessage());
        }
    }

    /**
     * Verifica si la migración está habilitada
     */
    private boolean isMigrationEnabled() {
        return environment.getProperty(MIGRATION_PROPERTY, Boolean.class, false);
    }

    /**
     * Crea usuarios de ejemplo para demostración
     */
    private List<MonolithUser> createExampleUsers() {
        return Arrays.asList(
            MonolithUser.builder()
                .id(2L)
                .email("maria.gonzalez@mtn.cl")
                .firstName("María")
                .lastName("González")
                .rut("11111111-1")
                .phone("+56911111111")
                .role("TEACHER")
                .educationalLevel("PRESCHOOL")
                .subject("GENERAL")
                .enabled(true)
                .emailVerified(true)
                .createdAt(Instant.now().minusSeconds(2160000))
                .lastLoginAt(Instant.now().minusSeconds(172800))
                .build(),
                
            MonolithUser.builder()
                .id(3L)
                .email("familia01@test.cl")
                .firstName("Carlos")
                .lastName("Pérez")
                .rut("77777777-7")
                .phone("+56977777777")
                .role("APODERADO")
                .enabled(true)
                .emailVerified(true)
                .createdAt(Instant.now().minusSeconds(432000))
                .lastLoginAt(Instant.now().minusSeconds(432000))
                .build()
        );
    }

    /**
     * Excepción específica para errores de migración
     */
    public static class MigrationException extends RuntimeException {
        public MigrationException(String message) {
            super(message);
        }
        
        public MigrationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}