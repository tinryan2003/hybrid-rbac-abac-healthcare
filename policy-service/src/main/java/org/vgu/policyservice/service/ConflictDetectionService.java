package org.vgu.policyservice.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.vgu.policyservice.dto.ConflictDetectionResult;
import org.vgu.policyservice.dto.ConflictPair;
import org.vgu.policyservice.dto.PolicyRule;
import org.vgu.policyservice.model.Policy;
import org.vgu.policyservice.repository.PolicyRepository;
import org.vgu.policyservice.service.conflict.TimeInterval;
import org.vgu.policyservice.service.conflict.TimeIntervalBinarySearch;
import org.vgu.policyservice.service.conflict.TimeIntervalReducer;

import java.util.*;

/**
 * Service for detecting statically conflicting ABAC policies
 * Based on the paper: "Detecting Conflicts in ABAC Policies with Rule Reduction
 * and Binary-Search Techniques"
 *
 * Two rules are statically conflicting if:
 * 1. Different effects (one Allow, one Deny)
 * 2. Action sets overlap
 * 3. One rule shares all attribute identifiers with the other
 * 4. For each shared attribute, value sets intersect
 *
 * Optimizations (paper 2):
 * - Rule reduction: only consider rules that can conflict (have actions and
 * attributes).
 * - Indexing: partition by effect (Allow vs Deny), index by action; only
 * compare
 * (Allow, Deny) pairs that share at least one action. Reduces comparisons from
 * O(n²)
 * to O(|A| * k_allow * k_deny) where A = distinct actions, k = rules per
 * action.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConflictDetectionService {

    private final PolicyRepository policyRepository;
    private final ObjectMapper objectMapper;

    /**
     * Detect conflicts in all enabled policies
     */
    public ConflictDetectionResult detectConflicts() {
        long startTime = System.currentTimeMillis();

        List<Policy> policies = policyRepository.findByEnabledWithRules(true);

        // Expand multi-rule policies: each rule becomes an independent PolicyRule for
        // conflict analysis
        List<PolicyRule> rules = policies.stream()
                .flatMap(p -> convertToRules(p).stream())
                .toList();

        List<ConflictPair> conflicts = new ArrayList<>(detectConflictsInRules(rules));

        long duration = System.currentTimeMillis() - startTime;

        // Detect redundancy conflicts (same effect + overlap) - Zheng 2019
        List<ConflictPair> redundancyConflicts = detectRedundancyConflicts(rules);
        conflicts.addAll(redundancyConflicts);

        log.info("Conflict detection completed: {} policies, {} auth conflicts, {} redundancy conflicts found in {}ms",
                rules.size(), conflicts.size() - redundancyConflicts.size(), redundancyConflicts.size(), duration);

        return new ConflictDetectionResult(rules.size(), conflicts.size(), conflicts, duration);
    }

    /**
     * Convert a list of policies to a flat list of PolicyRule (for conflict check on proposed state).
     */
    public List<PolicyRule> rulesFromPolicies(List<Policy> policies) {
        if (policies == null) return List.of();
        return policies.stream()
                .flatMap(p -> convertToRules(p).stream())
                .toList();
    }

    /**
     * Convert a single policy to list of PolicyRule.
     */
    public List<PolicyRule> rulesFromPolicy(Policy policy) {
        if (policy == null) return List.of();
        return convertToRules(policy);
    }

    /**
     * Run conflict detection on an in-memory list of rules (e.g. existing + proposed policy).
     * Used for conflict-on-save: before persisting, check if adding/updating would introduce AUTH_CONFLICT.
     */
    public ConflictDetectionResult detectConflictsInRuleList(List<PolicyRule> rules) {
        long startTime = System.currentTimeMillis();
        List<ConflictPair> authConflicts = detectConflictsInRules(rules);
        List<ConflictPair> redundancyConflicts = detectRedundancyConflicts(rules);
        List<ConflictPair> all = new ArrayList<>(authConflicts);
        all.addAll(redundancyConflicts);
        long duration = System.currentTimeMillis() - startTime;
        return new ConflictDetectionResult(rules.size(), all.size(), all, duration);
    }

    /**
     * Detect redundancy conflicts: same effect + full overlap (EXCEPTION).
     * Per paper: redundancy = one rule is covered/subsumed by another (same
     * effect).
     * We only report when there exists a request matching BOTH rules (full
     * attribute
     * intersection), not when they merely share attribute names with disjoint
     * values
     * (e.g. Doctor vs Admin: roles DOCTOR vs ADMIN → no common request → not
     * redundancy).
     */
    private List<ConflictPair> detectRedundancyConflicts(List<PolicyRule> rules) {
        List<ConflictPair> redundancyConflicts = new ArrayList<>();

        // Group by effect
        Map<String, List<PolicyRule>> rulesByEffect = new HashMap<>();
        for (PolicyRule rule : rules) {
            String effect = rule.getEffect();
            rulesByEffect.computeIfAbsent(effect, k -> new ArrayList<>()).add(rule);
        }

        // Check pairs within same effect
        for (List<PolicyRule> sameEffectRules : rulesByEffect.values()) {
            for (int i = 0; i < sameEffectRules.size(); i++) {
                for (int j = i + 1; j < sameEffectRules.size(); j++) {
                    PolicyRule rule1 = sameEffectRules.get(i);
                    PolicyRule rule2 = sameEffectRules.get(j);

                    if (checkRedundancy(rule1, rule2)) {
                        String reason = String.format(
                                "Redundancy detected: %s and %s have same effect (%s) and overlapping attributes/constraints",
                                rule1.getPolicyId(), rule2.getPolicyId(), rule1.getEffect());

                        Set<String> overlapActions = new HashSet<>(
                                rule1.getActions() != null ? rule1.getActions() : List.of());
                        overlapActions.retainAll(rule2.getActions() != null ? rule2.getActions() : List.of());

                        Map<String, Object> witness = buildWitness(rule1, rule2);

                        redundancyConflicts.add(new ConflictPair(
                                rule1.getPolicyId(),
                                rule2.getPolicyId(),
                                reason,
                                "REDUNDANCY_CONFLICT",
                                new ArrayList<>(overlapActions),
                                rule1.getResourceType(),
                                witness));
                    }
                }
            }
        }

        return redundancyConflicts;
    }

    /**
     * Check if two rules with same effect are redundant (overlap S, O, P, E)
     */
    private boolean checkRedundancy(PolicyRule rule1, PolicyRule rule2) {
        // Must have same effect
        if (!rule1.getEffect().equals(rule2.getEffect())) {
            return false;
        }

        // Actions must overlap
        if (!actionsOverlap(rule1.getActions(), rule2.getActions())) {
            return false;
        }

        // Same resource type
        if (!resourceTypesOverlap(rule1.getResourceType(), rule2.getResourceType())) {
            return false;
        }

        // TIME_RANGE must overlap (if both have time constraints)
        if (!timeRangesOverlap(rule1, rule2)) {
            return false;
        }

        // Redundancy requires full overlap: some request matches BOTH rules (subject
        // and
        // object value sets intersect). EXCEPTION = full intersect; ASSOCIATION =
        // partial
        // (e.g. same key "roles" but disjoint values DOCTOR vs ADMIN) → not redundancy.
        ConflictType conflictType = checkAttributeConflict(rule1, rule2);
        return conflictType == ConflictType.EXCEPTION;
    }

    /**
     * Detect conflicts in a specific list of policy IDs (AUTH + REDUNDANCY).
     */
    public ConflictDetectionResult detectConflicts(List<String> policyIds) {
        long startTime = System.currentTimeMillis();

        List<Policy> policies = policyRepository.findByEnabledWithRules(true).stream()
                .filter(p -> policyIds.contains(p.getPolicyId()))
                .toList();

        List<PolicyRule> rules = policies.stream()
                .flatMap(p -> convertToRules(p).stream())
                .toList();

        List<ConflictPair> conflicts = new ArrayList<>(detectConflictsInRules(rules));
        conflicts.addAll(detectRedundancyConflicts(rules));

        long duration = System.currentTimeMillis() - startTime;

        return new ConflictDetectionResult(rules.size(), conflicts.size(), conflicts, duration);
    }

    /**
     * Conflict detection with rule reduction + indexing + binary search (enhanced
     * Shu et al. 2009).
     * 
     * Tiers of reduction:
     * 1. RBAC gate: Index by (role, resource_type, action) bucket
     * 2. TIME_RANGE reduction: Convert overlapping intervals to disjoint segments
     * 3. Binary search: Fast lookup on sorted interval list
     * 
     * - Reduction: drop rules that cannot conflict (missing actions or attributes).
     * - Index: partition by effect; index Allow/Deny rules by each action they
     * contain.
     * - Only compare (Allow, Deny) pairs that share at least one action.
     * - TIME_RANGE: Use binary search on disjoint intervals for fast overlap
     * detection.
     */
    private List<ConflictPair> detectConflictsInRules(List<PolicyRule> rules) {
        // Rule reduction: only rules that can participate in a conflict
        List<PolicyRule> reduced = rules.stream()
                .filter(this::canParticipateInConflict)
                .toList();

        if (reduced.isEmpty()) {
            return List.of();
        }

        // Tier 1: Index by (resource_type, action) bucket (RBAC gate reduction)
        Map<String, List<PolicyRule>> bucketMap = new HashMap<>();
        for (PolicyRule rule : reduced) {
            String bucketKey = buildBucketKey(rule);
            bucketMap.computeIfAbsent(bucketKey, k -> new ArrayList<>()).add(rule);
        }

        Set<ConflictPair> conflictSet = new LinkedHashSet<>();
        int totalComparisons = 0;

        // Process each bucket separately (reduces search space)
        for (Map.Entry<String, List<PolicyRule>> bucketEntry : bucketMap.entrySet()) {
            List<PolicyRule> bucketRules = bucketEntry.getValue();

            // Partition by effect within bucket
            List<PolicyRule> allowRules = bucketRules.stream()
                    .filter(r -> "Allow".equalsIgnoreCase(r.getEffect()))
                    .toList();
            List<PolicyRule> denyRules = bucketRules.stream()
                    .filter(r -> "Deny".equalsIgnoreCase(r.getEffect()))
                    .toList();

            if (allowRules.isEmpty() || denyRules.isEmpty()) {
                continue; // No conflicts possible in this bucket
            }

            // Tier 2: TIME_RANGE reduction (if rules have time constraints)
            List<PolicyRule> rulesWithTime = bucketRules.stream()
                    .filter(r -> hasTimeConstraint(r))
                    .toList();

            List<TimeInterval> disjointIntervals = null;
            if (!rulesWithTime.isEmpty()) {
                disjointIntervals = TimeIntervalReducer.reduceToDisjointIntervals(rulesWithTime);
                log.debug("Bucket {}: Reduced {} rules with time to {} disjoint intervals",
                        bucketEntry.getKey(), rulesWithTime.size(), disjointIntervals.size());
            }

            // Index by action within bucket
            Map<String, List<Integer>> allowByAction = indexRulesByAction(allowRules);
            Map<String, List<Integer>> denyByAction = indexRulesByAction(denyRules);

            Set<String> actionsInBoth = new HashSet<>(allowByAction.keySet());
            actionsInBoth.retainAll(denyByAction.keySet());

            int bucketComparisons = 0;

            for (String action : actionsInBoth) {
                List<Integer> allowIndices = allowByAction.get(action);
                List<Integer> denyIndices = denyByAction.get(action);

                for (Integer i : allowIndices) {
                    PolicyRule allowRule = allowRules.get(i);

                    // If using binary search for time, use it here
                    if (disjointIntervals != null && hasTimeConstraint(allowRule)) {
                        TimeInterval allowTime = extractTimeInterval(allowRule);
                        if (allowTime != null) {
                            // Binary search for overlapping policies
                            Set<String> conflictingPolicyIds = TimeIntervalBinarySearch
                                    .findConflictingPolicies(disjointIntervals, allowTime, allowRule.getEffect());

                            for (Integer j : denyIndices) {
                                PolicyRule denyRule = denyRules.get(j);
                                if (conflictingPolicyIds.contains(denyRule.getPolicyId())) {
                                    bucketComparisons++;
                                    ConflictPair conflict = checkConflict(allowRule, denyRule);
                                    if (conflict != null) {
                                        conflictSet.add(conflict);
                                    }
                                }
                            }
                            continue; // Skip naive comparison for this allowRule
                        }
                    }

                    // Fallback: naive comparison (for rules without time or when binary search not
                    // applicable)
                    for (Integer j : denyIndices) {
                        bucketComparisons++;
                        PolicyRule denyRule = denyRules.get(j);
                        ConflictPair conflict = checkConflict(allowRule, denyRule);
                        if (conflict != null) {
                            conflictSet.add(conflict);
                        }
                    }
                }
            }

            totalComparisons += bucketComparisons;
            log.debug("Bucket {}: {} allow, {} deny rules; {} comparisons",
                    bucketEntry.getKey(), allowRules.size(), denyRules.size(), bucketComparisons);
        }

        log.info("Conflict detection: {} rules; {} buckets; {} total comparisons (indexed by bucket+action)",
                reduced.size(), bucketMap.size(), totalComparisons);

        return new ArrayList<>(conflictSet);
    }

    /**
     * Build bucket key: resource_type only. Rules in the same bucket are then
     * indexed by action; we only compare (Allow, Deny) pairs that share at least
     * one action. Including actions in the key would put [read] and [read,write]
     * in different buckets and miss conflicts.
     */
    private String buildBucketKey(PolicyRule rule) {
        String resourceType = rule.getResourceType() != null && !rule.getResourceType().isBlank()
                ? rule.getResourceType()
                : "ANY";
        return resourceType;
    }

    /**
     * Check if rule has TIME_RANGE constraint
     */
    @SuppressWarnings("unchecked")
    private boolean hasTimeConstraint(PolicyRule rule) {
        if (rule.getConditions() == null)
            return false;
        Object timeObj = rule.getConditions().get("time");
        if (timeObj instanceof Map) {
            Map<String, Object> timeMap = (Map<String, Object>) timeObj;
            return "TIME_RANGE".equals(timeMap.get("type"));
        }
        return false;
    }

    /**
     * Extract TimeInterval from PolicyRule
     */
    @SuppressWarnings("unchecked")
    private TimeInterval extractTimeInterval(PolicyRule rule) {
        if (rule.getConditions() == null)
            return null;
        Object timeObj = rule.getConditions().get("time");
        if (timeObj instanceof Map) {
            return TimeInterval.fromConstraint((Map<String, Object>) timeObj);
        }
        return null;
    }

    /**
     * Rule reduction: rule can only conflict if it has actions and (subject or
     * object) attributes.
     */
    private boolean canParticipateInConflict(PolicyRule rule) {
        if (rule.getActions() == null || rule.getActions().isEmpty()) {
            return false;
        }
        boolean hasSubject = rule.getSubjectAttributes() != null && !rule.getSubjectAttributes().isEmpty();
        boolean hasObject = rule.getObjectAttributes() != null && !rule.getObjectAttributes().isEmpty();
        return hasSubject || hasObject;
    }

    /**
     * Index rules by action: each action in a rule's action list maps to that
     * rule's index.
     */
    private Map<String, List<Integer>> indexRulesByAction(List<PolicyRule> ruleList) {
        Map<String, List<Integer>> actionToIndices = new HashMap<>();
        for (int i = 0; i < ruleList.size(); i++) {
            List<String> actions = ruleList.get(i).getActions();
            if (actions == null)
                continue;
            for (String action : actions) {
                if (action == null || action.isBlank())
                    continue;
                actionToIndices.computeIfAbsent(action, k -> new ArrayList<>()).add(i);
            }
        }
        return actionToIndices;
    }

    /**
     * Check if two rules are statically conflicting
     * 
     * Definition from paper (enhanced with TIME_RANGE):
     * 1. Different effects (Allow vs Deny)
     * 2. Action sets overlap
     * 3. Same resource_type
     * 4. One rule shares all attribute identifiers with the other
     * 5. For each shared attribute, value sets intersect
     * 6. TIME_RANGE constraints overlap (if present)
     */
    private ConflictPair checkConflict(PolicyRule rule1, PolicyRule rule2) {
        // Condition 1: Different effects
        if (rule1.getEffect().equals(rule2.getEffect())) {
            return null; // No conflict if same effect (will be handled by redundancy detection)
        }

        // Condition 2: Action sets overlap
        if (!actionsOverlap(rule1.getActions(), rule2.getActions())) {
            return null;
        }

        // Condition 2b (spec): same resource_type
        if (!resourceTypesOverlap(rule1.getResourceType(), rule2.getResourceType())) {
            return null;
        }

        // Condition 6: TIME_RANGE constraints must overlap (if both have time
        // constraints)
        if (!timeRangesOverlap(rule1, rule2)) {
            return null; // Time disjoint → no conflict
        }

        // Condition 3 & 4: Check attribute sharing and value set intersection
        ConflictType conflictType = checkAttributeConflict(rule1, rule2);

        if (conflictType != null) {
            String reason = buildConflictReason(rule1, rule2, conflictType);
            Set<String> overlapActions = new HashSet<>(rule1.getActions() != null ? rule1.getActions() : List.of());
            overlapActions.retainAll(rule2.getActions() != null ? rule2.getActions() : List.of());
            List<String> overlappingActionsList = new ArrayList<>(overlapActions);
            String resType = rule1.getResourceType() != null ? rule1.getResourceType() : rule2.getResourceType();
            Map<String, Object> witness = buildWitness(rule1, rule2);
            return new ConflictPair(
                    rule1.getPolicyId(),
                    rule2.getPolicyId(),
                    reason,
                    "AUTH_CONFLICT",
                    overlappingActionsList,
                    resType,
                    witness);
        }

        return null;
    }

    /**
     * Check if TIME_RANGE constraints overlap between two rules.
     * If either rule has no time constraint, they overlap (no time restriction).
     */
    private boolean timeRangesOverlap(PolicyRule rule1, PolicyRule rule2) {
        return TimeIntervalReducer.timeRangesOverlap(rule1, rule2);
    }

    /**
     * Resource types overlap if both are null/empty (no constraint) or equal.
     */
    private boolean resourceTypesOverlap(String r1, String r2) {
        if (r1 == null || r2 == null)
            return true; // ANY matches
        if (r1.isBlank() || r2.isBlank())
            return true;
        return r1.equals(r2);
    }

    /**
     * Build witness request: for each attribute in union(subject, object), pick one
     * value in the
     * intersection of both rules (so one sample request matches both rules).
     */
    private Map<String, Object> buildWitness(PolicyRule rule1, PolicyRule rule2) {
        Map<String, Object> witness = new LinkedHashMap<>();
        Set<String> attrs = new HashSet<>();
        if (rule1.getSubjectAttributes() != null)
            attrs.addAll(rule1.getSubjectAttributes().keySet());
        if (rule2.getSubjectAttributes() != null)
            attrs.addAll(rule2.getSubjectAttributes().keySet());
        if (rule1.getObjectAttributes() != null)
            attrs.addAll(rule1.getObjectAttributes().keySet());
        if (rule2.getObjectAttributes() != null)
            attrs.addAll(rule2.getObjectAttributes().keySet());
        for (String attr : attrs) {
            List<String> v1 = getAttributeValues(rule1, attr);
            List<String> v2 = getAttributeValues(rule2, attr);
            String w = witnessValue(v1, v2);
            if (w != null) {
                witness.put(attr, w);
            }
        }
        return witness;
    }

    private List<String> getAttributeValues(PolicyRule rule, String attr) {
        if (rule.getSubjectAttributes() != null && rule.getSubjectAttributes().containsKey(attr)) {
            return rule.getSubjectAttributes().get(attr);
        }
        if (rule.getObjectAttributes() != null && rule.getObjectAttributes().containsKey(attr)) {
            return rule.getObjectAttributes().get(attr);
        }
        return null;
    }

    /**
     * One value in the intersection of two value lists (SET overlap); if either is
     * null/empty treat as ANY -> pick from other.
     */
    private String witnessValue(List<String> v1, List<String> v2) {
        if (v1 == null || v1.isEmpty())
            return v2 != null && !v2.isEmpty() ? v2.get(0) : null;
        if (v2 == null || v2.isEmpty())
            return v1.get(0);
        Set<String> s1 = new HashSet<>(v1);
        s1.retainAll(v2);
        return s1.isEmpty() ? null : s1.iterator().next();
    }

    /**
     * Check if action sets overlap
     */
    private boolean actionsOverlap(List<String> actions1, List<String> actions2) {
        if (actions1 == null || actions2 == null) {
            return false;
        }

        Set<String> set1 = new HashSet<>(actions1);
        Set<String> set2 = new HashSet<>(actions2);

        set1.retainAll(set2);
        return !set1.isEmpty();
    }

    /**
     * Check attribute-based conflict
     * Returns conflict type or null if no conflict
     */
    private ConflictType checkAttributeConflict(PolicyRule rule1, PolicyRule rule2) {
        // Check subject attributes
        AttributeConflictResult subjectConflict = checkAttributeSetConflict(
                rule1.getSubjectAttributes(), rule2.getSubjectAttributes());

        // Check object attributes
        AttributeConflictResult objectConflict = checkAttributeSetConflict(
                rule1.getObjectAttributes(), rule2.getObjectAttributes());

        // Both must have conflicts (intersection) for rules to be conflicting
        if (subjectConflict == AttributeConflictResult.INTERSECT &&
                objectConflict == AttributeConflictResult.INTERSECT) {
            return ConflictType.EXCEPTION; // One rule is exception of another
        }

        if (subjectConflict == AttributeConflictResult.PARTIAL_INTERSECT ||
                objectConflict == AttributeConflictResult.PARTIAL_INTERSECT) {
            return ConflictType.ASSOCIATION; // Rules have partial overlap
        }

        return null;
    }

    /**
     * Check conflict between two attribute sets
     */
    private AttributeConflictResult checkAttributeSetConflict(
            Map<String, List<String>> attrs1, Map<String, List<String>> attrs2) {

        if (attrs1 == null || attrs2 == null || attrs1.isEmpty() || attrs2.isEmpty()) {
            return AttributeConflictResult.DISJOINT;
        }

        // Find shared attribute identifiers
        Set<String> sharedKeys = new HashSet<>(attrs1.keySet());
        sharedKeys.retainAll(attrs2.keySet());

        if (sharedKeys.isEmpty()) {
            return AttributeConflictResult.DISJOINT;
        }

        // Check if one rule shares ALL attributes with the other
        boolean rule1SharesAll = sharedKeys.containsAll(attrs1.keySet());
        boolean rule2SharesAll = sharedKeys.containsAll(attrs2.keySet());

        if (!rule1SharesAll && !rule2SharesAll) {
            return AttributeConflictResult.DISJOINT; // Neither shares all
        }

        // Check if value sets intersect for all shared attributes
        boolean allIntersect = true;
        for (String key : sharedKeys) {
            List<String> values1 = attrs1.get(key);
            List<String> values2 = attrs2.get(key);

            if (!valueSetsIntersect(values1, values2)) {
                allIntersect = false;
                break;
            }
        }

        if (allIntersect) {
            return AttributeConflictResult.INTERSECT;
        } else {
            return AttributeConflictResult.PARTIAL_INTERSECT;
        }
    }

    /**
     * Check if two value sets intersect
     */
    private boolean valueSetsIntersect(List<String> values1, List<String> values2) {
        if (values1 == null || values2 == null) {
            return false;
        }

        Set<String> set1 = new HashSet<>(values1);
        Set<String> set2 = new HashSet<>(values2);

        set1.retainAll(set2);
        return !set1.isEmpty();
    }

    /**
     * Build human-readable conflict reason
     */
    private String buildConflictReason(PolicyRule rule1, PolicyRule rule2, ConflictType type) {
        Set<String> overlappingActions = new HashSet<>(rule1.getActions());
        overlappingActions.retainAll(rule2.getActions());

        return String.format(
                "Conflict detected: %s (%s) and %s (%s) have conflicting effects for actions %s. " +
                        "Conflict type: %s. Both rules have overlapping subject/object attributes.",
                rule1.getPolicyName(), rule1.getEffect(),
                rule2.getPolicyName(), rule2.getEffect(),
                overlappingActions, type);
    }

    /**
     * Convert a Policy entity (with JPA rules list) to one or more PolicyRule DTOs.
     * Each PolicyRuleEntity in policy.getRules() produces one PolicyRule DTO.
     */
    private List<PolicyRule> convertToRules(Policy policy) {
        if (policy.getRules() == null || policy.getRules().isEmpty()) {
            log.warn("Policy '{}' has no rules", policy.getPolicyId());
            return Collections.emptyList();
        }

        List<PolicyRule> policyRules = new ArrayList<>();
        for (var ruleEntity : policy.getRules()) {
            PolicyRule pr = convertRuleEntityToDto(policy, ruleEntity);
            policyRules.add(pr);
        }
        return policyRules;
    }

    /**
     * Convert a single PolicyRuleEntity to a PolicyRule DTO for conflict detection.
     */
    @SuppressWarnings("unchecked")
    private PolicyRule convertRuleEntityToDto(Policy policy, org.vgu.policyservice.model.PolicyRuleEntity ruleEntity) {
        PolicyRule rule = new PolicyRule();
        rule.setPolicyId(policy.getPolicyId() + "__" + ruleEntity.getRuleId());
        rule.setPolicyName(ruleEntity.getRuleName() != null ? ruleEntity.getRuleName() : policy.getPolicyName());
        rule.setEffect(ruleEntity.getEffect() != null ? ruleEntity.getEffect() : "Allow");

        try {
            // Subjects (handle plain JSON or double-encoded string from some DB/drivers)
            Map<String, Object> subjects = parseJsonMap(ruleEntity.getSubjects());
            rule.setSubjectAttributes(extractAttributes(subjects));

            // Resources
            Map<String, Object> resources = parseJsonMap(ruleEntity.getResources());
            rule.setObjectAttributes(extractAttributes(resources));
            Object rt = resources.get("type");
            if (rt == null)
                rt = resources.get("object");
            rule.setResourceType(rt != null ? rt.toString() : null);

            // Actions
            List<String> actions = parseJsonList(ruleEntity.getActions());
            rule.setActions(actions);

            // Conditions
            if (ruleEntity.getConditions() != null && !ruleEntity.getConditions().isBlank()) {
                Map<String, Object> conditions = parseJsonMap(ruleEntity.getConditions());
                rule.setConditions(conditions);
            }
        } catch (Exception e) {
            log.error("Failed to parse rule entity {} in policy {}: {}",
                    ruleEntity.getRuleId(), policy.getPolicyId(), e.getMessage());
        }
        return rule;
    }

    /**
     * Parse a JSON string into Map. Handles double-encoded values (e.g. when the
     * stored value is a JSON string whose content is itself a JSON object).
     */
    private Map<String, Object> parseJsonMap(String json) {
        if (json == null || json.isBlank()) return new HashMap<>();
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            try {
                String inner = objectMapper.readValue(json, String.class);
                if (inner != null && inner.trim().startsWith("{")) {
                    return objectMapper.readValue(inner.trim(), new TypeReference<Map<String, Object>>() {});
                }
            } catch (Exception ignored) { }
            log.debug("parseJsonMap failed: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Parse a JSON string into List. Handles double-encoded string when needed.
     */
    private List<String> parseJsonList(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            try {
                String inner = objectMapper.readValue(json, String.class);
                if (inner != null && inner.trim().startsWith("[")) {
                    return objectMapper.readValue(inner, new TypeReference<List<String>>() {});
                }
            } catch (Exception ignored) { }
            return new ArrayList<>();
        }
    }

    /**
     * Extract attributes from JSON object
     * Handles both single values and arrays
     */
    @SuppressWarnings("unchecked")
    private Map<String, List<String>> extractAttributes(Map<String, Object> jsonObject) {
        Map<String, List<String>> attributes = new HashMap<>();

        for (Map.Entry<String, Object> entry : jsonObject.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            List<String> values = new ArrayList<>();
            if (value instanceof List) {
                for (Object item : (List<?>) value) {
                    values.add(item.toString());
                }
            } else if (value != null) {
                values.add(value.toString());
            }

            if (!values.isEmpty()) {
                attributes.put(key, values);
            }
        }

        return attributes;
    }

    // =====================================================
    // Internal Enums
    // =====================================================

    private enum AttributeConflictResult {
        DISJOINT, // No shared attributes or no intersection
        INTERSECT, // All shared attributes intersect
        PARTIAL_INTERSECT // Some shared attributes intersect
    }

    private enum ConflictType {
        EXCEPTION, // One rule is exception of another (full overlap)
        ASSOCIATION // Rules have partial overlap
    }
}
