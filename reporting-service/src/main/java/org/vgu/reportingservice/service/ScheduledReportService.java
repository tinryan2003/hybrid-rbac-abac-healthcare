package org.vgu.reportingservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.vgu.reportingservice.dto.ReportRequest;
import org.vgu.reportingservice.enums.ReportFormat;
import org.vgu.reportingservice.enums.ReportType;

import java.time.LocalDateTime;

/**
 * Generates scheduled reports automatically.
 * Times are configurable in application.yml under reporting.scheduled.
 * All scheduled reports are generated as CSV for easy consumption.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledReportService {

    private final ReportService reportService;

    /** Daily summary: runs every day at 01:00 AM. */
    @Scheduled(cron = "${reporting.scheduled.daily-report:0 0 1 * * ?}")
    public void generateDailyReport() {
        log.info("Generating daily summary report...");
        try {
            LocalDateTime now = LocalDateTime.now();
            ReportRequest request = ReportRequest.builder()
                    .type(ReportType.DAILY_SUMMARY)
                    .format(ReportFormat.CSV)
                    .name("Daily_Summary_" + now.toLocalDate())
                    .startDate(now.toLocalDate().atStartOfDay().minusDays(1))
                    .endDate(now.toLocalDate().atStartOfDay())
                    .build();
            reportService.generateReport(request, "system-scheduler");
            log.info("Daily summary report queued successfully.");
        } catch (Exception e) {
            log.error("Failed to queue daily summary report: {}", e.getMessage(), e);
        }
    }

    /** Weekly summary: runs every Sunday at 02:00 AM. */
    @Scheduled(cron = "${reporting.scheduled.weekly-report:0 0 2 * * SUN}")
    public void generateWeeklyReport() {
        log.info("Generating weekly summary report...");
        try {
            LocalDateTime now = LocalDateTime.now();
            ReportRequest request = ReportRequest.builder()
                    .type(ReportType.WEEKLY_SUMMARY)
                    .format(ReportFormat.EXCEL)
                    .name("Weekly_Summary_" + now.toLocalDate())
                    .startDate(now.minusWeeks(1))
                    .endDate(now)
                    .build();
            reportService.generateReport(request, "system-scheduler");
            log.info("Weekly summary report queued successfully.");
        } catch (Exception e) {
            log.error("Failed to queue weekly summary report: {}", e.getMessage(), e);
        }
    }

    /** Monthly audit trail: runs on the 1st of each month at 03:00 AM. */
    @Scheduled(cron = "${reporting.scheduled.monthly-report:0 0 3 1 * ?}")
    public void generateMonthlyAuditReport() {
        log.info("Generating monthly audit trail report...");
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime startOfLastMonth = now.withDayOfMonth(1).minusMonths(1).toLocalDate().atStartOfDay();
            LocalDateTime endOfLastMonth = now.withDayOfMonth(1).toLocalDate().atStartOfDay().minusSeconds(1);

            ReportRequest request = ReportRequest.builder()
                    .type(ReportType.AUDIT_TRAIL)
                    .format(ReportFormat.CSV)
                    .name("Audit_Trail_" + startOfLastMonth.toLocalDate().getMonth() + "_" + startOfLastMonth.getYear())
                    .startDate(startOfLastMonth)
                    .endDate(endOfLastMonth)
                    .build();
            reportService.generateReport(request, "system-scheduler");
            log.info("Monthly audit trail report queued successfully.");
        } catch (Exception e) {
            log.error("Failed to queue monthly audit report: {}", e.getMessage(), e);
        }
    }

    /** Monthly billing report: runs on the 1st of each month at 04:00 AM. */
    @Scheduled(cron = "0 0 4 1 * ?")
    public void generateMonthlyBillingReport() {
        log.info("Generating monthly billing report...");
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime startOfLastMonth = now.withDayOfMonth(1).minusMonths(1).toLocalDate().atStartOfDay();
            LocalDateTime endOfLastMonth = now.withDayOfMonth(1).toLocalDate().atStartOfDay().minusSeconds(1);

            ReportRequest request = ReportRequest.builder()
                    .type(ReportType.BILLING_REPORT)
                    .format(ReportFormat.EXCEL)
                    .name("Billing_" + startOfLastMonth.toLocalDate().getMonth() + "_" + startOfLastMonth.getYear())
                    .startDate(startOfLastMonth)
                    .endDate(endOfLastMonth)
                    .build();
            reportService.generateReport(request, "system-scheduler");
            log.info("Monthly billing report queued successfully.");
        } catch (Exception e) {
            log.error("Failed to queue monthly billing report: {}", e.getMessage(), e);
        }
    }
}
