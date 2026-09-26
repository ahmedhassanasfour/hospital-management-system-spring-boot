package com.ahmed.hospital.laborder.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LabOrderRequest(

        @NotNull(message = "Appointment ID is required")
        Long appointmentId,

        @NotNull(message = "Lab test ID is required")
        Long labTestId,

        @Size(max = 1000)
        String notes

) {
}