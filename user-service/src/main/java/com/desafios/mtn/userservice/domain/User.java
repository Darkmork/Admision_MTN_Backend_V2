// user-service/src/main/java/com/desafios/mtn/userservice/domain/User.java

package com.desafios.mtn.userservice.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
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
@Table(name = "users", indexes = {
    @Index(name = "idx_users_email", columnList = "email"),
    @Index(name = "idx_users_username", columnList = "username"),
    @Index(name = "idx_users_rut", columnList = "rut"),
    @Index(name = "idx_users_enabled", columnList = "enabled"),
    @Index(name = "idx_users_created_at", columnList = "created_at")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@ToString(exclude = {"userRoles"})
@EqualsAndHashCode(of = {"id", "email"})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    @Email(message = "Email debe tener formato válido")
    @NotBlank(message = "Email es obligatorio")
    @Size(max = 255, message = "Email no puede exceder 255 caracteres")
    private String email;

    @Column(name = "username", unique = true, length = 100)
    @Size(max = 100, message = "Username no puede exceder 100 caracteres")
    private String username;

    @Column(name = "password", length = 255)
    @Size(max = 255, message = "Password no puede exceder 255 caracteres")
    private String password;

    @Column(name = "first_name", nullable = false, length = 100)
    @NotBlank(message = "Nombre es obligatorio")
    @Size(max = 100, message = "Nombre no puede exceder 100 caracteres")
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    @NotBlank(message = "Apellido es obligatorio")
    @Size(max = 100, message = "Apellido no puede exceder 100 caracteres")
    private String lastName;

    @Column(name = "rut", length = 12)
    @Size(max = 12, message = "RUT no puede exceder 12 caracteres")
    private String rut;

    @Column(name = "phone", length = 20)
    @Size(max = 20, message = "Teléfono no puede exceder 20 caracteres")
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "educational_level", length = 50)
    private EducationalLevel educationalLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "subject", length = 50)
    private Subject subject;

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private Boolean emailVerified = false;

    @Column(name = "account_non_expired", nullable = false)
    @Builder.Default
    private Boolean accountNonExpired = true;

    @Column(name = "credentials_non_expired", nullable = false)
    @Builder.Default
    private Boolean credentialsNonExpired = true;

    @Column(name = "account_non_locked", nullable = false)
    @Builder.Default
    private Boolean accountNonLocked = true;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "login_attempts", nullable = false)
    @Builder.Default
    private Integer loginAttempts = 0;

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

    // Relación con roles (Many-to-Many)
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<UserRole> userRoles = new HashSet<>();

    // === MÉTODOS DE NEGOCIO ===

    /**
     * Obtiene el nombre completo del usuario
     */
    public String getFullName() {
        return (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
    }

    /**
     * Verifica si el usuario está activo y puede acceder al sistema
     */
    public boolean isActive() {
        return enabled && accountNonExpired && accountNonLocked && credentialsNonExpired;
    }

    /**
     * Verifica si el usuario tiene un rol específico
     */
    public boolean hasRole(String roleName) {
        return userRoles.stream()
                .anyMatch(ur -> ur.getRole().getName().equalsIgnoreCase(roleName));
    }

    /**
     * Obtiene todos los nombres de roles del usuario
     */
    public Set<String> getRoleNames() {
        return userRoles.stream()
                .map(ur -> ur.getRole().getName())
                .collect(java.util.stream.Collectors.toSet());
    }

    /**
     * Agrega un rol al usuario
     */
    public void addRole(Role role) {
        UserRole userRole = UserRole.builder()
                .user(this)
                .role(role)
                .assignedAt(Instant.now())
                .build();
        userRoles.add(userRole);
    }

    /**
     * Remueve un rol del usuario
     */
    public void removeRole(Role role) {
        userRoles.removeIf(ur -> ur.getRole().equals(role));
    }

    /**
     * Actualiza la fecha del último login
     */
    public void updateLastLogin() {
        this.lastLoginAt = Instant.now();
        this.loginAttempts = 0;
    }

    /**
     * Incrementa intentos de login fallidos
     */
    public void incrementLoginAttempts() {
        this.loginAttempts++;
    }

    /**
     * Bloquea la cuenta del usuario
     */
    public void lockAccount() {
        this.accountNonLocked = false;
        this.updatedAt = Instant.now();
    }

    /**
     * Desbloquea la cuenta del usuario
     */
    public void unlockAccount() {
        this.accountNonLocked = true;
        this.loginAttempts = 0;
        this.updatedAt = Instant.now();
    }

    // === ENUMS ===

    public enum EducationalLevel {
        PRESCHOOL("Preescolar"),
        BASIC("Básica"),
        HIGH_SCHOOL("Media"),
        ALL_LEVELS("Todos los niveles");

        private final String displayName;

        EducationalLevel(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum Subject {
        GENERAL("General"),
        LANGUAGE("Lenguaje"),
        MATHEMATICS("Matemáticas"),
        ENGLISH("Inglés"),
        SCIENCE("Ciencias"),
        HISTORY("Historia"),
        ARTS("Artes"),
        PHYSICAL_EDUCATION("Educación Física"),
        TECHNOLOGY("Tecnología"),
        RELIGION("Religión"),
        ALL_SUBJECTS("Todas las materias");

        private final String displayName;

        Subject(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // === BUILDERS PERSONALIZADOS ===

    /**
     * Builder para crear usuarios básicos (familia/apoderados)
     */
    public static User createBasicUser(String email, String firstName, String lastName) {
        return User.builder()
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .enabled(true)
                .emailVerified(false)
                .accountNonExpired(true)
                .credentialsNonExpired(true)
                .accountNonLocked(true)
                .loginAttempts(0)
                .build();
    }

    /**
     * Builder para crear usuarios del staff educativo
     */
    public static User createStaffUser(String email, String firstName, String lastName, 
                                     EducationalLevel educationalLevel, Subject subject) {
        return User.builder()
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .educationalLevel(educationalLevel)
                .subject(subject)
                .enabled(true)
                .emailVerified(true)  // Staff pre-verificado
                .accountNonExpired(true)
                .credentialsNonExpired(true)
                .accountNonLocked(true)
                .loginAttempts(0)
                .build();
    }
}