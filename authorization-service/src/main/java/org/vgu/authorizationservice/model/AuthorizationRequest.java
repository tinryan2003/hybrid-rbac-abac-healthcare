package org.vgu.authorizationservice.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;

@Data
public class AuthorizationRequest {

    @NotBlank(message = "Subject is required")
    private String subject; // User ID from JWT

    @NotBlank(message = "Object is required")
    private String object; // Resource: e.g. "patient_record", "appointment", "policy_management", "audit_log"

    @NotBlank(message = "Action is required")
    private String action; // Action: "read", "create", "update", "delete", "approve", "dispense", "cancel", "complete", "manage", etc.

    // RBAC (from JWT)
    private String role;

    // Hospital ABAC (from User Service / PIP)
    private String department; // e.g., "CARDIOLOGY", "PEDIATRICS"
    private String position; // e.g., "DOCTOR", "NURSE"
    private String hospital; // Hospital ID for multi-tenant
    private Integer sensitivityLevel; // Data sensitivity level (1-4)

    // Request context (for OPA env / audit)
    private String ip;
    private String time;
    private String deviceType;
    private String networkZone;
    private String channel;

    // Resource (e.g. patient ID, appointment ID)
    private String resourceId;

    // Extra attributes (ward_id, position_level, etc.). Supports "additionalContext" or "environment".
    @JsonAlias("environment")
    private Map<String, Object> additionalContext;

    /**
     * Build environment map for OPA context and response.
     * Hospital-relevant attributes only (no bank/transaction fields).
     */
    public Map<String, Object> buildEnvironmentMap() {
        Map<String, Object> env = new java.util.HashMap<>();
        if (role != null) env.put("role", role);
        if (department != null) env.put("department", department);
        if (position != null) env.put("position", position);
        if (hospital != null) env.put("hospital", hospital);
        if (sensitivityLevel != null) env.put("sensitivityLevel", sensitivityLevel);
        if (ip != null) env.put("ip", ip);
        if (time != null) env.put("time", time);
        if (deviceType != null) env.put("deviceType", deviceType);
        if (networkZone != null) env.put("networkZone", networkZone);
        if (channel != null) env.put("channel", channel);
        if (resourceId != null) env.put("resourceId", resourceId);
        if (additionalContext != null) env.putAll(additionalContext);
        return env;
    }
}