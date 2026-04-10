# Spring Cloud Gateway - Tổng Hợp Các Routes

## ✅ Tất Cả Services Đã Được Cấu Hình

Gateway đang chạy trên **port 8083** và đã bao gồm routes cho **11 microservices**:

| # | Service | Gateway Route | Target Port | Status |
|---|---------|---------------|-------------|--------|
| 1 | **User Service** | `/api/users/**` | 8080 | ✅ Đã cấu hình |
| 2 | **Patient Service** | `/api/patients/**` | 8091 | ✅ Đã cấu hình |
| 3 | **Appointment Service** | `/api/appointments/**` | 8093 | ✅ Đã cấu hình |
| 4 | **Lab Service** | `/api/lab/**` | 8094 | ✅ Đã cấu hình |
| 5 | **Pharmacy Service** | `/api/pharmacy/**` | 8096 | ✅ Đã cấu hình |
| 6 | **Billing Service** | `/api/billing/**` | 8098 | ✅ Đã cấu hình |
| 7 | **Authorization Service** | `/api/authorization/**` | 8102 | ✅ Đã cấu hình |
| 8 | **Audit Service** | `/api/audit/**` | 8090 | ✅ Đã cấu hình |
| 9 | **Notification Service** | `/api/notifications/**` | 8088 | ✅ Đã cấu hình |
| 10 | **Reporting Service** | `/api/reports/**` | 8100 | ✅ Đã cấu hình |
| 11 | **Policy Service** | `/api/policies/**` | 8101 | ✅ Đã cấu hình |

## 🌐 Cách Sử Dụng Gateway

### Từ Frontend
Thay vì gọi trực tiếp đến từng service:
```javascript
// ❌ KHÔNG NÊN - Gọi trực tiếp service
fetch('http://localhost:8091/patients/123')
fetch('http://localhost:8093/appointments')
fetch('http://localhost:8096/pharmacy/medicines')
```

Hãy gọi thông qua Gateway:
```javascript
// ✅ NÊN - Gọi qua Gateway
const GATEWAY_URL = 'http://localhost:8083';

fetch(`${GATEWAY_URL}/api/patients/123`)
fetch(`${GATEWAY_URL}/api/appointments`)
fetch(`${GATEWAY_URL}/api/pharmacy/medicines`)
```

### Ví Dụ Cụ Thể

#### 1. User Service
```bash
# Qua Gateway
curl http://localhost:8083/api/users/profile

# Gateway sẽ forward đến
→ http://localhost:8080/users/profile
```

#### 2. Patient Service
```bash
# Qua Gateway
curl http://localhost:8083/api/patients/123

# Gateway sẽ forward đến
→ http://localhost:8091/patients/123
```

#### 3. Pharmacy Service
```bash
# Qua Gateway
curl http://localhost:8083/api/pharmacy/prescriptions

# Gateway sẽ forward đến
→ http://localhost:8096/pharmacy/prescriptions
```

## 🔐 Bảo Mật Qua Gateway

Gateway đã được cấu hình với:

### 1. Keycloak JWT Authentication
- Tất cả requests qua `/api/**` đều cần JWT token
- Token được validate với Keycloak realm: `hospital-realm`

### 2. Role-Based Access Control (RBAC)
```yaml
# Admin endpoints
/api/admin/** → SYSTEM_ADMIN, HOSPITAL_ADMIN

# Patient endpoints  
/api/patients/** → DOCTOR, NURSE, RECEPTIONIST, SYSTEM_ADMIN, HOSPITAL_ADMIN

# Appointment endpoints
/api/appointments/** → DOCTOR, NURSE, RECEPTIONIST, PATIENT, SYSTEM_ADMIN

# Lab endpoints
/api/lab/** → DOCTOR, LAB_TECH, SYSTEM_ADMIN

# Pharmacy endpoints
/api/pharmacy/** → DOCTOR, PHARMACIST, SYSTEM_ADMIN

# Billing endpoints
/api/billing/** → BILLING_CLERK, SYSTEM_ADMIN, HOSPITAL_ADMIN
```

### 3. CORS Configuration
- Allowed Origins: `http://localhost:3000`, `http://localhost:5000`, `http://localhost:5173`
- Allowed Methods: GET, POST, PUT, DELETE, OPTIONS, PATCH
- Credentials: Enabled

## 📊 Kiểm Tra Gateway

### 1. Health Check
```bash
curl http://localhost:8083/actuator/health
```

### 2. Xem Tất Cả Routes
```bash
curl http://localhost:8083/actuator/gateway/routes
```

### 3. Gateway Info
```bash
curl http://localhost:8083/actuator/gateway/routes | jq
```

## 🚀 Lợi Ích Khi Dùng Gateway

### 1. Single Entry Point
- Frontend chỉ cần biết 1 URL: `http://localhost:8083`
- Không cần quan tâm đến port của từng service

### 2. Centralized Authentication
- JWT validation tại gateway
- Không cần validate lại ở mỗi service

### 3. Load Balancing (Tương Lai)
- Có thể add multiple instances của service
- Gateway sẽ tự động load balance

### 4. Rate Limiting
- Đã cấu hình Redis cho rate limiting
- 60 requests/minute per user

### 5. Circuit Breaker
- Tự động retry khi service lỗi
- Fallback response khi service down

## 📝 Cấu Hình Chi Tiết

### StripPrefix Filter
Tất cả routes đều dùng `StripPrefix=1`:

```yaml
# Request từ client
GET http://localhost:8083/api/patients/123

# Gateway removes "/api" (prefix) và forward
→ GET http://localhost:8091/patients/123
```

### Route Pattern
```yaml
routes:
  - id: service-name
    uri: http://localhost:PORT
    predicates:
      - Path=/api/service-path/**
    filters:
      - StripPrefix=1
```

## 🎯 Tóm Tắt

✅ **Đã cấu hình đầy đủ**: Tất cả 11 services đã có route trong gateway  
✅ **Security**: JWT authentication + RBAC đã được setup  
✅ **CORS**: Đã cấu hình cho frontend  
✅ **Rate Limiting**: Redis-based rate limiting  
✅ **Health Check**: Actuator endpoints đã expose  
✅ **Logging**: Debug logging đã enable  

**Gateway sẵn sàng sử dụng!** 🚀

## 📖 Hướng Dẫn Sử Dụng Cho Frontend Developer

### Setup API Client
```typescript
// src/config/api.ts
import axios from 'axios';

export const apiClient = axios.create({
  baseURL: 'http://localhost:8083/api', // Gateway URL
  headers: {
    'Content-Type': 'application/json'
  }
});

// Add Keycloak token interceptor
apiClient.interceptors.request.use((config) => {
  const token = keycloak.token; // Từ Keycloak
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});
```

### Example API Calls
```typescript
// Get patients
const patients = await apiClient.get('/patients');

// Create appointment
const appointment = await apiClient.post('/appointments', {
  patientId: 123,
  doctorId: 456,
  date: '2026-02-15'
});

// Get prescriptions
const prescriptions = await apiClient.get('/pharmacy/prescriptions/123');
```

Tất cả đều đi qua Gateway tại port **8083**! 🎯
