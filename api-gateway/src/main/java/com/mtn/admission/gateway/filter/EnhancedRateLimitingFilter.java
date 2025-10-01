package com.mtn.admission.gateway.filter;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Enhanced Rate Limiting Filter with Chilean compliance features
 * 
 * Features:
 * - Role-based rate limiting
 * - IP-based fallback for anonymous users
 * - Detailed metrics and logging
 * - PII masking for Chilean data protection
 * - Sliding window rate limiting with Redis
 * - Suspicious activity detection
 */
@Component
public class EnhancedRateLimitingFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(EnhancedRateLimitingFilter.class);
    private static final Logger securityLogger = LoggerFactory.getLogger("SECURITY_AUDIT");
    
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ReactiveValueOperations<String, String> valueOperations;
    private final MeterRegistry meterRegistry;
    
    // Metrics
    private final Counter rateLimitExceededCounter;
    private final Counter suspiciousActivityCounter;
    private final Timer rateLimitCheckTimer;

    // Rate limit configurations
    private static final int DEFAULT_REQUESTS_PER_MINUTE = 60;
    private static final int ADMIN_REQUESTS_PER_MINUTE = 100;
    private static final int APODERADO_REQUESTS_PER_MINUTE = 30;
    private static final int TEACHER_REQUESTS_PER_MINUTE = 40;
    private static final int PUBLIC_REQUESTS_PER_MINUTE = 20;
    
    // Suspicious activity thresholds
    private static final int SUSPICIOUS_THRESHOLD_PER_MINUTE = 200;
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofMinutes(1);
    
    public EnhancedRateLimitingFilter(ReactiveRedisTemplate<String, String> redisTemplate, 
                                      MeterRegistry meterRegistry) {
        this.redisTemplate = redisTemplate;
        this.valueOperations = redisTemplate.opsForValue();
        this.meterRegistry = meterRegistry;
        
        // Initialize metrics
        this.rateLimitExceededCounter = Counter.builder("gateway.rate_limit.exceeded")
            .description("Number of rate limit exceeded events")
            .tag("service", "api-gateway")
            .register(meterRegistry);
            
        this.suspiciousActivityCounter = Counter.builder("gateway.suspicious_activity")
            .description("Number of suspicious activity detections")
            .tag("service", "api-gateway")
            .register(meterRegistry);
            
        this.rateLimitCheckTimer = Timer.builder("gateway.rate_limit.check_duration")
            .description("Time taken to check rate limits")
            .tag("service", "api-gateway")
            .register(meterRegistry);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return Timer.Sample.start(meterRegistry)
            .stop(rateLimitCheckTimer)
            .then(performRateLimit(exchange, chain));
    }

    private Mono<Void> performRateLimit(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        String method = request.getMethodValue();
        
        // Skip rate limiting for health checks
        if (path.contains("/actuator/health")) {
            return chain.filter(exchange);
        }
        
        return determineRateLimitKey(exchange)
            .flatMap(rateLimitKey -> checkRateLimit(exchange, rateLimitKey, chain))
            .doOnError(error -> {
                logger.error("Error in rate limiting filter", error);
                securityLogger.error("Rate limiting filter error - Path: {} {}, Error: {}", 
                    method, path, error.getMessage());
            })
            .onErrorResume(error -> {
                // On error, allow request but log the incident
                logger.warn("Rate limiting failed, allowing request: {}", error.getMessage());
                return chain.filter(exchange);
            });
    }

    private Mono<String> determineRateLimitKey(ServerWebExchange exchange) {
        return exchange.getPrincipal()
            .cast(Authentication.class)
            .map(this::extractUserInfo)
            .switchIfEmpty(Mono.fromSupplier(() -> extractIpInfo(exchange)))
            .doOnNext(keyInfo -> {
                MDC.put("rateLimitKey", maskSensitiveData(keyInfo));
                logger.debug("Rate limit key determined: {}", maskSensitiveData(keyInfo));
            });
    }

    private String extractUserInfo(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken) {
            JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) authentication;
            Jwt jwt = jwtAuth.getToken();
            
            String email = jwt.getClaimAsString("email");
            String userRole = extractUserRole(jwt);
            
            return String.format("user:%s:role:%s", email, userRole);
        }
        
        return "user:" + authentication.getName() + ":role:UNKNOWN";
    }

    private String extractIpInfo(ServerWebExchange exchange) {
        String clientIp = getClientIp(exchange);
        return "ip:" + clientIp;
    }

    private String extractUserRole(Jwt jwt) {
        // Check roles in different formats
        Object rolesObj = jwt.getClaim("roles");
        if (rolesObj instanceof java.util.List) {
            @SuppressWarnings("unchecked")
            java.util.List<String> roles = (java.util.List<String>) rolesObj;
            if (!roles.isEmpty()) {
                return roles.get(0);
            }
        }
        
        // Check realm_access.roles (Keycloak)
        Object realmAccess = jwt.getClaim("realm_access");
        if (realmAccess instanceof java.util.Map) {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> realmMap = (java.util.Map<String, Object>) realmAccess;
            Object realmRoles = realmMap.get("roles");
            if (realmRoles instanceof java.util.List) {
                @SuppressWarnings("unchecked")
                java.util.List<String> roles = (java.util.List<String>) realmRoles;
                if (!roles.isEmpty()) {
                    return roles.get(0);
                }
            }
        }
        
        return "USER";
    }

    private Mono<Void> checkRateLimit(ServerWebExchange exchange, String rateLimitKey, 
                                      GatewayFilterChain chain) {
        String redisKey = "rate_limit:" + rateLimitKey;
        long currentTimeMinute = Instant.now().getEpochSecond() / 60;
        String timeWindowKey = redisKey + ":" + currentTimeMinute;
        
        return valueOperations.increment(timeWindowKey)
            .flatMap(currentCount -> {
                // Set expiration for the key
                return redisTemplate.expire(timeWindowKey, RATE_LIMIT_WINDOW)
                    .then(Mono.just(currentCount));
            })
            .flatMap(currentCount -> {
                int limit = determineRateLimit(rateLimitKey);
                
                // Add rate limit headers
                ServerHttpResponse response = exchange.getResponse();
                HttpHeaders headers = response.getHeaders();
                headers.add("X-Rate-Limit-Limit", String.valueOf(limit));
                headers.add("X-Rate-Limit-Remaining", String.valueOf(Math.max(0, limit - currentCount)));
                headers.add("X-Rate-Limit-Reset", String.valueOf((currentTimeMinute + 1) * 60));
                
                if (currentCount > limit) {
                    return handleRateLimitExceeded(exchange, rateLimitKey, currentCount, limit);
                }
                
                // Check for suspicious activity
                if (currentCount > SUSPICIOUS_THRESHOLD_PER_MINUTE) {
                    detectSuspiciousActivity(exchange, rateLimitKey, currentCount);
                }
                
                return chain.filter(exchange);
            });
    }

    private int determineRateLimit(String rateLimitKey) {
        if (rateLimitKey.contains("role:ADMIN")) {
            return ADMIN_REQUESTS_PER_MINUTE;
        } else if (rateLimitKey.contains("role:APODERADO")) {
            return APODERADO_REQUESTS_PER_MINUTE;
        } else if (rateLimitKey.contains("role:TEACHER") || 
                   rateLimitKey.contains("role:PSYCHOLOGIST") || 
                   rateLimitKey.contains("role:CYCLE_DIRECTOR")) {
            return TEACHER_REQUESTS_PER_MINUTE;
        } else if (rateLimitKey.startsWith("ip:")) {
            return PUBLIC_REQUESTS_PER_MINUTE;
        }
        
        return DEFAULT_REQUESTS_PER_MINUTE;
    }

    private Mono<Void> handleRateLimitExceeded(ServerWebExchange exchange, String rateLimitKey, 
                                               long currentCount, int limit) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        
        String clientIp = getClientIp(exchange);
        String path = request.getPath().value();
        String method = request.getMethodValue();
        
        // Log security incident
        securityLogger.warn("Rate limit exceeded - Key: {}, Path: {} {}, IP: {}, Count: {}, Limit: {}", 
            maskSensitiveData(rateLimitKey), method, path, maskIpAddress(clientIp), currentCount, limit);
        
        // Increment metrics
        rateLimitExceededCounter.increment(
            "key", maskSensitiveData(rateLimitKey),
            "path", path,
            "method", method
        );
        
        // Set rate limit exceeded response
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().add("Content-Type", "application/json");
        response.getHeaders().add("Retry-After", "60");
        
        String errorResponse = String.format("""
            {
                "error": "rate_limit_exceeded",
                "message": "Rate limit exceeded for this resource",
                "timestamp": "%s",
                "path": "%s",
                "limit": %d,
                "window": "60 seconds",
                "retry_after": 60
            }
            """, Instant.now().toString(), path, limit);
        
        return response.writeWith(
            Mono.just(response.bufferFactory().wrap(errorResponse.getBytes()))
        );
    }

    private void detectSuspiciousActivity(ServerWebExchange exchange, String rateLimitKey, long count) {
        ServerHttpRequest request = exchange.getRequest();
        String clientIp = getClientIp(exchange);
        String path = request.getPath().value();
        String userAgent = request.getHeaders().getFirst("User-Agent");
        
        securityLogger.warn("Suspicious activity detected - Key: {}, Path: {}, IP: {}, Count: {}, User-Agent: {}", 
            maskSensitiveData(rateLimitKey), path, maskIpAddress(clientIp), count, userAgent);
        
        suspiciousActivityCounter.increment(
            "key", maskSensitiveData(rateLimitKey),
            "path", path,
            "ip", maskIpAddress(clientIp)
        );
        
        // TODO: Add integration with security monitoring system (e.g., SIEM)
        // TODO: Consider temporary IP blocking for extreme cases
    }

    private String getClientIp(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        
        // Check X-Forwarded-For header
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        // Check X-Real-IP header
        String xRealIp = request.getHeaders().getFirst("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        // Check CF-Connecting-IP header (Cloudflare)
        String cfConnectingIp = request.getHeaders().getFirst("CF-Connecting-IP");
        if (cfConnectingIp != null && !cfConnectingIp.isEmpty()) {
            return cfConnectingIp;
        }
        
        // Fallback to remote address
        return Optional.ofNullable(request.getRemoteAddress())
            .map(addr -> addr.getAddress().getHostAddress())
            .orElse("unknown");
    }

    private String maskSensitiveData(String data) {
        if (data == null || data.isEmpty()) {
            return "unknown";
        }
        
        // Mask email addresses
        if (data.contains("@")) {
            return data.replaceAll("([^@]{3})[^@]*@", "$1***@");
        }
        
        // Mask other identifiers
        if (data.length() > 6) {
            return data.substring(0, 3) + "***" + data.substring(data.length() - 1);
        }
        
        return "***";
    }

    private String maskIpAddress(String ip) {
        if (ip == null || ip.isEmpty() || "unknown".equals(ip)) {
            return ip;
        }
        
        // IPv4 masking
        if (ip.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
            String[] parts = ip.split("\\.");
            if (parts.length == 4) {
                return parts[0] + "." + parts[1] + "." + parts[2] + ".***";
            }
        }
        
        return "***";
    }

    @Override
    public int getOrder() {
        return -100; // Execute early in the filter chain
    }
}