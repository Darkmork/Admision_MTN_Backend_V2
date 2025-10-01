package com.desafios.mtn.notificationservice.repository;

import com.desafios.mtn.notificationservice.domain.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository para entidad Message
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    /**
     * Encuentra mensajes por estado
     */
    List<Message> findByStatus(Message.MessageStatus status);

    /**
     * Encuentra mensajes por estado con paginación
     */
    Page<Message> findByStatus(Message.MessageStatus status, Pageable pageable);

    /**
     * Encuentra mensajes por canal
     */
    List<Message> findByChannel(Message.NotificationChannel channel);

    /**
     * Encuentra mensajes por canal y estado
     */
    List<Message> findByChannelAndStatus(Message.NotificationChannel channel, Message.MessageStatus status);

    /**
     * Encuentra mensajes por idempotency key
     */
    Optional<Message> findByIdempotencyKey(String idempotencyKey);

    /**
     * Verifica si existe un mensaje con la clave de idempotencia
     */
    boolean existsByIdempotencyKey(String idempotencyKey);

    /**
     * Encuentra mensajes por correlation ID
     */
    List<Message> findByCorrelationId(String correlationId);

    /**
     * Encuentra mensajes pendientes (RECEIVED o PROCESSING)
     */
    @Query("SELECT m FROM Message m WHERE m.status IN ('RECEIVED', 'PROCESSING') ORDER BY m.priority DESC, m.createdAt ASC")
    List<Message> findPendingMessages();

    /**
     * Encuentra mensajes pendientes por canal
     */
    @Query("SELECT m FROM Message m WHERE m.channel = :channel AND m.status IN ('RECEIVED', 'PROCESSING') " +
           "ORDER BY m.priority DESC, m.createdAt ASC")
    List<Message> findPendingMessagesByChannel(@Param("channel") Message.NotificationChannel channel);

    /**
     * Encuentra mensajes para retry
     */
    @Query("SELECT m FROM Message m WHERE m.status = 'FAILED' " +
           "AND m.attemptCount < :maxAttempts " +
           "AND (m.expiresAt IS NULL OR m.expiresAt > CURRENT_TIMESTAMP) " +
           "ORDER BY m.priority DESC, m.createdAt ASC")
    List<Message> findMessagesForRetry(@Param("maxAttempts") int maxAttempts);

    /**
     * Encuentra mensajes expirados
     */
    @Query("SELECT m FROM Message m WHERE m.expiresAt IS NOT NULL AND m.expiresAt < CURRENT_TIMESTAMP " +
           "AND m.status NOT IN ('SENT', 'DLQ')")
    List<Message> findExpiredMessages();

    /**
     * Encuentra mensajes en DLQ
     */
    List<Message> findByStatusOrderByCreatedAtDesc(Message.MessageStatus status);

    /**
     * Encuentra mensajes por template
     */
    List<Message> findByTemplateId(String templateId);

    /**
     * Encuentra mensajes por servicio origen
     */
    List<Message> findBySourceService(String sourceService);

    /**
     * Encuentra mensajes por rango de fechas
     */
    @Query("SELECT m FROM Message m WHERE m.createdAt BETWEEN :startDate AND :endDate ORDER BY m.createdAt DESC")
    List<Message> findMessagesBetweenDates(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    /**
     * Encuentra mensajes de alta prioridad
     */
    @Query("SELECT m FROM Message m WHERE m.priority = 'high' AND m.status NOT IN ('SENT', 'DLQ') " +
           "ORDER BY m.createdAt ASC")
    List<Message> findHighPriorityPendingMessages();

    /**
     * Estadísticas por canal y estado
     */
    @Query("SELECT m.channel, m.status, COUNT(m) FROM Message m GROUP BY m.channel, m.status")
    List<Object[]> getMessageStatisticsByChannelAndStatus();

    /**
     * Estadísticas de las últimas horas
     */
    @Query("SELECT m.channel, m.status, COUNT(m) FROM Message m " +
           "WHERE m.createdAt >= :afterDate GROUP BY m.channel, m.status")
    List<Object[]> getRecentMessageStatistics(@Param("afterDate") Instant afterDate);

    /**
     * Cuenta mensajes por estado
     */
    @Query("SELECT COUNT(m) FROM Message m WHERE m.status = :status")
    long countByStatus(@Param("status") Message.MessageStatus status);

    /**
     * Cuenta mensajes en DLQ por canal
     */
    @Query("SELECT m.channel, COUNT(m) FROM Message m WHERE m.status = 'DLQ' GROUP BY m.channel")
    List<Object[]> countDlqMessagesByChannel();

    /**
     * Promedio de intentos por canal
     */
    @Query("SELECT m.channel, AVG(m.attemptCount) FROM Message m WHERE m.status = 'SENT' GROUP BY m.channel")
    List<Object[]> getAverageAttemptsByChannel();

    /**
     * Tiempo promedio de entrega
     */
    @Query("SELECT AVG(EXTRACT(EPOCH FROM (m.sentAt - m.createdAt))) FROM Message m WHERE m.sentAt IS NOT NULL")
    Double getAverageDeliveryTimeInSeconds();

    /**
     * Tiempo promedio de entrega por canal
     */
    @Query("SELECT m.channel, AVG(EXTRACT(EPOCH FROM (m.sentAt - m.createdAt))) FROM Message m " +
           "WHERE m.sentAt IS NOT NULL GROUP BY m.channel")
    List<Object[]> getAverageDeliveryTimeByChannel();

    /**
     * Mensajes más antiguos pendientes
     */
    @Query("SELECT m FROM Message m WHERE m.status IN ('RECEIVED', 'PROCESSING', 'FAILED') " +
           "ORDER BY m.createdAt ASC")
    List<Message> findOldestPendingMessages(Pageable pageable);

    /**
     * Buscar por contenido del payload
     */
    @Query("SELECT m FROM Message m WHERE CAST(m.payload AS string) LIKE %:searchTerm%")
    List<Message> searchInPayload(@Param("searchTerm") String searchTerm);

    /**
     * Limpieza de mensajes antiguos exitosos
     */
    @Query("SELECT m FROM Message m WHERE m.status = 'SENT' AND m.createdAt < :beforeDate")
    List<Message> findOldSuccessfulMessages(@Param("beforeDate") Instant beforeDate);

    /**
     * Limpieza de mensajes antiguos en DLQ
     */
    @Query("SELECT m FROM Message m WHERE m.status = 'DLQ' AND m.createdAt < :beforeDate")
    List<Message> findOldDlqMessages(@Param("beforeDate") Instant beforeDate);

    /**
     * Actualizar estado de mensajes en lote
     */
    @Modifying
    @Transactional
    @Query("UPDATE Message m SET m.status = :newStatus WHERE m.id IN :messageIds")
    void updateStatusBatch(@Param("messageIds") List<UUID> messageIds, @Param("newStatus") Message.MessageStatus newStatus);

    /**
     * Marcar mensajes como expirados
     */
    @Modifying
    @Transactional
    @Query("UPDATE Message m SET m.status = 'DLQ', m.lastError = 'Message expired' " +
           "WHERE m.expiresAt < CURRENT_TIMESTAMP AND m.status NOT IN ('SENT', 'DLQ')")
    int markExpiredMessagesAsDlq();

    /**
     * Incrementar contador de intentos
     */
    @Modifying
    @Transactional
    @Query("UPDATE Message m SET m.attemptCount = m.attemptCount + 1 WHERE m.id = :messageId")
    void incrementAttemptCount(@Param("messageId") UUID messageId);

    /**
     * Mensajes duplicados por idempotency key (para auditoría)
     */
    @Query("SELECT m.idempotencyKey, COUNT(m) FROM Message m " +
           "WHERE m.idempotencyKey IS NOT NULL " +
           "GROUP BY m.idempotencyKey HAVING COUNT(m) > 1")
    List<Object[]> findDuplicateIdempotencyKeys();

    /**
     * Historial de mensajes por destinatario (enmascarado para logs)
     */
    @Query("SELECT m FROM Message m WHERE CAST(m.toJson AS string) LIKE %:recipientPattern% " +
           "ORDER BY m.createdAt DESC")
    List<Message> findMessagesByRecipientPattern(@Param("recipientPattern") String recipientPattern, Pageable pageable);

    /**
     * Mensajes con múltiples fallos
     */
    @Query("SELECT m FROM Message m WHERE m.attemptCount > :threshold ORDER BY m.attemptCount DESC, m.createdAt ASC")
    List<Message> findMessagesWithManyFailures(@Param("threshold") int threshold);

    /**
     * Tasa de éxito por template
     */
    @Query("SELECT m.templateId, " +
           "COUNT(m) as total, " +
           "COUNT(CASE WHEN m.status = 'SENT' THEN 1 END) as successful, " +
           "COUNT(CASE WHEN m.status = 'DLQ' THEN 1 END) as failed " +
           "FROM Message m WHERE m.templateId IS NOT NULL " +
           "GROUP BY m.templateId")
    List<Object[]> getSuccessRateByTemplate();

    /**
     * Distribución por horas del día
     */
    @Query("SELECT EXTRACT(HOUR FROM m.createdAt), COUNT(m) FROM Message m " +
           "WHERE m.createdAt >= :afterDate " +
           "GROUP BY EXTRACT(HOUR FROM m.createdAt) " +
           "ORDER BY EXTRACT(HOUR FROM m.createdAt)")
    List<Object[]> getMessageDistributionByHour(@Param("afterDate") Instant afterDate);

    /**
     * Últimos mensajes por estado
     */
    @Query("SELECT m FROM Message m WHERE m.status = :status ORDER BY m.createdAt DESC")
    List<Message> findLatestMessagesByStatus(@Param("status") Message.MessageStatus status, Pageable pageable);
}