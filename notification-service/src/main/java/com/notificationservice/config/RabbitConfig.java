package com.notificationservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

@Configuration
public class RabbitConfig {

    public static final String EMAIL_QUEUE = "email.queue";
    public static final String BOOKING_EXCHANGE = "booking.exchange";
    public static final String ROUTING_CREATED = "booking.created";
    public static final String ROUTING_CANCELLED = "booking.cancelled";
    public static final String ROUTING_PASSWORD_RESET = "auth.password.reset";
    
    @Bean
    public Binding bindingPasswordReset(Queue emailQueue, TopicExchange bookingExchange) {
        return BindingBuilder.bind(emailQueue)
                .to(bookingExchange)
                .with(ROUTING_PASSWORD_RESET);
    }

    @Bean
    public Queue emailQueue() {
        return QueueBuilder.durable(EMAIL_QUEUE).build();
    }

    @Bean
    public TopicExchange bookingExchange() {
        return ExchangeBuilder.topicExchange(BOOKING_EXCHANGE).durable(true).build();
    }

    @Bean
    public Binding bindingCreated(Queue emailQueue, TopicExchange bookingExchange) {
        return BindingBuilder.bind(emailQueue)
                .to(bookingExchange)
                .with(ROUTING_CREATED);
    }

    @Bean
    public Binding bindingCancelled(Queue emailQueue, TopicExchange bookingExchange) {
        return BindingBuilder.bind(emailQueue)
                .to(bookingExchange)
                .with(ROUTING_CANCELLED);
    }

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        admin.setAutoStartup(true);
        return admin;
    }

    // ✅ Spring AMQP 4.x correct converter
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}