package org.vgu.appointmentservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Entity
@Table(name = "appointment_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    private Long historyId;

    @Column(name = "appointment_id", nullable = false)
    private Long appointmentId;

    @Column(name = "changed_by_keycloak_id", length = 255)
    private String changedByKeycloakId;

    @Column(name = "action", columnDefinition = "ENUM('CREATED', 'CONFIRMED', 'CANCELLED', 'RESCHEDULED', 'COMPLETED')", nullable = false)
    private String action;

    @Column(name = "previous_status", length = 20)
    private String previousStatus;

    @Column(name = "new_status", length = 20)
    private String newStatus;

    @Column(name = "previous_date")
    private LocalDate previousDate;

    @Column(name = "previous_time")
    private LocalTime previousTime;

    @Column(name = "new_date")
    private LocalDate newDate;

    @Column(name = "new_time")
    private LocalTime newTime;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "changed_at")
    private LocalDateTime changedAt;

    @PrePersist
    protected void onCreate() {
        changedAt = LocalDateTime.now();
    }
}
