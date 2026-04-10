package org.vgu.billingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceResponse {
    private Long invoiceId;
    private String invoiceNumber;
    private Long patientId;
    private Long appointmentId;
    private LocalDate invoiceDate;
    private LocalDate dueDate;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private String status;
    private BigDecimal paidAmount;
    private BigDecimal outstandingAmount;
    private LocalDateTime paidDate;
    private String insuranceCompany;
    private String insurancePolicyNumber;
    private BigDecimal insuranceCoverageAmount;
    private String notes;
    private String internalNotes;
    private String hospitalId;
    private String createdByKeycloakId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<InvoiceItemResponse> items;
}
