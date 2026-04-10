package org.vgu.policyservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single rule within a multi-rule policy.
 *
 * A Policy can contain one or more rules. Each rule defines:
 * - WHO is affected (subjects: roles)
 * - WHAT actions are controlled
 * - ON WHAT resource
 * - UNDER WHAT conditions (ABAC constraints)
 * - Effect: Allow or Deny
 *
 * The parent Policy's combiningAlgorithm determines how multiple rules are resolved.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PolicyRuleItemDto {

    /**
     * Unique identifier within the policy (e.g. "r1", "rule-allow-read").
     * Used to build the OPA data key: {policyId}__{ruleId}
     */
    private String ruleId;

    /** Human-readable description of this rule. */
    private String ruleName;

    /** Allow or Deny */
    private String effect;

    /** WHO - e.g. {"roles": ["DOCTOR"]} */
    private Object subjects;

    /** WHAT - e.g. ["read", "update"] */
    private Object actions;

    /** ON WHAT - e.g. {"type": "patient_record"} */
    private Object resources;

    /**
     * WHEN/WHERE (ABAC conditions).
     * Same format as top-level conditions:
     * flat: {"same_department": true, "working_hours_only": true}
     * tree: {"operator":"AND","children":[{"field":"env.time","operator":"eq","value":"8:00-17:00"}]}
     */
    private Object conditions;

    /**
     * Rule-level priority (0–1000). Higher = evaluated first within the policy.
     * Relevant for first-applicable combining algorithm.
     */
    private Integer priority;
}
