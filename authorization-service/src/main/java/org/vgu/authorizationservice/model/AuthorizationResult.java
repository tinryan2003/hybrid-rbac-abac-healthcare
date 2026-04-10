package org.vgu.authorizationservice.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;
import java.util.List;

@Data
@NoArgsConstructor
public class AuthorizationResult {

    private boolean allowed;
    private String reason;
    private Map<String, Object> context;
    /** OPA obligations (e.g. REQUIRE_APPROVAL). Exposed as "obligations" in API/JSON. */
    private List<String> obligations;
    private long evaluationTimeMs;

    public AuthorizationResult(boolean allowed, String reason) {
        this(allowed, reason, null, null, 0);
    }

    public AuthorizationResult(boolean allowed, String reason, Map<String, Object> context, List<String> obligations, long evaluationTimeMs) {
        this.allowed = allowed;
        this.reason = reason;
        this.context = context;
        this.obligations = obligations;
        this.evaluationTimeMs = evaluationTimeMs;
    }

    public static AuthorizationResult allow(String reason, Map<String, Object> context) {
        return new AuthorizationResult(true, reason, context, null, 0);
    }

    public static AuthorizationResult deny(String reason, Map<String, Object> context) {
        return new AuthorizationResult(false, reason, context, null, 0);
    }
}