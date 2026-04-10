package com.parvez.blogs.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // =========================
    // MAIN EMAIL PIPELINE
    // =========================
    public static final String EMAIL_QUEUE = "email.queue.v1";
    public static final String EMAIL_EXCHANGE = "email.exchange";
    public static final String EMAIL_ROUTING_KEY = "email.send";

    // =========================
    // DEAD LETTER CONFIG
    // =========================
    public static final String EMAIL_DLQ = "email.dlq";
    public static final String EMAIL_DL_EXCHANGE = "email.dlx";
    public static final String EMAIL_DL_ROUTING_KEY = "email.dead";

    // =========================
    // ADMIN (NO SILENT FAIL)
    // =========================
    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    // =========================
    // DEAD LETTER EXCHANGE
    // =========================
    @Bean
    public TopicExchange emailDeadLetterExchange() {
        return new TopicExchange(EMAIL_DL_EXCHANGE);
    }

    // =========================
    // DEAD LETTER QUEUE
    // =========================
    @Bean
    public Queue emailDeadLetterQueue() {
        return QueueBuilder
                .durable(EMAIL_DLQ)
                .build();
    }

    @Bean
    public Binding emailDeadLetterBinding() {
        return BindingBuilder
                .bind(emailDeadLetterQueue())
                .to(emailDeadLetterExchange())
                .with(EMAIL_DL_ROUTING_KEY);
    }

    // =========================
    // MAIN QUEUE (QUORUM + DLQ)
    // =========================
    @Bean
    public Queue emailQueue() {
        return QueueBuilder
                .durable(EMAIL_QUEUE)
                .quorum() // ✅ Required for delivery-limit
                .withArgument("x-dead-letter-exchange", EMAIL_DL_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", EMAIL_DL_ROUTING_KEY)
                .withArgument("x-delivery-limit", 3)
                .build();
    }

    // =========================
    // MAIN EXCHANGE
    // =========================
    @Bean
    public TopicExchange emailExchange() {
        return new TopicExchange(EMAIL_EXCHANGE);
    }

    // =========================
    // BINDING
    // =========================
    @Bean
    public Binding emailBinding() {
        return BindingBuilder
                .bind(emailQueue())
                .to(emailExchange())
                .with(EMAIL_ROUTING_KEY);
    }
}