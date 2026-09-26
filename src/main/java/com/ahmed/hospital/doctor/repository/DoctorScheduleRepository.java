package com.ahmed.hospital.doctor.repository;

import com.ahmed.hospital.doctor.entity.DoctorSchedule;
import com.ahmed.hospital.doctor.entity.DoctorBranch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;



public interface DoctorScheduleRepository
        extends JpaRepository<DoctorSchedule, Long> {

    List<DoctorSchedule> findByDoctorBranch(
            DoctorBranch doctorBranch
    );

    List<DoctorSchedule> findByDoctorBranchId(
            Long doctorBranchId
    );

    List<DoctorSchedule> findByDoctorBranchIdAndDayOfWeek(
            Long doctorBranchId,
            DayOfWeek dayOfWeek
    );

    List<DoctorSchedule> findByDoctorBranchDoctorIdAndDayOfWeek(
            Long doctorId,
            DayOfWeek dayOfWeek
    );

    boolean existsByDoctorBranchIdAndDayOfWeekAndStartTimeAndEndTime(
            Long doctorBranchId,
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime
    );
}