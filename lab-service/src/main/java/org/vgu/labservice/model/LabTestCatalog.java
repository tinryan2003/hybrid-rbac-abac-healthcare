package org.vgu.labservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "lab_tests_catalog")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LabTestCatalog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "test_id")
    private Long testId;

    @Column(name = "test_code", unique = true, nullable = false, length = 50)
    private String testCode;

    @Column(name = "test_name", nullable = false, length = 200)
    private String testName;

    @Column(name = "test_category", length = 100)
    private String testCategory;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "specimen_type", length = 100)
    private String specimenType;

    @Column(name = "specimen_volume", length = 50)
    private String specimenVolume;

    @Column(name = "turnaround_time_hours")
    private Integer turnaroundTimeHours;

    @Column(name = "requires_fasting")
    private Boolean requiresFasting = false;

    @Column(name = "special_preparation", columnDefinition = "TEXT")
    private String specialPreparation;

    @Column(name = "price", precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
