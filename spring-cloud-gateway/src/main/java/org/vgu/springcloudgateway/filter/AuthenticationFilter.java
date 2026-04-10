package org.vgu.springcloudgateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

/**
 * Global filter to enrich downstream requests with user authentication details
 * Extracts JWT claims and adds custom headers for backend services
 */
@Slf4j
@Component
public class AuthenticationFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .flatMap(authentication -> {
                    if (authentication instanceof JwtAuthenticationToken jwtAuth) {
                        Jwt jwt = jwtAuth.getToken();

                        // Extract user information from JWT
                        String userId = jwt.getClaimAsString("sub");
                        String username = jwt.getClaimAsString("preferred_username");
                        String email = jwt.getClaimAsString("email");

                        // Extract roles
                        String roles = authentication.getAuthorities().stream()
                                .map(GrantedAuthority::getAuthority)
                                .collect(Collectors.joining(","));

                        // Log authentication details
                        log.info("Authenticated request: user={}, roles={}, path={}",
                                username, roles, exchange.getRequest().getPath());

                        // Add custom headers for downstream services
                        ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                                .header("X-User-Id", userId != null ? userId : "")
                                .header("X-Username", username != null ? username : "")
                                .header("X-User-Email", email != null ? email : "")
                                .header("X-User-Roles", roles)
                                .build();

                        return chain.filter(exchange.mutate().request(modifiedRequest).build());
                    }

                    // No authentication present, continue with original request
                    return chain.filter(exchange);
                })
                .switchIfEmpty(chain.filter(exchange)); // Handle case when no security context
    }

    @Override
    public int getOrder() {
        return -100; // Execute early in filter chain
    }
}
