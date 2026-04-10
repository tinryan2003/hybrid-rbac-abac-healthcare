package org.vgu.billingservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceRequest {
    @NotNull(message = "Patient ID is required")
    private Long patientId;

    private Long appointmentId;
    private LocalDate invoiceDate;
    private LocalDate dueDate;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private String insuranceCompany;
    private String insurancePolicyNumber;
    private BigDecimal insuranceCoverageAmount;
    private String notes;
    private String internalNotes;
    private String hospitalId;
    private String status; // PENDING, PAID, PARTIALLY_PAID, CANCELLED, REFUNDED

    @NotNull(message = "At least one item is required")
    private List<InvoiceItemRequest> items;
}
