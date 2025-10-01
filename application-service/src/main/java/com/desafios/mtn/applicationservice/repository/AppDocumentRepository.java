package com.desafios.mtn.applicationservice.repository;

import com.desafios.mtn.applicationservice.domain.AppDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository para AppDocument con consultas especializadas de gestión documental
 */
@Repository
public interface AppDocumentRepository extends JpaRepository<AppDocument, UUID> {

    // ================================
    // CONSULTAS POR APLICACIÓN
    // ================================

    /**
     * Encuentra todos los documentos de una aplicación
     */
    List<AppDocument> findByApplicationId(UUID applicationId);

    /**
     * Encuentra documentos de una aplicación con paginación
     */
    Page<AppDocument> findByApplicationId(UUID applicationId, Pageable pageable);

    /**
     * Cuenta documentos de una aplicación
     */
    long countByApplicationId(UUID applicationId);

    /**
     * Encuentra documentos de una aplicación por estado
     */
    List<AppDocument> findByApplicationIdAndStatus(UUID applicationId, AppDocument.DocumentStatus status);

    /**
     * Encuentra documentos de una aplicación por estado de revisión
     */
    List<AppDocument> findByApplicationIdAndReviewStatus(UUID applicationId, AppDocument.ReviewStatus reviewStatus);

    // ================================
    // CONSULTAS POR TIPO DE DOCUMENTO
    // ================================

    /**
     * Encuentra documentos por tipo
     */
    List<AppDocument> findByDocType(AppDocument.DocumentType docType);

    /**
     * Encuentra documentos de una aplicación por tipo
     */
    List<AppDocument> findByApplicationIdAndDocType(UUID applicationId, AppDocument.DocumentType docType);

    /**
     * Encuentra documentos por categoría
     */
    List<AppDocument> findByDocumentCategory(AppDocument.DocumentCategory category);

    /**
     * Encuentra documentos requeridos de una aplicación
     */
    List<AppDocument> findByApplicationIdAndRequiredTrue(UUID applicationId);

    /**
     * Encuentra documentos opcionales de una aplicación
     */
    List<AppDocument> findByApplicationIdAndRequiredFalse(UUID applicationId);

    // ================================
    // CONSULTAS POR ESTADO
    // ================================

    /**
     * Encuentra documentos por estado
     */
    List<AppDocument> findByStatus(AppDocument.DocumentStatus status);

    /**
     * Encuentra documentos por estado de revisión
     */
    List<AppDocument> findByReviewStatus(AppDocument.ReviewStatus reviewStatus);

    /**
     * Encuentra documentos aprobados
     */
    List<AppDocument> findByStatusAndReviewStatus(AppDocument.DocumentStatus status, AppDocument.ReviewStatus reviewStatus);

    /**
     * Encuentra documentos pendientes de revisión
     */
    @Query("SELECT d FROM AppDocument d WHERE d.reviewStatus IN ('PENDING', 'IN_REVIEW')")
    List<AppDocument> findPendingReview();

    /**
     * Encuentra documentos rechazados
     */
    List<AppDocument> findByReviewStatus(AppDocument.ReviewStatus reviewStatus);

    // ================================
    // CONSULTAS POR EXTERNAL ID
    // ================================

    /**
     * Encuentra documento por ID externo
     */
    Optional<AppDocument> findByExternalId(String externalId);

    /**
     * Verifica si existe un documento con ID externo
     */
    boolean existsByExternalId(String externalId);

    /**
     * Encuentra documentos por múltiples IDs externos
     */
    List<AppDocument> findByExternalIdIn(List<String> externalIds);

    // ================================
    // CONSULTAS POR USUARIO
    // ================================

    /**
     * Encuentra documentos subidos por un usuario
     */
    List<AppDocument> findByUploadedBy(String uploadedBy);

    /**
     * Encuentra documentos revisados por un usuario
     */
    List<AppDocument> findByReviewedBy(String reviewedBy);

    /**
     * Encuentra documentos de un usuario en un rango de fechas
     */
    List<AppDocument> findByUploadedByAndUploadedAtBetween(String uploadedBy, Instant startDate, Instant endDate);

    // ================================
    // CONSULTAS TEMPORALES
    // ================================

    /**
     * Encuentra documentos subidos en un rango de fechas
     */
    List<AppDocument> findByUploadedAtBetween(Instant startDate, Instant endDate);

    /**
     * Encuentra documentos revisados en un rango de fechas
     */
    List<AppDocument> findByReviewedAtBetween(Instant startDate, Instant endDate);

    /**
     * Encuentra documentos antiguos sin revisar
     */
    @Query("SELECT d FROM AppDocument d WHERE d.uploadedAt < :cutoffDate AND d.reviewStatus IN ('PENDING', 'IN_REVIEW')")
    List<AppDocument> findOldPendingDocuments(@Param("cutoffDate") Instant cutoffDate);

    /**
     * Encuentra documentos que expiran pronto
     */
    List<AppDocument> findByExpiryDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Encuentra documentos expirados
     */
    List<AppDocument> findByExpiryDateBefore(LocalDate date);

    // ================================
    // CONSULTAS DE VERSIONES
    // ================================

    /**
     * Encuentra la versión más reciente de un documento
     */
    @Query("SELECT d FROM AppDocument d WHERE d.applicationId = :applicationId AND d.docType = :docType AND d.replacesDocumentId IS NULL")
    Optional<AppDocument> findLatestVersion(@Param("applicationId") UUID applicationId, 
                                          @Param("docType") AppDocument.DocumentType docType);

    /**
     * Encuentra todas las versiones de un documento
     */
    @Query("SELECT d FROM AppDocument d WHERE (d.applicationId = :applicationId AND d.docType = :docType) OR d.replacesDocumentId = :documentId ORDER BY d.versionNumber DESC")
    List<AppDocument> findAllVersions(@Param("applicationId") UUID applicationId, 
                                     @Param("docType") AppDocument.DocumentType docType, 
                                     @Param("documentId") UUID documentId);

    /**
     * Encuentra documentos reemplazados
     */
    List<AppDocument> findByReplacesDocumentIdIsNotNull();

    /**
     * Encuentra documento que fue reemplazado por otro
     */
    Optional<AppDocument> findByReplacesDocumentId(UUID replacesDocumentId);

    // ================================
    // CONSULTAS AVANZADAS
    // ================================

    /**
     * Busca documentos por nombre de archivo
     */
    List<AppDocument> findByFilenameContainingIgnoreCase(String filename);

    /**
     * Encuentra documentos por tipo MIME
     */
    List<AppDocument> findByMimeType(String mimeType);

    /**
     * Encuentra documentos por rango de tamaño
     */
    List<AppDocument> findBySizeBytesBetween(Long minSize, Long maxSize);

    /**
     * Encuentra documentos grandes
     */
    @Query("SELECT d FROM AppDocument d WHERE d.sizeBytes > :maxSize")
    List<AppDocument> findLargeDocuments(@Param("maxSize") Long maxSize);

    /**
     * Encuentra documentos por checksum (para detectar duplicados)
     */
    List<AppDocument> findByChecksum(String checksum);

    /**
     * Encuentra documentos duplicados por checksum
     */
    @Query("SELECT d.checksum, COUNT(d) FROM AppDocument d WHERE d.checksum IS NOT NULL GROUP BY d.checksum HAVING COUNT(d) > 1")
    List<Object[]> findDuplicateDocumentsByChecksum();

    // ================================
    // ESTADÍSTICAS Y REPORTES
    // ================================

    /**
     * Obtiene estadísticas por tipo de documento
     */
    @Query("SELECT d.docType, COUNT(d) FROM AppDocument d GROUP BY d.docType ORDER BY COUNT(d) DESC")
    List<Object[]> getDocumentTypeStatistics();

    /**
     * Obtiene estadísticas por estado de revisión
     */
    @Query("SELECT d.reviewStatus, COUNT(d) FROM AppDocument d GROUP BY d.reviewStatus")
    List<Object[]> getReviewStatusStatistics();

    /**
     * Obtiene estadísticas por categoría
     */
    @Query("SELECT d.documentCategory, COUNT(d) FROM AppDocument d GROUP BY d.documentCategory")
    List<Object[]> getCategoryStatistics();

    /**
     * Calcula tamaño total de documentos
     */
    @Query("SELECT SUM(d.sizeBytes) FROM AppDocument d WHERE d.sizeBytes IS NOT NULL")
    Optional<Long> getTotalDocumentsSize();

    /**
     * Calcula tamaño promedio por tipo
     */
    @Query("SELECT d.docType, AVG(d.sizeBytes) FROM AppDocument d WHERE d.sizeBytes IS NOT NULL GROUP BY d.docType")
    List<Object[]> getAverageSizeByType();

    /**
     * Obtiene estadísticas mensuales de subida
     */
    @Query(value = "SELECT DATE_TRUNC('month', uploaded_at) as month, COUNT(*) FROM app_documents GROUP BY month ORDER BY month DESC LIMIT 12", 
           nativeQuery = true)
    List<Object[]> getMonthlyUploadStatistics();

    // ================================
    // CONSULTAS PARA REVISIÓN
    // ================================

    /**
     * Encuentra documentos que requieren atención urgente
     */
    @Query("SELECT d FROM AppDocument d WHERE (d.reviewStatus IN ('PENDING', 'IN_REVIEW') AND d.uploadedAt < :urgentCutoff) OR d.expiryDate <= :expiryAlert")
    List<AppDocument> findDocumentsRequiringUrgentAttention(@Param("urgentCutoff") Instant urgentCutoff, 
                                                           @Param("expiryAlert") LocalDate expiryAlert);

    /**
     * Encuentra documentos por revisor y estado
     */
    List<AppDocument> findByReviewedByAndReviewStatus(String reviewedBy, AppDocument.ReviewStatus reviewStatus);

    /**
     * Calcula tiempo promedio de revisión
     */
    @Query("SELECT AVG(EXTRACT(EPOCH FROM (d.reviewedAt - d.uploadedAt))/3600) FROM AppDocument d WHERE d.reviewedAt IS NOT NULL AND d.uploadedAt IS NOT NULL")
    Optional<Double> getAverageReviewTimeInHours();

    // ================================
    // CONSULTAS DE COMPLETITUD
    // ================================

    /**
     * Verifica si una aplicación tiene todos los documentos requeridos
     */
    @Query(value = """
        SELECT 
            CASE 
                WHEN COUNT(CASE WHEN d.required = true AND d.review_status = 'APPROVED' THEN 1 END) = 
                     COUNT(CASE WHEN d.required = true THEN 1 END)
                THEN true 
                ELSE false 
            END as all_required_approved
        FROM app_documents d 
        WHERE d.application_id = :applicationId
        """, nativeQuery = true)
    Optional<Boolean> hasAllRequiredDocumentsApproved(@Param("applicationId") UUID applicationId);

    /**
     * Obtiene resumen de documentos por aplicación
     */
    @Query(value = """
        SELECT 
            application_id,
            COUNT(*) as total_docs,
            COUNT(CASE WHEN required = true THEN 1 END) as required_docs,
            COUNT(CASE WHEN required = true AND review_status = 'APPROVED' THEN 1 END) as approved_required,
            COUNT(CASE WHEN review_status = 'PENDING' THEN 1 END) as pending_review,
            COUNT(CASE WHEN review_status = 'REJECTED' THEN 1 END) as rejected
        FROM app_documents 
        WHERE application_id = :applicationId
        GROUP BY application_id
        """, nativeQuery = true)
    Optional<Object[]> getDocumentSummary(@Param("applicationId") UUID applicationId);

    /**
     * Encuentra aplicaciones con documentos incompletos
     */
    @Query(value = """
        SELECT DISTINCT d.application_id
        FROM app_documents d
        WHERE d.application_id IN :applicationIds
        GROUP BY d.application_id
        HAVING COUNT(CASE WHEN d.required = true AND d.review_status = 'APPROVED' THEN 1 END) < 
               COUNT(CASE WHEN d.required = true THEN 1 END)
        """, nativeQuery = true)
    List<UUID> findApplicationsWithIncompleteDocuments(@Param("applicationIds") List<UUID> applicationIds);
}