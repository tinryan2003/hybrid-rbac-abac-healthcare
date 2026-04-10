package org.vgu.billingservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceItemRequest {
    @NotNull(message = "Item type is required")
    private String itemType; // CONSULTATION, LAB_TEST, IMAGING, PROCEDURE, MEDICATION, ROOM_CHARGE, OTHER

    private String itemCode;
    @NotNull(message = "Item description is required")
    private String itemDescription;
    private Long referenceId;
    private String referenceType;
    private Integer quantity;
    @NotNull(message = "Unit price is required")
    private BigDecimal unitPrice;
    private BigDecimal discountPercent;
    private BigDecimal discountAmount;
    private BigDecimal taxPercent;
    private BigDecimal taxAmount;
    private Long providerId;
}
