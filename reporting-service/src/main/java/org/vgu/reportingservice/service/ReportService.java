package org.vgu.reportingservice.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vgu.reportingservice.dto.ReportRequest;
import org.vgu.reportingservice.dto.ReportResponse;
import org.vgu.reportingservice.enums.ReportStatus;
import org.vgu.reportingservice.enums.ReportType;
import org.vgu.reportingservice.model.Report;
import org.vgu.reportingservice.repository.ReportRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final ReportGeneratorService reportGeneratorService;

    @Transactional
    public ReportResponse generateReport(ReportRequest request, String username) {
        log.info("Creating report request: type={}, format={}, user={}",
                request.getType(), request.getFormat(), username);

        Report report = Report.builder()
                .name(request.getName() != null ? request.getName() : generateReportName(request.getType()))
                .type(request.getType())
                .format(request.getFormat())
                .status(ReportStatus.PENDING)
                .createdBy(username)
                .createdAt(LocalDateTime.now())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .filters(request.getFilters())
                .emailRecipients(
                        request.getEmailRecipients() != null ? String.join(",", request.getEmailRecipients()) : null)
                .scheduled(false)
                .build();

        report = reportRepository.save(report);
        log.info("Report request created with ID: {}", report.getId());

        // Generate report asynchronously
        final Long reportId = report.getId();
        reportGeneratorService.generateReportAsync(reportId);

        return convertToResponse(report);
    }

    public ReportResponse getReport(Long id) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Report not found with id: " + id));
        return convertToResponse(report);
    }

    public List<ReportResponse> getAllReports() {
        return reportRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<ReportResponse> getReportsByUser(String username) {
        return reportRepository.findByCreatedByOrderByCreatedAtDesc(username).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<ReportResponse> getReportsByStatus(ReportStatus status) {
        return reportRepository.findByStatusOrderByCreatedAtDesc(status).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<ReportResponse> getReportsByType(ReportType type) {
        return reportRepository.findByTypeOrderByCreatedAtDesc(type).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void cancelReport(Long id) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Report not found with id: " + id));

        if (report.getStatus() == ReportStatus.PENDING || report.getStatus() == ReportStatus.PROCESSING) {
            report.setStatus(ReportStatus.CANCELLED);
            reportRepository.save(report);
            log.info("Report {} cancelled", id);
        } else {
            throw new RuntimeException("Cannot cancel report in status: " + report.getStatus());
        }
    }

    @Transactional
    public void deleteReport(Long id) {
        reportRepository.deleteById(id);
        log.info("Report {} deleted", id);
    }

    private String generateReportName(ReportType type) {
        return type.name() + "_" + LocalDateTime.now().toString().replace(":", "-");
    }

    private ReportResponse convertToResponse(Report report) {
        return ReportResponse.builder()
                .id(report.getId())
                .name(report.getName())
                .type(report.getType())
                .format(report.getFormat())
                .status(report.getStatus())
                .filePath(report.getFilePath())
                .fileSize(report.getFileSize())
                .createdBy(report.getCreatedBy())
                .createdAt(report.getCreatedAt())
                .completedAt(report.getCompletedAt())
                .errorMessage(report.getErrorMessage())
                .startDate(report.getStartDate())
                .endDate(report.getEndDate())
                .build();
    }
}
