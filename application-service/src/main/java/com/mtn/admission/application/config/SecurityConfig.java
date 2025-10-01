package com.mtn.admission.application.config;

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
 * Application Service Security Configuration
 * 
 * Implements fine-grained RBAC for application management endpoints:
 * - Application submission (APODERADO, ADMIN)
 * - Status changes (ADMIN, CYCLE_DIRECTOR)  
 * - Document management (APODERADO, ADMIN)
 * - Application reading (multiple roles based on context)
 * - Administrative operations (ADMIN only)
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
            
            // Configure authorization rules for Application Service
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/info").permitAll()
                .requestMatchers(HttpMethod.GET, "/applications/public/**").permitAll()
                
                // Application submission - Apoderado and Admin
                .requestMatchers(HttpMethod.POST, "/applications").hasAnyRole("APODERADO", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/applications/submit").hasAnyRole("APODERADO", "ADMIN")
                
                // Document management - Apoderado (own applications) and Admin (all)
                .requestMatchers(HttpMethod.POST, "/applications/*/documents/**").hasAnyRole("APODERADO", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/applications/*/documents/**").hasAnyRole("APODERADO", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/applications/*/documents/**").hasAnyRole("APODERADO", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/applications/*/documents/**").hasAnyRole("APODERADO", "ADMIN", "CYCLE_DIRECTOR", "COORDINATOR")
                
                // Status changes - Admin and Cycle Directors only
                .requestMatchers(HttpMethod.POST, "/applications/*/state-changes").hasAnyRole("ADMIN", "CYCLE_DIRECTOR")
                .requestMatchers(HttpMethod.PUT, "/applications/*/status").hasAnyRole("ADMIN", "CYCLE_DIRECTOR")
                .requestMatchers(HttpMethod.PATCH, "/applications/*/status").hasAnyRole("ADMIN", "CYCLE_DIRECTOR")
                
                // Application updates - Apoderado (own) and Admin (all)
                .requestMatchers(HttpMethod.PUT, "/applications/*").hasAnyRole("APODERADO", "ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/applications/*").hasAnyRole("APODERADO", "ADMIN")
                
                // Application reading - Role-based access with context
                .requestMatchers(HttpMethod.GET, "/applications/me/**").hasRole("APODERADO")
                .requestMatchers(HttpMethod.GET, "/applications/admin/**").hasAnyRole("ADMIN", "COORDINATOR")
                .requestMatchers(HttpMethod.GET, "/applications/*/full").hasAnyRole("ADMIN", "CYCLE_DIRECTOR")
                .requestMatchers(HttpMethod.GET, "/applications/**").hasAnyRole("ADMIN", "CYCLE_DIRECTOR", "COORDINATOR", "APODERADO")
                
                // Administrative operations - Admin only
                .requestMatchers(HttpMethod.POST, "/applications/*/archive").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/applications/*/restore").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/applications/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/applications/statistics/**").hasAnyRole("ADMIN", "COORDINATOR")
                .requestMatchers(HttpMethod.GET, "/applications/reports/**").hasAnyRole("ADMIN", "CYCLE_DIRECTOR", "COORDINATOR")
                
                // Export and bulk operations - Admin and Coordinator
                .requestMatchers(HttpMethod.GET, "/applications/export/**").hasAnyRole("ADMIN", "COORDINATOR")
                .requestMatchers(HttpMethod.POST, "/applications/bulk/**").hasRole("ADMIN")
                
                // Evaluation triggers - Admin and Cycle Directors
                .requestMatchers(HttpMethod.POST, "/applications/*/evaluations/trigger").hasAnyRole("ADMIN", "CYCLE_DIRECTOR")
                .requestMatchers(HttpMethod.GET, "/applications/*/evaluations/**").hasAnyRole("ADMIN", "CYCLE_DIRECTOR", "COORDINATOR", "TEACHER", "PSYCHOLOGIST")
                
                // Notification triggers - Internal service calls and Admin
                .requestMatchers(HttpMethod.POST, "/applications/*/notifications/**").hasAnyRole("ADMIN", "SYSTEM")
                
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
                Object clientRoles = resourceMap.get("application-service");
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
            auditLogger.info("JWT authentication successful in Application Service - Subject: {}, Email: {}, Roles: {}", 
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
            
            auditLogger.warn("Authentication failed in Application Service - Path: {} {}, IP: {}, Reason: {}", 
                method, path, maskIpAddress(clientIp), authException.getMessage());
            
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.setHeader("WWW-Authenticate", "Bearer realm=\"MTN Application Service\"");
            
            String errorResponse = """
                {
                    "error": "unauthorized",
                    "message": "Authentication required for application service",
                    "timestamp": "%s",
                    "path": "%s",
                    "service": "application-service"
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
            
            auditLogger.warn("Access denied in Application Service - User: {}, Path: {} {}, IP: {}, Reason: {}", 
                userInfo, method, path, maskIpAddress(clientIp), accessDeniedException.getMessage());
            
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            
            String errorResponse = """
                {
                    "error": "access_denied", 
                    "message": "Insufficient permissions for application service",
                    "timestamp": "%s",
                    "path": "%s",
                    "service": "application-service",
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