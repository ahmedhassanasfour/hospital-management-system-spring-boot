package com.ahmed.hospital.branch.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "branches",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_branch_name", columnNames = "name"),
                @UniqueConstraint(name = "uk_branch_email", columnNames = "email")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Branch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 255)
    private String address;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;
}