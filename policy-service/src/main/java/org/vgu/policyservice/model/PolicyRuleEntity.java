package org.vgu.policyservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "policy_rules")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PolicyRuleEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_id", nullable = false, length = 100)
    private String ruleId;

    @Column(name = "policy_id", nullable = false, length = 100)
    private String policyId;

    @Column(name = "rule_name", length = 200)
    private String ruleName;

    @Column(name = "effect", columnDefinition = "ENUM('Allow', 'Deny')", nullable = false)
    private String effect = "Allow";

    @Column(name = "subjects", columnDefinition = "JSON", nullable = false)
    private String subjects;

    @Column(name = "actions", columnDefinition = "JSON", nullable = false)
    private String actions;

    @Column(name = "resources", columnDefinition = "JSON", nullable = false)
    private String resources;

    @Column(name = "conditions", columnDefinition = "JSON")
    private String conditions;

    @Column(name = "priority")
    private Integer priority = 0;

    @Column(name = "enabled")
    private Boolean enabled = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
