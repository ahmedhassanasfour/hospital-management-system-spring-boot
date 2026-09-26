package com.ahmed.hospital.billing.dto;

import com.ahmed.hospital.billing.entity.InvoiceStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record InvoiceResponse(

        Long id,

        String invoiceNumber,

        Long patientId,

        Long appointmentId,

        InvoiceStatus status,

        BigDecimal totalAmount,

        String notes,

        LocalDateTime issuedAt,

        LocalDateTime dueAt,

        List<InvoiceItemResponse> items
) {
}