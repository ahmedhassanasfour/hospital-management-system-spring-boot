package com.ahmed.hospital.doctor.entity;

import com.ahmed.hospital.branch.entity.Branch;
import jakarta.persistence.*;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Entity
@Table(
        name = "doctor_schedules",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_doctor_branch_day_time",
                        columnNames = {
                                "doctor_branch_id",
                                "day_of_week",
                                "start_time",
                                "end_time"
                        }
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "doctor_branch_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_schedule_doctor_branch"
            )
    )
    private DoctorBranch doctorBranch;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "day_of_week",
            nullable = false,
            length = 10
    )
    private DayOfWeek dayOfWeek;

    @Column(
            name = "start_time",
            nullable = false
    )
    private LocalTime startTime;

    @Column(
            name = "end_time",
            nullable = false
    )
    private LocalTime endTime;
}