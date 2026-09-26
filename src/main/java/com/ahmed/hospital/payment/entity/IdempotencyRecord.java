package com.ahmed.hospital.payment.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "idempotency_records",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_idempotency_key",
                        columnNames = "idempotency_key"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdempotencyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "idempotency_key",
            nullable = false,
            unique = true,
            length = 100
    )
    private String idempotencyKey;

    @Column(nullable = false)
    private Long paymentId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}