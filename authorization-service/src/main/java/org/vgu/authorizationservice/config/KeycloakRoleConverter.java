package org.vgu.authorizationservice.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class KeycloakRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(KeycloakRoleConverter.class);

        // Extract realm roles
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess instanceof Map<?, ?> ra && ra.get("roles") instanceof List<?> roles) {
            log.debug("Found {} realm roles", roles.size());
            for (Object r : roles) {
                String roleName = String.valueOf(r).toUpperCase();
                String authority = "ROLE_" + roleName;
                authorities.add(new SimpleGrantedAuthority(authority));
                log.debug("Added realm role authority: {}", authority);
            }
        } else {
            log.debug("No realm_access found or invalid format");
        }

        // Extract client roles (from resource_access)
        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
        if (resourceAccess instanceof Map<?, ?> resources) {
            log.debug("Found {} resource access entries", resources.size());
            for (Map.Entry<?, ?> entry : resources.entrySet()) {
                String clientId = String.valueOf(entry.getKey());
                Object clientObj = entry.getValue();
                if (clientObj instanceof Map<?, ?> co && co.get("roles") instanceof List<?> cRoles) {
                    log.debug("Found {} roles for client: {}", cRoles.size(), clientId);
                    for (Object r : cRoles) {
                        String roleName = String.valueOf(r).toUpperCase();
                        // For client roles, add both with and without ROLE_ prefix for flexibility
                        String authority = "ROLE_" + roleName;
                        authorities.add(new SimpleGrantedAuthority(authority));
                        // Also add without ROLE_ prefix for direct role checks (e.g., DOCTOR, NURSE)
                        authorities.add(new SimpleGrantedAuthority(roleName));
                        log.debug("Added client role authority: {} and {}", authority, roleName);
                    }
                }
            }
        } else {
            log.debug("No resource_access found or invalid format");
        }

        log.info("Total authorities extracted: {}", authorities.size());
        log.debug("Authorities: {}", authorities);
        return authorities;
    }
}