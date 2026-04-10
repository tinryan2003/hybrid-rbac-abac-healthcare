package org.vgu.userservice.dto;

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
public class DoctorResponse {
    private Long doctorId;
    private Long userId;
    private String keycloakUserId;
    private String firstName;
    private String lastName;
    /** Full name for display (firstName + " " + lastName) */
    private String name;
    private String gender;
    private String field; // Specialization
    private LocalDate birthday;
    private String emailAddress; // maps to email_address column via @AttributeOverride
    private String phoneNumber;
    
    // ABAC Attributes
    private Long departmentId;
    private String departmentName;
    private String hospitalId;
    private Integer positionLevel;
    
    // Employment
    private Boolean isActive;
    
    private LocalDateTime createdAt;
}
