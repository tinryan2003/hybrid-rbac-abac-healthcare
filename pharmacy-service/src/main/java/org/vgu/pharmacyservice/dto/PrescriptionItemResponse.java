package org.vgu.pharmacyservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionItemResponse {
    private Long itemId;
    private Long prescriptionId;
    private Long medicineId;
    private String medicineName;
    private String dosage;
    private String frequency;
    private Integer durationDays;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer quantity;
    private Integer quantityDispensed;
    private String instructions;
    private String beforeAfterMeal;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private LocalDateTime createdAt;
}
