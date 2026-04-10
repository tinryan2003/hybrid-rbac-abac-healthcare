package org.vgu.auditservice.enums;

public enum ResourceType {
    ACCOUNT,
    TRANSACTION,
    CUSTOMER,
    EMPLOYEE,
    BRANCH,
    POLICY,
    TRUST_SCORE,
    SYSTEM,

    // Hospital domain resource types (from Gateway PEP)
    PATIENT_RECORD,
    APPOINTMENT,
    PRESCRIPTION,
    LAB_ORDER,
    BILLING,
    USER,
    AUDIT_LOG,
    POLICY_MANAGEMENT,

    // Fallback for unknown types
    UNKNOWN
}
