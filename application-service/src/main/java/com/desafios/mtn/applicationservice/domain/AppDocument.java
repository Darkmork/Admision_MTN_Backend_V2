package com.desafios.mtn.applicationservice.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Metadatos de documentos adjuntos a aplicaciones
 * Los binarios permanecen en el monolito, aquí solo metadatos
 */
@Entity
@Table(name = "app_documents", 
       uniqueConstraints = @UniqueConstraint(
           name = "idx_app_documents_external_app_unique",
           columnNames = {"applicationId", "externalId"}
       ),
       indexes = {
    @Index(name = "idx_app_documents_application_id", columnList = "applicationId"),
    @Index(name = "idx_app_documents_external_id", columnList = "externalId"),
    @Index(name = "idx_app_documents_doc_type", columnList = "docType"),
    @Index(name = "idx_app_documents_status", columnList = "status"),
    @Index(name = "idx_app_documents_review_status", columnList = "reviewStatus"),
    @Index(name = "idx_app_documents_uploaded_at", columnList = "uploadedAt"),
    @Index(name = "idx_app_documents_required", columnList = "required"),
    @Index(name = "idx_app_documents_expiry_date", columnList = "expiryDate")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class AppDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    @Column(name = "external_id", nullable = false)
    private String externalId; // ID del binario en monolito o file-service

    @Enumerated(EnumType.STRING)
    @Column(name = "doc_type", nullable = false)
    private DocumentType docType;

    @Column(nullable = false)
    private String filename;

    @Column(name = "mime_type")
    private String mimeType;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column
    private String checksum; // Hash para integridad

    // Estados del documento
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private DocumentStatus status = DocumentStatus.UPLOADED;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false)
    @Builder.Default
    private ReviewStatus reviewStatus = ReviewStatus.PENDING;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    // Auditoría
    @Column(name = "uploaded_by", nullable = false)
    private String uploadedBy;

    @Column(name = "reviewed_by")
    private String reviewedBy;

    @CreatedDate
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private Instant uploadedAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    // Metadatos adicionales
    @Enumerated(EnumType.STRING)
    @Column(name = "document_category")
    private DocumentCategory documentCategory;

    @Column(nullable = false)
    @Builder.Default
    private Boolean required = true;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    // Versioning de documentos
    @Column(name = "version_number", nullable = false)
    @Builder.Default
    private Integer versionNumber = 1;

    @Column(name = "replaces_document_id")
    private UUID replacesDocumentId; // Documento que este reemplaza

    @Column(name = "upload_source", nullable = false)
    @Builder.Default
    private String uploadSource = "WEB";

    // Relación opcional con la aplicación (para queries)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", insertable = false, updatable = false)
    private Application application;

    // ================================
    // ENUMS
    // ================================

    public enum DocumentType {
        // Documentos de identidad
        BIRTH_CERTIFICATE("Certificado de Nacimiento", true, DocumentCategory.PERSONAL),
        ID_CARD("Cédula de Identidad", false, DocumentCategory.PERSONAL),
        PASSPORT("Pasaporte", false, DocumentCategory.PERSONAL),
        
        // Documentos académicos
        ACADEMIC_TRANSCRIPT("Certificado de Notas", true, DocumentCategory.ACADEMIC),
        SCHOOL_CERTIFICATE("Certificado de Estudios", true, DocumentCategory.ACADEMIC),
        CONDUCT_CERTIFICATE("Certificado de Conducta", false, DocumentCategory.ACADEMIC),
        TRANSFER_CERTIFICATE("Certificado de Traslado", false, DocumentCategory.ACADEMIC),
        
        // Documentos médicos
        MEDICAL_CERTIFICATE("Certificado Médico", true, DocumentCategory.MEDICAL),
        VACCINATION_RECORD("Carnet de Vacunas", true, DocumentCategory.MEDICAL),
        MEDICAL_REPORT("Informe Médico", false, DocumentCategory.MEDICAL),
        PSYCHOLOGICAL_REPORT("Informe Psicológico", false, DocumentCategory.MEDICAL),
        
        // Documentos familiares
        PARENT_ID("Cédula de Identidad Apoderado", true, DocumentCategory.PERSONAL),
        MARRIAGE_CERTIFICATE("Certificado de Matrimonio", false, DocumentCategory.PERSONAL),
        DIVORCE_DECREE("Sentencia de Divorcio", false, DocumentCategory.PERSONAL),
        CUSTODY_DOCUMENT("Documento de Tuición", false, DocumentCategory.PERSONAL),
        
        // Documentos financieros
        INCOME_PROOF("Comprobante de Ingresos", false, DocumentCategory.FINANCIAL),
        TAX_RETURN("Declaración de Impuestos", false, DocumentCategory.FINANCIAL),
        BANK_STATEMENT("Cartola Bancaria", false, DocumentCategory.FINANCIAL),
        
        // Otros documentos
        PHOTO("Fotografía", true, DocumentCategory.PERSONAL),
        ADDITIONAL_INFO("Información Adicional", false, DocumentCategory.OTHER);

        private final String displayName;
        private final boolean commonlyRequired;
        private final DocumentCategory category;

        DocumentType(String displayName, boolean commonlyRequired, DocumentCategory category) {
            this.displayName = displayName;
            this.commonlyRequired = commonlyRequired;
            this.category = category;
        }

        public String getDisplayName() {
            return displayName;
        }

        public boolean isCommonlyRequired() {
            return commonlyRequired;
        }

        public DocumentCategory getCategory() {
            return category;
        }

        /**
         * Verifica si este tipo requiere validación estricta
         */
        public boolean requiresStrictValidation() {
            return switch (this) {
                case BIRTH_CERTIFICATE, ACADEMIC_TRANSCRIPT, MEDICAL_CERTIFICATE -> true;
                default -> false;
            };
        }

        /**
         * Obtiene el tamaño máximo permitido en bytes
         */
        public long getMaxSizeBytes() {
            return switch (this) {
                case PHOTO -> 5 * 1024 * 1024; // 5MB para fotos
                default -> 10 * 1024 * 1024; // 10MB para documentos generales
            };
        }

        /**
         * Obtiene los tipos MIME permitidos
         */
        public String[] getAllowedMimeTypes() {
            return switch (this) {
                case PHOTO -> new String[]{"image/jpeg", "image/png", "image/jpg"};
                default -> new String[]{"application/pdf", "image/jpeg", "image/png"};
            };
        }
    }

    public enum DocumentStatus {
        UPLOADED("Subido"),
        PROCESSING("Procesando"),
        APPROVED("Aprobado"),
        REJECTED("Rechazado"),
        EXPIRED("Expirado"),
        REPLACED("Reemplazado");

        private final String displayName;

        DocumentStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public boolean isActive() {
            return this == UPLOADED || this == PROCESSING || this == APPROVED;
        }

        public boolean isFinal() {
            return this == APPROVED || this == REJECTED || this == EXPIRED || this == REPLACED;
        }
    }

    public enum ReviewStatus {
        PENDING("Pendiente de Revisión"),
        IN_REVIEW("En Revisión"),
        APPROVED("Aprobado"),
        REJECTED("Rechazado"),
        REQUIRES_RESUBMISSION("Requiere Reenvío");

        private final String displayName;

        ReviewStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public boolean isPending() {
            return this == PENDING || this == IN_REVIEW;
        }

        public boolean isResolved() {
            return this == APPROVED || this == REJECTED || this == REQUIRES_RESUBMISSION;
        }
    }

    public enum DocumentCategory {
        ACADEMIC("Académicos"),
        PERSONAL("Personales"),
        MEDICAL("Médicos"),
        FINANCIAL("Financieros"),
        OTHER("Otros");

        private final String displayName;

        DocumentCategory(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // ================================
    // BUSINESS METHODS
    // ================================

    /**
     * Verifica si el documento está aprobado
     */
    public boolean isApproved() {
        return status == DocumentStatus.APPROVED && reviewStatus == ReviewStatus.APPROVED;
    }

    /**
     * Verifica si el documento está rechazado
     */
    public boolean isRejected() {
        return status == DocumentStatus.REJECTED || reviewStatus == ReviewStatus.REJECTED;
    }

    /**
     * Verifica si el documento está pendiente de revisión
     */
    public boolean isPendingReview() {
        return reviewStatus.isPending();
    }

    /**
     * Verifica si el documento está activo
     */
    public boolean isActive() {
        return status.isActive();
    }

    /**
     * Verifica si el documento ha expirado
     */
    public boolean isExpired() {
        if (expiryDate == null) {
            return false;
        }
        return LocalDate.now().isAfter(expiryDate);
    }

    /**
     * Verifica si el documento está próximo a vencer
     */
    public boolean isExpiringWithinDays(int days) {
        if (expiryDate == null) {
            return false;
        }
        LocalDate alertDate = LocalDate.now().plusDays(days);
        return expiryDate.isBefore(alertDate) || expiryDate.isEqual(alertDate);
    }

    /**
     * Verifica si el documento es requerido
     */
    public boolean isRequired() {
        return Boolean.TRUE.equals(required);
    }

    /**
     * Verifica si es la versión más reciente
     */
    public boolean isLatestVersion() {
        return replacesDocumentId == null; // Si no reemplaza a otro, es la más reciente
    }

    /**
     * Obtiene el tamaño en formato legible
     */
    public String getFormattedSize() {
        if (sizeBytes == null) {
            return "Desconocido";
        }
        
        if (sizeBytes < 1024) {
            return sizeBytes + " B";
        } else if (sizeBytes < 1024 * 1024) {
            return String.format("%.1f KB", sizeBytes / 1024.0);
        } else {
            return String.format("%.1f MB", sizeBytes / (1024.0 * 1024.0));
        }
    }

    /**
     * Calcula la edad del documento en días
     */
    public long getAgeInDays() {
        if (uploadedAt == null) {
            return 0;
        }
        return java.time.Duration.between(uploadedAt, Instant.now()).toDays();
    }

    /**
     * Obtiene el tiempo de revisión en horas
     */
    public Long getReviewTimeInHours() {
        if (reviewedAt == null || uploadedAt == null) {
            return null;
        }
        return java.time.Duration.between(uploadedAt, reviewedAt).toHours();
    }

    /**
     * Verifica si el documento requiere atención urgente
     */
    public boolean requiresUrgentAttention() {
        return (isPendingReview() && getAgeInDays() > 3) || isExpiringWithinDays(7);
    }

    /**
     * Marca el documento como aprobado
     */
    public void approve(String reviewedBy) {
        this.status = DocumentStatus.APPROVED;
        this.reviewStatus = ReviewStatus.APPROVED;
        this.reviewedBy = reviewedBy;
        this.reviewedAt = Instant.now();
        this.rejectionReason = null;
    }

    /**
     * Marca el documento como rechazado
     */
    public void reject(String reviewedBy, String reason) {
        this.status = DocumentStatus.REJECTED;
        this.reviewStatus = ReviewStatus.REJECTED;
        this.reviewedBy = reviewedBy;
        this.reviewedAt = Instant.now();
        this.rejectionReason = reason;
    }

    /**
     * Solicita reenvío del documento
     */
    public void requireResubmission(String reviewedBy, String reason) {
        this.reviewStatus = ReviewStatus.REQUIRES_RESUBMISSION;
        this.reviewedBy = reviewedBy;
        this.reviewedAt = Instant.now();
        this.rejectionReason = reason;
    }

    /**
     * Marca el documento como reemplazado
     */
    public void markAsReplaced() {
        this.status = DocumentStatus.REPLACED;
    }

    /**
     * Obtiene un resumen del documento
     */
    public String getSummary() {
        return String.format("Document[%s: %s (%s) - %s]",
                docType.getDisplayName(),
                filename,
                getFormattedSize(),
                reviewStatus.getDisplayName());
    }

    // ================================
    // FACTORY METHODS
    // ================================

    /**
     * Crea un documento básico
     */
    public static AppDocument create(
            UUID applicationId,
            String externalId,
            DocumentType docType,
            String filename,
            String uploadedBy) {
        
        return AppDocument.builder()
                .applicationId(applicationId)
                .externalId(externalId)
                .docType(docType)
                .filename(filename)
                .uploadedBy(uploadedBy)
                .documentCategory(docType.getCategory())
                .required(docType.isCommonlyRequired())
                .build();
    }

    /**
     * Crea un documento con metadatos completos
     */
    public static AppDocument createWithMetadata(
            UUID applicationId,
            String externalId,
            DocumentType docType,
            String filename,
            String mimeType,
            Long sizeBytes,
            String checksum,
            String uploadedBy) {
        
        return AppDocument.builder()
                .applicationId(applicationId)
                .externalId(externalId)
                .docType(docType)
                .filename(filename)
                .mimeType(mimeType)
                .sizeBytes(sizeBytes)
                .checksum(checksum)
                .uploadedBy(uploadedBy)
                .documentCategory(docType.getCategory())
                .required(docType.isCommonlyRequired())
                .build();
    }

    @Override
    public String toString() {
        return String.format("AppDocument{id=%s, app=%s, type=%s, file=%s, status=%s}",
                id != null ? id.toString().substring(0, 8) : "null",
                applicationId != null ? applicationId.toString().substring(0, 8) : "null",
                docType, filename, reviewStatus);
    }
}