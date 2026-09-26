package com.ahmed.hospital.prescription.dto;

import java.time.LocalDateTime;
import java.util.List;

public record PrescriptionResponse(

        Long id,

        Long patientId,

        Long doctorId,

        Long appointmentId,

        String notes,

        LocalDateTime prescribedAt,

        List<PrescriptionItemResponse> items

) {
}