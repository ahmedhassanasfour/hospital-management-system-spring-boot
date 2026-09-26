package com.ahmed.hospital.doctor.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateDoctorRequest(

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
        String specialization,

        @NotBlank
        String licenseNumber,

        @NotBlank
        String phone,

        @Size(max = 1000)
        String bio
) {
}