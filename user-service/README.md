# User/Employee Management Service

## 📋 Overview

User Service quản lý thông tin nhân viên và cấu hình approval limits cho **J.P. Morgan Hierarchy-based Transaction Workflow**.

**Port:** 8090  
**Database:** `banking_users`  
**Role:** Employee profile management + Approval limit provider

---

## 🎯 Purpose

Service này cung cấp:
1. **Employee Profile Management** - Quản lý thông tin nhân viên
2. **Approval Limit Configuration** - Cấu hình hạn mức tự duyệt cho từng nhân viên
3. **Organizational Hierarchy** - Cấu trúc báo cáo (reports-to)
4. **Integration với Transaction Service** - Cung cấp approval limits cho workflow

---

## 🏗️ Architecture

```
User Service (Port 8090)
    │
    ├─► MySQL Database (banking_users)
    │   └─ employee_profiles table
    │
    ├─► Keycloak Integration
    │   └─ JWT Authentication
    │
    └─► REST APIs
        ├─ Public endpoints (for services)
        └─ Protected endpoints (for admins)
```

---

## 📊 Database Schema

### **Table: employee_profiles**

```sql
CREATE TABLE employee_profiles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    keycloak_user_id VARCHAR(50) UNIQUE NOT NULL,
    employee_number VARCHAR(20) UNIQUE NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    
    -- Role & Department
    role VARCHAR(50) NOT NULL,  -- ROLE_EMPLOYEE, ROLE_MANAGER, ROLE_DIRECTOR, ROLE_CEO
    department VARCHAR(50),
    position VARCHAR(100),
    branch_id VARCHAR(10),
    
    -- Approval Settings (CRITICAL for J.P. Morgan workflow)
    approval_limit DECIMAL(19,2) NOT NULL DEFAULT 0,
    can_approve_transactions BOOLEAN DEFAULT FALSE,
    
    -- Hierarchy
    reports_to_user_id BIGINT,  -- Manager ID
    
    -- Employment
    employment_status VARCHAR(20) DEFAULT 'ACTIVE',
    hire_date DATE,
    
    FOREIGN KEY (reports_to_user_id) REFERENCES employee_profiles(id)
);
```

---

## 🔑 Key Features

### **1. Approval Limit Management**

Mỗi nhân viên có `approval_limit` xác định số tiền tối đa họ có thể tự duyệt:

| Role | Approval Limit | Can Approve Others |
|------|----------------|-------------------|
| EMPLOYEE | 10,000,000 VND | ❌ No |
| MANAGER | 100,000,000 VND | ✅ Yes |
| DIRECTOR | 5,000,000,000 VND | ✅ Yes |
| CEO | Unlimited | ✅ Yes |

### **2. Organizational Hierarchy**

```
CEO (EMP001)
 ├─ Director Finance (EMP002)
 │  └─ Manager BR001 (EMP003)
 │     └─ Teller (EMP004)
 └─ Director Operations (EMP007)
    └─ Manager BR002 (EMP005)
       └─ Employee (EMP006)
```

### **3. Integration with Transaction Service**

Transaction Service gọi User Service để lấy approval limit:

```java
// Transaction Service calls:
GET /api/users/keycloak/{keycloakUserId}/approval-limit

// Response:
{
  "keycloakUserId": "employee-keycloak-uuid",
  "employeeNumber": "EMP004",
  "fullName": "Phạm Thị Employee",
  "role": "ROLE_EMPLOYEE",
  "approvalLimit": 10000000,
  "canApproveTransactions": false,
  "branchId": "BR001"
}
```

---

## 🌐 API Endpoints

### **Public Endpoints (No Auth - Internal Service Calls)**

```http
# Get approval limit (used by Transaction Service)
GET /api/users/keycloak/{keycloakUserId}/approval-limit

# Get approvers by role and branch (used by Transaction Service)
GET /api/users/approvers?role=ROLE_MANAGER&branchId=BR001

# Health check
GET /api/users/health
```

### **Protected Endpoints (JWT Required)**

```http
# Get user profile
GET /api/users/keycloak/{keycloakUserId}
GET /api/users/employee/{employeeNumber}

# Get users by role
GET /api/users/role/{role}

# Get users by branch
GET /api/users/branch/{branchId}

# Get user's manager
GET /api/users/keycloak/{keycloakUserId}/manager

# Get all active users
GET /api/users/active

# Create new user (ADMIN only)
POST /api/users

# Update approval limit (ADMIN/CEO only)
PUT /api/users/keycloak/{keycloakUserId}/approval-limit?newLimit=50000000

# Update employment status (ADMIN/DIRECTOR/CEO)
PUT /api/users/keycloak/{keycloakUserId}/status?status=INACTIVE

# Delete user (ADMIN only)
DELETE /api/users/{id}
```

---

## 🚀 Setup & Run

### **1. Create Database**

```bash
mysql -u root -p < src/main/resources/database/01-create-database.sql
mysql -u root -p < src/main/resources/database/02-create-tables.sql
mysql -u root -p < src/main/resources/database/03-seed-data.sql
```

### **2. Update Configuration**

Update `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/banking_users
    username: root
    password: YOUR_PASSWORD  # Update this
```

### **3. Run Service**

```bash
cd user-service
./mvnw spring-boot:run
```

Service will start on **port 8090**.

### **4. Verify**

```bash
# Health check
curl http://localhost:8090/api/users/health

# Get sample user approval limit
curl http://localhost:8090/api/users/keycloak/employee-keycloak-uuid/approval-limit
```

---

## 🔧 Configuration

### **application.yml**

```yaml
server:
  port: 8090

spring:
  application:
    name: user-service
  datasource:
    url: jdbc:mysql://localhost:3306/banking_users
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8180/realms/employee-portal
```

---

## 🧪 Sample Data

Chạy `03-seed-data.sql` để tạo 7 sample employees:

1. **CEO** (EMP001) - Approval: Unlimited
2. **Director Finance** (EMP002) - Approval: 5 tỷ VND
3. **Manager BR001** (EMP003) - Approval: 100 triệu VND
4. **Teller** (EMP004) - Approval: 10 triệu VND
5. **Manager BR002** (EMP005) - Approval: 100 triệu VND
6. **Employee BR002** (EMP006) - Approval: 10 triệu VND
7. **Director Operations** (EMP007) - Approval: 5 tỷ VND

---

## 🔗 Integration with Other Services

### **Transaction Service Integration**

```java
// In Transaction Service - Create Feign Client:
@FeignClient(name = "user-service", url = "http://localhost:8090")
public interface UserServiceClient {
    
    @GetMapping("/api/users/keycloak/{keycloakUserId}/approval-limit")
    ApprovalLimitDTO getApprovalLimit(@PathVariable String keycloakUserId);
    
    @GetMapping("/api/users/approvers")
    List<UserProfileDTO> getApproversByRoleAndBranch(
        @RequestParam String role,
        @RequestParam String branchId
    );
}

// Usage in TransactionService:
BigDecimal userApprovalLimit = userServiceClient
    .getApprovalLimit(keycloakUserId)
    .getApprovalLimit();
```

---

## 📝 Notes

- **Keycloak Integration**: Service sử dụng `keycloak_user_id` để link với Keycloak users
- **Approval Limits**: Giá trị này CRITICAL cho J.P. Morgan routing logic
- **Hierarchy**: `reports_to_user_id` dùng để xác định manager chain
- **Public Endpoints**: `/approval-limit` và `/approvers` là public để các services khác gọi được

---

**Status:** ✅ **COMPLETE**  
**Last Updated:** December 22, 2025  
**Version:** 1.0.0

