# User-Service: Employees & Doctors – Tổng quan code

## 1. Luồng request (Gateway → User-Service)

| Frontend gọi | Gateway | StripPrefix=1 | User-Service nhận |
|--------------|---------|----------------|-------------------|
| `GET /api/users/doctors` | Path=/api/users/** → uri 8090 | Bỏ "api" | `GET /users/doctors` |
| `GET /api/users/employees` | Path=/api/users/** | Bỏ "api" | `GET /users/employees` |
| `GET /api/users/wards` | Path=/api/users/** | Bỏ "api" | `GET /users/wards` |

Gateway map **toàn bộ** `/api/users/**` sang resource **"user"** khi gọi OPA (AuthorizationEnforcementFilter).

---

## 2. Controller – UserController.java

- **Base path:** `@RequestMapping("/users")` (vì gateway đã strip "api", request tới service là `/users/...`).

### Doctors

| Method | Path | Mô tả |
|--------|------|--------|
| GET | `/users/doctors` | Danh sách tất cả bác sĩ |
| GET | `/users/doctors/{doctorId}` | Chi tiết bác sĩ theo ID |
| GET | `/users/doctors/keycloak/{keycloakUserId}` | Bác sĩ theo Keycloak ID |
| GET | `/users/doctors/department/{departmentId}` | Bác sĩ theo khoa |
| GET | `/users/doctors/department/{departmentId}/active` | Bác sĩ active theo khoa |
| GET | `/users/doctors/hospital/{hospitalId}` | Bác sĩ theo bệnh viện |
| POST | `/users/doctors` | Tạo bác sĩ (Body: DoctorCreateRequest) |

### Employees

| Method | Path | Mô tả |
|--------|------|--------|
| GET | `/users/employees` | Danh sách tất cả nhân viên (doctors + nurses + admins), sort theo name |

### Wards

| Method | Path | Mô tả |
|--------|------|--------|
| GET | `/users/wards` | Danh sách wards (hospitalId + wardId + doctorCount, nurseCount). Query: `?hospitalId=...` (optional) |

Tất cả endpoint trên đều `@PreAuthorize("isAuthenticated()")` – chỉ cần JWT hợp lệ; **không** check role tại user-service (authorization do gateway + OPA xử lý).

---

## 3. Security – SecurityConfig.java

- **PermitAll:** `/actuator/health`, `/actuator/info`, `/users/health`, `/users/keycloak/**`
- **Authenticated:** `/users/**` (phải có JWT)
- **OAuth2 Resource Server:** JWT từ Keycloak (`issuer-uri`, `jwk-set-uri` trong application.yml)
- **JwtAuthenticationConverter:** Lấy `realm_access.roles` → `ROLE_<ROLE>` (ví dụ ROLE_ADMIN)

User-service **không** từ chối theo role; nếu gateway đã cho qua (OPA allow) thì request tới đây chỉ cần token hợp lệ.

---

## 4. Service – UserService.java

### Doctors

- **getAllDoctors():** `doctorRepository.findAll()` → map sang `DoctorResponse`.
- **getDoctorById / getDoctorByKeycloakId:** Find by id hoặc keycloakUserId, throw `UserNotFoundException` nếu không có.
- **getDoctorsByDepartment / getActiveDoctorsByDepartment / getDoctorsByHospital:** Query theo department hoặc hospital.
- **createDoctor(DoctorCreateRequest):** Tạo user trong Keycloak (KeycloakAdminService), tạo bản ghi User + Doctor trong DB, set department/hospital/ward/positionLevel.

### Employees

- **getAllEmployees():** Gộp 3 nguồn:
  - `doctorRepository.findAll()` → `mapDoctorToEmployeeResponse` (role="DOCTOR")
  - `nurseRepository.findAll()` → `mapNurseToEmployeeResponse` (role="NURSE")
  - `adminRepository.findAll()` → `mapAdminToEmployeeResponse` (role="ADMIN")
  - Sort theo `name`, trả về `List<EmployeeResponse>`.

### Wards

- **getWards(hospitalId):** Lấy cặp (hospitalId, wardId) phân biệt từ doctors và nurses, filter theo `hospitalId` nếu có, đếm số doctor/nurse theo từng ward → `List<WardSummaryDto>`.

---

## 5. DTO & Model

- **DoctorResponse:** doctorId, userId, keycloakUserId, name, gender, field, birthday, emailAddress, phoneNumber, departmentId, departmentName, hospitalId, wardId, positionLevel, isActive, hiredDate, createdAt.
- **EmployeeResponse:** role (DOCTOR/NURSE/ADMIN), entityId, userId, keycloakUserId, name, email, phoneNumber, gender, birthday, departmentId, departmentName, hospitalId, wardId, positionLevel, field, adminLevel, isActive, hiredDate, createdAt.
- **WardSummaryDto:** hospitalId, wardId, doctorCount, nurseCount.

---

## 6. Repository – DoctorRepository.java

- `findByUser_KeycloakUserId`, `findByUser_UserId`, `findByDepartment_DepartmentId`, `findByHospitalId`, `findByWardId`, `findByHospitalIdAndWardId`, `findByIsActiveTrue`, `findActiveDoctorsByDepartment`.
- `findDistinctHospitalIdAndWardId()` (dùng cho getWards).

---

## 7. Kết luận kiểm tra

- **Routing:** Gateway → user-service (port 8090), StripPrefix=1 → path khớp với `@RequestMapping("/users")`.
- **Authorization:** Do gateway + OPA (resource "user"); user-service chỉ kiểm tra `isAuthenticated()`.
- **Employees:** Gộp doctors, nurses, admins; không có endpoint riêng theo role (frontend dùng `/employees` cho “All employees”).
- **Doctors:** CRUD + filter theo department/hospital; tạo doctor có tích hợp Keycloak.
- **Wards:** Đọc từ dữ liệu doctors/nurses (hospitalId, wardId), không có bảng ward riêng.

Nếu frontend gọi đúng `GET /api/users/doctors` hoặc `GET /api/users/employees` mà vẫn "Access denied" thì lỗi nằm ở **gateway hoặc OPA** (resource "user" cho role ADMIN), không phải ở user-service.
