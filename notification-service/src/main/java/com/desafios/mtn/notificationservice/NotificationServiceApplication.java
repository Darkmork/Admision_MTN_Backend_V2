package com.desafios.mtn.notificationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.bind.annotation.CrossOrigin;

/**
 * Aplicación principal del Notification Service
 * 
 * Microservicio de notificaciones para el Sistema de Admisión MTN
 * 
 * Características:
 * - Procesamiento de eventos EmailRequested.v1 y SmsRequested.v1
 * - Sistema de plantillas con Mustache/Handlebars
 * - Rate limiting y control de spam
 * - Retry automático con backoff exponencial (5 niveles)
 * - Dead Letter Queue para mensajes fallidos
 * - Métricas y observabilidad con Prometheus/OpenTelemetry
 * - Seguridad OAuth2 Resource Server
 * - Soporte para modo mock (desarrollo) y real (producción)
 * - Idempotencia con ventana de 5 minutos
 */
@SpringBootApplication
@EnableEurekaClient
@EnableConfigurationProperties
@EnableTransactionManagement
@EnableScheduling
@CrossOrigin(origins = {
    "http://localhost:5173", 
    "http://localhost:5174", 
    "http://localhost:5175", 
    "http://localhost:5176", 
    "http://localhost:5177"
})
public class NotificationServiceApplication {

    public static void main(String[] args) {
        System.out.println("🚀 Starting Notification Service Microservice...");
        System.out.println("📧 Handles email and SMS notifications");
        System.out.println("🔗 Registering with Eureka Server...");
        System.out.println("🌐 CORS enabled for frontend");
        
        SpringApplication.run(NotificationServiceApplication.class, args);
        
        System.out.println("✅ Notification Service Microservice started successfully!");
        System.out.println("📍 Available at: http://localhost:8085/api/notifications");
        System.out.println("🏥 Health check: http://localhost:8085/actuator/health");
    }
}