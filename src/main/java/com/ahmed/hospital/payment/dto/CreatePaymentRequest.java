package com.ahmed.hospital.payment.dto;

import com.ahmed.hospital.payment.entity.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreatePaymentRequest(

        @NotNull
        Long invoiceId,

        @NotNull
        @DecimalMin(value = "0.01")
        BigDecimal amount,

        @NotNull
        PaymentMethod method,

        String notes
) {
}