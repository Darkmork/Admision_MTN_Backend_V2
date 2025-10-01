// user-service/src/main/java/com/desafios/mtn/userservice/config/SecurityConfig.java

package com.desafios.mtn.userservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // Security Headers
            .headers(headers -> headers
                .frameOptions().deny()
                .contentTypeOptions().and()
                .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                    .maxAgeInSeconds(31536000)
                    .includeSubdomains(true)
                )
                .referrerPolicy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
            )
            
            // OAuth2 Resource Server Configuration
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtDecoder(jwtDecoder())
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            )
            
            // Authorization Rules
            .authorizeHttpRequests(authz -> authz
                // Health and monitoring endpoints
                .requestMatchers(
                    "/actuator/health",
                    "/actuator/health/**",
                    "/actuator/info",
                    "/actuator/metrics",
                    "/actuator/prometheus"
                ).permitAll()
                
                // API Documentation
                .requestMatchers(
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/swagger-resources/**",
                    "/webjars/**"
                ).permitAll()
                
                // Public endpoints (if any)
                .requestMatchers(HttpMethod.GET, "/api/health").permitAll()
                
                // User profile endpoints - require authentication
                .requestMatchers(HttpMethod.GET, "/api/me/**").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/me/**").authenticated()
                
                // Admin only endpoints for user management
                .requestMatchers(HttpMethod.POST, "/api/users").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/users/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/users/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/users/statistics").hasRole("ADMIN")
                
                // Role management - admin only
                .requestMatchers(HttpMethod.POST, "/api/roles").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/roles/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/roles/**").hasRole("ADMIN")
                
                // Read operations - require appropriate role
                .requestMatchers(HttpMethod.GET, "/api/users/**").hasAnyRole("ADMIN", "COORDINATOR")
                .requestMatchers(HttpMethod.GET, "/api/roles/**").hasAnyRole("ADMIN", "COORDINATOR")
                
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            
            .build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        // Configure JWT decoder for Keycloak
        NimbusJwtDecoder jwtDecoder = JwtDecoders.fromIssuerLocation(
            "${spring.security.oauth2.resourceserver.jwt.issuer-uri}"
        );
        
        // Optional: Configure additional validation
        jwtDecoder.setJwtValidator(jwtValidator());
        
        return jwtDecoder;
    }

    @Bean
    public Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter());
        jwtConverter.setPrincipalClaimName("preferred_username");
        return jwtConverter;
    }

    @Bean
    public Converter<Jwt, Collection<GrantedAuthority>> jwtGrantedAuthoritiesConverter() {
        return jwt -> {
            // Extract roles from Keycloak JWT token
            Collection<GrantedAuthority> authorities = extractRealmRoles(jwt);
            authorities.addAll(extractResourceRoles(jwt));
            return authorities;
        };
    }

    @Bean
    public Oauth2JwtValidator jwtValidator() {
        List<Oauth2TokenValidator<Jwt>> validators = List.of(
            new JwtTimestampValidator(),
            new JwtIssuerValidator("${spring.security.oauth2.resourceserver.jwt.issuer-uri}"),
            new JwtAudienceValidator("user-service") // Expected audience
        );
        
        return new DelegatingOAuth2TokenValidator<>(validators);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Allow origins
        configuration.setAllowedOriginPatterns(List.of(
            "http://localhost:3000",
            "http://localhost:5173",
            "http://localhost:5174",
            "http://localhost:5175",
            "http://localhost:5176",
            "http://localhost:5177",
            "https://*.mtn.cl"
        ));
        
        // Allow methods
        configuration.setAllowedMethods(List.of(
            HttpMethod.GET.name(),
            HttpMethod.POST.name(),
            HttpMethod.PUT.name(),
            HttpMethod.DELETE.name(),
            HttpMethod.PATCH.name(),
            HttpMethod.OPTIONS.name()
        ));
        
        // Allow headers
        configuration.setAllowedHeaders(List.of(
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "Accept",
            "Origin",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers",
            "X-Correlation-ID",
            "X-Request-ID"
        ));
        
        // Expose headers
        configuration.setExposedHeaders(List.of(
            "X-Total-Count",
            "X-Page-Number",
            "X-Page-Size",
            "X-Correlation-ID",
            "X-Request-ID"
        ));
        
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        
        return source;
    }

    /**
     * Extract realm roles from Keycloak JWT
     */
    private Collection<GrantedAuthority> extractRealmRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        
        if (realmAccess == null) {
            return List.of();
        }
        
        @SuppressWarnings("unchecked")
        Collection<String> realmRoles = (Collection<String>) realmAccess.get("roles");
        
        if (realmRoles == null) {
            return List.of();
        }
        
        return realmRoles.stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
            .collect(Collectors.toList());
    }

    /**
     * Extract resource-specific roles from Keycloak JWT
     */
    private Collection<GrantedAuthority> extractResourceRoles(Jwt jwt) {
        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
        
        if (resourceAccess == null) {
            return List.of();
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> userService = (Map<String, Object>) resourceAccess.get("user-service");
        
        if (userService == null) {
            return List.of();
        }
        
        @SuppressWarnings("unchecked")
        Collection<String> roles = (Collection<String>) userService.get("roles");
        
        if (roles == null) {
            return List.of();
        }
        
        return roles.stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
            .collect(Collectors.toList());
    }

    /**
     * Custom JWT audience validator
     */
    public static class JwtAudienceValidator implements Oauth2TokenValidator<Jwt> {
        
        private final String expectedAudience;
        
        public JwtAudienceValidator(String expectedAudience) {
            this.expectedAudience = expectedAudience;
        }
        
        @Override
        public Oauth2TokenValidatorResult validate(Jwt jwt) {
            Collection<String> audiences = jwt.getAudience();
            
            if (audiences != null && audiences.contains(expectedAudience)) {
                return Oauth2TokenValidatorResult.success();
            }
            
            return Oauth2TokenValidatorResult.failure(
                "The required audience is missing: " + expectedAudience
            );
        }
    }
}