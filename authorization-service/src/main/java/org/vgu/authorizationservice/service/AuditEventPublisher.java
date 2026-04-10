package org.vgu.authorizationservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Service to publish authorization audit events to RabbitMQ
 * These events will be consumed by audit-service for tamper-evident logging
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${rabbitmq.exchange.audit.name:audit.exchange}")
    private String auditExchange;

    @Value("${rabbitmq.routing.audit.authorization:audit.authorization}")
    private String authorizationRoutingKey;

    /**
     * Publish authorization decision event to audit-service
     * 
     * @param subject User ID (from JWT)
     * @param role User role
     * @param action Action being authorized (e.g., "create", "approve")
     * @param resourceType Resource type (e.g., "patient_record", "appointment", "audit_log")
     * @param resourceId Resource ID (if available)
     * @param allowed Whether authorization was allowed
     * @param reason OPA policy reason (e.g., "Cross-department access denied")
     * @param obligations OPA obligations (e.g., ["REQUIRE_APPROVAL"])
     * @param context Additional context (IP, time, networkZone, department, hospital, etc.)
     */
    public void publishAuthorizationEvent(
            String subject,
            String role,
            String action,
            String resourceType,
            Long resourceId,
            boolean allowed,
            String reason,
            java.util.List<String> obligations,
            Map<String, Object> context) {

        try {
            // Build audit event payload
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", allowed ? "AUTHORIZATION_SUCCESS" : "AUTHORIZATION_DENIED");
            event.put("subject", subject);
            event.put("role", role);
            event.put("action", action);
            event.put("resourceType", resourceType);
            event.put("resourceId", resourceId);
            event.put("allowed", allowed);
            event.put("reason", reason);
            event.put("obligations", obligations != null ? obligations : java.util.Collections.emptyList());
            event.put("context", context != null ? context : java.util.Collections.emptyMap());
            event.put("timestamp", LocalDateTime.now().toString());

            // Publish to RabbitMQ (async, fire-and-forget)
            rabbitTemplate.convertAndSend(auditExchange, authorizationRoutingKey, event);
            
            log.info("Published authorization audit event: subject={}, role={}, action={}, allowed={}, reason={}, ipAddress={}",
                    subject, role, action, allowed, reason, 
                    context != null ? context.get("ipAddress") : null);

        } catch (Exception e) {
            // Don't fail authorization if audit publishing fails
            log.error("Failed to publish authorization audit event: {}", e.getMessage(), e);
        }
    }
}

