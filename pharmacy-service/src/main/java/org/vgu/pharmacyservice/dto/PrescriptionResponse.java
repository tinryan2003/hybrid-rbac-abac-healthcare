package org.vgu.pharmacyservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionResponse {
    private Long prescriptionId;
    private Long doctorId;
    private Long patientId;
    private Long appointmentId;
    private LocalDate prescriptionDate;
    private String diagnosis;
    private String notes;
    private String status;
    private Long dispensedByPharmacistId;
    private LocalDateTime dispensedAt;
    private String hospitalId;
    private String sensitivityLevel;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<PrescriptionItemResponse> items;
}
