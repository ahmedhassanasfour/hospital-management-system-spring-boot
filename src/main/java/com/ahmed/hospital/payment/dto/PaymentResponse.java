package com.ahmed.hospital.payment.dto;

import com.ahmed.hospital.payment.entity.PaymentMethod;
import com.ahmed.hospital.payment.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(

        Long id,

        Long invoiceId,

        String paymentReference,

        BigDecimal amount,

        PaymentMethod method,

        PaymentStatus status,

        LocalDateTime createdAt,

        LocalDateTime paidAt,

        String notes
) {
}