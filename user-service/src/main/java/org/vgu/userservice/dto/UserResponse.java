package org.vgu.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    private Long userId;
    private String keycloakUserId;
    private String email;
    private String phoneNumber;
    /** Optional hospital identifier for staff users (ADMIN/DOCTOR/NURSE/etc.). */
    private String hospitalId;
    /**
     * Numeric seniority level used by ABAC policies (1=Junior/Staff, 2=Senior,
     * 3=Head/System).
     */
    private Integer positionLevel;
    /** High-level job title for display (e.g. DOCTOR, NURSE, ADMIN). */
    private String jobTitle;
    private LocalDateTime createdAt;
}
