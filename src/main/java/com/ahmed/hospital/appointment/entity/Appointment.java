package com.ahmed.hospital.appointment.entity;

import com.ahmed.hospital.doctor.entity.Doctor;
import com.ahmed.hospital.patient.entity.Patient;
import com.ahmed.hospital.doctor.entity.DoctorBranch;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(
        name = "appointments"
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "doctor_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_appointment_doctor"
            )
    )
    private Doctor doctor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "patient_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_appointment_patient"
            )
    )
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "doctor_branch_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_appointment_doctor_branch"
            )
    )
    private DoctorBranch doctorBranch;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AppointmentStatus status = AppointmentStatus.PENDING;

    @Column(length = 1000)
    private String notes;
}