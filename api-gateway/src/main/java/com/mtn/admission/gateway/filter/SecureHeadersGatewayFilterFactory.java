package com.mtn.admission.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;

/**
 * Gateway filter to add security headers to all responses
 * Implements Chilean banking-level security standards
 */
@Component
public class SecureHeadersGatewayFilterFactory extends AbstractGatewayFilterFactory<SecureHeadersGatewayFilterFactory.Config> {

    public SecureHeadersGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            return chain.filter(exchange).then(
                org.springframework.web.server.ServerWebExchange.class.cast(exchange)
                    .getResponse()
                    .beforeCommit(() -> {
                        ServerHttpResponse response = exchange.getResponse();
                        HttpHeaders headers = response.getHeaders();
                        
                        // Content Security Policy - Strict policy for Chilean compliance
                        headers.add("Content-Security-Policy", 
                            "default-src 'self'; " +
                            "script-src 'self' 'unsafe-inline'; " +
                            "style-src 'self' 'unsafe-inline'; " +
                            "img-src 'self' data: https:; " +
                            "font-src 'self'; " +
                            "connect-src 'self' https://auth.mtn.cl; " +
                            "frame-ancestors 'none'; " +
                            "base-uri 'self'; " +
                            "form-action 'self';"
                        );
                        
                        // Strict Transport Security - Force HTTPS
                        headers.add("Strict-Transport-Security", 
                            "max-age=31536000; includeSubDomains; preload");
                        
                        // X-Frame-Options - Prevent clickjacking
                        headers.add("X-Frame-Options", "DENY");
                        
                        // X-Content-Type-Options - Prevent MIME type sniffing
                        headers.add("X-Content-Type-Options", "nosniff");
                        
                        // X-XSS-Protection - Enable XSS filtering
                        headers.add("X-XSS-Protection", "1; mode=block");
                        
                        // Referrer Policy - Control referrer information
                        headers.add("Referrer-Policy", "strict-origin-when-cross-origin");
                        
                        // Permissions Policy - Disable unnecessary browser APIs
                        headers.add("Permissions-Policy", 
                            "geolocation=(), " +
                            "camera=(), " +
                            "microphone=(), " +
                            "usb=(), " +
                            "magnetometer=(), " +
                            "accelerometer=(), " +
                            "gyroscope=()");
                        
                        // Cache Control - Prevent sensitive data caching
                        if (exchange.getRequest().getPath().value().contains("/api/")) {
                            headers.add("Cache-Control", "no-store, no-cache, must-revalidate, proxy-revalidate");
                            headers.add("Pragma", "no-cache");
                            headers.add("Expires", "0");
                        }
                        
                        // Custom security headers for Chilean compliance
                        headers.add("X-Chilean-PII-Protected", "true");
                        headers.add("X-Service-Gateway", "mtn-api-gateway");
                        headers.add("X-Security-Level", "high");
                        
                        return org.reactivestreams.Publisher.class.cast(
                            reactor.core.publisher.Mono.empty()
                        );
                    })
            );
        };
    }

    public static class Config {
        // Configuration properties can be added here if needed
    }
}