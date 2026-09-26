package com.ahmed.hospital.file.dto;

import com.ahmed.hospital.file.entity.FileType;

import java.time.LocalDateTime;

public record MedicalFileResponse(

        Long id,

        String originalName,

        String contentType,

        Long size,

        FileType fileType,

        Long patientId,

        Long prescriptionId,

        Long labResultId,

        LocalDateTime uploadedAt

) {
}