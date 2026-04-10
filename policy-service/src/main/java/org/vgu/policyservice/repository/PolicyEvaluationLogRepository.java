package org.vgu.policyservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.vgu.policyservice.model.PolicyEvaluationLog;

import java.util.List;

@Repository
public interface PolicyEvaluationLogRepository extends JpaRepository<PolicyEvaluationLog, Long> {
    List<PolicyEvaluationLog> findByUserKeycloakId(String userKeycloakId);
    List<PolicyEvaluationLog> findByAction(String action);
    List<PolicyEvaluationLog> findByDecision(String decision);
}
