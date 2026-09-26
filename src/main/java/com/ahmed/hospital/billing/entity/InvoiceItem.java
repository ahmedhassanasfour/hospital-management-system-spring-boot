package com.ahmed.hospital.billing.entity;

import com.ahmed.hospital.laborder.entity.LabOrder;
import com.ahmed.hospital.prescription.entity.PrescriptionItem;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "invoice_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InvoiceItemType type;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lab_order_id")
    private LabOrder labOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescription_item_id")
    private PrescriptionItem prescriptionItem;

    @PrePersist
    @PreUpdate
    protected void calculateTotal() {

        if (quantity != null && unitPrice != null) {
            totalPrice = quantity.multiply(unitPrice);
        }
    }
}