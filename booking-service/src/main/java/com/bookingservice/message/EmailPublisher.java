package com.bookingservice.message;

import com.bookingservice.config.RabbitConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailPublisher {

    private final AmqpTemplate amqpTemplate;

    /**
     * Send directly to queue by name. This avoids needing exchanges/bindings.
     */
    public void publishBookingCreated(EmailMessage msg) {
        amqpTemplate.convertAndSend(RabbitConfig.EMAIL_QUEUE, msg);
    }

    public void publishBookingCancelled(EmailMessage msg) {
        amqpTemplate.convertAndSend(RabbitConfig.EMAIL_QUEUE, msg);
    }
}