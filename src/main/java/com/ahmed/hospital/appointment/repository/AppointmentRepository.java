package com.ahmed.hospital.appointment.repository;

import com.ahmed.hospital.appointment.entity.Appointment;
import com.ahmed.hospital.appointment.entity.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface AppointmentRepository
        extends JpaRepository<Appointment, Long> {

    List<Appointment> findByDoctorIdAndDate(
            Long doctorId,
            LocalDate date
    );

    List<Appointment> findByPatientIdOrderByDateDescStartTimeDesc(
            Long patientId
    );

    List<Appointment> findByDoctorIdOrderByDateDescStartTimeDesc(
            Long doctorId
    );

    @Query("""
            SELECT COUNT(a) > 0
            FROM Appointment a
            WHERE a.doctor.id = :doctorId
              AND a.date = :date
              AND a.status IN :statuses
              AND :startTime < a.endTime
              AND :endTime > a.startTime
            """)
    boolean existsOverlappingAppointment(
            @Param("doctorId") Long doctorId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("statuses") List<AppointmentStatus> statuses
    );

    /**
     * Finds CONFIRMED appointments whose date falls between {@code fromDate} and
     * {@code toDate} (inclusive).  Used by the appointment-reminder scheduler to
     * find appointments happening in the next N hours.
     */
    @Query("""
            SELECT a
            FROM Appointment a
            WHERE a.status = com.ahmed.hospital.appointment.entity.AppointmentStatus.CONFIRMED
              AND a.date >= :fromDate
              AND a.date <= :toDate
            """)
    List<Appointment> findUpcomingAppointmentsForReminder(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );
}