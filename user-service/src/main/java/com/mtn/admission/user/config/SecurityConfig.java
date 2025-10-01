package com.mtn.admission.user.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.AuthenticationEntryPoint;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.stream.Collectors;

/**
 * User Service Security Configuration
 * 
 * Implements specialized RBAC for user management:
 * - User authentication and validation (all authenticated users)
 * - Profile management (self-service for own profile, Admin for all)
 * - User CRUD operations (Admin only)
 * - Role and permission management (Admin only)
 * - User statistics and audit (Admin and Coordinators)
 * - Password reset and email verification (self-service + Admin)
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);
    private static final Logger auditLogger = LoggerFactory.getLogger("SECURITY_AUDIT");

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    @Value("${security.cors.allowed-origins:https://admision.mtn.cl,https://admin.mtn.cl,https://api.mtn.cl}")
    private String[] allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            // Disable CSRF for stateless API
            .csrf(csrf -> csrf.disable())
            
            // Configure session management (stateless)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // Configure OAuth2 Resource Server
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .decoder(jwtDecoder())
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
                .authenticationEntryPoint(customAuthenticationEntryPoint())
                .accessDeniedHandler(customAccessDeniedHandler())
            )
            
            // Configure authorization rules for User Service
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/info").permitAll()
                
                // Public user registration and verification
                .requestMatchers(HttpMethod.POST, "/users/register").permitAll()
                .requestMatchers(HttpMethod.POST, "/users/verify-email").permitAll()
                .requestMatchers(HttpMethod.POST, "/users/resend-verification").permitAll()
                .requestMatchers(HttpMethod.POST, "/users/forgot-password").permitAll()
                .requestMatchers(HttpMethod.POST, "/users/reset-password").permitAll()
                
                // Public RUT validation for Chilean users
                .requestMatchers(HttpMethod.GET, "/users/validate-rut/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/users/validate-rut").permitAll()
                
                // User authentication and token validation - All authenticated users
                .requestMatchers(HttpMethod.POST, "/users/authenticate").hasAnyRole("ADMIN", "TEACHER", "PSYCHOLOGIST", "CYCLE_DIRECTOR", "COORDINATOR", "APODERADO")
                .requestMatchers(HttpMethod.GET, "/users/me").hasAnyRole("ADMIN", "TEACHER", "PSYCHOLOGIST", "CYCLE_DIRECTOR", "COORDINATOR", "APODERADO")
                .requestMatchers(HttpMethod.GET, "/users/validate-token").hasAnyRole("ADMIN", "TEACHER", "PSYCHOLOGIST", "CYCLE_DIRECTOR", "COORDINATOR", "APODERADO", "SYSTEM")
                
                // Profile management - Self-service and Admin
                .requestMatchers(HttpMethod.GET, "/users/*/profile").hasAnyRole("ADMIN", "TEACHER", "PSYCHOLOGIST", "CYCLE_DIRECTOR", "COORDINATOR", "APODERADO")
                .requestMatchers(HttpMethod.PUT, "/users/*/profile").hasAnyRole("ADMIN", "TEACHER", "PSYCHOLOGIST", "CYCLE_DIRECTOR", "COORDINATOR", "APODERADO")
                .requestMatchers(HttpMethod.PATCH, "/users/*/profile").hasAnyRole("ADMIN", "TEACHER", "PSYCHOLOGIST", "CYCLE_DIRECTOR", "COORDINATOR", "APODERADO")
                
                // Password changes - Self-service and Admin
                .requestMatchers(HttpMethod.PUT, "/users/*/password").hasAnyRole("ADMIN", "TEACHER", "PSYCHOLOGIST", "CYCLE_DIRECTOR", "COORDINATOR", "APODERADO")
                .requestMatchers(HttpMethod.POST, "/users/change-password").hasAnyRole("ADMIN", "TEACHER", "PSYCHOLOGIST", "CYCLE_DIRECTOR", "COORDINATOR", "APODERADO")
                
                // User CRUD operations - Admin only
                .requestMatchers(HttpMethod.GET, "/users").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/users/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/users").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/users/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/users/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/users/**").hasRole("ADMIN")
                
                // User activation and deactivation - Admin only
                .requestMatchers(HttpMethod.POST, "/users/*/activate").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/users/*/deactivate").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/users/*/suspend").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/users/*/unsuspend").hasRole("ADMIN")
                
                // Role and permission management - Admin only
                .requestMatchers(HttpMethod.GET, "/users/*/roles").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/users/*/roles").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/users/*/roles/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/roles/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/roles/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/roles/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/roles/**").hasRole("ADMIN")
                
                // User queries by role and specialization - Admin and Coordinators
                .requestMatchers(HttpMethod.GET, "/users/by-role/**").hasAnyRole("ADMIN", "COORDINATOR")
                .requestMatchers(HttpMethod.GET, "/users/teachers/**").hasAnyRole("ADMIN", "COORDINATOR")
                .requestMatchers(HttpMethod.GET, "/users/psychologists/**").hasAnyRole("ADMIN", "COORDINATOR")
                .requestMatchers(HttpMethod.GET, "/users/cycle-directors/**").hasAnyRole("ADMIN", "COORDINATOR")
                .requestMatchers(HttpMethod.GET, "/users/available-evaluators").hasAnyRole("ADMIN", "COORDINATOR")
                
                // User search and filtering - Admin and Coordinators
                .requestMatchers(HttpMethod.GET, "/users/search/**").hasAnyRole("ADMIN", "COORDINATOR")
                .requestMatchers(HttpMethod.POST, "/users/search").hasAnyRole("ADMIN", "COORDINATOR")
                .requestMatchers(HttpMethod.GET, "/users/filter/**").hasAnyRole("ADMIN", "COORDINATOR")
                
                // User statistics and reports - Admin and Coordinators
                .requestMatchers(HttpMethod.GET, "/users/statistics/**").hasAnyRole("ADMIN", "COORDINATOR")
                .requestMatchers(HttpMethod.GET, "/users/reports/**").hasAnyRole("ADMIN", "COORDINATOR")
                .requestMatchers(HttpMethod.GET, "/users/analytics/**").hasRole("ADMIN")
                
                // Email verification management - Admin only
                .requestMatchers(HttpMethod.POST, "/users/*/send-verification").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/users/*/mark-verified").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/users/unverified").hasRole("ADMIN")
                
                // User session management - Admin only
                .requestMatchers(HttpMethod.GET, "/users/*/sessions").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/users/*/sessions/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/users/*/force-logout").hasRole("ADMIN")
                
                // Bulk operations - Admin only
                .requestMatchers(HttpMethod.POST, "/users/bulk/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/users/bulk/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/users/bulk/**").hasRole("ADMIN")
                
                // User audit and activity logs - Admin only
                .requestMatchers(HttpMethod.GET, "/users/*/audit/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/users/activity-log/**").hasRole("ADMIN")
                
                // User export and import - Admin only
                .requestMatchers(HttpMethod.GET, "/users/export/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/users/import/**").hasRole("ADMIN")
                
                // Service-to-service user lookups - System services
                .requestMatchers(HttpMethod.GET, "/internal/users/by-email/**").hasRole("SYSTEM")
                .requestMatchers(HttpMethod.GET, "/internal/users/by-id/**").hasRole("SYSTEM")
                .requestMatchers(HttpMethod.POST, "/internal/users/validate-credentials").hasRole("SYSTEM")
                
                // Monitoring endpoints - Admin only
                .requestMatchers("/actuator/**").hasRole("ADMIN")
                
                // All other requests must be authenticated
                .anyRequest().authenticated()
            )
            
            // Configure CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // Security headers
            .headers(headers -> headers
                .frameOptions(frameOptions -> frameOptions.deny())
                .contentSecurityPolicy("default-src 'self'; frame-ancestors 'none';")
                .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                    .maxAgeInSeconds(31536000)
                    .includeSubdomains(true))
            )
            
            .build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return JwtDecoders.fromIssuerLocation(issuerUri);
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter());
        return converter;
    }

    @Bean
    public Converter<Jwt, Collection<GrantedAuthority>> jwtGrantedAuthoritiesConverter() {
        return jwt -> {
            // Extract roles from different claim formats
            Set<String> roles = new HashSet<>();
            
            // Format 1: Direct "roles" claim
            Object rolesObj = jwt.getClaim("roles");
            if (rolesObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> rolesList = (List<String>) rolesObj;
                roles.addAll(rolesList);
            }
            
            // Format 2: Keycloak "realm_access.roles" format
            Object realmAccess = jwt.getClaim("realm_access");
            if (realmAccess instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> realmMap = (Map<String, Object>) realmAccess;
                Object realmRoles = realmMap.get("roles");
                if (realmRoles instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<String> rolesList = (List<String>) realmRoles;
                    roles.addAll(rolesList);
                }
            }
            
            // Format 3: Resource access format
            Object resourceAccess = jwt.getClaim("resource_access");
            if (resourceAccess instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> resourceMap = (Map<String, Object>) resourceAccess;
                Object clientRoles = resourceMap.get("user-service");
                if (clientRoles instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> clientMap = (Map<String, Object>) clientRoles;
                    Object clientRolesList = clientMap.get("roles");
                    if (clientRolesList instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<String> rolesList = (List<String>) clientRolesList;
                        roles.addAll(rolesList);
                    }
                }
            }
            
            // Convert to granted authorities
            Collection<GrantedAuthority> authorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority(role.startsWith("ROLE_") ? role : "ROLE_" + role))
                .collect(Collectors.toList());
            
            // Log authentication success (with PII masking)
            String subject = jwt.getSubject();
            String email = jwt.getClaimAsString("email");
            auditLogger.info("JWT authentication successful in User Service - Subject: {}, Email: {}, Roles: {}", 
                subject, maskEmail(email), roles);
                
            return authorities;
        };
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization", "Content-Type", "X-Requested-With", "X-Request-ID", "X-Correlation-ID"));
        configuration.setExposedHeaders(Arrays.asList("X-Request-ID", "X-Total-Count"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationEntryPoint customAuthenticationEntryPoint() {
        return (request, response, authException) -> {
            // Log authentication failure
            String path = request.getRequestURI();
            String method = request.getMethod();
            String clientIp = getClientIp(request);
            
            auditLogger.warn("Authentication failed in User Service - Path: {} {}, IP: {}, Reason: {}", 
                method, path, maskIpAddress(clientIp), authException.getMessage());
            
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.setHeader("WWW-Authenticate", "Bearer realm=\"MTN User Service\"");
            
            String errorResponse = """
                {
                    "error": "unauthorized",
                    "message": "Authentication required for user service",
                    "timestamp": "%s",
                    "path": "%s",
                    "service": "user-service"
                }
                """.formatted(java.time.Instant.now().toString(), path);
                
            response.getWriter().write(errorResponse);
        };
    }

    @Bean
    public AccessDeniedHandler customAccessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            // Log access denied
            String path = request.getRequestURI();
            String method = request.getMethod();
            String clientIp = getClientIp(request);
            String userInfo = "anonymous";
            
            // Try to get user info from request
            if (request.getUserPrincipal() != null) {
                userInfo = maskEmail(request.getUserPrincipal().getName());
            }
            
            auditLogger.warn("Access denied in User Service - User: {}, Path: {} {}, IP: {}, Reason: {}", 
                userInfo, method, path, maskIpAddress(clientIp), accessDeniedException.getMessage());
            
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            
            String errorResponse = """
                {
                    "error": "access_denied", 
                    "message": "Insufficient permissions for user service",
                    "timestamp": "%s",
                    "path": "%s",
                    "service": "user-service",
                    "required_roles": "Check endpoint documentation for required roles"
                }
                """.formatted(java.time.Instant.now().toString(), path);
                
            response.getWriter().write(errorResponse);
        };
    }

    /**
     * Extract client IP address from request
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    /**
     * Mask email addresses for Chilean PII compliance
     */
    private String maskEmail(String email) {
        if (email == null || email.isEmpty()) {
            return "unknown";
        }
        
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return "***@***";
        }
        
        String localPart = email.substring(0, atIndex);
        if (localPart.length() <= 3) {
            return localPart.charAt(0) + "***@***";
        }
        
        return localPart.substring(0, 3) + "***@***";
    }

    /**
     * Mask IP addresses for logging
     */
    private String maskIpAddress(String ip) {
        if (ip == null || ip.isEmpty()) {
            return "unknown";
        }
        
        if (ip.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
            String[] parts = ip.split("\\.");
            if (parts.length == 4) {
                return parts[0] + "." + parts[1] + "." + parts[2] + ".***";
            }
        }
        
        return "***";
    }
}