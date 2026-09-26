package com.ahmed.hospital.billing.repository;

import com.ahmed.hospital.billing.entity.Invoice;
import com.ahmed.hospital.billing.entity.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    boolean existsByInvoiceNumber(String invoiceNumber);

    List<Invoice> findByPatientIdOrderByIssuedAtDesc(Long patientId);

    List<Invoice> findByStatusOrderByIssuedAtDesc(InvoiceStatus status);
}