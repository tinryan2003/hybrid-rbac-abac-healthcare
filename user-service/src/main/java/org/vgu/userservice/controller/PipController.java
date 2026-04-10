package org.vgu.userservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.vgu.userservice.service.PipService;

import java.util.Map;

/**
 * Policy Information Point (PIP) - Subject attributes for ABAC.
 * Returns subject (user) attributes by Keycloak ID for authorization decisions.
 */
@RestController
@RequestMapping("/users/pip")
@RequiredArgsConstructor
@Slf4j
public class PipController {

    private final PipService pipService;

    /**
     * Get subject attributes for the user identified by Keycloak ID.
     * Used by authorization-service to enrich subject (department, hospital, position_level, job_title).
     *
     * @param keycloakUserId Keycloak subject (sub) from JWT
     * @return Map with keys: department_id, hospital_id, position_level, job_title
     */
    @GetMapping("/subject/{keycloakUserId}")
    public ResponseEntity<Map<String, Object>> getSubjectAttributes(@PathVariable String keycloakUserId) {
        log.debug("PIP: get subject attributes for keycloakUserId={}", keycloakUserId);
        return pipService.getSubjectAttributes(keycloakUserId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get resource attributes for a user record (when the user profile IS the resource being accessed).
     * Used by authorization-service when object="user" or "staff_record".
     * Returns owner_id, department_id, hospital_id, sensitivity_level for access control.
     *
     * @param keycloakUserId Keycloak user ID of the target user record
     */
    @GetMapping("/resource/{keycloakUserId}")
    public ResponseEntity<Map<String, Object>> getResourceAttributes(@PathVariable String keycloakUserId) {
        log.debug("PIP: get resource attributes for user keycloakUserId={}", keycloakUserId);
        return pipService.getResourceAttributes(keycloakUserId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
