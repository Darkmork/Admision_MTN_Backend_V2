// user-service/src/main/java/com/desafios/mtn/userservice/config/RabbitMQConfig.java

package com.desafios.mtn.userservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de RabbitMQ para eventos de dominio
 */
@Slf4j
@Configuration
@EnableRabbit
@ConditionalOnProperty(name = "user-service.messaging.enabled", havingValue = "true", matchIfMissing = true)
public class RabbitMQConfig {

    public static final String USER_EVENTS_EXCHANGE = "user-events";
    public static final String USER_EVENTS_DLX = "user-events-dlx";
    
    // Colas principales
    public static final String USER_CREATED_QUEUE = "user.created";
    public static final String USER_UPDATED_QUEUE = "user.updated";
    public static final String USER_DELETED_QUEUE = "user.deleted";
    public static final String USER_ROLES_CHANGED_QUEUE = "user.roles.changed";
    public static final String USER_STATUS_CHANGED_QUEUE = "user.status.changed";
    public static final String USER_LOGGED_IN_QUEUE = "user.logged.in";
    public static final String USER_PASSWORD_CHANGED_QUEUE = "user.password.changed";
    public static final String USER_EMAIL_VERIFIED_QUEUE = "user.email.verified";
    
    // Colas de dead letter
    public static final String USER_CREATED_DLQ = "user.created.dlq";
    public static final String USER_UPDATED_DLQ = "user.updated.dlq";
    public static final String USER_DELETED_DLQ = "user.deleted.dlq";
    public static final String USER_ROLES_CHANGED_DLQ = "user.roles.changed.dlq";
    public static final String USER_STATUS_CHANGED_DLQ = "user.status.changed.dlq";
    public static final String USER_LOGGED_IN_DLQ = "user.logged.in.dlq";
    public static final String USER_PASSWORD_CHANGED_DLQ = "user.password.changed.dlq";
    public static final String USER_EMAIL_VERIFIED_DLQ = "user.email.verified.dlq";
    
    // Routing keys
    public static final String USER_CREATED_ROUTING_KEY = "user.user_created";
    public static final String USER_UPDATED_ROUTING_KEY = "user.user_updated";
    public static final String USER_DELETED_ROUTING_KEY = "user.user_deleted";
    public static final String USER_ROLES_CHANGED_ROUTING_KEY = "user.user_roles_changed";
    public static final String USER_STATUS_CHANGED_ROUTING_KEY = "user.user_status_changed";
    public static final String USER_LOGGED_IN_ROUTING_KEY = "user.user_logged_in";
    public static final String USER_PASSWORD_CHANGED_ROUTING_KEY = "user.user_password_changed";
    public static final String USER_EMAIL_VERIFIED_ROUTING_KEY = "user.user_email_verified";

    /**
     * Exchange principal para eventos de usuario
     */
    @Bean
    public TopicExchange userEventsExchange() {
        return ExchangeBuilder
                .topicExchange(USER_EVENTS_EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * Dead Letter Exchange
     */
    @Bean
    public TopicExchange deadLetterExchange() {
        return ExchangeBuilder
                .topicExchange(USER_EVENTS_DLX)
                .durable(true)
                .build();
    }

    // === COLAS PRINCIPALES ===

    @Bean
    public Queue userCreatedQueue() {
        return QueueBuilder
                .durable(USER_CREATED_QUEUE)
                .withArgument("x-dead-letter-exchange", USER_EVENTS_DLX)
                .withArgument("x-dead-letter-routing-key", USER_CREATED_DLQ)
                .withArgument("x-message-ttl", 3600000) // 1 hora
                .withArgument("x-max-retries", 3)
                .build();
    }

    @Bean
    public Queue userUpdatedQueue() {
        return QueueBuilder
                .durable(USER_UPDATED_QUEUE)
                .withArgument("x-dead-letter-exchange", USER_EVENTS_DLX)
                .withArgument("x-dead-letter-routing-key", USER_UPDATED_DLQ)
                .withArgument("x-message-ttl", 3600000)
                .withArgument("x-max-retries", 3)
                .build();
    }

    @Bean
    public Queue userDeletedQueue() {
        return QueueBuilder
                .durable(USER_DELETED_QUEUE)
                .withArgument("x-dead-letter-exchange", USER_EVENTS_DLX)
                .withArgument("x-dead-letter-routing-key", USER_DELETED_DLQ)
                .withArgument("x-message-ttl", 3600000)
                .withArgument("x-max-retries", 3)
                .build();
    }

    @Bean
    public Queue userRolesChangedQueue() {
        return QueueBuilder
                .durable(USER_ROLES_CHANGED_QUEUE)
                .withArgument("x-dead-letter-exchange", USER_EVENTS_DLX)
                .withArgument("x-dead-letter-routing-key", USER_ROLES_CHANGED_DLQ)
                .withArgument("x-message-ttl", 3600000)
                .withArgument("x-max-retries", 3)
                .build();
    }

    @Bean
    public Queue userStatusChangedQueue() {
        return QueueBuilder
                .durable(USER_STATUS_CHANGED_QUEUE)
                .withArgument("x-dead-letter-exchange", USER_EVENTS_DLX)
                .withArgument("x-dead-letter-routing-key", USER_STATUS_CHANGED_DLQ)
                .withArgument("x-message-ttl", 3600000)
                .withArgument("x-max-retries", 3)
                .build();
    }

    @Bean
    public Queue userLoggedInQueue() {
        return QueueBuilder
                .durable(USER_LOGGED_IN_QUEUE)
                .withArgument("x-dead-letter-exchange", USER_EVENTS_DLX)
                .withArgument("x-dead-letter-routing-key", USER_LOGGED_IN_DLQ)
                .withArgument("x-message-ttl", 1800000) // 30 minutos (eventos más volátiles)
                .withArgument("x-max-retries", 2)
                .build();
    }

    @Bean
    public Queue userPasswordChangedQueue() {
        return QueueBuilder
                .durable(USER_PASSWORD_CHANGED_QUEUE)
                .withArgument("x-dead-letter-exchange", USER_EVENTS_DLX)
                .withArgument("x-dead-letter-routing-key", USER_PASSWORD_CHANGED_DLQ)
                .withArgument("x-message-ttl", 3600000)
                .withArgument("x-max-retries", 3)
                .build();
    }

    @Bean
    public Queue userEmailVerifiedQueue() {
        return QueueBuilder
                .durable(USER_EMAIL_VERIFIED_QUEUE)
                .withArgument("x-dead-letter-exchange", USER_EVENTS_DLX)
                .withArgument("x-dead-letter-routing-key", USER_EMAIL_VERIFIED_DLQ)
                .withArgument("x-message-ttl", 3600000)
                .withArgument("x-max-retries", 3)
                .build();
    }

    // === COLAS DEAD LETTER ===

    @Bean
    public Queue userCreatedDLQ() {
        return QueueBuilder.durable(USER_CREATED_DLQ).build();
    }

    @Bean
    public Queue userUpdatedDLQ() {
        return QueueBuilder.durable(USER_UPDATED_DLQ).build();
    }

    @Bean
    public Queue userDeletedDLQ() {
        return QueueBuilder.durable(USER_DELETED_DLQ).build();
    }

    @Bean
    public Queue userRolesChangedDLQ() {
        return QueueBuilder.durable(USER_ROLES_CHANGED_DLQ).build();
    }

    @Bean
    public Queue userStatusChangedDLQ() {
        return QueueBuilder.durable(USER_STATUS_CHANGED_DLQ).build();
    }

    @Bean
    public Queue userLoggedInDLQ() {
        return QueueBuilder.durable(USER_LOGGED_IN_DLQ).build();
    }

    @Bean
    public Queue userPasswordChangedDLQ() {
        return QueueBuilder.durable(USER_PASSWORD_CHANGED_DLQ).build();
    }

    @Bean
    public Queue userEmailVerifiedDLQ() {
        return QueueBuilder.durable(USER_EMAIL_VERIFIED_DLQ).build();
    }

    // === BINDINGS PRINCIPALES ===

    @Bean
    public Binding userCreatedBinding() {
        return BindingBuilder
                .bind(userCreatedQueue())
                .to(userEventsExchange())
                .with(USER_CREATED_ROUTING_KEY);
    }

    @Bean
    public Binding userUpdatedBinding() {
        return BindingBuilder
                .bind(userUpdatedQueue())
                .to(userEventsExchange())
                .with(USER_UPDATED_ROUTING_KEY);
    }

    @Bean
    public Binding userDeletedBinding() {
        return BindingBuilder
                .bind(userDeletedQueue())
                .to(userEventsExchange())
                .with(USER_DELETED_ROUTING_KEY);
    }

    @Bean
    public Binding userRolesChangedBinding() {
        return BindingBuilder
                .bind(userRolesChangedQueue())
                .to(userEventsExchange())
                .with(USER_ROLES_CHANGED_ROUTING_KEY);
    }

    @Bean
    public Binding userStatusChangedBinding() {
        return BindingBuilder
                .bind(userStatusChangedQueue())
                .to(userEventsExchange())
                .with(USER_STATUS_CHANGED_ROUTING_KEY);
    }

    @Bean
    public Binding userLoggedInBinding() {
        return BindingBuilder
                .bind(userLoggedInQueue())
                .to(userEventsExchange())
                .with(USER_LOGGED_IN_ROUTING_KEY);
    }

    @Bean
    public Binding userPasswordChangedBinding() {
        return BindingBuilder
                .bind(userPasswordChangedQueue())
                .to(userEventsExchange())
                .with(USER_PASSWORD_CHANGED_ROUTING_KEY);
    }

    @Bean
    public Binding userEmailVerifiedBinding() {
        return BindingBuilder
                .bind(userEmailVerifiedQueue())
                .to(userEventsExchange())
                .with(USER_EMAIL_VERIFIED_ROUTING_KEY);
    }

    // === BINDINGS DEAD LETTER ===

    @Bean
    public Binding userCreatedDLQBinding() {
        return BindingBuilder
                .bind(userCreatedDLQ())
                .to(deadLetterExchange())
                .with(USER_CREATED_DLQ);
    }

    @Bean
    public Binding userUpdatedDLQBinding() {
        return BindingBuilder
                .bind(userUpdatedDLQ())
                .to(deadLetterExchange())
                .with(USER_UPDATED_DLQ);
    }

    @Bean
    public Binding userDeletedDLQBinding() {
        return BindingBuilder
                .bind(userDeletedDLQ())
                .to(deadLetterExchange())
                .with(USER_DELETED_DLQ);
    }

    @Bean
    public Binding userRolesChangedDLQBinding() {
        return BindingBuilder
                .bind(userRolesChangedDLQ())
                .to(deadLetterExchange())
                .with(USER_ROLES_CHANGED_DLQ);
    }

    @Bean
    public Binding userStatusChangedDLQBinding() {
        return BindingBuilder
                .bind(userStatusChangedDLQ())
                .to(deadLetterExchange())
                .with(USER_STATUS_CHANGED_DLQ);
    }

    @Bean
    public Binding userLoggedInDLQBinding() {
        return BindingBuilder
                .bind(userLoggedInDLQ())
                .to(deadLetterExchange())
                .with(USER_LOGGED_IN_DLQ);
    }

    @Bean
    public Binding userPasswordChangedDLQBinding() {
        return BindingBuilder
                .bind(userPasswordChangedDLQ())
                .to(deadLetterExchange())
                .with(USER_PASSWORD_CHANGED_DLQ);
    }

    @Bean
    public Binding userEmailVerifiedDLQBinding() {
        return BindingBuilder
                .bind(userEmailVerifiedDLQ())
                .to(deadLetterExchange())
                .with(USER_EMAIL_VERIFIED_DLQ);
    }

    // === CONFIGURACIÓN DE RABBIT TEMPLATE ===

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jackson2JsonMessageConverter(objectMapper));
        template.setDefaultReceiveQueue(USER_EVENTS_EXCHANGE);
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                log.debug("Mensaje confirmado por RabbitMQ: {}", correlationData);
            } else {
                log.error("Mensaje rechazado por RabbitMQ: {} - Causa: {}", correlationData, cause);
            }
        });
        template.setReturnsCallback(returnedMessage -> {
            log.warn("Mensaje devuelto por RabbitMQ: {} - Routing Key: {} - Exchange: {}", 
                    returnedMessage.getMessage(), 
                    returnedMessage.getRoutingKey(), 
                    returnedMessage.getExchange());
        });
        return template;
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitListenerContainerFactory<SimpleMessageListenerContainer> rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jackson2JsonMessageConverter(objectMapper));
        factory.setConcurrentConsumers(2);
        factory.setMaxConcurrentConsumers(5);
        factory.setDefaultRequeueRejected(false); // Enviar a DLQ en lugar de requeue infinito
        factory.setAcknowledgeMode(AcknowledgeMode.AUTO);
        return factory;
    }

    /**
     * Configuración adicional para monitoreo y debugging
     */
    @Bean
    @ConditionalOnProperty(name = "user-service.messaging.debug", havingValue = "true")
    public Queue debugQueue() {
        return QueueBuilder
                .durable("user-events.debug")
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "user-service.messaging.debug", havingValue = "true")
    public Binding debugBinding() {
        return BindingBuilder
                .bind(debugQueue())
                .to(userEventsExchange())
                .with("user.*"); // Captura todos los eventos de usuario para debugging
    }

    /**
     * Inicialización de la infraestructura de mensajería
     */
    @Bean
    public MessagingInfrastructureInitializer messagingInitializer() {
        return new MessagingInfrastructureInitializer();
    }

    /**
     * Clase para inicializar la infraestructura de mensajería
     */
    public static class MessagingInfrastructureInitializer {
        
        public MessagingInfrastructureInitializer() {
            log.info("Infraestructura de mensajería de usuario inicializada");
            log.info("Exchange principal: {}", USER_EVENTS_EXCHANGE);
            log.info("Dead Letter Exchange: {}", USER_EVENTS_DLX);
            log.info("Colas configuradas para todos los tipos de eventos de usuario");
        }
    }
}