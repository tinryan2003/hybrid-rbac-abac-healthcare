package org.vgu.policyservice.dto;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing a pair of conflicting policies.
 * Aligned with baseline spec: AUTH_CONFLICT when effects differ and rules overlap;
 * witnessRequest is a sample request that would match both rules (for debugging).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConflictPair {
    private String policyId1;
    private String policyId2;
    private String reason;
    private String conflictType; // "EXCEPTION", "ASSOCIATION", "AUTH_CONFLICT", etc.

    /** Overlapping actions (subset of actions that trigger the conflict). */
    private List<String> overlappingActions;

    /** Resource type on which both rules apply. */
    private String resourceType;

    /** Witness request: attribute -> value that satisfies both rules (for debugging). */
    private Map<String, Object> witnessRequest;
}
