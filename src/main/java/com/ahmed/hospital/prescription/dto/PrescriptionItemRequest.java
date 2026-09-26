package com.ahmed.hospital.prescription.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PrescriptionItemRequest(

        @NotNull(message = "Medicine ID is required")
        Long medicineId,

        @NotBlank(message = "Dosage is required")
        @Size(max = 100)
        String dosage,

        @NotBlank(message = "Frequency is required")
        @Size(max = 100)
        String frequency,

        @NotBlank(message = "Duration is required")
        @Size(max = 100)
        String duration,

        @Size(max = 500)
        String instructions

) {
}