package org.vgu.policyservice.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.vgu.policyservice.dto.ConflictDetectionResult;
import org.vgu.policyservice.dto.PolicyBuilderRequest;
import org.vgu.policyservice.dto.PolicyCreateUpdateRequest;
import org.vgu.policyservice.exception.PolicyConflictException;
import org.vgu.policyservice.model.Policy;
import org.vgu.policyservice.service.PolicyCrudService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST API to create, list, get, update, and delete IAM-style policies.
 * Requires ADMIN for write operations.
 */
@Slf4j
@RestController
@RequestMapping("/policies")
@RequiredArgsConstructor
public class PolicyController {

    private final PolicyCrudService policyCrudService;

    /**
     * Create a new policy.
     * POST /api/policies
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> create(@Valid @RequestBody PolicyCreateUpdateRequest request) {
        log.info("[POLICY] >>> POST /policies | policyId={}", request.getPolicyId());
        try {
            Policy created = policyCrudService.create(request);
            log.info("[POLICY] <<< POST /policies 201 | policyId={}, id={}", created.getPolicyId(), created.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            log.warn("[POLICY] <<< POST /policies 400 | error={}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (PolicyConflictException e) {
            ConflictDetectionResult report = e.getConflictReport();
            log.warn("[POLICY] <<< POST /policies 409 | conflicts={}", report.getConflictCount());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "error", "Policy would introduce authorization conflicts. Resolve AUTH_CONFLICT before saving.",
                    "conflictReport", report));
        }
    }

    /**
     * List all policies, optionally filtered by tenant and enabled.
     * GET /api/policies?tenantId=HOSPITAL_A&enabled=true
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Policy>> list(
            @RequestParam(required = false) String tenantId,
            @RequestParam(required = false) Boolean enabled) {
        log.info("[POLICY] >>> GET /policies | tenantId={}, enabled={}", tenantId, enabled);
        List<Policy> list = policyCrudService.findAll(tenantId, enabled);
        log.info("[POLICY] <<< GET /policies 200 | count={}", list.size());
        return ResponseEntity.ok(list);
    }

    /**
     * Get policy by database ID.
     * GET /api/policies/1
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Policy> getById(@PathVariable Long id) {
        log.info("[POLICY] >>> GET /policies/{}", id);
        ResponseEntity<Policy> result = policyCrudService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
        log.info("[POLICY] <<< GET /policies/{} {}", id, result.getStatusCode().value());
        return result;
    }

    /**
     * Get policy by policyId (business key).
     * GET /api/policies/by-policy-id/policy-doctor-view-same-dept
     */
    @GetMapping("/by-policy-id/{policyId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Policy> getByPolicyId(@PathVariable String policyId) {
        log.info("[POLICY] >>> GET /policies/by-policy-id/{}", policyId);
        ResponseEntity<Policy> result = policyCrudService.findByPolicyId(policyId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
        log.info("[POLICY] <<< GET /policies/by-policy-id/{} {}", policyId, result.getStatusCode().value());
        return result;
    }

    /**
     * Update an existing policy.
     * PUT /api/policies/1
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> update(@PathVariable Long id,
            @Valid @RequestBody PolicyCreateUpdateRequest request) {
        log.info("[POLICY] >>> PUT /policies/{} | policyId={}", id, request.getPolicyId());
        try {
            Policy updated = policyCrudService.update(id, request);
            log.info("[POLICY] <<< PUT /policies/{} 200 | policyId={}", id, updated.getPolicyId());
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            log.warn("[POLICY] <<< PUT /policies/{} 400 | error={}", id, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (PolicyConflictException e) {
            ConflictDetectionResult report = e.getConflictReport();
            log.warn("[POLICY] <<< PUT /policies/{} 409 | conflicts={}", id, report.getConflictCount());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "error", "Policy would introduce authorization conflicts. Resolve AUTH_CONFLICT before saving.",
                    "conflictReport", report));
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            log.error("[POLICY] <<< PUT /policies/{} 409 | constraint violation: {}", id, e.getMessage());
            String msg = e.getMessage();
            if (msg != null && msg.contains("Duplicate entry")) {
                msg = "A rule with the same ruleId already exists. Please use a different ruleId.";
            }
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", msg != null ? msg : "Database constraint violation"));
        } catch (Exception e) {
            log.error("[POLICY] <<< PUT /policies/{} 500 | unexpected error: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update policy: " + e.getMessage()));
        }
    }

    /**
     * Delete a policy.
     * DELETE /api/policies/1
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("[POLICY] >>> DELETE /policies/{}", id);
        try {
            policyCrudService.delete(id);
            log.info("[POLICY] <<< DELETE /policies/{} 204", id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.warn("[POLICY] <<< DELETE /policies/{} 404 | error={}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Create policy using builder-style request (frontend-friendly)
     * POST /api/policies/builder
     * 
     * This endpoint provides a more structured and type-safe way to create policies
     * with better validation and clearer constraint definitions
     */
    @PostMapping("/builder")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createWithBuilder(@Valid @RequestBody PolicyBuilderRequest builderRequest) {
        log.info("[POLICY] >>> POST /policies/builder | policyId={}, roles={}", builderRequest.getPolicyId(),
                builderRequest.getTargetRoles());
        try {
            PolicyCreateUpdateRequest request = builderRequest.toCreateUpdateRequest();
            Policy created = policyCrudService.create(request);
            log.info("[POLICY] <<< POST /policies/builder 201 | policyId={}, id={}", created.getPolicyId(),
                    created.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            log.warn("[POLICY] <<< POST /policies/builder 400 | error={}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Create a policy with N generated rules (for load / conflict-detection scale
     * testing).
     * Avoids sending a very large JSON body. Example: POST
     * /api/policies/generate-large?ruleCount=10000
     */
    @PostMapping("/generate-large")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createPolicyWithGeneratedRules(
            @RequestParam(defaultValue = "10000") int ruleCount,
            @RequestParam(required = false) String policyId) {
        String id = policyId != null && !policyId.isBlank() ? policyId : "policy-generated-" + ruleCount + "-rules";
        log.info("[POLICY] >>> POST /policies/generate-large | policyId={}, ruleCount={}", id, ruleCount);
        try {
            Policy created = policyCrudService.createPolicyWithGeneratedRules(id, ruleCount);
            log.info("[POLICY] <<< POST /policies/generate-large 201 | policyId={}, id={}, rules={}",
                    created.getPolicyId(), created.getId(), ruleCount);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "policyId", created.getPolicyId(),
                    "id", created.getId(),
                    "ruleCount", ruleCount));
        } catch (IllegalArgumentException e) {
            log.warn("[POLICY] <<< POST /policies/generate-large 400 | error={}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get policy templates for common scenarios
     * GET /api/policies/templates
     */
    @GetMapping("/templates")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getTemplates(@RequestParam String tenantId) {
        log.info("[POLICY] >>> GET /policies/templates | tenantId={}", tenantId);
        return ResponseEntity.ok(Map.of(
                "working_hours", Map.of(
                        "name", "Working Hours Restriction",
                        "description", "Restricts role to working hours (8-17) with department isolation",
                        "example", PolicyBuilderRequest.workingHoursTemplate(tenantId, "NURSE")),
                "department_isolation", Map.of(
                        "name", "Department Isolation",
                        "description", "Restricts role to same department and hospital only",
                        "example", PolicyBuilderRequest.departmentIsolationTemplate(tenantId, "DOCTOR"))));
    }
}
