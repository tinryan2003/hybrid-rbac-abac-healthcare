package org.vgu.appointmentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentResponse {
    private Long appointmentId;
    private Long doctorId;
    private Long patientId;
    private String doctorSpecialization;
    private LocalDate appointmentDate;
    private LocalTime appointmentTime;
    private Integer durationMinutes;
    private String reason;
    private String notes;
    private String status;
    private String hospitalId;
    private Long departmentId;
    private LocalDateTime createDate;
    private LocalDateTime approveDate;
    private LocalDateTime cancelDate;
    private LocalDateTime completedDate;
    private LocalDateTime updatedAt;
    private String createdByKeycloakId;
}
