package com.desafios.mtn.evaluationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.bind.annotation.CrossOrigin;

/**
 * Aplicación principal del microservicio de evaluaciones
 * 
 * Este microservicio maneja:
 * - Evaluaciones académicas y psicológicas
 * - Entrevistas de admisión
 * - Asignación automática de evaluadores
 * - Gestión de SLAs y métricas
 * - Eventos de dominio con patrón Outbox
 */
@SpringBootApplication
@EnableEurekaClient
@EnableJpaAuditing
@EnableScheduling
@EnableTransactionManagement
@CrossOrigin(origins = {
    "http://localhost:5173", 
    "http://localhost:5174", 
    "http://localhost:5175", 
    "http://localhost:5176", 
    "http://localhost:5177"
})
public class EvaluationServiceApplication {

    public static void main(String[] args) {
        System.out.println("🚀 Starting Evaluation Service Microservice...");
        System.out.println("📝 Handles evaluations and interviews");
        System.out.println("🔗 Registering with Eureka Server...");
        System.out.println("🌐 CORS enabled for frontend");
        
        SpringApplication.run(EvaluationServiceApplication.class, args);
        
        System.out.println("✅ Evaluation Service Microservice started successfully!");
        System.out.println("📍 Available at: http://localhost:8084/api/interviews");
        System.out.println("🏥 Health check: http://localhost:8084/actuator/health");
    }
}