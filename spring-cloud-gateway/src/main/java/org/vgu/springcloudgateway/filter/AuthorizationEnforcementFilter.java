package org.vgu.springcloudgateway.filter;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Policy Enforcement Point (PEP) Filter
 * Enforces HyARBAC authorization by calling authorization-service before
 * forwarding requests
 */
@Slf4j
@Component
public class AuthorizationEnforcementFilter implements GlobalFilter, Ordered {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${authorization.service.url:http://localhost:8087}")
    private String authorizationServiceUrl;

    @Value("${authorization.enforcement.enabled:true}")
    private boolean enforcementEnabled;

    // Paths that skip authorization check (PEP). Keep this list minimal.
    private static final List<String> SKIP_PATHS = List.of(
            "/api/authorization/health",
            "/api/authorization/check",
            "/actuator",
            "/auth",
            "/health",
            "/api/users/me", // Profile: any authenticated role can access without OPA
            "/api/users/keycloak" // For internal service calls
    );

    public AuthorizationEnforcementFilter(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClientBuilder = webClientBuilder;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        String method = exchange.getRequest().getMethod().name();

        // Skip authorization for certain paths.
        // Forward request as-is so the client's Authorization header reaches the
        // backend.
        if (shouldSkipAuthorization(path)) {
            log.info("PEP skipped for path: {} -> forwarding to backend", path);
            ServerWebExchange mutatedExchange = exchange.mutate()
                    .request(exchange.getRequest().mutate()
                            .header("X-PEP-Skipped", "true")
                            .build())
                    .build();
            return chain.filter(mutatedExchange);
        }

        // Skip if enforcement is disabled
        if (!enforcementEnabled) {
            log.warn("Authorization enforcement is DISABLED - allowing all requests");
            return chain.filter(exchange);
        }

        // Enforce authorization
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .flatMap(authentication -> {
                    if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
                        log.warn("No JWT authentication found for path: {} - denying access", path);
                        return unauthorized(exchange, "Authentication required");
                    }

                    Jwt jwt = jwtAuth.getToken();

                    // Build authorization request
                    AuthorizationCheckRequest authzRequest = buildAuthorizationRequest(jwt, path, method, exchange);

                    // Log thông tin tài khoản + role ra terminal
                    String username = jwt.getClaimAsString("preferred_username");
                    String email = jwt.getClaimAsString("email");
                    String name = jwt.getClaimAsString("name");
                    log.info(
                            "🔐 [AUTH] Tài khoản: username={}, email={}, name={}, role={}, subject={} | Request: {} {}",
                            username != null ? username : "-",
                            email != null ? email : "-",
                            name != null ? name : "-",
                            authzRequest.getRole() != null ? authzRequest.getRole() : "-",
                            authzRequest.getSubject(),
                            method, path);
                    log.info("🔐 PEP: Checking authorization for user={}, role={}, object={}, action={}",
                            authzRequest.getSubject(), authzRequest.getRole(),
                            authzRequest.getObject(), authzRequest.getAction());

                    // Extra log for /api/audit to debug 403
                    if (path.contains("/api/audit")) {
                        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
                        Object rolesObj = realmAccess != null ? realmAccess.get("roles") : null;
                        String allRoles = rolesObj instanceof List ? String.valueOf(rolesObj)
                                : String.valueOf(rolesObj);
                        log.warn(
                                "📋 [AUDIT REQUEST] path={} | selected_role={}, object={}, action={} | realm_roles={} | (403 = selected role not in ADMIN/EXTERNAL_AUDITOR or OPA deny)",
                                path, authzRequest.getRole(), authzRequest.getObject(), authzRequest.getAction(),
                                allRoles);
                    }

                    // Call authorization-service
                    return checkAuthorization(authzRequest)
                            .flatMap(result -> {
                                if (result.isAllowed()) {
                                    log.info("✅ [AUTH] GRANTED | user={}, role={} | {}",
                                            username != null ? username : "-",
                                            authzRequest.getRole() != null ? authzRequest.getRole() : "-",
                                            result.getReason());
                                    return chain.filter(exchange);
                                } else {
                                    log.error("❌ [AUTH] DENIED | user={}, role={} | {}",
                                            username != null ? username : "-",
                                            authzRequest.getRole() != null ? authzRequest.getRole() : "-",
                                            result.getReason());
                                    if (path.contains("/api/audit")) {
                                        log.error("📋 [AUDIT 403] path={} | role={} | reason={}", path,
                                                authzRequest.getRole(), result.getReason());
                                    }
                                    return denyAccess(exchange, result.getReason());
                                }
                            })
                            .onErrorResume(e -> {
                                log.error("Authorization check failed: {}", e.getMessage(), e);
                                // Fail-closed: deny access on error
                                return denyAccess(exchange, "Authorization service unavailable");
                            });
                })
                .switchIfEmpty(unauthorized(exchange, "No security context"));
    }

    /**
     * Check if path should skip authorization (PEP not applied; JWT still required
     * by gateway).
     * Normalizes path (collapse slashes, strip query) so /api//policies and
     * /api/policies?x=1 match.
     */
    private boolean shouldSkipAuthorization(String path) {
        if (path == null || path.isEmpty())
            return false;
        String normalized = path.split("\\?")[0].replaceAll("/+", "/");
        return SKIP_PATHS.stream().anyMatch(prefix -> normalized.equals(prefix) || normalized.startsWith(prefix + "/"));
    }

    /**
     * Build authorization request from JWT and request context
     */
    private AuthorizationCheckRequest buildAuthorizationRequest(Jwt jwt, String path, String method,
            ServerWebExchange exchange) {
        AuthorizationCheckRequest request = new AuthorizationCheckRequest();

        // Subject from JWT
        request.setSubject(jwt.getClaimAsString("sub"));

        // Role from JWT realm_access.roles: filter Keycloak internals, then pick
        // highest-priority role. Only ADMIN is used (no SYSTEM_ADMIN/HOSPITAL_ADMIN).
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess != null && realmAccess.get("roles") instanceof List) {
            List<?> roles = (List<?>) realmAccess.get("roles");
            Set<String> filtered = roles.stream()
                    .map(Object::toString)
                    .filter(role -> !role.startsWith("default-roles-")
                            && !role.equals("offline_access")
                            && !role.equals("uma_authorization"))
                    .collect(Collectors.toSet());
            // Priority order: ADMIN, then audit, clinical, USER
            String[] priorityOrder = {
                    "ADMIN", "EXTERNAL_AUDITOR", "DEPARTMENT_HEAD", "PRIMARY_DOCTOR", "DOCTOR", "NURSE", "PHARMACIST",
                    "RECEPTIONIST",
                    "PATIENT", "USER"
            };
            String extractedRole = null;
            for (String r : priorityOrder) {
                if (filtered.contains(r)) {
                    extractedRole = r;
                    break;
                }
            }
            if (extractedRole == null && !filtered.isEmpty()) {
                extractedRole = filtered.iterator().next();
            }
            // Normalize Admin/admin to ADMIN for OPA/authorization
            if ("Admin".equals(extractedRole) || "admin".equalsIgnoreCase(extractedRole)) {
                extractedRole = "ADMIN";
            }
            // Normalize to uppercase so OPA and policies (e.g. target_roles ["ADMIN"])
            // match
            if (extractedRole != null) {
                request.setRole(extractedRole.toUpperCase());
            }
        }

        // Hospital-specific attributes from JWT
        request.setDepartment(jwt.getClaimAsString("department_id"));
        request.setHospital(jwt.getClaimAsString("hospital_id"));
        request.setPosition(jwt.getClaimAsString("position"));

        // Map path and method to resource object and action
        ResourceMapping mapping = mapPathToResource(path, method);
        request.setObject(mapping.getObject());
        request.setAction(mapping.getAction());
        request.setResourceId(mapping.getResourceId());

        // Context: time, IP, channel
        request.setTime(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        request.setIp(getClientIp(exchange));
        request.setChannel("WEB");

        // Additional context for ward_id, position_level, emergency flag, and JWT user
        // info (for audit)
        Map<String, Object> additionalContext = new HashMap<>();

        String wardId = jwt.getClaimAsString("ward_id");
        if (wardId != null) {
            additionalContext.put("ward_id", wardId);
        }

        Object positionLevel = jwt.getClaim("position_level");
        if (positionLevel != null) {
            additionalContext.put("position_level", positionLevel.toString());
        }

        // JWT user info for audit log (email, display name) when User Service has no
        // record
        String email = jwt.getClaimAsString("email");
        if (email != null && !email.isBlank()) {
            additionalContext.put("email", email);
        }
        String name = jwt.getClaimAsString("name");
        if (name != null && !name.isBlank()) {
            additionalContext.put("name", name);
        }
        String preferredUsername = jwt.getClaimAsString("preferred_username");
        if (preferredUsername != null && !preferredUsername.isBlank()) {
            additionalContext.put("preferred_username", preferredUsername);
        }

        // Check for emergency header (for emergency override)
        String emergencyHeader = exchange.getRequest().getHeaders().getFirst("X-Emergency-Access");
        if ("true".equalsIgnoreCase(emergencyHeader)) {
            additionalContext.put("emergency", true);
            log.warn("⚠️ EMERGENCY ACCESS requested by user={}", request.getSubject());
        }

        request.setAdditionalContext(additionalContext);

        return request;
    }

    /**
     * Map HTTP path and method to resource object and action.
     * Supports both /api/<resource>/<id> and /<resource>/<id> (e.g. after
     * StripPrefix).
     */
    private ResourceMapping mapPathToResource(String path, String method) {
        ResourceMapping mapping = new ResourceMapping();

        // Extract resource ID: match /api/patients/123 or /patients/123 (and similar)
        Pattern idPatternWithApi = Pattern.compile("/api/[^/]+/(\\d+)");
        Pattern idPatternNoApi = Pattern.compile("/(?:patients|appointments|users|policies|audit|billing)/(\\d+)");
        Matcher m1 = idPatternWithApi.matcher(path);
        Matcher m2 = idPatternNoApi.matcher(path);
        if (m1.find()) {
            mapping.setResourceId(m1.group(1));
        } else if (m2.find()) {
            mapping.setResourceId(m2.group(1));
        }
        // Pharmacy and lab: /api/pharmacy/prescriptions/1 or /api/pharmacy/medicines/1,
        // /api/lab/orders/1
        if (mapping.getResourceId() == null) {
            Pattern pharmacyLab = Pattern
                    .compile("/(?:api/)?(?:pharmacy/(?:prescriptions|medicines)|lab(?:/orders)?)/(\\d+)");
            Matcher m3 = pharmacyLab.matcher(path);
            if (m3.find()) {
                mapping.setResourceId(m3.group(1));
            }
        }

        // Map path patterns to resource objects (with and without /api prefix)
        if (path.startsWith("/api/patients") || path.startsWith("/patients")) {
            mapping.setObject("patient_record");
        } else if (path.startsWith("/api/appointments") || path.startsWith("/appointments")) {
            mapping.setObject("appointment");
        } else if (path.startsWith("/api/pharmacy/prescriptions") || path.contains("/prescriptions")) {
            // Pharmacy: prescription domain
            mapping.setObject("prescription");
        } else if (path.startsWith("/api/pharmacy/medicines") || path.contains("/medicines/")) {
            mapping.setObject("medicine");
        } else if (path.startsWith("/api/lab") || path.startsWith("/lab")) {
            mapping.setObject("lab_order");
        } else if (path.startsWith("/api/billing") || path.startsWith("/billing")) {
            mapping.setObject("billing");
        } else if (path.startsWith("/api/users") || path.startsWith("/users")) {
            mapping.setObject("user");
        } else if (path.startsWith("/api/audit") || path.startsWith("/audit")) {
            mapping.setObject("audit_log");
        } else if (path.startsWith("/api/policies") || path.startsWith("/policies")) {
            mapping.setObject("policy_management");
        } else {
            mapping.setObject("unknown");
        }

        // Detect business action from path pattern first
        String businessAction = detectBusinessAction(path);
        if (businessAction != null) {
            mapping.setAction(businessAction);
        } else {
            // Standard CRUD mapping
            mapping.setAction(switch (method) {
                case "GET" -> "read";
                case "POST" -> "create";
                case "PUT", "PATCH" -> "update"; // ✅ Changed from "write" to "update"
                case "DELETE" -> "delete";
                default -> "unknown";
            });
        }

        return mapping;
    }

    /**
     * Detect business action from path pattern
     * Business actions are custom actions beyond standard CRUD (approve, reject,
     * dispense, etc.)
     */
    private String detectBusinessAction(String path) {
        // Approve patterns
        if (path.contains("/approve") || path.contains("/approval")) {
            return "approve";
        }

        // Reject patterns
        if (path.contains("/reject") || path.contains("/rejection")) {
            return "reject";
        }

        // Dispense patterns (pharmacy)
        if (path.contains("/dispense") || path.contains("/dispensing")) {
            return "dispense";
        }

        // Cancel patterns
        if (path.contains("/cancel") || path.contains("/cancellation")) {
            return "cancel";
        }

        // Complete patterns
        if (path.contains("/complete") || path.contains("/completion")) {
            return "complete";
        }

        return null; // No business action detected
    }

    /**
     * Get client IP: X-Forwarded-For (first/leftmost = client) or remote address.
     * Behind proxy/load balancer, X-Forwarded-For may be "client, proxy1, proxy2".
     */
    private String getClientIp(ServerWebExchange exchange) {
        String forwarded = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            String first = forwarded.split(",")[0].trim();
            if (!first.isEmpty()) {
                return normalizeIp(first);
            }
        }
        var remoteAddress = exchange.getRequest().getRemoteAddress();
        if (remoteAddress != null && remoteAddress.getAddress() != null) {
            String ip = remoteAddress.getAddress().getHostAddress();
            return normalizeIp(ip);
        }
        return "unknown";
    }

    /**
     * Normalize IP address to IPv4 format
     * Converts IPv6 loopback to IPv4, and replaces localhost with actual machine IP
     */
    private String normalizeIp(String ip) {
        if (ip == null || ip.isEmpty()) {
            return "unknown";
        }

        // Convert IPv6 loopback to IPv4 loopback
        if ("::1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip)) {
            ip = "127.0.0.1";
        }

        // Remove IPv6 brackets if present
        if (ip.startsWith("[") && ip.endsWith("]")) {
            ip = ip.substring(1, ip.length() - 1);
        }

        // If localhost detected, try to get actual machine IPv4 address
        if ("127.0.0.1".equals(ip) || "localhost".equalsIgnoreCase(ip)) {
            String realIp = getLocalIpv4Address();
            if (realIp != null && !realIp.equals("127.0.0.1")) {
                return realIp;
            }
        }

        return ip;
    }

    /**
     * Get the actual local IPv4 address of this machine (not loopback)
     * Prefers private network addresses (192.168.x.x, 10.x.x.x, 172.16-31.x.x)
     * Falls back to any non-loopback IPv4 address
     */
    private String getLocalIpv4Address() {
        try {
            java.util.Enumeration<java.net.NetworkInterface> interfaces = java.net.NetworkInterface
                    .getNetworkInterfaces();

            String privateIp = null;
            String anyIp = null;

            while (interfaces.hasMoreElements()) {
                java.net.NetworkInterface iface = interfaces.nextElement();

                // Skip loopback and inactive interfaces
                if (iface.isLoopback() || !iface.isUp()) {
                    continue;
                }

                java.util.Enumeration<java.net.InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    java.net.InetAddress addr = addresses.nextElement();

                    // Only process IPv4 addresses
                    if (addr instanceof java.net.Inet4Address && !addr.isLoopbackAddress()) {
                        String ip = addr.getHostAddress();

                        // Store any valid IPv4
                        if (anyIp == null) {
                            anyIp = ip;
                        }

                        // Prefer private network addresses
                        if (isPrivateIp(ip)) {
                            privateIp = ip;
                        }
                    }
                }
            }

            // Return private IP if found, otherwise any IPv4, otherwise null
            return privateIp != null ? privateIp : anyIp;

        } catch (Exception e) {
            log.warn("Failed to get local IPv4 address: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Check if IP is in private network range
     * - 10.0.0.0/8 (10.0.0.0 - 10.255.255.255)
     * - 172.16.0.0/12 (172.16.0.0 - 172.31.255.255)
     * - 192.168.0.0/16 (192.168.0.0 - 192.168.255.255)
     */
    private boolean isPrivateIp(String ip) {
        if (ip == null)
            return false;
        String[] parts = ip.split("\\.");
        if (parts.length != 4)
            return false;

        try {
            int first = Integer.parseInt(parts[0]);
            int second = Integer.parseInt(parts[1]);

            // 10.x.x.x
            if (first == 10)
                return true;

            // 192.168.x.x
            if (first == 192 && second == 168)
                return true;

            // 172.16.x.x - 172.31.x.x
            if (first == 172 && second >= 16 && second <= 31)
                return true;

        } catch (NumberFormatException e) {
            return false;
        }

        return false;
    }

    /**
     * Call authorization-service to check authorization
     */
    private Mono<AuthorizationResult> checkAuthorization(AuthorizationCheckRequest request) {
        WebClient client = webClientBuilder
                .baseUrl(authorizationServiceUrl)
                .build();

        return client.post()
                .uri("/api/authorization/check")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(AuthorizationResult.class)
                .doOnError(e -> log.error("Failed to call authorization-service: {}", e.getMessage()));
    }

    /**
     * Deny access with 403 Forbidden
     */
    private Mono<Void> denyAccess(ServerWebExchange exchange, String reason) {
        var response = exchange.getResponse();
        if (response.isCommitted()) {
            return Mono.empty();
        }
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> errorBody = Map.of(
                "status", 403,
                "error", "Forbidden",
                "message", "Access denied: " + reason,
                "path", exchange.getRequest().getPath().value());

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(errorBody);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (Exception e) {
            log.error("Failed to write error response", e);
            return response.setComplete();
        }
    }

    /**
     * Authentication required (401 Unauthorized)
     */
    private Mono<Void> unauthorized(ServerWebExchange exchange, String reason) {
        var response = exchange.getResponse();
        if (response.isCommitted()) {
            return Mono.empty();
        }
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> errorBody = Map.of(
                "status", 401,
                "error", "Unauthorized",
                "message", reason,
                "path", exchange.getRequest().getPath().value());

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(errorBody);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (Exception e) {
            log.error("Failed to write error response", e);
            return response.setComplete();
        }
    }

    @Override
    public int getOrder() {
        return -90; // After AuthenticationFilter (-100), before other filters
    }

    // =====================================================
    // DTOs
    // =====================================================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthorizationCheckRequest {
        private String subject;
        private String role;
        private String object;
        private String action;
        private String department;
        private String hospital;
        private String position;
        private String resourceId;
        private String time;
        private String ip;
        private String channel;
        private Map<String, Object> additionalContext;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthorizationResult {
        private boolean allowed;
        private String reason;
        private Map<String, Object> context;
        private List<String> obligations;
        @JsonAlias("evaluationTimeMs") // PDP returns evaluationTimeMs
        private long duration;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResourceMapping {
        private String object;
        private String action;
        private String resourceId;
    }
}
