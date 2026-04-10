package org.vgu.billingservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vgu.billingservice.dto.*;
import org.vgu.billingservice.exception.BillingNotFoundException;
import org.vgu.billingservice.model.Invoice;
import org.vgu.billingservice.model.InvoiceItem;
import org.vgu.billingservice.repository.InvoiceItemRepository;
import org.vgu.billingservice.repository.InvoiceRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;

    public InvoiceResponse getInvoiceById(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new BillingNotFoundException("Invoice not found with ID: " + invoiceId));
        return mapToInvoiceResponse(invoice);
    }

    /** PIP: Resource attributes for ABAC (owner_id, hospital_id, status, sensitivity_level, created_by). */
    public Optional<Map<String, Object>> getPipResourceAttributes(Long resourceId) {
        return invoiceRepository.findById(resourceId).map(inv -> {
            Map<String, Object> attrs = new HashMap<>();
            attrs.put("owner_id", String.valueOf(inv.getPatientId()));
            if (inv.getHospitalId() != null) attrs.put("hospital_id", inv.getHospitalId());
            if (inv.getStatus() != null) attrs.put("status", inv.getStatus());
            if (inv.getCreatedByKeycloakId() != null) attrs.put("created_by", inv.getCreatedByKeycloakId());
            // Financial records with insurance data are HIGH sensitivity, others NORMAL
            boolean hasInsurance = inv.getInsuranceCompany() != null && !inv.getInsuranceCompany().isBlank();
            attrs.put("sensitivity_level", hasInsurance ? "HIGH" : "NORMAL");
            // Amount as resource attribute for ABAC policies (e.g. high-value invoice requires approval)
            if (inv.getTotalAmount() != null) attrs.put("amount", inv.getTotalAmount().toString());
            return attrs;
        });
    }

    public InvoiceResponse getInvoiceByInvoiceNumber(String invoiceNumber) {
        Invoice invoice = invoiceRepository.findByInvoiceNumber(invoiceNumber)
                .orElseThrow(() -> new BillingNotFoundException("Invoice not found with number: " + invoiceNumber));
        return mapToInvoiceResponse(invoice);
    }

    public List<InvoiceResponse> getInvoicesByPatient(Long patientId) {
        return invoiceRepository.findByPatientId(patientId).stream()
                .map(this::mapToInvoiceResponse)
                .collect(Collectors.toList());
    }

    public List<InvoiceResponse> getInvoicesByStatus(String status) {
        return invoiceRepository.findByStatus(status).stream()
                .map(this::mapToInvoiceResponse)
                .collect(Collectors.toList());
    }

    public List<InvoiceResponse> getInvoicesByHospital(String hospitalId) {
        return invoiceRepository.findByHospitalId(hospitalId).stream()
                .map(this::mapToInvoiceResponse)
                .collect(Collectors.toList());
    }

    public List<InvoiceResponse> getAllInvoices() {
        return invoiceRepository.findAll().stream()
                .map(this::mapToInvoiceResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public InvoiceResponse createInvoice(InvoiceRequest request, String keycloakUserId) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(generateInvoiceNumber());
        invoice.setPatientId(request.getPatientId());
        invoice.setAppointmentId(request.getAppointmentId());
        invoice.setInvoiceDate(request.getInvoiceDate() != null ? request.getInvoiceDate() : LocalDate.now());
        invoice.setDueDate(request.getDueDate());
        invoice.setStatus(request.getStatus() != null ? request.getStatus() : "PENDING");
        invoice.setHospitalId(request.getHospitalId() != null ? request.getHospitalId() : "HOSPITAL_A");
        invoice.setCreatedByKeycloakId(keycloakUserId);
        invoice.setInsuranceCompany(request.getInsuranceCompany());
        invoice.setInsurancePolicyNumber(request.getInsurancePolicyNumber());
        invoice.setInsuranceCoverageAmount(request.getInsuranceCoverageAmount() != null ? request.getInsuranceCoverageAmount() : BigDecimal.ZERO);
        invoice.setNotes(request.getNotes());
        invoice.setInternalNotes(request.getInternalNotes());

        // Calculate totals from items
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalDiscount = request.getDiscountAmount() != null ? request.getDiscountAmount() : BigDecimal.ZERO;
        BigDecimal totalTax = request.getTaxAmount() != null ? request.getTaxAmount() : BigDecimal.ZERO;

        Invoice savedInvoice = invoiceRepository.save(invoice);

        // Create invoice items
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            for (InvoiceItemRequest itemRequest : request.getItems()) {
                InvoiceItem item = new InvoiceItem();
                item.setInvoice(savedInvoice);
                item.setItemType(itemRequest.getItemType());
                item.setItemCode(itemRequest.getItemCode());
                item.setItemDescription(itemRequest.getItemDescription());
                item.setReferenceId(itemRequest.getReferenceId());
                item.setReferenceType(itemRequest.getReferenceType());
                item.setQuantity(itemRequest.getQuantity() != null ? itemRequest.getQuantity() : 1);
                item.setUnitPrice(itemRequest.getUnitPrice());
                item.setDiscountPercent(itemRequest.getDiscountPercent() != null ? itemRequest.getDiscountPercent() : BigDecimal.ZERO);
                item.setDiscountAmount(itemRequest.getDiscountAmount() != null ? itemRequest.getDiscountAmount() : BigDecimal.ZERO);
                item.setTaxPercent(itemRequest.getTaxPercent() != null ? itemRequest.getTaxPercent() : BigDecimal.ZERO);
                item.setTaxAmount(itemRequest.getTaxAmount() != null ? itemRequest.getTaxAmount() : BigDecimal.ZERO);
                item.setProviderId(itemRequest.getProviderId());

                // Calculate item total
                BigDecimal itemSubtotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                BigDecimal itemDiscount = item.getDiscountAmount();
                BigDecimal itemTax = item.getTaxAmount();
                BigDecimal itemTotal = itemSubtotal.subtract(itemDiscount).add(itemTax);
                item.setTotalPrice(itemTotal);

                subtotal = subtotal.add(itemSubtotal);
                totalDiscount = totalDiscount.add(itemDiscount);
                totalTax = totalTax.add(itemTax);

                invoiceItemRepository.save(item);
            }
        }

        // Update invoice totals
        savedInvoice.setSubtotal(subtotal);
        savedInvoice.setDiscountAmount(totalDiscount);
        savedInvoice.setTaxAmount(totalTax);
        BigDecimal totalAmount = subtotal.subtract(totalDiscount).add(totalTax);
        savedInvoice.setTotalAmount(totalAmount);
        savedInvoice.setOutstandingAmount(totalAmount);
        savedInvoice.setPaidAmount(BigDecimal.ZERO);

        Invoice updatedInvoice = invoiceRepository.save(savedInvoice);
        return mapToInvoiceResponse(updatedInvoice);
    }

    @Transactional
    public InvoiceResponse updateInvoiceStatus(Long invoiceId, String status) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new BillingNotFoundException("Invoice not found with ID: " + invoiceId));

        invoice.setStatus(status);
        if ("PAID".equals(status) && invoice.getPaidDate() == null) {
            invoice.setPaidDate(LocalDateTime.now());
            invoice.setPaidAmount(invoice.getTotalAmount());
            invoice.setOutstandingAmount(BigDecimal.ZERO);
        }

        Invoice updatedInvoice = invoiceRepository.save(invoice);
        return mapToInvoiceResponse(updatedInvoice);
    }

    private String generateInvoiceNumber() {
        return "INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase() + "-" + System.currentTimeMillis() % 10000;
    }

    private InvoiceResponse mapToInvoiceResponse(Invoice invoice) {
        List<InvoiceItemResponse> items = invoice.getItems() != null
                ? invoice.getItems().stream().map(this::mapToItemResponse).collect(Collectors.toList())
                : List.of();

        return InvoiceResponse.builder()
                .invoiceId(invoice.getInvoiceId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .patientId(invoice.getPatientId())
                .appointmentId(invoice.getAppointmentId())
                .invoiceDate(invoice.getInvoiceDate())
                .dueDate(invoice.getDueDate())
                .subtotal(invoice.getSubtotal())
                .discountAmount(invoice.getDiscountAmount())
                .taxAmount(invoice.getTaxAmount())
                .totalAmount(invoice.getTotalAmount())
                .status(invoice.getStatus())
                .paidAmount(invoice.getPaidAmount())
                .outstandingAmount(invoice.getOutstandingAmount())
                .paidDate(invoice.getPaidDate())
                .insuranceCompany(invoice.getInsuranceCompany())
                .insurancePolicyNumber(invoice.getInsurancePolicyNumber())
                .insuranceCoverageAmount(invoice.getInsuranceCoverageAmount())
                .notes(invoice.getNotes())
                .internalNotes(invoice.getInternalNotes())
                .hospitalId(invoice.getHospitalId())
                .createdByKeycloakId(invoice.getCreatedByKeycloakId())
                .createdAt(invoice.getCreatedAt())
                .updatedAt(invoice.getUpdatedAt())
                .items(items)
                .build();
    }

    private InvoiceItemResponse mapToItemResponse(InvoiceItem item) {
        return InvoiceItemResponse.builder()
                .itemId(item.getItemId())
                .invoiceId(item.getInvoice().getInvoiceId())
                .itemType(item.getItemType())
                .itemCode(item.getItemCode())
                .itemDescription(item.getItemDescription())
                .referenceId(item.getReferenceId())
                .referenceType(item.getReferenceType())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .discountPercent(item.getDiscountPercent())
                .discountAmount(item.getDiscountAmount())
                .taxPercent(item.getTaxPercent())
                .taxAmount(item.getTaxAmount())
                .totalPrice(item.getTotalPrice())
                .providerId(item.getProviderId())
                .createdAt(item.getCreatedAt())
                .build();
    }
}
