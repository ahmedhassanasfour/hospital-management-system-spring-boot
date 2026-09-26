package com.ahmed.hospital.email.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Override
    @Async("taskExecutor")
    public void sendEmail(
            String to,
            String subject,
            String body
    ) {

        SimpleMailMessage message =
                new SimpleMailMessage();

        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);

        try {
            mailSender.send(message);

            log.info(
                    "Email sent successfully to {}",
                    to
            );

        } catch (Exception ex) {

            log.error(
                    "Failed to send email to {}",
                    to,
                    ex
            );
        }
    }
}