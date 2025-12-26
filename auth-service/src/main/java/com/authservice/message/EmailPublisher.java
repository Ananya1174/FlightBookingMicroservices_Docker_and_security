package com.authservice.message;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import com.flightapp.message.EmailMessage;

@Service
@RequiredArgsConstructor
public class EmailPublisher {

    private static final String EXCHANGE = "booking.exchange";
    private static final String ROUTING_KEY = "auth.password.reset"; 
    // reuse existing queue

    private final RabbitTemplate rabbitTemplate;

    public void publishPasswordReset(EmailMessage message) {
        rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, message);
    }
}