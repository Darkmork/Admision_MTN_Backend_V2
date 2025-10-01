package com.mtn.admission.gateway.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.util.Optional;

/**
 * Rate Limiting Key Resolvers for MTN Admission API Gateway
 * 
 * Provides different strategies for rate limiting based on:
 * - Authenticated user principal (preferred for logged-in users)
 * - IP address (fallback for unauthenticated requests) 
 * - Combined principal+IP for enhanced security
 * - Role-based rate limiting for different access patterns
 */
@Configuration
public class KeyResolvers {
    
    private static final Logger logger = LoggerFactory.getLogger(KeyResolvers.class);
    private static final String ANONYMOUS_USER = "anonymous";
    private static final String UNKNOWN_IP = "unknown";

    /**
     * Primary key resolver: Use authenticated principal if available, otherwise fall back to IP
     * This provides the best user experience while maintaining security
     */
    @Bean
    KeyResolver principalOrIpKeyResolver() {
        return exchange -> {
            return exchange.getPrincipal()
                .cast(Authentication.class)
                .map(this::extractUserIdentifier)
                .switchIfEmpty(getClientIpAddress(exchange))
                .doOnNext(key -> {
                    // Add rate limit key to MDC for logging correlation
                    MDC.put("rateLimitKey", key);
                    logger.debug("Rate limit key resolved: {}", maskSensitiveData(key));
                })
                .doOnError(error -> logger.warn("Error resolving rate limit key", error))
                .onErrorReturn(UNKNOWN_IP);
        };
    }

    /**
     * IP-based key resolver for public endpoints and fallback scenarios
     */
    @Bean
    KeyResolver ipKeyResolver() {
        return exchange -> {
            return getClientIpAddress(exchange)
                .doOnNext(ip -> {
                    MDC.put("rateLimitKey", ip);
                    MDC.put("rateLimitType", "ip");
                    logger.debug("IP-based rate limit key: {}", maskIpAddress(ip));
                });
        };
    }

    /**
     * Role-based key resolver for different rate limits per user role
     */
    @Bean
    KeyResolver roleBasedKeyResolver() {
        return exchange -> {
            return exchange.getPrincipal()
                .cast(Authentication.class)
                .map(auth -> {
                    String userIdentifier = extractUserIdentifier(auth);
                    String userRole = extractUserRole(auth);
                    String roleBasedKey = userRole + ":" + userIdentifier;
                    
                    MDC.put("rateLimitKey", roleBasedKey);
                    MDC.put("rateLimitType", "role-based");
                    MDC.put("userRole", userRole);
                    
                    logger.debug("Role-based rate limit key: {}:{}", userRole, maskSensitiveData(userIdentifier));
                    return roleBasedKey;
                })
                .switchIfEmpty(getClientIpAddress(exchange).map(ip -> "ANONYMOUS:" + ip))
                .onErrorReturn("ERROR:" + UNKNOWN_IP);
        };
    }

    /**
     * Composite key resolver combining user and IP for enhanced security
     * Useful for detecting potential account compromise or abuse
     */
    @Bean
    KeyResolver compositeKeyResolver() {
        return exchange -> {
            Mono<String> userMono = exchange.getPrincipal()
                .cast(Authentication.class)
                .map(this::extractUserIdentifier)
                .defaultIfEmpty(ANONYMOUS_USER);
                
            Mono<String> ipMono = getClientIpAddress(exchange);
            
            return Mono.zip(userMono, ipMono)
                .map(tuple -> {
                    String user = tuple.getT1();
                    String ip = tuple.getT2();
                    String compositeKey = user + ":" + ip;
                    
                    MDC.put("rateLimitKey", compositeKey);
                    MDC.put("rateLimitType", "composite");
                    
                    logger.debug("Composite rate limit key: {}:{}", 
                        maskSensitiveData(user), maskIpAddress(ip));
                    
                    return compositeKey;
                })
                .onErrorReturn(ANONYMOUS_USER + ":" + UNKNOWN_IP);
        };
    }

    /**
     * Admin-specific key resolver with stricter limits
     */
    @Bean
    KeyResolver adminKeyResolver() {
        return exchange -> {
            return exchange.getPrincipal()
                .cast(Authentication.class)
                .filter(this::isAdminUser)
                .map(auth -> {
                    String userIdentifier = extractUserIdentifier(auth);
                    String adminKey = "ADMIN:" + userIdentifier;
                    
                    MDC.put("rateLimitKey", adminKey);
                    MDC.put("rateLimitType", "admin");
                    MDC.put("isAdmin", "true");
                    
                    logger.info("Admin rate limit key: {}", maskSensitiveData(userIdentifier));
                    return adminKey;
                })
                .switchIfEmpty(Mono.error(new SecurityException("Admin key resolver used for non-admin user")))
                .onErrorReturn("INVALID_ADMIN");
        };
    }

    /**
     * Extract user identifier from authentication token
     */
    private String extractUserIdentifier(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken) {
            JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) authentication;
            Jwt jwt = jwtAuth.getToken();
            
            // Prefer email as stable identifier, fall back to subject
            String email = jwt.getClaimAsString("email");
            if (email != null && !email.isEmpty()) {
                return email;
            }
            
            String subject = jwt.getSubject();
            return subject != null ? subject : ANONYMOUS_USER;
        }
        
        return authentication.getName() != null ? authentication.getName() : ANONYMOUS_USER;
    }

    /**
     * Extract user role from JWT token for role-based rate limiting
     */
    private String extractUserRole(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken) {
            JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) authentication;
            Jwt jwt = jwtAuth.getToken();
            
            // Check for roles in different claim formats
            Object rolesObj = jwt.getClaim("roles");
            if (rolesObj instanceof java.util.List) {
                @SuppressWarnings("unchecked")
                java.util.List<String> roles = (java.util.List<String>) rolesObj;
                if (!roles.isEmpty()) {
                    String primaryRole = roles.get(0); // Use first role as primary
                    return primaryRole.startsWith("ROLE_") ? primaryRole.substring(5) : primaryRole;
                }
            }
            
            // Check realm_access.roles format (Keycloak)
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
        }
        
        // Check Spring Security authorities
        return authentication.getAuthorities().stream()
            .findFirst()
            .map(auth -> auth.getAuthority().startsWith("ROLE_") ? 
                auth.getAuthority().substring(5) : auth.getAuthority())
            .orElse("USER");
    }

    /**
     * Check if user has admin role
     */
    private boolean isAdminUser(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken) {
            JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) authentication;
            Jwt jwt = jwtAuth.getToken();
            
            // Check roles claim
            Object rolesObj = jwt.getClaim("roles");
            if (rolesObj instanceof java.util.List) {
                @SuppressWarnings("unchecked")
                java.util.List<String> roles = (java.util.List<String>) rolesObj;
                return roles.contains("ADMIN") || roles.contains("ROLE_ADMIN");
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
                    return roles.contains("ADMIN");
                }
            }
        }
        
        // Check Spring Security authorities
        return authentication.getAuthorities().stream()
            .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN") || 
                           auth.getAuthority().equals("ADMIN"));
    }

    /**
     * Extract client IP address with proper proxy header handling
     */
    private Mono<String> getClientIpAddress(ServerWebExchange exchange) {
        return Mono.fromCallable(() -> {
            ServerHttpRequest request = exchange.getRequest();
            
            // Check X-Forwarded-For header (load balancers, proxies)
            String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                // Take first IP in comma-separated list
                String clientIp = xForwardedFor.split(",")[0].trim();
                if (!clientIp.isEmpty() && !"unknown".equalsIgnoreCase(clientIp)) {
                    return clientIp;
                }
            }
            
            // Check X-Real-IP header (Nginx)
            String xRealIp = request.getHeaders().getFirst("X-Real-IP");
            if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
                return xRealIp;
            }
            
            // Check CF-Connecting-IP header (Cloudflare)
            String cfConnectingIp = request.getHeaders().getFirst("CF-Connecting-IP");
            if (cfConnectingIp != null && !cfConnectingIp.isEmpty()) {
                return cfConnectingIp;
            }
            
            // Fall back to remote address
            return Optional.ofNullable(request.getRemoteAddress())
                .map(addr -> addr.getAddress().getHostAddress())
                .orElse(UNKNOWN_IP);
        })
        .onErrorReturn(UNKNOWN_IP);
    }

    /**
     * Mask sensitive data in logs (email addresses)
     */
    private String maskSensitiveData(String data) {
        if (data == null || data.isEmpty() || ANONYMOUS_USER.equals(data)) {
            return data;
        }
        
        // Mask email addresses
        if (data.contains("@")) {
            int atIndex = data.indexOf('@');
            if (atIndex > 2) {
                return data.substring(0, 3) + "***@" + data.substring(atIndex + 1);
            } else {
                return "***@" + data.substring(atIndex + 1);
            }
        }
        
        // Mask other identifiers (keep first 3 characters)
        if (data.length() > 6) {
            return data.substring(0, 3) + "***" + data.substring(data.length() - 1);
        }
        
        return "***";
    }

    /**
     * Mask IP addresses (keep first 3 octets for IPv4)
     */
    private String maskIpAddress(String ip) {
        if (ip == null || ip.isEmpty() || UNKNOWN_IP.equals(ip)) {
            return ip;
        }
        
        // IPv4 address masking
        if (ip.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
            String[] parts = ip.split("\\.");
            if (parts.length == 4) {
                return parts[0] + "." + parts[1] + "." + parts[2] + ".***";
            }
        }
        
        // IPv6 or other formats - mask last part
        int lastColonIndex = ip.lastIndexOf(':');
        if (lastColonIndex > 0) {
            return ip.substring(0, lastColonIndex) + ":***";
        }
        
        return "***";
    }
}