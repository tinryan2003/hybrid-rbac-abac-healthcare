package org.vgu.labservice.dto;

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
public class LabOrderItemResponse {
    private Long orderItemId;
    private Long labOrderId;
    private Long testId;
    private String testName;
    private String status;
    private Integer priority;
    private BigDecimal price;
    private LocalDateTime createdAt;
}
