package org.vgu.policyservice.dto;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing a simplified ABAC policy rule for conflict detection
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PolicyRule {
    private String policyId;
    private String policyName;
    private String effect; // "Allow" or "Deny"

    // Subject attributes (e.g., {"role": ["DOCTOR"], "department_id": ["1", "2"]})
    private Map<String, List<String>> subjectAttributes;

    // Object attributes (e.g., {"object_type": ["patient_record"], "department_id":
    // ["1"]})
    private Map<String, List<String>> objectAttributes;

    // Actions (e.g., ["read", "update", "approve"])
    private List<String> actions;

    /** Resource type for overlap check (e.g. "patient_record", "emr"). From resources.type or resources.object. */
    private String resourceType;

    // Conditions (for advanced conflict detection)
    private Map<String, Object> conditions;
}
