package com.desafios.mtn.notificationservice.repository;

import com.desafios.mtn.notificationservice.domain.Template;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository para entidad Template
 */
@Repository
public interface TemplateRepository extends JpaRepository<Template, String> {

    /**
     * Encuentra plantillas por canal
     */
    List<Template> findByChannelAndActiveTrue(Template.NotificationChannel channel);

    /**
     * Encuentra plantillas activas
     */
    List<Template> findByActiveTrue();

    /**
     * Encuentra plantilla por ID y que esté activa
     */
    Optional<Template> findByIdAndActiveTrue(String id);

    /**
     * Encuentra plantillas por canal específico
     */
    @Query("SELECT t FROM Template t WHERE t.channel = :channel AND t.active = true ORDER BY t.createdAt DESC")
    List<Template> findActiveTemplatesByChannel(@Param("channel") Template.NotificationChannel channel);

    /**
     * Encuentra plantillas de email activas
     */
    @Query("SELECT t FROM Template t WHERE t.channel = 'email' AND t.active = true ORDER BY t.createdAt DESC")
    List<Template> findActiveEmailTemplates();

    /**
     * Encuentra plantillas de SMS activas
     */
    @Query("SELECT t FROM Template t WHERE t.channel = 'sms' AND t.active = true ORDER BY t.createdAt DESC")
    List<Template> findActiveSmsTemplates();

    /**
     * Busca plantillas por texto en subject o body
     */
    @Query("SELECT t FROM Template t WHERE t.active = true AND " +
           "(LOWER(t.subject) LIKE LOWER(CONCAT('%', :searchText, '%')) OR " +
           " LOWER(t.bodyText) LIKE LOWER(CONCAT('%', :searchText, '%')) OR " +
           " LOWER(t.bodyHtml) LIKE LOWER(CONCAT('%', :searchText, '%')))")
    List<Template> searchByText(@Param("searchText") String searchText);

    /**
     * Cuenta plantillas por canal
     */
    @Query("SELECT t.channel, COUNT(t) FROM Template t WHERE t.active = true GROUP BY t.channel")
    List<Object[]> countTemplatesByChannel();

    /**
     * Encuentra plantillas creadas después de una fecha
     */
    @Query("SELECT t FROM Template t WHERE t.active = true AND t.createdAt > :afterDate ORDER BY t.createdAt DESC")
    List<Template> findTemplatesCreatedAfter(@Param("afterDate") Instant afterDate);

    /**
     * Encuentra plantillas modificadas después de una fecha
     */
    @Query("SELECT t FROM Template t WHERE t.active = true AND t.updatedAt > :afterDate ORDER BY t.updatedAt DESC")
    List<Template> findTemplatesModifiedAfter(@Param("afterDate") Instant afterDate);

    /**
     * Encuentra plantillas que requieren una variable específica
     */
    @Query("SELECT t FROM Template t WHERE t.active = true AND " +
           "JSON_CONTAINS(CAST(t.variables AS JSON), JSON_ARRAY(:variableName))")
    List<Template> findTemplatesRequiringVariable(@Param("variableName") String variableName);

    /**
     * Encuentra plantillas por creador
     */
    List<Template> findByCreatedByAndActiveTrue(String createdBy);

    /**
     * Verifica si existe una plantilla con el ID dado
     */
    boolean existsByIdAndActiveTrue(String id);

    /**
     * Encuentra plantillas sin contenido válido (para limpieza)
     */
    @Query("SELECT t FROM Template t WHERE t.active = true AND " +
           "((t.channel = 'email' AND (t.bodyText IS NULL OR t.bodyText = '') AND (t.bodyHtml IS NULL OR t.bodyHtml = '')) OR " +
           " (t.channel = 'sms' AND (t.bodyText IS NULL OR t.bodyText = '')))")
    List<Template> findTemplatesWithoutValidContent();

    /**
     * Estadísticas de uso de plantillas (requiere join con messages)
     */
    @Query("SELECT t.id, t.subject, COUNT(m.id) as usage_count " +
           "FROM Template t LEFT JOIN Message m ON t.id = m.templateId " +
           "WHERE t.active = true " +
           "GROUP BY t.id, t.subject " +
           "ORDER BY usage_count DESC")
    List<Object[]> getTemplateUsageStatistics();

    /**
     * Plantillas más utilizadas en los últimos días
     */
    @Query("SELECT t.id, t.subject, t.channel, COUNT(m.id) as usage_count " +
           "FROM Template t LEFT JOIN Message m ON t.id = m.templateId " +
           "WHERE t.active = true AND m.createdAt > :afterDate " +
           "GROUP BY t.id, t.subject, t.channel " +
           "ORDER BY usage_count DESC")
    List<Object[]> getMostUsedTemplatesSince(@Param("afterDate") Instant afterDate);

    /**
     * Desactivar plantillas en lote
     */
    @Query("UPDATE Template t SET t.active = false, t.updatedAt = CURRENT_TIMESTAMP WHERE t.id IN :templateIds")
    void deactivateTemplates(@Param("templateIds") List<String> templateIds);

    /**
     * Activar plantillas en lote
     */
    @Query("UPDATE Template t SET t.active = true, t.updatedAt = CURRENT_TIMESTAMP WHERE t.id IN :templateIds")
    void activateTemplates(@Param("templateIds") List<String> templateIds);

    /**
     * Limpieza de plantillas antiguas no utilizadas
     */
    @Query("SELECT t FROM Template t WHERE t.active = true " +
           "AND t.createdAt < :beforeDate " +
           "AND NOT EXISTS (SELECT 1 FROM Message m WHERE m.templateId = t.id)")
    List<Template> findUnusedTemplatesOlderThan(@Param("beforeDate") Instant beforeDate);

    /**
     * Encuentra plantillas duplicadas por contenido
     */
    @Query("SELECT t1 FROM Template t1, Template t2 WHERE " +
           "t1.id != t2.id AND t1.active = true AND t2.active = true " +
           "AND t1.channel = t2.channel " +
           "AND ((t1.bodyText = t2.bodyText AND t1.bodyText IS NOT NULL) OR " +
           "     (t1.bodyHtml = t2.bodyHtml AND t1.bodyHtml IS NOT NULL))")
    List<Template> findDuplicateTemplates();

    /**
     * Cuenta total de plantillas activas
     */
    @Query("SELECT COUNT(t) FROM Template t WHERE t.active = true")
    long countActiveTemplates();

    /**
     * Encuentra plantillas por múltiples IDs
     */
    @Query("SELECT t FROM Template t WHERE t.id IN :templateIds AND t.active = true")
    List<Template> findByIdsAndActive(@Param("templateIds") List<String> templateIds);
}