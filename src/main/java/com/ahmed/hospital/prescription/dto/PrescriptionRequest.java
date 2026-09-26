package com.ahmed.hospital.prescription.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PrescriptionRequest(

        @NotNull(message = "Appointment ID is required")
        Long appointmentId,

        @Size(max = 1000)
        String notes,

        @NotEmpty(message = "Prescription must contain at least one medicine")
        @Valid
        List<PrescriptionItemRequest> items

) {
}