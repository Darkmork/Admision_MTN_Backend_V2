package com.desafios.mtn.userservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.web.bind.annotation.CrossOrigin;

/**
 * User Service Microservice Application
 * Configuración simplificada para integración con frontend
 */
@SpringBootApplication
@EnableEurekaClient
@CrossOrigin(origins = {
    "http://localhost:5173", 
    "http://localhost:5174", 
    "http://localhost:5175", 
    "http://localhost:5176", 
    "http://localhost:5177"
})
public class UserServiceApplication {

    public static void main(String[] args) {
        System.out.println("🚀 Starting User Service Microservice...");
        System.out.println("🔗 Registering with Eureka Server...");
        System.out.println("📊 Database: users_db (separate from monolith)");
        System.out.println("🌐 CORS enabled for frontend");
        
        SpringApplication.run(UserServiceApplication.class, args);
        
        System.out.println("✅ User Service Microservice started successfully!");
        System.out.println("📍 Available at: http://localhost:8082/api/users");
        System.out.println("🏥 Health check: http://localhost:8082/actuator/health");
    }
}