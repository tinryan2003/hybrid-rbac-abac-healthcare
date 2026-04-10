package org.vgu.labservice.dto;

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
public class LabOrderItemRequest {
    @NotNull(message = "Test ID is required")
    private Long testId;

    private Integer priority;
    private BigDecimal price;
}
