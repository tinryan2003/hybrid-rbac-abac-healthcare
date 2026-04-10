package org.vgu.auditservice.util;

import java.time.LocalDateTime;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.vgu.auditservice.dto.AuditEvent;
import org.vgu.auditservice.enums.AuditEventType;
import org.vgu.auditservice.enums.ResourceType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for publishing audit events to RabbitMQ
 * This can be used by other services to send audit events
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.audit.name:audit.exchange}")
    private String auditExchange;

    /**
     * Publish a general audit event
     */
    public void publishAuditEvent(AuditEvent event) {
        try {
            String routingKey = determineRoutingKey(event.getResourceType());
            log.info("Publishing audit event: type={}, routing={}", event.getEventType(), routingKey);
            rabbitTemplate.convertAndSend(auditExchange, routingKey, event);
            log.debug("Audit event published successfully");
        } catch (Exception e) {
            log.error("Failed to publish audit event", e);
        }
    }

    /**
     * Publish an audit event with builder pattern
     */
    public void publishEvent(
            AuditEventType eventType,
            Long userId,
            String username,
            String userRole,
            ResourceType resourceType,
            Long resourceId,
            String action,
            String description,
            Boolean success,
            String failureReason,
            String metadata) {

        AuditEvent event = AuditEvent.builder()
                .eventType(eventType)
                .userId(userId)
                .username(username)
                .userRole(userRole)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .action(action)
                .description(description)
                .success(success)
                .failureReason(failureReason)
                .metadata(metadata)
                .timestamp(LocalDateTime.now())
                .build();

        publishAuditEvent(event);
    }

    /**
     * Publish a successful audit event (convenience method)
     */
    public void publishSuccessEvent(
            AuditEventType eventType,
            Long userId,
            String username,
            ResourceType resourceType,
            Long resourceId,
            String action,
            String description) {

        publishEvent(eventType, userId, username, null, resourceType, resourceId,
                action, description, true, null, null);
    }

    /**
     * Publish a failed audit event (convenience method)
     */
    public void publishFailureEvent(
            AuditEventType eventType,
            Long userId,
            String username,
            ResourceType resourceType,
            Long resourceId,
            String action,
            String failureReason) {

        publishEvent(eventType, userId, username, null, resourceType, resourceId,
                action, "Action failed", false, failureReason, null);
    }

    /**
     * Determine routing key based on resource type
     */
    private String determineRoutingKey(ResourceType resourceType) {
        if (resourceType == null) {
            return "audit.general";
        }

        return switch (resourceType) {
            case ACCOUNT -> "audit.account";
            case TRANSACTION -> "audit.transaction";
            case CUSTOMER -> "audit.customer";
            case EMPLOYEE -> "audit.employee";
            default -> "audit.general";
        };
    }
}
