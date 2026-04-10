package org.vgu.policyservice.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for conflict detection result
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConflictDetectionResult {
    private int totalPolicies;
    private int conflictCount;
    private List<ConflictPair> conflicts;
    private long detectionTimeMs;
}
