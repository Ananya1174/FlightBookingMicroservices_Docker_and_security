package com.bookingservice.message;

import com.bookingservice.config.RabbitConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailPublisher {

	private final AmqpTemplate amqpTemplate;

	public void publishBookingCreated(EmailMessage msg) {
		amqpTemplate.convertAndSend(RabbitConfig.EMAIL_QUEUE, msg);
	}

	public void publishBookingCancelled(EmailMessage msg) {
		amqpTemplate.convertAndSend(RabbitConfig.EMAIL_QUEUE, msg);
	}
}