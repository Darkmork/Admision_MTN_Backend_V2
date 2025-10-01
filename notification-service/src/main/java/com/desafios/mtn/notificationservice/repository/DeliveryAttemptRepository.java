package com.desafios.mtn.notificationservice.repository;

import com.desafios.mtn.notificationservice.domain.DeliveryAttempt;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository para entidad DeliveryAttempt
 */
@Repository
public interface DeliveryAttemptRepository extends JpaRepository<DeliveryAttempt, UUID> {

    /**
     * Encuentra intentos por message ID ordenados por número de intento
     */
    List<DeliveryAttempt> findByMessageIdOrderByAttemptNumber(UUID messageId);

    /**
     * Encuentra el último intento de un mensaje
     */
    Optional<DeliveryAttempt> findFirstByMessageIdOrderByAttemptNumberDesc(UUID messageId);

    /**
     * Encuentra intentos por estado
     */
    List<DeliveryAttempt> findByStatus(DeliveryAttempt.DeliveryStatus status);

    /**
     * Encuentra intentos exitosos
     */
    List<DeliveryAttempt> findByStatusOrderByCreatedAtDesc(DeliveryAttempt.DeliveryStatus status);

    /**
     * Encuentra intentos fallidos
     */
    @Query("SELECT da FROM DeliveryAttempt da WHERE da.status IN ('FAILED', 'TIMEOUT', 'REJECTED') " +
           "ORDER BY da.createdAt DESC")
    List<DeliveryAttempt> findFailedAttempts();

    /**
     * Encuentra intentos por rango de fechas
     */
    @Query("SELECT da FROM DeliveryAttempt da WHERE da.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY da.createdAt DESC")
    List<DeliveryAttempt> findAttemptsBetweenDates(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    /**
     * Cuenta intentos por message ID
     */
    @Query("SELECT COUNT(da) FROM DeliveryAttempt da WHERE da.messageId = :messageId")
    int countByMessageId(@Param("messageId") UUID messageId);

    /**
     * Cuenta intentos exitosos por message ID
     */
    @Query("SELECT COUNT(da) FROM DeliveryAttempt da WHERE da.messageId = :messageId AND da.status = 'SUCCESS'")
    int countSuccessfulAttemptsByMessageId(@Param("messageId") UUID messageId);

    /**
     * Estadísticas por estado
     */
    @Query("SELECT da.status, COUNT(da) FROM DeliveryAttempt da GROUP BY da.status")
    List<Object[]> getAttemptStatisticsByStatus();

    /**
     * Estadísticas de duración promedio por estado
     */
    @Query("SELECT da.status, AVG(da.durationMs) FROM DeliveryAttempt da " +
           "WHERE da.durationMs IS NOT NULL GROUP BY da.status")
    List<Object[]> getAverageDurationByStatus();

    /**
     * Tasa de éxito general
     */
    @Query("SELECT " +
           "COUNT(*) as total, " +
           "COUNT(CASE WHEN da.status = 'SUCCESS' THEN 1 END) as successful, " +
           "COUNT(CASE WHEN da.status != 'SUCCESS' THEN 1 END) as failed " +
           "FROM DeliveryAttempt da")
    List<Object[]> getOverallSuccessRate();

    /**
     * Tasa de éxito por periodo
     */
    @Query("SELECT " +
           "COUNT(*) as total, " +
           "COUNT(CASE WHEN da.status = 'SUCCESS' THEN 1 END) as successful, " +
           "COUNT(CASE WHEN da.status != 'SUCCESS' THEN 1 END) as failed " +
           "FROM DeliveryAttempt da WHERE da.createdAt >= :afterDate")
    List<Object[]> getSuccessRateSince(@Param("afterDate") Instant afterDate);

    /**
     * Distribución de intentos por número de intento
     */
    @Query("SELECT da.attemptNumber, COUNT(da) FROM DeliveryAttempt da GROUP BY da.attemptNumber ORDER BY da.attemptNumber")
    List<Object[]> getAttemptDistributionByNumber();

    /**
     * Códigos de error más frecuentes
     */
    @Query("SELECT da.errorCode, COUNT(da) FROM DeliveryAttempt da " +
           "WHERE da.errorCode IS NOT NULL AND da.status != 'SUCCESS' " +
           "GROUP BY da.errorCode ORDER BY COUNT(da) DESC")
    List<Object[]> getMostFrequentErrorCodes();

    /**
     * Mensajes de error más frecuentes (primeras 100 chars)
     */
    @Query("SELECT SUBSTRING(da.errorMessage, 1, 100), COUNT(da) FROM DeliveryAttempt da " +
           "WHERE da.errorMessage IS NOT NULL AND da.status != 'SUCCESS' " +
           "GROUP BY SUBSTRING(da.errorMessage, 1, 100) " +
           "ORDER BY COUNT(da) DESC")
    List<Object[]> getMostFrequentErrorMessages();

    /**
     * Intentos con duración alta (posibles problemas de rendimiento)
     */
    @Query("SELECT da FROM DeliveryAttempt da WHERE da.durationMs > :thresholdMs ORDER BY da.durationMs DESC")
    List<DeliveryAttempt> findSlowAttempts(@Param("thresholdMs") long thresholdMs);

    /**
     * Estadísticas de SMTP response codes
     */
    @Query("SELECT da.smtpResponseCode, COUNT(da) FROM DeliveryAttempt da " +
           "WHERE da.smtpResponseCode IS NOT NULL " +
           "GROUP BY da.smtpResponseCode ORDER BY da.smtpResponseCode")
    List<Object[]> getSmtpResponseCodeStatistics();

    /**
     * Estadísticas de SMS segments
     */
    @Query("SELECT da.smsSegments, COUNT(da) FROM DeliveryAttempt da " +
           "WHERE da.smsSegments IS NOT NULL " +
           "GROUP BY da.smsSegments ORDER BY da.smsSegments")
    List<Object[]> getSmsSegmentStatistics();

    /**
     * Intentos por email (que tienen SMTP response code)
     */
    @Query("SELECT da FROM DeliveryAttempt da WHERE da.smtpResponseCode IS NOT NULL ORDER BY da.createdAt DESC")
    List<DeliveryAttempt> findEmailAttempts();

    /**
     * Intentos por SMS (que tienen SMS segments)
     */
    @Query("SELECT da FROM DeliveryAttempt da WHERE da.smsSegments IS NOT NULL ORDER BY da.createdAt DESC")
    List<DeliveryAttempt> findSmsAttempts();

    /**
     * Últimos intentos fallidos para análisis
     */
    @Query("SELECT da FROM DeliveryAttempt da WHERE da.status IN ('FAILED', 'TIMEOUT', 'REJECTED') " +
           "ORDER BY da.createdAt DESC")
    List<DeliveryAttempt> findRecentFailedAttempts(Pageable pageable);

    /**
     * Intentos que requieren análisis adicional
     */
    @Query("SELECT da FROM DeliveryAttempt da WHERE " +
           "(da.status = 'REJECTED') OR " +
           "(da.status = 'FAILED' AND da.attemptNumber > 3) " +
           "ORDER BY da.createdAt DESC")
    List<DeliveryAttempt> findAttemptsRequiringAnalysis();

    /**
     * Duración promedio de entrega exitosa
     */
    @Query("SELECT AVG(da.durationMs) FROM DeliveryAttempt da WHERE da.status = 'SUCCESS' AND da.durationMs IS NOT NULL")
    Double getAverageSuccessfulDeliveryDuration();

    /**
     * Duración promedio por código de respuesta SMTP
     */
    @Query("SELECT da.smtpResponseCode, AVG(da.durationMs) FROM DeliveryAttempt da " +
           "WHERE da.smtpResponseCode IS NOT NULL AND da.durationMs IS NOT NULL " +
           "GROUP BY da.smtpResponseCode ORDER BY da.smtpResponseCode")
    List<Object[]> getAverageDurationBySmtpCode();

    /**
     * Rendimiento por hora del día
     */
    @Query("SELECT EXTRACT(HOUR FROM da.createdAt) as hour, " +
           "COUNT(*) as total, " +
           "AVG(da.durationMs) as avg_duration, " +
           "COUNT(CASE WHEN da.status = 'SUCCESS' THEN 1 END) as successful " +
           "FROM DeliveryAttempt da " +
           "WHERE da.createdAt >= :afterDate AND da.durationMs IS NOT NULL " +
           "GROUP BY EXTRACT(HOUR FROM da.createdAt) " +
           "ORDER BY EXTRACT(HOUR FROM da.createdAt)")
    List<Object[]> getPerformanceByHour(@Param("afterDate") Instant afterDate);

    /**
     * Limpieza de intentos antiguos exitosos
     */
    @Query("SELECT da FROM DeliveryAttempt da WHERE da.status = 'SUCCESS' AND da.createdAt < :beforeDate")
    List<DeliveryAttempt> findOldSuccessfulAttempts(@Param("beforeDate") Instant beforeDate);

    /**
     * Encuentra intentos específicos de un mensaje con criterios
     */
    @Query("SELECT da FROM DeliveryAttempt da WHERE da.messageId = :messageId " +
           "AND (:status IS NULL OR da.status = :status) " +
           "AND (:minAttemptNumber IS NULL OR da.attemptNumber >= :minAttemptNumber) " +
           "ORDER BY da.attemptNumber")
    List<DeliveryAttempt> findMessageAttemptsByCriteria(
            @Param("messageId") UUID messageId,
            @Param("status") DeliveryAttempt.DeliveryStatus status,
            @Param("minAttemptNumber") Integer minAttemptNumber);

    /**
     * Busca intentos por patrón en respuesta del proveedor
     */
    @Query("SELECT da FROM DeliveryAttempt da WHERE da.providerResponse LIKE %:pattern% ORDER BY da.createdAt DESC")
    List<DeliveryAttempt> searchByProviderResponse(@Param("pattern") String pattern);

    /**
     * Verifica si un mensaje tiene al menos un intento exitoso
     */
    @Query("SELECT COUNT(da) > 0 FROM DeliveryAttempt da WHERE da.messageId = :messageId AND da.status = 'SUCCESS'")
    boolean hasSuccessfulAttempt(@Param("messageId") UUID messageId);

    /**
     * Obtiene el tiempo promedio hasta el primer éxito
     */
    @Query("SELECT AVG(da.durationMs) FROM DeliveryAttempt da WHERE da.status = 'SUCCESS' AND da.attemptNumber = 1")
    Double getAverageTimeToFirstSuccess();

    /**
     * Estadísticas de retry - cuántos mensajes necesitaron N intentos
     */
    @Query("SELECT successful_attempts.attempts_needed, COUNT(*) " +
           "FROM (SELECT da.messageId, MIN(da.attemptNumber) as attempts_needed " +
           "      FROM DeliveryAttempt da WHERE da.status = 'SUCCESS' " +
           "      GROUP BY da.messageId) successful_attempts " +
           "GROUP BY successful_attempts.attempts_needed " +
           "ORDER BY successful_attempts.attempts_needed")
    List<Object[]> getRetryStatistics();
}