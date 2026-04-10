package org.vgu.policyservice.exception;

import org.vgu.policyservice.dto.ConflictDetectionResult;

/**
 * Thrown when conflict-on-save is strict and saving the policy would introduce
 * at least one AUTH_CONFLICT. Controller returns 409 with the conflict report.
 */
public class PolicyConflictException extends RuntimeException {

    private final ConflictDetectionResult conflictReport;

    public PolicyConflictException(ConflictDetectionResult conflictReport) {
        super("Policy conflicts detected: " + conflictReport.getConflictCount() + " conflict(s). " +
                "Resolve AUTH_CONFLICT before saving.");
        this.conflictReport = conflictReport;
    }

    public ConflictDetectionResult getConflictReport() {
        return conflictReport;
    }
}
