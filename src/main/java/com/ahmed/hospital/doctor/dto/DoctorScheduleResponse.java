package com.ahmed.hospital.doctor.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Getter
@Builder
public class DoctorScheduleResponse {

    private Long id;

    private Long doctorBranchId;

    private Long doctorId;
    private String doctorName;

    private Long branchId;
    private String branchName;

    private DayOfWeek dayOfWeek;

    private LocalTime startTime;
    private LocalTime endTime;
}