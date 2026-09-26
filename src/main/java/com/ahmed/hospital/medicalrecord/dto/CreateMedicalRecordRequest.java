package com.ahmed.hospital.medicalrecord.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateMedicalRecordRequest {

    @NotBlank(message = "Diagnosis is required")
    private String diagnosis;

    private String symptoms;

    private String notes;

    private String treatment;
}