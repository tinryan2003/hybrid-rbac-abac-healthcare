package org.vgu.auditservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * Feign Client for User Service
 * Used to fetch employee details (email, employeeNumber) by keycloakId
 */
@FeignClient(name = "user-service", url = "${services.user-service.url:http://localhost:8200}")
public interface UserServiceClient {

    /**
     * Get user profile by Keycloak ID
     * 
     * @param keycloakUserId Keycloak user ID (UUID)
     * @return User profile with employeeNumber, email, etc.
     */
    @GetMapping("/users/keycloak/{keycloakUserId}")
    Map<String, Object> getUserProfileByKeycloakId(@PathVariable("keycloakUserId") String keycloakUserId);
}
