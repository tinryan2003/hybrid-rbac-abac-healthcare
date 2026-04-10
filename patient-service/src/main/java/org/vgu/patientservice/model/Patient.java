package org.vgu.patientservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "patients")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Patient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "patient_id")
    private Long patientId;

    @Column(name = "firstname", nullable = false, length = 50)
    private String firstname;

    @Column(name = "lastname", nullable = false, length = 50)
    private String lastname;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "birthday", nullable = false)
    private LocalDate birthday;

    @Column(name = "gender", nullable = false, length = 50)
    private String gender;

    @Column(name = "phone_number", nullable = false, length = 50)
    private String phoneNumber;

    @Column(name = "emergency_contact", length = 50)
    private String emergencyContact;

    @Lob
    @Column(name = "photo_image", columnDefinition = "LONGBLOB")
    private byte[] photoImage;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "last_visited")
    private LocalDateTime lastVisited;

    // Link to Keycloak account
    @Column(name = "keycloak_user_id", unique = true, length = 255)
    private String keycloakUserId;

    // ABAC Attribute
    @Column(name = "hospital_id", length = 50)
    private String hospitalId = "HOSPITAL_A";

    @Column(name = "created_by_keycloak_id", length = 255)
    private String createdByKeycloakId;

    @PrePersist
    protected void onCreate() {
        if (createdDate == null) {
            createdDate = LocalDateTime.now();
        }
    }
}
