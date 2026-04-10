package org.vgu.policyservice.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating or updating an IAM-style policy.
 *
 * Supports two modes:
 * 1. Single-rule (flat): provide top-level effect/subjects/actions/resources/conditions.
 * 2. Multi-rule: provide a rules[] array. Top-level fields are used as policy metadata
 *    (name, priority, tenantId) but effect/subjects/actions/resources/conditions are
 *    derived from each rule.
 *
 * When rules[] is present and non-empty, it takes precedence over the flat fields.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PolicyCreateUpdateRequest {

    @NotBlank(message = "tenantId is required")
    private String tenantId;

    @NotBlank(message = "policyId is required")
    private String policyId;

    @NotBlank(message = "policyName is required")
    private String policyName;

    private String description;

    /** Effect for single-rule mode. Ignored when rules[] is provided. */
    private String effect; // Allow | Deny

    /** Subjects for single-rule mode. e.g. {"roles": ["DOCTOR"]} */
    private Object subjects;

    /** Actions for single-rule mode. e.g. ["read", "update"] */
    private Object actions;

    /** Resources for single-rule mode. e.g. {"type": "patient_record"} */
    private Object resources;

    /** Conditions for single-rule mode. e.g. {"same_department": true} */
    private Object conditions;

    /**
     * Multi-rule mode: array of rules within this policy.
     * When non-empty, overrides effect/subjects/actions/resources/conditions.
     */
    private List<PolicyRuleItemDto> rules;

    /**
     * How to combine multiple rules within this policy.
     * deny-overrides (default): any Deny rule wins over Allow rules in same policy.
     * allow-overrides: any Allow rule wins (suppresses Deny rules in same policy).
     * first-applicable: use highest-priority matching rule only.
     */
    private String combiningAlgorithm;

    private Boolean enabled;

    /** Higher number = higher precedence when multiple policies match (0–1000). */
    private Integer priority;

    private Object tags;

    // Governance & Accountability Metadata
    /** Justification for creating/updating this policy (optional but recommended) */
    private String justification;

    /** Reference ticket ID (e.g., JIRA-123, INCIDENT-456) */
    private String ticketId;

    /** Business owner or responsible person for this policy */
    private String businessOwner;
}
