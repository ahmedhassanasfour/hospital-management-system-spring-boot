package com.ahmed.hospital.billing.dto;

import com.ahmed.hospital.billing.entity.InvoiceItemType;

import java.math.BigDecimal;

public record InvoiceItemResponse(

        Long id,

        InvoiceItemType type,

        String description,

        BigDecimal quantity,

        BigDecimal unitPrice,

        BigDecimal totalPrice,

        Long labOrderId,

        Long prescriptionItemId
) {
}