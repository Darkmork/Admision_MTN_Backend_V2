package com.desafios.mtn.applicationservice.domain;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Entidad principal para aplicaciones/postulaciones de admisión
 */
@Entity
@Table(name = "applications", indexes = {
    @Index(name = "idx_applications_status", columnList = "status"),
    @Index(name = "idx_applications_created_at", columnList = "createdAt"),
    @Index(name = "idx_applications_created_by", columnList = "createdBy"),
    @Index(name = "idx_applications_grade_applied", columnList = "gradeApplied"),
    @Index(name = "idx_applications_special_needs", columnList = "specialNeeds")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "created_by", nullable = false)
    private String createdBy; // ID del usuario que creó la aplicación

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ApplicationStatus status = ApplicationStatus.DRAFT;

    @Column(name = "grade_applied")
    private String gradeApplied; // Grado al que postula

    // Datos del postulante almacenados como JSONB
    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> applicant;

    // Contexto familiar almacenado como JSONB
    @Type(JsonType.class)
    @Column(name = "family_context", columnDefinition = "jsonb")
    private Map<String, Object> familyContext;

    // Campos de auditoría
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Optimistic locking para concurrencia
    @Version
    @Column(nullable = false)
    @Builder.Default
    private Integer version = 0;

    // Campos adicionales
    @Column(name = "source_channel")
    @Builder.Default
    private String sourceChannel = "WEB";

    @Column(name = "external_reference")
    private String externalReference;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "special_needs")
    @Builder.Default
    private Boolean specialNeeds = false;

    // Timestamps de estados críticos
    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "rejected_at")
    private Instant rejectedAt;

    @Column(name = "enrolled_at")
    private Instant enrolledAt;

    // ================================
    // BUSINESS METHODS
    // ================================

    /**
     * Verifica si la aplicación está en estado DRAFT
     */
    public boolean isDraft() {
        return status == ApplicationStatus.DRAFT;
    }

    /**
     * Verifica si la aplicación está enviada (no DRAFT)
     */
    public boolean isSubmitted() {
        return status != ApplicationStatus.DRAFT && submittedAt != null;
    }

    /**
     * Verifica si la aplicación está en estado terminal
     */
    public boolean isTerminal() {
        return status.isTerminal();
    }

    /**
     * Verifica si la aplicación está aprobada
     */
    public boolean isApproved() {
        return status == ApplicationStatus.APPROVED || status == ApplicationStatus.ENROLLED;
    }

    /**
     * Verifica si la aplicación está rechazada
     */
    public boolean isRejected() {
        return status.isRejected();
    }

    /**
     * Verifica si la aplicación permite subir documentos
     */
    public boolean allowsDocumentUpload() {
        return status.allowsDocumentUpload();
    }

    /**
     * Verifica si la aplicación requiere acción del apoderado
     */
    public boolean requiresParentAction() {
        return status.requiresParentAction();
    }

    /**
     * Verifica si la aplicación requiere acción administrativa
     */
    public boolean requiresAdminAction() {
        return status.requiresAdminAction();
    }

    /**
     * Obtiene el nombre del postulante desde el JSON
     */
    public String getApplicantName() {
        if (applicant == null) {
            return null;
        }
        return (String) applicant.get("nombre");
    }

    /**
     * Obtiene el RUT del postulante desde el JSON
     */
    public String getApplicantRut() {
        if (applicant == null) {
            return null;
        }
        return (String) applicant.get("rut");
    }

    /**
     * Obtiene la fecha de nacimiento del postulante
     */
    public String getApplicantBirthDate() {
        if (applicant == null) {
            return null;
        }
        return (String) applicant.get("fecha_nacimiento");
    }

    /**
     * Obtiene información del padre desde el contexto familiar
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getFatherInfo() {
        if (familyContext == null) {
            return null;
        }
        return (Map<String, Object>) familyContext.get("padre");
    }

    /**
     * Obtiene información de la madre desde el contexto familiar
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getMotherInfo() {
        if (familyContext == null) {
            return null;
        }
        return (Map<String, Object>) familyContext.get("madre");
    }

    /**
     * Obtiene el teléfono de contacto principal
     */
    public String getPrimaryContactPhone() {
        Map<String, Object> fatherInfo = getFatherInfo();
        if (fatherInfo != null && fatherInfo.get("telefono") != null) {
            return (String) fatherInfo.get("telefono");
        }
        
        Map<String, Object> motherInfo = getMotherInfo();
        if (motherInfo != null && motherInfo.get("telefono") != null) {
            return (String) motherInfo.get("telefono");
        }
        
        return null;
    }

    /**
     * Obtiene el email de contacto principal
     */
    public String getPrimaryContactEmail() {
        Map<String, Object> fatherInfo = getFatherInfo();
        if (fatherInfo != null && fatherInfo.get("email") != null) {
            return (String) fatherInfo.get("email");
        }
        
        Map<String, Object> motherInfo = getMotherInfo();
        if (motherInfo != null && motherInfo.get("email") != null) {
            return (String) motherInfo.get("email");
        }
        
        return null;
    }

    /**
     * Calcula la edad del estudiante basada en la fecha de nacimiento
     */
    public Integer getApplicantAge() {
        String birthDate = getApplicantBirthDate();
        if (birthDate == null) {
            return null;
        }
        
        try {
            // Asumir formato YYYY-MM-DD
            String[] parts = birthDate.split("-");
            if (parts.length == 3) {
                int birthYear = Integer.parseInt(parts[0]);
                int currentYear = Instant.now().atZone(java.time.ZoneId.of("America/Santiago")).getYear();
                return currentYear - birthYear;
            }
        } catch (Exception e) {
            // Ignorar errores de parsing
        }
        
        return null;
    }

    /**
     * Verifica si la aplicación tiene necesidades especiales
     */
    public boolean hasSpecialNeeds() {
        return Boolean.TRUE.equals(specialNeeds);
    }

    /**
     * Obtiene el tiempo transcurrido desde la creación
     */
    public long getAgeInDays() {
        if (createdAt == null) {
            return 0;
        }
        return java.time.Duration.between(createdAt, Instant.now()).toDays();
    }

    /**
     * Obtiene el tiempo transcurrido desde la última actualización
     */
    public long getTimeSinceLastUpdateInHours() {
        if (updatedAt == null) {
            return 0;
        }
        return java.time.Duration.between(updatedAt, Instant.now()).toHours();
    }

    /**
     * Verifica si la aplicación está "stale" (sin actividad reciente)
     */
    public boolean isStale(int maxDaysWithoutUpdate) {
        return getTimeSinceLastUpdateInHours() > (maxDaysWithoutUpdate * 24);
    }

    /**
     * Obtiene un resumen de la aplicación para logs
     */
    public String getSummary() {
        return String.format("Application[id=%s, applicant=%s, status=%s, grade=%s, age=%dd]",
                id != null ? id.toString().substring(0, 8) : "null",
                getApplicantName(),
                status,
                gradeApplied,
                getAgeInDays());
    }

    // ================================
    // STATE TRANSITION METHODS
    // ================================

    /**
     * Marca la aplicación como enviada
     */
    public void markAsSubmitted() {
        if (this.status == ApplicationStatus.DRAFT) {
            this.status = ApplicationStatus.PENDING;
            this.submittedAt = Instant.now();
        }
    }

    /**
     * Cambia el estado de la aplicación
     * IMPORTANTE: Este método debe ser usado solo por StateTransitionService
     */
    public void changeStatus(ApplicationStatus newStatus) {
        ApplicationStatus previousStatus = this.status;
        this.status = newStatus;
        
        // Actualizar timestamps de estados críticos
        Instant now = Instant.now();
        switch (newStatus) {
            case PENDING -> {
                if (previousStatus == ApplicationStatus.DRAFT && submittedAt == null) {
                    this.submittedAt = now;
                }
            }
            case APPROVED -> this.approvedAt = now;
            case REJECTED, EXPIRED -> this.rejectedAt = now;
            case ENROLLED -> this.enrolledAt = now;
        }
    }

    /**
     * Verifica si puede transicionar al estado especificado
     */
    public boolean canTransitionTo(ApplicationStatus targetStatus) {
        return this.status.canTransitionTo(targetStatus);
    }

    // ================================
    // FACTORY METHODS
    // ================================

    /**
     * Crea una nueva aplicación en estado DRAFT
     */
    public static Application createDraft(String createdBy, String gradeApplied) {
        return Application.builder()
                .createdBy(createdBy)
                .status(ApplicationStatus.DRAFT)
                .gradeApplied(gradeApplied)
                .sourceChannel("WEB")
                .specialNeeds(false)
                .build();
    }

    /**
     * Crea una aplicación con datos completos del postulante
     */
    public static Application createWithApplicantData(
            String createdBy, 
            String gradeApplied,
            Map<String, Object> applicantData,
            Map<String, Object> familyContextData) {
        
        return Application.builder()
                .createdBy(createdBy)
                .status(ApplicationStatus.DRAFT)
                .gradeApplied(gradeApplied)
                .applicant(applicantData)
                .familyContext(familyContextData)
                .sourceChannel("WEB")
                .specialNeeds(false)
                .build();
    }

    // ================================
    // JPA CALLBACKS
    // ================================

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (updatedAt == null) {
            updatedAt = createdAt;
        }
        if (version == null) {
            version = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    @Override
    public String toString() {
        return String.format("Application{id=%s, status=%s, applicant=%s, createdBy=%s}", 
                id, status, getApplicantName(), createdBy);
    }
}