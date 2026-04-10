package org.vgu.policyservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.vgu.policyservice.model.Policy;
import org.vgu.policyservice.repository.PolicyRepository;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OPA Sync Service - Synchronizes policies from Database to OPA runtime
 * 
 * This service implements the "Policy as Data" pattern for Role-centric RBAC-A:
 * - Policies stored in DB are transformed to OPA-compatible JSON data
 * - Data is pushed to OPA via Data API (/v1/data/...)
 * - OPA's Rego code reads this data dynamically (no restart needed)
 * 
 * Pattern: DB (Source of Truth) → Sync Service → OPA (Policy Engine)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OpaSyncService {

    private final PolicyRepository policyRepository;
    private final OpaDataTransformer opaDataTransformer;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${opa.url:http://localhost:8181}")
    private String opaUrl;

    @Value("${opa.data-path-prefix:/v1/data/dynamic_policies}")
    private String dataPathPrefix;

    @Value("${opa.sync.enabled:true}")
    private boolean syncEnabled;

    @Value("${opa.sync.on-startup:true}")
    private boolean syncOnStartup;

    @Value("${opa.sync.batch-size:50}")
    private int batchSize;

    @Value("${opa.sync.startup-retries:10}")
    private int startupRetries;

    @Value("${opa.sync.startup-retry-delay-ms:3000}")
    private long startupRetryDelayMs;

    @Value("${opa.sync.interval-ms:300000}")
    private long syncIntervalMs;

    /**
     * Sync all enabled policies to OPA on service startup.
     * Retries with backoff if OPA is not ready (e.g. policy-service starts before
     * OPA).
     */
    @PostConstruct
    public void init() {
        if (!syncEnabled) {
            log.warn("[OPA] OPA sync is DISABLED. Policies will not be synced to OPA.");
            return;
        }

        log.info("[OPA] OPA Sync Service initialized: url={}, dataPath={}", opaUrl, dataPathPrefix);

        if (!syncOnStartup) {
            log.info("[OPA] OPA sync on startup is disabled. Use manual resync or wait for periodic sync.");
            return;
        }

        // Retry until OPA is reachable (handles policy-service starting before OPA)
        int attempt = 0;
        while (attempt < startupRetries) {
            if (isOpaHealthy()) {
                log.info("[OPA] OPA is ready (attempt {}), starting full sync...", attempt + 1);
                syncAllPoliciesToOpa();
                return;
            }
            attempt++;
            if (attempt < startupRetries) {
                log.warn("[OPA] OPA not reachable at {} (attempt {}/{}). Retrying in {} ms...",
                        opaUrl, attempt, startupRetries, startupRetryDelayMs);
                try {
                    Thread.sleep(startupRetryDelayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("Startup sync interrupted");
                    return;
                }
            }
        }

        log.error("[OPA] OPA server not reachable after {} attempts. Sync skipped. " +
                "Policies will sync on next periodic run or when OPA is up and resync is triggered.", startupRetries);
    }

    /**
     * Periodic full sync so that if OPA was restarted (in-memory data lost),
     * policies are repopulated.
     * Disabled when opa.sync.interval-ms <= 0 (then we sleep to avoid tight loop).
     */
    @Scheduled(fixedDelayString = "${opa.sync.interval-ms:300000}")
    public void scheduledSync() {
        if (!syncEnabled) {
            return;
        }
        if (syncIntervalMs <= 0) {
            try {
                Thread.sleep(60_000); // Disabled: avoid tight loop when fixedDelay is 0
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return;
        }
        if (!isOpaHealthy()) {
            log.debug("OPA not healthy, skipping scheduled sync");
            return;
        }
        log.debug("Running scheduled OPA policy sync");
        syncAllPoliciesToOpa();
    }

    /**
     * Sync all enabled policies to OPA (typically called on startup)
     */
    public void syncAllPoliciesToOpa() {
        if (!syncEnabled) {
            log.debug("OPA sync is disabled, skipping sync all");
            return;
        }

        try {
            List<Policy> policies = policyRepository.findByEnabledWithRules(true);

            if (policies.isEmpty()) {
                log.info("[OPA] No enabled policies found to sync to OPA");
                return;
            }

            log.info("[OPA] Starting sync of {} enabled policies to OPA...", policies.size());

            int successCount = 0;
            int failCount = 0;

            for (Policy policy : policies) {
                try {
                    syncPolicyToOpa(policy, false); // false = don't log individual success
                    successCount++;
                } catch (Exception e) {
                    log.error("[OPA] Failed to sync policy {}: {}", policy.getPolicyId(), e.getMessage());
                    failCount++;
                }
            }

            if (failCount > 0) {
                log.warn("[OPA] Synced {}/{} policies to OPA ({} failed)",
                        successCount, policies.size(), failCount);
            } else {
                log.info("[OPA] Successfully synced all {} policies to OPA", successCount);
            }

        } catch (Exception e) {
            log.error("[OPA] Failed to sync policies to OPA: {}", e.getMessage(), e);
        }
    }

    /**
     * Sync a single policy to OPA
     * Called when policy is created or updated
     */
    public void syncPolicyToOpa(Policy policy) {
        syncPolicyToOpa(policy, true);
    }

    /**
     * Sync a single policy to OPA with optional logging.
     * Supports both single-rule and multi-rule policies.
     * For multi-rule: cleans up old entries before writing new rule entries.
     */
    private void syncPolicyToOpa(Policy policy, boolean logSuccess) {
        if (!syncEnabled) {
            log.debug("OPA sync is disabled, skipping sync for policy {}", policy.getPolicyId());
            return;
        }

        try {
            // Transform to one-or-many OPA entries
            List<Map<String, Object>> entries = opaDataTransformer.transformToOpaEntries(policy);

            // For multi-rule transition: also delete the old single-rule entry
            // (in case this policy was previously single-rule)
            boolean isMultiRule = entries.size() > 1 ||
                    (entries.size() == 1 && !policy.getPolicyId().equals(OpaDataTransformer.getOpaKey(entries.get(0))));
            if (isMultiRule) {
                // Delete any old single-rule entry silently
                deleteOpaKey(policy.getPolicyId(), false);
            }

            for (Map<String, Object> entry : entries) {
                String opaKey = OpaDataTransformer.getOpaKey(entry);
                if (opaKey == null)
                    opaKey = policy.getPolicyId();

                // Guard: remove internal meta key before sending to OPA
                Map<String, Object> opaData = new HashMap<>(entry);
                opaData.remove("_opa_key");

                // Sanitize constraints: remove tree artifacts
                Object constraintsObj = opaData.get("constraints");
                if (constraintsObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> constraintsMap = (Map<String, Object>) constraintsObj;
                    constraintsMap.remove("children");
                    constraintsMap.remove("operator");
                }

                if (log.isInfoEnabled()) {
                    Object constraints = opaData.get("constraints");
                    if (constraints != null) {
                        log.info("[OPA] PUT constraints for '{}': {}", opaKey, constraints);
                    }
                }

                String endpoint = opaUrl + dataPathPrefix + "/" + opaKey;
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Map<String, Object>> request = new HttpEntity<>(opaData, headers);
                restTemplate.exchange(endpoint, HttpMethod.PUT, request, Void.class);

                if (logSuccess) {
                    log.info("[OPA] Synced entry '{}' to OPA", opaKey);
                } else {
                    log.debug("Synced entry '{}' to OPA", opaKey);
                }
            }

        } catch (Exception e) {
            log.error("[OPA] Failed to sync policy '{}' to OPA: {}", policy.getPolicyId(), e.getMessage(), e);
            throw new RuntimeException("Failed to sync policy to OPA: " + e.getMessage(), e);
        }
    }

    /**
     * Delete all OPA entries for a policy (single-rule or multi-rule).
     * Pass the Policy object so we know which rule keys to delete.
     */
    public void deletePolicyFromOpa(Policy policy) {
        if (!syncEnabled) {
            log.debug("OPA sync disabled, skipping delete for policy {}", policy.getPolicyId());
            return;
        }
        // Derive keys the same way sync does
        List<Map<String, Object>> entries;
        try {
            entries = opaDataTransformer.transformToOpaEntries(policy);
        } catch (Exception e) {
            entries = Collections.emptyList();
        }

        // Delete all rule entries
        boolean deletedAny = false;
        for (Map<String, Object> entry : entries) {
            String key = OpaDataTransformer.getOpaKey(entry);
            if (key != null && !key.equals(policy.getPolicyId())) {
                deleteOpaKey(key, true);
                deletedAny = true;
            }
        }
        // Always delete the plain policyId entry (covers single-rule and transition
        // from single→multi)
        deleteOpaKey(policy.getPolicyId(), !deletedAny);
    }

    /**
     * Legacy overload: delete by policyId only (no rule expansion).
     * Used for cases where the Policy entity is already gone from DB.
     */
    public void deletePolicyFromOpa(String policyId) {
        deleteOpaKey(policyId, true);
    }

    private void deleteOpaKey(String key, boolean logResult) {
        try {
            String endpoint = opaUrl + dataPathPrefix + "/" + key;
            restTemplate.delete(endpoint);
            if (logResult)
                log.info("[OPA] Deleted '{}' from OPA", key);
        } catch (Exception e) {
            if (logResult)
                log.warn("[OPA] Could not delete '{}' from OPA: {}", key, e.getMessage());
        }
    }

    /**
     * Check if OPA server is healthy and reachable
     */
    public boolean isOpaHealthy() {
        try {
            String healthEndpoint = opaUrl + "/health";
            String response = restTemplate.getForObject(healthEndpoint, String.class);

            // OPA health endpoint returns "{}" for healthy status
            boolean healthy = response != null &&
                    (response.contains("{}") || response.contains("ok"));

            if (healthy) {
                log.debug("[OPA] OPA health check passed");
            } else {
                log.warn("[OPA] OPA health check returned unexpected response: {}", response);
            }
            return healthy;

        } catch (Exception e) {
            log.warn("[OPA] OPA health check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Manually trigger full resync (useful for admin operations)
     */
    public void forceResync() {
        log.info("[OPA] Manual resync triggered");
        syncAllPoliciesToOpa();
    }

    /**
     * Get sync status and statistics
     */
    public Map<String, Object> getSyncStatus() {
        long totalPolicies = policyRepository.count();
        long enabledPolicies = policyRepository.findByEnabled(true).size();
        boolean opaHealthy = isOpaHealthy();

        return Map.of(
                "sync_enabled", syncEnabled,
                "opa_url", opaUrl,
                "opa_healthy", opaHealthy,
                "total_policies", totalPolicies,
                "enabled_policies", enabledPolicies,
                "last_sync", "N/A" // TODO: Track last sync time
        );
    }
}
