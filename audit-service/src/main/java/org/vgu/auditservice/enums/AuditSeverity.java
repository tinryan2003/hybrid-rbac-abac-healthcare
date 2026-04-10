package org.vgu.auditservice.enums;

public enum AuditSeverity {
    LOW, // Informational events
    MEDIUM, // Normal operations
    HIGH, // Important actions requiring audit
    CRITICAL // Security-related or high-risk actions
}
