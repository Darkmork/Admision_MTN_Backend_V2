package com.desafios.mtn.applicationservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

/**
 * Configuración de RabbitMQ para el microservicio application-service
 */
@Configuration
@Slf4j
@ConditionalOnProperty(name = "application.events.enabled", havingValue = "true", matchIfMissing = true)
public class RabbitConfig {

    // ================================
    // EXCHANGES
    // ================================

    /**
     * Exchange principal para eventos de admisión
     */
    @Bean
    public TopicExchange admissionEventsExchange() {
        return ExchangeBuilder
                .topicExchange("admission.events")
                .durable(true)
                .build();
    }

    /**
     * Exchange para eventos de notificación
     */
    @Bean
    public TopicExchange notificationExchange() {
        return ExchangeBuilder
                .topicExchange("notification.events")
                .durable(true)
                .build();
    }

    /**
     * Exchange para eventos de documentos
     */
    @Bean
    public TopicExchange documentExchange() {
        return ExchangeBuilder
                .topicExchange("document.events")
                .durable(true)
                .build();
    }

    /**
     * Dead Letter Exchange para eventos fallidos
     */
    @Bean
    public DirectExchange deadLetterExchange() {
        return ExchangeBuilder
                .directExchange("admission.dlx")
                .durable(true)
                .build();
    }

    // ================================
    // COLAS PRINCIPALES
    // ================================

    /**
     * Cola para eventos de aplicaciones enviadas
     */
    @Bean
    public Queue applicationSubmittedQueue() {
        return QueueBuilder
                .durable("application.submitted.queue")
                .withArgument("x-dead-letter-exchange", "admission.dlx")
                .withArgument("x-dead-letter-routing-key", "application.submitted.failed")
                .withArgument("x-message-ttl", 3600000) // 1 hora TTL
                .build();
    }

    /**
     * Cola para eventos de cambio de estado
     */
    @Bean
    public Queue stateChangedQueue() {
        return QueueBuilder
                .durable("application.state.changed.queue")
                .withArgument("x-dead-letter-exchange", "admission.dlx")
                .withArgument("x-dead-letter-routing-key", "state.changed.failed")
                .withArgument("x-message-ttl", 3600000)
                .build();
    }

    /**
     * Cola para eventos de documentos
     */
    @Bean
    public Queue documentUploadedQueue() {
        return QueueBuilder
                .durable("document.uploaded.queue")
                .withArgument("x-dead-letter-exchange", "admission.dlx")
                .withArgument("x-dead-letter-routing-key", "document.uploaded.failed")
                .withArgument("x-message-ttl", 1800000) // 30 minutos TTL
                .build();
    }

    /**
     * Cola para eventos de evaluación
     */
    @Bean
    public Queue evaluationCompletedQueue() {
        return QueueBuilder
                .durable("evaluation.completed.queue")
                .withArgument("x-dead-letter-exchange", "admission.dlx")
                .withArgument("x-dead-letter-routing-key", "evaluation.completed.failed")
                .withArgument("x-message-ttl", 3600000)
                .build();
    }

    /**
     * Cola para notificaciones programadas
     */
    @Bean
    public Queue notificationScheduledQueue() {
        return QueueBuilder
                .durable("notification.scheduled.queue")
                .withArgument("x-dead-letter-exchange", "admission.dlx")
                .withArgument("x-dead-letter-routing-key", "notification.scheduled.failed")
                .withArgument("x-message-ttl", 7200000) // 2 horas TTL
                .build();
    }

    // ================================
    // COLAS DE DEAD LETTER
    // ================================

    /**
     * Cola para eventos de aplicación fallidos
     */
    @Bean
    public Queue applicationFailedQueue() {
        return QueueBuilder
                .durable("application.failed.queue")
                .build();
    }

    /**
     * Cola para eventos de estado fallidos
     */
    @Bean
    public Queue stateChangeFailedQueue() {
        return QueueBuilder
                .durable("state.change.failed.queue")
                .build();
    }

    /**
     * Cola para eventos de documento fallidos
     */
    @Bean
    public Queue documentFailedQueue() {
        return QueueBuilder
                .durable("document.failed.queue")
                .build();
    }

    /**
     * Cola para eventos de evaluación fallidos
     */
    @Bean
    public Queue evaluationFailedQueue() {
        return QueueBuilder
                .durable("evaluation.failed.queue")
                .build();
    }

    /**
     * Cola para notificaciones fallidas
     */
    @Bean
    public Queue notificationFailedQueue() {
        return QueueBuilder
                .durable("notification.failed.queue")
                .build();
    }

    // ================================
    // BINDINGS - EXCHANGE TO QUEUE
    // ================================

    /**
     * Binding para aplicaciones enviadas
     */
    @Bean
    public Binding applicationSubmittedBinding() {
        return BindingBuilder
                .bind(applicationSubmittedQueue())
                .to(admissionEventsExchange())
                .with("application.submitted");
    }

    /**
     * Binding para cambios de estado
     */
    @Bean
    public Binding stateChangedBinding() {
        return BindingBuilder
                .bind(stateChangedQueue())
                .to(admissionEventsExchange())
                .with("application.state.*");
    }

    /**
     * Binding para documentos subidos
     */
    @Bean
    public Binding documentUploadedBinding() {
        return BindingBuilder
                .bind(documentUploadedQueue())
                .to(documentExchange())
                .with("document.uploaded");
    }

    /**
     * Binding para evaluaciones completadas
     */
    @Bean
    public Binding evaluationCompletedBinding() {
        return BindingBuilder
                .bind(evaluationCompletedQueue())
                .to(admissionEventsExchange())
                .with("evaluation.completed");
    }

    /**
     * Binding para notificaciones programadas
     */
    @Bean
    public Binding notificationScheduledBinding() {
        return BindingBuilder
                .bind(notificationScheduledQueue())
                .to(notificationExchange())
                .with("notification.scheduled");
    }

    // ================================
    // DEAD LETTER BINDINGS
    // ================================

    /**
     * Binding para aplicaciones fallidas
     */
    @Bean
    public Binding applicationFailedBinding() {
        return BindingBuilder
                .bind(applicationFailedQueue())
                .to(deadLetterExchange())
                .with("application.submitted.failed");
    }

    /**
     * Binding para estados fallidos
     */
    @Bean
    public Binding stateChangeFailedBinding() {
        return BindingBuilder
                .bind(stateChangeFailedQueue())
                .to(deadLetterExchange())
                .with("state.changed.failed");
    }

    /**
     * Binding para documentos fallidos
     */
    @Bean
    public Binding documentFailedBinding() {
        return BindingBuilder
                .bind(documentFailedQueue())
                .to(deadLetterExchange())
                .with("document.uploaded.failed");
    }

    /**
     * Binding para evaluaciones fallidas
     */
    @Bean
    public Binding evaluationFailedBinding() {
        return BindingBuilder
                .bind(evaluationFailedQueue())
                .to(deadLetterExchange())
                .with("evaluation.completed.failed");
    }

    /**
     * Binding para notificaciones fallidas
     */
    @Bean
    public Binding notificationFailedBinding() {
        return BindingBuilder
                .bind(notificationFailedQueue())
                .to(deadLetterExchange())
                .with("notification.scheduled.failed");
    }

    // ================================
    // RABBIT TEMPLATE CONFIGURATION
    // ================================

    /**
     * RabbitTemplate con configuración de retry y timeout
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        
        // Configurar conversión JSON
        template.setMessageConverter(jackson2JsonMessageConverter());
        
        // Configurar retry policy
        template.setRetryTemplate(retryTemplate());
        
        // Configurar timeouts
        template.setReceiveTimeout(5000); // 5 segundos
        template.setReplyTimeout(10000); // 10 segundos
        
        // Configurar confirmaciones de publicación
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                log.debug("Message published successfully: {}", correlationData != null ? correlationData.getId() : "unknown");
            } else {
                log.error("Failed to publish message: {} - Cause: {}", 
                         correlationData != null ? correlationData.getId() : "unknown", cause);
            }
        });
        
        // Configurar callback de retorno (para mensajes no rutables)
        template.setReturnsCallback(returnedMessage -> {
            log.error("Message returned: Exchange={}, RoutingKey={}, ReplyCode={}, ReplyText={}",
                     returnedMessage.getExchange(),
                     returnedMessage.getRoutingKey(),
                     returnedMessage.getReplyCode(),
                     returnedMessage.getReplyText());
        });
        
        return template;
    }

    /**
     * Convertidor JSON para mensajes
     */
    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * Template de retry para operaciones RabbitMQ
     */
    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        
        // Política de retry - 3 intentos
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(3);
        retryTemplate.setRetryPolicy(retryPolicy);
        
        // Política de backoff exponencial
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(1000); // 1 segundo inicial
        backOffPolicy.setMultiplier(2.0); // Multiplicador x2
        backOffPolicy.setMaxInterval(10000); // Máximo 10 segundos
        retryTemplate.setBackOffPolicy(backOffPolicy);
        
        return retryTemplate;
    }

    // ================================
    // LISTENER CONFIGURATION
    // ================================

    /**
     * Factory para listeners con configuración personalizada
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jackson2JsonMessageConverter());
        
        // Configuración de concurrencia
        factory.setConcurrentConsumers(2);
        factory.setMaxConcurrentConsumers(10);
        
        // Configuración de prefetch (número de mensajes no confirmados por consumidor)
        factory.setPrefetchCount(5);
        
        // Configuración de acknowledge
        factory.setAcknowledgeMode(AcknowledgeMode.AUTO);
        
        // Configuración de retry
        factory.setRetryTemplate(retryTemplate());
        
        return factory;
    }

    // ================================
    // UTILIDADES Y MONITOREO
    // ================================

    /**
     * Bean para declarar automáticamente todas las colas y exchanges al arrancar
     */
    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        admin.setAutoStartup(true);
        return admin;
    }

    /**
     * Configuración adicional para el entorno de desarrollo
     */
    @Bean
    @ConditionalOnProperty(name = "spring.profiles.active", havingValue = "dev")
    public Queue devTestQueue() {
        return QueueBuilder
                .durable("dev.test.queue")
                .withArgument("x-message-ttl", 300000) // 5 minutos TTL para desarrollo
                .build();
    }

    /**
     * Binding de prueba para desarrollo
     */
    @Bean
    @ConditionalOnProperty(name = "spring.profiles.active", havingValue = "dev")
    public Binding devTestBinding() {
        return BindingBuilder
                .bind(devTestQueue())
                .to(admissionEventsExchange())
                .with("dev.test");
    }
}