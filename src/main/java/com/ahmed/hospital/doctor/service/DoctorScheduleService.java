package com.ahmed.hospital.doctor.service;

import com.ahmed.hospital.common.exception.DuplicateResourceException;
import com.ahmed.hospital.common.exception.ResourceNotFoundException;
import com.ahmed.hospital.doctor.dto.CreateDoctorScheduleRequest;
import com.ahmed.hospital.doctor.dto.DoctorScheduleResponse;
import com.ahmed.hospital.doctor.dto.UpdateDoctorScheduleRequest;
import com.ahmed.hospital.doctor.entity.DoctorBranch;
import com.ahmed.hospital.doctor.entity.DoctorSchedule;
import com.ahmed.hospital.doctor.repository.DoctorBranchRepository;
import com.ahmed.hospital.doctor.repository.DoctorScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DoctorScheduleService {

    private final DoctorScheduleRepository doctorScheduleRepository;
    private final DoctorBranchRepository doctorBranchRepository;

    @Transactional
    public DoctorScheduleResponse createSchedule(
            Long doctorId,
            Long branchId,
            CreateDoctorScheduleRequest request
    ) {

        DoctorBranch doctorBranch =
                findActiveDoctorBranch(doctorId, branchId);

        validateTime(
                request.getStartTime(),
                request.getEndTime()
        );

        validateDuplicate(
                doctorBranch.getId(),
                request.getDayOfWeek(),
                request.getStartTime(),
                request.getEndTime()
        );

        validateOverlap(
                doctorId,
                request.getDayOfWeek(),
                request.getStartTime(),
                request.getEndTime()
        );

        DoctorSchedule schedule = DoctorSchedule.builder()
                .doctorBranch(doctorBranch)
                .dayOfWeek(request.getDayOfWeek())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .build();

        DoctorSchedule saved =
                doctorScheduleRepository.save(schedule);

        return mapToResponse(saved);
    }

    public DoctorScheduleResponse getScheduleById(Long id) {

        DoctorSchedule schedule =
                findScheduleById(id);

        return mapToResponse(schedule);
    }

    public List<DoctorScheduleResponse> getDoctorSchedules(
            Long doctorId,
            Long branchId
    ) {

        DoctorBranch doctorBranch =
                findActiveDoctorBranch(
                        doctorId,
                        branchId
                );

        return doctorScheduleRepository
                .findByDoctorBranchId(doctorBranch.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public DoctorScheduleResponse updateSchedule(
            Long id,
            UpdateDoctorScheduleRequest request
    ) {

        DoctorSchedule schedule =
                findScheduleById(id);

        validateTime(
                request.getStartTime(),
                request.getEndTime()
        );

        Long doctorBranchId =
                schedule.getDoctorBranch().getId();

        Long doctorId =
                schedule.getDoctorBranch()
                        .getDoctor()
                        .getId();

        validateDuplicateForUpdate(
                id,
                doctorBranchId,
                request.getDayOfWeek(),
                request.getStartTime(),
                request.getEndTime()
        );

        validateOverlapForUpdate(
                id,
                doctorId,
                request.getDayOfWeek(),
                request.getStartTime(),
                request.getEndTime()
        );

        schedule.setDayOfWeek(
                request.getDayOfWeek()
        );

        schedule.setStartTime(
                request.getStartTime()
        );

        schedule.setEndTime(
                request.getEndTime()
        );

        return mapToResponse(schedule);
    }

    @Transactional
    public void deleteSchedule(Long id) {

        DoctorSchedule schedule =
                findScheduleById(id);

        doctorScheduleRepository.delete(schedule);
    }

    private DoctorBranch findActiveDoctorBranch(
            Long doctorId,
            Long branchId
    ) {

        DoctorBranch doctorBranch =
                doctorBranchRepository
                        .findByDoctorIdAndBranchId(
                                doctorId,
                                branchId
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Doctor is not assigned to this branch"
                                )
                        );

        if (!doctorBranch.isEnabled()) {

            throw new IllegalStateException(
                    "Doctor is disabled in this branch"
            );
        }

        return doctorBranch;
    }

    private DoctorSchedule findScheduleById(
            Long id
    ) {

        return doctorScheduleRepository
                .findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Schedule not found with id: " + id
                        )
                );
    }

    private void validateTime(
            LocalTime startTime,
            LocalTime endTime
    ) {

        if (!startTime.isBefore(endTime)) {

            throw new IllegalArgumentException(
                    "Start time must be before end time"
            );
        }
    }

    private void validateDuplicate(
            Long doctorBranchId,
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime
    ) {

        boolean exists =
                doctorScheduleRepository
                        .existsByDoctorBranchIdAndDayOfWeekAndStartTimeAndEndTime(
                                doctorBranchId,
                                dayOfWeek,
                                startTime,
                                endTime
                        );

        if (exists) {

            throw new DuplicateResourceException(
                    "Doctor schedule already exists"
            );
        }
    }

    private void validateDuplicateForUpdate(
            Long scheduleId,
            Long doctorBranchId,
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime
    ) {

        List<DoctorSchedule> schedules =
                doctorScheduleRepository
                        .findByDoctorBranchIdAndDayOfWeek(
                                doctorBranchId,
                                dayOfWeek
                        );

        boolean duplicate =
                schedules.stream()
                        .anyMatch(schedule ->
                                !schedule.getId()
                                        .equals(scheduleId)
                                        && schedule.getStartTime()
                                        .equals(startTime)
                                        && schedule.getEndTime()
                                        .equals(endTime)
                        );

        if (duplicate) {

            throw new DuplicateResourceException(
                    "Doctor schedule already exists"
            );
        }
    }

    private void validateOverlap(
            Long doctorId,
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime
    ) {

        List<DoctorSchedule> schedules =
                doctorScheduleRepository
                        .findByDoctorBranchDoctorIdAndDayOfWeek(
                                doctorId,
                                dayOfWeek
                        );

        boolean overlap =
                schedules.stream()
                        .anyMatch(schedule ->
                                startTime.isBefore(
                                        schedule.getEndTime()
                                )
                                        && endTime.isAfter(
                                        schedule.getStartTime()
                                )
                        );

        if (overlap) {

            throw new DuplicateResourceException(
                    "Doctor schedule overlaps with an existing schedule"
            );
        }
    }

    private void validateOverlapForUpdate(
            Long scheduleId,
            Long doctorId,
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime
    ) {

        List<DoctorSchedule> schedules =
                doctorScheduleRepository
                        .findByDoctorBranchDoctorIdAndDayOfWeek(
                                doctorId,
                                dayOfWeek
                        );

        boolean overlap =
                schedules.stream()
                        .anyMatch(schedule ->
                                !schedule.getId()
                                        .equals(scheduleId)
                                        && startTime.isBefore(
                                        schedule.getEndTime()
                                )
                                        && endTime.isAfter(
                                        schedule.getStartTime()
                                )
                        );

        if (overlap) {

            throw new DuplicateResourceException(
                    "Doctor schedule overlaps with an existing schedule"
            );
        }
    }

    private DoctorScheduleResponse mapToResponse(
            DoctorSchedule schedule
    ) {

        DoctorBranch doctorBranch =
                schedule.getDoctorBranch();

        var doctor =
                doctorBranch.getDoctor();

        var branch =
                doctorBranch.getBranch();

        String doctorName =
                doctor.getUser().getFirstName()
                        + " "
                        + doctor.getUser().getLastName();

        return DoctorScheduleResponse.builder()
                .id(schedule.getId())
                .doctorBranchId(doctorBranch.getId())
                .doctorId(doctor.getId())
                .doctorName(doctorName)
                .branchId(branch.getId())
                .branchName(branch.getName())
                .dayOfWeek(schedule.getDayOfWeek())
                .startTime(schedule.getStartTime())
                .endTime(schedule.getEndTime())
                .build();
    }
}