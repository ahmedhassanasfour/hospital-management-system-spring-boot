package com.ahmed.hospital.prescription.entity;

import com.ahmed.hospital.appointment.entity.Appointment;
import com.ahmed.hospital.doctor.entity.Doctor;
import com.ahmed.hospital.patient.entity.Patient;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "prescriptions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_prescription_appointment",
                        columnNames = "appointment_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Prescription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "appointment_id", nullable = false, unique = true)
    private Appointment appointment;

    @Column(length = 1000)
    private String notes;

    @Column(nullable = false, updatable = false)
    private LocalDateTime prescribedAt;

    @OneToMany(
            mappedBy = "prescription",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<PrescriptionItem> items = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        prescribedAt = LocalDateTime.now();
    }

    public void addItem(PrescriptionItem item) {
        items.add(item);
        item.setPrescription(this);
    }

    public void removeItem(PrescriptionItem item) {
        items.remove(item);
        item.setPrescription(null);
    }
}