package com.parvez.blogs.amqp;

import com.parvez.blogs.config.RabbitMQConfig;
import com.parvez.blogs.dto.EmailEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailProducer {

    private final RabbitTemplate rabbitTemplate;

    public void sendPasswordResetEmail(String to, String resetLink) {
        sendEmail(new EmailEvent(
                to,
                "Reset your Blog Post password",
                "Click the link below to reset your password. It expires in 10 minutes.\n\n"
                        + resetLink
                        + "\n\nIf you did not request this, you can safely ignore this email."
        ));
    }

    // Package-private — other services can publish custom events if needed
    void sendEmail(EmailEvent event) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EMAIL_EXCHANGE,
                    RabbitMQConfig.EMAIL_ROUTING_KEY,
                    event
            );
            log.info("Email event queued for '{}'", event.getTo());
        } catch (AmqpException e) {
            // Log and re-throw so the caller's transaction rolls back
            log.error("Failed to enqueue email event for '{}': {}", event.getTo(), e.getMessage());
            throw e;
        }
    }
}