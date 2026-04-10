package org.vgu.auditservice.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.vgu.auditservice.dto.AuditEvent;
import org.vgu.auditservice.enums.AuditEventType;
import org.vgu.auditservice.enums.ResourceType;
import org.vgu.auditservice.service.AuditService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Consumer for transaction events from transaction service
 * Converts TransactionEvent to AuditEvent and saves to audit logs
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionEventConsumer {

    private final AuditService auditService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Listen to transaction.created events
     */
    @RabbitListener(queues = "transaction.created.queue")
    public void consumeTransactionCreated(Object event) {
        log.info("Received transaction.created event");
        try {
            Map<String, Object> eventMap = convertToMap(event);
            AuditEvent auditEvent = convertTransactionEventToAuditEvent(eventMap, AuditEventType.TRANSACTION_CREATED);
            auditService.processAuditEvent(auditEvent);
            log.debug("Successfully processed transaction.created event");
        } catch (Exception e) {
            log.error("Error processing transaction.created event", e);
        }
    }

    /**
     * Listen to transaction.approved events
     */
    @RabbitListener(queues = "transaction.approved.queue")
    public void consumeTransactionApproved(Object event) {
        log.info("Received transaction.approved event");
        try {
            Map<String, Object> eventMap = convertToMap(event);
            AuditEvent auditEvent = convertTransactionEventToAuditEvent(eventMap, AuditEventType.TRANSACTION_APPROVED);
            auditService.processAuditEvent(auditEvent);
            log.debug("Successfully processed transaction.approved event");
        } catch (Exception e) {
            log.error("Error processing transaction.approved event", e);
        }
    }

    /**
     * Listen to transaction.rejected events
     */
    @RabbitListener(queues = "transaction.rejected.queue")
    public void consumeTransactionRejected(Object event) {
        log.info("Received transaction.rejected event");
        try {
            Map<String, Object> eventMap = convertToMap(event);
            AuditEvent auditEvent = convertTransactionEventToAuditEvent(eventMap, AuditEventType.TRANSACTION_REJECTED);
            auditService.processAuditEvent(auditEvent);
            log.debug("Successfully processed transaction.rejected event");
        } catch (Exception e) {
            log.error("Error processing transaction.rejected event", e);
        }
    }

    /**
     * Listen to transaction.completed events
     */
    @RabbitListener(queues = "transaction.completed.queue")
    public void consumeTransactionCompleted(Object event) {
        log.info("Received transaction.completed event");
        try {
            Map<String, Object> eventMap = convertToMap(event);
            AuditEvent auditEvent = convertTransactionEventToAuditEvent(eventMap, AuditEventType.TRANSACTION_COMPLETED);
            auditService.processAuditEvent(auditEvent);
            log.debug("Successfully processed transaction.completed event");
        } catch (Exception e) {
            log.error("Error processing transaction.completed event", e);
        }
    }

    /**
     * Convert TransactionEvent to AuditEvent
     */
    private AuditEvent convertTransactionEventToAuditEvent(Map<String, Object> eventMap, AuditEventType eventType) {
        try {
            // Extract transaction ID
            Long transactionId = null;
            if (eventMap.get("transactionId") != null) {
                transactionId = Long.valueOf(eventMap.get("transactionId").toString());
            }

            // Extract user info (from customerId or initiatedBy)
            Long userId = null;
            if (eventMap.get("customerId") != null) {
                userId = Long.valueOf(eventMap.get("customerId").toString());
            }

            // Extract username
            String username = null;
            if (eventMap.get("customerName") != null) {
                username = eventMap.get("customerName").toString();
            } else if (eventMap.get("customerEmail") != null) {
                username = eventMap.get("customerEmail").toString();
            }

            // Build description
            String description = buildTransactionDescription(eventMap, eventType);

            // Build metadata JSON
            String metadata = buildMetadata(eventMap);

            // Determine success based on status
            String status = eventMap.get("status") != null ? eventMap.get("status").toString() : "";
            Boolean success = !status.equals("REJECTED") && !status.equals("FAILED");

            return AuditEvent.builder()
                    .eventType(eventType)
                    .userId(userId)
                    .username(username)
                    .userRole(null) // Will be populated from transaction context if available
                    .resourceType(ResourceType.TRANSACTION)
                    .resourceId(transactionId)
                    .action(eventType.name())
                    .description(description)
                    .success(success)
                    .failureReason(status.equals("REJECTED") ? eventMap.get("rejectionReason") != null 
                            ? eventMap.get("rejectionReason").toString() : "Transaction rejected" : null)
                    .metadata(metadata)
                    .beforeState(null) // Can be populated if needed
                    .afterState(buildAfterState(eventMap))
                    .timestamp(LocalDateTime.now())
                    .correlationId(eventMap.get("transactionIdString") != null 
                            ? eventMap.get("transactionIdString").toString() : null)
                    .build();
        } catch (Exception e) {
            log.error("Error converting transaction event to audit event", e);
            throw new RuntimeException("Failed to convert transaction event", e);
        }
    }

    /**
     * Build transaction description
     */
    private String buildTransactionDescription(Map<String, Object> eventMap, AuditEventType eventType) {
        StringBuilder desc = new StringBuilder();
        desc.append(eventType.name().replace("_", " "));
        
        if (eventMap.get("transactionType") != null) {
            desc.append(" - Type: ").append(eventMap.get("transactionType"));
        }
        
        if (eventMap.get("amount") != null) {
            desc.append(", Amount: ").append(eventMap.get("amount"));
        }
        
        if (eventMap.get("fromAccount") != null) {
            desc.append(", From: ").append(eventMap.get("fromAccount"));
        }
        
        if (eventMap.get("toAccount") != null) {
            desc.append(", To: ").append(eventMap.get("toAccount"));
        }
        
        if (eventMap.get("status") != null) {
            desc.append(", Status: ").append(eventMap.get("status"));
        }
        
        return desc.toString();
    }

    /**
     * Build metadata JSON string
     */
    private String buildMetadata(Map<String, Object> eventMap) {
        try {
            Map<String, Object> metadata = new HashMap<>();
            if (eventMap.get("transactionType") != null) {
                metadata.put("transactionType", eventMap.get("transactionType"));
            }
            if (eventMap.get("amount") != null) {
                metadata.put("amount", eventMap.get("amount"));
            }
            if (eventMap.get("currency") != null) {
                metadata.put("currency", eventMap.get("currency"));
            }
            if (eventMap.get("fromAccount") != null) {
                metadata.put("fromAccount", eventMap.get("fromAccount"));
            }
            if (eventMap.get("toAccount") != null) {
                metadata.put("toAccount", eventMap.get("toAccount"));
            }
            if (eventMap.get("description") != null) {
                metadata.put("description", eventMap.get("description"));
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
    private String buildAfterState(Map<String, Object> eventMap) {
        try {
            Map<String, Object> afterState = new HashMap<>();
            if (eventMap.get("status") != null) {
                afterState.put("status", eventMap.get("status"));
            }
            if (eventMap.get("approvedAt") != null) {
                afterState.put("approvedAt", eventMap.get("approvedAt"));
            }
            if (eventMap.get("approvedByEmployeeId") != null) {
                afterState.put("approvedBy", eventMap.get("approvedByEmployeeId"));
            }
            return objectMapper.writeValueAsString(afterState);
        } catch (Exception e) {
            log.warn("Failed to build after state JSON", e);
            return null;
        }
    }

    /**
     * Convert object to Map
     * Handles Map directly, JSON string, or byte array
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> convertToMap(Object obj) {
        // If already a Map, return it directly
        if (obj instanceof Map) {
            return (Map<String, Object>) obj;
        }
        
        // If it's a byte array (from Java serialization), try to deserialize
        if (obj instanceof byte[]) {
            try {
                String jsonString = new String((byte[]) obj);
                return objectMapper.readValue(jsonString, Map.class);
            } catch (Exception e) {
                log.warn("Failed to deserialize byte array to Map: {}", e.getMessage());
                return new HashMap<>();
            }
        }
        
        // If it's a String (JSON), parse it
        if (obj instanceof String) {
            try {
                return objectMapper.readValue((String) obj, Map.class);
            } catch (Exception e) {
                log.warn("Failed to parse JSON string to Map: {}", e.getMessage());
                return new HashMap<>();
            }
        }
        
        // For other types, try to convert (but this might fail for complex objects)
        try {
            // Only try to convert if it's a simple POJO, not internal Java types
            String className = obj.getClass().getName();
            if (className.startsWith("java.") || className.startsWith("sun.") || className.startsWith("org.springframework")) {
                log.warn("Attempting to convert internal Java type to Map: {}. This may fail.", className);
                return new HashMap<>();
            }
            return objectMapper.convertValue(obj, Map.class);
        } catch (Exception e) {
            log.error("Failed to convert object to Map. Object type: {}, Error: {}", 
                    obj != null ? obj.getClass().getName() : "null", e.getMessage());
            return new HashMap<>();
        }
    }
}

