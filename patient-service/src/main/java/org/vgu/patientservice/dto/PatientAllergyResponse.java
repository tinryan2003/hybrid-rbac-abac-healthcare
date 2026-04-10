package org.vgu.patientservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientAllergyResponse {
    private Long allergyId;
    private Long patientId;
    private String allergen;
    private String severity;
    private String reaction;
    private LocalDate diagnosedDate;
    private LocalDateTime createdAt;
}
