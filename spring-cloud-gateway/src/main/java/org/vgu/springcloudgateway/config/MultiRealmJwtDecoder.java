package org.vgu.springcloudgateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * JWT Decoder that supports multiple Keycloak realms.
 * Validates signature and exp/nbf via Nimbus; optionally checks Keycloak token
 * introspection so that revoked/expired sessions are rejected even if JWT exp has not passed.
 */
@Slf4j
@Component
public class MultiRealmJwtDecoder implements ReactiveJwtDecoder {

    private final List<ReactiveJwtDecoder> jwtDecoders;
    private final List<String> realmNames;
    private final KeycloakIntrospectionService introspectionService;

    public MultiRealmJwtDecoder(KeycloakIntrospectionService introspectionService) {
        this.introspectionService = introspectionService;
        this.jwtDecoders = new ArrayList<>();
        this.realmNames = new ArrayList<>();

        // Hospital Realm (main realm for Hospital Management System)
        String hospitalIssuer = "http://localhost:8180/realms/hospital-realm";
        ReactiveJwtDecoder hospitalDecoder = NimbusReactiveJwtDecoder
                .withJwkSetUri(hospitalIssuer + "/protocol/openid-connect/certs")
                .build();
        jwtDecoders.add(hospitalDecoder);
        realmNames.add("hospital-realm");
        log.info("Initialized JWT decoder for hospital-realm");
    }

    @Override
    public reactor.core.publisher.Mono<Jwt> decode(String token) throws JwtException {
        return tryDecodeWithRealms(token, 0);
    }

    /**
     * Recursively tries to decode JWT with each realm decoder
     */
    private reactor.core.publisher.Mono<Jwt> tryDecodeWithRealms(String token, int index) {
        if (index >= jwtDecoders.size()) {
            // All decoders failed
            log.error("Failed to decode JWT with all {} decoders. Token may be invalid or expired.", jwtDecoders.size());
            return reactor.core.publisher.Mono.error(
                new JwtException("Unable to decode JWT token with hospital-realm")
            );
        }

        ReactiveJwtDecoder decoder = jwtDecoders.get(index);
        String realmName = realmNames.get(index);

        return decoder.decode(token)
            .doOnSuccess(jwt -> {
                log.info("Successfully decoded JWT from realm: {}", realmName);
                log.debug("JWT issuer: {}, subject: {}", jwt.getIssuer(), jwt.getSubject());
            })
            .flatMap(jwt -> introspectionService.isTokenActive(token)
                .flatMap(active -> {
                    if (Boolean.TRUE.equals(active)) {
                        return reactor.core.publisher.Mono.just(jwt);
                    }
                    log.warn("Keycloak introspection: token not active (session expired or revoked)");
                    return reactor.core.publisher.Mono.error(
                        new JwtException("Token revoked or session expired"));
                }))
            .onErrorResume(error -> {
                // Do not try next decoder if introspection said token revoked/session expired
                if (error instanceof JwtException
                        && error.getMessage() != null
                        && error.getMessage().contains("revoked")) {
                    return reactor.core.publisher.Mono.error(error);
                }
                log.debug("Failed to decode JWT with {} realm decoder: {} - {}",
                    realmName, error.getClass().getSimpleName(), error.getMessage());
                return tryDecodeWithRealms(token, index + 1);
            });
    }
}

