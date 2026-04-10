# Billing Service Setup Complete

## Summary

Billing service has been successfully configured and set up following the same patterns as all other services.

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
- Port: **8097** (8096 was in use by pharmacy-service)
- Database: **hospital_billing**
- MySQL connection configured
- Keycloak JWT authentication configured
- Hibernate schema validation: `validate`
- Logging levels configured
- Actuator endpoints exposed

### 3. Security Configuration (SecurityConfig.java)
- OAuth2 Resource Server with JWT authentication
- Keycloak integration for role extraction from `realm_access.roles`
- Public endpoints: `/actuator/health`, `/actuator/info`, `/api/billing/health`, `/api/billing/keycloak/**`
- All other `/api/**` endpoints require authentication
- Fixed: Uses `Converter<Jwt, Collection<GrantedAuthority>>` instead of extending final `JwtGrantedAuthoritiesConverter`

### 4. Entity Models Created
All entities properly mapped with **columnDefinition** for ENUM fields to match MySQL schema:

#### Invoice.java
- Maps to `invoices` table
- ENUMs: `status` (PENDING, PAID, PARTIALLY_PAID, CANCELLED, REFUNDED)
- Main invoice entity with patient, amounts, insurance info
- OneToMany relationship with InvoiceItem

#### InvoiceItem.java
- Maps to `invoice_items` table
- ENUMs: `item_type` (CONSULTATION, LAB_TEST, IMAGING, PROCEDURE, MEDICATION, ROOM_CHARGE, OTHER)
- Individual line items in an invoice

#### Payment.java
- Maps to `payments` table
- ENUMs: `payment_method` (CASH, CREDIT_CARD, DEBIT_CARD, BANK_TRANSFER, INSURANCE, OTHER), `status` (PENDING, COMPLETED, FAILED, REFUNDED)
- Payment records for invoices

#### ServicePricing.java
- Maps to `service_pricing` table
- ENUMs: `service_category` (CONSULTATION, LAB, IMAGING, PROCEDURE, ROOM, OTHER)
- Catalog of service prices

#### PaymentRefund.java
- Maps to `payment_refunds` table
- ENUMs: `refund_method` (CASH, CREDIT_CARD, BANK_TRANSFER, OTHER), `status` (PENDING, APPROVED, REJECTED, COMPLETED)
- Refund records

### 5. Repositories Created
- `InvoiceRepository`
- `InvoiceItemRepository`
- `PaymentRepository`
- `ServicePricingRepository`
- `PaymentRefundRepository`

All with relevant finder methods for querying by patient, status, invoice number, etc.

### 6. Controllers Created
- `HealthController` with `/api/billing/health` endpoint

## Key Fixes Applied

### ENUM Column Definition
All ENUM columns use `columnDefinition` to match MySQL schema:
```java
@Column(name = "status", columnDefinition = "ENUM('PENDING', 'PAID', 'PARTIALLY_PAID', 'CANCELLED', 'REFUNDED')")
private String status = "PENDING";
```

This prevents Hibernate schema validation errors where it expects `VARCHAR` but finds `ENUM`.

### Security Configuration
Uses lambda implementation of `Converter<Jwt, Collection<GrantedAuthority>>` instead of trying to extend the final `JwtGrantedAuthoritiesConverter` class:
```java
Converter<Jwt, Collection<GrantedAuthority>> grantedAuthoritiesConverter = jwt -> {
    // Extract roles from realm_access.roles
    ...
};
```

## Database Schema
Connects to `hospital_billing` database with 5 main tables:
- `invoices` - Patient invoices
- `invoice_items` - Invoice line items
- `payments` - Payment records
- `service_pricing` - Service pricing catalog
- `payment_refunds` - Refund records

## Running the Service

```bash
cd billing-service
./mvnw spring-boot:run
```

Service will start on **http://localhost:8097**

Health check: `GET http://localhost:8097/api/billing/health` (public endpoint)

## Testing

The service compiles successfully and initializes JPA EntityManagerFactory without schema validation errors.

## Notes

- Using Spring Boot 4.0.2 (from original pom.xml)
- All ENUM fields stay as `String` in Java
- Follows the same security and architecture patterns as other services
- Ready for controller and service layer implementation
- Supports invoice management, payments, refunds, and service pricing
- Port changed to 8097 to avoid conflict with pharmacy-service (8096)
