package com.ahmed.hospital.doctor.entity;

import com.ahmed.hospital.branch.entity.Branch;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "doctor_branches",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_doctor_branch",
                        columnNames = {"doctor_id", "branch_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorBranch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "doctor_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_doctor_branch_doctor")
    )
    private Doctor doctor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "branch_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_doctor_branch_branch")
    )
    private Branch branch;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;
}