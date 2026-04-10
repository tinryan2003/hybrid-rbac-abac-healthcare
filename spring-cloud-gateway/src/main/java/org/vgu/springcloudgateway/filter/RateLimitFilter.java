package org.vgu.springcloudgateway.filter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Rate limiting filter using Redis — keyed per authenticated user (JWT sub).
 * Falls back to IP only when no JWT is present.
 * Can be disabled entirely via ratelimit.enabled=false (recommended for dev).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter implements GlobalFilter, Ordered {

    private final ReactiveStringRedisTemplate redisTemplate;

    @Value("${ratelimit.enabled:false}")
    private boolean enabled;

    @Value("${ratelimit.max-requests:1000}")
    private int maxRequests;

    @Value("${ratelimit.window-minutes:1}")
    private int windowMinutes;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Skip entirely when disabled (dev mode)
        if (!enabled) {
            return chain.filter(exchange);
        }

        String path = exchange.getRequest().getPath().value();
        if (path.startsWith("/actuator") || path.startsWith("/auth")) {
            return chain.filter(exchange);
        }

        // Resolve identifier: JWT sub (per user) → IP (fallback)
        // Use flatMap so we never emit null from map() — null in Reactor = NPE.
        return ReactiveSecurityContextHolder.getContext()
                .flatMap(ctx -> {
                    if (ctx.getAuthentication() instanceof JwtAuthenticationToken jwtAuth) {
                        Jwt jwt = (Jwt) jwtAuth.getToken();
                        String sub = jwt.getSubject();
                        if (sub != null && !sub.isBlank()) {
                            return Mono.just(sub);
                        }
                    }
                    return Mono.empty(); // will trigger switchIfEmpty
                })
                .switchIfEmpty(Mono.fromSupplier(() ->
                        exchange.getRequest().getRemoteAddress() != null
                                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                                : "unknown"
                ))
                .flatMap(identifier -> applyRateLimit(exchange, chain, identifier))
                .onErrorResume(e -> {
                    // Fail open: if Redis/anything breaks, allow the request
                    log.warn("Rate limit error (fail-open): {}", e.getMessage());
                    return chain.filter(exchange);
                });
    }

    private Mono<Void> applyRateLimit(ServerWebExchange exchange, GatewayFilterChain chain, String identifier) {
        String key = "rate_limit:" + identifier;
        Duration window = Duration.ofMinutes(windowMinutes);

        return redisTemplate.opsForValue()
                .increment(key)
                .flatMap(count -> {
                    if (count == 1) {
                        // First request in window — set TTL
                        return redisTemplate.expire(key, window)
                                .then(chain.filter(exchange));
                    }

                    if (count > maxRequests) {
                        log.warn("Rate limit exceeded for '{}': {} > {}", identifier, count, maxRequests);
                        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                        safeSetHeader(exchange, "X-RateLimit-Limit", String.valueOf(maxRequests));
                        safeSetHeader(exchange, "X-RateLimit-Remaining", "0");
                        safeSetHeader(exchange, "Retry-After", String.valueOf(windowMinutes * 60));
                        return exchange.getResponse().setComplete();
                    }

                    safeSetHeader(exchange, "X-RateLimit-Limit", String.valueOf(maxRequests));
                    safeSetHeader(exchange, "X-RateLimit-Remaining", String.valueOf(maxRequests - count));
                    return chain.filter(exchange);
                });
    }

    private void safeSetHeader(ServerWebExchange exchange, String name, String value) {
        try {
            if (!exchange.getResponse().isCommitted()) {
                exchange.getResponse().getHeaders().set(name, value);
            }
        } catch (UnsupportedOperationException ignored) {
            // Headers read-only after response starts — safe to ignore
        }
    }

    @Override
    public int getOrder() {
        return -50;
    }
}
