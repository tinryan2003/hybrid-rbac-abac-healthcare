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
public class AppointmentHistoryResponse {
    private Long historyId;
    private Long appointmentId;
    private String changedByKeycloakId;
    private String action;
    private String previousStatus;
    private String newStatus;
    private LocalDate previousDate;
    private LocalTime previousTime;
    private LocalDate newDate;
    private LocalTime newTime;
    private String reason;
    private String notes;
    private LocalDateTime changedAt;
}
