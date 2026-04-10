package org.vgu.springcloudgateway.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Security Configuration for Spring Cloud Gateway
 * Integrates with Keycloak for JWT token validation and RBAC role extraction
 * Supports multiple realms (employee-portal and customer-portal)
 */
@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {

        private final MultiRealmJwtDecoder multiRealmJwtDecoder;

        @Bean
        public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
                http
                                .csrf(csrf -> csrf.disable()) // Disable CSRF for stateless JWT auth
                                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // Enable CORS
                                .authorizeExchange(exchanges -> exchanges
                                                // Allow OPTIONS requests for CORS preflight
                                                .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                                                // Public endpoints (no authentication required)
                                                .pathMatchers("/actuator/**").permitAll()
                                                .pathMatchers("/auth/**").permitAll()
                                                .pathMatchers("/gateway/**").permitAll()
                                                .pathMatchers("/fallback/**").permitAll()

                                                // Admin endpoints (Hospital realm)
                                                .pathMatchers("/api/admin/**")
                                                .hasRole("ADMIN")

                                                // User management endpoints (admin: create/list users; DOCTOR/NURSE/RECEPTIONIST: list doctors/colleagues)
                                                .pathMatchers("/api/users/**")
                                                .hasAnyRole("ADMIN", "DEPARTMENT_HEAD", "DOCTOR", "NURSE", "RECEPTIONIST")

                                                // Patient endpoints
                                                .pathMatchers("/api/patients/**")
                                                .hasAnyRole("DOCTOR", "NURSE", "RECEPTIONIST", "ADMIN")

                                                // Appointment endpoints
                                                .pathMatchers("/api/appointments/**")
                                                .hasAnyRole("DOCTOR", "NURSE", "RECEPTIONIST", "PATIENT", "ADMIN")

                                                // Lab endpoints
                                                .pathMatchers("/api/lab/**")
                                                .hasAnyRole("DOCTOR", "LAB_TECH", "ADMIN")

                                                // Pharmacy endpoints
                                                .pathMatchers("/api/pharmacy/**")
                                                .hasAnyRole("DOCTOR", "PHARMACIST", "ADMIN")

                                                // Billing endpoints
                                                .pathMatchers("/api/billing/**")
                                                .hasAnyRole("BILLING_CLERK", "ADMIN")

                                                // Audit logs: admin/auditor only (defense-in-depth; PDP also enforces)
                                                .pathMatchers("/api/audit", "/api/audit/**")
                                                .hasAnyRole("ADMIN", "EXTERNAL_AUDITOR")

                                                // Policy CRUD: admin only (defense-in-depth; PDP also enforces)
                                                .pathMatchers("/api/policies", "/api/policies/**")
                                                .hasRole("ADMIN")

                                                // All other API endpoints require authentication (any realm)
                                                .pathMatchers("/api/**").authenticated()

                                                // Allow everything else
                                                .anyExchange().permitAll())
                                .oauth2ResourceServer(oauth2 -> oauth2
                                                .jwt(jwt -> jwt
                                                                .jwtDecoder(multiRealmJwtDecoder)
                                                                .jwtAuthenticationConverter(
                                                                                new ReactiveJwtAuthenticationConverterAdapter(
                                                                                                new JwtAuthenticationConverter() {
                                                                                                        {
                                                                                                                setJwtGrantedAuthoritiesConverter(
                                                                                                                                new KeycloakRoleConverter());
                                                                                                        }
                                                                                                }))));

                return http.build();
        }

        /**
         * CORS Configuration for Spring Cloud Gateway
         * Allows requests from frontend (localhost:3000) and handles preflight OPTIONS
         * requests
         */
        @Bean
        public org.springframework.web.cors.reactive.CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration corsConfig = new CorsConfiguration();
                corsConfig.setAllowedOrigins(Arrays.asList(
                                "http://localhost:3000",
                                "http://localhost:5000",
                                "http://localhost:5173"));
                corsConfig.setAllowedMethods(Arrays.asList(
                                HttpMethod.GET.name(),
                                HttpMethod.POST.name(),
                                HttpMethod.PUT.name(),
                                HttpMethod.DELETE.name(),
                                HttpMethod.OPTIONS.name(),
                                HttpMethod.PATCH.name()));
                corsConfig.setAllowedHeaders(Arrays.asList("*"));
                corsConfig.setAllowCredentials(true);
                corsConfig.setMaxAge(3600L);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", corsConfig);
                return source;
        }
}
