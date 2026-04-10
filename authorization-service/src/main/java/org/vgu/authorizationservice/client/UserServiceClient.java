package org.vgu.authorizationservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * Policy Information Point (PIP) - Subject attributes.
 * Fetches subject (user) attributes by Keycloak ID for ABAC.
 */
@FeignClient(name = "user-service", url = "${services.user-service.url}")
public interface UserServiceClient {

    /**
     * PIP Subject: get attributes for the user (department_id, hospital_id, position_level, job_title).
     * Endpoint: GET /users/pip/subject/{keycloakUserId}
     */
    @GetMapping("/users/pip/subject/{keycloakUserId}")
    ResponseEntity<Map<String, Object>> getSubjectAttributes(@PathVariable("keycloakUserId") String keycloakUserId);

    /**
     * PIP Resource: get resource attributes when a user record is the ACCESS TARGET.
     * Endpoint: GET /users/pip/resource/{keycloakUserId}
     */
    @GetMapping("/users/pip/resource/{keycloakUserId}")
    ResponseEntity<Map<String, Object>> getResourceAttributes(@PathVariable("keycloakUserId") String keycloakUserId);
}