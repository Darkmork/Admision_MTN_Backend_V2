// user-service/src/main/java/com/desafios/mtn/userservice/service/RoleService.java

package com.desafios.mtn.userservice.service;

import com.desafios.mtn.userservice.domain.Role;
import com.desafios.mtn.userservice.exception.DuplicateResourceException;
import com.desafios.mtn.userservice.exception.RoleNotFoundException;
import com.desafios.mtn.userservice.exception.ValidationException;
import com.desafios.mtn.userservice.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class RoleService {

    private final RoleRepository roleRepository;

    // === OPERACIONES CRUD ===

    /**
     * Crea un nuevo rol
     */
    @Transactional
    @CacheEvict(value = "roles", allEntries = true)
    public Role createRole(Role role, UUID createdBy) {
        log.info("Creando nuevo rol: {}", role.getName());

        // Validaciones de negocio
        validateRoleForCreation(role);

        // Normalizar datos
        normalizeRoleData(role);

        // Establecer metadatos
        role.setCreatedBy(createdBy);
        role.setUpdatedBy(createdBy);

        Role savedRole = roleRepository.save(role);

        log.info("Rol creado exitosamente: {} con ID: {}", savedRole.getName(), savedRole.getId());
        return savedRole;
    }

    /**
     * Actualiza un rol existente
     */
    @Transactional
    @CacheEvict(value = "roles", key = "#roleId")
    public Role updateRole(UUID roleId, Role roleUpdate, UUID updatedBy) {
        log.info("Actualizando rol: {}", roleId);

        Role existingRole = findRoleById(roleId);

        // Verificar que no sea un rol del sistema
        if (existingRole.isSystemRole()) {
            throw new ValidationException("No se puede modificar un rol del sistema: " + existingRole.getName());
        }

        // Validaciones de negocio para actualización
        validateRoleForUpdate(roleUpdate, existingRole);

        // Actualizar campos
        updateRoleFields(existingRole, roleUpdate);
        
        // Normalizar datos
        normalizeRoleData(existingRole);

        // Establecer metadatos
        existingRole.setUpdatedBy(updatedBy);

        Role savedRole = roleRepository.save(existingRole);

        log.info("Rol actualizado exitosamente: {}", savedRole.getId());
        return savedRole;
    }

    /**
     * Elimina un rol
     */
    @Transactional
    @CacheEvict(value = "roles", key = "#roleId")
    public void deleteRole(UUID roleId) {
        log.info("Eliminando rol: {}", roleId);

        Role role = findRoleById(roleId);

        // Verificar que no sea un rol del sistema
        if (role.isSystemRole()) {
            throw new ValidationException("No se puede eliminar un rol del sistema: " + role.getName());
        }

        // Verificar que no tenga usuarios asignados
        if (roleRepository.hasActiveUsers(roleId)) {
            throw new ValidationException("No se puede eliminar un rol que tiene usuarios asignados: " + role.getName());
        }

        roleRepository.delete(role);

        log.info("Rol eliminado exitosamente: {}", role.getName());
    }

    // === BÚSQUEDAS ===

    /**
     * Encuentra rol por ID
     */
    @Cacheable(value = "roles", key = "#roleId")
    public Role findRoleById(UUID roleId) {
        return roleRepository.findById(roleId)
                .orElseThrow(() -> new RoleNotFoundException("Rol no encontrado con ID: " + roleId));
    }

    /**
     * Encuentra rol por nombre
     */
    @Cacheable(value = "roles", key = "#name")
    public Optional<Role> findRoleByName(String name) {
        return roleRepository.findByNameIgnoreCase(name);
    }

    /**
     * Obtiene rol por nombre (lanza excepción si no existe)
     */
    @Cacheable(value = "roles", key = "#name")
    public Role getRoleByName(String name) {
        return findRoleByName(name)
                .orElseThrow(() -> new RoleNotFoundException("Rol no encontrado: " + name));
    }

    /**
     * Encuentra roles por nombres
     */
    public List<Role> findRolesByNames(Set<String> names) {
        return roleRepository.findByNameIn(names);
    }

    /**
     * Búsqueda paginada con filtros
     */
    public Page<Role> findRoles(String query, Role.RoleCategory category, 
                               Boolean enabled, Boolean systemRole, Pageable pageable) {
        return roleRepository.findWithFilters(query, category, enabled, systemRole, pageable);
    }

    /**
     * Obtiene todos los roles activos
     */
    @Cacheable(value = "roles", key = "'all-active'")
    public List<Role> findAllActiveRoles() {
        return roleRepository.findAllActiveRoles();
    }

    /**
     * Obtiene todos los roles del sistema activos
     */
    @Cacheable(value = "roles", key = "'system-active'")
    public List<Role> findAllActiveSystemRoles() {
        return roleRepository.findAllActiveSystemRoles();
    }

    /**
     * Encuentra roles por categoría
     */
    public Page<Role> findRolesByCategory(Role.RoleCategory category, Pageable pageable) {
        return roleRepository.findByCategory(category, pageable);
    }

    /**
     * Encuentra roles activos por categoría
     */
    public List<Role> findActiveRolesByCategory(Role.RoleCategory category) {
        return roleRepository.findActiveByCategoryOrderByDisplayName(category);
    }

    // === GESTIÓN DE ESTADO ===

    /**
     * Habilita/deshabilita un rol
     */
    @Transactional
    @CacheEvict(value = "roles", key = "#roleId")
    public Role updateRoleStatus(UUID roleId, boolean enabled, UUID updatedBy) {
        log.info("Actualizando estado del rol: {} a: {}", roleId, enabled);

        Role role = findRoleById(roleId);

        // Verificar que no sea un rol del sistema si se intenta deshabilitar
        if (!enabled && role.isSystemRole()) {
            throw new ValidationException("No se puede deshabilitar un rol del sistema: " + role.getName());
        }

        role.setEnabled(enabled);
        role.setUpdatedBy(updatedBy);
        
        return roleRepository.save(role);
    }

    // === INICIALIZACIÓN DE ROLES DEL SISTEMA ===

    /**
     * Inicializa los roles del sistema si no existen
     */
    @Transactional
    public void initializeSystemRoles() {
        log.info("Inicializando roles del sistema");

        Set<Role> systemRoles = Role.getSystemRoles();
        
        for (Role systemRole : systemRoles) {
            if (!roleRepository.existsByNameIgnoreCase(systemRole.getName())) {
                roleRepository.save(systemRole);
                log.info("Rol del sistema creado: {}", systemRole.getName());
            }
        }
    }

    /**
     * Verifica y crea roles del sistema faltantes
     */
    @Transactional
    public void ensureSystemRolesExist() {
        List<String> systemRoleNames = Arrays.asList(
            Role.ADMIN, Role.TEACHER, Role.COORDINATOR, 
            Role.PSYCHOLOGIST, Role.CYCLE_DIRECTOR, Role.APODERADO
        );
        
        for (String roleName : systemRoleNames) {
            if (!roleRepository.existsByNameIgnoreCase(roleName)) {
                Role role = createSystemRoleByName(roleName);
                roleRepository.save(role);
                log.info("Rol del sistema creado: {}", roleName);
            }
        }
    }

    // === ESTADÍSTICAS Y REPORTES ===

    /**
     * Cuenta roles activos
     */
    public long countActiveRoles() {
        return roleRepository.countActiveRoles();
    }

    /**
     * Cuenta roles del sistema
     */
    public long countSystemRoles() {
        return roleRepository.countSystemRoles();
    }

    /**
     * Cuenta roles por categoría
     */
    public long countRolesByCategory(Role.RoleCategory category) {
        return roleRepository.countByCategory(category);
    }

    /**
     * Obtiene estadísticas de roles
     */
    public Map<String, Object> getRoleStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRoles", roleRepository.count());
        stats.put("activeRoles", countActiveRoles());
        stats.put("systemRoles", countSystemRoles());
        
        // Estadísticas por categoría
        for (Role.RoleCategory category : Role.RoleCategory.values()) {
            stats.put(category.name().toLowerCase() + "Roles", countRolesByCategory(category));
        }
        
        return stats;
    }

    /**
     * Obtiene roles con cantidad de usuarios
     */
    public List<Map<String, Object>> getRolesWithUserCount() {
        List<Object[]> results = roleRepository.findRolesWithUserCount();
        
        return results.stream()
                .map(result -> {
                    Role role = (Role) result[0];
                    Long userCount = (Long) result[1];
                    
                    Map<String, Object> roleInfo = new HashMap<>();
                    roleInfo.put("role", role);
                    roleInfo.put("userCount", userCount);
                    return roleInfo;
                })
                .collect(Collectors.toList());
    }

    /**
     * Obtiene estadísticas detalladas por categoría
     */
    public List<Map<String, Object>> getRoleStatisticsByCategory() {
        List<Object[]> results = roleRepository.getRoleStatisticsByCategory();
        
        return results.stream()
                .map(result -> {
                    Map<String, Object> categoryStats = new HashMap<>();
                    categoryStats.put("category", result[0]);
                    categoryStats.put("totalRoles", result[1]);
                    categoryStats.put("activeRoles", result[2]);
                    categoryStats.put("systemRoles", result[3]);
                    return categoryStats;
                })
                .collect(Collectors.toList());
    }

    // === VALIDACIÓN Y UTILIDADES ===

    /**
     * Verifica si un rol existe
     */
    public boolean roleExists(String roleName) {
        return roleRepository.existsByNameIgnoreCase(roleName);
    }

    /**
     * Verifica si un rol se puede eliminar
     */
    public boolean canDeleteRole(UUID roleId) {
        return roleRepository.canDeleteRole(roleId);
    }

    /**
     * Obtiene roles que se pueden eliminar
     */
    public List<Role> getDeletableRoles() {
        return roleRepository.findDeletableRoles();
    }

    /**
     * Verifica si un rol tiene usuarios asignados
     */
    public boolean hasUsers(UUID roleId) {
        return roleRepository.hasActiveUsers(roleId);
    }

    // === MÉTODOS AUXILIARES PRIVADOS ===

    private void validateRoleForCreation(Role role) {
        // Validar nombre único
        if (roleRepository.existsByNameIgnoreCase(role.getName())) {
            throw new DuplicateResourceException("Ya existe un rol con el nombre: " + role.getName());
        }

        // Validar display name único
        if (roleRepository.existsByDisplayNameIgnoreCase(role.getDisplayName())) {
            throw new DuplicateResourceException("Ya existe un rol con el nombre de visualización: " + role.getDisplayName());
        }

        // Validar que no use nombres reservados del sistema
        if (Role.isValidRoleName(role.getName().toUpperCase())) {
            throw new ValidationException("No se puede crear un rol con nombre reservado del sistema: " + role.getName());
        }
    }

    private void validateRoleForUpdate(Role roleUpdate, Role existingRole) {
        // Validar nombre único (excluyendo el rol actual)
        if (!existingRole.getName().equalsIgnoreCase(roleUpdate.getName()) &&
            roleRepository.existsByNameIgnoreCaseAndIdNot(roleUpdate.getName(), existingRole.getId())) {
            throw new DuplicateResourceException("Ya existe otro rol con el nombre: " + roleUpdate.getName());
        }

        // Validar que no use nombres reservados del sistema (solo si cambió)
        if (!existingRole.getName().equals(roleUpdate.getName()) &&
            Role.isValidRoleName(roleUpdate.getName().toUpperCase())) {
            throw new ValidationException("No se puede usar un nombre reservado del sistema: " + roleUpdate.getName());
        }
    }

    private void normalizeRoleData(Role role) {
        // Normalizar nombre (uppercase)
        if (role.getName() != null) {
            role.setName(role.getName().toUpperCase().trim().replace(" ", "_"));
        }

        // Normalizar display name
        if (role.getDisplayName() != null) {
            role.setDisplayName(role.getDisplayName().trim());
        }

        // Normalizar descripción
        if (role.getDescription() != null) {
            role.setDescription(role.getDescription().trim());
        }
    }

    private void updateRoleFields(Role existingRole, Role roleUpdate) {
        existingRole.setName(roleUpdate.getName());
        existingRole.setDisplayName(roleUpdate.getDisplayName());
        existingRole.setDescription(roleUpdate.getDescription());
        
        if (roleUpdate.getCategory() != null) {
            existingRole.setCategory(roleUpdate.getCategory());
        }
    }

    private Role createSystemRoleByName(String roleName) {
        return switch (roleName.toUpperCase()) {
            case Role.ADMIN -> Role.createAdminRole();
            case Role.TEACHER -> Role.createTeacherRole();
            case Role.COORDINATOR -> Role.createCoordinatorRole();
            case Role.PSYCHOLOGIST -> Role.createPsychologistRole();
            case Role.CYCLE_DIRECTOR -> Role.createCycleDirectorRole();
            case Role.APODERADO -> Role.createApoderadoRole();
            default -> throw new ValidationException("Rol del sistema desconocido: " + roleName);
        };
    }
}