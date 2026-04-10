package org.vgu.springcloudgateway.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Converts Keycloak JWT roles to Spring Security GrantedAuthorities
 * Extracts roles from: token.realm_access.roles[] and
 * token.resource_access.{client}.roles[]
 */
public class KeycloakRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(@org.springframework.lang.NonNull Jwt jwt) {
        // Extract realm-level roles
        Collection<GrantedAuthority> realmRoles = extractRealmRoles(jwt);

        // Extract client-level roles (optional)
        Collection<GrantedAuthority> clientRoles = extractClientRoles(jwt, "hospital-client");

        // Combine both (only ADMIN is used; no SYSTEM_ADMIN/HOSPITAL_ADMIN mapping)
        List<GrantedAuthority> allRoles = new java.util.ArrayList<>(realmRoles);
        allRoles.addAll(clientRoles);

        return allRoles;
    }

    /**
     * Extract realm-level roles from: token.realm_access.roles[]
     */
    @SuppressWarnings("unchecked")
    private Collection<GrantedAuthority> extractRealmRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");

        if (realmAccess == null || !realmAccess.containsKey("roles")) {
            return Collections.emptyList();
        }

        List<String> roles = (List<String>) realmAccess.get("roles");

        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                .collect(Collectors.toList());
    }

    /**
     * Extract client-specific roles from: token.resource_access.{clientId}.roles[]
     */
    @SuppressWarnings("unchecked")
    private Collection<GrantedAuthority> extractClientRoles(Jwt jwt, String clientId) {
        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");

        if (resourceAccess == null || !resourceAccess.containsKey(clientId)) {
            return Collections.emptyList();
        }

        Map<String, Object> clientAccess = (Map<String, Object>) resourceAccess.get(clientId);

        if (!clientAccess.containsKey("roles")) {
            return Collections.emptyList();
        }

        List<String> roles = (List<String>) clientAccess.get("roles");

        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                .collect(Collectors.toList());
    }
}
