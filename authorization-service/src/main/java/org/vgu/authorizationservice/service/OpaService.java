package org.vgu.authorizationservice.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * OPA (Open Policy Agent) Service for Hospital (HyARBAC).
 * Evaluates authorization using hospital.authz package; result is a single decision (allow/deny + reason + obligations).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpaService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE_REF = new TypeReference<>() {};

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${opa.url:http://localhost:8181}")
    private String opaUrl;

    @Value("${opa.policy.package:hospital.authz}")
    private String policyPackage;

    private WebClient webClient;

    @PostConstruct
    public void init() {
        this.webClient = webClientBuilder
                .baseUrl(opaUrl != null ? opaUrl : "http://localhost:8181")
                .build();
        // Hospital-specific: always use hospital.authz package
        String pkg = policyPackage != null ? policyPackage : "hospital.authz";
        log.info("OPA Service initialized: url={}, package={}", opaUrl, pkg);

        // Test OPA connection at startup
        try {
            if (isHealthy()) {
                log.info("✅ OPA server is reachable and healthy at {}", opaUrl);
            } else {
                log.warn("⚠️ OPA server is not reachable at {}. Authorization checks will fail until OPA is available!",
                        opaUrl);
            }
        } catch (Exception e) {
            log.error("❌ Failed to connect to OPA server at {}: {}", opaUrl, e.getMessage());
            log.error("Authorization Service will start but authorization checks will fail until OPA is available!");
        }
    }

    /**
     * Evaluate authorization request with OPA
     * 
     * @param input Complete input map with user, resource, and context attributes
     * @return AuthorizationResult with allow/deny decision
     */
    public AuthorizationResult evaluate(Map<String, Object> input) {
        try {
            // Hospital-specific: always use hospital.authz package
            String pkg = policyPackage != null ? policyPackage : "hospital.authz";
            String path = String.format("/v1/data/%s/allow", pkg.replace(".", "/"));

            log.debug("Evaluating OPA policy: path={}, input={}", path, input);

            OpaRequest request = new OpaRequest(input);

            // Use String body + ObjectMapper so "result" array is deserialized as List, not Map
            String body = webClient.post()
                    .uri(path)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (body == null || body.isBlank()) {
                log.warn("OPA returned empty body");
                return AuthorizationResult.deny("OPA evaluation returned empty response", null);
            }

            log.debug("OPA raw body: {}", body.length() > 500 ? body.substring(0, 500) + "..." : body);

            Map<String, Object> response;
            try {
                response = objectMapper.readValue(body, MAP_TYPE_REF);
            } catch (JsonProcessingException e) {
                log.error("OPA response parse error: {}", e.getMessage());
                return AuthorizationResult.deny("OPA response parse error: " + e.getMessage(), null);
            }
            if (response != null && response.containsKey("result")) {
                Object resultObj = response.get("result");
                logOpaResult(resultObj);
                Map<String, Object> decision = firstAllowDecision(resultObj);
                if (decision != null) {
                    Boolean allowedValue = (Boolean) decision.get("allowed");
                    if (allowedValue == null) {
                        allowedValue = (Boolean) decision.get("allow");
                    }
                    boolean allowed = Boolean.TRUE.equals(allowedValue);
                    String reason = (String) decision.get("reason");
                    if (reason == null) {
                        reason = allowed ? "Access granted by OPA policy" : "Access denied by OPA policy";
                    }
                    @SuppressWarnings("unchecked")
                    List<String> obligations = (List<String>) decision.get("obligations");
                    if (obligations == null) {
                        obligations = new ArrayList<>();
                    }

                    AuthorizationResult result = AuthorizationResult.builder()
                            .allowed(allowed)
                            .reason(reason)
                            .obligations(obligations)
                            .build();

                    if (allowed) {
                        log.info("✅ OPA evaluation: ALLOWED - reason={}, obligations={}", reason, obligations);
                    } else {
                        log.error("❌ OPA evaluation: DENIED - reason={}", reason);
                    }
                    return result;
                }
            }

            log.warn("OPA returned null or empty result");
            return AuthorizationResult.deny("OPA evaluation returned null result", null);

        } catch (WebClientResponseException e) {
            String body = e.getResponseBodyAsString();
            log.error("OPA returned {}: {}", e.getStatusCode(), body);
            return AuthorizationResult.deny("OPA service unavailable: " + e.getStatusCode() + " - " + (body != null && !body.isBlank() ? body : e.getMessage()), null);
        } catch (Exception e) {
            log.error("OPA evaluation error", e);
            return AuthorizationResult.deny("OPA service unavailable: " + e.getMessage(), null);
        }
    }

    /**
     * Log raw OPA result for debugging (type, size, and each element's allowed value).
     */
    @SuppressWarnings("unchecked")
    private void logOpaResult(Object resultObj) {
        if (resultObj == null) {
            log.debug("OPA result: null");
            return;
        }
        if (resultObj instanceof List<?> list) {
            log.debug("OPA result: array size={}", list.size());
            for (int i = 0; i < list.size(); i++) {
                Object item = list.get(i);
                if (item instanceof Map) {
                    Object a = ((Map<String, Object>) item).get("allowed");
                    Object r = ((Map<String, Object>) item).get("reason");
                    String reasonSnippet = (r instanceof String s) ? (s.length() > 50 ? s.substring(0, 50) + "..." : s) : String.valueOf(r);
                    log.debug("  [{}] allowed={}, reason={}", i, a, reasonSnippet);
                }
            }
        } else if (resultObj instanceof Map) {
            Map<String, Object> m = (Map<String, Object>) resultObj;
            log.debug("OPA result: single object type={}, size={}, keys={}", m.getClass().getName(), m.size(), m.keySet());
            if (!m.isEmpty()) {
                Object firstKey = m.keySet().iterator().next();
                log.debug("OPA result: first key type={}, value type={}", 
                    firstKey != null ? firstKey.getClass().getName() : "null",
                    m.get(firstKey) != null ? m.get(firstKey).getClass().getName() : "null");
            }
            Object a = m.get("allowed");
            Object allowKey = m.get("allow");
            log.debug("OPA result: allowed={}, allow={}", a, allowKey);
        }
    }

    /**
     * OPA may return result as:
     * - array of decision objects (set from Rego rule),
     * - single decision object (when set has one element, some OPA versions),
     * - document object with key "allow" containing the array (when path returns package).
     * Prefer the first decision with allowed=true (or allow=true); otherwise use the first element.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> firstAllowDecision(Object resultObj) {
        if (resultObj == null) {
            return null;
        }
        // Unwrap: if result is document with "allow" array (e.g. data.hospital.authz), use that array
        if (resultObj instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) resultObj;
            Object allowArray = map.get("allow");
            if (allowArray instanceof List<?> list && !list.isEmpty()) {
                resultObj = list;
            } else if (map.containsKey("allowed") || map.containsKey("allow")) {
                return map;
            } else {
                // Array serialized as Map: try "0","1","2" first, then values(), then keys as decisions
                Map<String, Object> fromArray = firstAllowFromMapByIndices(map);
                if (fromArray == null) {
                    fromArray = firstAllowFromMapValues(map);
                }
                if (fromArray != null) {
                    return fromArray;
                }
                return null;
            }
        }
        if (resultObj instanceof List<?> list && !list.isEmpty()) {
            for (Object item : list) {
                if (item instanceof Map) {
                    Map<String, Object> m = (Map<String, Object>) item;
                    if (Boolean.TRUE.equals(m.get("allowed")) || Boolean.TRUE.equals(m.get("allow"))) {
                        return m;
                    }
                }
            }
            Object first = list.get(0);
            return first instanceof Map ? (Map<String, Object>) first : null;
        }
        return null;
    }

    /**
     * Try to get decisions by index keys "0","1","2" (OPA/Jackson may return array as object with numeric keys).
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> firstAllowFromMapByIndices(Map<String, Object> map) {
        Map<String, Object> firstAny = null;
        for (int i = 0; i < map.size(); i++) {
            String key = String.valueOf(i);
            Object item = map.get(key);
            if (item == null) {
                continue;
            }
            if (!(item instanceof Map)) {
                continue;
            }
            Map<String, Object> m = (Map<String, Object>) item;
            if (firstAny == null) {
                firstAny = m;
            }
            if (Boolean.TRUE.equals(m.get("allowed")) || Boolean.TRUE.equals(m.get("allow"))) {
                return m;
            }
        }
        return firstAny;
    }

    /**
     * When OPA returns set as object (keys are JSON strings of decisions, values are true),
     * parse each key string as JSON and find first with allowed=true.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> firstAllowFromMapValues(Map<String, Object> map) {
        Map<String, Object> firstAny = null;
        
        // Case 1: keys are JSON strings (OPA set serialization)
        for (Object key : map.keySet()) {
            if (!(key instanceof String)) {
                continue;
            }
            String keyStr = (String) key;
            // Check if key looks like JSON object
            if (keyStr.startsWith("{") && keyStr.contains("allowed")) {
                try {
                    Map<String, Object> decision = objectMapper.readValue(keyStr, MAP_TYPE_REF);
                    if (firstAny == null) {
                        firstAny = decision;
                    }
                    if (Boolean.TRUE.equals(decision.get("allowed"))) {
                        log.debug("Found allowed decision in key: {}", keyStr.substring(0, Math.min(80, keyStr.length())));
                        return decision;
                    }
                } catch (JsonProcessingException e) {
                    log.debug("Key '{}' is not valid JSON", keyStr.substring(0, Math.min(50, keyStr.length())));
                }
            }
        }
        
        // Case 2: normal map with values as decisions
        for (Object value : map.values()) {
            if (!(value instanceof Map)) {
                continue;
            }
            Map<String, Object> m = (Map<String, Object>) value;
            if (firstAny == null) {
                firstAny = m;
            }
            if (Boolean.TRUE.equals(m.get("allowed")) || Boolean.TRUE.equals(m.get("allow"))) {
                return m;
            }
        }
        
        return firstAny;
    }

    /**
     * Check if OPA server is healthy
     * OPA health endpoint returns "{}" for healthy status
     */
    public boolean isHealthy() {
        try {
            String response = webClient.get()
                    .uri("/health")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            // OPA health endpoint returns "{}" or contains "ok" in some versions
            boolean healthy = response != null &&
                    (response.contains("{}") ||
                            response.contains("ok") ||
                            response.contains("\"status\":\"ok\""));

            if (healthy) {
                log.debug("OPA health check passed");
            } else {
                log.warn("OPA health check returned unexpected response: {}", response);
            }
            return healthy;
        } catch (Exception e) {
            log.warn("OPA health check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Authorization result from OPA
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthorizationResult {
        private boolean allowed;
        private String reason;
        private List<String> obligations;

        public static AuthorizationResult allow(String reason, List<String> obligations) {
            return new AuthorizationResult(true, reason, obligations);
        }

        public static AuthorizationResult deny(String reason, List<String> obligations) {
            return new AuthorizationResult(false, reason, obligations);
        }
    }

    /**
     * OPA request wrapper
     */
    @Data
    private static class OpaRequest {
        private Map<String, Object> input;

        public OpaRequest(Map<String, Object> input) {
            this.input = input;
        }
    }
}
