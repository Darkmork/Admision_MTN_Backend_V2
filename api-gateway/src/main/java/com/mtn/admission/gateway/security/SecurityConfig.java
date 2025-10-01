package com.mtn.admission.gateway.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Security Configuration for MTN Admission API Gateway
 * 
 * Implements comprehensive security including:
 * - OAuth2/OIDC Resource Server configuration
 * - Role-based access control (RBAC) per endpoint
 * - CORS configuration for Chilean domains
 * - Security headers and CSP policies
 * - Audit logging for security events
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);
    private static final Logger auditLogger = LoggerFactory.getLogger("SECURITY_AUDIT");

    @Value("${security.cors.allowed-origins:https://admision.mtn.cl,https://admin.mtn.cl}")
    private String[] allowedOrigins;

    @Value("${security.cors.max-age:3600}")
    private long corsMaxAge;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
            // Disable CSRF for API Gateway (stateless JWT authentication)
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            
            // Disable sessions (stateless authentication with JWT)
            .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
            
            // Configure OAuth2 Resource Server with JWT
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtDecoder(jwtDecoder())
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
                .authenticationEntryPoint(customAuthenticationEntryPoint())
                .accessDeniedHandler(customAccessDeniedHandler())
            )
            
            // Configure authorization rules based on MTN business requirements
            .authorizeExchange(exchanges -> exchanges
                // Public endpoints - no authentication required
                .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .pathMatchers(HttpMethod.GET, "/actuator/health", "/actuator/info").permitAll()
                .pathMatchers(HttpMethod.GET, "/api/applications/public/**", "/api/info/**").permitAll()
                
                // Admin-only endpoints
                .pathMatchers("/actuator/**").hasRole("ADMIN")
                .pathMatchers(HttpMethod.POST, "/api/users/admin/**").hasRole("ADMIN") 
                .pathMatchers(HttpMethod.PUT, "/api/users/admin/**").hasRole("ADMIN")
                .pathMatchers(HttpMethod.DELETE, "/api/users/admin/**").hasRole("ADMIN")
                .pathMatchers("/api/applications/*/state-changes").hasAnyRole("ADMIN", "CYCLE_DIRECTOR")
                .pathMatchers("/api/applications/admin/**").hasAnyRole("ADMIN", "COORDINATOR")
                .pathMatchers("/api/notifications/admin/**").hasRole("ADMIN")
                
                // User management - Admin and Coordinator access
                .pathMatchers(HttpMethod.GET, "/api/users/**").hasAnyRole("ADMIN", "COORDINATOR")
                .pathMatchers(HttpMethod.POST, "/api/users").hasAnyRole("ADMIN", "COORDINATOR") 
                .pathMatchers(HttpMethod.PUT, "/api/users/me", "/api/users/profile/**").authenticated()
                
                // Application submission - Apoderado and Admin
                .pathMatchers(HttpMethod.POST, "/api/applications/submit").hasAnyRole("APODERADO", "ADMIN")
                .pathMatchers(HttpMethod.POST, "/api/applications/*/documents/**").hasAnyRole("APODERADO", "ADMIN")
                .pathMatchers(HttpMethod.PUT, "/api/applications/*/documents/**").hasAnyRole("APODERADO", "ADMIN")
                
                // Application reading - Multiple roles based on context
                .pathMatchers(HttpMethod.GET, "/api/applications/**").hasAnyRole("ADMIN", "CYCLE_DIRECTOR", "COORDINATOR", "APODERADO")
                
                // Interview management - Cycle Directors and Admin
                .pathMatchers("/api/interviews/**").hasAnyRole("ADMIN", "CYCLE_DIRECTOR")
                
                // Evaluation scoring - Teachers and Psychologists
                .pathMatchers(HttpMethod.POST, "/api/evaluations/*/scores").hasAnyRole("TEACHER", "PSYCHOLOGIST", "ADMIN")
                .pathMatchers(HttpMethod.PUT, "/api/evaluations/*/scores").hasAnyRole("TEACHER", "PSYCHOLOGIST", "ADMIN")
                .pathMatchers(HttpMethod.PATCH, "/api/evaluations/*/scores").hasAnyRole("TEACHER", "PSYCHOLOGIST", "ADMIN")
                
                // Evaluation reading - Multiple roles
                .pathMatchers(HttpMethod.GET, "/api/evaluations/**").hasAnyRole("ADMIN", "CYCLE_DIRECTOR", "COORDINATOR", "TEACHER", "PSYCHOLOGIST")
                
                // All other authenticated endpoints
                .anyExchange().authenticated()
            )
            
            // Configure CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // Add custom security headers
            .headers(headers -> headers
                .frameOptions(ServerHttpSecurity.HeaderSpec.FrameOptionsSpec::deny)
                .contentSecurityPolicy("default-src 'self'; " +
                    "script-src 'self' 'unsafe-inline'; " +
                    "style-src 'self' 'unsafe-inline'; " +
                    "img-src 'self' data: https:; " +
                    "font-src 'self'; " +
                    "connect-src 'self' https://auth.mtn.cl; " +
                    "frame-ancestors 'none';")
                .and()
                .httpStrictTransportSecurity(hstsSpec -> hstsSpec
                    .maxAgeInSeconds(31536000)
                    .includeSubdomains(true))
            )
            
            .build();
    }

    /**
     * Custom JWT decoder with enhanced validation and logging
     */
    @Bean
    public org.springframework.security.oauth2.jwt.ReactiveJwtDecoder jwtDecoder() {
        org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder jwtDecoder = 
            org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder
                .withJwkSetUri("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
                .jwsAlgorithms(algs -> algs.addAll(Arrays.asList(
                    org.springframework.security.oauth2.jose.jws.JwsAlgorithms.RS256,
                    org.springframework.security.oauth2.jose.jws.JwsAlgorithms.RS384,
                    org.springframework.security.oauth2.jose.jws.JwsAlgorithms.RS512
                )))
                .cache(java.time.Duration.ofMinutes(5))
                .build();

        // Set JWT validation rules
        jwtDecoder.setJwtValidator(jwtValidator());
        
        return jwtDecoder;
    }

    /**
     * JWT validator with custom validation rules
     */
    @Bean
    public org.springframework.security.oauth2.jwt.Validator<org.springframework.security.oauth2.jwt.Jwt> jwtValidator() {
        var validators = java.util.List.of(
            new org.springframework.security.oauth2.jwt.JwtTimestampValidator(java.time.Duration.ofSeconds(60)),
            new org.springframework.security.oauth2.jwt.JwtIssuerValidator("${spring.security.oauth2.resourceserver.jwt.issuer-uri}"),
            jwtAudienceValidator()
        );
        
        return new org.springframework.security.oauth2.jwt.DelegatingOAuth2TokenValidator<>(validators);
    }

    /**
     * Custom audience validator for JWT tokens
     */
    private org.springframework.security.oauth2.jwt.Validator<org.springframework.security.oauth2.jwt.Jwt> jwtAudienceValidator() {
        return jwt -> {
            List<String> audiences = jwt.getAudience();
            if (audiences == null || audiences.isEmpty()) {
                // Allow tokens without explicit audience for backward compatibility
                logger.debug("JWT token has no audience claim");
                return org.springframework.security.oauth2.jwt.Oauth2TokenValidatorResult.success();
            }
            
            // Validate that token includes expected audience
            boolean hasValidAudience = audiences.stream()
                .anyMatch(aud -> aud.equals("api-gateway") || 
                              aud.equals("mtn-admission") || 
                              aud.equals("account"));
                              
            if (hasValidAudience) {
                logger.debug("JWT token has valid audience: {}", audiences);
                return org.springframework.security.oauth2.jwt.Oauth2TokenValidatorResult.success();
            } else {
                logger.warn("JWT token has invalid audience: {}", audiences);
                return org.springframework.security.oauth2.jwt.Oauth2TokenValidatorResult.failure(
                    new org.springframework.security.oauth2.jwt.Oauth2Error("invalid_audience", 
                        "Token audience validation failed", null));
            }
        };
    }

    /**
     * JWT authentication converter to extract roles and create authorities
     */
    @Bean
    public org.springframework.security.authentication.converter.Converter<Jwt, Mono<org.springframework.security.core.Authentication>> 
        jwtAuthenticationConverter() {
        
        return jwt -> {
            // Extract user information for logging
            String subject = jwt.getSubject();
            String email = jwt.getClaimAsString("email");
            
            // Extract roles from JWT claims
            List<String> roles = extractRoles(jwt);
            
            // Convert roles to Spring Security authorities
            List<org.springframework.security.core.GrantedAuthority> authorities = roles.stream()
                .map(role -> new org.springframework.security.core.authority.SimpleGrantedAuthority(
                    role.startsWith("ROLE_") ? role : "ROLE_" + role))
                .collect(java.util.stream.Collectors.toList());

            // Log successful authentication (with PII masking)
            auditLogger.info("JWT authentication successful - Subject: {}, Email: {}, Roles: {}", 
                subject, maskEmail(email), roles);

            // Create authentication token
            JwtAuthenticationToken authToken = new JwtAuthenticationToken(jwt, authorities);
            
            return Mono.just(authToken);
        };
    }

    /**
     * Extract roles from JWT token (supports multiple claim formats)
     */
    private List<String> extractRoles(Jwt jwt) {
        // Try different role claim formats
        
        // Format 1: Direct "roles" claim
        Object rolesObj = jwt.getClaim("roles");
        if (rolesObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> rolesList = (List<String>) rolesObj;
            return rolesList;
        }
        
        // Format 2: Keycloak "realm_access.roles" format
        Object realmAccess = jwt.getClaim("realm_access");
        if (realmAccess instanceof java.util.Map) {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> realmMap = (java.util.Map<String, Object>) realmAccess;
            Object realmRoles = realmMap.get("roles");
            if (realmRoles instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> rolesList = (List<String>) realmRoles;
                return rolesList;
            }
        }
        
        // Format 3: Resource access format
        Object resourceAccess = jwt.getClaim("resource_access");
        if (resourceAccess instanceof java.util.Map) {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> resourceMap = (java.util.Map<String, Object>) resourceAccess;
            Object clientRoles = resourceMap.get("api-gateway");
            if (clientRoles instanceof java.util.Map) {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> clientMap = (java.util.Map<String, Object>) clientRoles;
                Object roles = clientMap.get("roles");
                if (roles instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<String> rolesList = (List<String>) roles;
                    return rolesList;
                }
            }
        }
        
        // Fallback: no roles found
        logger.debug("No roles found in JWT token for subject: {}", jwt.getSubject());
        return Collections.emptyList();
    }

    /**
     * CORS configuration for Chilean domains and monitoring tools
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Set allowed origins from configuration
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins));
        
        // Allow specific HTTP methods
        configuration.setAllowedMethods(Arrays.asList(
            HttpMethod.GET.name(),
            HttpMethod.POST.name(), 
            HttpMethod.PUT.name(),
            HttpMethod.PATCH.name(),
            HttpMethod.DELETE.name(),
            HttpMethod.OPTIONS.name()
        ));
        
        // Allow specific headers
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "X-Request-ID", 
            "X-Correlation-ID",
            "Accept",
            "Origin",
            "User-Agent"
        ));
        
        // Expose custom headers to frontend
        configuration.setExposedHeaders(Arrays.asList(
            "X-Request-ID",
            "X-Rate-Limit-Remaining",
            "X-Rate-Limit-Retry-After"
        ));
        
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(corsMaxAge);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        
        // Separate CORS config for actuator endpoints (monitoring)
        CorsConfiguration actuatorConfig = new CorsConfiguration();
        actuatorConfig.setAllowedOrigins(Arrays.asList("https://grafana.mtn.cl", "https://prometheus.mtn.cl"));
        actuatorConfig.setAllowedMethods(Arrays.asList(HttpMethod.GET.name()));
        actuatorConfig.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));
        actuatorConfig.setAllowCredentials(false);
        actuatorConfig.setMaxAge(86400); // 24 hours for monitoring endpoints
        
        source.registerCorsConfiguration("/actuator/**", actuatorConfig);
        
        return source;
    }

    /**
     * Custom authentication entry point for unauthorized requests
     */
    private org.springframework.security.web.server.ServerAuthenticationEntryPoint customAuthenticationEntryPoint() {
        return (exchange, ex) -> {
            // Log authentication failure
            String path = exchange.getRequest().getPath().value();
            String method = exchange.getRequest().getMethod().name();
            String clientIp = getClientIp(exchange);
            
            auditLogger.warn("Authentication failed - Path: {} {}, IP: {}, Reason: {}", 
                method, path, maskIpAddress(clientIp), ex.getMessage());
            
            // Set response status and headers
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            exchange.getResponse().getHeaders().add("Content-Type", "application/json");
            exchange.getResponse().getHeaders().add("WWW-Authenticate", 
                "Bearer realm=\"MTN Admission\", error=\"invalid_token\"");
            
            // Return structured error response
            String errorResponse = """
                {
                    "error": "unauthorized",
                    "message": "Authentication required",
                    "timestamp": "%s",
                    "path": "%s"
                }
                """.formatted(java.time.Instant.now().toString(), path);
                
            org.springframework.core.io.buffer.DataBuffer buffer = 
                exchange.getResponse().bufferFactory().wrap(errorResponse.getBytes());
            
            return exchange.getResponse().writeWith(Mono.just(buffer));
        };
    }

    /**
     * Custom access denied handler for insufficient permissions
     */
    private ServerAccessDeniedHandler customAccessDeniedHandler() {
        return (exchange, denied) -> {
            // Log access denied
            String path = exchange.getRequest().getPath().value();
            String method = exchange.getRequest().getMethod().name();
            String clientIp = getClientIp(exchange);
            
            // Try to get user info from authentication
            return exchange.getPrincipal()
                .cast(Authentication.class)
                .map(auth -> {
                    String userInfo = "anonymous";
                    if (auth instanceof JwtAuthenticationToken) {
                        JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) auth;
                        String email = jwtAuth.getToken().getClaimAsString("email");
                        userInfo = maskEmail(email);
                    }
                    return userInfo;
                })
                .defaultIfEmpty("anonymous")
                .flatMap(userInfo -> {
                    auditLogger.warn("Access denied - User: {}, Path: {} {}, IP: {}, Reason: {}", 
                        userInfo, method, path, maskIpAddress(clientIp), denied.getMessage());
                    
                    // Set response status and headers  
                    exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                    exchange.getResponse().getHeaders().add("Content-Type", "application/json");
                    
                    // Return structured error response
                    String errorResponse = """
                        {
                            "error": "access_denied",
                            "message": "Insufficient permissions",
                            "timestamp": "%s",
                            "path": "%s"
                        }
                        """.formatted(java.time.Instant.now().toString(), path);
                        
                    org.springframework.core.io.buffer.DataBuffer buffer = 
                        exchange.getResponse().bufferFactory().wrap(errorResponse.getBytes());
                    
                    return exchange.getResponse().writeWith(Mono.just(buffer));
                });
        };
    }

    /**
     * Extract client IP address from request
     */
    private String getClientIp(ServerWebExchange exchange) {
        String xForwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = exchange.getRequest().getHeaders().getFirst("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return exchange.getRequest().getRemoteAddress() != null ? 
            exchange.getRequest().getRemoteAddress().getAddress().getHostAddress() : "unknown";
    }

    /**
     * Mask email addresses for logging (Chilean PII protection)
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
        if (ip == null || ip.isEmpty() || "unknown".equals(ip)) {
            return ip;
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