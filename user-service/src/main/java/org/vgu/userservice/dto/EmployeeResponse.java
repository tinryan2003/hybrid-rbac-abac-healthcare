package org.vgu.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Unified response for any employee (doctor, nurse, admin) - excludes patients.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeResponse {
    /** Role type: DOCTOR, NURSE, ADMIN */
    private String role;

    /** Entity ID: doctorId, nurseId, or adminId */
    private Long entityId;
    private Long userId;
    private String keycloakUserId;

    private String firstName;
    private String lastName;
    /** Full name for display (firstName + " " + lastName) */
    private String name;
    private String email;
    private String phoneNumber;
    private String gender;
    private LocalDate birthday;

    // ABAC / context
    private Long departmentId;
    private String departmentName;
    private String hospitalId;
    private Integer positionLevel;

    // Role-specific (null when not applicable)
    private String field;           // Doctor specialization
    private String adminLevel;     // Admin: SYSTEM, HOSPITAL, DEPARTMENT

    private Boolean isActive;
    private LocalDateTime createdAt;
}
