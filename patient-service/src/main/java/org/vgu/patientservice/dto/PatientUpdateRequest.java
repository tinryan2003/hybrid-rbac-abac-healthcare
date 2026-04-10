package org.vgu.patientservice.dto;

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
public class PatientUpdateRequest {

    @Size(max = 50)
    private String firstname;

    @Size(max = 50)
    private String lastname;

    @Size(max = 500)
    private String address;

    @Past(message = "Birthday must be in the past")
    private LocalDate birthday;

    @Size(max = 50)
    private String gender;

    @Size(max = 50)
    private String phoneNumber;

    @Size(max = 50)
    private String emergencyContact;

    @Size(max = 50)
    private String hospitalId;
}
