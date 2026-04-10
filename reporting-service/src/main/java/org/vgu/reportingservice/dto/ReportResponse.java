package org.vgu.reportingservice.dto;

import java.time.LocalDateTime;

import org.vgu.reportingservice.enums.ReportFormat;
import org.vgu.reportingservice.enums.ReportStatus;
import org.vgu.reportingservice.enums.ReportType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportResponse {
    private Long id;
    private String name;
    private ReportType type;
    private ReportFormat format;
    private ReportStatus status;
    private String filePath;
    private Long fileSize;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private String errorMessage;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
