package com.ahmed.hospital.doctor.entity;

import com.ahmed.hospital.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "doctors",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_doctor_license_number",
                        columnNames = "license_number"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Doctor {

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

    @Column(nullable = false)
    private String specialization;

    @Column(name = "license_number", nullable = false)
    private String licenseNumber;

    @Column(nullable = false)
    private String phone;

    @Column(length = 1000)
    private String bio;
}