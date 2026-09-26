package com.ahmed.hospital.email.service;

public interface EmailService {

    void sendEmail(
            String to,
            String subject,
            String body
    );
}