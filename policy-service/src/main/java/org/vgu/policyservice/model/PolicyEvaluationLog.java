package org.vgu.policyservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "policy_evaluation_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PolicyEvaluationLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @Column(name = "request_id", length = 100)
    private String requestId;

    @Column(name = "user_keycloak_id", length = 255)
    private String userKeycloakId;

    @Column(name = "user_role", length = 50)
    private String userRole;

    @Column(name = "action", nullable = false, length = 100)
    private String action;

    @Column(name = "resource_type", length = 50)
    private String resourceType;

    @Column(name = "resource_id", length = 100)
    private String resourceId;

    @Column(name = "decision", columnDefinition = "ENUM('ALLOW', 'DENY', 'NOT_APPLICABLE')", nullable = false)
    private String decision;

    @Column(name = "matched_policies", columnDefinition = "JSON")
    private String matchedPolicies;

    @Column(name = "evaluation_time_ms")
    private Integer evaluationTimeMs;

    @Column(name = "context", columnDefinition = "JSON")
    private String context;

    @Column(name = "evaluated_at")
    private LocalDateTime evaluatedAt;

    @PrePersist
    protected void onCreate() {
        evaluatedAt = LocalDateTime.now();
    }
}
