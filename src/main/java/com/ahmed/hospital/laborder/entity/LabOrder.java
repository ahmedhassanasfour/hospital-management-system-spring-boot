package com.ahmed.hospital.laborder.entity;

import com.ahmed.hospital.appointment.entity.Appointment;
import com.ahmed.hospital.doctor.entity.Doctor;
import com.ahmed.hospital.labtest.entity.LabTest;
import com.ahmed.hospital.patient.entity.Patient;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "lab_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "appointment_id", nullable = false)
    private Appointment appointment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lab_test_id", nullable = false)
    private LabTest labTest;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private LabOrderStatus status = LabOrderStatus.ORDERED;

    @Column(length = 1000)
    private String notes;

    @Column(nullable = false, updatable = false)
    private LocalDateTime orderedAt;

    @Column
    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        orderedAt = LocalDateTime.now();
    }

    public void markInProgress() {
        this.status = LabOrderStatus.IN_PROGRESS;
    }

    public void markCompleted() {
        this.status = LabOrderStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public void cancel() {
        this.status = LabOrderStatus.CANCELLED;
    }
}