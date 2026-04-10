package org.vgu.pharmacyservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "medicine_inventory_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedicineInventoryTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Long transactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medicine_id", nullable = false)
    private Medicine medicine;

    @Column(name = "transaction_type", nullable = false, columnDefinition = "ENUM('IN', 'OUT', 'ADJUSTMENT', 'EXPIRED')")
    private String transactionType; // IN, OUT, ADJUSTMENT, EXPIRED

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @Column(name = "performed_by_keycloak_id", length = 255)
    private String performedByKeycloakId;

    @Column(name = "hospital_id", length = 50)
    private String hospitalId;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "transaction_date")
    private LocalDateTime transactionDate;

    @PrePersist
    protected void onCreate() {
        if (transactionDate == null) transactionDate = LocalDateTime.now();
    }
}
