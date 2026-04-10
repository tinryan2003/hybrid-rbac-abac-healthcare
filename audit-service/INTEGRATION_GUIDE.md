# Integration Guide: Adding Audit Events to Bank Hybrid Service

This guide shows how to integrate the audit service with the bank_hybrid service to automatically log audit events.

## Step 1: Add Dependencies to bank_hybrid/pom.xml

The dependencies are likely already present, but ensure you have:

```xml
<!-- RabbitMQ for event publishing -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

## Step 2: Update bank_hybrid/application.yml

Add audit exchange configuration:

```yaml
rabbitmq:
  exchange:
    audit:
      name: audit.exchange
```

## Step 3: Create AuditEventPublisher in bank_hybrid

Create a file: `bank_hybrid/src/main/java/org/vgu/bank_hybrid/util/AuditEventPublisher.java`

```java
package org.vgu.bank_hybrid.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.audit.name:audit.exchange}")
    private String auditExchange;

    public void publishAccountEvent(String eventType, Long userId, String username, 
                                   Long accountId, String action, String description, 
                                   Boolean success, String failureReason) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", eventType);
            event.put("userId", userId);
            event.put("username", username);
            event.put("resourceType", "ACCOUNT");
            event.put("resourceId", accountId);
            event.put("action", action);
            event.put("description", description);
            event.put("success", success);
            event.put("failureReason", failureReason);
            event.put("timestamp", LocalDateTime.now().toString());

            log.info("Publishing audit event: {}", eventType);
            rabbitTemplate.convertAndSend(auditExchange, "audit.account", event);
        } catch (Exception e) {
            log.error("Failed to publish audit event", e);
        }
    }

    public void publishTransactionEvent(String eventType, Long userId, String username,
                                       Long transactionId, String action, String description,
                                       Boolean success, String failureReason) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", eventType);
            event.put("userId", userId);
            event.put("username", username);
            event.put("resourceType", "TRANSACTION");
            event.put("resourceId", transactionId);
            event.put("action", action);
            event.put("description", description);
            event.put("success", success);
            event.put("failureReason", failureReason);
            event.put("timestamp", LocalDateTime.now().toString());

            log.info("Publishing audit event: {}", eventType);
            rabbitTemplate.convertAndSend(auditExchange, "audit.transaction", event);
        } catch (Exception e) {
            log.error("Failed to publish audit event", e);
        }
    }

    public void publishAuthorizationEvent(String eventType, Long userId, String username,
                                         String action, Boolean success, String failureReason) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", eventType);
            event.put("userId", userId);
            event.put("username", username);
            event.put("action", action);
            event.put("description", success ? "Authorization granted" : "Authorization denied");
            event.put("success", success);
            event.put("failureReason", failureReason);
            event.put("timestamp", LocalDateTime.now().toString());

            log.info("Publishing authorization audit event");
            rabbitTemplate.convertAndSend(auditExchange, "audit.authorization", event);
        } catch (Exception e) {
            log.error("Failed to publish audit event", e);
        }
    }
}
```

## Step 4: Update AccountController

Update `bank_hybrid/src/main/java/org/vgu/bank_hybrid/controllers/AccountController.java`:

```java
@Autowired
private AuditEventPublisher auditEventPublisher;

// In approveAccount method, replace the log.info with:
auditEventPublisher.publishAccountEvent(
    "ACCOUNT_APPROVED",
    SecurityUtils.getCurrentUserId(),
    SecurityUtils.getCurrentUsername(),
    id,
    "APPROVE_ACCOUNT",
    "Account approved by " + SecurityUtils.getCurrentUsername(),
    true,
    null
);

// In rejectAccount method, replace the log.info with:
auditEventPublisher.publishAccountEvent(
    "ACCOUNT_REJECTED",
    SecurityUtils.getCurrentUserId(),
    SecurityUtils.getCurrentUsername(),
    id,
    "REJECT_ACCOUNT",
    "Account rejected. Reason: " + reason,
    true,
    reason
);
```

## Step 5: Update TransactionService

Similarly, update transaction operations to publish audit events:

```java
@Autowired
private AuditEventPublisher auditEventPublisher;

// After transaction creation:
auditEventPublisher.publishTransactionEvent(
    "TRANSACTION_CREATED",
    fromAccount.getCustomer().getId(),
    fromAccount.getCustomer().getFullName(),
    saved.getId(),
    "CREATE_TRANSACTION",
    String.format("Transaction of %.2f from account %s", 
        request.getAmount(), fromAccount.getAccountNumber()),
    true,
    null
);
```

## Step 6: Update OpaService (Optional)

Track authorization decisions in OPA service:

```java
@Autowired
private AuditEventPublisher auditEventPublisher;

// In authorize method, after getting result:
auditEventPublisher.publishAuthorizationEvent(
    result.isAllowed() ? "AUTHORIZATION_SUCCESS" : "AUTHORIZATION_DENIED",
    // Extract from context or security context
    userId,
    username,
    action + " on " + resource,
    result.isAllowed(),
    result.getReason()
);
```

## Benefits

1. **Centralized Audit Trail**: All audit logs in one place
2. **Asynchronous Logging**: No performance impact on main operations
3. **Queryable History**: Rich APIs for compliance and investigation
4. **Separation of Concerns**: Audit logic separate from business logic
5. **Scalable**: Can handle high volume of events

## Testing

1. Start all services (MySQL, RabbitMQ, Keycloak, OPA)
2. Start audit-service: `cd audit-service && ./mvnw spring-boot:run`
3. Start bank_hybrid: `cd bank_hybrid && ./mvnw spring-boot:run`
4. Perform operations (approve account, create transaction)
5. Query audit logs: `GET http://localhost:8086/api/audit`

## Viewing Audit Logs

```bash
# Get all recent audit logs
curl -X GET "http://localhost:8086/api/audit?page=0&size=20" \
  -H "Authorization: Bearer {your-token}"

# Get logs for specific user
curl -X GET "http://localhost:8086/api/audit/user/1" \
  -H "Authorization: Bearer {your-token}"

# Get account-related logs
curl -X GET "http://localhost:8086/api/audit/resource/ACCOUNT/123" \
  -H "Authorization: Bearer {your-token}"
```
