package com.ahmed.hospital.patient.dto;

import com.ahmed.hospital.patient.entity.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreatePatientRequest(

        @NotBlank
        @Email
        String email,

        @NotBlank
        @Size(min = 8, max = 100)
        String password,

        @NotBlank
        String firstName,

        @NotBlank
        String lastName,

        @NotBlank
        String nationalId,

        @NotBlank
        String phone,

        LocalDate dateOfBirth,

        Gender gender,

        @Size(max = 1000)
        String address
) {
}