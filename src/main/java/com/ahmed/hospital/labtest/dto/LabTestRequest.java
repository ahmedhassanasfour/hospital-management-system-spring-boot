package com.ahmed.hospital.labtest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LabTestRequest(

        @NotBlank(message = "Lab test name is required")
        @Size(max = 150, message = "Lab test name must not exceed 150 characters")
        String name,

        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description

) {
}