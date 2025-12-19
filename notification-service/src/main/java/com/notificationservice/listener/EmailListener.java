package com.notificationservice.listener;

import com.flightapp.message.EmailMessage;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailListener {

    private final JavaMailSender mailSender;

    public EmailListener(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @RabbitListener(queues = "email.queue")
    public void receive(EmailMessage message) {

        if (message.getTo() == null || !message.getTo().contains("@")) {
            System.err.println("Invalid email received: " + message.getTo());
            return;
        }

        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setTo(message.getTo());
        mail.setSubject(message.getSubject());
        mail.setText(message.getBody());

        mailSender.send(mail);

        System.out.println("Email sent to: " + message.getTo());
    }
}