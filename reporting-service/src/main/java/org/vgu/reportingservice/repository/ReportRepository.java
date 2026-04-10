package org.vgu.reportingservice.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.vgu.reportingservice.enums.ReportStatus;
import org.vgu.reportingservice.enums.ReportType;
import org.vgu.reportingservice.model.Report;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    List<Report> findByCreatedByOrderByCreatedAtDesc(String createdBy);

    List<Report> findByStatusOrderByCreatedAtDesc(ReportStatus status);

    List<Report> findByTypeOrderByCreatedAtDesc(ReportType type);

    List<Report> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    List<Report> findByScheduledTrue();

    long countByStatus(ReportStatus status);
}
