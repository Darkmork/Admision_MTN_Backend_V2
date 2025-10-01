package com.desafios.mtn.evaluationservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Configuración de seguridad para el microservicio de evaluaciones
 * Implementa autenticación OAuth2/OIDC con Keycloak
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Configuración CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // Deshabilitar CSRF para APIs REST
            .csrf(csrf -> csrf.disable())
            
            // Configuración de sesiones - stateless para microservicios
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // Configurar autenticación OAuth2 Resource Server
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            )
            
            // Configurar autorización de endpoints
            .authorizeHttpRequests(authz -> authz
                // Endpoints públicos - actuator health
                .requestMatchers("/actuator/health/**").permitAll()
                .requestMatchers("/actuator/info").permitAll()
                
                // Documentación de API - público en desarrollo
                .requestMatchers("/v3/api-docs/**").permitAll()
                .requestMatchers("/swagger-ui/**").permitAll()
                .requestMatchers("/swagger-ui.html").permitAll()
                
                // Endpoints de gestión - solo admin
                .requestMatchers("/api/management/**").hasRole("ADMIN")
                .requestMatchers("/actuator/**").hasRole("ADMIN")
                
                // APIs de evaluación - según roles específicos
                .requestMatchers("/api/evaluations/my-evaluations").hasAnyRole("TEACHER", "PSYCHOLOGIST")
                .requestMatchers("/api/evaluations/*/assign").hasAnyRole("ADMIN", "COORDINATOR")
                .requestMatchers("/api/evaluations/*/reassign").hasAnyRole("ADMIN", "COORDINATOR")
                .requestMatchers("/api/evaluations/auto-assign").hasAnyRole("ADMIN", "COORDINATOR")
                .requestMatchers("/api/evaluations/*/start").hasAnyRole("TEACHER", "PSYCHOLOGIST")
                .requestMatchers("/api/evaluations/*/complete").hasAnyRole("TEACHER", "PSYCHOLOGIST")
                
                // APIs de entrevista - según roles específicos
                .requestMatchers("/api/interviews/my-interviews").hasAnyRole("CYCLE_DIRECTOR", "PSYCHOLOGIST", "TEACHER")
                .requestMatchers("/api/interviews/my-upcoming").hasAnyRole("CYCLE_DIRECTOR", "PSYCHOLOGIST", "TEACHER")
                .requestMatchers("/api/interviews/*/start").hasAnyRole("CYCLE_DIRECTOR", "PSYCHOLOGIST")
                .requestMatchers("/api/interviews/*/complete").hasAnyRole("CYCLE_DIRECTOR", "PSYCHOLOGIST")
                .requestMatchers("/api/interviews").hasAnyRole("ADMIN", "COORDINATOR", "CYCLE_DIRECTOR")
                
                // Todo el resto requiere autenticación
                .anyRequest().authenticated()
            );

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter());
        return converter;
    }

    @Bean
    public Converter<Jwt, Collection<GrantedAuthority>> jwtGrantedAuthoritiesConverter() {
        return new JwtGrantedAuthoritiesConverter();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Orígenes permitidos
        configuration.setAllowedOrigins(List.of(
            "http://localhost:3000",
            "http://localhost:5173",
            "http://localhost:5174", 
            "http://localhost:5175",
            "http://localhost:5176",
            "http://localhost:8080",
            "http://localhost:8081", // API Gateway
            "http://localhost:8082", // Otros servicios
            "http://localhost:8083"
        ));
        
        // Métodos HTTP permitidos
        configuration.setAllowedMethods(List.of(
            "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));
        
        // Headers permitidos
        configuration.setAllowedHeaders(List.of("*"));
        
        // Permitir credenciales
        configuration.setAllowCredentials(true);
        
        // Headers expuestos
        configuration.setExposedHeaders(List.of(
            "Authorization", "Content-Type", "X-Requested-With", "Accept",
            "X-Total-Count", "X-Page-Number", "X-Page-Size"
        ));
        
        // Tiempo de cache para preflight requests
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }

    /**
     * Convertidor personalizado para extraer roles de Keycloak JWT
     */
    private static class JwtGrantedAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

        @Override
        public Collection<GrantedAuthority> convert(Jwt jwt) {
            // Extraer roles del realm
            Collection<GrantedAuthority> realmRoles = extractRealmRoles(jwt);
            
            // Extraer roles de resource/client
            Collection<GrantedAuthority> resourceRoles = extractResourceRoles(jwt);
            
            // Combinar ambos
            return realmRoles.stream()
                .collect(Collectors.toList())
                .stream()
                .collect(Collectors.toCollection(() -> 
                    resourceRoles.stream().collect(Collectors.toList())
                ));
        }

        @SuppressWarnings("unchecked")
        private Collection<GrantedAuthority> extractRealmRoles(Jwt jwt) {
            Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
            if (realmAccess == null) {
                return Collections.emptyList();
            }

            Collection<String> roles = (Collection<String>) realmAccess.get("roles");
            if (roles == null) {
                return Collections.emptyList();
            }

            return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                .collect(Collectors.toList());
        }

        @SuppressWarnings("unchecked")
        private Collection<GrantedAuthority> extractResourceRoles(Jwt jwt) {
            Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
            if (resourceAccess == null) {
                return Collections.emptyList();
            }

            // Buscar roles específicos del client/resource 'evaluation-service'
            Map<String, Object> resource = (Map<String, Object>) resourceAccess.get("evaluation-service");
            if (resource == null) {
                return Collections.emptyList();
            }

            Collection<String> roles = (Collection<String>) resource.get("roles");
            if (roles == null) {
                return Collections.emptyList();
            }

            return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                .collect(Collectors.toList());
        }
    }
}