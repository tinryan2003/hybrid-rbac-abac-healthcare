package org.vgu.policyservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.vgu.policyservice.model.PolicyRuleEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface PolicyRuleRepository extends JpaRepository<PolicyRuleEntity, Long> {
    List<PolicyRuleEntity> findByPolicyId(String policyId);
    Optional<PolicyRuleEntity> findByPolicyIdAndRuleId(String policyId, String ruleId);

    @Modifying
    @Query("DELETE FROM PolicyRuleEntity r WHERE r.policyId = :policyId")
    void deleteByPolicyId(@Param("policyId") String policyId);
}
