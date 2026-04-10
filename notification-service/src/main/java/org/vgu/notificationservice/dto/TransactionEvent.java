package org.vgu.notificationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionEvent {
    private Long transactionId;
    private String transactionIdString; // Business transaction ID
    private Long customerId;
    private String customerEmail;
    private String customerName;
    private String transactionType; // DEPOSIT, WITHDRAWAL, TRANSFER
    private BigDecimal amount;
    private String currency; // VND, USD, etc.
    private String status; // PENDING, APPROVED, REJECTED, COMPLETED
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime approvedAt;
    private Long approvedByEmployeeId;
    private String eventType; // TRANSACTION_CREATED, TRANSACTION_APPROVED, TRANSACTION_REJECTED, TRANSACTION_COMPLETED
    private String rejectionReason;
    private String fromAccount;
    private String toAccount;
}

