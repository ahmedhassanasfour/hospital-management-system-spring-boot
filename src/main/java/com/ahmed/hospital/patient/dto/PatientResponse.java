package com.ahmed.hospital.patient.dto;

import com.ahmed.hospital.patient.entity.Gender;

import java.time.LocalDate;

public record PatientResponse(

        Long id,
        Long userId,
        String email,
        String firstName,
        String lastName,
        String nationalId,
        String phone,
        LocalDate dateOfBirth,
        Gender gender,
        String address
) {
}