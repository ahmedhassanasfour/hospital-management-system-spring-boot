package com.ahmed.hospital.medicalrecord.entity;

import com.ahmed.hospital.appointment.entity.Appointment;
import com.ahmed.hospital.doctor.entity.Doctor;
import com.ahmed.hospital.patient.entity.Patient;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "medical_records",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_medical_record_appointment",
                        columnNames = "appointment_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicalRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "appointment_id",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(
                    name = "fk_medical_record_appointment"
            )
    )
    private Appointment appointment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "patient_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_medical_record_patient"
            )
    )
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "doctor_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_medical_record_doctor"
            )
    )
    private Doctor doctor;

    @Column(length = 2000)
    private String diagnosis;

    @Column(length = 3000)
    private String symptoms;

    @Column(length = 3000)
    private String notes;

    @Column(length = 3000)
    private String treatment;
}