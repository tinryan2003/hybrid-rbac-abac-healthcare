package org.vgu.patientservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicalHistoryResponse {
    private Long id;
    private Long patientId;
    private Float bloodPressure;
    private Float bloodSugar;
    private Float weight;
    private Float height;
    private String temperature;
    private String medicalPrescription;
    private LocalDateTime creationDate;
}
