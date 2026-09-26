package com.ahmed.hospital.billing.repository;

import com.ahmed.hospital.billing.entity.InvoiceItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, Long> {

    List<InvoiceItem> findByInvoiceId(Long invoiceId);

    List<InvoiceItem> findByLabOrderId(Long labOrderId);

    List<InvoiceItem> findByPrescriptionItemId(Long prescriptionItemId);
}