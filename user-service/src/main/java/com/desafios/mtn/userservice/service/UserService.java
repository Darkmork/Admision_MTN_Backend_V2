// user-service/src/main/java/com/desafios/mtn/userservice/service/UserService.java

package com.desafios.mtn.userservice.service;

import com.desafios.mtn.userservice.domain.Role;
import com.desafios.mtn.userservice.domain.User;
import com.desafios.mtn.userservice.domain.UserRole;
import com.desafios.mtn.userservice.events.EventPublisher;
import com.desafios.mtn.userservice.events.EventTypes;
import com.desafios.mtn.userservice.exception.UserNotFoundException;
import com.desafios.mtn.userservice.exception.DuplicateResourceException;
import com.desafios.mtn.userservice.exception.ValidationException;
import com.desafios.mtn.userservice.repository.RoleRepository;
import com.desafios.mtn.userservice.repository.UserRepository;
import com.desafios.mtn.userservice.util.RutValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EventPublisher eventPublisher;
    private final RutValidator rutValidator;

    // === OPERACIONES CRUD ===

    /**
     * Crea un nuevo usuario
     */
    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    public User createUser(User user, Set<String> roleNames, UUID createdBy) {
        log.info("Creando nuevo usuario: {}", user.getEmail());

        // Validaciones de negocio
        validateUserForCreation(user);

        // Procesar password si está presente
        if (StringUtils.hasText(user.getPassword())) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }

        // Normalizar datos
        normalizeUserData(user);

        // Establecer metadatos
        user.setCreatedBy(createdBy);
        user.setUpdatedBy(createdBy);

        // Guardar usuario
        User savedUser = userRepository.save(user);

        // Asignar roles si se especifican
        if (roleNames != null && !roleNames.isEmpty()) {
            assignRolesToUser(savedUser, roleNames, createdBy);
        }

        // Publicar evento
        publishUserCreatedEvent(savedUser);

        log.info("Usuario creado exitosamente: {} con ID: {}", savedUser.getEmail(), savedUser.getId());
        return savedUser;
    }

    /**
     * Actualiza un usuario existente
     */
    @Transactional
    @CacheEvict(value = "users", key = "#userId")
    public User updateUser(UUID userId, User userUpdate, UUID updatedBy) {
        log.info("Actualizando usuario: {}", userId);

        User existingUser = findUserById(userId);

        // Validaciones de negocio para actualización
        validateUserForUpdate(userUpdate, existingUser);

        // Actualizar campos
        updateUserFields(existingUser, userUpdate);
        
        // Normalizar datos
        normalizeUserData(existingUser);

        // Establecer metadatos
        existingUser.setUpdatedBy(updatedBy);

        User savedUser = userRepository.save(existingUser);

        // Publicar evento
        publishUserUpdatedEvent(savedUser);

        log.info("Usuario actualizado exitosamente: {}", savedUser.getId());
        return savedUser;
    }

    /**
     * Elimina un usuario (soft delete)
     */
    @Transactional
    @CacheEvict(value = "users", key = "#userId")
    public void deleteUser(UUID userId, UUID deletedBy) {
        log.info("Eliminando usuario: {}", userId);

        User user = findUserById(userId);
        
        // Desactivar usuario en lugar de eliminar físicamente
        user.setEnabled(false);
        user.setUpdatedBy(deletedBy);
        
        userRepository.save(user);

        // Publicar evento
        publishUserDeletedEvent(user);

        log.info("Usuario eliminado exitosamente: {}", userId);
    }

    // === BÚSQUEDAS ===

    /**
     * Encuentra usuario por ID
     */
    @Cacheable(value = "users", key = "#userId")
    public User findUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado con ID: " + userId));
    }

    /**
     * Encuentra usuario por email
     */
    @Cacheable(value = "users", key = "#email")
    public Optional<User> findUserByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email);
    }

    /**
     * Encuentra usuario por username
     */
    public Optional<User> findUserByUsername(String username) {
        return userRepository.findByUsernameIgnoreCase(username);
    }

    /**
     * Encuentra usuario por email o username
     */
    public Optional<User> findUserByEmailOrUsername(String identifier) {
        return userRepository.findByEmailOrUsername(identifier);
    }

    /**
     * Búsqueda paginada con filtros
     */
    public Page<User> findUsers(String query, String roleName, Boolean enabled, 
                               Boolean emailVerified, Pageable pageable) {
        return userRepository.findWithFilters(query, roleName, enabled, emailVerified, pageable);
    }

    /**
     * Búsqueda con especificaciones dinámicas
     */
    public Page<User> findUsers(Specification<User> spec, Pageable pageable) {
        return userRepository.findAll(spec, pageable);
    }

    /**
     * Obtiene todos los usuarios activos
     */
    public Page<User> findActiveUsers(Pageable pageable) {
        return userRepository.findActiveUsers(pageable);
    }

    /**
     * Encuentra usuarios por rol
     */
    public Page<User> findUsersByRole(String roleName, Pageable pageable) {
        return userRepository.findByRoleName(roleName, pageable);
    }

    // === GESTIÓN DE ROLES ===

    /**
     * Asigna roles a un usuario
     */
    @Transactional
    @CacheEvict(value = "users", key = "#userId")
    public User assignRolesToUser(UUID userId, Set<String> roleNames, UUID assignedBy) {
        User user = findUserById(userId);
        return assignRolesToUser(user, roleNames, assignedBy);
    }

    /**
     * Asigna roles a un usuario (sobrecarga interna)
     */
    @Transactional
    public User assignRolesToUser(User user, Set<String> roleNames, UUID assignedBy) {
        log.info("Asignando roles {} al usuario: {}", roleNames, user.getId());

        Set<Role> rolesToAssign = findRolesByNames(roleNames);
        
        for (Role role : rolesToAssign) {
            // Verificar si el usuario ya tiene este rol activo
            boolean hasRole = user.getUserRoles().stream()
                    .anyMatch(ur -> ur.getRole().equals(role) && ur.isActive());
            
            if (!hasRole) {
                UserRole userRole = UserRole.createPermanentAssignment(user, role, assignedBy);
                user.getUserRoles().add(userRole);
            }
        }

        User savedUser = userRepository.save(user);
        
        // Publicar evento de cambio de roles
        publishRoleChangedEvent(savedUser, roleNames, "ASSIGNED");

        return savedUser;
    }

    /**
     * Remueve roles de un usuario
     */
    @Transactional
    @CacheEvict(value = "users", key = "#userId")
    public User removeRolesFromUser(UUID userId, Set<String> roleNames, UUID removedBy) {
        log.info("Removiendo roles {} del usuario: {}", roleNames, userId);

        User user = findUserById(userId);
        Set<Role> rolesToRemove = findRolesByNames(roleNames);

        for (Role role : rolesToRemove) {
            user.getUserRoles().removeIf(ur -> ur.getRole().equals(role));
        }

        user.setUpdatedBy(removedBy);
        User savedUser = userRepository.save(user);

        // Publicar evento de cambio de roles
        publishRoleChangedEvent(savedUser, roleNames, "REMOVED");

        return savedUser;
    }

    /**
     * Reemplaza todos los roles de un usuario
     */
    @Transactional
    @CacheEvict(value = "users", key = "#userId")
    public User replaceUserRoles(UUID userId, Set<String> newRoleNames, UUID updatedBy) {
        log.info("Reemplazando roles del usuario: {} con: {}", userId, newRoleNames);

        User user = findUserById(userId);
        
        // Obtener roles actuales para comparación
        Set<String> currentRoleNames = user.getRoleNames();
        
        // Limpiar roles actuales
        user.getUserRoles().clear();
        
        // Asignar nuevos roles
        if (newRoleNames != null && !newRoleNames.isEmpty()) {
            assignRolesToUser(user, newRoleNames, updatedBy);
        }

        // Publicar evento con los roles que cambiaron
        Set<String> changedRoles = new HashSet<>(currentRoleNames);
        changedRoles.addAll(newRoleNames);
        publishRoleChangedEvent(user, changedRoles, "REPLACED");

        return user;
    }

    // === GESTIÓN DE ESTADO ===

    /**
     * Habilita/deshabilita un usuario
     */
    @Transactional
    @CacheEvict(value = "users", key = "#userId")
    public User updateUserStatus(UUID userId, boolean enabled, UUID updatedBy) {
        log.info("Actualizando estado del usuario: {} a: {}", userId, enabled);

        User user = findUserById(userId);
        boolean previousStatus = user.getEnabled();
        
        user.setEnabled(enabled);
        user.setUpdatedBy(updatedBy);
        
        User savedUser = userRepository.save(user);

        // Publicar evento de cambio de estado solo si cambió
        if (previousStatus != enabled) {
            publishUserStatusChangedEvent(savedUser, enabled);
        }

        return savedUser;
    }

    /**
     * Bloquea cuenta de usuario
     */
    @Transactional
    @CacheEvict(value = "users", key = "#userId")
    public User lockUserAccount(UUID userId, UUID updatedBy) {
        log.info("Bloqueando cuenta del usuario: {}", userId);

        User user = findUserById(userId);
        user.lockAccount();
        user.setUpdatedBy(updatedBy);
        
        User savedUser = userRepository.save(user);
        publishUserStatusChangedEvent(savedUser, false);

        return savedUser;
    }

    /**
     * Desbloquea cuenta de usuario
     */
    @Transactional
    @CacheEvict(value = "users", key = "#userId")
    public User unlockUserAccount(UUID userId, UUID updatedBy) {
        log.info("Desbloqueando cuenta del usuario: {}", userId);

        User user = findUserById(userId);
        user.unlockAccount();
        user.setUpdatedBy(updatedBy);
        
        User savedUser = userRepository.save(user);
        publishUserStatusChangedEvent(savedUser, true);

        return savedUser;
    }

    /**
     * Verifica email de usuario
     */
    @Transactional
    @CacheEvict(value = "users", key = "#userId")
    public User verifyUserEmail(UUID userId) {
        log.info("Verificando email del usuario: {}", userId);

        User user = findUserById(userId);
        user.setEmailVerified(true);
        
        User savedUser = userRepository.save(user);
        publishUserEmailVerifiedEvent(savedUser);

        return savedUser;
    }

    // === AUTENTICACIÓN Y SEGURIDAD ===

    /**
     * Actualiza último login del usuario
     */
    @Transactional
    @CacheEvict(value = "users", key = "#userId")
    public void updateLastLogin(UUID userId) {
        userRepository.updateLastLogin(userId, Instant.now());
    }

    /**
     * Incrementa intentos fallidos de login
     */
    @Transactional
    @CacheEvict(value = "users", key = "#userId")
    public void incrementLoginAttempts(UUID userId) {
        userRepository.incrementLoginAttempts(userId);
    }

    /**
     * Verifica si la cuenta debe ser bloqueada por intentos fallidos
     */
    @Transactional
    @CacheEvict(value = "users", key = "#userId")
    public boolean checkAndLockAccountIfNeeded(UUID userId, int maxAttempts) {
        User user = findUserById(userId);
        
        if (user.getLoginAttempts() >= maxAttempts) {
            user.lockAccount();
            userRepository.save(user);
            publishUserStatusChangedEvent(user, false);
            return true;
        }
        
        return false;
    }

    // === ESTADÍSTICAS ===

    /**
     * Cuenta usuarios por rol
     */
    public long countUsersByRole(String roleName) {
        return userRepository.countByRoleName(roleName);
    }

    /**
     * Cuenta usuarios activos
     */
    public long countActiveUsers() {
        return userRepository.countActiveUsers();
    }

    /**
     * Obtiene estadísticas de usuarios
     */
    public Map<String, Object> getUserStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("activeUsers", countActiveUsers());
        stats.put("verifiedUsers", userRepository.countByEmailVerified(true));
        stats.put("adminUsers", countUsersByRole(Role.ADMIN));
        stats.put("teacherUsers", countUsersByRole(Role.TEACHER));
        stats.put("apoderadoUsers", countUsersByRole(Role.APODERADO));
        return stats;
    }

    // === MÉTODOS AUXILIARES PRIVADOS ===

    private void validateUserForCreation(User user) {
        // Validar email único
        if (userRepository.existsByEmailIgnoreCase(user.getEmail())) {
            throw new DuplicateResourceException("Ya existe un usuario con el email: " + user.getEmail());
        }

        // Validar username único si está presente
        if (StringUtils.hasText(user.getUsername()) && 
            userRepository.existsByUsernameIgnoreCase(user.getUsername())) {
            throw new DuplicateResourceException("Ya existe un usuario con el username: " + user.getUsername());
        }

        // Validar RUT si está presente
        validateRut(user.getRut());
    }

    private void validateUserForUpdate(User userUpdate, User existingUser) {
        // Validar email único (excluyendo el usuario actual)
        if (!existingUser.getEmail().equalsIgnoreCase(userUpdate.getEmail()) &&
            userRepository.existsByEmailIgnoreCaseAndIdNot(userUpdate.getEmail(), existingUser.getId())) {
            throw new DuplicateResourceException("Ya existe otro usuario con el email: " + userUpdate.getEmail());
        }

        // Validar username único si está presente (excluyendo el usuario actual)
        if (StringUtils.hasText(userUpdate.getUsername()) &&
            !Objects.equals(existingUser.getUsername(), userUpdate.getUsername()) &&
            userRepository.existsByUsernameIgnoreCaseAndIdNot(userUpdate.getUsername(), existingUser.getId())) {
            throw new DuplicateResourceException("Ya existe otro usuario con el username: " + userUpdate.getUsername());
        }

        // Validar RUT si está presente
        validateRut(userUpdate.getRut());
    }

    private void validateRut(String rut) {
        if (StringUtils.hasText(rut) && !rutValidator.isValid(rut)) {
            throw new ValidationException("RUT inválido: " + rut);
        }
    }

    private void normalizeUserData(User user) {
        // Normalizar email a lowercase
        if (user.getEmail() != null) {
            user.setEmail(user.getEmail().toLowerCase().trim());
        }

        // Normalizar nombres
        if (user.getFirstName() != null) {
            user.setFirstName(user.getFirstName().trim());
        }
        if (user.getLastName() != null) {
            user.setLastName(user.getLastName().trim());
        }

        // Normalizar username
        if (user.getUsername() != null) {
            user.setUsername(user.getUsername().toLowerCase().trim());
        }

        // Normalizar RUT
        if (user.getRut() != null) {
            user.setRut(rutValidator.format(user.getRut()));
        }
    }

    private void updateUserFields(User existingUser, User userUpdate) {
        existingUser.setEmail(userUpdate.getEmail());
        existingUser.setFirstName(userUpdate.getFirstName());
        existingUser.setLastName(userUpdate.getLastName());
        
        if (userUpdate.getUsername() != null) {
            existingUser.setUsername(userUpdate.getUsername());
        }
        if (userUpdate.getRut() != null) {
            existingUser.setRut(userUpdate.getRut());
        }
        if (userUpdate.getPhone() != null) {
            existingUser.setPhone(userUpdate.getPhone());
        }
        if (userUpdate.getEducationalLevel() != null) {
            existingUser.setEducationalLevel(userUpdate.getEducationalLevel());
        }
        if (userUpdate.getSubject() != null) {
            existingUser.setSubject(userUpdate.getSubject());
        }
    }

    private Set<Role> findRolesByNames(Set<String> roleNames) {
        List<Role> foundRoles = roleRepository.findByNameIn(roleNames);
        
        Set<String> foundRoleNames = foundRoles.stream()
                .map(Role::getName)
                .collect(Collectors.toSet());
        
        Set<String> missingRoles = roleNames.stream()
                .filter(name -> !foundRoleNames.contains(name))
                .collect(Collectors.toSet());
        
        if (!missingRoles.isEmpty()) {
            throw new ValidationException("Roles no encontrados: " + missingRoles);
        }
        
        return new HashSet<>(foundRoles);
    }

    // === EVENTOS ===

    private void publishUserCreatedEvent(User user) {
        try {
            Map<String, Object> eventData = createUserEventData(user);
            eventPublisher.publishEvent(EventTypes.USER_CREATED, user.getId(), eventData);
        } catch (Exception e) {
            log.warn("Error al publicar evento USER_CREATED para usuario: {}", user.getId(), e);
        }
    }

    private void publishUserUpdatedEvent(User user) {
        try {
            Map<String, Object> eventData = createUserEventData(user);
            eventPublisher.publishEvent(EventTypes.USER_UPDATED, user.getId(), eventData);
        } catch (Exception e) {
            log.warn("Error al publicar evento USER_UPDATED para usuario: {}", user.getId(), e);
        }
    }

    private void publishUserDeletedEvent(User user) {
        try {
            Map<String, Object> eventData = Map.of(
                "userId", user.getId(),
                "email", user.getEmail(),
                "deletedAt", Instant.now()
            );
            eventPublisher.publishEvent(EventTypes.USER_DELETED, user.getId(), eventData);
        } catch (Exception e) {
            log.warn("Error al publicar evento USER_DELETED para usuario: {}", user.getId(), e);
        }
    }

    private void publishRoleChangedEvent(User user, Set<String> roleNames, String operation) {
        try {
            Map<String, Object> eventData = Map.of(
                "userId", user.getId(),
                "email", user.getEmail(),
                "roles", roleNames,
                "operation", operation,
                "changedAt", Instant.now()
            );
            eventPublisher.publishEvent(EventTypes.ROLE_CHANGED, user.getId(), eventData);
        } catch (Exception e) {
            log.warn("Error al publicar evento ROLE_CHANGED para usuario: {}", user.getId(), e);
        }
    }

    private void publishUserStatusChangedEvent(User user, boolean enabled) {
        try {
            Map<String, Object> eventData = Map.of(
                "userId", user.getId(),
                "email", user.getEmail(),
                "enabled", enabled,
                "changedAt", Instant.now()
            );
            eventPublisher.publishEvent(EventTypes.USER_STATUS_CHANGED, user.getId(), eventData);
        } catch (Exception e) {
            log.warn("Error al publicar evento USER_STATUS_CHANGED para usuario: {}", user.getId(), e);
        }
    }

    private void publishUserEmailVerifiedEvent(User user) {
        try {
            Map<String, Object> eventData = Map.of(
                "userId", user.getId(),
                "email", user.getEmail(),
                "verifiedAt", Instant.now()
            );
            eventPublisher.publishEvent(EventTypes.USER_EMAIL_VERIFIED, user.getId(), eventData);
        } catch (Exception e) {
            log.warn("Error al publicar evento USER_EMAIL_VERIFIED para usuario: {}", user.getId(), e);
        }
    }

    private Map<String, Object> createUserEventData(User user) {
        return Map.of(
            "userId", user.getId(),
            "email", user.getEmail(),
            "firstName", user.getFirstName(),
            "lastName", user.getLastName(),
            "roles", user.getRoleNames(),
            "enabled", user.getEnabled(),
            "emailVerified", user.getEmailVerified(),
            "educationalLevel", user.getEducationalLevel() != null ? user.getEducationalLevel().name() : null,
            "subject", user.getSubject() != null ? user.getSubject().name() : null
        );
    }
}