package org.vgu.labservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabResultRequest {
    @NotNull(message = "Lab order ID is required")
    private Long labOrderId;

    @NotNull(message = "Order item ID is required")
    private Long orderItemId;

    @NotNull(message = "Test ID is required")
    private Long testId;

    private String resultValue;
    private String resultUnit;
    private String referenceRange;
    private String resultStatus; // NORMAL, ABNORMAL, CRITICAL, PENDING
    private String interpretation;
    private String flags;
    private String specimenAdequacy; // ADEQUATE, INADEQUATE, HEMOLYZED, CLOTTED
    private String comments;
    private Long performedByLabTechId;
    private String sensitivityLevel; // NORMAL, HIGH, CRITICAL
}
