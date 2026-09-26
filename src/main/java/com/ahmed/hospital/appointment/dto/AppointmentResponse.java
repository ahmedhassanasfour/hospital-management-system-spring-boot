package com.ahmed.hospital.appointment.dto;

import com.ahmed.hospital.appointment.entity.AppointmentStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Builder
public class AppointmentResponse {

    private Long id;

    private Long doctorId;
    private String doctorName;

    private Long patientId;
    private String patientName;

    private Long branchId;
    private String branchName;

    private LocalDate date;

    private LocalTime startTime;
    private LocalTime endTime;

    private AppointmentStatus status;

    private String notes;
}