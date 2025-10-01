// user-service/src/main/java/com/desafios/mtn/userservice/repository/RoleRepository.java

package com.desafios.mtn.userservice.repository;

import com.desafios.mtn.userservice.domain.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {

    // === BÚSQUEDAS BÁSICAS ===

    /**
     * Encuentra un rol por nombre (case insensitive)
     */
    @Query("SELECT r FROM Role r WHERE LOWER(r.name) = LOWER(:name)")
    Optional<Role> findByNameIgnoreCase(@Param("name") String name);

    /**
     * Encuentra un rol por nombre (case sensitive)
     */
    Optional<Role> findByName(String name);

    /**
     * Encuentra roles por nombres
     */
    @Query("SELECT r FROM Role r WHERE r.name IN :names")
    List<Role> findByNameIn(@Param("names") Set<String> names);

    /**
     * Encuentra rol por display name
     */
    @Query("SELECT r FROM Role r WHERE LOWER(r.displayName) = LOWER(:displayName)")
    Optional<Role> findByDisplayNameIgnoreCase(@Param("displayName") String displayName);

    // === VERIFICACIONES DE EXISTENCIA ===

    /**
     * Verifica si existe un rol con el nombre dado (case insensitive)
     */
    @Query("SELECT COUNT(r) > 0 FROM Role r WHERE LOWER(r.name) = LOWER(:name)")
    boolean existsByNameIgnoreCase(@Param("name") String name);

    /**
     * Verifica si existe otro rol con el mismo nombre (para actualizaciones)
     */
    @Query("SELECT COUNT(r) > 0 FROM Role r WHERE LOWER(r.name) = LOWER(:name) AND r.id != :roleId")
    boolean existsByNameIgnoreCaseAndIdNot(@Param("name") String name, @Param("roleId") UUID roleId);

    /**
     * Verifica si existe un display name
     */
    @Query("SELECT COUNT(r) > 0 FROM Role r WHERE LOWER(r.displayName) = LOWER(:displayName)")
    boolean existsByDisplayNameIgnoreCase(@Param("displayName") String displayName);

    // === BÚSQUEDAS POR ESTADO ===

    /**
     * Encuentra roles activos
     */
    Page<Role> findByEnabledTrue(Pageable pageable);

    /**
     * Encuentra roles inactivos
     */
    Page<Role> findByEnabledFalse(Pageable pageable);

    /**
     * Encuentra roles del sistema
     */
    Page<Role> findBySystemRoleTrue(Pageable pageable);

    /**
     * Encuentra roles no del sistema (personalizados)
     */
    Page<Role> findBySystemRoleFalse(Pageable pageable);

    /**
     * Obtiene todos los roles activos
     */
    @Query("SELECT r FROM Role r WHERE r.enabled = true ORDER BY r.displayName")
    List<Role> findAllActiveRoles();

    /**
     * Obtiene todos los roles del sistema activos
     */
    @Query("SELECT r FROM Role r WHERE r.enabled = true AND r.systemRole = true ORDER BY r.name")
    List<Role> findAllActiveSystemRoles();

    // === BÚSQUEDAS POR CATEGORÍA ===

    /**
     * Encuentra roles por categoría
     */
    Page<Role> findByCategory(Role.RoleCategory category, Pageable pageable);

    /**
     * Encuentra roles activos por categoría
     */
    @Query("SELECT r FROM Role r WHERE r.category = :category AND r.enabled = true ORDER BY r.displayName")
    List<Role> findActiveByCategoryOrderByDisplayName(@Param("category") Role.RoleCategory category);

    // === BÚSQUEDAS CON FILTROS ===

    /**
     * Búsqueda de texto libre en nombre y descripción
     */
    @Query("SELECT r FROM Role r WHERE " +
           "LOWER(r.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(r.displayName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(r.description) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Role> findByTextSearch(@Param("query") String query, Pageable pageable);

    /**
     * Búsqueda combinada con filtros múltiples
     */
    @Query("SELECT r FROM Role r WHERE " +
           "(:query IS NULL OR " +
           " LOWER(r.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(r.displayName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(r.description) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "AND (:category IS NULL OR r.category = :category) " +
           "AND (:enabled IS NULL OR r.enabled = :enabled) " +
           "AND (:systemRole IS NULL OR r.systemRole = :systemRole)")
    Page<Role> findWithFilters(@Param("query") String query,
                              @Param("category") Role.RoleCategory category,
                              @Param("enabled") Boolean enabled,
                              @Param("systemRole") Boolean systemRole,
                              Pageable pageable);

    // === CONSULTAS CON USUARIOS ===

    /**
     * Encuentra roles con cantidad de usuarios asignados
     */
    @Query("SELECT r, COUNT(ur.user) as userCount FROM Role r " +
           "LEFT JOIN r.userRoles ur " +
           "WHERE ur.active = true OR ur IS NULL " +
           "GROUP BY r " +
           "ORDER BY r.displayName")
    List<Object[]> findRolesWithUserCount();

    /**
     * Encuentra roles sin usuarios asignados
     */
    @Query("SELECT r FROM Role r WHERE r.id NOT IN " +
           "(SELECT DISTINCT ur.role.id FROM UserRole ur WHERE ur.active = true)")
    List<Role> findRolesWithoutUsers();

    /**
     * Encuentra roles con al menos un usuario asignado
     */
    @Query("SELECT DISTINCT r FROM Role r JOIN r.userRoles ur WHERE ur.active = true")
    List<Role> findRolesWithUsers();

    /**
     * Verifica si un rol tiene usuarios asignados
     */
    @Query("SELECT COUNT(ur) > 0 FROM UserRole ur WHERE ur.role.id = :roleId AND ur.active = true")
    boolean hasActiveUsers(@Param("roleId") UUID roleId);

    // === CONSULTAS DE ESTADÍSTICAS ===

    /**
     * Cuenta roles activos
     */
    @Query("SELECT COUNT(r) FROM Role r WHERE r.enabled = true")
    long countActiveRoles();

    /**
     * Cuenta roles del sistema
     */
    @Query("SELECT COUNT(r) FROM Role r WHERE r.systemRole = true")
    long countSystemRoles();

    /**
     * Cuenta roles por categoría
     */
    @Query("SELECT COUNT(r) FROM Role r WHERE r.category = :category")
    long countByCategory(@Param("category") Role.RoleCategory category);

    /**
     * Cuenta roles creados en un período
     */
    @Query("SELECT COUNT(r) FROM Role r WHERE r.createdAt BETWEEN :startDate AND :endDate")
    long countCreatedBetween(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    // === OPERACIONES DE ACTUALIZACIÓN ===

    /**
     * Habilita/deshabilita rol
     */
    @Modifying
    @Query("UPDATE Role r SET r.enabled = :enabled WHERE r.id = :roleId")
    int updateRoleStatus(@Param("roleId") UUID roleId, @Param("enabled") boolean enabled);

    /**
     * Actualiza descripción de rol
     */
    @Modifying
    @Query("UPDATE Role r SET r.description = :description WHERE r.id = :roleId")
    int updateRoleDescription(@Param("roleId") UUID roleId, @Param("description") String description);

    // === CONSULTAS DE SEGURIDAD ===

    /**
     * Encuentra roles que se pueden eliminar (no son del sistema y no tienen usuarios)
     */
    @Query("SELECT r FROM Role r WHERE r.systemRole = false AND r.id NOT IN " +
           "(SELECT DISTINCT ur.role.id FROM UserRole ur WHERE ur.active = true)")
    List<Role> findDeletableRoles();

    /**
     * Verifica si un rol se puede eliminar
     */
    @Query("SELECT CASE WHEN (r.systemRole = false AND " +
           "(SELECT COUNT(ur) FROM UserRole ur WHERE ur.role = r AND ur.active = true) = 0) " +
           "THEN true ELSE false END " +
           "FROM Role r WHERE r.id = :roleId")
    boolean canDeleteRole(@Param("roleId") UUID roleId);

    // === CONSULTAS PARA MIGRACIONES ===

    /**
     * Obtiene todos los roles ordenados por nombre para exportación/migración
     */
    @Query("SELECT r FROM Role r ORDER BY r.name")
    List<Role> findAllOrderByName();

    /**
     * Encuentra roles por múltiples IDs
     */
    @Query("SELECT r FROM Role r WHERE r.id IN :roleIds")
    List<Role> findByIdIn(@Param("roleIds") Set<UUID> roleIds);

    // === CONSULTAS PARA REPORTES ===

    /**
     * Obtiene estadísticas completas de roles
     */
    @Query("SELECT r.category as category, " +
           "COUNT(r) as totalRoles, " +
           "SUM(CASE WHEN r.enabled = true THEN 1 ELSE 0 END) as activeRoles, " +
           "SUM(CASE WHEN r.systemRole = true THEN 1 ELSE 0 END) as systemRoles " +
           "FROM Role r GROUP BY r.category ORDER BY r.category")
    List<Object[]> getRoleStatisticsByCategory();

    /**
     * Obtiene roles con información de usuarios para reportes
     */
    @Query("SELECT r, " +
           "COUNT(DISTINCT ur.user) as userCount, " +
           "COUNT(DISTINCT CASE WHEN u.enabled = true THEN ur.user END) as activeUserCount " +
           "FROM Role r " +
           "LEFT JOIN r.userRoles ur " +
           "LEFT JOIN ur.user u " +
           "WHERE ur.active = true OR ur IS NULL " +
           "GROUP BY r " +
           "ORDER BY r.category, r.displayName")
    List<Object[]> findRolesWithUserStatistics();
}