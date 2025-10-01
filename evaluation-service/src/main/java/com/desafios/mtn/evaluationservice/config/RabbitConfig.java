package com.desafios.mtn.evaluationservice.config;

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
 * Configuración de RabbitMQ para evaluation-service
 * Define exchanges, queues, bindings y configuración de retry/DLQ
 */
@Configuration
@Slf4j
@ConditionalOnProperty(name = "application.events.enabled", havingValue = "true", matchIfMissing = true)
public class RabbitConfig {

    // ================================
    // EXCHANGES
    // ================================

    /**
     * Exchange principal para eventos de evaluaciones
     */
    @Bean
    public TopicExchange evaluationsExchange() {
        return ExchangeBuilder
                .topicExchange("evaluations.events")
                .durable(true)
                .build();
    }

    /**
     * Exchange para eventos de entrevistas
     */
    @Bean
    public TopicExchange interviewsExchange() {
        return ExchangeBuilder
                .topicExchange("interviews.events")
                .durable(true)
                .build();
    }

    /**
     * Exchange para eventos de admisiones (consumo desde application-service)
     */
    @Bean
    public TopicExchange admissionsExchange() {
        return ExchangeBuilder
                .topicExchange("admission.events")
                .durable(true)
                .build();
    }

    /**
     * Exchange para notificaciones (producción hacia notification-service)
     */
    @Bean
    public TopicExchange notificationExchange() {
        return ExchangeBuilder
                .topicExchange("notification.events")
                .durable(true)
                .build();
    }

    /**
     * Dead Letter Exchange para eventos fallidos
     */
    @Bean
    public DirectExchange deadLetterExchange() {
        return ExchangeBuilder
                .directExchange("evaluations.dlx")
                .durable(true)
                .build();
    }

    // ================================
    // QUEUES PRINCIPALES - EVALUATIONS
    // ================================

    /**
     * Cola para eventos de evaluaciones asignadas
     */
    @Bean
    public Queue evaluationsAssignedQueue() {
        return QueueBuilder
                .durable("evaluations.assigned.q")
                .withArgument("x-dead-letter-exchange", "evaluations.dlx")
                .withArgument("x-dead-letter-routing-key", "evaluations.assigned.failed")
                .withArgument("x-message-ttl", 3600000) // 1 hora TTL
                .build();
    }

    /**
     * Cola para eventos de evaluaciones iniciadas
     */
    @Bean
    public Queue evaluationsStartedQueue() {
        return QueueBuilder
                .durable("evaluations.started.q")
                .withArgument("x-dead-letter-exchange", "evaluations.dlx")
                .withArgument("x-dead-letter-routing-key", "evaluations.started.failed")
                .withArgument("x-message-ttl", 3600000)
                .build();
    }

    /**
     * Cola para eventos de evaluaciones completadas
     */
    @Bean
    public Queue evaluationsCompletedQueue() {
        return QueueBuilder
                .durable("evaluations.completed.q")
                .withArgument("x-dead-letter-exchange", "evaluations.dlx")
                .withArgument("x-dead-letter-routing-key", "evaluations.completed.failed")
                .withArgument("x-message-ttl", 7200000) // 2 horas TTL (importante para saga)
                .build();
    }

    /**
     * Cola para eventos de todas las evaluaciones completadas por aplicación
     */
    @Bean
    public Queue allEvaluationsCompletedQueue() {
        return QueueBuilder
                .durable("evaluations.all_completed.q")
                .withArgument("x-dead-letter-exchange", "evaluations.dlx")
                .withArgument("x-dead-letter-routing-key", "evaluations.all_completed.failed")
                .withArgument("x-message-ttl", 7200000) // 2 horas TTL (crítico para saga)
                .build();
    }

    // ================================
    // QUEUES PRINCIPALES - INTERVIEWS
    // ================================

    /**
     * Cola para eventos de entrevistas programadas
     */
    @Bean
    public Queue interviewsScheduledQueue() {
        return QueueBuilder
                .durable("interviews.scheduled.q")
                .withArgument("x-dead-letter-exchange", "evaluations.dlx")
                .withArgument("x-dead-letter-routing-key", "interviews.scheduled.failed")
                .withArgument("x-message-ttl", 3600000)
                .build();
    }

    /**
     * Cola para eventos de entrevistas confirmadas
     */
    @Bean
    public Queue interviewsConfirmedQueue() {
        return QueueBuilder
                .durable("interviews.confirmed.q")
                .withArgument("x-dead-letter-exchange", "evaluations.dlx")
                .withArgument("x-dead-letter-routing-key", "interviews.confirmed.failed")
                .withArgument("x-message-ttl", 3600000)
                .build();
    }

    /**
     * Cola para eventos de entrevistas completadas
     */
    @Bean
    public Queue interviewsCompletedQueue() {
        return QueueBuilder
                .durable("interviews.completed.q")
                .withArgument("x-dead-letter-exchange", "evaluations.dlx")
                .withArgument("x-dead-letter-routing-key", "interviews.completed.failed")
                .withArgument("x-message-ttl", 3600000)
                .build();
    }

    /**
     * Cola para eventos de entrevistas no show
     */
    @Bean
    public Queue interviewsNoShowQueue() {
        return QueueBuilder
                .durable("interviews.noshow.q")
                .withArgument("x-dead-letter-exchange", "evaluations.dlx")
                .withArgument("x-dead-letter-routing-key", "interviews.noshow.failed")
                .withArgument("x-message-ttl", 3600000)
                .build();
    }

    /**
     * Cola para eventos de entrevistas reprogramadas
     */
    @Bean
    public Queue interviewsRescheduledQueue() {
        return QueueBuilder
                .durable("interviews.rescheduled.q")
                .withArgument("x-dead-letter-exchange", "evaluations.dlx")
                .withArgument("x-dead-letter-routing-key", "interviews.rescheduled.failed")
                .withArgument("x-message-ttl", 3600000)
                .build();
    }

    // ================================
    // QUEUE CONSUMIDORA - APPLICATION SERVICE
    // ================================

    /**
     * Cola para consumir eventos de cambio de estado desde application-service
     */
    @Bean
    public Queue applicationStateChangedQueue() {
        return QueueBuilder
                .durable("app.state_changed.q")
                .withArgument("x-dead-letter-exchange", "evaluations.dlx")
                .withArgument("x-dead-letter-routing-key", "app.state_changed.failed")
                .withArgument("x-message-ttl", 1800000) // 30 minutos TTL
                .build();
    }

    // ================================
    // QUEUES DE NOTIFICACIÓN
    // ================================

    /**
     * Cola para enviar solicitudes de email
     */
    @Bean
    public Queue emailRequestQueue() {
        return QueueBuilder
                .durable("email.request.q")
                .withArgument("x-dead-letter-exchange", "evaluations.dlx")
                .withArgument("x-dead-letter-routing-key", "email.request.failed")
                .withArgument("x-message-ttl", 3600000)
                .build();
    }

    /**
     * Cola para enviar solicitudes de SMS
     */
    @Bean
    public Queue smsRequestQueue() {
        return QueueBuilder
                .durable("sms.request.q")
                .withArgument("x-dead-letter-exchange", "evaluations.dlx")
                .withArgument("x-dead-letter-routing-key", "sms.request.failed")
                .withArgument("x-message-ttl", 1800000) // 30 minutos TTL
                .build();
    }

    // ================================
    // DEAD LETTER QUEUES
    // ================================

    /**
     * Cola para evaluaciones fallidas
     */
    @Bean
    public Queue evaluationsFailedQueue() {
        return QueueBuilder
                .durable("evaluations.failed.q")
                .build();
    }

    /**
     * Cola para entrevistas fallidas
     */
    @Bean
    public Queue interviewsFailedQueue() {
        return QueueBuilder
                .durable("interviews.failed.q")
                .build();
    }

    /**
     * Cola para cambios de estado de aplicación fallidos
     */
    @Bean
    public Queue applicationStateFailedQueue() {
        return QueueBuilder
                .durable("app.state_changed.failed.q")
                .build();
    }

    /**
     * Cola para notificaciones fallidas
     */
    @Bean
    public Queue notificationFailedQueue() {
        return QueueBuilder
                .durable("notification.failed.q")
                .build();
    }

    // ================================
    // QUEUES DE RETRY CON DELAY
    // ================================

    /**
     * Cola de retry con delay para evaluaciones críticas
     */
    @Bean
    public Queue evaluationsRetryQueue() {
        return QueueBuilder
                .durable("evaluations.retry.q")
                .withArgument("x-message-ttl", 300000) // 5 minutos delay
                .withArgument("x-dead-letter-exchange", "evaluations.events")
                .withArgument("x-dead-letter-routing-key", "evaluation.retry")
                .build();
    }

    /**
     * Cola de retry con delay para saga de evaluaciones completadas
     */
    @Bean
    public Queue sagaRetryQueue() {
        return QueueBuilder
                .durable("saga.retry.q")
                .withArgument("x-message-ttl", 600000) // 10 minutos delay
                .withArgument("x-dead-letter-exchange", "evaluations.events")
                .withArgument("x-dead-letter-routing-key", "saga.retry")
                .build();
    }

    // ================================
    // BINDINGS - EVALUATIONS
    // ================================

    /**
     * Binding para evaluaciones asignadas
     */
    @Bean
    public Binding evaluationsAssignedBinding() {
        return BindingBuilder
                .bind(evaluationsAssignedQueue())
                .to(evaluationsExchange())
                .with("evaluation.assigned");
    }

    /**
     * Binding para evaluaciones iniciadas
     */
    @Bean
    public Binding evaluationsStartedBinding() {
        return BindingBuilder
                .bind(evaluationsStartedQueue())
                .to(evaluationsExchange())
                .with("evaluation.started");
    }

    /**
     * Binding para evaluaciones completadas
     */
    @Bean
    public Binding evaluationsCompletedBinding() {
        return BindingBuilder
                .bind(evaluationsCompletedQueue())
                .to(evaluationsExchange())
                .with("evaluation.completed");
    }

    /**
     * Binding para todas las evaluaciones completadas
     */
    @Bean
    public Binding allEvaluationsCompletedBinding() {
        return BindingBuilder
                .bind(allEvaluationsCompletedQueue())
                .to(evaluationsExchange())
                .with("evaluations.all_completed");
    }

    // ================================
    // BINDINGS - INTERVIEWS
    // ================================

    /**
     * Binding para entrevistas programadas
     */
    @Bean
    public Binding interviewsScheduledBinding() {
        return BindingBuilder
                .bind(interviewsScheduledQueue())
                .to(interviewsExchange())
                .with("interview.scheduled");
    }

    /**
     * Binding para entrevistas confirmadas
     */
    @Bean
    public Binding interviewsConfirmedBinding() {
        return BindingBuilder
                .bind(interviewsConfirmedQueue())
                .to(interviewsExchange())
                .with("interview.confirmed");
    }

    /**
     * Binding para entrevistas completadas
     */
    @Bean
    public Binding interviewsCompletedBinding() {
        return BindingBuilder
                .bind(interviewsCompletedQueue())
                .to(interviewsExchange())
                .with("interview.completed");
    }

    /**
     * Binding para entrevistas no show
     */
    @Bean
    public Binding interviewsNoShowBinding() {
        return BindingBuilder
                .bind(interviewsNoShowQueue())
                .to(interviewsExchange())
                .with("interview.noshow");
    }

    /**
     * Binding para entrevistas reprogramadas
     */
    @Bean
    public Binding interviewsRescheduledBinding() {
        return BindingBuilder
                .bind(interviewsRescheduledQueue())
                .to(interviewsExchange())
                .with("interview.rescheduled");
    }

    // ================================
    // BINDINGS - APPLICATION SERVICE CONSUMER
    // ================================

    /**
     * Binding para consumir cambios de estado desde application-service
     */
    @Bean
    public Binding applicationStateChangedBinding() {
        return BindingBuilder
                .bind(applicationStateChangedQueue())
                .to(admissionsExchange())
                .with("application.state.changed");
    }

    // ================================
    // BINDINGS - NOTIFICATIONS
    // ================================

    /**
     * Binding para solicitudes de email
     */
    @Bean
    public Binding emailRequestBinding() {
        return BindingBuilder
                .bind(emailRequestQueue())
                .to(notificationExchange())
                .with("email.request");
    }

    /**
     * Binding para solicitudes de SMS
     */
    @Bean
    public Binding smsRequestBinding() {
        return BindingBuilder
                .bind(smsRequestQueue())
                .to(notificationExchange())
                .with("sms.request");
    }

    // ================================
    // DEAD LETTER BINDINGS
    // ================================

    /**
     * Bindings para colas de dead letter
     */
    @Bean
    public Binding evaluationsFailedBinding() {
        return BindingBuilder
                .bind(evaluationsFailedQueue())
                .to(deadLetterExchange())
                .with("evaluations.assigned.failed");
    }

    @Bean
    public Binding evaluationsStartedFailedBinding() {
        return BindingBuilder
                .bind(evaluationsFailedQueue())
                .to(deadLetterExchange())
                .with("evaluations.started.failed");
    }

    @Bean
    public Binding evaluationsCompletedFailedBinding() {
        return BindingBuilder
                .bind(evaluationsFailedQueue())
                .to(deadLetterExchange())
                .with("evaluations.completed.failed");
    }

    @Bean
    public Binding allEvaluationsCompletedFailedBinding() {
        return BindingBuilder
                .bind(evaluationsFailedQueue())
                .to(deadLetterExchange())
                .with("evaluations.all_completed.failed");
    }

    @Bean
    public Binding interviewsScheduledFailedBinding() {
        return BindingBuilder
                .bind(interviewsFailedQueue())
                .to(deadLetterExchange())
                .with("interviews.scheduled.failed");
    }

    @Bean
    public Binding interviewsConfirmedFailedBinding() {
        return BindingBuilder
                .bind(interviewsFailedQueue())
                .to(deadLetterExchange())
                .with("interviews.confirmed.failed");
    }

    @Bean
    public Binding interviewsCompletedFailedBinding() {
        return BindingBuilder
                .bind(interviewsFailedQueue())
                .to(deadLetterExchange())
                .with("interviews.completed.failed");
    }

    @Bean
    public Binding interviewsNoShowFailedBinding() {
        return BindingBuilder
                .bind(interviewsFailedQueue())
                .to(deadLetterExchange())
                .with("interviews.noshow.failed");
    }

    @Bean
    public Binding interviewsRescheduledFailedBinding() {
        return BindingBuilder
                .bind(interviewsFailedQueue())
                .to(deadLetterExchange())
                .with("interviews.rescheduled.failed");
    }

    @Bean
    public Binding applicationStateFailedBinding() {
        return BindingBuilder
                .bind(applicationStateFailedQueue())
                .to(deadLetterExchange())
                .with("app.state_changed.failed");
    }

    @Bean
    public Binding emailRequestFailedBinding() {
        return BindingBuilder
                .bind(notificationFailedQueue())
                .to(deadLetterExchange())
                .with("email.request.failed");
    }

    @Bean
    public Binding smsRequestFailedBinding() {
        return BindingBuilder
                .bind(notificationFailedQueue())
                .to(deadLetterExchange())
                .with("sms.request.failed");
    }

    // ================================
    // RABBIT TEMPLATE CONFIGURATION
    // ================================

    /**
     * RabbitTemplate con configuración de retry y confirmaciones
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
        
        // Configurar recuperador para casos de fallos permanentes
        factory.setRecoveryCallback(context -> {
            log.error("Message processing failed permanently: {}", context.getLastThrowable().getMessage());
            return null;
        });
        
        return factory;
    }

    // ================================
    // ADMIN CONFIGURATION
    // ================================

    /**
     * RabbitAdmin para declarar automáticamente topología
     */
    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        admin.setAutoStartup(true);
        return admin;
    }

    // ================================
    // QUEUES ESPECÍFICAS PARA DESARROLLO
    // ================================

    /**
     * Cola de prueba para desarrollo
     */
    @Bean
    @ConditionalOnProperty(name = "spring.profiles.active", havingValue = "dev")
    public Queue devTestQueue() {
        return QueueBuilder
                .durable("dev.test.evaluations.q")
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
                .to(evaluationsExchange())
                .with("dev.test");
    }

    // ================================
    // CONFIGURACIÓN DE MONITOREO
    // ================================

    /**
     * Bean para métricas de RabbitMQ
     */
    @Bean
    public RabbitTemplateConfigurer rabbitTemplateConfigurer() {
        return rabbitTemplate -> {
            // Configurar métricas personalizadas
            rabbitTemplate.setBeforePublishPostProcessors(message -> {
                // Agregar headers de trazabilidad
                message.getMessageProperties().setHeader("service", "evaluation-service");
                message.getMessageProperties().setHeader("timestamp", System.currentTimeMillis());
                return message;
            });
        };
    }

    @FunctionalInterface
    public interface RabbitTemplateConfigurer {
        void configure(RabbitTemplate rabbitTemplate);
    }
}