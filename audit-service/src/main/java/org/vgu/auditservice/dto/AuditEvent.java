package org.vgu.auditservice.dto;

import java.time.LocalDateTime;

import org.vgu.auditservice.enums.AuditEventType;
import org.vgu.auditservice.enums.ResourceType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for audit events received from RabbitMQ
 * Note: Authorization Service sends "reason" field, but we map it to "failureReason"
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true) // Ignore unknown fields like "reason" from Authorization Service
public class AuditEvent {
    private AuditEventType eventType;
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
    private String ipAddress; // IP address of the user/client
    private Boolean success;
    private String failureReason;
    private String metadata;
    private String beforeState;
    private String afterState;
    private LocalDateTime timestamp;
    private String correlationId;
}
