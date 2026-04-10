# Quick Start Guide - Audit Service

## Prerequisites Check

Before starting the audit service, ensure these services are running:

1. **MySQL Database**
   ```bash
   # Check MySQL is running
   mysql -u root -p -e "SHOW DATABASES;"
   # Should see 'bank_hybrid' database
   ```

2. **RabbitMQ**
   ```bash
   # Check RabbitMQ is running
   curl http://localhost:15672
   # Should see RabbitMQ Management UI
   # Login: admin/admin
   ```

3. **Keycloak**
   ```bash
   # Check Keycloak is running
   curl http://localhost:8180
   # Should see Keycloak welcome page
   ```

## Option 1: Quick Start with Docker Compose

```bash
# Start infrastructure services
cd e:\hybrid-rbac-abac
docker-compose up -d

# Wait for services to be healthy (30-60 seconds)
docker-compose ps

# Start audit service
cd audit-service
./mvnw spring-boot:run
```

## Option 2: Step by Step

### 1. Start Infrastructure

```bash
# Start MySQL (if not using Docker)
# Start as Windows service or:
net start MySQL80

# Start RabbitMQ
docker run -d --name rabbitmq \
  -p 5672:5672 -p 15672:15672 \
  -e RABBITMQ_DEFAULT_USER=admin \
  -e RABBITMQ_DEFAULT_PASS=admin \
  rabbitmq:4.2.1-management

# Start Keycloak
docker run -d --name keycloak \
  -p 8180:8080 \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=admin \
  quay.io/keycloak/keycloak:26.4.7 start-dev
```

### 2. Start Audit Service

```bash
cd e:\hybrid-rbac-abac\audit-service

# Clean and compile
./mvnw clean compile

# Run tests (optional)
./mvnw test

# Start the service
./mvnw spring-boot:run
```

### 3. Verify Service is Running

```bash
# Check health endpoint
curl http://localhost:8086/api/audit/health

# Expected response:
# {
#   "status": "UP",
#   "service": "audit-service"
# }
```

## Testing the Service

### 1. Get Access Token from Keycloak

First, you need to authenticate with Keycloak to get a JWT token:

```bash
# For employee portal (has ADMIN/MANAGER roles)
curl -X POST "http://localhost:8180/realms/employee-portal/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=admin" \
  -d "password=admin" \
  -d "grant_type=password" \
  -d "client_id=employee-portal" \
  -d "client_secret=G4sw0AxkXbWEo8GqnJ5CNnk6CzyHUg5J"

# Copy the 'access_token' from the response
```

### 2. Query Audit Logs

```bash
# Set your token
TOKEN="your-access-token-here"

# Get all audit logs
curl -X GET "http://localhost:8086/api/audit?page=0&size=10" \
  -H "Authorization: Bearer $TOKEN"

# Get logs for a specific user
curl -X GET "http://localhost:8086/api/audit/user/1" \
  -H "Authorization: Bearer $TOKEN"

# Search with filters
curl -X GET "http://localhost:8086/api/audit/search?success=false" \
  -H "Authorization: Bearer $TOKEN"
```

### 3. Create a Test Audit Log

```bash
curl -X POST "http://localhost:8086/api/audit" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "ACCOUNT_APPROVED",
    "severity": "HIGH",
    "userId": 1,
    "username": "test.user",
    "userRole": "ROLE_MANAGER",
    "resourceType": "ACCOUNT",
    "resourceId": 100,
    "action": "APPROVE_ACCOUNT",
    "description": "Test account approval",
    "success": true
  }'
```

## Integration with Bank Hybrid Service

### 1. Start Bank Hybrid

```bash
cd e:\hybrid-rbac-abac\bank_hybrid
./mvnw spring-boot:run
```

### 2. Perform Operations

When you perform operations in bank_hybrid (like approving accounts), audit events will be automatically published to RabbitMQ and consumed by the audit service.

### 3. Check RabbitMQ Queues

Visit: http://localhost:15672
- Username: admin
- Password: admin
- Check the "Queues" tab to see audit queues

## Troubleshooting

### Port Already in Use
```bash
# Check what's using port 8086
netstat -ano | findstr :8086

# Kill the process if needed
taskkill /PID <process_id> /F
```

### Database Connection Issues
```bash
# Test MySQL connection
mysql -u root -pS@l19092003 -e "USE bank_hybrid; SHOW TABLES;"

# Check if audit_logs table exists
mysql -u root -pS@l19092003 -e "USE bank_hybrid; DESCRIBE audit_logs;"
```

### RabbitMQ Connection Issues
```bash
# Check RabbitMQ is running
docker ps | grep rabbitmq

# View RabbitMQ logs
docker logs rabbitmq

# Restart RabbitMQ
docker restart rabbitmq
```

### Authentication Issues
```bash
# Verify Keycloak is running
curl http://localhost:8180/health

# Check realm exists
curl http://localhost:8180/realms/employee-portal
```

## Default Configuration

- **Service Port**: 8086
- **Database**: MySQL on localhost:3306
- **Database Name**: bank_hybrid
- **RabbitMQ**: localhost:5672
- **Keycloak**: localhost:8180
- **Realm**: employee-portal

## Logging

Check logs for debugging:

```bash
# View application logs (while running)
# Logs appear in console with DEBUG level for:
# - org.vgu.auditservice
# - org.springframework.amqp
```

## Next Steps

1. ✅ Service is running
2. ✅ Can query audit logs
3. ⏭️ Integrate with bank_hybrid (see INTEGRATION_GUIDE.md)
4. ⏭️ Configure spring-cloud-gateway routes
5. ⏭️ Test end-to-end audit flow

## Common API Endpoints Summary

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/audit/health` | GET | Health check (no auth) |
| `/api/audit` | GET | Get all logs (paginated) |
| `/api/audit/{id}` | GET | Get log by ID |
| `/api/audit/user/{userId}` | GET | Get logs by user |
| `/api/audit/resource/{type}/{id}` | GET | Get logs by resource |
| `/api/audit/failed` | GET | Get failed actions |
| `/api/audit/high-severity` | GET | Get critical events |
| `/api/audit/search` | GET | Advanced search |
| `/api/audit` | POST | Create audit log |

## Support

For detailed documentation, see:
- `README.md` - Full documentation
- `INTEGRATION_GUIDE.md` - Integration with other services
- `IMPLEMENTATION_SUMMARY.md` - Implementation details
