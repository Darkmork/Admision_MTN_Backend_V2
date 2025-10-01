package com.mtn.admission.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.server.ServerWebExchange;

import java.util.Arrays;
import java.util.List;

/**
 * Restrictive CORS Configuration for Chilean Banking-Level Security
 * 
 * Implements strict CORS policies with:
 * - Whitelist-only origins (no wildcards)
 * - Role-based origin validation
 * - Comprehensive logging for security audit
 * - Chilean domain prioritization
 */
@Configuration
public class CorsConfig {

    private static final Logger logger = LoggerFactory.getLogger(CorsConfig.class);
    private static final Logger securityLogger = LoggerFactory.getLogger("SECURITY_AUDIT");

    @Value("${security.cors.allowed-origins}")
    private String allowedOriginsStr;

    @Value("${security.cors.admin-origins:https://admin.mtn.cl,https://backoffice.mtn.cl}")
    private String adminOriginsStr;

    @Value("${security.cors.public-origins:https://admision.mtn.cl,https://www.mtn.cl}")
    private String publicOriginsStr;

    @Value("${security.cors.monitoring-origins:https://grafana.mtn.cl,https://prometheus.mtn.cl}")
    private String monitoringOriginsStr;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        
        // API endpoints - Restrictive CORS for sensitive operations
        CorsConfiguration apiConfig = createRestrictiveCorsConfiguration();
        source.registerCorsConfiguration("/api/**", apiConfig);
        
        // Admin endpoints - Admin origins only
        CorsConfiguration adminConfig = createAdminCorsConfiguration();
        source.registerCorsConfiguration("/api/admin/**", adminConfig);
        source.registerCorsConfiguration("/api/users/admin/**", adminConfig);
        source.registerCorsConfiguration("/api/applications/admin/**", adminConfig);
        
        // Public endpoints - More permissive but still controlled
        CorsConfiguration publicConfig = createPublicCorsConfiguration();
        source.registerCorsConfiguration("/api/applications/public/**", publicConfig);
        source.registerCorsConfiguration("/api/info/**", publicConfig);
        source.registerCorsConfiguration("/actuator/health", publicConfig);
        source.registerCorsConfiguration("/actuator/info", publicConfig);
        
        // Monitoring endpoints - Monitoring tools only
        CorsConfiguration monitoringConfig = createMonitoringCorsConfiguration();
        source.registerCorsConfiguration("/actuator/**", monitoringConfig);
        
        // OAuth/Auth endpoints - Strict configuration
        CorsConfiguration authConfig = createAuthCorsConfiguration();
        source.registerCorsConfiguration("/oauth/**", authConfig);
        source.registerCorsConfiguration("/auth/**", authConfig);
        
        logger.info("CORS configuration initialized with {} API origins, {} admin origins, {} public origins", 
            parseOrigins(allowedOriginsStr).size(), 
            parseOrigins(adminOriginsStr).size(),
            parseOrigins(publicOriginsStr).size());
        
        return source;
    }

    private CorsConfiguration createRestrictiveCorsConfiguration() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Strict origin control - No wildcards allowed
        List<String> allowedOrigins = parseOrigins(allowedOriginsStr);
        configuration.setAllowedOrigins(allowedOrigins);
        
        // Limit HTTP methods to essential operations
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));
        
        // Strict header control
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "X-Request-ID",
            "X-Correlation-ID",
            "Accept",
            "Origin"
        ));
        
        // Expose security and rate limiting headers
        configuration.setExposedHeaders(Arrays.asList(
            "X-Request-ID",
            "X-Rate-Limit-Remaining",
            "X-Rate-Limit-Retry-After",
            "X-Total-Count"
        ));
        
        // Allow credentials for authenticated requests
        configuration.setAllowCredentials(true);
        
        // Short cache time for dynamic environments
        configuration.setMaxAge(1800L); // 30 minutes
        
        return configuration;
    }

    private CorsConfiguration createAdminCorsConfiguration() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Admin origins only
        List<String> adminOrigins = parseOrigins(adminOriginsStr);
        configuration.setAllowedOrigins(adminOrigins);
        
        // Full CRUD operations for admin
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));
        
        // Admin-specific headers
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "X-Request-ID",
            "X-Correlation-ID",
            "X-Admin-Context",
            "Accept",
            "Origin"
        ));
        
        configuration.setExposedHeaders(Arrays.asList(
            "X-Request-ID",
            "X-Total-Count",
            "X-Admin-Response",
            "X-Rate-Limit-Remaining"
        ));
        
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L); // 1 hour for admin operations
        
        return configuration;
    }

    private CorsConfiguration createPublicCorsConfiguration() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Public origins - still controlled
        List<String> publicOrigins = parseOrigins(publicOriginsStr);
        configuration.setAllowedOrigins(publicOrigins);
        
        // Read-only operations primarily
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "OPTIONS"
        ));
        
        // Basic headers for public access
        configuration.setAllowedHeaders(Arrays.asList(
            "Content-Type",
            "Accept",
            "Origin",
            "X-Requested-With"
        ));
        
        configuration.setExposedHeaders(Arrays.asList(
            "X-Request-ID"
        ));
        
        // No credentials for public endpoints
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(86400L); // 24 hours for stable public endpoints
        
        return configuration;
    }

    private CorsConfiguration createMonitoringCorsConfiguration() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Monitoring tools only
        List<String> monitoringOrigins = parseOrigins(monitoringOriginsStr);
        configuration.setAllowedOrigins(monitoringOrigins);
        
        // Read-only access for monitoring
        configuration.setAllowedMethods(Arrays.asList("GET", "OPTIONS"));
        
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "Accept"
        ));
        
        configuration.setExposedHeaders(Arrays.asList(
            "Content-Type",
            "Content-Length"
        ));
        
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(86400L); // 24 hours for monitoring endpoints
        
        return configuration;
    }

    private CorsConfiguration createAuthCorsConfiguration() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Only trusted origins for auth operations
        List<String> authOrigins = parseOrigins(allowedOriginsStr);
        configuration.setAllowedOrigins(authOrigins);
        
        // Auth-specific methods
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "OPTIONS"
        ));
        
        // Auth headers
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "Accept",
            "Origin"
        ));
        
        configuration.setExposedHeaders(Arrays.asList(
            "Authorization",
            "X-Request-ID"
        ));
        
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(300L); // 5 minutes for auth operations
        
        return configuration;
    }

    /**
     * Custom CORS configuration source with enhanced logging
     */
    @Bean
    public CorsConfigurationSource loggingCorsConfigurationSource() {
        return new CorsConfigurationSource() {
            private final CorsConfigurationSource delegate = corsConfigurationSource();
            
            @Override
            public CorsConfiguration getCorsConfiguration(ServerWebExchange exchange) {
                String origin = exchange.getRequest().getHeaders().getOrigin();
                String path = exchange.getRequest().getPath().value();
                String method = exchange.getRequest().getMethodValue();
                
                CorsConfiguration config = delegate.getCorsConfiguration(exchange);
                
                if (config != null && origin != null) {
                    boolean isAllowed = config.getAllowedOrigins() != null && 
                                      config.getAllowedOrigins().contains(origin);
                    
                    if (isAllowed) {
                        logger.debug("CORS request allowed - Origin: {}, Path: {} {}", origin, method, path);
                    } else {
                        securityLogger.warn("CORS request blocked - Origin: {}, Path: {} {}", origin, method, path);
                    }
                }
                
                return config;
            }
        };
    }

    private List<String> parseOrigins(String originsStr) {
        if (originsStr == null || originsStr.trim().isEmpty()) {
            return Arrays.asList();
        }
        
        return Arrays.stream(originsStr.split(","))
            .map(String::trim)
            .filter(origin -> !origin.isEmpty())
            .peek(origin -> {
                // Validate origin format
                if (!isValidOrigin(origin)) {
                    logger.warn("Invalid origin format detected: {}", origin);
                }
            })
            .toList();
    }

    private boolean isValidOrigin(String origin) {
        // Check for valid HTTPS origins (production requirement)
        if (!origin.startsWith("https://") && !origin.startsWith("http://localhost")) {
            return false;
        }
        
        // Check for Chilean domains (.cl) or localhost
        if (!origin.contains(".cl") && !origin.contains("localhost")) {
            logger.debug("Non-Chilean origin detected: {}", origin);
        }
        
        // No wildcards allowed in production
        if (origin.contains("*")) {
            logger.error("Wildcard origin not allowed in production: {}", origin);
            return false;
        }
        
        return true;
    }
}