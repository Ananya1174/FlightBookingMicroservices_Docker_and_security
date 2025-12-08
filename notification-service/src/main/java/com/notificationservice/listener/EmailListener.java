package com.notificationservice.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notificationservice.message.EmailMessage;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailListener {

    private final JavaMailSender mailSender;
    private final ObjectMapper objectMapper;

    public EmailListener(JavaMailSender mailSender, ObjectMapper objectMapper) {
        this.mailSender = mailSender;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = "email.queue")
    public void receive(String jsonPayload) {
        try {
            EmailMessage message = objectMapper.readValue(jsonPayload, EmailMessage.class);

            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setTo(message.getTo());
            mail.setSubject(message.getSubject());
            mail.setText(message.getBody());

            mailSender.send(mail);

            System.out.println("Email sent to: " + message.getTo());
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Failed to process email message", ex);
        }
    }
}