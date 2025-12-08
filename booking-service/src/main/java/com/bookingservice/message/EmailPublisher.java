package com.bookingservice.message;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailPublisher {

    private final AmqpTemplate amqpTemplate;

    @Value("${app.rabbitmq.exchange}")
    private String exchange;

    @Value("${app.rabbitmq.routing.booking.created}")
    private String routingCreated;

    @Value("${app.rabbitmq.routing.booking.cancelled}")
    private String routingCancelled;

    public void publishBookingCreated(EmailMessage msg) {
        amqpTemplate.convertAndSend(exchange, routingCreated, msg);
    }

    public void publishBookingCancelled(EmailMessage msg) {
        amqpTemplate.convertAndSend(exchange, routingCancelled, msg);
    }
}