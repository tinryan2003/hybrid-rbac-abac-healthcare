package org.vgu.billingservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_refunds")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRefund {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "refund_id")
    private Long refundId;

    @Column(name = "payment_id", nullable = false)
    private Long paymentId;

    @Column(name = "invoice_id", nullable = false)
    private Long invoiceId;

    @Column(name = "refund_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal refundAmount;

    @Column(name = "refund_reason", columnDefinition = "TEXT")
    private String refundReason;

    @Column(name = "refund_method", columnDefinition = "ENUM('CASH', 'CREDIT_CARD', 'BANK_TRANSFER', 'OTHER')")
    private String refundMethod;

    @Column(name = "status", columnDefinition = "ENUM('PENDING', 'APPROVED', 'REJECTED', 'COMPLETED')")
    private String status = "PENDING";

    @Column(name = "requested_by_keycloak_id", length = 255)
    private String requestedByKeycloakId;

    @Column(name = "approved_by_keycloak_id", length = 255)
    private String approvedByKeycloakId;

    @Column(name = "processed_by_keycloak_id", length = 255)
    private String processedByKeycloakId;

    @Column(name = "refund_date")
    private LocalDateTime refundDate;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
