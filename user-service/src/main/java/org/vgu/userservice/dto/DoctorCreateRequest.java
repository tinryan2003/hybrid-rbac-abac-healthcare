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

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorCreateRequest {
    @NotBlank(message = "First name is required")
    @Size(max = 50)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 50)
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Size(max = 100)
    private String emailAddress;

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
    private String field; // Specialization

    private LocalDate birthday;

    // ABAC Attributes
    private Long departmentId;

    @Size(max = 50)
    private String hospitalId;

    @Builder.Default
    private Integer positionLevel = 2; // Default to 2 (Senior)
}
