package com.ahmed.hospital.prescription.dto;

public record PrescriptionItemResponse(

        Long id,

        Long medicineId,

        String medicineName,

        String dosage,

        String frequency,

        String duration,

        String instructions

) {
}