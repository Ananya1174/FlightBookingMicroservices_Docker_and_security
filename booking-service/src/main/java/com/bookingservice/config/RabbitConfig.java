package com.bookingservice.config;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Value("${app.rabbitmq.exchange}")
    private String exchangeName;

    @Value("${app.rabbitmq.booking.queue}")
    private String bookingQueue;

    @Value("${app.rabbitmq.routing.booking.created}")
    private String routingCreated;

    @Value("${app.rabbitmq.routing.booking.cancelled}")
    private String routingCancelled;

    @Bean
    public TopicExchange bookingExchange() {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    public Queue bookingQueue() {
        return QueueBuilder.durable(bookingQueue).build();
    }

    @Bean
    public Binding bindingCreated(Queue bookingQueue, TopicExchange bookingExchange) {
        return BindingBuilder.bind(bookingQueue).to(bookingExchange).with(routingCreated);
    }

    @Bean
    public Binding bindingCancelled(Queue bookingQueue, TopicExchange bookingExchange) {
        return BindingBuilder.bind(bookingQueue).to(bookingExchange).with(routingCancelled);
    }
}