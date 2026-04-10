package org.vgu.appointmentservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Entity
@Table(name = "appointments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Appointment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "appointment_id")
    private Long appointmentId;

    @Column(name = "doctor_id", nullable = false)
    private Long doctorId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "doctor_specialization", length = 100)
    private String doctorSpecialization;

    @Column(name = "appointment_date", nullable = false)
    private LocalDate appointmentDate;

    @Column(name = "appointment_time", nullable = false)
    private LocalTime appointmentTime;

    @Column(name = "duration_minutes")
    private Integer durationMinutes = 30;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "status", columnDefinition = "ENUM('PENDING', 'CONFIRMED', 'CANCELLED', 'COMPLETED', 'NO_SHOW')")
    private String status = "PENDING";

    @Column(name = "hospital_id", length = 50)
    private String hospitalId = "HOSPITAL_A";

    @Column(name = "department_id")
    private Long departmentId;

    @Column(name = "create_date")
    private LocalDateTime createDate;

    @Column(name = "approve_date")
    private LocalDateTime approveDate;

    @Column(name = "cancel_date")
    private LocalDateTime cancelDate;

    @Column(name = "completed_date")
    private LocalDateTime completedDate;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by_keycloak_id", length = 255)
    private String createdByKeycloakId;

    @PrePersist
    protected void onCreate() {
        createDate = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
