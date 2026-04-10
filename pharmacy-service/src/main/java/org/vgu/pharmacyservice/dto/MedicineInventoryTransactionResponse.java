package org.vgu.pharmacyservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicineInventoryTransactionResponse {
    private Long transactionId;
    private Long medicineId;
    private String medicineName;
    private String transactionType;
    private Integer quantity;
    private Long referenceId;
    private String referenceType;
    private String performedByKeycloakId;
    private String hospitalId;
    private String notes;
    private LocalDateTime transactionDate;
}
