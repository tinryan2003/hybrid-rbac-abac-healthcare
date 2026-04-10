package org.vgu.policyservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.vgu.policyservice.dto.ConflictDetectionResult;
import org.vgu.policyservice.dto.PolicyCreateUpdateRequest;
import org.vgu.policyservice.dto.PolicyRule;
import org.vgu.policyservice.dto.PolicyRuleItemDto;
import org.vgu.policyservice.exception.PolicyConflictException;
import org.vgu.policyservice.model.Policy;
import org.vgu.policyservice.model.PolicyRuleEntity;
import org.vgu.policyservice.repository.PolicyRepository;
import org.vgu.policyservice.repository.PolicyRuleRepository;
import org.vgu.policyservice.security.SecurityContextUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class PolicyCrudService {

    private final PolicyRepository policyRepository;
    private final PolicyRuleRepository policyRuleRepository;
    private final ObjectMapper objectMapper;
    private final OpaSyncService opaSyncService;
    private final ConflictDetectionService conflictDetectionService;

    @Value("${policy.conflict-on-save:off}")
    private String conflictOnSave;

    @Value("${policy.reject-wildcard:false}")
    private boolean rejectWildcard;

    @Transactional(readOnly = true)
    public List<Policy> findAll(String tenantId, Boolean enabled) {
        if (tenantId != null && !tenantId.isBlank()) {
            if (enabled != null) {
                return policyRepository.findByTenantIdAndEnabledWithRules(tenantId, enabled);
            }
            return policyRepository.findByTenantIdWithRules(tenantId);
        }
        if (enabled != null) {
            return policyRepository.findByEnabledWithRules(enabled);
        }
        return policyRepository.findAllWithRules();
    }

    @Transactional(readOnly = true)
    public Optional<Policy> findById(Long id) {
        return policyRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Policy> findByPolicyId(String policyId) {
        return policyRepository.findByPolicyId(policyId);
    }

    @Transactional
    public Policy create(PolicyCreateUpdateRequest request) {
        if (policyRepository.findByPolicyId(request.getPolicyId()).isPresent()) {
            throw new IllegalArgumentException("Policy already exists with policyId: " + request.getPolicyId());
        }

        String actorKeycloakId = SecurityContextUtil.currentSubject().orElse(null);

        // Normalize: if no rules[] but top-level effect/subjects/actions/resources
        // present, build single rule
        List<PolicyRuleItemDto> rules = request.getRules();
        if (rules == null || rules.isEmpty()) {
            rules = buildSingleRuleFromTopLevel(request);
            if (rules == null || rules.isEmpty()) {
                throw new IllegalArgumentException(
                        "Policy must have at least one rule: provide 'rules' array or top-level effect/subjects/actions/resources");
            }
            request.setRules(rules);
        }

        // Validate actions in each rule
        for (var ruleDto : request.getRules()) {
            validateActions(ruleDto.getActions());
        }
        validateNoWildcardIfRequired(request);

        // Conflict check (strict): before save, simulate "all enabled + this new
        // policy"
        if ("strict".equalsIgnoreCase(conflictOnSave)) {
            List<Policy> existingEnabled = findAll(null, true);
            Policy newPolicy = toEntity(request);
            List<PolicyRule> allRules = new ArrayList<>(conflictDetectionService.rulesFromPolicies(existingEnabled));
            allRules.addAll(conflictDetectionService.rulesFromPolicy(newPolicy));
            ConflictDetectionResult result = conflictDetectionService.detectConflictsInRuleList(allRules);
            if (result.getConflictCount() > 0 && result.getConflicts().stream()
                    .anyMatch(c -> "AUTH_CONFLICT".equals(c.getConflictType()))) {
                throw new PolicyConflictException(result);
            }
        }

        Policy policy = toEntity(request);
        policy.setVersion(1);
        policy.setCreatedByKeycloakId(actorKeycloakId);
        policy.setUpdatedByKeycloakId(actorKeycloakId);
        policy = policyRepository.save(policy);
        log.info("[POLICY] Created policy: policyId={}, name={}, id={}, rules={}",
                policy.getPolicyId(), policy.getPolicyName(), policy.getId(), policy.getRules().size());

        // Sync to OPA if enabled
        if (policy.getEnabled()) {
            try {
                opaSyncService.syncPolicyToOpa(policy);
            } catch (Exception e) {
                log.error("[POLICY] Failed to sync new policy to OPA: {}", e.getMessage());
            }
        }

        return policy;
    }

    @Transactional
    public Policy update(Long id, PolicyCreateUpdateRequest request) {
        // Load policy WITHOUT rules so we don't have old rule entities in persistence
        // context.
        // Otherwise clear() would trigger orphanRemoval DELETE on already-deleted rows
        // -> StaleStateException.
        Policy existing = policyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Policy not found: id=" + id));

        String actorKeycloakId = SecurityContextUtil.currentSubject().orElse(null);

        boolean wasEnabled = existing.getEnabled();
        String policyId = existing.getPolicyId();

        // Normalize single-rule mode for update
        List<PolicyRuleItemDto> rulesUpdate = request.getRules();
        if (rulesUpdate == null || rulesUpdate.isEmpty()) {
            rulesUpdate = buildSingleRuleFromTopLevel(request);
            if (rulesUpdate != null && !rulesUpdate.isEmpty()) {
                request.setRules(rulesUpdate);
            }
        }
        if (request.getRules() == null || request.getRules().isEmpty()) {
            throw new IllegalArgumentException(
                    "Policy must have at least one rule: provide 'rules' array or top-level effect/subjects/actions/resources");
        }

        // Validate actions in each rule
        for (var ruleDto : request.getRules()) {
            validateActions(ruleDto.getActions());
        }
        validateNoWildcardIfRequired(request);

        // Conflict check (strict): before save, simulate "all enabled with this policy
        // updated". Use findByEnabled (no fetch rules) so we do not load existing.rules;
        // otherwise later deleteByPolicyId + existing.getRules().clear() would trigger
        // orphanRemoval DELETE for already-deleted rows -> StaleStateException.
        if ("strict".equalsIgnoreCase(conflictOnSave)) {
            Policy updatedAsPolicy = toEntity(request);
            updatedAsPolicy.setPolicyId(policyId);
            List<Policy> enabled = policyRepository.findByEnabled(true);
            List<PolicyRule> allRules = new ArrayList<>();
            for (Policy p : enabled) {
                if (p.getPolicyId().equals(policyId)) {
                    allRules.addAll(conflictDetectionService.rulesFromPolicy(updatedAsPolicy));
                } else {
                    allRules.addAll(conflictDetectionService.rulesFromPolicy(p));
                }
            }
            ConflictDetectionResult result = conflictDetectionService.detectConflictsInRuleList(allRules);
            if (result.getConflictCount() > 0 && result.getConflicts().stream()
                    .anyMatch(c -> "AUTH_CONFLICT".equals(c.getConflictType()))) {
                throw new PolicyConflictException(result);
            }
        }

        // Update policy metadata
        existing.setPolicyName(request.getPolicyName());
        existing.setDescription(request.getDescription());
        existing.setEnabled(request.getEnabled() != null ? request.getEnabled() : true);
        existing.setPriority(request.getPriority() != null ? request.getPriority()
                : (existing.getPriority() != null ? existing.getPriority() : 0));
        existing.setTags(request.getTags() != null ? toJson(request.getTags()) : null);
        existing.setCombiningAlgorithm(request.getCombiningAlgorithm() != null
                ? request.getCombiningAlgorithm()
                : existing.getCombiningAlgorithm());

        // Governance / accountability metadata
        existing.setJustification(request.getJustification());
        existing.setTicketId(request.getTicketId());
        existing.setBusinessOwner(request.getBusinessOwner());
        existing.setUpdatedByKeycloakId(actorKeycloakId);
        existing.setVersion(existing.getVersion() != null ? (existing.getVersion() + 1) : 1);

        // Delete old rules in DB first (rules were not loaded, so no orphanRemoval
        // conflict)
        policyRuleRepository.deleteByPolicyId(policyId);
        policyRepository.flush();

        // Reuse the same collection (do not setRules(newList) - orphanRemoval forbids
        // replacing the reference)
        existing.getRules().clear();
        for (var ruleDto : request.getRules()) {
            existing.getRules().add(toRuleEntity(policyId, ruleDto));
        }

        existing = policyRepository.save(existing);
        log.info("[POLICY] Updated policy: id={}, policyId={}, rules={}",
                id, existing.getPolicyId(), existing.getRules().size());

        try {
            if (existing.getEnabled()) {
                opaSyncService.syncPolicyToOpa(existing);
            } else if (wasEnabled && !existing.getEnabled()) {
                opaSyncService.deletePolicyFromOpa(existing);
            }
        } catch (Exception e) {
            log.error("[POLICY] Failed to sync updated policy to OPA: {}", e.getMessage());
        }

        return existing;
    }

    @Transactional
    public void delete(Long id) {
        Policy policy = policyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Policy not found: id=" + id));

        String policyId = policy.getPolicyId();

        // Delete from OPA first (need the rules to derive keys)
        try {
            opaSyncService.deletePolicyFromOpa(policy);
        } catch (Exception e) {
            log.error("[POLICY] Failed to delete policy from OPA: {}", e.getMessage());
        }

        // Delete rules first to avoid Hibernate doing UPDATE policy_rules SET
        // policy_id=null
        // (which fails because policy_id is NOT NULL)
        policyRuleRepository.deleteByPolicyId(policyId);
        policyRepository.flush();

        // Delete policy by query so we don't load the entity (and trigger cascade
        // delete on already-deleted rules)
        policyRepository.deletePolicyById(id);
        log.info("[POLICY] Deleted policy: id={}, policyId={}", id, policyId);
    }

    /**
     * Convert API request to Policy entity with rules.
     */
    private Policy toEntity(PolicyCreateUpdateRequest request) {
        Policy p = new Policy();
        p.setTenantId(request.getTenantId());
        p.setPolicyId(request.getPolicyId());
        p.setPolicyName(request.getPolicyName());
        p.setDescription(request.getDescription());
        p.setEnabled(request.getEnabled() != null ? request.getEnabled() : true);
        p.setPriority(request.getPriority() != null ? request.getPriority() : 0);
        p.setTags(request.getTags() != null ? toJson(request.getTags()) : null);
        p.setCombiningAlgorithm(request.getCombiningAlgorithm() != null
                ? request.getCombiningAlgorithm()
                : "deny-overrides");

        // Governance / accountability metadata
        p.setJustification(request.getJustification());
        p.setTicketId(request.getTicketId());
        p.setBusinessOwner(request.getBusinessOwner());
        p.setVersion(1);

        // Convert rules from DTO to entity
        List<PolicyRuleEntity> ruleEntities = new ArrayList<>();
        if (request.getRules() != null) {
            for (var ruleDto : request.getRules()) {
                PolicyRuleEntity ruleEntity = toRuleEntity(request.getPolicyId(), ruleDto);
                ruleEntities.add(ruleEntity);
            }
        }
        p.setRules(ruleEntities);

        return p;
    }

    /**
     * Convert PolicyRuleItemDto to PolicyRuleEntity.
     */
    private PolicyRuleEntity toRuleEntity(String policyId, PolicyRuleItemDto dto) {
        PolicyRuleEntity entity = new PolicyRuleEntity();
        entity.setPolicyId(policyId);
        entity.setRuleId(dto.getRuleId());
        entity.setRuleName(dto.getRuleName());
        entity.setEffect(dto.getEffect() != null ? dto.getEffect() : "Allow");
        entity.setSubjects(toJson(dto.getSubjects()));
        entity.setActions(toJson(dto.getActions()));
        entity.setResources(toJson(dto.getResources()));
        entity.setConditions(dto.getConditions() != null ? toJson(dto.getConditions()) : null);
        entity.setPriority(dto.getPriority() != null ? dto.getPriority() : 0);
        entity.setEnabled(true);
        return entity;
    }

    private String toJson(Object obj) {
        if (obj == null)
            return null;
        if (obj instanceof String s)
            return s;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid JSON for policy field: " + e.getMessage());
        }
    }

    /**
     * Create a policy with a generated set of N rules (for load/testing).
     * Rules are minimal but valid: Allow, role DOCTOR, read, patient_record.
     * Use this to avoid sending a huge JSON over HTTP (e.g. 10_000 rules).
     */
    @Transactional
    public Policy createPolicyWithGeneratedRules(String policyId, int ruleCount) {
        if (ruleCount < 1 || ruleCount > 50_000) {
            throw new IllegalArgumentException("ruleCount must be between 1 and 50_000");
        }
        if (policyRepository.findByPolicyId(policyId).isPresent()) {
            throw new IllegalArgumentException("Policy already exists: " + policyId);
        }
        PolicyCreateUpdateRequest request = new PolicyCreateUpdateRequest();
        request.setTenantId("default");
        request.setPolicyId(policyId);
        request.setPolicyName("Generated policy with " + ruleCount + " rules");
        request.setDescription("Auto-generated for load test / conflict detection scale");
        request.setEnabled(true);
        request.setCombiningAlgorithm("deny-overrides");
        List<PolicyRuleItemDto> rules = new ArrayList<>(ruleCount);
        for (int i = 1; i <= ruleCount; i++) {
            PolicyRuleItemDto r = new PolicyRuleItemDto();
            r.setRuleId("r" + i);
            r.setRuleName("Generated rule " + i);
            r.setEffect("Allow");
            r.setSubjects(Map.of("roles", List.of("DOCTOR")));
            r.setActions(List.of("read"));
            r.setResources(Map.of("type", "patient_record"));
            r.setConditions(null);
            r.setPriority(0);
            rules.add(r);
        }
        request.setRules(rules);
        Policy policy = create(request);
        log.info("[POLICY] Created policy with {} generated rules: {}", ruleCount, policyId);
        return policy;
    }

    /**
     * Validate actions and warn about deprecated actions.
     * Does not reject, only logs warnings to help admins migrate.
     */
    @SuppressWarnings("unchecked")
    private void validateActions(Object actionsObj) {
        if (actionsObj == null)
            return;

        try {
            List<String> actions;
            if (actionsObj instanceof List) {
                actions = (List<String>) actionsObj;
            } else if (actionsObj instanceof String) {
                // Try to parse JSON string
                actions = objectMapper.readValue((String) actionsObj,
                        new TypeReference<List<String>>() {
                        });
            } else {
                // Convert to JSON and parse
                String json = objectMapper.writeValueAsString(actionsObj);
                actions = objectMapper.readValue(json,
                        new TypeReference<List<String>>() {
                        });
            }

            // Check for deprecated actions
            for (String action : actions) {
                if ("write".equalsIgnoreCase(action)) {
                    log.warn("⚠️ Deprecated action 'write' detected. Please use 'update' instead. " +
                            "Policy will not match requests (Gateway maps PUT/PATCH to 'update', not 'write').");
                }
                if ("view".equalsIgnoreCase(action)) {
                    log.warn("⚠️ Deprecated action 'view' detected. Please use 'read' instead. " +
                            "Policy will not match requests (Gateway maps GET to 'read', not 'view').");
                }
            }
        } catch (Exception e) {
            // If parsing fails, skip validation (will fail later in toJson anyway)
            log.debug("Could not validate actions: {}", e.getMessage());
        }
    }

    /**
     * If policy.reject-wildcard is true, reject rules that use resource="*" or
     * action="*".
     */
    /**
     * Build a single rule from top-level
     * effect/subjects/actions/resources/conditions (single-rule mode).
     * Returns null if top-level does not have enough to form a rule (e.g. no
     * actions).
     */
    private List<PolicyRuleItemDto> buildSingleRuleFromTopLevel(PolicyCreateUpdateRequest request) {
        if (request.getActions() == null)
            return null;
        PolicyRuleItemDto one = new PolicyRuleItemDto();
        one.setRuleId("r1");
        one.setRuleName(request.getPolicyName() != null ? request.getPolicyName() : "Rule 1");
        one.setEffect(request.getEffect() != null ? request.getEffect() : "Allow");
        one.setSubjects(request.getSubjects());
        one.setActions(request.getActions());
        one.setResources(request.getResources());
        one.setConditions(request.getConditions());
        one.setPriority(request.getPriority() != null ? request.getPriority() : 0);
        return List.of(one);
    }

    private void validateNoWildcardIfRequired(PolicyCreateUpdateRequest request) {
        if (!rejectWildcard || request.getRules() == null)
            return;
        for (PolicyRuleItemDto r : request.getRules()) {
            if (hasWildcardActions(r.getActions())) {
                throw new IllegalArgumentException(
                        "Wildcard action '*' is not allowed when policy.reject-wildcard is enabled. Rule: "
                                + r.getRuleId());
            }
            if (hasWildcardResource(r.getResources())) {
                throw new IllegalArgumentException(
                        "Wildcard resource type '*' is not allowed when policy.reject-wildcard is enabled. Rule: "
                                + r.getRuleId());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private boolean hasWildcardActions(Object actionsObj) {
        if (actionsObj == null)
            return false;
        try {
            List<String> actions;
            if (actionsObj instanceof List) {
                actions = (List<String>) actionsObj;
            } else {
                String json = objectMapper.writeValueAsString(actionsObj);
                actions = objectMapper.readValue(json, new TypeReference<List<String>>() {
                });
            }
            return actions != null && (actions.contains("*") || actions.stream().anyMatch("*"::equalsIgnoreCase));
        } catch (Exception e) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private boolean hasWildcardResource(Object resourcesObj) {
        if (resourcesObj == null)
            return false;
        try {
            Map<String, Object> res;
            if (resourcesObj instanceof Map) {
                res = (Map<String, Object>) resourcesObj;
            } else {
                String json = objectMapper.writeValueAsString(resourcesObj);
                res = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
                });
            }
            if (res == null)
                return false;
            Object type = res.get("type");
            return type != null && "*".equals(type.toString().trim());
        } catch (Exception e) {
            return false;
        }
    }
}
