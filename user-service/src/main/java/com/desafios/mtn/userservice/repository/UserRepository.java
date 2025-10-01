// user-service/src/main/java/com/desafios/mtn/userservice/repository/UserRepository.java

package com.desafios.mtn.userservice.repository;

import com.desafios.mtn.userservice.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {

    // === BÚSQUEDAS BÁSICAS ===

    /**
     * Encuentra un usuario por email (case insensitive)
     */
    @Query("SELECT u FROM User u WHERE LOWER(u.email) = LOWER(:email)")
    Optional<User> findByEmailIgnoreCase(@Param("email") String email);

    /**
     * Encuentra un usuario por username (case insensitive)
     */
    @Query("SELECT u FROM User u WHERE LOWER(u.username) = LOWER(:username)")
    Optional<User> findByUsernameIgnoreCase(@Param("username") String username);

    /**
     * Encuentra un usuario por RUT
     */
    Optional<User> findByRut(String rut);

    /**
     * Encuentra un usuario por email o username
     */
    @Query("SELECT u FROM User u WHERE LOWER(u.email) = LOWER(:identifier) OR LOWER(u.username) = LOWER(:identifier)")
    Optional<User> findByEmailOrUsername(@Param("identifier") String identifier);

    // === VERIFICACIONES DE EXISTENCIA ===

    /**
     * Verifica si existe un email (case insensitive)
     */
    @Query("SELECT COUNT(u) > 0 FROM User u WHERE LOWER(u.email) = LOWER(:email)")
    boolean existsByEmailIgnoreCase(@Param("email") String email);

    /**
     * Verifica si existe un username (case insensitive)
     */
    @Query("SELECT COUNT(u) > 0 FROM User u WHERE LOWER(u.username) = LOWER(:username)")
    boolean existsByUsernameIgnoreCase(@Param("username") String username);

    /**
     * Verifica si existe un RUT
     */
    boolean existsByRut(String rut);

    /**
     * Verifica si existe otro usuario con el mismo email (para actualizaciones)
     */
    @Query("SELECT COUNT(u) > 0 FROM User u WHERE LOWER(u.email) = LOWER(:email) AND u.id != :userId")
    boolean existsByEmailIgnoreCaseAndIdNot(@Param("email") String email, @Param("userId") UUID userId);

    /**
     * Verifica si existe otro usuario con el mismo username (para actualizaciones)
     */
    @Query("SELECT COUNT(u) > 0 FROM User u WHERE LOWER(u.username) = LOWER(:username) AND u.id != :userId")
    boolean existsByUsernameIgnoreCaseAndIdNot(@Param("username") String username, @Param("userId") UUID userId);

    // === BÚSQUEDAS POR ESTADO ===

    /**
     * Encuentra usuarios activos
     */
    @Query("SELECT u FROM User u WHERE u.enabled = true AND u.accountNonLocked = true")
    Page<User> findActiveUsers(Pageable pageable);

    /**
     * Encuentra usuarios inactivos
     */
    @Query("SELECT u FROM User u WHERE u.enabled = false OR u.accountNonLocked = false")
    Page<User> findInactiveUsers(Pageable pageable);

    /**
     * Encuentra usuarios con email no verificado
     */
    Page<User> findByEmailVerifiedFalse(Pageable pageable);

    /**
     * Encuentra usuarios bloqueados
     */
    Page<User> findByAccountNonLockedFalse(Pageable pageable);

    // === BÚSQUEDAS CON FILTROS ===

    /**
     * Búsqueda de texto libre en nombre, email y username
     */
    @Query("SELECT u FROM User u WHERE " +
           "LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<User> findByTextSearch(@Param("query") String query, Pageable pageable);

    /**
     * Encuentra usuarios por rol
     */
    @Query("SELECT DISTINCT u FROM User u JOIN u.userRoles ur JOIN ur.role r WHERE r.name = :roleName AND ur.active = true")
    Page<User> findByRoleName(@Param("roleName") String roleName, Pageable pageable);

    /**
     * Encuentra usuarios por nivel educativo
     */
    Page<User> findByEducationalLevel(User.EducationalLevel educationalLevel, Pageable pageable);

    /**
     * Encuentra usuarios por materia
     */
    Page<User> findBySubject(User.Subject subject, Pageable pageable);

    // === BÚSQUEDAS COMBINADAS ===

    /**
     * Búsqueda combinada con filtros múltiples
     */
    @Query("SELECT DISTINCT u FROM User u " +
           "LEFT JOIN u.userRoles ur " +
           "LEFT JOIN ur.role r " +
           "WHERE (:query IS NULL OR " +
           "       LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "       LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "AND (:roleName IS NULL OR (r.name = :roleName AND ur.active = true)) " +
           "AND (:enabled IS NULL OR u.enabled = :enabled) " +
           "AND (:emailVerified IS NULL OR u.emailVerified = :emailVerified)")
    Page<User> findWithFilters(@Param("query") String query,
                              @Param("roleName") String roleName,
                              @Param("enabled") Boolean enabled,
                              @Param("emailVerified") Boolean emailVerified,
                              Pageable pageable);

    // === CONSULTAS DE ESTADÍSTICAS ===

    /**
     * Cuenta usuarios por rol
     */
    @Query("SELECT COUNT(DISTINCT u) FROM User u JOIN u.userRoles ur JOIN ur.role r WHERE r.name = :roleName AND ur.active = true")
    long countByRoleName(@Param("roleName") String roleName);

    /**
     * Cuenta usuarios activos
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.enabled = true AND u.accountNonLocked = true")
    long countActiveUsers();

    /**
     * Cuenta usuarios creados en un período
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt BETWEEN :startDate AND :endDate")
    long countCreatedBetween(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    /**
     * Obtiene estadísticas de login por período
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.lastLoginAt BETWEEN :startDate AND :endDate")
    long countLoginsBetween(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    // === OPERACIONES DE ACTUALIZACIÓN ===

    /**
     * Actualiza fecha de último login
     */
    @Modifying
    @Query("UPDATE User u SET u.lastLoginAt = :lastLoginAt, u.loginAttempts = 0 WHERE u.id = :userId")
    int updateLastLogin(@Param("userId") UUID userId, @Param("lastLoginAt") Instant lastLoginAt);

    /**
     * Incrementa intentos de login
     */
    @Modifying
    @Query("UPDATE User u SET u.loginAttempts = u.loginAttempts + 1 WHERE u.id = :userId")
    int incrementLoginAttempts(@Param("userId") UUID userId);

    /**
     * Bloquea cuenta de usuario
     */
    @Modifying
    @Query("UPDATE User u SET u.accountNonLocked = false WHERE u.id = :userId")
    int lockUserAccount(@Param("userId") UUID userId);

    /**
     * Desbloquea cuenta de usuario
     */
    @Modifying
    @Query("UPDATE User u SET u.accountNonLocked = true, u.loginAttempts = 0 WHERE u.id = :userId")
    int unlockUserAccount(@Param("userId") UUID userId);

    /**
     * Verifica email de usuario
     */
    @Modifying
    @Query("UPDATE User u SET u.emailVerified = true WHERE u.id = :userId")
    int verifyUserEmail(@Param("userId") UUID userId);

    /**
     * Habilita/deshabilita usuario
     */
    @Modifying
    @Query("UPDATE User u SET u.enabled = :enabled WHERE u.id = :userId")
    int updateUserStatus(@Param("userId") UUID userId, @Param("enabled") boolean enabled);

    // === CONSULTAS DE LIMPIEZA ===

    /**
     * Encuentra usuarios inactivos por mucho tiempo (para limpieza)
     */
    @Query("SELECT u FROM User u WHERE u.enabled = false AND u.updatedAt < :cutoffDate")
    List<User> findInactiveUsersBefore(@Param("cutoffDate") Instant cutoffDate);

    /**
     * Encuentra usuarios sin login reciente
     */
    @Query("SELECT u FROM User u WHERE u.lastLoginAt IS NULL OR u.lastLoginAt < :cutoffDate")
    List<User> findUsersWithoutRecentLogin(@Param("cutoffDate") Instant cutoffDate);

    // === CONSULTAS PARA REPORTES ===

    /**
     * Obtiene usuarios con sus roles para reportes
     */
    @Query("SELECT DISTINCT u FROM User u " +
           "LEFT JOIN FETCH u.userRoles ur " +
           "LEFT JOIN FETCH ur.role r " +
           "WHERE ur.active = true OR ur IS NULL " +
           "ORDER BY u.lastName, u.firstName")
    List<User> findAllWithRoles();

    /**
     * Encuentra usuarios por múltiples criterios para exportación
     */
    @Query("SELECT u FROM User u WHERE " +
           "(:enabled IS NULL OR u.enabled = :enabled) AND " +
           "(:emailVerified IS NULL OR u.emailVerified = :emailVerified) AND " +
           "(:educationalLevel IS NULL OR u.educationalLevel = :educationalLevel) AND " +
           "(:subject IS NULL OR u.subject = :subject) " +
           "ORDER BY u.lastName, u.firstName")
    List<User> findForExport(@Param("enabled") Boolean enabled,
                            @Param("emailVerified") Boolean emailVerified,
                            @Param("educationalLevel") User.EducationalLevel educationalLevel,
                            @Param("subject") User.Subject subject);
}