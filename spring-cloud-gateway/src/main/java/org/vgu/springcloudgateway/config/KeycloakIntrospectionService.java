package org.vgu.springcloudgateway.config;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Calls Keycloak Token Introspection (RFC 7662) to check if the token is still
 * active (e.g. session not expired/revoked). When enabled, the gateway will
 * reject tokens that Keycloak considers invalid even if JWT exp has not passed.
 *
 * Keycloak: Realm → Client (e.g. gateway-client) → Service account ON,
 * assign role realm-management → introspection to the service account.
 */
@Slf4j
@Component
public class KeycloakIntrospectionService {

    private final WebClient webClient;
    private final boolean enabled;
    private final String introspectionUri;
    private final String clientId;
    private final String clientSecret;

    public KeycloakIntrospectionService(
            WebClient.Builder webClientBuilder,
            @Value("${keycloak.introspection.enabled:false}") boolean enabled,
            @Value("${keycloak.introspection.uri:}") String introspectionUri,
            @Value("${keycloak.introspection.client-id:}") String clientId,
            @Value("${keycloak.introspection.client-secret:}") String clientSecret) {
        this.enabled = enabled;
        this.introspectionUri = introspectionUri != null ? introspectionUri : "";
        this.clientId = clientId != null ? clientId : "";
        this.clientSecret = clientSecret != null ? clientSecret : "";
        if (enabled && (this.introspectionUri.isBlank() || this.clientId.isBlank() || this.clientSecret.isBlank())) {
            log.warn("Keycloak introspection is enabled but uri/client-id/client-secret are missing. Introspection will be skipped.");
            this.webClient = null;
        } else if (enabled) {
            this.webClient = webClientBuilder.build();
            log.info("Keycloak token introspection enabled: {}", this.introspectionUri);
        } else {
            this.webClient = null;
            log.debug("Keycloak token introspection disabled");
        }
    }

    /**
     * Returns true if the token is active in Keycloak (session not expired/revoked).
     * If introspection is disabled or fails (e.g. network), returns true to avoid blocking.
     */
    public Mono<Boolean> isTokenActive(String token) {
        if (!enabled || webClient == null || introspectionUri.isBlank()) {
            return Mono.just(true);
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("token", token);
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);

        return webClient.post()
                .uri(introspectionUri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(form))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(node -> node != null && node.has("active") && node.get("active").asBoolean(false))
                .onErrorResume(e -> {
                    log.warn("Keycloak introspection failed: {} - treating token as active to avoid blocking", e.getMessage());
                    return Mono.just(true);
                })
                .defaultIfEmpty(false);
    }
}
