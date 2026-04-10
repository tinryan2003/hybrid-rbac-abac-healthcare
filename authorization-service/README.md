# Authorization Service

Centralized authorization microservice using **OPA (Open Policy Agent)** for Hybrid RBAC/ABAC in a digital banking system.

## 🎯 Purpose

This service acts as a **Policy Decision Point (PDP)** that:
- Evaluates authorization requests using **OPA** (Open Policy Agent)
- Combines RBAC (Role-Based Access Control) from Keycloak with ABAC (Attribute-Based Access Control)
- Uses **declarative Rego policies** for external, hot-reloadable authorization rules
- Integrates with other microservices as Policy Information Points (PIPs)

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────┐
│              Authorization Service                       │
│                  (Port 8087)                            │
│                                                         │
│  ┌──────────────┐    ┌──────────────┐                 │
│  │ Authorization│    │   OPA        │                 │
│  │  Controller  │───▶│   Service    │                 │
│  └──────────────┘    └──────┬───────┘                 │
│                              │                          │
│                       ┌──────▼───────┐                 │
│                       │  WebClient   │                 │
│                       │  (HTTP)      │                 │
│                       └──────┬───────┘                 │
│                              │                          │
│  ┌──────────────┐           │                          │
│  │ Account      │           ▼                          │
│  │ Client (PIP) │      ┌─────────┐                    │
│  └──────────────┘      │   OPA   │                    │
│                        │ Server  │                    │
│                        │ (8181)  │                    │
│                        └─────────┘                    │
│                              │                          │
│                        ┌─────▼─────┐                  │
│                        │  Rego     │                  │
│                        │  Policies │                  │
│                        └───────────┘                  │
└─────────────────────────────────────────────────────────┘
```

## 📦 Features

- ✅ **OPA Integration**: External policy engine via HTTP
- ✅ **Declarative Policies**: Rego policies (hot-reloadable)
- ✅ **RBAC Support**: Role-based authorization from Keycloak JWT
- ✅ **ABAC Support**: 9 context-aware attributes (IP, time, amount, risk score, etc.)
- ✅ **PIP Integration**: Fetches resource attributes from Account Service
- ✅ **REST API**: Simple authorization check endpoints
- ✅ **Policy Management**: Reload policies via API
- ✅ **Batch Authorization**: Check multiple requests at once
- ✅ **Health Monitoring**: OPA server health checks

## 🚀 Getting Started

### Prerequisites

- Java 21
- Maven 3.6+
- **OPA Server** (running on localhost:8181)
- Keycloak (running on localhost:8180)

### Running OPA Server

```bash
# Using Docker Compose (recommended)
docker-compose up -d opa

# Or manually
docker run -d -p 8181:8181 \
  -v $(pwd)/authorization-service/src/main/resources/policies:/policies \
  openpolicyagent/opa:latest-rootless \
  run --server --log-level=debug /policies
```

### Running the Service

```bash
# Navigate to service directory
cd authorization-service

# Run with Maven
mvn spring-boot:run

# Or build and run JAR
mvn clean package
java -jar target/authorization-service-0.0.1-SNAPSHOT.jar
```

The service will start on **http://localhost:8087**

## 📡 API Endpoints

### 1. Check Authorization

Main endpoint to check if an action is authorized.

```http
POST /api/authorization/check
Content-Type: application/json
Authorization: Bearer <JWT_TOKEN>

{
  "subject": "user-123",
  "object": "transaction",
  "action": "approve",
  "role": "ROLE_MANAGER",
  "branchId": "BRANCH_001",
  "approvalLimit": "1000000",
  "ip": "192.168.1.100",
  "time": "14:30",
  "channel": "WEB",
  "resourceId": "txn-456",
  "amount": 500000,
  "riskScore": 65
}
```

**Response:**

```json
{
  "allowed": true,
  "reason": "Access granted by RBAC and ABAC policies",
  "context": {
    "role": "ROLE_MANAGER",
    "ip": "192.168.1.100",
    "amount": 500000,
    "riskScore": 65
  },
  "evaluationTimeMs": 45
}
```

### 2. Batch Authorization Check

Check multiple requests at once.

```http
POST /api/authorization/check-batch
Content-Type: application/json
Authorization: Bearer <JWT_TOKEN>

[
  {
    "subject": "user-123",
    "object": "transaction",
    "action": "approve"
  },
  {
    "subject": "user-456",
    "object": "account",
    "action": "view"
  }
]
```

### 3. Health Check

Check service and OPA server health.

```http
GET /api/authorization/health
```

**Response:**

```json
{
  "status": "UP",
  "service": "authorization-service",
  "opa": {
    "status": "UP",
    "url": "http://localhost:8181"
  }
}
```

### 4. Reload Policies (Admin Only)

Trigger OPA to reload policies from disk.

```http
POST /api/authorization/policies/reload
Authorization: Bearer <ADMIN_JWT_TOKEN>
```

## 🔧 Configuration

Edit `src/main/resources/application.yml`:

```yaml
server:
  port: 8087

opa:
  url: http://localhost:8181
  policy:
    package: bank.authz

spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8180/realms/employee-portal
          jwk-set-uri: http://localhost:8180/realms/employee-portal/protocol/openid-connect/certs

services:
  account-service:
    url: http://localhost:8085
```

## 📋 OPA Rego Policies

The service uses **Rego** (Open Policy Agent's policy language) for declarative authorization:

**banking-authz.rego:**

```rego
package bank.authz

# Main authorization rule
allow = result if {
    # RBAC check
    rbac_allowed := check_rbac_permission
    rbac_allowed == true
    
    # ABAC check
    abac_result := check_abac_conditions
    abac_result.allowed == true
    
    result := {
        "allowed": true,
        "reason": "Access granted by RBAC and ABAC policies"
    }
}

# RBAC: Role-based permissions
permission_allowed("ROLE_ADMIN", _, _) if {
    true
}

permission_allowed("ROLE_MANAGER", "transaction", "approve") if {
    true
}

# ABAC: Context-aware checks
check_abac_conditions = result if {
    # 9 context checks
    ip_check.allowed == true
    time_check.allowed == true
    channel_check.allowed == true
    # ... more checks
    
    result := {"allowed": true, "reason": "All ABAC checks passed"}
}
```

### Policy Structure

Policies are stored in:
- `src/main/resources/policies/banking-authz.rego` - Rego policy rules
- `src/main/resources/policies/data.json` - Policy configuration data

### Viewing Policies

```bash
# View Rego policies
cat authorization-service/src/main/resources/policies/banking-authz.rego

# View policy data
cat authorization-service/src/main/resources/policies/data.json

# Test OPA directly
curl -X POST http://localhost:8181/v1/data/bank/authz/allow \
  -H "Content-Type: application/json" \
  -d '{
    "input": {
      "user": {"role": "ROLE_MANAGER"},
      "resource": {"object": "transaction", "action": "approve"},
      "context": {"ipAddress": "127.0.0.1", "hour": 14}
    }
  }'
```

## 🔗 Integration with Other Services

### Transaction Service Example

```java
@RestController
@RequestMapping("/api/transactions")
public class TransactionController {
    
    @Autowired
    private AuthorizationServiceClient authClient;
    
    @PostMapping("/{id}/approve")
    public ResponseEntity<?> approveTransaction(@PathVariable String id) {
        
        // Build authorization request
        AuthorizationRequest authRequest = AuthorizationRequest.builder()
            .subject(getCurrentUserId())
            .object("transaction")
            .action("approve")
            .resourceId(id)
            .role(getUserRole())
            .ip(getClientIp())
            .time(getCurrentTime())
            .amount(getTransactionAmount(id))
            .build();
        
        // Call Authorization Service
        AuthorizationResult result = authClient.checkAuthorization(authRequest);
        
        if (!result.isAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body("Access denied: " + result.getReason());
        }
        
        // Proceed with approval
        return ResponseEntity.ok(transactionService.approve(id));
    }
}
```

## 🧪 Testing

### Manual Testing with curl

```bash
# 1. Get JWT token from Keycloak
TOKEN=$(curl -X POST http://localhost:8180/realms/employee-portal/protocol/openid-connect/token \
  -d "client_id=banking-client" \
  -d "username=manager1" \
  -d "password=password" \
  -d "grant_type=password" | jq -r '.access_token')

# 2. Check authorization
curl -X POST http://localhost:8087/api/authorization/check \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "subject": "user-123",
    "object": "transaction",
    "action": "approve",
    "role": "ROLE_MANAGER",
    "ip": "127.0.0.1",
    "time": "14:30",
    "channel": "WEB",
    "amount": 500000
  }'

# 3. Check health
curl http://localhost:8087/api/authorization/health
```

### Test OPA Directly

```bash
# Health check
curl http://localhost:8181/health

# Test policy evaluation
curl -X POST http://localhost:8181/v1/data/bank/authz/allow \
  -H "Content-Type: application/json" \
  -d '{
    "input": {
      "user": {
        "role": "ROLE_MANAGER",
        "branchId": "BR001"
      },
      "resource": {
        "object": "transaction",
        "action": "approve",
        "amount": 50000000
      },
      "context": {
        "ipAddress": "127.0.0.1",
        "hour": 14,
        "channel": "WEB"
      }
    }
  }' | jq
```

### Unit Testing

```bash
mvn test
```

## 📊 Monitoring

### Actuator Endpoints

- Health: `http://localhost:8087/actuator/health`
- Metrics: `http://localhost:8087/actuator/metrics`
- Info: `http://localhost:8087/actuator/info`

### OPA Health

The service performs automatic health checks on OPA at startup and provides health status via `/api/authorization/health`.

## 🐛 Troubleshooting

### Issue: OPA server not reachable

**Solution:**
```bash
# Check if OPA is running
curl http://localhost:8181/health

# Start OPA if not running
docker-compose up -d opa

# Check OPA logs
docker logs bank_hybrid_opa
```

### Issue: Authorization always returns false

**Possible causes:**
1. OPA server not running
2. Policy package name mismatch
3. Input format doesn't match Rego expectations
4. No matching policy rule

**Debug:**
```bash
# Enable debug logging in application.yml
logging:
  level:
    org.vgu.authorizationservice: DEBUG

# Test OPA directly with same input
curl -X POST http://localhost:8181/v1/data/bank/authz/allow \
  -H "Content-Type: application/json" \
  -d @test-input.json
```

### Issue: Cannot connect to Account Service (PIP)

**Solution:**
- Ensure Account Service is running on port 8085
- Check `services.account-service.url` in application.yml
- The service continues without PIP data if unavailable (may affect ABAC checks)

### Issue: Policies not reloading

**Solution:**
```bash
# Reload policies via API
curl -X POST http://localhost:8087/api/authorization/policies/reload \
  -H "Authorization: Bearer <ADMIN_TOKEN>"

# Or restart OPA container
docker-compose restart opa
```

## 🎯 ABAC Context Attributes

The service supports **9 context-aware attributes**:

1. **IP Address** - Network-based access control (whitelist)
2. **Time/Business Hours** - Temporal restrictions (8:00-18:00)
3. **Channel** - Request channel (WEB, MOBILE, BRANCH, INTERNAL)
4. **Amount** - Transaction amount validation
5. **Risk Score** - Account risk assessment
6. **Branch ID** - Location-based access
7. **Network Zone** - Network security zones (INTERNAL, VPN, EXTERNAL)
8. **Daily Limits** - Temporal transaction limits
9. **Resource Status** - Account/transaction status

All checks are implemented in Rego policies (`banking-authz.rego`).

## 🔒 Security

- JWT validation via Keycloak
- Admin-only endpoints for policy management
- Stateless architecture
- Fail-closed: Returns `false` on errors
- OPA server isolation (separate container)

## 📚 References

- [OPA Documentation](https://www.openpolicyagent.org/docs/latest/)
- [Rego Language](https://www.openpolicyagent.org/docs/latest/policy-language/)
- [Spring Boot](https://spring.io/projects/spring-boot)
- [Spring WebFlux](https://docs.spring.io/spring-framework/reference/web/webflux.html)

## 👥 Support

For issues or questions, contact the development team.

---

**Service Status:** ✅ Production Ready  
**Version:** 2.0.0 (OPA-based)  
**Last Updated:** December 2025
