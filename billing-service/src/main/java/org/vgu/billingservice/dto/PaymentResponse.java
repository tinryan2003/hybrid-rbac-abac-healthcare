package org.vgu.billingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {
    private Long paymentId;
    private Long invoiceId;
    private LocalDateTime paymentDate;
    private String paymentMethod;
    private BigDecimal amount;
    private String transactionId;
    private String paymentReference;
    private String status;
    private String cardLastFour;
    private String cardType;
    private Long receivedByBillingClerkId;
    private String receivedByKeycloakId;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
