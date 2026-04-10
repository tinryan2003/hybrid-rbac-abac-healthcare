package org.vgu.policyservice.dto;

import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Frontend-friendly DTO for building policies
 * Provides a more structured and type-safe way to create policies
 * compared to the generic PolicyCreateUpdateRequest
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PolicyBuilderRequest {

    // Basic metadata
    @NotBlank(message = "tenantId is required")
    private String tenantId;

    @NotBlank(message = "policyId is required (use lowercase with underscores, e.g., 'nurse_working_hours')")
    private String policyId;

    @NotBlank(message = "policyName is required")
    private String policyName;

    private String description;

    // Effect: "Allow" or "Deny"
    @NotBlank(message = "effect is required (Allow or Deny)")
    private String effect;

    // Enabled flag
    @Builder.Default
    private Boolean enabled = true;

    // ===== RBAC Component: Roles =====
    @NotEmpty(message = "At least one target role is required")
    private List<String> targetRoles; // e.g., ["DOCTOR", "NURSE"]

    // ===== Actions =====
    @NotEmpty(message = "At least one action is required")
    private List<String> actions; // e.g., ["read", "update", "approve"]

    // ===== Resources =====
    @NotBlank(message = "resourceObject is required (e.g., 'patient_record')")
    private String resourceObject; // e.g., "patient_record", "medical_procedure"

    private String resourceType; // Optional additional type

    // ===== ABAC Component: Constraints =====
    private PolicyConstraintDto constraints;

    // ===== Tags for organization =====
    private List<String> tags; // e.g., ["hipaa", "cardiology", "critical"]

    /**
     * Convert to generic PolicyCreateUpdateRequest for persistence
     */
    public PolicyCreateUpdateRequest toCreateUpdateRequest() {
        PolicyCreateUpdateRequest request = new PolicyCreateUpdateRequest();
        request.setTenantId(tenantId);
        request.setPolicyId(policyId);
        request.setPolicyName(policyName);
        request.setDescription(description);
        request.setEffect(effect);
        request.setEnabled(enabled);

        // Build subjects JSON
        request.setSubjects(Map.of("roles", targetRoles));

        // Build actions
        request.setActions(actions);

        // Build resources JSON
        Map<String, Object> resources = new java.util.HashMap<>();
        resources.put("object", resourceObject);
        if (resourceType != null) {
            resources.put("type", resourceType);
        }
        request.setResources(resources);

        // Build conditions JSON from constraints
        if (constraints != null) {
            request.setConditions(buildConditionsMap());
        }

        // Tags
        if (tags != null && !tags.isEmpty()) {
            request.setTags(tags);
        }

        return request;
    }

    /**
     * Convert PolicyConstraintDto to conditions map
     */
    private Map<String, Object> buildConditionsMap() {
        Map<String, Object> conditions = new java.util.HashMap<>();

        if (constraints.getTimeRange() != null) {
            conditions.put("time_range", constraints.getTimeRange());
        }
        if (Boolean.TRUE.equals(constraints.getWorkingHoursOnly())) {
            conditions.put("working_hours_only", true);
        }
        if (Boolean.TRUE.equals(constraints.getSameDepartment())) {
            conditions.put("same_department", true);
        }
        if (constraints.getAllowedDepartments() != null && !constraints.getAllowedDepartments().isEmpty()) {
            conditions.put("allowed_departments", constraints.getAllowedDepartments());
        }
        if (Boolean.TRUE.equals(constraints.getSameHospital())) {
            conditions.put("same_hospital", true);
        }
        if (constraints.getAllowedHospitals() != null && !constraints.getAllowedHospitals().isEmpty()) {
            conditions.put("allowed_hospitals", constraints.getAllowedHospitals());
        }
        if (constraints.getMinPositionLevel() != null) {
            conditions.put("min_position_level", constraints.getMinPositionLevel());
        }
        if (constraints.getMaxPositionLevel() != null) {
            conditions.put("max_position_level", constraints.getMaxPositionLevel());
        }
        if (Boolean.TRUE.equals(constraints.getAllowEmergencyOverride())) {
            conditions.put("allow_emergency_override", true);
        }
        if (constraints.getAllowedIpRanges() != null && !constraints.getAllowedIpRanges().isEmpty()) {
            conditions.put("allowed_ip_ranges", constraints.getAllowedIpRanges());
        }
        if (Boolean.TRUE.equals(constraints.getOwnerOnly())) {
            conditions.put("owner_only", true);
        }
        if (constraints.getCustomAttributes() != null) {
            conditions.putAll(constraints.getCustomAttributes());
        }

        return conditions;
    }

    /**
     * Factory methods for common policy templates
     */
    public static PolicyBuilderRequest workingHoursTemplate(String tenantId, String rolePrefix) {
        return PolicyBuilderRequest.builder()
                .tenantId(tenantId)
                .policyId(rolePrefix.toLowerCase() + "_working_hours")
                .policyName(rolePrefix + " Working Hours Restriction")
                .effect("Allow")
                .targetRoles(List.of(rolePrefix))
                .actions(List.of("read", "update"))
                .resourceObject("patient_record")
                .constraints(new PolicyConstraintDto() {
                    {
                        setWorkingHoursOnly(true);
                        setSameDepartment(true);
                        setSameHospital(true);
                    }
                })
                .tags(List.of("time-based", rolePrefix.toLowerCase()))
                .build();
    }

    public static PolicyBuilderRequest departmentIsolationTemplate(String tenantId, String role) {
        return PolicyBuilderRequest.builder()
                .tenantId(tenantId)
                .policyId(role.toLowerCase() + "_dept_isolation")
                .policyName(role + " Department Isolation")
                .effect("Allow")
                .targetRoles(List.of(role))
                .actions(List.of("read", "update"))
                .resourceObject("patient_record")
                .constraints(new PolicyConstraintDto() {
                    {
                        setSameDepartment(true);
                        setSameHospital(true);
                    }
                })
                .tags(List.of("department-isolation", role.toLowerCase()))
                .build();
    }
}
