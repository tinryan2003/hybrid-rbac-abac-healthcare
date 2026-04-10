package org.vgu.auditservice.dto;

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
public class AuditLogRequest {
    private AuditEventType eventType;
    private AuditSeverity severity;
    private String employeeNumber;
    private String email;
    private String keycloakId;
    private String userRole;
    private ResourceType resourceType;
    private Long resourceId;
    
    // Legacy fields (kept for backward compatibility)
    private Long userId;
    private String username;
    private String action;
    private String description;
    private String ipAddress;
    private String userAgent;
    private Boolean success;
    private String failureReason;
    private String metadata;
    private String beforeState;
    private String afterState;
    private String sessionId;
    private String correlationId;
}
