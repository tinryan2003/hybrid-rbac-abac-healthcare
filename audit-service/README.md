# Audit Service

A comprehensive audit logging service for the hybrid RBAC-ABAC banking system. This service collects, stores, and provides APIs for querying audit logs from all system operations.

## Features

- **Comprehensive Audit Logging**: Tracks all critical operations across the system
- **Event-Driven Architecture**: Consumes audit events from RabbitMQ
- **REST API**: Query audit logs with various filters
- **Secure Access**: Protected with OAuth2 and role-based access control
- **Flexible Querying**: Search by user, resource, event type, date range, severity, etc.
- **Correlation Tracking**: Track related events using correlation IDs
- **Performance Optimized**: Database indexes on frequently queried fields

## Architecture

### Components

1. **AuditLog Entity**: Core entity for storing audit records
2. **RabbitMQ Consumers**: Listen for audit events from other services
3. **REST Controller**: Expose APIs for querying audit logs
4. **Repository Layer**: Optimized queries with JPA
5. **Security**: OAuth2 integration with Keycloak

### Event Types

- Account operations (created, updated, approved, rejected, closed)
- Transaction operations (created, approved, rejected, completed)
- Customer operations (created, updated, deleted, viewed)
- Employee operations (created, updated, deleted, role changed)
- Authorization events (success, denied, policy evaluated)
- Authentication events (login, logout, failed attempts)
- Security events (trust score updates, suspicious activity)

### Severity Levels

- **LOW**: Informational events
- **MEDIUM**: Normal operations
- **HIGH**: Important actions requiring audit
- **CRITICAL**: Security-related or high-risk actions

## Configuration

### Application Properties

```yaml
server:
  port: 8086

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/bank_hybrid
    username: root
    password: S@l19092003
  
  rabbitmq:
    host: localhost
    port: 5672
    username: admin
    password: admin
```

### RabbitMQ Queues

The service listens to the following queues:
- `audit.events.queue` - General audit events
- `audit.account.queue` - Account-specific events
- `audit.transaction.queue` - Transaction-specific events
- `audit.customer.queue` - Customer-specific events
- `audit.employee.queue` - Employee-specific events
- `audit.authorization.queue` - Authorization/policy events

## API Endpoints

All endpoints require authentication and appropriate roles (ADMIN, MANAGER, or AUDITOR).

### Query Endpoints

- `GET /api/audit` - Get all audit logs (paginated)
- `GET /api/audit/{id}` - Get audit log by ID
- `GET /api/audit/user/{userId}` - Get logs by user ID
- `GET /api/audit/username/{username}` - Get logs by username
- `GET /api/audit/resource/{resourceType}/{resourceId}` - Get logs by resource
- `GET /api/audit/event-type/{eventType}` - Get logs by event type
- `GET /api/audit/date-range` - Get logs by date range
- `GET /api/audit/failed` - Get failed actions
- `GET /api/audit/high-severity` - Get high/critical severity events
- `GET /api/audit/search` - Advanced search with multiple filters
- `GET /api/audit/correlation/{correlationId}` - Get related events
- `GET /api/audit/stats/user/{userId}` - Get user statistics

### Create Endpoint

- `POST /api/audit` - Manually create an audit log (admin only)

### Example Requests

#### Get all audit logs
```bash
curl -X GET "http://localhost:8086/api/audit?page=0&size=20" \
  -H "Authorization: Bearer {token}"
```

#### Search with filters
```bash
curl -X GET "http://localhost:8086/api/audit/search?userId=1&eventType=ACCOUNT_APPROVED&startDate=2024-01-01T00:00:00&endDate=2024-12-31T23:59:59" \
  -H "Authorization: Bearer {token}"
```

#### Get user statistics
```bash
curl -X GET "http://localhost:8086/api/audit/stats/user/1" \
  -H "Authorization: Bearer {token}"
```

## Integration with Other Services

### Publishing Audit Events

Other services can publish audit events to RabbitMQ using the `AuditEvent` DTO:

```java
@Autowired
private RabbitTemplate rabbitTemplate;

public void publishAuditEvent() {
    AuditEvent event = AuditEvent.builder()
        .eventType(AuditEventType.ACCOUNT_APPROVED)
        .userId(userId)
        .username(username)
        .resourceType(ResourceType.ACCOUNT)
        .resourceId(accountId)
        .action("APPROVE_ACCOUNT")
        .description("Account approved by manager")
        .success(true)
        .timestamp(LocalDateTime.now())
        .build();
    
    rabbitTemplate.convertAndSend("audit.exchange", "audit.account", event);
}
```

## Database Schema

The service automatically creates the `audit_logs` table with the following indexes:
- `idx_event_type` - Event type index
- `idx_user_id` - User ID index
- `idx_resource_type` - Resource type index
- `idx_resource_id` - Resource ID index
- `idx_timestamp` - Timestamp index
- `idx_severity` - Severity index

## Security

### Authentication
- OAuth2 Resource Server with JWT tokens
- Keycloak integration for token validation

### Authorization
- Role-based access control (RBAC)
- Required roles: `ROLE_ADMIN`, `ROLE_MANAGER`, or `ROLE_EXTERNAL_AUDITOR`

## Running the Service

### Prerequisites
- Java 21
- MySQL database
- RabbitMQ server
- Keycloak (for authentication)

### Start the service

```bash
cd audit-service
./mvnw spring-boot:run
```

The service will start on port 8086.

### Docker Compose

The service is included in the main `docker-compose.yml` file but runs locally for development:

```bash
# Start infrastructure services (MySQL, RabbitMQ, Keycloak, OPA)
docker-compose up -d

# Run audit service locally
cd audit-service
./mvnw spring-boot:run
```

## Monitoring and Maintenance

### Health Check
```bash
curl http://localhost:8086/api/audit/health
```

### Logging
- Application logs are set to DEBUG level for `org.vgu.auditservice`
- RabbitMQ logs are also set to DEBUG for troubleshooting

### Performance Considerations
- Audit logs can grow large over time
- Consider implementing data retention policies
- Archive old logs periodically
- Monitor database size and performance

## Future Enhancements

- [ ] Data retention and archiving policies
- [ ] Real-time dashboards for audit monitoring
- [ ] Alerting for suspicious activities
- [ ] Export audit logs to CSV/PDF
- [ ] Compliance report generation
- [ ] Integration with SIEM systems
- [ ] Dead-letter queue handling for failed events
- [ ] Audit log encryption at rest

## Compliance

This audit service helps meet compliance requirements for:
- SOX (Sarbanes-Oxley)
- PCI DSS (Payment Card Industry Data Security Standard)
- GDPR (General Data Protection Regulation)
- Banking regulations requiring comprehensive audit trails

## Support

For issues or questions, please contact the development team or create an issue in the project repository.
