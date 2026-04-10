package org.vgu.labservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "lab_orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LabOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lab_order_id")
    private Long labOrderId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "doctor_id", nullable = false)
    private Long doctorId;

    @Column(name = "appointment_id")
    private Long appointmentId;

    @Column(name = "order_date")
    private LocalDateTime orderDate;

    @Column(name = "order_type", columnDefinition = "ENUM('LAB', 'IMAGING', 'PATHOLOGY')")
    private String orderType = "LAB";

    @Column(name = "clinical_diagnosis", columnDefinition = "TEXT")
    private String clinicalDiagnosis;

    @Column(name = "clinical_notes", columnDefinition = "TEXT")
    private String clinicalNotes;

    @Column(name = "urgency", columnDefinition = "ENUM('ROUTINE', 'URGENT', 'STAT')")
    private String urgency = "ROUTINE";

    @Column(name = "status", columnDefinition = "ENUM('PENDING', 'COLLECTED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')")
    private String status = "PENDING";

    @Column(name = "specimen_collected_at")
    private LocalDateTime specimenCollectedAt;

    @Column(name = "specimen_collected_by_keycloak_id", length = 255)
    private String specimenCollectedByKeycloakId;

    @Column(name = "processed_by_lab_tech_id")
    private Long processedByLabTechId;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "hospital_id", length = 50)
    private String hospitalId = "HOSPITAL_A";

    @Column(name = "department_id")
    private Long departmentId;

    @Column(name = "sensitivity_level", columnDefinition = "ENUM('NORMAL', 'HIGH', 'CRITICAL')")
    private String sensitivityLevel = "NORMAL";

    @Column(name = "created_by_keycloak_id", length = 255)
    private String createdByKeycloakId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "labOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LabOrderItem> orderItems = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (orderDate == null) orderDate = LocalDateTime.now();
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
