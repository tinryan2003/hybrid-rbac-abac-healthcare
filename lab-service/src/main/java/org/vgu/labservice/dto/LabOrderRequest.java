package org.vgu.labservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabOrderRequest {
    @NotNull(message = "Patient ID is required")
    private Long patientId;

    @NotNull(message = "Doctor ID is required")
    private Long doctorId;

    private Long appointmentId;
    private String orderType; // LAB, IMAGING, PATHOLOGY
    private String clinicalDiagnosis;
    private String clinicalNotes;
    private String urgency; // ROUTINE, URGENT, STAT
    private Long departmentId;
    private String hospitalId;
    private String sensitivityLevel; // NORMAL, HIGH, CRITICAL

    @NotNull(message = "At least one test is required")
    private List<LabOrderItemRequest> items;
}
