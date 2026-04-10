# Hospital Database Seed Data Guide

## Tổng quan

Thư mục này chứa các script SQL để khởi tạo và seed data cho các database của Hospital Management System.

## Các file SQL hiện có

1. **01-hospital-users.sql** - Database và tables cho user service
   - ✅ Có seed data cho: doctor1, nurse1, admin1
   - ❌ THIẾU: pharmacist1, billing_clerk1, lab_tech1

2. **02-hospital-patients.sql** - Database và tables cho patient service
   - ✅ Có seed data cho: 1 patient

3. **03-hospital-appointments.sql** - Database và tables cho appointment service
   - ❌ THIẾU: Sample appointments

4. **04-hospital-pharmacy.sql** - Database và tables cho pharmacy service
   - ✅ Có seed data cho: 5 medicines
   - ❌ THIẾU: Sample prescriptions

5. **05-hospital-lab-orders.sql** - Database và tables cho lab service
   - ✅ Có seed data cho: 8 lab tests catalog
   - ❌ THIẾU: Sample lab orders và results

6. **06-hospital-billing.sql** - Database và tables cho billing service
   - ✅ Có seed data cho: 5 service pricing
   - ❌ THIẾU: Sample invoices và payments

7. **07-hospital-audit.sql** - Database và tables cho audit service
8. **08-hospital-policies.sql** - Database và tables cho policy service
9. **09-hospital-notifications.sql** - Database và tables cho notification service
10. **10-sample-policies-for-conflict-test.sql** - Sample policies để test conflict detection

## Các file mới được thêm

### 11-hospital-seed-additional-users.sql
**Mục đích**: Bổ sung seed data cho các users còn thiếu

**Nội dung**:
- pharmacist1 user và pharmacist record
- billing_clerk1 user và billing_clerk record  
- lab_tech1 user và lab_technician record

**LƯU Ý QUAN TRỌNG**:
- File này sử dụng placeholder UUIDs (`pharmacist1-keycloak-uuid`, etc.)
- **Bạn cần cập nhật các UUID này** để match với users thực tế trong Keycloak sau khi tạo users
- Hoặc chạy script này sau khi đã tạo users trong Keycloak và có UUIDs thực tế

**Cách sử dụng**:
```sql
-- 1. Tạo users trong Keycloak trước (pharmacist1, billing_clerk1, lab_tech1)
-- 2. Lấy UUIDs từ Keycloak
-- 3. Cập nhật các UUID trong file này
-- 4. Chạy script
source scripts/hospital-db/11-hospital-seed-additional-users.sql;
```

### 12-hospital-seed-sample-data.sql
**Mục đích**: Thêm sample data để test các chức năng

**Nội dung**:
- 2 sample prescriptions (1 PENDING, 1 APPROVED) với items
- 2 sample lab orders (1 PENDING, 1 COMPLETED) với results
- 3 sample invoices (PENDING, PAID, PARTIALLY_PAID) với payments
- 3 sample appointments (CONFIRMED, PENDING, COMPLETED)

**Cách sử dụng**:
```sql
source scripts/hospital-db/12-hospital-seed-sample-data.sql;
```

## Checklist dữ liệu

### ✅ Đã có đầy đủ
- [x] Departments (7 departments)
- [x] Medicines (5 medicines)
- [x] Lab tests catalog (8 tests)
- [x] Service pricing (5 services)
- [x] Basic users: doctor1, nurse1, admin1, patient1

### ❌ Còn thiếu (cần chạy các file mới)
- [ ] pharmacist1 user và record
- [ ] billing_clerk1 user và record
- [ ] lab_tech1 user và record
- [ ] Sample prescriptions
- [ ] Sample lab orders và results
- [ ] Sample invoices và payments
- [ ] Sample appointments

## Hướng dẫn setup đầy đủ

### Bước 1: Chạy các script cơ bản
```bash
# Chạy tất cả các script cơ bản (01-10)
mysql -u root -p < scripts/hospital-db/01-hospital-users.sql
mysql -u root -p < scripts/hospital-db/02-hospital-patients.sql
# ... các file khác
```

### Bước 2: Tạo users trong Keycloak
1. Đăng nhập vào Keycloak Admin Console
2. Tạo các users:
   - pharmacist1@hospital.com (role: PHARMACIST)
   - billing_clerk1@hospital.com (role: BILLING_CLERK)
   - lab_tech1@hospital.com (role: LAB_TECH)
3. Lấy UUIDs của các users này

### Bước 3: Cập nhật và chạy script bổ sung users
```sql
-- Mở file 11-hospital-seed-additional-users.sql
-- Thay thế các placeholder UUIDs bằng UUIDs thực tế từ Keycloak
-- Sau đó chạy:
source scripts/hospital-db/11-hospital-seed-additional-users.sql;
```

### Bước 4: Chạy script sample data
```sql
source scripts/hospital-db/12-hospital-seed-sample-data.sql;
```

## Kiểm tra dữ liệu

Sau khi chạy các script, bạn có thể kiểm tra:

```sql
-- Kiểm tra users
USE hospital_users;
SELECT * FROM users;
SELECT * FROM pharmacists;
SELECT * FROM billing_clerks;
SELECT * FROM lab_technicians;

-- Kiểm tra prescriptions
USE hospital_pharmacy;
SELECT * FROM prescriptions;
SELECT * FROM prescription_items;

-- Kiểm tra lab orders
USE hospital_lab;
SELECT * FROM lab_orders;
SELECT * FROM lab_results;

-- Kiểm tra invoices
USE hospital_billing;
SELECT * FROM invoices;
SELECT * FROM payments;

-- Kiểm tra appointments
USE hospital_appointments;
SELECT * FROM appointments;
```

## Lưu ý

1. **Keycloak UUIDs**: Các UUIDs trong script là placeholder. Bạn PHẢI cập nhật chúng sau khi tạo users trong Keycloak.

2. **Foreign Keys**: Các sample data phụ thuộc vào:
   - doctor_id = 1 (phải có doctor với ID 1)
   - patient_id = 1 (phải có patient với ID 1)
   - medicine_id = 1, 2, 3 (phải có medicines với các IDs này)

3. **Dates**: Các dates trong sample data sử dụng CURDATE() và DATE_SUB/DATE_ADD để tạo dates tương đối, giúp test các trường hợp khác nhau.

4. **Testing**: Sample data được thiết kế để test các trường hợp:
   - PENDING, APPROVED, DISPENSED prescriptions
   - PENDING, COMPLETED lab orders
   - PENDING, PAID, PARTIALLY_PAID invoices
   - CONFIRMED, PENDING, COMPLETED appointments
