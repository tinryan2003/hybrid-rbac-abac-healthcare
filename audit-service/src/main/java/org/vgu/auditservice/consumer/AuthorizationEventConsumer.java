package org.vgu.auditservice.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.stereotype.Component;
import org.vgu.auditservice.client.UserServiceClient;
import org.vgu.auditservice.dto.AuditEvent;
import org.vgu.auditservice.enums.AuditEventType;
import org.vgu.auditservice.enums.ResourceType;
import org.vgu.auditservice.service.AuditService;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Consumer for authorization events from authorization service
 * Converts AuthorizationEvent to AuditEvent and saves to audit logs with
 * tamper-evident hashing
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthorizationEventConsumer {

    private final AuditService auditService;
    private final UserServiceClient userServiceClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Listen to authorization events from authorization-service
     * Queue: audit.authorization.queue (routing key: audit.authorization)
     */
    @RabbitListener(queues = "audit.authorization.queue")
    public void consumeAuthorizationEvent(Object event) {
        log.info("Received authorization event from authorization-service, type: {}",
                event != null ? event.getClass().getName() : "null");
        try {
            Map<String, Object> eventMap = convertToMap(event);

            // Debug logging to see what we're receiving
            Map<String, Object> userMap = eventMap.get("user") != null
                    ? (Map<String, Object>) eventMap.get("user")
                    : null;
            String jobTitleFromEvent = userMap != null && userMap.get("jobTitle") != null
                    ? userMap.get("jobTitle").toString()
                    : null;
            log.info(
                    "Authorization event details: subject={}, role={} (RBAC), jobTitle={} (ABAC), allowed={}, reason={}, ipAddress={}",
                    eventMap.get("subject"),
                    eventMap.get("role"),
                    jobTitleFromEvent,
                    eventMap.get("allowed"),
                    eventMap.get("reason"),
                    eventMap.get("context") != null ? ((Map<?, ?>) eventMap.get("context")).get("ipAddress") : null);

            AuditEvent auditEvent = convertAuthorizationEventToAuditEvent(eventMap);

            log.info(
                    "Converted audit event: username={}, userRole={} (RBAC), jobTitle={} (ABAC), ipAddress={}, failureReason={}, success={}",
                    auditEvent.getUsername(),
                    auditEvent.getUserRole(),
                    auditEvent.getJobTitle(),
                    auditEvent.getIpAddress(),
                    auditEvent.getFailureReason(),
                    auditEvent.getSuccess());

            auditService.processAuditEvent(auditEvent);
            log.info("Successfully processed authorization event with ID: {}", auditEvent.getUsername());
        } catch (Exception e) {
            log.error("Error processing authorization event", e);
        }
    }

    /**
     * Convert AuthorizationEvent to AuditEvent
     */
    private AuditEvent convertAuthorizationEventToAuditEvent(Map<String, Object> eventMap) {
        try {
            // Extract event type
            String eventTypeStr = eventMap.get("eventType") != null
                    ? eventMap.get("eventType").toString()
                    : "AUTHORIZATION_POLICY_EVALUATED";
            AuditEventType eventType = mapToAuditEventType(eventTypeStr);

            // Extract user info
            String subject = eventMap.get("subject") != null
                    ? eventMap.get("subject").toString()
                    : null;
            // subject is the Keycloak user ID (UUID)
            String keycloakId = subject;

            // Extract context for additional user info
            Map<String, Object> contextMap = eventMap.get("context") != null
                    ? (Map<String, Object>) eventMap.get("context")
                    : new HashMap<>();

            // Extract jobTitle from eventMap (user object) first
            String jobTitle = null;
            Map<String, Object> userMap = eventMap.get("user") != null
                    ? (Map<String, Object>) eventMap.get("user")
                    : null;
            if (userMap != null && userMap.get("jobTitle") != null) {
                jobTitle = userMap.get("jobTitle").toString();
                log.debug("Extracted jobTitle from event user object: {}", jobTitle);
            }

            // Fetch email, employee_number, name, and jobTitle from User Service using keycloakId
            String email = null;
            String employeeNumber = null;
            String displayName = null; // preferred_username, name, or email for audit log display
            if (keycloakId != null && !keycloakId.isEmpty()) {
                try {
                    Map<String, Object> userProfile = userServiceClient.getUserProfileByKeycloakId(keycloakId);
                    if (userProfile != null && !userProfile.isEmpty()) {
                        if (userProfile.get("email") != null) {
                            email = userProfile.get("email").toString();
                        }
                        if (userProfile.get("employeeNumber") != null) {
                            employeeNumber = userProfile.get("employeeNumber").toString();
                        }
                        if (userProfile.get("name") != null) {
                            displayName = userProfile.get("name").toString();
                        } else if (userProfile.get("preferred_username") != null) {
                            displayName = userProfile.get("preferred_username").toString();
                        } else if (email != null) {
                            displayName = email;
                        }
                        // Fetch jobTitle from User Service if not already extracted from event
                        if (jobTitle == null && userProfile.get("jobTitle") != null) {
                            jobTitle = userProfile.get("jobTitle").toString();
                            log.debug("Fetched jobTitle from User Service: {}", jobTitle);
                        }
                        log.info(
                                "✅ Fetched user details from User Service: keycloakId={}, displayName={}, email={}, employeeNumber={}, jobTitle={}",
                                keycloakId, displayName, email, employeeNumber, jobTitle);
                    } else {
                        log.warn("⚠️ User profile not found in User Service for keycloakId={} (returned null or empty)",
                                keycloakId);
                    }
                } catch (Exception e) {
                    // Check if it's a 404 (user not found) - this is expected for some users
                    String errorMessage = e.getMessage();
                    if (errorMessage != null && (errorMessage.contains("404") || errorMessage.contains("Not Found") ||
                            errorMessage.contains("User not found")
                            || e.getClass().getSimpleName().contains("NotFound"))) {
                        log.warn(
                                "⚠️ User not found in User Service for keycloakId={} (404). User may not be registered in employee_profiles table.",
                                keycloakId);
                    } else {
                        log.error(
                                "❌ Failed to fetch user details from User Service for keycloakId={}: {} (Exception type: {})",
                                keycloakId, e.getMessage(), e.getClass().getName(), e);
                    }
                }

                // Fallback: try to extract from context if available (e.g. from JWT when User Service has no record)
                if ((email == null || employeeNumber == null || jobTitle == null || displayName == null) && contextMap != null) {
                    if (email == null && contextMap.get("email") != null) {
                        email = contextMap.get("email").toString();
                        log.debug("Using email from context: {}", email);
                    }
                    if (employeeNumber == null && contextMap.get("employeeNumber") != null) {
                        employeeNumber = contextMap.get("employeeNumber").toString();
                        log.debug("Using employeeNumber from context: {}", employeeNumber);
                    }
                    if (employeeNumber == null && contextMap.get("employee_number") != null) {
                        employeeNumber = contextMap.get("employee_number").toString();
                        log.debug("Using employee_number from context: {}", employeeNumber);
                    }
                    if (jobTitle == null && contextMap.get("jobTitle") != null) {
                        jobTitle = contextMap.get("jobTitle").toString();
                        log.debug("Using jobTitle from context: {}", jobTitle);
                    }
                    if (displayName == null) {
                        if (contextMap.get("name") != null) {
                            displayName = contextMap.get("name").toString();
                            log.debug("Using display name from context (name): {}", displayName);
                        } else if (contextMap.get("preferred_username") != null) {
                            displayName = contextMap.get("preferred_username").toString();
                            log.debug("Using display name from context (preferred_username): {}", displayName);
                        } else if (email != null) {
                            displayName = email;
                            log.debug("Using email as display name from context");
                        }
                    }
                }
            }

            // Legacy: Extract userId (for backward compatibility)
            Long userId = extractUserId(subject);

            // Extract role
            String role = eventMap.get("role") != null
                    ? eventMap.get("role").toString()
                    : null;

            // Extract resource info
            String resourceTypeStr = eventMap.get("resourceType") != null
                    ? eventMap.get("resourceType").toString()
                    : "TRANSACTION";
            ResourceType resourceType = mapToResourceType(resourceTypeStr);

            Long resourceId = null;
            if (eventMap.get("resourceId") != null) {
                try {
                    resourceId = Long.valueOf(eventMap.get("resourceId").toString());
                } catch (NumberFormatException e) {
                    log.debug("Resource ID is not a number: {}", eventMap.get("resourceId"));
                }
            }

            // Extract authorization decision
            Boolean allowed = eventMap.get("allowed") != null
                    ? Boolean.valueOf(eventMap.get("allowed").toString())
                    : false;

            // Extract OPA policy reason (THIS IS THE KEY - "why OPA blocked")
            String reason = eventMap.get("reason") != null
                    ? eventMap.get("reason").toString()
                    : null;

            // Extract IP address from context
            String ipAddress = null;
            if (contextMap != null && contextMap.get("ipAddress") != null) {
                ipAddress = normalizeIp(contextMap.get("ipAddress").toString());
            }

            // Extract obligations
            java.util.List<String> obligations = null;
            if (eventMap.get("obligations") != null) {
                obligations = (java.util.List<String>) eventMap.get("obligations");
            }

            // Build description with OPA reason
            String description = buildAuthorizationDescription(
                    eventMap.get("action") != null ? eventMap.get("action").toString() : "authorize",
                    allowed,
                    reason,
                    obligations,
                    contextMap);

            // Build metadata JSON with full context
            String metadata = buildMetadata(eventMap, contextMap);

            // Determine severity based on decision
            // DENIED events are HIGH severity (security concern)
            // SUCCESS with REQUIRE_APPROVAL is MEDIUM (needs attention)
            // SUCCESS without obligations is LOW

            return AuditEvent.builder()
                    .eventType(eventType)
                    .employeeNumber(employeeNumber)
                    .email(email)
                    .keycloakId(keycloakId) // Keycloak user ID (UUID)
                    .userRole(role) // RBAC: Role from Keycloak JWT
                    .jobTitle(jobTitle) // ABAC: JobTitleType enum value
                    .resourceType(resourceType)
                    .resourceId(resourceId)
                    .action(eventMap.get("action") != null ? eventMap.get("action").toString() : "AUTHORIZE")
                    .description(description)
                    .ipAddress(ipAddress) // Set IP address directly
                    .success(allowed)
                    .failureReason(allowed ? null : reason) // OPA reason goes here - this is what we want to see in
                                                            // audit logs
                    .metadata(metadata)
                    .beforeState(null)
                    .afterState(buildAfterState(allowed, reason, obligations))
                    .timestamp(extractTimestamp(eventMap))
                    .correlationId(null) // Can be added if correlation ID is provided
                    // Username: display name from User Service when available, else Keycloak subject (UUID)
                    .username(displayName != null && !displayName.isEmpty() ? displayName : subject)
                    .userId(userId)
                    .build();
        } catch (Exception e) {
            log.error("Error converting authorization event to audit event", e);
            throw new RuntimeException("Failed to convert authorization event", e);
        }
    }

    /**
     * Map event type string to AuditEventType enum
     */
    private AuditEventType mapToAuditEventType(String eventTypeStr) {
        try {
            return AuditEventType.valueOf(eventTypeStr);
        } catch (IllegalArgumentException e) {
            log.debug("Unknown event type: {}, defaulting to AUTHORIZATION_POLICY_EVALUATED", eventTypeStr);
            return AuditEventType.AUTHORIZATION_POLICY_EVALUATED;
        }
    }

    /**
     * Map resource type string to ResourceType enum
     */
    private ResourceType mapToResourceType(String resourceTypeStr) {
        try {
            return ResourceType.valueOf(resourceTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.debug("Unknown resource type: {}, defaulting to UNKNOWN", resourceTypeStr);
            return ResourceType.UNKNOWN;
        }
    }

    /**
     * Extract user ID from subject (if subject is numeric)
     */
    private Long extractUserId(String subject) {
        if (subject == null)
            return null;
        try {
            return Long.parseLong(subject);
        } catch (NumberFormatException e) {
            // Subject might be a username/email, not a numeric ID
            return null;
        }
    }

    /**
     * Build authorization description with OPA reason
     */
    private String buildAuthorizationDescription(
            String action,
            boolean allowed,
            String reason,
            java.util.List<String> obligations,
            Map<String, Object> context) {

        StringBuilder desc = new StringBuilder();
        desc.append("Authorization ").append(allowed ? "ALLOWED" : "DENIED");
        desc.append(" for action: ").append(action);

        if (reason != null && !reason.isEmpty()) {
            desc.append(". OPA Policy Reason: ").append(reason);
        }

        if (obligations != null && !obligations.isEmpty()) {
            desc.append(". Obligations: ").append(String.join(", ", obligations));
        }

        if (context.get("amount") != null) {
            desc.append(". Amount: ").append(context.get("amount"));
        }

        if (context.get("networkZone") != null) {
            desc.append(". Network: ").append(context.get("networkZone"));
        }

        return desc.toString();
    }

    /**
     * Build metadata JSON string
     */
    private String buildMetadata(Map<String, Object> eventMap, Map<String, Object> contextMap) {
        try {
            Map<String, Object> metadata = new HashMap<>();

            // Add context fields
            if (contextMap.get("ipAddress") != null) {
                metadata.put("ipAddress", contextMap.get("ipAddress"));
            }
            if (contextMap.get("time") != null) {
                metadata.put("time", contextMap.get("time"));
            }
            if (contextMap.get("channel") != null) {
                metadata.put("channel", contextMap.get("channel"));
            }
            if (contextMap.get("amount") != null) {
                metadata.put("amount", contextMap.get("amount"));
            }
            if (contextMap.get("networkZone") != null) {
                metadata.put("networkZone", contextMap.get("networkZone"));
            }
            if (contextMap.get("riskScore") != null) {
                metadata.put("riskScore", contextMap.get("riskScore"));
            }
            if (contextMap.get("resourceBranchId") != null) {
                metadata.put("resourceBranchId", contextMap.get("resourceBranchId"));
            }
            if (contextMap.get("resourceStatus") != null) {
                metadata.put("resourceStatus", contextMap.get("resourceStatus"));
            }
            if (contextMap.get("dailyAccumulatedAmount") != null) {
                metadata.put("dailyAccumulatedAmount", contextMap.get("dailyAccumulatedAmount"));
            }
            if (contextMap.get("additionalContext") != null) {
                metadata.put("additionalContext", contextMap.get("additionalContext"));
            }

            // Add obligations
            if (eventMap.get("obligations") != null) {
                metadata.put("obligations", eventMap.get("obligations"));
            }

            return objectMapper.writeValueAsString(metadata);
        } catch (Exception e) {
            log.warn("Failed to build metadata JSON", e);
            return null;
        }
    }

    /**
     * Build after state JSON
     */
    private String buildAfterState(boolean allowed, String reason, java.util.List<String> obligations) {
        try {
            Map<String, Object> afterState = new HashMap<>();
            afterState.put("allowed", allowed);
            if (reason != null) {
                afterState.put("reason", reason);
            }
            if (obligations != null && !obligations.isEmpty()) {
                afterState.put("obligations", obligations);
            }
            return objectMapper.writeValueAsString(afterState);
        } catch (Exception e) {
            log.warn("Failed to build after state JSON", e);
            return null;
        }
    }

    /**
     * Extract timestamp from event map
     */
    private LocalDateTime extractTimestamp(Map<String, Object> eventMap) {
        if (eventMap.get("timestamp") != null) {
            try {
                String timestampStr = eventMap.get("timestamp").toString();
                return LocalDateTime.parse(timestampStr);
            } catch (Exception e) {
                log.debug("Failed to parse timestamp: {}", eventMap.get("timestamp"));
            }
        }
        return LocalDateTime.now();
    }

    /**
     * Convert object to Map
     * Handles Spring Messaging Message objects, Maps, byte arrays, Strings, and
     * other types
     * Uses multiple strategies to extract payload from GenericMessage to handle
     * classloader issues
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> convertToMap(Object obj) {
        if (obj == null) {
            log.warn("convertToMap called with null object");
            return new HashMap<>();
        }

        String className = obj.getClass().getName();
        log.info("convertToMap called with object type: {}", className);

        // Strategy 1: Check for AMQP Message (org.springframework.amqp.core.Message) -
        // has getBody() method
        if (obj instanceof org.springframework.amqp.core.Message) {
            org.springframework.amqp.core.Message amqpMessage = (org.springframework.amqp.core.Message) obj;
            byte[] body = amqpMessage.getBody();
            if (body != null && body.length > 0) {
                try {
                    String jsonString = new String(body, StandardCharsets.UTF_8);
                    log.info("Extracted body from AMQP Message, deserializing JSON string");
                    return objectMapper.readValue(jsonString, Map.class);
                } catch (Exception e) {
                    log.error("Failed to deserialize AMQP Message body: {}", e.getMessage(), e);
                }
            } else {
                log.warn("AMQP Message body is null or empty");
            }
        }

        // Strategy 2: Check for GenericMessage by instanceof
        if (obj instanceof GenericMessage) {
            Object payload = ((GenericMessage<?>) obj).getPayload();
            log.info("Extracted payload from GenericMessage (instanceof), payload type: {}",
                    payload != null ? payload.getClass().getName() : "null");
            return convertToMap(payload);
        }

        // Strategy 3: Check for Spring Messaging Message interface
        // (org.springframework.messaging.Message)
        if (obj instanceof org.springframework.messaging.Message) {
            Object payload = ((org.springframework.messaging.Message<?>) obj).getPayload();
            log.info("Extracted payload from MessagingMessage (instanceof), payload type: {}",
                    payload != null ? payload.getClass().getName() : "null");
            return convertToMap(payload);
        }

        // Strategy 4: Reflection-based extraction for getPayload() method (for
        // GenericMessage/MessagingMessage)
        if (className.contains("GenericMessage")
                || (className.contains("Message") && !className.contains("amqp.core"))) {
            try {
                java.lang.reflect.Method getPayloadMethod = obj.getClass().getMethod("getPayload");
                Object payload = getPayloadMethod.invoke(obj);
                log.info("Extracted payload using reflection from {}, payload type: {}",
                        className, payload != null ? payload.getClass().getName() : "null");
                return convertToMap(payload);
            } catch (NoSuchMethodException e) {
                // Try getBody() for AMQP Message
                try {
                    java.lang.reflect.Method getBodyMethod = obj.getClass().getMethod("getBody");
                    byte[] body = (byte[]) getBodyMethod.invoke(obj);
                    if (body != null && body.length > 0) {
                        String jsonString = new String(body, StandardCharsets.UTF_8);
                        log.info("Extracted body using reflection from {}, deserializing JSON", className);
                        return objectMapper.readValue(jsonString, Map.class);
                    }
                } catch (Exception ex) {
                    log.debug("Failed to extract body using reflection: {}", ex.getMessage());
                }
            } catch (Exception e) {
                log.error("Failed to extract payload using reflection from {}: {}", className, e.getMessage(), e);
            }
        }

        // Strategy 5: If already a Map, return it directly
        if (obj instanceof Map) {
            log.info("Object is already a Map, returning directly");
            return (Map<String, Object>) obj;
        }

        // Strategy 6: Try to serialize to JSON and parse back (last resort for Spring
        // objects)
        if (className.contains("org.springframework")) {
            try {
                // Try to convert the entire object to JSON string, then parse
                String json = objectMapper.writeValueAsString(obj);
                log.info("Serialized Spring object to JSON, attempting to parse as Map");
                Map<String, Object> result = objectMapper.readValue(json, Map.class);
                // If it's a GenericMessage JSON, try to extract payload field
                if (result.containsKey("payload")) {
                    log.info("Found 'payload' field in serialized JSON, extracting it");
                    return convertToMap(result.get("payload"));
                }
                return result;
            } catch (Exception e) {
                log.error("Failed to serialize Spring object to JSON: {}", e.getMessage(), e);
            }
        }

        // Strategy 7: Handle byte arrays
        if (obj instanceof byte[]) {
            try {
                String jsonString = new String((byte[]) obj, StandardCharsets.UTF_8);
                log.info("Deserializing byte array to Map");
                return objectMapper.readValue(jsonString, Map.class);
            } catch (Exception e) {
                log.warn("Failed to deserialize byte array to Map: {}", e.getMessage());
                return new HashMap<>();
            }
        }

        // Strategy 8: Handle JSON strings
        if (obj instanceof String) {
            try {
                log.info("Parsing JSON string to Map");
                return objectMapper.readValue((String) obj, Map.class);
            } catch (Exception e) {
                log.warn("Failed to parse JSON string to Map: {}", e.getMessage());
                return new HashMap<>();
            }
        }

        // Last resort: Try convertValue (but skip internal Java types)
        if (className.startsWith("java.") || className.startsWith("sun.")) {
            log.error("Cannot convert internal Java type {} to Map", className);
            return new HashMap<>();
        }

        try {
            log.warn("Attempting to convert unknown type {} to Map using convertValue", className);
            return objectMapper.convertValue(obj, Map.class);
        } catch (Exception e) {
            log.error("Failed to convert object of type {} to Map. Error: {}", className, e.getMessage(), e);
            return new HashMap<>();
        }
    }

    /**
     * Normalize IP address to IPv4 format
     * Converts IPv6 loopback to IPv4, and replaces localhost with actual machine IP
     */
    private String normalizeIp(String ip) {
        if (ip == null || ip.isEmpty()) {
            return null;
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
            java.util.Enumeration<java.net.NetworkInterface> interfaces = 
                java.net.NetworkInterface.getNetworkInterfaces();
            
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
        if (ip == null) return false;
        String[] parts = ip.split("\\.");
        if (parts.length != 4) return false;
        
        try {
            int first = Integer.parseInt(parts[0]);
            int second = Integer.parseInt(parts[1]);
            
            // 10.x.x.x
            if (first == 10) return true;
            
            // 192.168.x.x
            if (first == 192 && second == 168) return true;
            
            // 172.16.x.x - 172.31.x.x
            if (first == 172 && second >= 16 && second <= 31) return true;
            
        } catch (NumberFormatException e) {
            return false;
        }
        
        return false;
    }
}
