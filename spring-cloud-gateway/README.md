# Banking API Gateway (Spring Cloud Gateway)

A production-ready API Gateway for the Hybrid RBAC-ABAC Banking System, providing secure routing, authentication, and rate limiting.

## 🏗️ Architecture Overview

```
┌─────────────┐         ┌──────────────────┐         ┌─────────────────┐
│  Next.js    │────────▶│  Spring Cloud    │────────▶│  Spring Boot    │
│  Frontend   │         │     Gateway      │         │  Backend (8081) │
└─────────────┘         └──────────────────┘         └─────────────────┘
                               │     │
                               │     └────────────────▶┌─────────────────┐
                               │                       │  Keycloak       │
                               │                       │  (8180)         │
                               │                       └─────────────────┘
                               │
                               └──────────────────────▶┌─────────────────┐
                                                       │  Redis          │
                                                       │  (6379)         │
                                                       └─────────────────┘
```

## ✨ Features

### 🔐 Security
- **JWT Authentication** - Validates tokens from Keycloak
- **RBAC Authorization** - Role-based access control (USER, MANAGER, ADMIN)
- **Role Extraction** - Automatically extracts Keycloak roles from JWT
- **User Context Enrichment** - Adds user headers to downstream requests

### 🚦 Resilience
- **Circuit Breaker** - Protects backend services from cascading failures
- **Retry Logic** - Automatic retry for transient failures
- **Fallback Responses** - Friendly error messages when services are down

### 🛡️ Rate Limiting
- **Redis-based Rate Limiting** - 60 requests per minute per user
- **IP-based Fallback** - Rate limiting by IP for unauthenticated requests
- **Graceful Degradation** - Fails open if Redis is unavailable

### 📊 Observability
- **Request Logging** - Detailed logs for all requests/responses
- **Actuator Endpoints** - Health checks and metrics
- **Performance Tracking** - Request duration monitoring

### 🌐 CORS
- **Configured for Next.js** - Allows requests from frontend (localhost:3000)
- **Credential Support** - Enables cookies and authentication headers

## 🚀 Getting Started

### Prerequisites

1. **Java 21** - Required for Spring Boot 4.0.0
2. **Redis** - For rate limiting (optional but recommended)
3. **Keycloak** - Running on port 8180 with `banking` realm
4. **Spring Boot Backend** - Running on port 8081

### Installation

1. **Install Redis** (if not already installed):

```bash
# Windows (using Chocolatey)
choco install redis-64

# Or use Docker
docker run -d -p 6379:6379 redis:latest
```

2. **Build the project**:

```bash
./mvnw clean install
```

3. **Run the gateway**:

```bash
./mvnw spring-boot:run
```

The gateway will start on **port 8080**.

## ⚙️ Configuration

### Environment Variables

You can override configuration using environment variables:

```bash
# Gateway port
SERVER_PORT=8080

# Backend service URL
BACKEND_URI=http://localhost:8081

# Keycloak configuration
KEYCLOAK_REALM=banking
KEYCLOAK_URL=http://localhost:8180

# Redis configuration
REDIS_HOST=localhost
REDIS_PORT=6379

# CORS allowed origins
CORS_ORIGINS=http://localhost:3000,http://localhost:5000
```

### application.properties

Key configuration sections:

```properties
# Gateway Routes
spring.cloud.gateway.routes[0].id=banking-backend
spring.cloud.gateway.routes[0].uri=http://localhost:8081
spring.cloud.gateway.routes[0].predicates[0]=Path=/api/**

# Keycloak OAuth2
spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8180/realms/banking

# Rate Limiting
MAX_REQUESTS_PER_MINUTE=60
```

## 🔑 API Routes

### Public Routes (No Authentication)

| Path | Description |
|------|-------------|
| `/actuator/**` | Health checks and metrics |
| `/auth/**` | Keycloak authentication endpoints |
| `/gateway/health` | Gateway health status |
| `/gateway/info` | Gateway information |

### Protected Routes (Authentication Required)

| Path | Required Role | Description |
|------|---------------|-------------|
| `/api/user/**` | USER, MANAGER, ADMIN | User-level endpoints |
| `/api/manager/**` | MANAGER, ADMIN | Manager-level endpoints |
| `/api/admin/**` | ADMIN | Admin-only endpoints |
| `/api/**` | Authenticated | All other API endpoints |

## 🔧 Custom Headers

The gateway adds the following headers to downstream requests:

| Header | Description | Example |
|--------|-------------|---------|
| `X-User-Id` | Keycloak user ID | `a1b2c3d4-e5f6-...` |
| `X-Username` | Username | `john.doe` |
| `X-User-Email` | User email | `john@example.com` |
| `X-User-Roles` | Comma-separated roles | `ROLE_USER,ROLE_MANAGER` |

## 🛠️ Development

### Testing the Gateway

1. **Check gateway health**:

```bash
curl http://localhost:8080/gateway/health
```

2. **Test authenticated request** (requires Keycloak token):

```bash
curl -H "Authorization: Bearer YOUR_JWT_TOKEN" \
     http://localhost:8080/api/accounts
```

3. **Test rate limiting**:

```bash
# Send 100 requests rapidly
for i in {1..100}; do
  curl http://localhost:8080/api/accounts
done
```

### Debugging

Enable debug logging in `application.properties`:

```properties
logging.level.org.springframework.cloud.gateway=DEBUG
logging.level.org.springframework.security=DEBUG
logging.level.org.vgu.springcloudgateway=DEBUG
```

## 📦 Project Structure

```
src/main/java/org/vgu/springcloudgateway/
├── config/
│   ├── SecurityConfig.java           # JWT validation & RBAC
│   ├── KeycloakRoleConverter.java    # Extracts Keycloak roles
│   ├── GatewayRoutesConfig.java      # Advanced routing with circuit breakers
│   └── RedisConfig.java              # Redis configuration
├── filter/
│   ├── AuthenticationFilter.java     # Adds user context headers
│   ├── LoggingFilter.java            # Request/response logging
│   └── RateLimitFilter.java          # Redis-based rate limiting
├── controller/
│   ├── FallbackController.java       # Circuit breaker fallbacks
│   └── GatewayController.java        # Gateway health endpoints
└── SpringCloudGatewayApplication.java
```

## 🔐 Keycloak Integration

### Required Keycloak Setup

1. **Create Realm**: `banking`
2. **Create Client**: `banking-backend`
3. **Create Roles**: `USER`, `MANAGER`, `ADMIN`
4. **Assign Roles** to users in the realm

### JWT Token Structure

The gateway expects JWT tokens with the following structure:

```json
{
  "sub": "user-id",
  "preferred_username": "john.doe",
  "email": "john@example.com",
  "realm_access": {
    "roles": ["USER", "MANAGER"]
  }
}
```

## 🚨 Error Handling

### Circuit Breaker Fallbacks

When the backend is unavailable, the gateway returns:

```json
{
  "timestamp": "2025-12-14T10:30:00",
  "status": 503,
  "error": "Service Temporarily Unavailable",
  "message": "The banking service is currently experiencing issues. Please try again later."
}
```

### Rate Limit Exceeded

```json
HTTP/1.1 429 Too Many Requests
X-RateLimit-Limit: 60
X-RateLimit-Remaining: 0
```

## 📊 Monitoring

### Actuator Endpoints

- `/actuator/health` - Health status
- `/actuator/gateway/routes` - Configured routes
- `/actuator/metrics` - Application metrics

## 🔄 Next Steps

1. **Connect Backend Service** - Ensure Spring Boot backend is running on port 8081
2. **Configure Keycloak** - Set up realm, client, and roles
3. **Set up Redis** - Required for rate limiting
4. **Test Integration** - Use Postman or curl to test routes
5. **Configure CORS** - Adjust for your frontend URL

## 🐛 Troubleshooting

### Issue: Gateway returns 401 Unauthorized

**Solution**: Check Keycloak configuration and JWT issuer URI.

### Issue: Rate limiting not working

**Solution**: Ensure Redis is running on port 6379.

### Issue: CORS errors from frontend

**Solution**: Update CORS configuration in `application.properties` with your frontend URL.

### Issue: Backend service not responding

**Solution**: Check if Spring Boot backend is running on port 8081 and routes are configured correctly.

## 📝 License

This project is part of the Hybrid RBAC-ABAC Banking System.

## 👥 Contributing

This is a banking system gateway - ensure all changes maintain security standards and backward compatibility.

