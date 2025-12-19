package com.bookingservice.message;

import com.bookingservice.config.RabbitConfig;
import com.flightapp.message.EmailMessage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.amqp.core.AmqpTemplate;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class EmailPublisherTest {

    @Mock AmqpTemplate amqpTemplate;
    @InjectMocks EmailPublisher emailPublisher;

    @Test
    void publishBookingCreated_callsConvertAndSend() {
        EmailMessage m = new EmailMessage("to@x.com", "subject", "body");
        emailPublisher.publishBookingCreated(m);
        verify(amqpTemplate, times(1)).convertAndSend(RabbitConfig.EMAIL_QUEUE, m);
    }

    @Test
    void publishBookingCancelled_callsConvertAndSend() {
        EmailMessage m = new EmailMessage("to@x.com", "subject", "body");
        emailPublisher.publishBookingCancelled(m);
        verify(amqpTemplate, times(1)).convertAndSend(RabbitConfig.EMAIL_QUEUE, m);
    }
}