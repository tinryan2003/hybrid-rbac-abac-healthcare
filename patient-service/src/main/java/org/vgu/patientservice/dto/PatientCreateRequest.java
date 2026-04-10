package org.vgu.patientservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientCreateRequest {

    @NotBlank(message = "First name is required")
    @Size(max = 50)
    private String firstname;

    @NotBlank(message = "Last name is required")
    @Size(max = 50)
    private String lastname;

    @Size(max = 500)
    private String address;

    @NotNull(message = "Birthday is required")
    @Past(message = "Birthday must be in the past")
    private LocalDate birthday;

    @NotBlank(message = "Gender is required")
    @Size(max = 50)
    private String gender;

    @NotBlank(message = "Phone number is required")
    @Size(max = 50)
    private String phoneNumber;

    @Size(max = 50)
    private String emergencyContact;

    /** Email for Keycloak user creation (optional if keycloakUserId is provided) */
    @jakarta.validation.constraints.Email(message = "Email should be valid")
    @Size(max = 100)
    private String email;

    /** Username for Keycloak (optional if keycloakUserId is provided) */
    @Size(max = 50)
    private String username;

    /** Password for Keycloak (optional - temporary password will be generated if not provided) */
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    /** Keycloak user ID when linking to an existing Keycloak account (e.g. PATIENT role). */
    @Size(max = 255)
    private String keycloakUserId;

    /** ABAC: hospital ID for multi-hospital support. Default from JWT or config. */
    @Size(max = 50)
    private String hospitalId;
}
