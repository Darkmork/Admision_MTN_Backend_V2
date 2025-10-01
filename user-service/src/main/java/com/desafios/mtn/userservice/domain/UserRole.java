// user-service/src/main/java/com/desafios/mtn/userservice/domain/UserRole.java

package com.desafios.mtn.userservice.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_roles", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "role_id"}),
       indexes = {
           @Index(name = "idx_user_roles_user_id", columnList = "user_id"),
           @Index(name = "idx_user_roles_role_id", columnList = "role_id"),
           @Index(name = "idx_user_roles_assigned_at", columnList = "assigned_at")
       })
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@ToString(exclude = {"user", "role"})
@EqualsAndHashCode(of = {"user", "role"})
public class UserRole {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, 
                foreignKey = @ForeignKey(name = "fk_user_role_user"))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_user_role_role"))
    private Role role;

    @CreationTimestamp
    @Column(name = "assigned_at", nullable = false, updatable = false)
    private Instant assignedAt;

    @Column(name = "assigned_by")
    private UUID assignedBy;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    // === MÉTODOS DE NEGOCIO ===

    /**
     * Verifica si la asignación del rol está activa
     */
    public boolean isActive() {
        return active && (expiresAt == null || expiresAt.isAfter(Instant.now()));
    }

    /**
     * Verifica si la asignación del rol ha expirado
     */
    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(Instant.now());
    }

    /**
     * Desactiva la asignación del rol
     */
    public void deactivate() {
        this.active = false;
    }

    /**
     * Activa la asignación del rol
     */
    public void activate() {
        this.active = true;
    }

    /**
     * Establece fecha de expiración
     */
    public void setExpirationDate(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    /**
     * Obtiene el nombre del usuario asociado
     */
    public String getUserFullName() {
        return user != null ? user.getFullName() : null;
    }

    /**
     * Obtiene el nombre del rol asociado
     */
    public String getRoleName() {
        return role != null ? role.getName() : null;
    }

    /**
     * Obtiene el nombre de visualización del rol asociado
     */
    public String getRoleDisplayName() {
        return role != null ? role.getDisplayName() : null;
    }

    // === BUILDERS PERSONALIZADOS ===

    /**
     * Crea una asignación de rol permanente
     */
    public static UserRole createPermanentAssignment(User user, Role role, UUID assignedBy) {
        return UserRole.builder()
                .user(user)
                .role(role)
                .assignedBy(assignedBy)
                .active(true)
                .build();
    }

    /**
     * Crea una asignación de rol temporal
     */
    public static UserRole createTemporaryAssignment(User user, Role role, UUID assignedBy, 
                                                   Instant expiresAt, String notes) {
        return UserRole.builder()
                .user(user)
                .role(role)
                .assignedBy(assignedBy)
                .expiresAt(expiresAt)
                .notes(notes)
                .active(true)
                .build();
    }

    /**
     * Crea una asignación de rol del sistema (sin usuario que lo asigna)
     */
    public static UserRole createSystemAssignment(User user, Role role) {
        return UserRole.builder()
                .user(user)
                .role(role)
                .active(true)
                .notes("Asignación automática del sistema")
                .build();
    }
}