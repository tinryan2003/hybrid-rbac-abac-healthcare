package org.vgu.policyservice.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.vgu.policyservice.dto.ConflictDetectionResult;
import org.vgu.policyservice.service.ConflictDetectionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST API for ABAC policy conflict detection
 * Implements conflict detection algorithms from:
 * "Detecting Conflicts in ABAC Policies with Rule Reduction and Binary-Search
 * Techniques"
 */
@Slf4j
@RestController
@RequestMapping("/policies/conflicts")
@RequiredArgsConstructor
public class ConflictDetectionController {

    private final ConflictDetectionService conflictDetectionService;

    /**
     * Detect conflicts in all enabled policies
     * GET /api/policies/conflicts
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ConflictDetectionResult> detectAllConflicts() {
        log.info("[CONFLICT] >>> GET /policies/conflicts | detecting conflicts in all enabled policies");

        ConflictDetectionResult result = conflictDetectionService.detectConflicts();

        if (result.getConflictCount() > 0) {
            log.warn("[CONFLICT] <<< GET /policies/conflicts 200 | found {} conflicts among {} policies",
                    result.getConflictCount(), result.getTotalPolicies());
        } else {
            log.info("[CONFLICT] <<< GET /policies/conflicts 200 | no conflicts among {} policies", result.getTotalPolicies());
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Detect conflicts in a specific set of policies
     * POST /api/policies/conflicts/detect
     * Body: { "policyIds": ["policy-001", "policy-002"] }
     */
    @PostMapping("/detect")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ConflictDetectionResult> detectConflictsInPolicies(
            @RequestBody DetectConflictsRequest request) {

        log.info("[CONFLICT] >>> POST /policies/conflicts/detect | policyIds={}", request.policyIds().size());

        ConflictDetectionResult result = conflictDetectionService
                .detectConflicts(request.policyIds());

        if (result.getConflictCount() > 0) {
            log.warn("[CONFLICT] <<< POST /policies/conflicts/detect 200 | {} conflicts among {} policies",
                    result.getConflictCount(), result.getTotalPolicies());
        } else {
            log.info("[CONFLICT] <<< POST /policies/conflicts/detect 200 | no conflicts among {} policies", result.getTotalPolicies());
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Request DTO for conflict detection
     */
    public record DetectConflictsRequest(List<String> policyIds) {
    }
}
