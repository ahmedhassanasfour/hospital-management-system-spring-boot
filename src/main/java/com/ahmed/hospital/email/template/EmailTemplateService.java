package com.ahmed.hospital.email.template;

import org.springframework.stereotype.Service;

@Service
public class EmailTemplateService {

    public String appointmentConfirmed(
            String patientName,
            String doctorName,
            String appointmentDate
    ) {
        return """
                Hello %s,

                Your appointment has been confirmed.

                Doctor: %s
                Date: %s

                Thank you,
                Hospital Management System
                """.formatted(
                patientName,
                doctorName,
                appointmentDate
        );
    }

    public String appointmentCancelled(
            String patientName,
            String doctorName,
            String appointmentDate
    ) {
        return """
                Hello %s,

                Your appointment has been cancelled.

                Doctor: %s
                Date: %s

                Thank you,
                Hospital Management System
                """.formatted(
                patientName,
                doctorName,
                appointmentDate
        );
    }

    public String passwordReset(String resetLink) {
        return """
                Hello,

                You requested a password reset.

                Use the following link:

                %s

                If you did not request this, please ignore this email.

                Hospital Management System
                """.formatted(resetLink);
    }
}