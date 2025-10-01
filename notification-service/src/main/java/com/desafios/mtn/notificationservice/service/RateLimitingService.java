package com.desafios.mtn.notificationservice.service;

import com.desafios.mtn.notificationservice.config.NotificationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio de rate limiting para prevenir spam y controlar la frecuencia de envío
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitingService {

    private final NotificationProperties notificationProperties;
    private final JdbcTemplate jdbcTemplate;
    
    // Cache en memoria para rate limits frecuentes (optimización)
    private final Map<String, RateLimitEntry> inMemoryCache = new ConcurrentHashMap<>();
    
    // Constantes para tipos de rate limit
    private static final String RATE_LIMIT_EMAIL_RECIPIENT = "email_recipient";
    private static final String RATE_LIMIT_SMS_RECIPIENT = "sms_recipient";
    private static final String RATE_LIMIT_TEMPLATE = "template";
    private static final String RATE_LIMIT_GLOBAL_EMAIL = "global_email";
    private static final String RATE_LIMIT_GLOBAL_SMS = "global_sms";

    /**
     * Verifica si una operación está permitida según los rate limits
     */
    public boolean isAllowed(String channel, String identifier) {
        return isAllowed(channel, identifier, null);
    }

    /**
     * Verifica si una operación está permitida según los rate limits con template
     */
    public boolean isAllowed(String channel, String identifier, String templateId) {
        try {
            // Verificar rate limit global por canal
            if (!checkGlobalRateLimit(channel)) {
                log.warn("Global rate limit exceeded for channel: {}", channel);
                return false;
            }
            
            // Verificar rate limit por destinatario
            if (!checkRecipientRateLimit(channel, identifier)) {
                log.warn("Recipient rate limit exceeded for {}: {}", channel, maskIdentifier(identifier));
                return false;
            }
            
            // Verificar rate limit por template si está especificado
            if (templateId != null && !checkTemplateRateLimit(templateId)) {
                log.warn("Template rate limit exceeded for template: {}", templateId);
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            log.error("Error checking rate limits for {} {}: {}", channel, maskIdentifier(identifier), e.getMessage());
            // En caso de error, permitir la operación pero registrar el evento
            return true;
        }
    }

    /**
     * Registra el uso de un recurso (incrementa contadores)
     */
    @Transactional
    public void recordUsage(String channel, String identifier, String templateId) {
        try {
            Instant now = Instant.now();
            
            // Registrar uso global
            recordUsageInDatabase(getGlobalRateLimitKey(channel), "global", now);
            
            // Registrar uso por destinatario
            recordUsageInDatabase(getRecipientRateLimitKey(channel), identifier, now);
            
            // Registrar uso por template si está especificado
            if (templateId != null) {
                recordUsageInDatabase(RATE_LIMIT_TEMPLATE, templateId, now);
            }
            
            // Actualizar cache en memoria
            updateInMemoryCache(channel, identifier, now);
            
        } catch (Exception e) {
            log.error("Error recording usage for {} {}: {}", channel, maskIdentifier(identifier), e.getMessage());
        }
    }

    /**
     * Verifica rate limit global por canal
     */
    private boolean checkGlobalRateLimit(String channel) {
        String keyType = getGlobalRateLimitKey(channel);
        int maxPerHour = getGlobalMaxPerHour(channel);
        int maxPerMinute = getGlobalMaxPerMinute(channel);
        
        if (maxPerHour <= 0 && maxPerMinute <= 0) {
            return true; // Rate limiting deshabilitado
        }
        
        return checkRateLimit(keyType, "global", maxPerMinute, maxPerHour);
    }

    /**
     * Verifica rate limit por destinatario
     */
    private boolean checkRecipientRateLimit(String channel, String identifier) {
        String keyType = getRecipientRateLimitKey(channel);
        int maxPerHour = getRecipientMaxPerHour(channel);
        int maxPerMinute = getRecipientMaxPerMinute(channel);
        
        if (maxPerHour <= 0 && maxPerMinute <= 0) {
            return true; // Rate limiting deshabilitado
        }
        
        return checkRateLimit(keyType, identifier, maxPerMinute, maxPerHour);
    }

    /**
     * Verifica rate limit por template
     */
    private boolean checkTemplateRateLimit(String templateId) {
        // Para templates, usar límites más generosos
        int maxPerMinute = 100; // 100 por minuto
        int maxPerHour = 1000; // 1000 por hora
        
        return checkRateLimit(RATE_LIMIT_TEMPLATE, templateId, maxPerMinute, maxPerHour);
    }

    /**
     * Verifica rate limit genérico
     */
    private boolean checkRateLimit(String keyType, String keyValue, int maxPerMinute, int maxPerHour) {
        Instant now = Instant.now();
        
        // Verificar límite por minuto si está configurado
        if (maxPerMinute > 0) {
            Instant windowStart = now.minus(1, ChronoUnit.MINUTES);
            int countLastMinute = getRateLimitCount(keyType, keyValue, windowStart, now);
            
            if (countLastMinute >= maxPerMinute) {
                log.debug("Rate limit exceeded for {} {}: {} >= {} per minute", 
                         keyType, keyValue, countLastMinute, maxPerMinute);
                return false;
            }
        }
        
        // Verificar límite por hora si está configurado
        if (maxPerHour > 0) {
            Instant windowStart = now.minus(1, ChronoUnit.HOURS);
            int countLastHour = getRateLimitCount(keyType, keyValue, windowStart, now);
            
            if (countLastHour >= maxPerHour) {
                log.debug("Rate limit exceeded for {} {}: {} >= {} per hour", 
                         keyType, keyValue, countLastHour, maxPerHour);
                return false;
            }
        }
        
        return true;
    }

    /**
     * Obtiene el conteo de rate limit desde la base de datos
     */
    private int getRateLimitCount(String keyType, String keyValue, Instant windowStart, Instant windowEnd) {
        String sql = """
            SELECT COALESCE(SUM(count), 0) 
            FROM rate_limits 
            WHERE key_type = ? AND key_value = ? 
            AND window_start >= ? AND window_end <= ?
            """;
            
        try {
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, 
                    keyType, keyValue, windowStart, windowEnd);
            return count != null ? count : 0;
        } catch (Exception e) {
            log.warn("Error querying rate limit count: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Registra uso en la base de datos
     */
    private void recordUsageInDatabase(String keyType, String keyValue, Instant timestamp) {
        // Usar ventanas de 1 minuto para granularidad
        Instant windowStart = timestamp.truncatedTo(ChronoUnit.MINUTES);
        Instant windowEnd = windowStart.plus(1, ChronoUnit.MINUTES);
        
        String upsertSql = """
            INSERT INTO rate_limits (key_type, key_value, window_start, window_end, count, created_at, updated_at)
            VALUES (?, ?, ?, ?, 1, ?, ?)
            ON CONFLICT (key_type, key_value, window_start)
            DO UPDATE SET count = rate_limits.count + 1, updated_at = ?
            """;
            
        try {
            jdbcTemplate.update(upsertSql, 
                    keyType, keyValue, windowStart, windowEnd, 
                    timestamp, timestamp, timestamp);
        } catch (Exception e) {
            log.warn("Error recording rate limit usage: {}", e.getMessage());
        }
    }

    /**
     * Actualiza cache en memoria para consultas frecuentes
     */
    private void updateInMemoryCache(String channel, String identifier, Instant timestamp) {
        String cacheKey = channel + ":" + identifier;
        RateLimitEntry entry = inMemoryCache.computeIfAbsent(cacheKey, 
                k -> new RateLimitEntry(identifier, timestamp));
        entry.update(timestamp);
        
        // Limpiar entradas antiguas del cache (>1 hora)
        if (inMemoryCache.size() > 10000) { // Limitar tamaño del cache
            inMemoryCache.entrySet().removeIf(e -> 
                    e.getValue().getLastUpdate().isBefore(timestamp.minus(1, ChronoUnit.HOURS)));
        }
    }

    /**
     * Limpia rate limits expirados de la base de datos
     */
    @Transactional
    public int cleanupExpiredRateLimits() {
        String sql = "DELETE FROM rate_limits WHERE window_end < ? - INTERVAL '1 hour'";
        
        try {
            int deleted = jdbcTemplate.update(sql, Instant.now());
            if (deleted > 0) {
                log.info("Cleaned up {} expired rate limit entries", deleted);
            }
            return deleted;
        } catch (Exception e) {
            log.error("Error cleaning up expired rate limits: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Obtiene estadísticas de rate limiting
     */
    public RateLimitStats getStats() {
        try {
            String sql = """
                SELECT 
                    key_type,
                    COUNT(*) as entries,
                    SUM(count) as total_requests,
                    COUNT(CASE WHEN limit_exceeded THEN 1 END) as exceeded_count
                FROM rate_limits 
                WHERE window_end > ? - INTERVAL '24 hours'
                GROUP BY key_type
                """;
                
            Instant oneDayAgo = Instant.now().minus(24, ChronoUnit.HOURS);
            
            Map<String, RateLimitTypeStats> typeStats = new ConcurrentHashMap<>();
            
            jdbcTemplate.query(sql, rs -> {
                String keyType = rs.getString("key_type");
                int entries = rs.getInt("entries");
                long totalRequests = rs.getLong("total_requests");
                int exceededCount = rs.getInt("exceeded_count");
                
                typeStats.put(keyType, new RateLimitTypeStats(
                        keyType, entries, totalRequests, exceededCount));
            }, oneDayAgo);
            
            return new RateLimitStats(typeStats, inMemoryCache.size());
            
        } catch (Exception e) {
            log.error("Error getting rate limit stats: {}", e.getMessage());
            return new RateLimitStats(Map.of(), 0);
        }
    }

    // ======================
    // UTILITY METHODS
    // ======================

    private String getGlobalRateLimitKey(String channel) {
        return "email".equals(channel) ? RATE_LIMIT_GLOBAL_EMAIL : RATE_LIMIT_GLOBAL_SMS;
    }

    private String getRecipientRateLimitKey(String channel) {
        return "email".equals(channel) ? RATE_LIMIT_EMAIL_RECIPIENT : RATE_LIMIT_SMS_RECIPIENT;
    }

    private int getGlobalMaxPerMinute(String channel) {
        if ("email".equals(channel)) {
            return notificationProperties.getEmail().getRateLimit().getMaxPerMinute();
        } else {
            return notificationProperties.getSms().getRateLimit().getMaxPerMinute();
        }
    }

    private int getGlobalMaxPerHour(String channel) {
        if ("email".equals(channel)) {
            return notificationProperties.getEmail().getRateLimit().getMaxPerHour();
        } else {
            return notificationProperties.getSms().getRateLimit().getMaxPerHour();
        }
    }

    private int getRecipientMaxPerMinute(String channel) {
        // Por destinatario, ser más restrictivo
        return Math.max(1, getGlobalMaxPerMinute(channel) / 10);
    }

    private int getRecipientMaxPerHour(String channel) {
        // Por destinatario, ser más restrictivo
        return Math.max(10, getGlobalMaxPerHour(channel) / 10);
    }

    private String maskIdentifier(String identifier) {
        if (identifier == null || identifier.length() < 4) {
            return "***";
        }
        
        if (identifier.contains("@")) {
            // Email
            int atIndex = identifier.indexOf('@');
            if (atIndex > 2) {
                return identifier.substring(0, 2) + "***" + identifier.substring(atIndex);
            }
        } else {
            // Teléfono
            return "***" + identifier.substring(identifier.length() - 4);
        }
        
        return "***";
    }

    // ======================
    // INNER CLASSES
    // ======================

    /**
     * Entrada del cache en memoria
     */
    private static class RateLimitEntry {
        private final String identifier;
        private Instant lastUpdate;
        private int countLastMinute = 1;

        public RateLimitEntry(String identifier, Instant timestamp) {
            this.identifier = identifier;
            this.lastUpdate = timestamp;
        }

        public void update(Instant timestamp) {
            if (lastUpdate.until(timestamp, ChronoUnit.MINUTES) >= 1) {
                countLastMinute = 1; // Nueva ventana de tiempo
            } else {
                countLastMinute++;
            }
            lastUpdate = timestamp;
        }

        public Instant getLastUpdate() {
            return lastUpdate;
        }

        public int getCountLastMinute() {
            return countLastMinute;
        }
    }

    /**
     * Estadísticas por tipo de rate limit
     */
    public static class RateLimitTypeStats {
        private final String keyType;
        private final int entries;
        private final long totalRequests;
        private final int exceededCount;

        public RateLimitTypeStats(String keyType, int entries, long totalRequests, int exceededCount) {
            this.keyType = keyType;
            this.entries = entries;
            this.totalRequests = totalRequests;
            this.exceededCount = exceededCount;
        }

        // Getters
        public String getKeyType() { return keyType; }
        public int getEntries() { return entries; }
        public long getTotalRequests() { return totalRequests; }
        public int getExceededCount() { return exceededCount; }
    }

    /**
     * Estadísticas generales de rate limiting
     */
    public static class RateLimitStats {
        private final Map<String, RateLimitTypeStats> typeStats;
        private final int cacheSize;

        public RateLimitStats(Map<String, RateLimitTypeStats> typeStats, int cacheSize) {
            this.typeStats = typeStats;
            this.cacheSize = cacheSize;
        }

        // Getters
        public Map<String, RateLimitTypeStats> getTypeStats() { return typeStats; }
        public int getCacheSize() { return cacheSize; }
    }
}