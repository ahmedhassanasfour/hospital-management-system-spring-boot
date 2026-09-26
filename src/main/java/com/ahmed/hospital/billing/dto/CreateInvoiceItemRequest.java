package com.ahmed.hospital.billing.dto;

import com.ahmed.hospital.billing.entity.InvoiceItemType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateInvoiceItemRequest(

        @NotNull
        InvoiceItemType type,

        @NotBlank
        String description,

        @NotNull
        @DecimalMin(value = "0.01")
        BigDecimal quantity,

        @NotNull
        @DecimalMin(value = "0.00")
        BigDecimal unitPrice,

        Long labOrderId,

        Long prescriptionItemId
) {
}