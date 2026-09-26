package com.ahmed.hospital.medicine.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

public record MedicineResponse(
        Long id,
        String name,
        String description,
        boolean active,
        LocalDateTime createdAt
) implements Serializable {
}