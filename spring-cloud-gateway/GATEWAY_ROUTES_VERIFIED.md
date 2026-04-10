# Spring Cloud Gateway - Đường Dẫn Đến Các Services (Đã Kiểm Tra & Sửa)

**Gateway:** `http://localhost:8083`  
**Cập nhật:** Đã đối chiếu với port thực tế của từng service trong project và sửa gateway cho khớp.

---

## Bảng Đường Dẫn (Gateway → Service)

| # | Service | Path qua Gateway | URI (Gateway forward tới) | Port thực tế | Trạng thái |
|---|---------|------------------|----------------------------|--------------|------------|
| 1 | User Service | `/api/users/**` | http://localhost:8090 | 8090 | ✅ Đã sửa (gateway trước: 8080) |
| 2 | Patient Service | `/api/patients/**` | http://localhost:8085 | 8085 | ✅ Khớp |
| 3 | Appointment Service | `/api/appointments/**` | http://localhost:8093 | 8093 | ✅ Khớp |
| 4 | Lab Service | `/api/lab/**` | http://localhost:8094 | 8094 | ✅ Khớp |
| 5 | Pharmacy Service | `/api/pharmacy/**` | http://localhost:8096 | 8096 | ✅ Khớp |
| 6 | Billing Service | `/api/billing/**` | http://localhost:8098 | 8098 | ✅ Khớp |
| 7 | Authorization Service | `/api/authorization/**` | http://localhost:8102 | 8102 | ✅ Khớp |
| 8 | Audit Service | `/api/audit/**` | http://localhost:8091 | 8091 | ✅ Đã sửa (gateway trước: 8090) |
| 9 | Notification Service | `/api/notifications/**` | http://localhost:8088 | 8088 | ✅ Khớp |
| 10 | Reporting Service | `/api/reports/**` | http://localhost:5523 | 5523 | ✅ Đã sửa (gateway trước: 8100) |
| 11 | Policy Service | `/api/policies/**` | http://localhost:8101 | 8101 | ✅ Đã sửa (policy-service trước: 8102 → đổi sang 8101) |

---

## Các Thay Đổi Đã Thực Hiện

### 1. `spring-cloud-gateway/src/main/resources/application.yml`
- **user-service:** `http://localhost:8080` → `http://localhost:8090` (khớp user-service port 8090).
- **audit-service:** `http://localhost:8090` → `http://localhost:8091` (khớp audit-service port 8091).
- **reporting-service:** `http://localhost:8100` → `http://localhost:5523` (khớp reporting-service port 5523).

### 2. `policy-service/src/main/resources/application.yml`
- **server.port:** `8102` → `8101` để tránh trùng với authorization-service (8102). Gateway đã trỏ policy-service tới 8101.

---

## Cách Request Đi Qua Gateway

Ví dụ từ frontend hoặc client:

```http
GET http://localhost:8083/api/patients/123
Authorization: Bearer <JWT>
```

Gateway sẽ:
1. Match predicate `Path=/api/patients/**`
2. Strip prefix 1 → path thành `/patients/123`
3. Forward tới `http://localhost:8085/patients/123` (Patient Service)

---

## Ví Dụ Theo Từng Service

| Gọi qua Gateway | Forward tới |
|-----------------|-------------|
| `GET http://localhost:8083/api/users/me` | `GET http://localhost:8090/users/me` |
| `GET http://localhost:8083/api/patients/1` | `GET http://localhost:8085/patients/1` |
| `POST http://localhost:8083/api/appointments` | `POST http://localhost:8093/appointments` |
| `GET http://localhost:8083/api/lab/orders` | `GET http://localhost:8094/lab/orders` |
| `GET http://localhost:8083/api/pharmacy/medicines` | `GET http://localhost:8096/pharmacy/medicines` |
| `GET http://localhost:8083/api/billing/invoices` | `GET http://localhost:8098/billing/invoices` |
| `POST http://localhost:8083/api/authorization/check` | `POST http://localhost:8102/authorization/check` |
| `GET http://localhost:8083/api/audit/events` | `GET http://localhost:8091/audit/events` |
| `GET http://localhost:8083/api/notifications` | `GET http://localhost:8088/notifications` |
| `GET http://localhost:8083/api/reports/...` | `GET http://localhost:5523/reports/...` |
| `GET http://localhost:8083/api/policies/health` | `GET http://localhost:8101/policies/health` |

---

## Lưu Ý

- **StripPrefix=1:** Gateway bỏ 1 segment đầu path. Request tới gateway `/api/patients/1` → backend nhận path `/patients/1` (không có `/api`).
- Các service phải expose API đúng path sau khi bỏ prefix (ví dụ patient-service có controller mapping `/patients/**`).
- JWT và CORS đã cấu hình trong gateway; frontend gọi `http://localhost:8083/api/...` kèm `Authorization: Bearer <token>`.

---

## Kiểm Tra Nhanh

```bash
# Health gateway
curl http://localhost:8083/actuator/health

# Xem danh sách routes
curl http://localhost:8083/actuator/gateway/routes
```

Sau khi sửa, đường dẫn gateway đến các service đã khớp với cấu hình port thực tế của từng service.
