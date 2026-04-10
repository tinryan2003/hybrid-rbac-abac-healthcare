package org.vgu.labservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabOrderResponse {
    private Long labOrderId;
    private Long patientId;
    private Long doctorId;
    private Long appointmentId;
    private LocalDateTime orderDate;
    private String orderType;
    private String clinicalDiagnosis;
    private String clinicalNotes;
    private String urgency;
    private String status;
    private LocalDateTime specimenCollectedAt;
    private String specimenCollectedByKeycloakId;
    private Long processedByLabTechId;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String hospitalId;
    private Long departmentId;
    private String sensitivityLevel;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<LabOrderItemResponse> orderItems;
}
