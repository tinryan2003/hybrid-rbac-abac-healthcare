# Policy Service Setup Complete

## Summary

Policy service has been successfully configured and set up for IAM-style policy management in the hospital system.

## Changes Made

### 1. Dependencies (pom.xml)
- Updated to include all necessary Spring Boot dependencies:
  - `spring-boot-starter-web`
  - `spring-boot-starter-data-jpa`
  - `spring-boot-starter-validation`
  - `spring-boot-starter-security`
  - `spring-boot-starter-oauth2-resource-server`
  - `mysql-connector-j`
  - `spring-boot-starter-actuator`
  - `lombok`

### 2. Configuration (application.yml)
- Port: **8101**
- Database: **hospital_policies**
- MySQL connection configured
- Keycloak JWT authentication configured
- Hibernate schema validation: `validate`
- Logging levels configured
- Actuator endpoints exposed

### 3. Security Configuration (SecurityConfig.java)
- OAuth2 Resource Server with JWT authentication
- Keycloak integration for role extraction from `realm_access.roles`
- Public endpoints: `/actuator/health`, `/actuator/info`, `/api/policies/health`, `/api/policies/evaluate`
- All other `/api/**` endpoints require authentication
- Fixed: Uses `Converter<Jwt, Collection<GrantedAuthority>>` instead of extending final `JwtGrantedAuthoritiesConverter`

### 4. Entity Models Created
IAM-style policy entities with JSON storage:

#### Policy.java
- Maps to `policies` table
- ENUMs: `effect` (Allow, Deny)
- **JSON fields** stored as TEXT:
  - `subjects` - WHO (roles, users, groups)
  - `actions` - WHAT actions are allowed/denied
  - `resources` - ON WHAT resources
  - `conditions` - WHEN/WHERE conditions apply
  - `tags` - Organization tags
- Fields: tenant_id, policy_id, policy_name, enabled, version
- Supports multi-tenant with tenant_id

#### PolicyEvaluationLog.java
- Maps to `policy_evaluation_logs` table
- ENUMs: `decision` (ALLOW, DENY, NOT_APPLICABLE)
- JSON fields: `matched_policies`, `context`
- Logs policy evaluation for debugging and auditing

### 5. Repositories Created
- `PolicyRepository`
- `PolicyEvaluationLogRepository`

All with relevant finder methods for querying by tenant, policy ID, effect, etc.

### 6. Controllers Created
- `HealthController` with `/api/policies/health` endpoint

## Key Features

### IAM-Style Policies
The service implements AWS IAM-style policies with:
- **Effect**: Allow or Deny
- **Subjects**: WHO (roles, departments, users)
- **Actions**: WHAT operations (patient:view, prescription:create)
- **Resources**: ON WHAT (patient_record, lab_order)
- **Conditions**: WHEN/WHERE (same_department, time_range, sensitivity_level)

### Policy Evaluation
- Evaluate policies based on user context
- Log all evaluations for audit
- Conflict detection (Allow vs Deny)

### Multi-Tenant Support
- Tenant isolation with `tenant_id`
- Per-hospital policy management

## Key Fixes Applied

### ENUM Column Definition
ENUM columns use `columnDefinition` to match MySQL schema:
```java
@Column(name = "effect", columnDefinition = "ENUM('Allow', 'Deny')", nullable = false)
private String effect = "Allow";
```

### JSON Column Handling
JSON fields are stored as TEXT/String in Java:
```java
@Column(name = "subjects", columnDefinition = "JSON", nullable = false)
private String subjects;
```

Application code will need to parse/stringify JSON when reading/writing these fields.

### Security Configuration
Uses lambda implementation of `Converter<Jwt, Collection<GrantedAuthority>>`:
```java
Converter<Jwt, Collection<GrantedAuthority>> grantedAuthoritiesConverter = jwt -> {
    // Extract roles from realm_access.roles
    ...
};
```

## Database Schema
Connects to `hospital_policies` database with main tables:
- `policies` - IAM-style policy definitions
- `policy_versions` - Version history
- `policy_conflicts` - Conflict detection
- `policy_evaluation_logs` - Evaluation audit logs
- `policy_templates` - Reusable templates

## Running the Service

```bash
cd policy-service
./mvnw spring-boot:run
```

Service will start on **http://localhost:8101**

Health check: `GET http://localhost:8101/api/policies/health` (public endpoint)

## Testing

The service compiles successfully and initializes JPA EntityManagerFactory without schema validation errors.

## Notes

- Using Spring Boot 4.0.2
- JSON fields are stored as String in Java entities (application must handle JSON serialization)
- Follows the same security and architecture patterns as other services
- Ready for policy evaluation service layer implementation
- Supports IAM-style ABAC policies with conditions
- Multi-tenant capable
- Includes policy evaluation logging for debugging
