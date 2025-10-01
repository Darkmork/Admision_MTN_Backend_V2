package com.desafios.mtn.notificationservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuración completa de la topología RabbitMQ para notification-service
 * 
 * Topología implementada:
 * - notifications.x (Topic Exchange) - Exchange principal
 * - Colas principales: notifications.email.q, notifications.sms.q
 * - Sistema de retry con 5 niveles de TTL progresivo
 * - Dead Letter Queue (DLQ) para mensajes fallidos definitivamente
 * - Retry automático con backoff exponencial
 */
@Configuration
@Slf4j
public class RabbitTopologyConfig {

    // Exchange principal
    public static final String NOTIFICATIONS_EXCHANGE = "notifications.x";
    
    // Routing keys
    public static final String EMAIL_ROUTING_KEY = "email.requested";
    public static final String SMS_ROUTING_KEY = "sms.requested";
    
    // Colas principales
    public static final String EMAIL_QUEUE = "notifications.email.q";
    public static final String SMS_QUEUE = "notifications.sms.q";
    
    // Sistema de retry - 5 niveles
    public static final String EMAIL_RETRY_QUEUE_1 = "notifications.email.retry.q.1"; // TTL: 30s
    public static final String EMAIL_RETRY_QUEUE_2 = "notifications.email.retry.q.2"; // TTL: 2m
    public static final String EMAIL_RETRY_QUEUE_3 = "notifications.email.retry.q.3"; // TTL: 10m
    public static final String EMAIL_RETRY_QUEUE_4 = "notifications.email.retry.q.4"; // TTL: 1h
    public static final String EMAIL_RETRY_QUEUE_5 = "notifications.email.retry.q.5"; // TTL: 4h
    
    public static final String SMS_RETRY_QUEUE_1 = "notifications.sms.retry.q.1"; // TTL: 30s
    public static final String SMS_RETRY_QUEUE_2 = "notifications.sms.retry.q.2"; // TTL: 2m
    public static final String SMS_RETRY_QUEUE_3 = "notifications.sms.retry.q.3"; // TTL: 10m
    public static final String SMS_RETRY_QUEUE_4 = "notifications.sms.retry.q.4"; // TTL: 1h
    public static final String SMS_RETRY_QUEUE_5 = "notifications.sms.retry.q.5"; // TTL: 4h
    
    // Dead Letter Queues
    public static final String EMAIL_DLQ = "notifications.email.dlq";
    public static final String SMS_DLQ = "notifications.sms.dlq";

    // TTL values en millisegundos
    private static final long TTL_30_SECONDS = 30_000L;
    private static final long TTL_2_MINUTES = 120_000L;
    private static final long TTL_10_MINUTES = 600_000L;
    private static final long TTL_1_HOUR = 3_600_000L;
    private static final long TTL_4_HOURS = 14_400_000L;

    @Value("${notification.retry.max-attempts:5}")
    private int maxRetryAttempts;

    // ======================
    // MESSAGE CONVERTER
    // ======================

    @Bean
    public MessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        converter.setCreateMessageIds(true);
        return converter;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        template.setMandatory(true);
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                log.error("Message failed to be delivered to exchange: {}", cause);
            }
        });
        template.setReturnsCallback(returnedMessage -> {
            log.error("Message returned: {} - {}", returnedMessage.getReplyText(), 
                     returnedMessage.getMessage());
        });
        return template;
    }

    // ======================
    // EXCHANGE PRINCIPAL
    // ======================

    @Bean
    public TopicExchange notificationsExchange() {
        return ExchangeBuilder
                .topicExchange(NOTIFICATIONS_EXCHANGE)
                .durable(true)
                .build();
    }

    // ======================
    // COLAS PRINCIPALES EMAIL
    // ======================

    @Bean
    public Queue emailQueue() {
        return QueueBuilder
                .durable(EMAIL_QUEUE)
                .deadLetterExchange(NOTIFICATIONS_EXCHANGE)
                .deadLetterRoutingKey("email.retry.1")
                .build();
    }

    @Bean
    public Binding emailQueueBinding() {
        return BindingBuilder
                .bind(emailQueue())
                .to(notificationsExchange())
                .with(EMAIL_ROUTING_KEY);
    }

    // ======================
    // SISTEMA DE RETRY EMAIL - 5 NIVELES
    // ======================

    @Bean
    public Queue emailRetryQueue1() {
        return QueueBuilder
                .durable(EMAIL_RETRY_QUEUE_1)
                .ttl(TTL_30_SECONDS)
                .deadLetterExchange(NOTIFICATIONS_EXCHANGE)
                .deadLetterRoutingKey("email.retry.2")
                .build();
    }

    @Bean
    public Binding emailRetryQueue1Binding() {
        return BindingBuilder
                .bind(emailRetryQueue1())
                .to(notificationsExchange())
                .with("email.retry.1");
    }

    @Bean
    public Queue emailRetryQueue2() {
        return QueueBuilder
                .durable(EMAIL_RETRY_QUEUE_2)
                .ttl(TTL_2_MINUTES)
                .deadLetterExchange(NOTIFICATIONS_EXCHANGE)
                .deadLetterRoutingKey("email.retry.3")
                .build();
    }

    @Bean
    public Binding emailRetryQueue2Binding() {
        return BindingBuilder
                .bind(emailRetryQueue2())
                .to(notificationsExchange())
                .with("email.retry.2");
    }

    @Bean
    public Queue emailRetryQueue3() {
        return QueueBuilder
                .durable(EMAIL_RETRY_QUEUE_3)
                .ttl(TTL_10_MINUTES)
                .deadLetterExchange(NOTIFICATIONS_EXCHANGE)
                .deadLetterRoutingKey("email.retry.4")
                .build();
    }

    @Bean
    public Binding emailRetryQueue3Binding() {
        return BindingBuilder
                .bind(emailRetryQueue3())
                .to(notificationsExchange())
                .with("email.retry.3");
    }

    @Bean
    public Queue emailRetryQueue4() {
        return QueueBuilder
                .durable(EMAIL_RETRY_QUEUE_4)
                .ttl(TTL_1_HOUR)
                .deadLetterExchange(NOTIFICATIONS_EXCHANGE)
                .deadLetterRoutingKey("email.retry.5")
                .build();
    }

    @Bean
    public Binding emailRetryQueue4Binding() {
        return BindingBuilder
                .bind(emailRetryQueue4())
                .to(notificationsExchange())
                .with("email.retry.4");
    }

    @Bean
    public Queue emailRetryQueue5() {
        return QueueBuilder
                .durable(EMAIL_RETRY_QUEUE_5)
                .ttl(TTL_4_HOURS)
                .deadLetterExchange(NOTIFICATIONS_EXCHANGE)
                .deadLetterRoutingKey("email.dlq")
                .build();
    }

    @Bean
    public Binding emailRetryQueue5Binding() {
        return BindingBuilder
                .bind(emailRetryQueue5())
                .to(notificationsExchange())
                .with("email.retry.5");
    }

    // ======================
    // DEAD LETTER QUEUE EMAIL
    // ======================

    @Bean
    public Queue emailDlq() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-message-ttl", 30 * 24 * 60 * 60 * 1000L); // 30 días
        return QueueBuilder
                .durable(EMAIL_DLQ)
                .withArguments(args)
                .build();
    }

    @Bean
    public Binding emailDlqBinding() {
        return BindingBuilder
                .bind(emailDlq())
                .to(notificationsExchange())
                .with("email.dlq");
    }

    // ======================
    // COLAS PRINCIPALES SMS
    // ======================

    @Bean
    public Queue smsQueue() {
        return QueueBuilder
                .durable(SMS_QUEUE)
                .deadLetterExchange(NOTIFICATIONS_EXCHANGE)
                .deadLetterRoutingKey("sms.retry.1")
                .build();
    }

    @Bean
    public Binding smsQueueBinding() {
        return BindingBuilder
                .bind(smsQueue())
                .to(notificationsExchange())
                .with(SMS_ROUTING_KEY);
    }

    // ======================
    // SISTEMA DE RETRY SMS - 5 NIVELES
    // ======================

    @Bean
    public Queue smsRetryQueue1() {
        return QueueBuilder
                .durable(SMS_RETRY_QUEUE_1)
                .ttl(TTL_30_SECONDS)
                .deadLetterExchange(NOTIFICATIONS_EXCHANGE)
                .deadLetterRoutingKey("sms.retry.2")
                .build();
    }

    @Bean
    public Binding smsRetryQueue1Binding() {
        return BindingBuilder
                .bind(smsRetryQueue1())
                .to(notificationsExchange())
                .with("sms.retry.1");
    }

    @Bean
    public Queue smsRetryQueue2() {
        return QueueBuilder
                .durable(SMS_RETRY_QUEUE_2)
                .ttl(TTL_2_MINUTES)
                .deadLetterExchange(NOTIFICATIONS_EXCHANGE)
                .deadLetterRoutingKey("sms.retry.3")
                .build();
    }

    @Bean
    public Binding smsRetryQueue2Binding() {
        return BindingBuilder
                .bind(smsRetryQueue2())
                .to(notificationsExchange())
                .with("sms.retry.2");
    }

    @Bean
    public Queue smsRetryQueue3() {
        return QueueBuilder
                .durable(SMS_RETRY_QUEUE_3)
                .ttl(TTL_10_MINUTES)
                .deadLetterExchange(NOTIFICATIONS_EXCHANGE)
                .deadLetterRoutingKey("sms.retry.4")
                .build();
    }

    @Bean
    public Binding smsRetryQueue3Binding() {
        return BindingBuilder
                .bind(smsRetryQueue3())
                .to(notificationsExchange())
                .with("sms.retry.3");
    }

    @Bean
    public Queue smsRetryQueue4() {
        return QueueBuilder
                .durable(SMS_RETRY_QUEUE_4)
                .ttl(TTL_1_HOUR)
                .deadLetterExchange(NOTIFICATIONS_EXCHANGE)
                .deadLetterRoutingKey("sms.retry.5")
                .build();
    }

    @Bean
    public Binding smsRetryQueue4Binding() {
        return BindingBuilder
                .bind(smsRetryQueue4())
                .to(notificationsExchange())
                .with("sms.retry.4");
    }

    @Bean
    public Queue smsRetryQueue5() {
        return QueueBuilder
                .durable(SMS_RETRY_QUEUE_5)
                .ttl(TTL_4_HOURS)
                .deadLetterExchange(NOTIFICATIONS_EXCHANGE)
                .deadLetterRoutingKey("sms.dlq")
                .build();
    }

    @Bean
    public Binding smsRetryQueue5Binding() {
        return BindingBuilder
                .bind(smsRetryQueue5())
                .to(notificationsExchange())
                .with("sms.retry.5");
    }

    // ======================
    // DEAD LETTER QUEUE SMS
    // ======================

    @Bean
    public Queue smsDlq() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-message-ttl", 30 * 24 * 60 * 60 * 1000L); // 30 días
        return QueueBuilder
                .durable(SMS_DLQ)
                .withArguments(args)
                .build();
    }

    @Bean
    public Binding smsDlqBinding() {
        return BindingBuilder
                .bind(smsDlq())
                .to(notificationsExchange())
                .with("sms.dlq");
    }

    // ======================
    // UTILIDADES PARA RETRY
    // ======================

    /**
     * Calcula el siguiente routing key de retry basado en el número de intento
     */
    public String getRetryRoutingKey(String channel, int attemptCount) {
        if (attemptCount >= maxRetryAttempts) {
            return channel + ".dlq";
        }
        return String.format("%s.retry.%d", channel, Math.min(attemptCount + 1, 5));
    }

    /**
     * Verifica si un mensaje debe ir a DLQ
     */
    public boolean shouldGoToDlq(int attemptCount) {
        return attemptCount >= maxRetryAttempts;
    }

    /**
     * Obtiene el TTL para un nivel de retry específico
     */
    public long getTtlForRetryLevel(int retryLevel) {
        switch (retryLevel) {
            case 1: return TTL_30_SECONDS;
            case 2: return TTL_2_MINUTES;
            case 3: return TTL_10_MINUTES;
            case 4: return TTL_1_HOUR;
            case 5: return TTL_4_HOURS;
            default: return TTL_30_SECONDS;
        }
    }
}