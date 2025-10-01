// user-service/src/main/java/com/desafios/mtn/userservice/domain/Role.java

package com.desafios.mtn.userservice.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "roles", indexes = {
    @Index(name = "idx_roles_name", columnList = "name", unique = true),
    @Index(name = "idx_roles_enabled", columnList = "enabled")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@ToString(exclude = {"userRoles"})
@EqualsAndHashCode(of = {"id", "name"})
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name", nullable = false, unique = true, length = 50)
    @NotBlank(message = "Nombre del rol es obligatorio")
    @Size(max = 50, message = "Nombre del rol no puede exceder 50 caracteres")
    private String name;

    @Column(name = "display_name", nullable = false, length = 100)
    @NotBlank(message = "Nombre de visualización es obligatorio")
    @Size(max = 100, message = "Nombre de visualización no puede exceder 100 caracteres")
    private String displayName;

    @Column(name = "description", length = 500)
    @Size(max = 500, message = "Descripción no puede exceder 500 caracteres")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    @Builder.Default
    private RoleCategory category = RoleCategory.SYSTEM;

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @Column(name = "system_role", nullable = false)
    @Builder.Default
    private Boolean systemRole = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    // Relación con usuarios (Many-to-Many)
    @OneToMany(mappedBy = "role", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<UserRole> userRoles = new HashSet<>();

    // === MÉTODOS DE NEGOCIO ===

    /**
     * Verifica si el rol está activo
     */
    public boolean isActive() {
        return enabled;
    }

    /**
     * Verifica si es un rol del sistema (no se puede eliminar)
     */
    public boolean isSystemRole() {
        return systemRole;
    }

    /**
     * Obtiene la cantidad de usuarios con este rol
     */
    public int getUserCount() {
        return userRoles != null ? userRoles.size() : 0;
    }

    /**
     * Verifica si el rol tiene usuarios asignados
     */
    public boolean hasUsers() {
        return getUserCount() > 0;
    }

    // === ENUMS ===

    public enum RoleCategory {
        SYSTEM("Sistema"),
        EDUCATIONAL("Educativo"),
        ADMINISTRATIVE("Administrativo"),
        FAMILY("Familia");

        private final String displayName;

        RoleCategory(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // === CONSTANTES DE ROLES DEL SISTEMA ===

    public static final String ADMIN = "ADMIN";
    public static final String TEACHER = "TEACHER";
    public static final String COORDINATOR = "COORDINATOR";
    public static final String PSYCHOLOGIST = "PSYCHOLOGIST";
    public static final String CYCLE_DIRECTOR = "CYCLE_DIRECTOR";
    public static final String APODERADO = "APODERADO";

    // === BUILDERS DE ROLES PREDEFINIDOS ===

    /**
     * Crea el rol de Administrador
     */
    public static Role createAdminRole() {
        return Role.builder()
                .name(ADMIN)
                .displayName("Administrador")
                .description("Administrador del sistema con acceso completo")
                .category(RoleCategory.SYSTEM)
                .systemRole(true)
                .enabled(true)
                .build();
    }

    /**
     * Crea el rol de Profesor
     */
    public static Role createTeacherRole() {
        return Role.builder()
                .name(TEACHER)
                .displayName("Profesor")
                .description("Profesor con acceso a evaluaciones académicas")
                .category(RoleCategory.EDUCATIONAL)
                .systemRole(true)
                .enabled(true)
                .build();
    }

    /**
     * Crea el rol de Coordinador
     */
    public static Role createCoordinatorRole() {
        return Role.builder()
                .name(COORDINATOR)
                .displayName("Coordinador")
                .description("Coordinador académico con supervisión de evaluaciones")
                .category(RoleCategory.EDUCATIONAL)
                .systemRole(true)
                .enabled(true)
                .build();
    }

    /**
     * Crea el rol de Psicólogo
     */
    public static Role createPsychologistRole() {
        return Role.builder()
                .name(PSYCHOLOGIST)
                .displayName("Psicólogo")
                .description("Psicólogo escolar para evaluaciones psicológicas")
                .category(RoleCategory.EDUCATIONAL)
                .systemRole(true)
                .enabled(true)
                .build();
    }

    /**
     * Crea el rol de Director de Ciclo
     */
    public static Role createCycleDirectorRole() {
        return Role.builder()
                .name(CYCLE_DIRECTOR)
                .displayName("Director de Ciclo")
                .description("Director de ciclo para entrevistas y decisiones finales")
                .category(RoleCategory.ADMINISTRATIVE)
                .systemRole(true)
                .enabled(true)
                .build();
    }

    /**
     * Crea el rol de Apoderado
     */
    public static Role createApoderadoRole() {
        return Role.builder()
                .name(APODERADO)
                .displayName("Apoderado")
                .description("Familia/Apoderado para postulaciones")
                .category(RoleCategory.FAMILY)
                .systemRole(true)
                .enabled(true)
                .build();
    }

    /**
     * Obtiene todos los roles predefinidos del sistema
     */
    public static Set<Role> getSystemRoles() {
        Set<Role> roles = new HashSet<>();
        roles.add(createAdminRole());
        roles.add(createTeacherRole());
        roles.add(createCoordinatorRole());
        roles.add(createPsychologistRole());
        roles.add(createCycleDirectorRole());
        roles.add(createApoderadoRole());
        return roles;
    }

    /**
     * Verifica si un nombre de rol es válido
     */
    public static boolean isValidRoleName(String roleName) {
        return roleName != null && 
               (ADMIN.equals(roleName) || 
                TEACHER.equals(roleName) || 
                COORDINATOR.equals(roleName) || 
                PSYCHOLOGIST.equals(roleName) || 
                CYCLE_DIRECTOR.equals(roleName) || 
                APODERADO.equals(roleName));
    }
}