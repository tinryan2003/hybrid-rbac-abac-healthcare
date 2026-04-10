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
public class PatientResponse {
    private Long patientId;
    private String firstname;
    private String lastname;
    private String address;
    private LocalDate birthday;
    private String gender;
    private String phoneNumber;
    private String emergencyContact;
    private LocalDateTime createdDate;
    private LocalDateTime lastVisited;
    private String keycloakUserId;
    private String hospitalId;
    private Integer age; // Calculated field
}
