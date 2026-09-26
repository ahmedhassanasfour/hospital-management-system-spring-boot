package com.ahmed.hospital.medicalrecord.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateMedicalRecordRequest {

    @NotBlank(message = "Diagnosis is required")
    private String diagnosis;

    private String symptoms;

    private String notes;

    private String treatment;
}