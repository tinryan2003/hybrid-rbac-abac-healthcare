package org.vgu.auditservice.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.vgu.auditservice.enums.AuditEventType;
import org.vgu.auditservice.enums.AuditSeverity;
import org.vgu.auditservice.enums.ResourceType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_event_type", columnList = "event_type"),
        @Index(name = "idx_employee_number", columnList = "employee_number"),
        @Index(name = "idx_email", columnList = "email"),
        @Index(name = "idx_keycloak_id", columnList = "keycloak_id"),
        @Index(name = "idx_resource_type", columnList = "resource_type"),
        @Index(name = "idx_resource_id", columnList = "resource_id"),
        @Index(name = "idx_timestamp", columnList = "timestamp"),
        @Index(name = "idx_severity", columnList = "severity")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private AuditEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    @Builder.Default
    private AuditSeverity severity = AuditSeverity.MEDIUM;

    @Column(name = "employee_number", length = 50)
    private String employeeNumber;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "keycloak_id", length = 100)
    private String keycloakId;

    @Column(name = "user_role", length = 50)
    private String userRole; // RBAC: Role from Keycloak JWT
    
    @Column(name = "job_title", length = 100)
    private String jobTitle; // ABAC: JobTitleType enum value (e.g., "HR_MANAGER", "HR_SPECIALIST")
    
    // Legacy fields (kept for backward compatibility)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "username", length = 100)
    private String username;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", length = 50)
    private ResourceType resourceType;

    @Column(name = "resource_id")
    private Long resourceId;

    @Column(name = "action", nullable = false, length = 100)
    private String action;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "success", nullable = false)
    @Builder.Default
    private Boolean success = true;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "metadata", columnDefinition = "JSON")
    private String metadata; // JSON string for additional data

    @Column(name = "before_state", columnDefinition = "JSON")
    private String beforeState; // State before the action

    @Column(name = "after_state", columnDefinition = "JSON")
    private String afterState; // State after the action

    @CreationTimestamp
    @Column(name = "timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @Column(name = "session_id", length = 100)
    private String sessionId;

    @Column(name = "correlation_id", length = 100)
    private String correlationId; // For tracing related events

    // Tamper-evident logging: Hash chain (like blockchain)
    @Column(name = "prev_hash", length = 128)
    private String prevHash; // Hash of the previous log entry

    @Column(name = "curr_hash", length = 128)
    private String currHash; // SHA-256 hash of current entry (data + prevHash)
}
