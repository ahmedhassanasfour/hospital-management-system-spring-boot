package com.ahmed.hospital.doctor.dto;

public record DoctorResponse(
        Long id,
        Long userId,
        String email,
        String firstName,
        String lastName,
        String specialization,
        String licenseNumber,
        String phone,
        String bio
) {
}