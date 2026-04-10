package org.vgu.userservice.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.vgu.userservice.config.KeycloakProperties;
import org.vgu.userservice.exception.KeycloakIntegrationException;

import java.util.*;

/**
 * Service to interact with Keycloak Admin REST API
 * Handles user creation, role assignment, and attribute management
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakAdminService {

    private final KeycloakProperties keycloakProperties;
    private final RestTemplate restTemplate;

    /**
     * Get admin access token from Keycloak
     */
    private String getAdminAccessToken() {
        try {
            String tokenUrl = keycloakProperties.getServerUrl() + "/realms/master/protocol/openid-connect/token";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "password");
            body.add("client_id", keycloakProperties.getClientId());
            body.add("username", keycloakProperties.getAdminUsername());
            body.add("password", keycloakProperties.getAdminPassword());
            
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
            ResponseEntity<TokenResponse> response = restTemplate.postForEntity(tokenUrl, request, TokenResponse.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody().getAccessToken();
            }
            
            throw new KeycloakIntegrationException("Failed to obtain admin access token");
        } catch (Exception e) {
            log.error("Error obtaining Keycloak admin token", e);
            throw new KeycloakIntegrationException("Failed to obtain admin access token: " + e.getMessage(), e);
        }
    }

    /**
     * Create user in Keycloak with role and attributes
     * 
     * @param username Username for Keycloak
     * @param email Email address
     * @param password Password (will be set as temporary if null)
     * @param firstName First name
     * @param lastName Last name
     * @param role Role to assign (e.g., "DOCTOR", "PATIENT", "NURSE")
     * @param attributes Custom attributes (only hospital_id; Keycloak mappers: username, email, firstName, lastName, hospital_id)
     * @return Keycloak user ID
     */
    public String createUser(String username, String email, String password, 
                             String firstName, String lastName, String role,
                             Map<String, String> attributes) {
        try {
            String accessToken = getAdminAccessToken();
            String usersUrl = keycloakProperties.getServerUrl() + "/admin/realms/" + 
                             keycloakProperties.getRealm() + "/users";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);
            
            // Build user representation
            Map<String, Object> userRepresentation = new HashMap<>();
            userRepresentation.put("username", username);
            userRepresentation.put("email", email);
            userRepresentation.put("firstName", firstName);
            userRepresentation.put("lastName", lastName);
            userRepresentation.put("enabled", true);
            userRepresentation.put("emailVerified", false);
            
            // Set password
            if (password != null && !password.isEmpty()) {
                Map<String, Object> credentials = new HashMap<>();
                credentials.put("type", "password");
                credentials.put("value", password);
                credentials.put("temporary", false);
                userRepresentation.put("credentials", Arrays.asList(credentials));
            } else {
                // Set temporary password
                Map<String, Object> credentials = new HashMap<>();
                credentials.put("type", "password");
                credentials.put("value", generateTemporaryPassword());
                credentials.put("temporary", true);
                userRepresentation.put("credentials", Arrays.asList(credentials));
            }
            
            // Set custom attributes
            if (attributes != null && !attributes.isEmpty()) {
                Map<String, List<String>> userAttributes = new HashMap<>();
                for (Map.Entry<String, String> entry : attributes.entrySet()) {
                    userAttributes.put(entry.getKey(), Arrays.asList(entry.getValue()));
                }
                userRepresentation.put("attributes", userAttributes);
            }
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(userRepresentation, headers);
            ResponseEntity<Void> response = restTemplate.postForEntity(usersUrl, request, Void.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                // Get user ID from Location header
                String location = response.getHeaders().getFirst(HttpHeaders.LOCATION);
                if (location != null) {
                    String userId = location.substring(location.lastIndexOf('/') + 1);
                    log.info("Created Keycloak user: {} with ID: {}", username, userId);
                    
                    // Assign role
                    if (role != null && !role.isEmpty()) {
                        assignRole(userId, role, accessToken);
                    }
                    
                    return userId;
                }
            }
            
            throw new KeycloakIntegrationException("Failed to create user in Keycloak. Status: " + response.getStatusCode());
        } catch (RestClientException e) {
            log.error("Error creating user in Keycloak", e);
            throw new KeycloakIntegrationException("Failed to create user in Keycloak: " + e.getMessage(), e);
        }
    }

    /**
     * Assign realm role to user
     */
    private void assignRole(String userId, String roleName, String accessToken) {
        try {
            // Get role representation
            String rolesUrl = keycloakProperties.getServerUrl() + "/admin/realms/" + 
                             keycloakProperties.getRealm() + "/roles/" + roleName;
            
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            
            ResponseEntity<RoleRepresentation> roleResponse = restTemplate.exchange(
                rolesUrl, HttpMethod.GET, new HttpEntity<>(headers), RoleRepresentation.class);
            
            if (roleResponse.getStatusCode().is2xxSuccessful() && roleResponse.getBody() != null) {
                RoleRepresentation role = roleResponse.getBody();
                
                // Assign role to user
                String userRolesUrl = keycloakProperties.getServerUrl() + "/admin/realms/" + 
                                     keycloakProperties.getRealm() + "/users/" + userId + "/role-mappings/realm";
                
                HttpHeaders assignHeaders = new HttpHeaders();
                assignHeaders.setContentType(MediaType.APPLICATION_JSON);
                assignHeaders.setBearerAuth(accessToken);
                
                HttpEntity<List<RoleRepresentation>> assignRequest = new HttpEntity<>(
                    Arrays.asList(role), assignHeaders);
                
                ResponseEntity<Void> assignResponse = restTemplate.postForEntity(
                    userRolesUrl, assignRequest, Void.class);
                
                if (assignResponse.getStatusCode().is2xxSuccessful()) {
                    log.info("Assigned role {} to user {}", roleName, userId);
                } else {
                    log.warn("Failed to assign role {} to user {}. Status: {}", 
                            roleName, userId, assignResponse.getStatusCode());
                }
            } else {
                log.warn("Role {} not found in Keycloak realm", roleName);
            }
        } catch (Exception e) {
            log.error("Error assigning role {} to user {}", roleName, userId, e);
            // Don't throw exception - role assignment failure shouldn't fail user creation
        }
    }

    /**
     * Generate temporary password
     */
    private String generateTemporaryPassword() {
        return UUID.randomUUID().toString().substring(0, 12) + "!Aa1";
    }

    /**
     * Delete user from Keycloak by Keycloak user ID.
     * Keycloak Admin API: DELETE /admin/realms/{realm}/users/{id}
     *
     * @param keycloakUserId Keycloak user ID (subject from JWT)
     * @throws KeycloakIntegrationException if delete fails or user not found
     */
    public void deleteUser(String keycloakUserId) {
        try {
            String accessToken = getAdminAccessToken();
            String userUrl = keycloakProperties.getServerUrl() + "/admin/realms/" +
                    keycloakProperties.getRealm() + "/users/" + keycloakUserId;

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);

            ResponseEntity<Void> response = restTemplate.exchange(
                    userUrl,
                    HttpMethod.DELETE,
                    new HttpEntity<>(headers),
                    Void.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Deleted Keycloak user: {}", keycloakUserId);
            } else {
                throw new KeycloakIntegrationException("Failed to delete user in Keycloak. Status: " + response.getStatusCode());
            }
        } catch (RestClientException e) {
            if (e instanceof org.springframework.web.client.HttpClientErrorException httpError
                    && httpError.getStatusCode() != null
                    && httpError.getStatusCode().value() == 404) {
                log.warn("Keycloak user not found (already deleted?): {}", keycloakUserId);
                return;
            }
            log.error("Error deleting user from Keycloak: {}", keycloakUserId, e);
            throw new KeycloakIntegrationException("Failed to delete user in Keycloak: " + e.getMessage(), e);
        }
    }

    /**
     * Update user attributes in Keycloak
     */
    public void updateUserAttributes(String keycloakUserId, Map<String, String> attributes) {
        try {
            String accessToken = getAdminAccessToken();
            String userUrl = keycloakProperties.getServerUrl() + "/admin/realms/" + 
                            keycloakProperties.getRealm() + "/users/" + keycloakUserId;
            
            // Get current user
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            ResponseEntity<UserRepresentation> getUserResponse = restTemplate.exchange(
                userUrl, HttpMethod.GET, new HttpEntity<>(headers), UserRepresentation.class);
            
            if (getUserResponse.getStatusCode().is2xxSuccessful() && getUserResponse.getBody() != null) {
                UserRepresentation user = getUserResponse.getBody();
                
                // Update attributes
                Map<String, List<String>> userAttributes = user.getAttributes() != null ? 
                    new HashMap<>(user.getAttributes()) : new HashMap<>();
                
                for (Map.Entry<String, String> entry : attributes.entrySet()) {
                    userAttributes.put(entry.getKey(), Arrays.asList(entry.getValue()));
                }
                user.setAttributes(userAttributes);
                
                // Update user
                HttpHeaders updateHeaders = new HttpHeaders();
                updateHeaders.setContentType(MediaType.APPLICATION_JSON);
                updateHeaders.setBearerAuth(accessToken);
                
                HttpEntity<UserRepresentation> updateRequest = new HttpEntity<>(user, updateHeaders);
                restTemplate.put(userUrl, updateRequest);
                
                log.info("Updated attributes for Keycloak user: {}", keycloakUserId);
            }
        } catch (Exception e) {
            log.error("Error updating user attributes in Keycloak", e);
            throw new KeycloakIntegrationException("Failed to update user attributes: " + e.getMessage(), e);
        }
    }

    // Inner classes for Keycloak API responses
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class TokenResponse {
        @JsonProperty("access_token")
        private String accessToken;
        
        @JsonProperty("token_type")
        private String tokenType;
        
        @JsonProperty("expires_in")
        private Integer expiresIn;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class RoleRepresentation {
        private String id;
        private String name;
        private String description;
        private Boolean composite;
        private Boolean clientRole;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class UserRepresentation {
        private String id;
        private String username;
        private String email;
        private String firstName;
        private String lastName;
        private Boolean enabled;
        private Map<String, List<String>> attributes;
    }
}
