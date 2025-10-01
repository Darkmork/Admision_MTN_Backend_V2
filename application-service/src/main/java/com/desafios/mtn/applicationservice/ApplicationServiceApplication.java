package com.desafios.mtn.applicationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.web.bind.annotation.CrossOrigin;

/**
 * Application Service Microservice
 * Handles application lifecycle and document management
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
public class ApplicationServiceApplication {

    public static void main(String[] args) {
        System.out.println("🚀 Starting Application Service Microservice...");
        System.out.println("📋 Handles applications and documents");
        System.out.println("🌐 CORS enabled for frontend");
        System.out.println("🔗 Registering with Eureka Server...");
        
        SpringApplication.run(ApplicationServiceApplication.class, args);
        
        System.out.println("✅ Application Service Microservice started successfully!");
        System.out.println("📍 Available at: http://localhost:8083/api/applications");
        System.out.println("🏥 Health check: http://localhost:8083/actuator/health");
    }
}