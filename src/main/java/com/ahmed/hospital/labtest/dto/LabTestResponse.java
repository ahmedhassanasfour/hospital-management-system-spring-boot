package com.ahmed.hospital.labtest.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

public record LabTestResponse(
        Long id,
        String name,
        String description,
        boolean active,
        LocalDateTime createdAt
) implements Serializable {
}