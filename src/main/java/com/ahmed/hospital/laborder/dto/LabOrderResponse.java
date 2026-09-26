package com.ahmed.hospital.laborder.dto;

import com.ahmed.hospital.laborder.entity.LabOrderStatus;

import java.time.LocalDateTime;

public record LabOrderResponse(

        Long id,

        Long patientId,

        Long doctorId,

        Long appointmentId,

        Long labTestId,

        String labTestName,

        LabOrderStatus status,

        String notes,

        LocalDateTime orderedAt,

        LocalDateTime completedAt

) {
}