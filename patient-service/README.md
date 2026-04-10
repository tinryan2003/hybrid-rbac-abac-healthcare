# Patient Service - Hospital Management System

## Overview

Patient Service quản lý thông tin bệnh nhân, hồ sơ y tế, và dị ứng trong hệ thống Hospital Management.

- **Port**: 8085
- **Database**: `hospital_patients`
- **Authentication**: Keycloak JWT (realm: `hospital-realm`)

---

## Features

### 1. Patient Management
- Thông tin cá nhân bệnh nhân (firstname, lastname, birthday, gender, phone, address)
- Link với Keycloak account (`keycloak_user_id`)
- ABAC attribute: `hospital_id` (multi-hospital support)
- Tính tuổi tự động từ `birthday`
- Tìm kiếm bệnh nhân theo tên

### 2. Medical History
- Lưu trữ lịch sử khám bệnh
- Vitals: blood pressure, blood sugar, weight, height, temperature
- Medical prescriptions (đơn thuốc)
- Sắp xếp theo thời gian (mới nhất trước)

### 3. Patient Allergies
- Quản lý dị ứng của bệnh nhân
- Thông tin: allergen (chất gây dị ứng), severity, reaction
- Quan trọng cho Pharmacy Service (tránh kê thuốc gây dị ứng)

---

## API Endpoints

### Patient Endpoints

| Method | Endpoint | Role Required | Description |
|--------|----------|---------------|-------------|
| GET | `/api/patients/{patientId}` | DOCTOR, NURSE, RECEPTIONIST, ADMIN | Lấy thông tin bệnh nhân theo ID |
| GET | `/api/patients/keycloak/{keycloakUserId}` | Authenticated | Lấy thông tin bệnh nhân theo Keycloak ID |
| GET | `/api/patients/me` | PATIENT | Lấy thông tin bệnh nhân hiện tại (JWT) |
| GET | `/api/patients` | ADMIN, RECEPTIONIST | Lấy danh sách tất cả bệnh nhân |
| GET | `/api/patients/hospital/{hospitalId}` | DOCTOR, NURSE, ADMIN | Lấy bệnh nhân theo hospital |
| GET | `/api/patients/search?query={name}` | DOCTOR, NURSE, RECEPTIONIST, ADMIN | Tìm kiếm bệnh nhân theo tên |
| GET | `/api/patients/{patientId}/detail` | DOCTOR, NURSE, ADMIN | Lấy chi tiết đầy đủ (patient + medical history + allergies) |

### Medical History Endpoints

| Method | Endpoint | Role Required | Description |
|--------|----------|---------------|-------------|
| GET | `/api/patients/{patientId}/medical-history` | DOCTOR, NURSE, ADMIN | Lấy toàn bộ lịch sử y tế |
| GET | `/api/patients/{patientId}/medical-history/recent?limit={n}` | DOCTOR, NURSE, ADMIN | Lấy N bản ghi gần nhất (default: 10) |
| GET | `/api/patients/medical-history/{historyId}` | DOCTOR, NURSE, ADMIN | Lấy một bản ghi y tế theo ID |

### Patient Allergy Endpoints

| Method | Endpoint | Role Required | Description |
|--------|----------|---------------|-------------|
| GET | `/api/patients/{patientId}/allergies` | DOCTOR, NURSE, PHARMACIST, ADMIN | Lấy danh sách dị ứng của bệnh nhân |
| GET | `/api/patients/allergies/{allergyId}` | DOCTOR, NURSE, PHARMACIST, ADMIN | Lấy thông tin một dị ứng theo ID |

### Health Check

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/patients/health` | Health check endpoint |

---

## Database Schema

### patients table
```sql
- patient_id (PK)
- firstname, lastname
- address
- birthday
- gender
- phone_number
- emergency_contact
- photo_image (LONGBLOB)
- created_date, last_visited
- keycloak_user_id (UK) -- Link to Keycloak
- hospital_id (ABAC attribute)
```

### medical_history table
```sql
- id (PK)
- patient_id (FK)
- blood_pressure, blood_sugar, weight, height, temperature
- medical_pres (TEXT)
- creation_date
```

### patient_allergies table
```sql
- allergy_id (PK)
- patient_id (FK)
- allergen
- severity (MILD, MODERATE, SEVERE)
- reaction (TEXT)
- diagnosed_date
- created_at
```

---

## Running the Service

### 1. Start Dependencies

```bash
# Start MySQL
docker-compose up -d mysql

# Run database scripts
./scripts/setup-databases.sh

# Start Keycloak
docker-compose up -d keycloak
```

### 2. Run Patient Service

```bash
cd patient-service
./mvnw spring-boot:run
```

Service will start on **port 8085**.

### 3. Test API

```bash
# Get JWT token from Keycloak
TOKEN=$(curl -s -X POST http://localhost:8180/realms/hospital-realm/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=hospital-client" \
  -d "client_secret=YOUR_SECRET" \
  -d "username=doctor1" \
  -d "password=doctor123" \
  -d "grant_type=password" | jq -r '.access_token')

# Test via Gateway
curl -X GET http://localhost:8083/api/patients \
  -H "Authorization: Bearer $TOKEN"

# Test Patient Service directly
curl -X GET http://localhost:8085/api/patients/1 \
  -H "Authorization: Bearer $TOKEN"

# Get patient detail (with medical history + allergies)
curl -X GET http://localhost:8085/api/patients/1/detail \
  -H "Authorization: Bearer $TOKEN"
```

---

## RBAC/ABAC Authorization

### RBAC (Roles)
- **DOCTOR**: Xem/sửa hồ sơ bệnh nhân, lịch sử y tế, dị ứng
- **NURSE**: Xem hồ sơ bệnh nhân, cập nhật vitals
- **RECEPTIONIST**: Xem danh sách bệnh nhân, tìm kiếm
- **PHARMACIST**: Xem dị ứng (để tránh kê thuốc gây dị ứng)
- **PATIENT**: Chỉ xem thông tin của chính mình
- **SYSTEM_ADMIN, HOSPITAL_ADMIN**: Full access

### ABAC (Attributes)
- **hospital_id**: Giới hạn truy cập theo bệnh viện
  - Ví dụ: Bác sĩ ở HOSPITAL_A chỉ xem bệnh nhân ở HOSPITAL_A
  - Policy: `user.hospital_id == patient.hospital_id`

---

## Security Configuration

- **JWT Authentication**: Keycloak OAuth2 Resource Server
- **Role Extraction**: Từ `realm_access.roles` trong JWT
- **Session Management**: Stateless (không lưu session)
- **Public Endpoints**: `/actuator/health`, `/api/patients/health`, `/api/patients/keycloak/**`

---

## Dependencies

- Spring Boot 3.x
- Spring Data JPA
- Spring Security OAuth2 Resource Server
- MySQL Connector
- Lombok
- Spring Boot Actuator

---

## Integration with Other Services

### 1. User Service
- Doctors, nurses có thể xem/sửa thông tin bệnh nhân

### 2. Appointment Service
- Lấy thông tin bệnh nhân khi đặt lịch hẹn
- Reference: `patient_id`

### 3. Pharmacy Service
- Kiểm tra dị ứng trước khi kê đơn thuốc
- API: `GET /api/patients/{patientId}/allergies`

### 4. Lab Service
- Lấy thông tin bệnh nhân cho lab orders
- Reference: `patient_id`

### 5. Billing Service
- Lấy thông tin bệnh nhân cho hóa đơn
- Reference: `patient_id`

### 6. Authorization Service (OPA)
- Policy evaluation với `hospital_id` attribute
- Ví dụ: Doctor chỉ xem bệnh nhân cùng hospital

---

## TODO / Future Enhancements

- [ ] Add CREATE/UPDATE/DELETE endpoints (hiện tại chỉ có READ)
- [ ] Upload patient photo (photo_image field)
- [ ] Advanced search (by age range, gender, etc.)
- [ ] Patient consent management
- [ ] Integration with external EHR systems
- [ ] Patient portal (self-service)
- [ ] Appointment history in patient detail
- [ ] Prescription history in patient detail

---

## Troubleshooting

### 1. Database Connection Error
```
Error: Communications link failure
```
**Solution**: Đảm bảo MySQL đang chạy và database `hospital_patients` đã được tạo:
```bash
docker-compose up -d mysql
./scripts/setup-databases.sh
```

### 2. 401 Unauthorized
```
Error: Full authentication is required
```
**Solution**: Kiểm tra JWT token có hợp lệ, chưa expired, và đúng realm (`hospital-realm`).

### 3. 403 Forbidden
```
Error: Access is denied
```
**Solution**: Kiểm tra user có đúng role trong Keycloak. Ví dụ: endpoint `/api/patients` yêu cầu role ADMIN hoặc RECEPTIONIST.

---

## Contributing

1. Follow coding standards (Lombok, @PreAuthorize cho RBAC)
2. Update this README khi thêm endpoints mới
3. Write unit tests cho service layer
4. Test với Keycloak JWT token

---

## Contact

- Service Owner: Hospital Management Team
- Port: 8085
- Database: hospital_patients
- Keycloak Realm: hospital-realm
