package org.vgu.reportingservice.enums;

public enum ReportType {
    // Clinical reports
    PATIENT_SUMMARY,          // Patient demographics, registration stats
    APPOINTMENT_REPORT,       // Appointments by doctor/department/status
    LAB_ORDER_REPORT,         // Lab orders, turnaround times, result stats
    PRESCRIPTION_REPORT,      // Prescription & medicine usage

    // Financial reports
    BILLING_REPORT,           // Revenue, outstanding invoices, payment status

    // Compliance & security
    AUDIT_TRAIL,              // Full audit log export with tamper check
    COMPLIANCE_REPORT,        // Policy evaluation pass/fail rates
    AUTHORIZATION_DECISIONS,  // OPA allow/deny decision summary

    // Operational reports
    USER_ACTIVITY,            // Staff login & action statistics

    // Summary reports (time-based, multi-domain)
    DAILY_SUMMARY,
    WEEKLY_SUMMARY,
    MONTHLY_SUMMARY
}
