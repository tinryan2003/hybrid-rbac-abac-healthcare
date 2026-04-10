package org.vgu.billingservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "service_pricing")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServicePricing {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pricing_id")
    private Long pricingId;

    @Column(name = "service_code", unique = true, nullable = false, length = 50)
    private String serviceCode;

    @Column(name = "service_name", nullable = false, length = 200)
    private String serviceName;

    @Column(name = "service_category", columnDefinition = "ENUM('CONSULTATION', 'LAB', 'IMAGING', 'PROCEDURE', 'ROOM', 'OTHER')", nullable = false)
    private String serviceCategory;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "base_price", precision = 10, scale = 2, nullable = false)
    private BigDecimal basePrice;

    @Column(name = "currency", length = 3)
    private String currency = "VND";

    @Column(name = "insurance_covered")
    private Boolean insuranceCovered = false;

    @Column(name = "insurance_coverage_percent", precision = 5, scale = 2)
    private BigDecimal insuranceCoveragePercent;

    @Column(name = "department_id")
    private Long departmentId;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "effective_from")
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "hospital_id", length = 50)
    private String hospitalId;

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
