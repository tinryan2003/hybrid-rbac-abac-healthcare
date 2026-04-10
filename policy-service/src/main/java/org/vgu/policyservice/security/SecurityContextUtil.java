package org.vgu.policyservice.security;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Minimal helper for extracting actor information from Spring Security.
 *
 * We prefer Keycloak's stable subject (JWT claim "sub") when available.
 */
public final class SecurityContextUtil {

    private SecurityContextUtil() {
    }

    public static Optional<String> currentSubject() {
        Authentication auth = SecurityContextHolder.getContext() != null
                ? SecurityContextHolder.getContext().getAuthentication()
                : null;
        if (auth == null) {
            return Optional.empty();
        }

        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            String sub = jwt.getClaimAsString("sub");
            if (sub != null && !sub.isBlank()) {
                return Optional.of(sub);
            }
        }

        String name = auth.getName();
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(name);
    }
}
