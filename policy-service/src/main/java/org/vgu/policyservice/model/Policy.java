package org.vgu.policyservice.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "policies")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Policy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    @Column(name = "policy_id", unique = true, nullable = false, length = 100)
    private String policyId;

    @Column(name = "policy_name", nullable = false, length = 200)
    private String policyName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * How to combine multiple rules within this policy.
     * deny-overrides (default): any deny → policy denies
     * allow-overrides: any allow → policy allows (overrides deny from same policy)
     * first-applicable: use the first matching rule by priority
     */
    @Column(name = "combining_algorithm", length = 50)
    private String combiningAlgorithm = "deny-overrides";

    @Column(name = "enabled")
    private Boolean enabled = true;

    @Column(name = "priority")
    private Integer priority = 0;

    @Column(name = "version")
    private Integer version = 1;

    @Column(name = "tags", columnDefinition = "JSON")
    private String tags;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by_keycloak_id", length = 255)
    private String createdByKeycloakId;

    @Column(name = "updated_by_keycloak_id", length = 255)
    private String updatedByKeycloakId;

    // Governance & Accountability Metadata
    @Column(name = "justification", columnDefinition = "TEXT")
    private String justification;

    @Column(name = "ticket_id", length = 100)
    private String ticketId;

    @Column(name = "business_owner", length = 255)
    private String businessOwner;

    /**
     * One-to-many relationship: a Policy contains multiple PolicyRuleEntities.
     * Cascade ALL ensures rules are persisted/updated/deleted with the policy.
     * orphanRemoval ensures rules removed from the list are deleted from DB.
     */
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "policy_id", referencedColumnName = "policy_id")
    private List<PolicyRuleEntity> rules = new ArrayList<>();

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
