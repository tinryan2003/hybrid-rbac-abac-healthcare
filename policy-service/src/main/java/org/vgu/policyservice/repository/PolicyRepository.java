package org.vgu.policyservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.vgu.policyservice.model.Policy;

import java.util.List;
import java.util.Optional;

@Repository
public interface PolicyRepository extends JpaRepository<Policy, Long> {
    Optional<Policy> findByPolicyId(String policyId);

    @Query("SELECT DISTINCT p FROM Policy p LEFT JOIN FETCH p.rules WHERE p.policyId = :policyId")
    Optional<Policy> findByPolicyIdWithRules(String policyId);

    /** Delete policy by id without loading entity (avoids cascade/orphanRemoval touching already-deleted rules). */
    @Modifying
    @Query("DELETE FROM Policy p WHERE p.id = :id")
    void deletePolicyById(@Param("id") Long id);

    List<Policy> findByTenantId(String tenantId);
    List<Policy> findByTenantIdAndEnabled(String tenantId, Boolean enabled);
    List<Policy> findByEnabled(boolean enabled);

    /** Loads enabled policies with their rules in one query to avoid LazyInitializationException when syncing to OPA. */
    @Query("SELECT DISTINCT p FROM Policy p LEFT JOIN FETCH p.rules WHERE p.enabled = true")
    List<Policy> findByEnabledWithRules(boolean enabled);

    /** Loads all policies with rules for list API (avoids LazyInitializationException on JSON serialize). */
    @Query("SELECT DISTINCT p FROM Policy p LEFT JOIN FETCH p.rules")
    List<Policy> findAllWithRules();

    @Query("SELECT DISTINCT p FROM Policy p LEFT JOIN FETCH p.rules WHERE p.tenantId = :tenantId")
    List<Policy> findByTenantIdWithRules(@Param("tenantId") String tenantId);

    @Query("SELECT DISTINCT p FROM Policy p LEFT JOIN FETCH p.rules WHERE p.tenantId = :tenantId AND p.enabled = :enabled")
    List<Policy> findByTenantIdAndEnabledWithRules(@Param("tenantId") String tenantId, @Param("enabled") Boolean enabled);
}
