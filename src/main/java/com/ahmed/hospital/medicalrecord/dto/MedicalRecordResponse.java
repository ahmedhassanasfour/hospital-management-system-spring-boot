package com.ahmed.hospital.medicalrecord.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MedicalRecordResponse {

    private Long id;

    private Long appointmentId;

    private Long doctorId;
    private String doctorName;

    private Long patientId;
    private String patientName;

    private String diagnosis;
    private String symptoms;
    private String notes;
    private String treatment;
}