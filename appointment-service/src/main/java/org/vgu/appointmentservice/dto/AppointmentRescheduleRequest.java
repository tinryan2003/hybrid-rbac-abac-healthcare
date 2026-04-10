package org.vgu.appointmentservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentRescheduleRequest {
    @NotNull(message = "New date is required")
    private LocalDate newDate;

    @NotNull(message = "New time is required")
    private LocalTime newTime;

    private String reason;
}
