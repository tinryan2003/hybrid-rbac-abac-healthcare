package org.vgu.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Generic employee creation request for roles without dedicated entities
 * (RECEPTIONIST, LAB_TECH, PHARMACIST, BILLING_CLERK, MANAGER)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GenericEmployeeCreateRequest {
    @NotBlank(message = "Role is required")
    @Size(max = 50)
    private String role; // RECEPTIONIST, LAB_TECH, PHARMACIST, BILLING_CLERK, MANAGER

    @NotBlank(message = "First name is required")
    @Size(max = 50)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 50)
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Size(max = 100)
    private String email;

    @NotBlank(message = "Phone number is required")
    @Size(max = 20)
    private String phoneNumber;

    @NotBlank(message = "Username is required")
    @Size(max = 50)
    private String username; // For Keycloak

    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password; // Optional - if not provided, temporary password will be generated

    private String gender; // Male, Female, Other

    @Size(max = 100)
    private String field; // Doctor specialization (used when role=DOCTOR)

    private LocalDate birthday;

    // ABAC Attributes
    private Long departmentId;

    @Size(max = 50)
    private String hospitalId;

    @Builder.Default
    private Integer positionLevel = 1; // Default to 1

    /** For ADMIN role: SYSTEM, HOSPITAL, DEPARTMENT */
    @Size(max = 20)
    private String adminLevel;
}
