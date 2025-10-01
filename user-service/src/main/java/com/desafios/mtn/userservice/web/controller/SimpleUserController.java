package com.desafios.mtn.userservice.web.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador simplificado para compatibilidad con frontend actual
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {
    "http://localhost:5173", 
    "http://localhost:5174", 
    "http://localhost:5175", 
    "http://localhost:5176", 
    "http://localhost:5177"
})
public class SimpleUserController {

    /**
     * Health check para verificar que el microservicio funciona
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        log.info("User Service health check called from frontend");
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "user-service",
            "message", "User Service is running as microservice"
        ));
    }

    /**
     * Endpoint para obtener información del servicio
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> info() {
        log.info("User Service info called from frontend");
        return ResponseEntity.ok(Map.of(
            "service", "user-service",
            "version", "1.0.0",
            "architecture", "microservices",
            "description", "User management microservice",
            "endpoints", List.of(
                "/api/users/health",
                "/api/users/info",
                "/api/users/demo-users"
            )
        ));
    }

    /**
     * Endpoint demo que simula usuarios del microservicio
     */
    @GetMapping("/demo-users")
    public ResponseEntity<List<Map<String, Object>>> getDemoUsers() {
        log.info("Demo users requested from frontend");
        
        List<Map<String, Object>> users = List.of(
            Map.of(
                "id", 1,
                "name", "Admin Microservicio",
                "email", "admin@microservice.mtn.cl",
                "role", "ADMIN",
                "service", "user-service"
            ),
            Map.of(
                "id", 2,
                "name", "Profesor Microservicio",
                "email", "profesor@microservice.mtn.cl",
                "role", "TEACHER",
                "service", "user-service"
            ),
            Map.of(
                "id", 3,
                "name", "Coordinador Microservicio",
                "email", "coordinador@microservice.mtn.cl",
                "role", "COORDINATOR",
                "service", "user-service"
            )
        );
        
        return ResponseEntity.ok(users);
    }

    /**
     * Endpoint para probar conectividad desde frontend
     */
    @PostMapping("/test-connection")
    public ResponseEntity<Map<String, Object>> testConnection(@RequestBody(required = false) Map<String, Object> data) {
        log.info("Test connection called from frontend with data: {}", data);
        
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "Connection to User Service microservice successful!",
            "receivedData", data != null ? data : Map.of(),
            "timestamp", java.time.Instant.now().toString(),
            "service", "user-service (microservice)"
        ));
    }

    /**
     * Endpoint para obtener estadísticas del servicio
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        log.info("User service stats requested");
        
        return ResponseEntity.ok(Map.of(
            "totalUsers", 3, // Demo data
            "activeUsers", 3,
            "roles", List.of("ADMIN", "TEACHER", "COORDINATOR"),
            "serviceUptime", "Running since startup",
            "architecture", "microservices",
            "database", "users_db (separate from monolith)"
        ));
    }
}