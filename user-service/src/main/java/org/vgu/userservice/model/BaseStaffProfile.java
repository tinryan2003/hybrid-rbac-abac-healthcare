package org.vgu.userservice.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Base class for all staff role entities (Doctor, Nurse, Admin, etc.)
 * Contains common fields shared across all role profiles.
 * 
 * ABAC Attributes:
 * - hospitalId: Hospital identifier for multi-hospital support
 * - isActive: Employment status
 */
@MappedSuperclass
@Data
public abstract class BaseStaffProfile {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore // Prevent serialization of lazy-loaded User entity
    protected User user;

    @Column(name = "first_name", nullable = false, length = 50)
    protected String firstName;

    @Column(name = "last_name", nullable = false, length = 50)
    protected String lastName;

    @Column(name = "email", length = 100)
    protected String email;

    @Column(name = "phone_number", length = 20)
    protected String phoneNumber;

    // Common personal attributes
    @Enumerated(EnumType.STRING)
    @Column(name = "gender")
    protected Gender gender;

    @Column(name = "birthday")
    protected LocalDate birthday;

    /**
     * Generic position level for staff seniority.
     * For example: 1 = Junior/Staff, 2 = Senior, 3 = Head.
     * Specific roles (doctor, nurse, admin) can interpret the scale as needed.
     */
    @Column(name = "position_level")
    protected Integer positionLevel;

    // ABAC Attributes
    @Column(name = "hospital_id", length = 50)
    protected String hospitalId;

    // Employment
    @Column(name = "is_active")
    protected Boolean isActive = true;

    // Audit
    @Column(name = "created_at")
    protected LocalDateTime createdAt;

    @Column(name = "updated_at")
    protected LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
