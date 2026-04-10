package org.vgu.pharmacyservice.dto;

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
public class MedicineResponse {
    private Long medicineId;
    private String name;
    private String genericName;
    private String brandName;
    private String description;
    private String sideEffect;
    private String category;
    private String dosageForm;
    private String strength;
    private String unit;
    private BigDecimal unitPrice;
    private Integer stockQuantity;
    private Integer reorderLevel;
    private Boolean requiresPrescription;
    private Boolean controlledSubstance;
    private String hospitalId;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean lowStock;
}
