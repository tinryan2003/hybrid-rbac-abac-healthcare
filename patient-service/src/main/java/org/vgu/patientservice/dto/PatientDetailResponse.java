package org.vgu.patientservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientDetailResponse {
    private PatientResponse patient;
    private List<MedicalHistoryResponse> medicalHistory;
    private List<PatientAllergyResponse> allergies;
}
