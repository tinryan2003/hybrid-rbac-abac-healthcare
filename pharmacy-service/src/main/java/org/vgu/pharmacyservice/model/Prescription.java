package org.vgu.pharmacyservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "prescriptions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Prescription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "prescription_id")
    private Long prescriptionId;

    @Column(name = "doctor_id", nullable = false)
    private Long doctorId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "appointment_id")
    private Long appointmentId;

    @Column(name = "prescription_date", nullable = false)
    private LocalDate prescriptionDate;

    @Column(name = "diagnosis", columnDefinition = "TEXT")
    private String diagnosis;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "status", columnDefinition = "ENUM('PENDING', 'APPROVED', 'DISPENSED', 'CANCELLED')")
    private String status = "PENDING";

    @Column(name = "dispensed_by_pharmacist_id")
    private Long dispensedByPharmacistId;

    @Column(name = "dispensed_at")
    private LocalDateTime dispensedAt;

    @Column(name = "hospital_id", length = 50)
    private String hospitalId = "HOSPITAL_A";

    @Column(name = "sensitivity_level", columnDefinition = "ENUM('NORMAL', 'HIGH', 'CRITICAL')")
    private String sensitivityLevel = "NORMAL";

    @Column(name = "created_by_keycloak_id", length = 255)
    private String createdByKeycloakId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "prescription", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PrescriptionItem> items = new ArrayList<>();

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
