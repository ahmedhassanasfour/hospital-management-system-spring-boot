package com.ahmed.hospital.patient.entity;

import com.ahmed.hospital.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(
        name = "patients",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_patient_national_id",
                        columnNames = "national_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            unique = true
    )
    private User user;

    @Column(
            name = "national_id",
            nullable = false,
            unique = true
    )
    private String nationalId;

    @Column(nullable = false)
    private String phone;

    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Column(length = 1000)
    private String address;
}