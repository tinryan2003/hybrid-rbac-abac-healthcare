package org.vgu.springcloudgateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

import java.time.Duration;

/**
 * Advanced route configuration with circuit breakers and retry logic
 * Supplements the basic routes defined in application.properties
 */
@Configuration
public class GatewayRoutesConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // Banking Backend Route with Circuit Breaker
                // Note: /api/transactions/** is handled by transaction-service route in application.yml
                // Note: /api/accounts/** is handled by account-service route in application.yml
                .route("banking-backend-with-cb", r -> r
                        .path("/api/transfers/**")
                        .filters(f -> f
                                .stripPrefix(1)
                                .circuitBreaker(config -> config
                                        .setName("backendCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/backend"))
                                .retry(config -> config
                                        .setRetries(3)
                                        .setMethods(HttpMethod.GET)
                                        .setBackoff(Duration.ofMillis(100), Duration.ofMillis(1000), 2, true))
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(redisRateLimiter())))
                        .uri("http://localhost:8082"))
                .build();
    }

    /**
     * Redis-based rate limiter bean
     */
    @Bean
    public org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter redisRateLimiter() {
        return new org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter(
                60, // replenishRate: tokens per second
                100 // burstCapacity: maximum tokens
        );
    }
}
