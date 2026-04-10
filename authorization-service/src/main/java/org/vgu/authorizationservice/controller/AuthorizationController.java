package org.vgu.authorizationservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.vgu.authorizationservice.model.AuthorizationRequest;
import org.vgu.authorizationservice.model.AuthorizationResult;
import org.vgu.authorizationservice.service.AuditEventPublisher;
import org.vgu.authorizationservice.service.OpaService;
import org.vgu.authorizationservice.service.PipEnrichmentService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Authorization API: evaluates requests against OPA (hospital.authz).
 * Grant/permission is determined entirely by OPA policies (dynamic + static);
 * this service only builds input, calls OPA, and returns the decision
 * (allow/deny + reason + obligations).
 */
@Slf4j
@RestController
@RequestMapping("/api/authorization")
@RequiredArgsConstructor
public class AuthorizationController {

    private final OpaService opaService;
    private final AuditEventPublisher auditEventPublisher;
    private final PipEnrichmentService pipEnrichmentService;

    /**
     * Single authorization check: OPA decides allow/deny from policies.
     * IP: from request body (gateway) or fallback to incoming request
     * (X-Forwarded-For / remote).
     * Time: from body or server now (HH:mm). Only time-of-day is used;
     * date/day_of_week can be added later.
     */
    @PostMapping("/check")
    public ResponseEntity<AuthorizationResult> authorize(
            @Valid @RequestBody AuthorizationRequest request,
            HttpServletRequest httpRequest) {

        // Backend fallback: if IP not in body (e.g. direct call), take from incoming
        // request
        if (request.getIp() == null || request.getIp().isBlank()) {
            String fromRequest = getClientIpFromRequest(httpRequest);
            if (fromRequest != null) {
                request.setIp(fromRequest);
            }
        }
        // Time: if not in body, use server time (authorization-service)
        if (request.getTime() == null || request.getTime().isBlank()) {
            request.setTime(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        }

        long startTime = System.currentTimeMillis();
        log.info(
                "🔐 Authorization check request: subject={}, role={}, object={}, action={}, ip={}, time={}",
                request.getSubject(), request.getRole(), request.getObject(), request.getAction(),
                request.getIp(), request.getTime());

        try {
            // 1. PIP: Enrich subject (user) and resource attributes IN PARALLEL with caching
            pipEnrichmentService.enrichRequest(request);

            // 2. Build OPA input
            Map<String, Object> opaInput = buildOpaInput(request);
            log.debug(
                    "OPA input built: user.role={}, resource.object={}, resource.action={}, context.ipAddress={}, context.hour={}, context.minute={}",
                    opaInput.get("user") != null ? ((Map<?, ?>) opaInput.get("user")).get("role") : null,
                    opaInput.get("resource") != null ? ((Map<?, ?>) opaInput.get("resource")).get("object") : null,
                    opaInput.get("resource") != null ? ((Map<?, ?>) opaInput.get("resource")).get("action") : null,
                    opaInput.get("context") != null ? ((Map<?, ?>) opaInput.get("context")).get("ipAddress") : null,
                    opaInput.get("context") != null ? ((Map<?, ?>) opaInput.get("context")).get("hour") : null,
                    opaInput.get("context") != null ? ((Map<?, ?>) opaInput.get("context")).get("minute") : null);

            // 3. Evaluate with OPA
            log.info("⚖️ Evaluating with OPA policy...");
            OpaService.AuthorizationResult opaResult = opaService.evaluate(opaInput);

            // 4. Build response
            long duration = System.currentTimeMillis() - startTime;
            AuthorizationResult result = new AuthorizationResult(
                    opaResult.isAllowed(),
                    opaResult.getReason(),
                    request.buildEnvironmentMap(),
                    opaResult.getObligations(),
                    duration);

            // 5. Publish audit event to RabbitMQ (async, fire-and-forget)
            publishAuditEvent(request, opaResult, result);

            if (opaResult.isAllowed()) {
                log.info("✅ Authorization GRANTED: subject={}, role={}, reason={}, obligations={}, duration={}ms",
                        request.getSubject(), request.getRole(), opaResult.getReason(),
                        opaResult.getObligations(), duration);
            } else {
                log.error("❌ Authorization DENIED: subject={}, role={}, reason={}, duration={}ms",
                        request.getSubject(), request.getRole(), opaResult.getReason(), duration);
            }
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Authorization check failed", e);
            return ResponseEntity.internalServerError()
                    .body(AuthorizationResult.deny("Authorization check failed: " + e.getMessage(), new HashMap<>()));
        }
    }

    /**
     * Batch authorization check
     */
    @PostMapping("/check-batch")
    public ResponseEntity<List<AuthorizationResult>> authorizeBatch(
            @Valid @RequestBody List<AuthorizationRequest> requests,
            HttpServletRequest httpRequest) {

        List<AuthorizationResult> results = new java.util.ArrayList<>();
        for (AuthorizationRequest req : requests) {
            AuthorizationResult body = authorize(req, httpRequest).getBody();
            if (body != null) {
                results.add(body);
            }
        }
        return ResponseEntity.ok(results);
    }

    /**
     * Health check (includes OPA health)
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        boolean opaHealthy = opaService.isHealthy();

        return ResponseEntity.ok(Map.of(
                "status", opaHealthy ? "UP" : "DEGRADED",
                "service", "authorization-service",
                "opa", opaHealthy ? "UP" : "DOWN"));
    }

    /**
     * Reload OPA policies. Permission: Admin only (aligned with policy_management
     * in OPA).
     * Grant/permission is decided by OPA; this service only evaluates and returns
     * allow/deny.
     */
    @PostMapping("/policies/reload")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> reloadPolicies() {
        // OPA reloads from mounted volume; this endpoint is a manual trigger for Admin
        return ResponseEntity.ok("OPA policies are auto-reloaded from volume mount");
    }

    /**
     * Build OPA input from authorization request
     * Hospital-specific: includes department_id, hospital_id, ward_id,
     * position_level
     */
    private Map<String, Object> buildOpaInput(AuthorizationRequest request) {
        Map<String, Object> input = new HashMap<>();

        // User attributes (RBAC + ABAC)
        Map<String, Object> user = new HashMap<>();
        user.put("id", request.getSubject());
        user.put("role", request.getRole()); // RBAC - from JWT realm_access.roles

        // Hospital-specific ABAC attributes (from JWT custom attributes)
        if (request.getDepartment() != null) {
            user.put("department_id", request.getDepartment());
            user.put("departmentId", request.getDepartment()); // Policy UI uses camelCase
        }
        if (request.getHospital() != null) {
            user.put("hospital_id", request.getHospital());
            user.put("hospitalId", request.getHospital()); // Policy UI uses camelCase
        }
        if (request.getPosition() != null) {
            user.put("position", request.getPosition());
        }

        // Additional attributes from additionalContext or direct fields
        if (request.getAdditionalContext() != null) {
            Object wardId = request.getAdditionalContext().get("ward_id");
            if (wardId != null) {
                user.put("ward_id", wardId.toString());
            }
            Object positionLevel = request.getAdditionalContext().get("position_level");
            if (positionLevel != null) {
                try {
                    user.put("position_level", Integer.parseInt(positionLevel.toString()));
                } catch (NumberFormatException e) {
                    log.warn("Failed to parse position_level: {}", positionLevel);
                }
            }
        }

        input.put("user", user);

        // Resource attributes
        Map<String, Object> resource = new HashMap<>();
        resource.put("object", request.getObject());
        resource.put("action", request.getAction());

        // Resource attributes (from PIP enrichment or additionalContext)
        if (request.getAdditionalContext() != null) {
            Object resourceDeptId = request.getAdditionalContext().get("resource_department_id");
            if (resourceDeptId != null) {
                resource.put("department_id", resourceDeptId.toString());
                resource.put("departmentId", resourceDeptId.toString()); // Policy UI uses camelCase
            }
            Object resourceHospitalId = request.getAdditionalContext().get("resource_hospital_id");
            if (resourceHospitalId != null) {
                resource.put("hospital_id", resourceHospitalId.toString());
                resource.put("hospitalId", resourceHospitalId.toString()); // Policy UI uses camelCase
            }
            Object ownerId = request.getAdditionalContext().get("owner_id");
            if (ownerId != null) {
                resource.put("ownerId", ownerId.toString()); // Rego owner constraint uses camelCase
            }
            Object resourceCreatedBy = request.getAdditionalContext().get("resource_created_by");
            if (resourceCreatedBy != null) {
                resource.put("createdBy", resourceCreatedBy.toString()); // Rego created_by constraint uses camelCase
            }
            // Sensitivity: NORMAL | HIGH | CRITICAL (from PIP enrichment)
            Object sensitivity = request.getAdditionalContext().get("resource_sensitivity");
            if (sensitivity != null) {
                resource.put("sensitivity", sensitivity.toString());
            }
            // Status: ACTIVE, PENDING, INACTIVE, etc. (for ALLOWED_STATUS constraint)
            Object resourceStatus = request.getAdditionalContext().get("resource_status");
            if (resourceStatus != null) {
                resource.put("status", resourceStatus.toString());
            }
        }

        // Resource ID for PIP enrichment (and policy conditions: resource.id = id of current resource)
        if (request.getResourceId() != null) {
            resource.put("resourceId", request.getResourceId());
            resource.put("id", request.getResourceId()); // alias for policy condition "resource.id"
        }

        input.put("resource", resource);

        // Context attributes (ABAC)
        Map<String, Object> context = new HashMap<>();

        // Time-of-day (env.time): HH:mm + hour/minute for range checks.
        // Date/day_of_week can be added later.
        String timeStr = request.getTime();
        if (timeStr == null || timeStr.isBlank()) {
            timeStr = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        }
        int hour = extractHour(timeStr);
        int minute = extractMinute(timeStr);
        context.put("time", timeStr);
        context.put("hour", hour);
        context.put("minute", minute);
        context.put("workShiftActive", (hour >= 8 && hour < 16));

        // IP: from gateway (body) or backend fallback. Rego uses context.ip for
        // constraints.
        String ip = request.getIp();
        if (ip != null && !ip.isBlank()) {
            context.put("ip", ip);
            context.put("ipAddress", ip);
        }
        if (request.getNetworkZone() != null) {
            context.put("networkZone", request.getNetworkZone());
        }
        if (request.getChannel() != null) {
            context.put("channel", request.getChannel());
        }

        // Emergency flag (for emergency override policy)
        if (request.getAdditionalContext() != null) {
            Object emergency = request.getAdditionalContext().get("emergency");
            if (emergency != null) {
                context.put("emergency", Boolean.parseBoolean(emergency.toString()));
            }
        }

        input.put("context", context);

        log.info(
                "Built Hospital OPA input - user.role={}, user.department_id={}, user.hospital_id={}, resource.object={}, resource.action={}, resource.department_id={}, resource.sensitivity={}, context.workShiftActive={}",
                user.get("role"), user.get("department_id"), user.get("hospital_id"),
                resource.get("object"), resource.get("action"), resource.get("department_id"),
                resource.get("sensitivity"), context.get("workShiftActive"));
        log.debug("Full OPA input: {}", input);
        return input;
    }

    /**
     * Extract hour from time string (HH:mm format)
     */
    private int extractHour(String time) {
        try {
            return Integer.parseInt(time.split(":")[0]);
        } catch (Exception e) {
            log.warn("Failed to parse time: {}", time);
            return 12; // Default to noon
        }
    }

    /**
     * Extract minute from time string (HH:mm format)
     */
    private int extractMinute(String time) {
        try {
            String[] parts = time.split(":");
            if (parts.length > 1) {
                return Integer.parseInt(parts[1]);
            }
            return 0;
        } catch (Exception e) {
            log.warn("Failed to parse minute from time: {}", time);
            return 0;
        }
    }

    /**
     * Get client IP from incoming HTTP request (when not provided in body).
     * Uses X-Forwarded-For (first entry) or getRemoteAddr().
     */
    private String getClientIpFromRequest(HttpServletRequest httpRequest) {
        if (httpRequest == null) {
            return null;
        }
        String forwarded = httpRequest.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            String first = forwarded.split(",")[0].trim();
            if (!first.isEmpty()) {
                return first;
            }
        }
        return httpRequest.getRemoteAddr();
    }

    /**
     * Publish authorization audit event to RabbitMQ
     */
    private void publishAuditEvent(AuthorizationRequest request,
            OpaService.AuthorizationResult opaResult,
            AuthorizationResult result) {
        try {
            // Extract resource ID if available
            Long resourceId = null;
            if (request.getResourceId() != null) {
                try {
                    resourceId = Long.parseLong(request.getResourceId());
                } catch (NumberFormatException e) {
                    // Resource ID might be a string, ignore
                }
            }

            // Build context map for audit (hospital-relevant + optional extras)
            Map<String, Object> context = new HashMap<>();
            if (request.getIp() != null)
                context.put("ipAddress", request.getIp());
            if (request.getTime() != null)
                context.put("time", request.getTime());
            if (request.getChannel() != null)
                context.put("channel", request.getChannel());
            if (request.getNetworkZone() != null)
                context.put("networkZone", request.getNetworkZone());
            if (request.getDepartment() != null)
                context.put("department", request.getDepartment());
            if (request.getHospital() != null)
                context.put("hospital", request.getHospital());
            if (request.getAdditionalContext() != null) {
                context.put("additionalContext", request.getAdditionalContext());
                // Top-level JWT user info for audit-service (when User Service has no record)
                Map<String, Object> addCtx = request.getAdditionalContext();
                if (addCtx.get("email") != null)
                    context.put("email", addCtx.get("email"));
                if (addCtx.get("name") != null)
                    context.put("name", addCtx.get("name"));
                if (addCtx.get("preferred_username") != null)
                    context.put("preferred_username", addCtx.get("preferred_username"));
            }

            // Publish audit event (async, fire-and-forget)
            auditEventPublisher.publishAuthorizationEvent(
                    request.getSubject(),
                    request.getRole(),
                    request.getAction(),
                    request.getObject(), // resourceType
                    resourceId,
                    opaResult.isAllowed(),
                    opaResult.getReason(), // OPA policy reason - this is the key for "why OPA blocked"
                    opaResult.getObligations(),
                    context);
        } catch (Exception e) {
            // Don't fail authorization if audit publishing fails
            log.error("Failed to publish audit event: {}", e.getMessage(), e);
        }
    }
}
