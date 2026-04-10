package org.vgu.reportingservice.dto;

import java.time.LocalDateTime;
import java.util.List;

import org.vgu.reportingservice.enums.ReportFormat;
import org.vgu.reportingservice.enums.ReportType;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportRequest {

    @NotNull(message = "Report type is required")
    private ReportType type;

    @NotNull(message = "Report format is required")
    private ReportFormat format;

    private String name;

    /** Start of reporting period (inclusive). Defaults to 30 days ago if absent. */
    private LocalDateTime startDate;

    /** End of reporting period (inclusive). Defaults to now if absent. */
    private LocalDateTime endDate;

    /** Optional free-form filter JSON (for backwards compatibility). */
    private String filters;

    /** Scope to a specific hospital — useful for multi-tenant deployments. */
    private String hospitalId;

    /** For appointment/lab/prescription reports: filter by specific doctor. */
    private Long doctorId;

    /** For patient/appointment/lab reports: filter by specific department. */
    private Long departmentId;

    /** Send the finished report to these email addresses. */
    private List<String> emailRecipients;
}
