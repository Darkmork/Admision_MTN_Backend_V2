package com.desafios.mtn.evaluationservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Configuración de RestTemplate para comunicación con otros servicios
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        
        // Agregar interceptors para logging, tracing, etc.
        List<ClientHttpRequestInterceptor> interceptors = List.of(
            new LoggingInterceptor(),
            new TracingInterceptor()
        );
        
        restTemplate.setInterceptors(interceptors);
        
        return restTemplate;
    }

    /**
     * Interceptor para logging de requests HTTP
     */
    private static class LoggingInterceptor implements ClientHttpRequestInterceptor {
        @Override
        public org.springframework.http.client.ClientHttpResponse intercept(
                org.springframework.http.HttpRequest request,
                byte[] body,
                org.springframework.http.client.ClientHttpRequestExecution execution) throws java.io.IOException {
            
            // Log request
            System.out.println("HTTP " + request.getMethod() + " " + request.getURI());
            
            org.springframework.http.client.ClientHttpResponse response = execution.execute(request, body);
            
            // Log response
            System.out.println("HTTP Response: " + response.getStatusCode());
            
            return response;
        }
    }

    /**
     * Interceptor para propagación de trace context
     */
    private static class TracingInterceptor implements ClientHttpRequestInterceptor {
        @Override
        public org.springframework.http.client.ClientHttpResponse intercept(
                org.springframework.http.HttpRequest request,
                byte[] body,
                org.springframework.http.client.ClientHttpRequestExecution execution) throws java.io.IOException {
            
            // Agregar headers de tracing si están disponibles
            // En una implementación real usaríamos OpenTelemetry o similar
            
            return execution.execute(request, body);
        }
    }
}