package org.vgu.auditservice.dto;

import java.time.LocalDateTime;

import org.vgu.auditservice.enums.AuditEventType;
import org.vgu.auditservice.enums.AuditSeverity;
import org.vgu.auditservice.enums.ResourceType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogResponse {
    private Long id;
    private AuditEventType eventType;
    private AuditSeverity severity;
    private String employeeNumber;
    private String email;
    private String keycloakId;
    private String userRole; // RBAC: Role from Keycloak JWT
    private String jobTitle; // ABAC: JobTitleType enum value (e.g., "HR_MANAGER", "HR_SPECIALIST")
    private ResourceType resourceType;
    private Long resourceId;
    
    // Legacy fields (kept for backward compatibility)
    private Long userId;
    private String username;
    private String action;
    private String description;
    private String ipAddress;
    private Boolean success;
    private String failureReason;
    private String metadata;
    private LocalDateTime timestamp;
    private String correlationId;
    
    // Tamper-evident hash chain fields
    private String prevHash; // Hash of the previous log entry
    private String currHash; // SHA-256 hash of current entry
}
