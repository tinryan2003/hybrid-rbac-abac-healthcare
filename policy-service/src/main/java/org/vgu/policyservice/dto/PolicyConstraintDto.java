package org.vgu.policyservice.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for policy constraints (ABAC component)
 * Used by frontend to build user-friendly constraint forms
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PolicyConstraintDto {

    // Time constraints
    private String timeRange; // e.g., "08:00-17:00"
    private Boolean workingHoursOnly; // true = 8:00-17:00

    // Department constraints
    private Boolean sameDepartment; // User and resource must be in same dept
    private List<String> allowedDepartments; // Specific department IDs

    // Hospital constraints
    private Boolean sameHospital; // User and resource must be in same hospital
    private List<String> allowedHospitals; // Specific hospital IDs

    // Position level constraints
    private Integer minPositionLevel; // Minimum position level required
    private Integer maxPositionLevel; // Maximum position level allowed

    // Emergency override
    private Boolean allowEmergencyOverride; // Allow access in emergency situations

    // IP-based constraints
    private List<String> allowedIpRanges; // IP CIDR ranges

    // Ownership constraints
    private Boolean ownerOnly; // User must own the resource

    // Custom attributes (key-value pairs)
    private java.util.Map<String, Object> customAttributes;
}
