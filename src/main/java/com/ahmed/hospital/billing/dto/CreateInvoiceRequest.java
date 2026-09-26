package com.ahmed.hospital.billing.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.time.LocalDateTime;
import java.util.List;

public record CreateInvoiceRequest(

        Long patientId,

        Long appointmentId,

        String notes,

        LocalDateTime dueAt,

        @NotEmpty
        @Valid
        List<CreateInvoiceItemRequest> items
) {
}