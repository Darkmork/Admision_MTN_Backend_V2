package com.desafios.mtn.applicationservice.service;

import com.desafios.mtn.applicationservice.domain.AppDocument;
import com.desafios.mtn.applicationservice.domain.Application;
import com.desafios.mtn.applicationservice.domain.OutboxEvent;
import com.desafios.mtn.applicationservice.repository.AppDocumentRepository;
import com.desafios.mtn.applicationservice.repository.ApplicationRepository;
import com.desafios.mtn.applicationservice.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Servicio para gestión de documentos de aplicaciones
 * Maneja metadatos de documentos (binarios permanecen en monolito)
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DocumentService {

    private final AppDocumentRepository documentRepository;
    private final ApplicationRepository applicationRepository;
    private final OutboxEventRepository outboxEventRepository;

    // ================================
    // OPERACIONES CRUD DE DOCUMENTOS
    // ================================

    /**
     * Registra un nuevo documento (metadatos)
     */
    public AppDocument createDocument(UUID applicationId, String externalId, 
                                    AppDocument.DocumentType docType, String filename, 
                                    String mimeType, Long sizeBytes, String checksum,
                                    String uploadedBy) {
        log.info("Creating document metadata for application {} - type: {} file: {}", 
                applicationId, docType, filename);

        // Verificar que la aplicación existe y permite subir documentos
        Application application = applicationRepository.findById(applicationId)
            .orElseThrow(() -> new IllegalArgumentException("Application not found: " + applicationId));

        if (!application.allowsDocumentUpload()) {
            throw new IllegalStateException(
                String.format("Application %s in state %s does not allow document upload", 
                    applicationId, application.getStatus()));
        }

        // Verificar si ya existe un documento con el mismo external ID
        if (documentRepository.existsByExternalId(externalId)) {
            throw new IllegalStateException("Document with external ID already exists: " + externalId);
        }

        // Crear el documento
        AppDocument document = AppDocument.createWithMetadata(
            applicationId, externalId, docType, filename, 
            mimeType, sizeBytes, checksum, uploadedBy);

        AppDocument savedDocument = documentRepository.save(document);

        // Crear evento de documento subido
        OutboxEvent event = OutboxEvent.documentUploaded(
            applicationId, savedDocument.getId(), docType, uploadedBy);
        outboxEventRepository.save(event);

        log.info("Created document: {}", savedDocument.getSummary());
        return savedDocument;
    }

    /**
     * Actualiza metadatos de un documento
     */
    public AppDocument updateDocument(UUID documentId, String filename, 
                                    String mimeType, Long sizeBytes, String checksum) {
        log.info("Updating document metadata: {}", documentId);

        AppDocument document = documentRepository.findById(documentId)
            .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

        if (document.isRejected() || document.status == AppDocument.DocumentStatus.REPLACED) {
            throw new IllegalStateException("Cannot update document in state: " + document.getStatus());
        }

        // Actualizar metadatos
        document.setFilename(filename);
        document.setMimeType(mimeType);
        document.setSizeBytes(sizeBytes);
        document.setChecksum(checksum);

        AppDocument savedDocument = documentRepository.save(document);
        log.info("Updated document: {}", savedDocument.getSummary());
        return savedDocument;
    }

    /**
     * Obtiene un documento por ID
     */
    @Transactional(readOnly = true)
    public Optional<AppDocument> findById(UUID documentId) {
        return documentRepository.findById(documentId);
    }

    /**
     * Obtiene un documento por ID externo
     */
    @Transactional(readOnly = true)
    public Optional<AppDocument> findByExternalId(String externalId) {
        return documentRepository.findByExternalId(externalId);
    }

    // ================================
    // CONSULTAS POR APLICACIÓN
    // ================================

    /**
     * Obtiene todos los documentos de una aplicación
     */
    @Transactional(readOnly = true)
    public List<AppDocument> findDocumentsByApplication(UUID applicationId) {
        return documentRepository.findByApplicationId(applicationId);
    }

    /**
     * Obtiene documentos de una aplicación con paginación
     */
    @Transactional(readOnly = true)
    public Page<AppDocument> findDocumentsByApplication(UUID applicationId, Pageable pageable) {
        return documentRepository.findByApplicationId(applicationId, pageable);
    }

    /**
     * Obtiene documentos requeridos de una aplicación
     */
    @Transactional(readOnly = true)
    public List<AppDocument> findRequiredDocuments(UUID applicationId) {
        return documentRepository.findByApplicationIdAndRequiredTrue(applicationId);
    }

    /**
     * Obtiene documentos opcionales de una aplicación
     */
    @Transactional(readOnly = true)
    public List<AppDocument> findOptionalDocuments(UUID applicationId) {
        return documentRepository.findByApplicationIdAndRequiredFalse(applicationId);
    }

    /**
     * Verifica si una aplicación tiene todos los documentos requeridos aprobados
     */
    @Transactional(readOnly = true)
    public boolean hasAllRequiredDocumentsApproved(UUID applicationId) {
        return documentRepository.hasAllRequiredDocumentsApproved(applicationId)
                                .orElse(false);
    }

    /**
     * Obtiene resumen de documentos de una aplicación
     */
    @Transactional(readOnly = true)
    public Optional<Object[]> getDocumentSummary(UUID applicationId) {
        return documentRepository.getDocumentSummary(applicationId);
    }

    // ================================
    // OPERACIONES DE REVISIÓN
    // ================================

    /**
     * Aprueba un documento
     */
    public AppDocument approveDocument(UUID documentId, String reviewedBy) {
        log.info("Approving document: {} by: {}", documentId, reviewedBy);

        AppDocument document = documentRepository.findById(documentId)
            .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

        if (!document.isPendingReview()) {
            throw new IllegalStateException(
                String.format("Document %s is not pending review (status: %s)", 
                    documentId, document.getReviewStatus()));
        }

        document.approve(reviewedBy);
        AppDocument savedDocument = documentRepository.save(document);

        log.info("Approved document: {}", savedDocument.getSummary());
        return savedDocument;
    }

    /**
     * Rechaza un documento
     */
    public AppDocument rejectDocument(UUID documentId, String reviewedBy, String reason) {
        log.info("Rejecting document: {} by: {} reason: {}", documentId, reviewedBy, reason);

        AppDocument document = documentRepository.findById(documentId)
            .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

        if (!document.isPendingReview()) {
            throw new IllegalStateException(
                String.format("Document %s is not pending review (status: %s)", 
                    documentId, document.getReviewStatus()));
        }

        document.reject(reviewedBy, reason);
        AppDocument savedDocument = documentRepository.save(document);

        log.info("Rejected document: {}", savedDocument.getSummary());
        return savedDocument;
    }

    /**
     * Solicita reenvío de un documento
     */
    public AppDocument requireResubmission(UUID documentId, String reviewedBy, String reason) {
        log.info("Requiring resubmission for document: {} by: {} reason: {}", 
                documentId, reviewedBy, reason);

        AppDocument document = documentRepository.findById(documentId)
            .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

        document.requireResubmission(reviewedBy, reason);
        AppDocument savedDocument = documentRepository.save(document);

        log.info("Required resubmission for document: {}", savedDocument.getSummary());
        return savedDocument;
    }

    // ================================
    // GESTIÓN DE VERSIONES
    // ================================

    /**
     * Reemplaza un documento con una nueva versión
     */
    public AppDocument replaceDocument(UUID oldDocumentId, String newExternalId, 
                                     String filename, String mimeType, Long sizeBytes, 
                                     String checksum, String uploadedBy) {
        log.info("Replacing document: {} with new version", oldDocumentId);

        AppDocument oldDocument = documentRepository.findById(oldDocumentId)
            .orElseThrow(() -> new IllegalArgumentException("Document not found: " + oldDocumentId));

        // Verificar que se puede reemplazar
        if (oldDocument.getStatus() == AppDocument.DocumentStatus.REPLACED) {
            throw new IllegalStateException("Document is already replaced");
        }

        // Crear nueva versión
        AppDocument newDocument = AppDocument.createWithMetadata(
            oldDocument.getApplicationId(), newExternalId, oldDocument.getDocType(),
            filename, mimeType, sizeBytes, checksum, uploadedBy);
        
        newDocument.setVersionNumber(oldDocument.getVersionNumber() + 1);
        newDocument.setReplacesDocumentId(oldDocumentId);
        newDocument.setRequired(oldDocument.isRequired());
        newDocument.setDocumentCategory(oldDocument.getDocumentCategory());

        // Marcar documento anterior como reemplazado
        oldDocument.markAsReplaced();
        documentRepository.save(oldDocument);

        // Guardar nueva versión
        AppDocument savedNewDocument = documentRepository.save(newDocument);

        log.info("Replaced document {} with new version: {}", 
                oldDocumentId, savedNewDocument.getSummary());
        return savedNewDocument;
    }

    /**
     * Obtiene la versión más reciente de un documento
     */
    @Transactional(readOnly = true)
    public Optional<AppDocument> findLatestVersion(UUID applicationId, AppDocument.DocumentType docType) {
        return documentRepository.findLatestVersion(applicationId, docType);
    }

    /**
     * Obtiene todas las versiones de un documento
     */
    @Transactional(readOnly = true)
    public List<AppDocument> findAllVersions(UUID applicationId, AppDocument.DocumentType docType, UUID documentId) {
        return documentRepository.findAllVersions(applicationId, docType, documentId);
    }

    // ================================
    // CONSULTAS DE REVISIÓN Y MONITOREO
    // ================================

    /**
     * Obtiene documentos pendientes de revisión
     */
    @Transactional(readOnly = true)
    public List<AppDocument> findPendingReview() {
        return documentRepository.findPendingReview();
    }

    /**
     * Obtiene documentos que requieren atención urgente
     */
    @Transactional(readOnly = true)
    public List<AppDocument> findDocumentsRequiringUrgentAttention() {
        Instant urgentCutoff = Instant.now().minusSeconds(3L * 24L * 3600L); // 3 días
        LocalDate expiryAlert = LocalDate.now().plusDays(7); // 7 días para expirar
        return documentRepository.findDocumentsRequiringUrgentAttention(urgentCutoff, expiryAlert);
    }

    /**
     * Obtiene documentos antiguos sin revisar
     */
    @Transactional(readOnly = true)
    public List<AppDocument> findOldPendingDocuments(int daysOld) {
        Instant cutoffDate = Instant.now().minusSeconds(daysOld * 24L * 3600L);
        return documentRepository.findOldPendingDocuments(cutoffDate);
    }

    /**
     * Obtiene documentos que expiran pronto
     */
    @Transactional(readOnly = true)
    public List<AppDocument> findExpiringDocuments(int daysUntilExpiry) {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(daysUntilExpiry);
        return documentRepository.findByExpiryDateBetween(startDate, endDate);
    }

    /**
     * Obtiene documentos expirados
     */
    @Transactional(readOnly = true)
    public List<AppDocument> findExpiredDocuments() {
        return documentRepository.findByExpiryDateBefore(LocalDate.now());
    }

    // ================================
    // ESTADÍSTICAS Y REPORTES
    // ================================

    /**
     * Obtiene estadísticas por tipo de documento
     */
    @Transactional(readOnly = true)
    public List<Object[]> getDocumentTypeStatistics() {
        return documentRepository.getDocumentTypeStatistics();
    }

    /**
     * Obtiene estadísticas por estado de revisión
     */
    @Transactional(readOnly = true)
    public List<Object[]> getReviewStatusStatistics() {
        return documentRepository.getReviewStatusStatistics();
    }

    /**
     * Obtiene estadísticas por categoría
     */
    @Transactional(readOnly = true)
    public List<Object[]> getCategoryStatistics() {
        return documentRepository.getCategoryStatistics();
    }

    /**
     * Calcula el tamaño total de documentos
     */
    @Transactional(readOnly = true)
    public Optional<Long> getTotalDocumentsSize() {
        return documentRepository.getTotalDocumentsSize();
    }

    /**
     * Obtiene tiempo promedio de revisión
     */
    @Transactional(readOnly = true)
    public Optional<Double> getAverageReviewTimeInHours() {
        return documentRepository.getAverageReviewTimeInHours();
    }

    /**
     * Obtiene estadísticas mensuales de subida
     */
    @Transactional(readOnly = true)
    public List<Object[]> getMonthlyUploadStatistics() {
        return documentRepository.getMonthlyUploadStatistics();
    }

    // ================================
    // OPERACIONES DE BATCH
    // ================================

    /**
     * Procesa documentos expirados automáticamente
     */
    public int processExpiredDocuments() {
        log.info("Processing expired documents");
        
        List<AppDocument> expiredDocuments = findExpiredDocuments();
        int processedCount = 0;
        
        for (AppDocument document : expiredDocuments) {
            if (document.getStatus() == AppDocument.DocumentStatus.APPROVED) {
                // Marcar como expirado solo si estaba aprobado
                document.setStatus(AppDocument.DocumentStatus.EXPIRED);
                documentRepository.save(document);
                processedCount++;
                
                log.info("Marked document as expired: {}", document.getSummary());
            }
        }
        
        log.info("Processed {} expired documents", processedCount);
        return processedCount;
    }

    /**
     * Encuentra aplicaciones con documentos incompletos
     */
    @Transactional(readOnly = true)
    public List<UUID> findApplicationsWithIncompleteDocuments(List<UUID> applicationIds) {
        return documentRepository.findApplicationsWithIncompleteDocuments(applicationIds);
    }

    /**
     * Encuentra documentos duplicados por checksum
     */
    @Transactional(readOnly = true)
    public List<Object[]> findDuplicateDocuments() {
        return documentRepository.findDuplicateDocumentsByChecksum();
    }

    // ================================
    // VALIDACIONES
    // ================================

    /**
     * Valida el tamaño de un documento según su tipo
     */
    public boolean isValidDocumentSize(AppDocument.DocumentType docType, long sizeBytes) {
        return sizeBytes <= docType.getMaxSizeBytes();
    }

    /**
     * Valida el tipo MIME de un documento
     */
    public boolean isValidMimeType(AppDocument.DocumentType docType, String mimeType) {
        String[] allowedTypes = docType.getAllowedMimeTypes();
        for (String allowedType : allowedTypes) {
            if (allowedType.equals(mimeType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Cuenta documentos por estado
     */
    @Transactional(readOnly = true)
    public long countDocumentsByStatus(AppDocument.DocumentStatus status) {
        return documentRepository.countByStatus(status);
    }

    /**
     * Cuenta documentos por estado de revisión
     */
    @Transactional(readOnly = true)
    public long countDocumentsByReviewStatus(AppDocument.ReviewStatus reviewStatus) {
        return documentRepository.countByReviewStatus(reviewStatus);
    }
}