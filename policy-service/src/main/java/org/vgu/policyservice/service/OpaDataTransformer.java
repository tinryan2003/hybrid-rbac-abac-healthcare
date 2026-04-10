package org.vgu.policyservice.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.vgu.policyservice.model.Policy;

import java.util.*;

/**
 * OPA Data Transformer - Converts Policy entities to OPA-compatible data
 * format.
 *
 * Supports both single-rule and multi-rule policies:
 * - Single-rule (legacy/flat): one OPA entry with key = policyId
 * - Multi-rule: one OPA entry per rule, key = {policyId}__{ruleId}
 * Each rule entry carries parent_policy_id and combining_algorithm so Rego
 * can apply the per-policy combining algorithm before global deny-overrides.
 *
 * Pattern: Policy Entity → List<OpaEntry> (key + data map)
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OpaDataTransformer {

    private final ObjectMapper objectMapper;

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Transform a Policy to a list of OPA entries.
     * Each entry is a Map with a special "_opa_key" field indicating the OPA data
     * key.
     *
     * Each PolicyRuleEntity in policy.getRules() → one OPA entry
     * Key format: {policyId}__{ruleId}
     */
    public List<Map<String, Object>> transformToOpaEntries(Policy policy) {
        if (policy.getRules() == null || policy.getRules().isEmpty()) {
            log.warn("[OPA] Policy '{}' has no rules, skipping transformation", policy.getPolicyId());
            return Collections.emptyList();
        }

        String combiningAlgo = policy.getCombiningAlgorithm() != null
                ? policy.getCombiningAlgorithm()
                : "deny-overrides";

        List<Map<String, Object>> entries = new ArrayList<>();
        for (var ruleEntity : policy.getRules()) {
            String opaKey = policy.getPolicyId() + "__" + ruleEntity.getRuleId();
            Map<String, Object> entry = buildRuleEntryFromEntity(policy, ruleEntity, opaKey, combiningAlgo);
            entries.add(entry);
        }

        log.info("[OPA] Transformed policy '{}' with {} rules (combining={})",
                policy.getPolicyId(), entries.size(), combiningAlgo);
        return entries;
    }

    /**
     * Backward-compatible helper: returns the first (or only) OPA entry data map.
     * Used by callers that haven't been updated to handle multi-entry yet.
     * 
     * @deprecated Use transformToOpaEntries instead
     */
    @Deprecated
    public Map<String, Object> transformToOpaFormat(Policy policy) {
        List<Map<String, Object>> entries = transformToOpaEntries(policy);
        return entries.isEmpty() ? new HashMap<>() : entries.get(0);
    }

    /**
     * Extract the OPA data key from an entry produced by transformToOpaEntries.
     */
    public static String getOpaKey(Map<String, Object> entry) {
        Object key = entry.get("_opa_key");
        return key != null ? key.toString() : null;
    }

    /**
     * Build OPA entry from a JPA PolicyRuleEntity.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> buildRuleEntryFromEntity(Policy policy,
            org.vgu.policyservice.model.PolicyRuleEntity ruleEntity,
            String opaKey,
            String combiningAlgorithm) {
        Map<String, Object> entry = new HashMap<>();
        entry.put("_opa_key", opaKey);

        // Identity
        entry.put("policy_id", opaKey);
        entry.put("policy_name", ruleEntity.getRuleName() != null ? ruleEntity.getRuleName()
                : policy.getPolicyName() + " [" + ruleEntity.getRuleId() + "]");
        entry.put("parent_policy_id", policy.getPolicyId());
        entry.put("combining_algorithm", combiningAlgorithm);
        entry.put("enabled", ruleEntity.getEnabled() != null ? ruleEntity.getEnabled() : true);
        entry.put("priority", ruleEntity.getPriority() != null ? ruleEntity.getPriority() : 0);

        // Effect
        String effect = ruleEntity.getEffect() != null ? ruleEntity.getEffect().toLowerCase() : "allow";
        entry.put("effect", effect);

        // Subjects
        Map<String, Object> subjects = parseJsonField(ruleEntity.getSubjects());
        populateRbacFields(entry, subjects);

        // Actions
        List<String> actions = parseJsonFieldAsList(ruleEntity.getActions());
        if (actions != null && !actions.isEmpty()) {
            entry.put("actions", actions);
        }

        // Resources
        Map<String, Object> resources = parseJsonField(ruleEntity.getResources());
        populateResourceFields(entry, resources);

        // Conditions
        if (ruleEntity.getConditions() != null && !ruleEntity.getConditions().isBlank()) {
            Map<String, Object> rawConditions = parseJsonField(ruleEntity.getConditions());
            if (!rawConditions.isEmpty()) {
                Map<String, Object> constraints = transformConditionsToConstraints(rawConditions);
                if (!constraints.isEmpty()) {
                    entry.put("constraints", constraints);
                }
            }
        }

        return entry;
    }

    // -------------------------------------------------------------------------
    // Field population helpers
    // -------------------------------------------------------------------------

    private void populateRbacFields(Map<String, Object> entry, Map<String, Object> subjects) {
        if (subjects == null || subjects.isEmpty())
            return;
        List<String> targetRoles = extractRoles(subjects);
        if (!targetRoles.isEmpty()) {
            entry.put("target_roles", targetRoles);
        }
    }

    private void populateResourceFields(Map<String, Object> entry, Map<String, Object> resources) {
        if (resources == null || resources.isEmpty())
            return;
        Object resType = resources.get("type");
        Object resObject = resources.get("object");
        entry.put("resource_type", resType);
        entry.put("resource_object", resObject != null ? resObject : resType);
        resources.forEach((key, value) -> {
            if (!key.equals("type") && !key.equals("object")) {
                entry.put("resource_" + key, value);
            }
        });
    }

    // -------------------------------------------------------------------------
    // RBAC extraction
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private List<String> extractRoles(Map<String, Object> subjects) {
        if (subjects == null || subjects.isEmpty())
            return Collections.emptyList();
        Object rolesObj = subjects.get("roles");
        if (rolesObj instanceof List)
            return (List<String>) rolesObj;
        Object roleObj = subjects.get("role");
        if (roleObj instanceof String)
            return List.of((String) roleObj);
        return Collections.emptyList();
    }

    // -------------------------------------------------------------------------
    // Conditions → Constraints (ABAC)
    // -------------------------------------------------------------------------

    private Map<String, Object> transformConditionsToConstraints(Map<String, Object> conditions) {
        Map<String, Object> constraints = new HashMap<>();

        Map<String, Object> tree = conditions;
        if (conditions.containsKey("conditions") && conditions.get("conditions") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> inner = (Map<String, Object>) conditions.get("conditions");
            tree = inner;
        }

        if (tree.containsKey("children")) {
            extractConstraintsFromConditionTree(tree, constraints);
        }
        if (tree.containsKey("constraints") && tree.get("constraints") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> innerMap = (Map<String, Object>) tree.get("constraints");
            if (innerMap.containsKey("children")) {
                extractConstraintsFromConditionTree(innerMap, constraints);
            }
        }

        if (tree.containsKey("time_range")) {
            String timeRange = (String) tree.get("time_range");
            Map<String, Object> timeConstraint = parseTimeRange(timeRange);
            if (!timeConstraint.isEmpty())
                constraints.put("time", timeConstraint);
        }

        if (tree.containsKey("working_hours_only")) {
            if (Boolean.TRUE.equals(tree.get("working_hours_only"))) {
                constraints.put("time", Map.of(
                        "start_hour", 8, "end_hour", 17,
                        "start_minute", 0, "end_minute", 0,
                        "type", "WORKING_HOURS"));
            }
        }

        if (tree.containsKey("same_department")) {
            if (Boolean.TRUE.equals(tree.get("same_department"))) {
                constraints.put("department", Map.of("type", "SAME_DEPARTMENT", "required", true));
            }
        }

        if (tree.containsKey("same_hospital")) {
            if (Boolean.TRUE.equals(tree.get("same_hospital"))) {
                constraints.put("hospital", Map.of("type", "SAME_HOSPITAL", "required", true));
            }
        }

        // min_position_level: ALLOW rule fires when position_level >= value
        if (tree.containsKey("min_position_level")) {
            constraints.put("position_level", Map.of(
                    "type", "MIN_LEVEL",
                    "value", tree.get("min_position_level")));
        }
        // position_level_less_than: DENY rule fires when position_level < value
        // e.g. "position_level_less_than": 3 → deny if position_level < 3
        if (tree.containsKey("position_level_less_than")) {
            constraints.put("position_level", Map.of(
                    "type", "MAX_LEVEL_EXCLUSIVE",
                    "value", tree.get("position_level_less_than")));
        }

        if (tree.containsKey("allow_emergency_override")) {
            if (Boolean.TRUE.equals(tree.get("allow_emergency_override"))) {
                constraints.put("emergency_override", Map.of("enabled", true));
            }
        }

        if (tree.containsKey("allowed_ip_ranges")) {
            @SuppressWarnings("unchecked")
            List<String> ipList = (List<String>) tree.get("allowed_ip_ranges");
            if (ipList != null && !ipList.isEmpty()) {
                constraints.put("ip", Map.of("type", "ALLOWED_RANGES", "ranges", ipList));
            }
        }
        if (tree.containsKey("allowed_ip")) {
            Object allowedIp = tree.get("allowed_ip");
            if (allowedIp != null && !allowedIp.toString().isBlank()) {
                constraints.put("ip", Map.of("type", "ALLOWED_ADDRESS", "address", allowedIp.toString().trim()));
            }
        }

        // Owner constraint: resource.ownerId must equal user.id
        if (tree.containsKey("owner_is_user")) {
            if (Boolean.TRUE.equals(tree.get("owner_is_user"))) {
                constraints.put("owner", Map.of("type", "OWNER_IS_USER", "required", true));
            }
        }

        // Status constraint: resource.status must be in allowed list
        if (tree.containsKey("allowed_resource_status")) {
            @SuppressWarnings("unchecked")
            List<String> statusList = (List<String>) tree.get("allowed_resource_status");
            if (statusList != null && !statusList.isEmpty()) {
                constraints.put("status", Map.of("type", "ALLOWED_STATUS", "values", statusList));
            }
        }

        // CreatedBy constraint: resource.createdBy must equal user.id
        if (tree.containsKey("created_by_is_user")) {
            if (Boolean.TRUE.equals(tree.get("created_by_is_user"))) {
                constraints.put("created_by", Map.of("type", "CREATED_BY_IS_USER", "required", true));
            }
        }

        return constraints;
    }

    @SuppressWarnings("unchecked")
    private void extractConstraintsFromConditionTree(Map<String, Object> node, Map<String, Object> outConstraints) {
        Object childrenObj = node.get("children");
        log.info("[COND-NODE] keys={}, childrenType={}", node.keySet(),
                childrenObj != null ? childrenObj.getClass().getName() : null);
        if (childrenObj instanceof List) {
            for (Object c : (List<?>) childrenObj) {
                if (c instanceof Map) {
                    Map<String, Object> child = (Map<String, Object>) c;
                    if (child.containsKey("children")) {
                        extractConstraintsFromConditionTree(child, outConstraints);
                    } else if (child.containsKey("field")) {
                        applyLeafCondition(child, outConstraints);
                    }
                }
            }
        }
    }

    private void applyLeafCondition(Map<String, Object> leaf, Map<String, Object> outConstraints) {
        String field = leaf.get("field") != null ? leaf.get("field").toString() : "";
        Object value = leaf.get("value");
        log.info("[COND-LEAF] field={}, operator={}, value={}", leaf.get("field"), leaf.get("operator"), value);
        if (value == null)
            return;

        if ("env.time".equals(field)) {
            String timeStr = value.toString().trim();
            if (timeStr.contains("-")) {
                Map<String, Object> timeConstraint = parseTimeRange(timeStr);
                if (!timeConstraint.isEmpty() && !outConstraints.containsKey("time")) {
                    outConstraints.put("time", timeConstraint);
                }
            }
        } else if ("env.ip".equals(field)) {
            String ipStr = value.toString().trim();
            if (ipStr.isEmpty())
                return;
            if (ipStr.contains(",")) {
                List<String> ips = Arrays.stream(ipStr.split(","))
                        .map(String::trim).filter(s -> !s.isEmpty()).toList();
                if (!ips.isEmpty() && !outConstraints.containsKey("ip")) {
                    outConstraints.put("ip", Map.of("type", "ALLOWED_RANGES", "ranges", ips));
                }
            } else {
                if (!outConstraints.containsKey("ip")) {
                    outConstraints.put("ip", Map.of("type", "ALLOWED_ADDRESS", "address", ipStr));
                }
            }
        } else if ("resource.ownerId".equals(field)) {
            // "eq" operator with value "user.id" → OWNER_IS_USER
            if (!outConstraints.containsKey("owner")) {
                outConstraints.put("owner", Map.of("type", "OWNER_IS_USER", "required", true));
            }
        } else if ("resource.status".equals(field)) {
            // value is comma-separated or single status
            String statusStr = value.toString().trim();
            List<String> statuses = Arrays.stream(statusStr.split(","))
                    .map(String::trim).filter(s -> !s.isEmpty()).toList();
            if (!statuses.isEmpty() && !outConstraints.containsKey("status")) {
                outConstraints.put("status", Map.of("type", "ALLOWED_STATUS", "values", statuses));
            }
        } else if ("resource.createdBy".equals(field)) {
            if (!outConstraints.containsKey("created_by")) {
                outConstraints.put("created_by", Map.of("type", "CREATED_BY_IS_USER", "required", true));
            }
        } else if ("resource.departmentId".equals(field)) {
            // departmentId == user.departmentId → SAME_DEPARTMENT
            if (!outConstraints.containsKey("department")) {
                outConstraints.put("department", Map.of("type", "SAME_DEPARTMENT", "required", true));
            }
        } else if ("resource.id".equals(field)) {
            // resource_id: id of current resource (patient_id, appointment_id, etc.) —
            // depends on resource type from path
            String op = leaf.get("operator") != null ? leaf.get("operator").toString() : "";
            if ("eq".equals(op) && value != null && !value.toString().isBlank()) {
                if (!outConstraints.containsKey("resource_id")) {
                    outConstraints.put("resource_id",
                            Map.of("type", "RESOURCE_ID_EQUALS", "value", value.toString().trim()));
                }
            } else if ("in".equals(op) && value != null) {
                List<String> ids = new ArrayList<>();
                if (value instanceof List) {
                    for (Object o : (List<?>) value) {
                        if (o != null)
                            ids.add(o.toString().trim());
                    }
                } else {
                    String s = value.toString().trim();
                    if (s.contains(",")) {
                        ids = Arrays.stream(s.split(",")).map(String::trim).filter(x -> !x.isEmpty()).toList();
                    } else if (!s.isEmpty()) {
                        ids.add(s);
                    }
                }
                if (!ids.isEmpty() && !outConstraints.containsKey("resource_id")) {
                    outConstraints.put("resource_id", Map.of("type", "RESOURCE_ID_IN", "allowed_ids", ids));
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Time parsing
    // -------------------------------------------------------------------------

    private Map<String, Object> parseTimeRange(String timeRange) {
        try {
            if (timeRange == null || !timeRange.contains("-"))
                return Collections.emptyMap();
            String[] parts = timeRange.split("-");
            if (parts.length != 2)
                return Collections.emptyMap();
            String startPart = parts[0].trim();
            String endPart = parts[1].trim();
            Map<String, Object> out = new HashMap<>();
            out.put("type", "TIME_RANGE");
            out.put("start_hour", parseHour(startPart));
            out.put("end_hour", parseHour(endPart));
            out.put("start_minute", parseMinute(startPart));
            out.put("end_minute", parseMinute(endPart));
            return out;
        } catch (Exception e) {
            log.warn("Failed to parse time range '{}': {}", timeRange, e.getMessage());
            return Collections.emptyMap();
        }
    }

    private int parseHour(String timePart) {
        return Integer.parseInt(timePart.split(":")[0].trim());
    }

    private int parseMinute(String timePart) {
        String[] t = timePart.split(":");
        return t.length > 1 ? Integer.parseInt(t[1].trim()) : 0;
    }

    // -------------------------------------------------------------------------
    // JSON parsing helpers
    // -------------------------------------------------------------------------

    private Map<String, Object> parseJsonField(String json) {
        if (json == null || json.isBlank())
            return new HashMap<>();
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            log.warn("Failed to parse JSON field: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    private List<String> parseJsonFieldAsList(String json) {
        if (json == null || json.isBlank())
            return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            log.warn("Failed to parse JSON list: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
}
