package org.vgu.labservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabResultResponse {
    private Long resultId;
    private Long labOrderId;
    private Long orderItemId;
    private Long testId;
    private String testName;
    private String resultValue;
    private String resultUnit;
    private String referenceRange;
    private String resultStatus;
    private String interpretation;
    private String flags;
    private String specimenAdequacy;
    private String comments;
    private Long performedByLabTechId;
    private Long verifiedByLabTechId;
    private Long approvedByPathologistId;
    private String sensitivityLevel;
    private LocalDateTime resultDate;
    private LocalDateTime verifiedAt;
    private LocalDateTime approvedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
