# Spring Cloud Gateway - Hospital System Configuration

## Summary

Spring Cloud Gateway has been verified and updated for the hospital management system with correct service port mappings.

## Configuration Status

### Gateway Port
- **Port**: 8083
- **Application Name**: hospital-gateway

## Service Routes (Updated Ports)

All routes have been verified and updated to match the actual running ports:

| Service | Route Path | Target URI | Port | Status |
|---------|-----------|------------|------|--------|
| **User Service** | `/api/users/**` | http://localhost:8080 | 8080 | ✅ Updated |
| **Patient Service** | `/api/patients/**` | http://localhost:8091 | 8091 | ✅ Verified |
| **Appointment Service** | `/api/appointments/**` | http://localhost:8093 | 8093 | ✅ Updated |
| **Lab Service** | `/api/lab/**` | http://localhost:8094 | 8094 | ✅ Verified |
| **Pharmacy Service** | `/api/pharmacy/**` | http://localhost:8096 | 8096 | ✅ Updated |
| **Billing Service** | `/api/billing/**` | http://localhost:8098 | 8098 | ✅ Updated |
| **Authorization Service** | `/api/authorization/**` | http://localhost:8102 | 8102 | ✅ Updated |
| **Audit Service** | `/api/audit/**` | http://localhost:8090 | 8090 | ✅ Updated |
| **Notification Service** | `/api/notifications/**` | http://localhost:8088 | 8088 | ✅ Verified |
| **Reporting Service** | `/api/reports/**` | http://localhost:8100 | 8100 | ✅ Updated |
| **Policy Service** | `/api/policies/**` | http://localhost:8101 | 8101 | ✅ Updated |

## Key Features

### 1. Security & Authentication
✅ **Multi-Realm JWT Support**: Configured for `hospital-realm`  
✅ **Role-Based Access Control**: Granular RBAC at gateway level  
✅ **Reactive Security**: WebFlux security for non-blocking operations

### 2. Keycloak Integration
- **Realm**: `hospital-realm`
- **Issuer URI**: `http://localhost:8180/realms/hospital-realm`
- **JWT Decoder**: Multi-realm support via `MultiRealmJwtDecoder`
- **Role Extraction**: `KeycloakRoleConverter` extracts roles from `realm_access.roles`

### 3. Authorization Rules (RBAC at Gateway)

#### Admin Endpoints
```
/api/admin/** → SYSTEM_ADMIN, HOSPITAL_ADMIN
```

#### User Management
```
/api/users/** → SYSTEM_ADMIN, HOSPITAL_ADMIN, DEPARTMENT_HEAD
```

#### Patient Endpoints
```
/api/patients/** → DOCTOR, NURSE, RECEPTIONIST, SYSTEM_ADMIN, HOSPITAL_ADMIN
```

#### Appointment Endpoints
```
/api/appointments/** → DOCTOR, NURSE, RECEPTIONIST, PATIENT, SYSTEM_ADMIN
```

#### Lab Endpoints
```
/api/lab/** → DOCTOR, LAB_TECH, SYSTEM_ADMIN
```

#### Pharmacy Endpoints
```
/api/pharmacy/** → DOCTOR, PHARMACIST, SYSTEM_ADMIN
```

#### Billing Endpoints
```
/api/billing/** → BILLING_CLERK, SYSTEM_ADMIN, HOSPITAL_ADMIN
```

#### Other API Endpoints
```
/api/** → Authenticated (any role)
```

### 4. CORS Configuration
Configured for frontend applications:
- **Allowed Origins**: 
  - `http://localhost:3000` (Next.js)
  - `http://localhost:5000` (Alternative frontend)
  - `http://localhost:5173` (Vite/React)
- **Allowed Methods**: GET, POST, PUT, DELETE, OPTIONS, PATCH
- **Allowed Headers**: All (`*`)
- **Credentials**: Enabled
- **Max Age**: 3600 seconds

### 5. Redis Integration
- **Purpose**: Rate limiting and caching
- **Host**: localhost
- **Port**: 6379
- **Status**: Optional (graceful degradation if unavailable)

### 6. Resilience & Circuit Breaker
- **Resilience4j**: Integrated for circuit breaker patterns
- **Circuit Breaker**: Protects backend services from cascading failures
- **Fallback**: `/fallback/**` endpoints for service unavailability

### 7. Observability
- **Actuator Endpoints**: 
  - `/actuator/health` - Health status
  - `/actuator/info` - Gateway info
  - `/actuator/gateway` - Gateway routes and filters
- **Request Logging**: `LoggingFilter` logs all requests/responses
- **Debug Logging**: Enabled for gateway, security, and netty HTTP client

## Architecture

```
┌──────────────────────────────────────────────────────────────┐
│                    Spring Cloud Gateway                      │
│                        (Port 8083)                           │
│                                                              │
│  ┌────────────────────────────────────────────────────┐    │
│  │              Security Filter Chain                  │    │
│  │  - JWT Validation (hospital-realm)                 │    │
│  │  - Role Extraction (KeycloakRoleConverter)         │    │
│  │  - RBAC Authorization                              │    │
│  └────────────────┬───────────────────────────────────┘    │
│                   │                                          │
│  ┌────────────────▼───────────────────────────────────┐    │
│  │              Request Routing                        │    │
│  │  - Path-based routing                              │    │
│  │  - StripPrefix filter                              │    │
│  │  - Load balancing (future)                         │    │
│  └────────────────┬───────────────────────────────────┘    │
│                   │                                          │
│  ┌────────────────▼───────────────────────────────────┐    │
│  │        Resilience & Rate Limiting                  │    │
│  │  - Circuit Breaker (Resilience4j)                  │    │
│  │  - Rate Limiting (Redis)                           │    │
│  │  - Retry Logic                                     │    │
│  └────────────────┬───────────────────────────────────┘    │
│                   │                                          │
└───────────────────┼──────────────────────────────────────────┘
                    │
        ┌───────────┴────────────┐
        │                        │
        ▼                        ▼
┌───────────────┐      ┌───────────────┐
│ User Service  │      │Patient Service│
│   (8080)      │      │   (8091)      │
└───────────────┘      └───────────────┘
        │                        │
        ▼                        ▼
     ... (9 more microservices) ...
```

## Request Flow

1. **Client Request** → Gateway (8083)
2. **CORS Check** → Allow if origin matches
3. **JWT Validation** → Decode and validate from Keycloak
4. **Role Extraction** → Extract roles from `realm_access.roles`
5. **RBAC Authorization** → Check if user has required role
6. **Rate Limiting** → Check Redis for rate limit (60 req/min)
7. **Circuit Breaker** → Check if service is healthy
8. **Route Matching** → Match path to service route
9. **Request Forwarding** → Forward to backend microservice
10. **Response** → Return response to client

## Public Endpoints (No Auth)

These endpoints do not require authentication:

- `/actuator/**` - Actuator health and metrics
- `/auth/**` - Keycloak authentication (not proxied)
- `/gateway/**` - Gateway health and info
- `/fallback/**` - Circuit breaker fallback responses

## Custom Filters

### 1. AuthenticationFilter
- Adds user context headers to downstream requests
- Headers: `X-User-Id`, `X-Username`, `X-User-Email`, `X-User-Roles`

### 2. LoggingFilter
- Logs all requests and responses
- Includes request path, method, status code, duration
- Debug level logging

### 3. RateLimitFilter
- Redis-based rate limiting
- 60 requests per minute per user
- IP-based fallback for unauthenticated requests
- Graceful degradation if Redis unavailable

## Dependencies

| Dependency | Version | Purpose |
|------------|---------|---------|
| Spring Boot | 3.2.5 | Base framework |
| Spring Cloud | 2023.0.0 | Cloud Gateway |
| Spring Security | 3.2.5 | OAuth2 Resource Server |
| Redis | Reactive | Rate limiting |
| Resilience4j | Latest | Circuit breaker |
| Lombok | Latest | Code simplification |

## Configuration Files

### Main Configuration
- **application.yml** - Main configuration with all routes
- **application-dev.yml** - Development profile
- **application-prod.yml** - Production profile

### Key Configuration Sections

#### Routes
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: http://localhost:8080
          predicates:
            - Path=/api/users/**
          filters:
            - StripPrefix=1
```

#### Security
```yaml
# Configured in SecurityConfig.java
# JWT validation via MultiRealmJwtDecoder
# RBAC rules in SecurityWebFilterChain
```

#### CORS
```yaml
spring:
  cloud:
    gateway:
      globalcors:
        corsConfigurations:
          "[/**]":
            allowedOrigins: http://localhost:3000
            allowedMethods: GET,POST,PUT,DELETE,OPTIONS,PATCH
            allowedHeaders: "*"
            allowCredentials: true
```

#### Redis
```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
```

## Running the Gateway

### Prerequisites
1. **Java 21** (required)
2. **Keycloak** running on port 8180 with `hospital-realm`
3. **Redis** running on port 6379 (optional but recommended)
4. **Backend Microservices** running on their respective ports

### Start Gateway
```bash
cd spring-cloud-gateway
./mvnw spring-boot:run
```

Gateway will start on **http://localhost:8083**

### Verify Gateway
```bash
# Health check
curl http://localhost:8083/actuator/health

# Gateway routes
curl http://localhost:8083/actuator/gateway/routes

# Gateway info
curl http://localhost:8083/gateway/info
```

## Testing

### 1. Test Public Endpoint
```bash
curl http://localhost:8083/gateway/health
```

Expected: `200 OK`

### 2. Test Protected Endpoint (No Auth)
```bash
curl http://localhost:8083/api/users
```

Expected: `401 Unauthorized`

### 3. Test Protected Endpoint (With Auth)
```bash
# Get token from Keycloak
TOKEN=$(curl -X POST http://localhost:8180/realms/hospital-realm/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=doctor1" \
  -d "password=password" \
  -d "grant_type=password" \
  -d "client_id=hospital-client" | jq -r '.access_token')

# Test with token
curl -H "Authorization: Bearer $TOKEN" \
     http://localhost:8083/api/patients
```

Expected: `200 OK` (forwarded to patient-service)

### 4. Test CORS
```bash
curl -X OPTIONS http://localhost:8083/api/users \
  -H "Origin: http://localhost:3000" \
  -H "Access-Control-Request-Method: GET" \
  -v
```

Expected: CORS headers in response

### 5. Test Rate Limiting
```bash
# Send 100 requests rapidly
for i in {1..100}; do
  curl -H "Authorization: Bearer $TOKEN" \
       http://localhost:8083/api/patients
done
```

Expected: `429 Too Many Requests` after 60 requests

## Implementation Status

| Component | Status | Notes |
|-----------|--------|-------|
| Port Configuration | ✅ Complete | All ports updated |
| Route Configuration | ✅ Complete | 11 services configured |
| Security Config | ✅ Complete | JWT + RBAC |
| Multi-Realm JWT | ✅ Complete | hospital-realm configured |
| Role-Based Access | ✅ Complete | Granular RBAC rules |
| CORS Configuration | ✅ Complete | Frontend origins configured |
| Rate Limiting | ✅ Complete | Redis-based |
| Circuit Breaker | ✅ Complete | Resilience4j integrated |
| Logging Filter | ✅ Complete | Request/response logging |
| Actuator | ✅ Complete | Health, info, metrics |
| Compilation | ✅ Success | No errors |
| Startup | ✅ Success | Port 8083 |

## Troubleshooting

### Issue: Gateway returns 401 Unauthorized
**Cause**: JWT token validation failed  
**Solution**: 
- Ensure Keycloak is running on port 8180
- Verify realm is `hospital-realm`
- Check JWT token is valid and not expired

### Issue: Gateway returns 403 Forbidden
**Cause**: User lacks required role  
**Solution**: 
- Check user has correct role in Keycloak
- Verify role extraction in logs
- Review RBAC rules in SecurityConfig

### Issue: CORS errors from frontend
**Cause**: Frontend origin not allowed  
**Solution**: 
- Add frontend origin to CORS configuration
- Check preflight OPTIONS requests are allowed

### Issue: Rate limiting not working
**Cause**: Redis not running or not reachable  
**Solution**: 
- Start Redis on port 6379
- Check Redis connection in logs
- Gateway fails open if Redis unavailable

### Issue: Service not responding
**Cause**: Backend service not running  
**Solution**: 
- Verify backend service is running on correct port
- Check route configuration in application.yml
- Review gateway logs for routing errors

## Performance Considerations

### 1. Non-Blocking I/O
- Uses Project Reactor (WebFlux) for reactive processing
- Non-blocking HTTP client (Netty)
- Efficient thread utilization

### 2. Rate Limiting
- Redis-based for distributed rate limiting
- Prevents backend overload
- Graceful degradation if Redis down

### 3. Circuit Breaker
- Prevents cascading failures
- Fast-fail when service down
- Automatic recovery when service healthy

### 4. Caching (Future)
- Can cache responses in Redis
- Reduces backend load
- Configurable TTL

## Security Best Practices

✅ **JWT Validation**: All requests validated against Keycloak  
✅ **RBAC at Gateway**: First line of defense  
✅ **CORS Configuration**: Only trusted origins allowed  
✅ **Rate Limiting**: Prevents abuse  
✅ **Circuit Breaker**: Protects backend services  
✅ **Secure Headers**: Custom headers for user context  
✅ **No Credential Leakage**: JWT not forwarded to backend

## Next Steps

1. **Configure Keycloak**: Create hospital-realm with required roles
2. **Start Redis**: For rate limiting and caching
3. **Start Microservices**: Ensure all 11 services are running
4. **Test Integration**: Use Postman or curl to test routes
5. **Monitor Logs**: Check for any routing or security issues
6. **Frontend Integration**: Update frontend to use gateway URL

## Status

**Gateway Status**: ✅ **READY FOR HOSPITAL SYSTEM**

- ✅ Compilation successful
- ✅ Startup successful on port 8083
- ✅ All service routes configured with correct ports
- ✅ Security configured for hospital-realm
- ✅ RBAC rules for hospital roles
- ✅ CORS configured for frontend
- ✅ Redis integration ready
- ✅ Circuit breaker configured
- ✅ Rate limiting enabled
- ✅ Actuator endpoints exposed
- ✅ Logging configured

**Last Updated**: 2026-02-01  
**Port**: 8083  
**Version**: 1.0.0-HOSPITAL  
**Spring Boot**: 3.2.5  
**Spring Cloud**: 2023.0.0
