package org.vgu.labservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "lab_results")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LabResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "result_id")
    private Long resultId;

    @Column(name = "lab_order_id", nullable = false)
    private Long labOrderId;

    @Column(name = "order_item_id", nullable = false)
    private Long orderItemId;

    @Column(name = "test_id", nullable = false)
    private Long testId;

    @Column(name = "result_value", length = 500)
    private String resultValue;

    @Column(name = "result_unit", length = 50)
    private String resultUnit;

    @Column(name = "reference_range", length = 200)
    private String referenceRange;

    @Column(name = "result_status", columnDefinition = "ENUM('NORMAL', 'ABNORMAL', 'CRITICAL', 'PENDING')")
    private String resultStatus = "PENDING";

    @Column(name = "interpretation", columnDefinition = "TEXT")
    private String interpretation;

    @Column(name = "flags", length = 50)
    private String flags;

    @Column(name = "specimen_adequacy", columnDefinition = "ENUM('ADEQUATE', 'INADEQUATE', 'HEMOLYZED', 'CLOTTED')")
    private String specimenAdequacy;

    @Column(name = "comments", columnDefinition = "TEXT")
    private String comments;

    @Column(name = "performed_by_lab_tech_id")
    private Long performedByLabTechId;

    @Column(name = "verified_by_lab_tech_id")
    private Long verifiedByLabTechId;

    @Column(name = "approved_by_pathologist_id")
    private Long approvedByPathologistId;

    @Column(name = "sensitivity_level", columnDefinition = "ENUM('NORMAL', 'HIGH', 'CRITICAL')")
    private String sensitivityLevel = "NORMAL";

    @Column(name = "result_date")
    private LocalDateTime resultDate;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (resultDate == null) resultDate = LocalDateTime.now();
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
