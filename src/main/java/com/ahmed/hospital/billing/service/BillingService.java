package com.ahmed.hospital.billing.service;

import com.ahmed.hospital.appointment.entity.Appointment;
import com.ahmed.hospital.appointment.repository.AppointmentRepository;
import com.ahmed.hospital.billing.dto.CreateInvoiceItemRequest;
import com.ahmed.hospital.billing.dto.CreateInvoiceRequest;
import com.ahmed.hospital.billing.dto.InvoiceItemResponse;
import com.ahmed.hospital.billing.dto.InvoiceResponse;
import com.ahmed.hospital.billing.entity.Invoice;
import com.ahmed.hospital.billing.entity.InvoiceItem;
import com.ahmed.hospital.billing.entity.InvoiceItemType;
import com.ahmed.hospital.billing.entity.InvoiceStatus;
import com.ahmed.hospital.billing.repository.InvoiceRepository;
import com.ahmed.hospital.doctor.entity.Doctor;
import com.ahmed.hospital.doctor.repository.DoctorRepository;
import com.ahmed.hospital.laborder.entity.LabOrder;
import com.ahmed.hospital.laborder.repository.LabOrderRepository;
import com.ahmed.hospital.patient.entity.Patient;
import com.ahmed.hospital.patient.repository.PatientRepository;
import com.ahmed.hospital.prescription.entity.PrescriptionItem;
import com.ahmed.hospital.prescription.repository.PrescriptionItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class BillingService {

    private final InvoiceRepository invoiceRepository;
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final LabOrderRepository labOrderRepository;
    private final PrescriptionItemRepository prescriptionItemRepository;

    public InvoiceResponse createInvoice(CreateInvoiceRequest request) {

        Patient patient = patientRepository.findById(request.patientId())
                .orElseThrow(() ->
                        new RuntimeException("Patient not found")
                );

        Appointment appointment = null;

        if (request.appointmentId() != null) {

            appointment = appointmentRepository.findById(request.appointmentId())
                    .orElseThrow(() ->
                            new com.ahmed.hospital.common.exception.ResourceNotFoundException("Appointment not found")
                    );

            if (!appointment.getPatient().getId().equals(patient.getId())) {
                throw new com.ahmed.hospital.common.exception.BadRequestException(
                        "Appointment does not belong to this patient"
                );
            }
        }

        Invoice invoice = Invoice.builder()
                .invoiceNumber(generateInvoiceNumber())
                .patient(patient)
                .appointment(appointment)
                .status(InvoiceStatus.ISSUED)
                .notes(request.notes())
                .dueAt(request.dueAt())
                .build();

        for (CreateInvoiceItemRequest itemRequest : request.items()) {

            InvoiceItem item = buildInvoiceItem(itemRequest);

            invoice.addItem(item);
        }

        invoice.calculateTotal();

        Invoice savedInvoice = invoiceRepository.save(invoice);

        return toResponse(savedInvoice);
    }

    private InvoiceItem buildInvoiceItem(
            CreateInvoiceItemRequest request
    ) {

        validateItemSource(request);

        InvoiceItem.InvoiceItemBuilder builder = InvoiceItem.builder()
                .type(request.type())
                .description(request.description())
                .quantity(request.quantity())
                .unitPrice(request.unitPrice());

        if (request.labOrderId() != null) {

            LabOrder labOrder = labOrderRepository.findById(
                    request.labOrderId()
            ).orElseThrow(() ->
                    new RuntimeException("Lab order not found")
            );

            builder.labOrder(labOrder);
        }

        if (request.prescriptionItemId() != null) {

            PrescriptionItem prescriptionItem =
                    prescriptionItemRepository.findById(
                            request.prescriptionItemId()
                    ).orElseThrow(() ->
                            new RuntimeException(
                                    "Prescription item not found"
                            )
                    );

            builder.prescriptionItem(prescriptionItem);
        }

        InvoiceItem item = builder.build();

        item.setTotalPrice(
                request.quantity()
                        .multiply(request.unitPrice())
        );

        return item;
    }

    private void validateItemSource(
            CreateInvoiceItemRequest request
    ) {

        if (request.type() == InvoiceItemType.LAB
                && request.labOrderId() == null) {

            throw new com.ahmed.hospital.common.exception.BadRequestException(
                    "LAB invoice item must have a lab order"
            );
        }

        if (request.type() == InvoiceItemType.MEDICATION
                && request.prescriptionItemId() == null) {

            throw new com.ahmed.hospital.common.exception.BadRequestException(
                    "MEDICATION invoice item must have a prescription item"
            );
        }

        if (request.type() == InvoiceItemType.CONSULTATION
                && request.labOrderId() != null) {

            throw new com.ahmed.hospital.common.exception.BadRequestException(
                    "CONSULTATION cannot be linked to a lab order"
            );
        }
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoice(Long invoiceId) {

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() ->
                        new com.ahmed.hospital.common.exception.ResourceNotFoundException("Invoice not found")
                );

        return toResponse(invoice);
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> getPatientInvoices(Long patientId) {

        return invoiceRepository
                .findByPatientIdOrderByIssuedAtDesc(patientId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private String generateInvoiceNumber() {

        return "INV-" +
                UUID.randomUUID()
                        .toString()
                        .substring(0, 8)
                        .toUpperCase();
    }

    private InvoiceResponse toResponse(Invoice invoice) {

        List<InvoiceItemResponse> items =
                invoice.getItems()
                        .stream()
                        .map(item -> new InvoiceItemResponse(
                                item.getId(),
                                item.getType(),
                                item.getDescription(),
                                item.getQuantity(),
                                item.getUnitPrice(),
                                item.getTotalPrice(),
                                item.getLabOrder() != null
                                        ? item.getLabOrder().getId()
                                        : null,
                                item.getPrescriptionItem() != null
                                        ? item.getPrescriptionItem().getId()
                                        : null
                        ))
                        .toList();

        return new InvoiceResponse(
                invoice.getId(),
                invoice.getInvoiceNumber(),
                invoice.getPatient().getId(),
                invoice.getAppointment() != null
                        ? invoice.getAppointment().getId()
                        : null,
                invoice.getStatus(),
                invoice.getTotalAmount(),
                invoice.getNotes(),
                invoice.getIssuedAt(),
                invoice.getDueAt(),
                items
        );
    }
}