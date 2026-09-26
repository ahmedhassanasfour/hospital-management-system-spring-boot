package com.ahmed.hospital.labresult.entity;

import com.ahmed.hospital.laborder.entity.LabOrder;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "lab_results",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_lab_result_order",
                        columnNames = "lab_order_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "lab_order_id",
            nullable = false,
            unique = true
    )
    private LabOrder labOrder;

    @Column(nullable = false, length = 2000)
    private String resultValue;

    @Column(length = 500)
    private String referenceRange;

    @Column(length = 1000)
    private String notes;

    @Column(nullable = false, updatable = false)
    private LocalDateTime resultDate;

    @PrePersist
    protected void onCreate() {
        resultDate = LocalDateTime.now();
    }
}