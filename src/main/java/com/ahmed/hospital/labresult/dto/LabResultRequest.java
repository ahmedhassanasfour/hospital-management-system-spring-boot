package com.ahmed.hospital.labresult.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LabResultRequest(

        @NotBlank(message = "Result value is required")
        @Size(max = 2000)
        String resultValue,

        @Size(max = 500)
        String referenceRange,

        @Size(max = 1000)
        String notes

) {
}