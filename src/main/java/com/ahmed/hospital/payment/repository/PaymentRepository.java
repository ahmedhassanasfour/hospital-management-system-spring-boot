package com.ahmed.hospital.payment.repository;

import com.ahmed.hospital.payment.entity.Payment;
import com.ahmed.hospital.payment.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByPaymentReference(String paymentReference);

    boolean existsByPaymentReference(String paymentReference);

    List<Payment> findByInvoiceIdOrderByCreatedAtDesc(Long invoiceId);

    List<Payment> findByInvoiceIdAndStatus(
            Long invoiceId,
            PaymentStatus status
    );

    @Query("""
            SELECT COALESCE(SUM(p.amount), 0)
            FROM Payment p
            WHERE p.invoice.id = :invoiceId
            AND p.status = :status
            """)
    BigDecimal calculateTotalPaid(
            @Param("invoiceId") Long invoiceId,
            @Param("status") PaymentStatus status
    );
}