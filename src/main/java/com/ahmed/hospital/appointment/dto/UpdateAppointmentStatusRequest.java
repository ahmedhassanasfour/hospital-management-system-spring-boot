package com.ahmed.hospital.appointment.dto;

import com.ahmed.hospital.appointment.entity.AppointmentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateAppointmentStatusRequest {

    @NotNull(message = "Status is required")
    private AppointmentStatus status;
}