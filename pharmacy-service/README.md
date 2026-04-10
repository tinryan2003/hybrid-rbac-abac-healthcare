# Pharmacy Service - Hospital Management System

## Overview

Pharmacy Service quản lý danh mục thuốc, đơn thuốc, dòng đơn thuốc và giao dịch tồn kho.

- **Port**: 8095
- **Database**: `hospital_pharmacy`
- **Gateway path**: `/api/pharmacy/**` (backend receives `/pharmacy/**` after StripPrefix=1)
- **Keycloak realm**: `hospital-realm`

---

## Features

### 1. Medicine Catalog
- Thông tin thuốc: name, generic_name, brand_name, category, dosage_form, strength
- Giá, tồn kho, mức đặt hàng lại (reorder_level)
- requires_prescription, controlled_substance
- ABAC: hospital_id

### 2. Prescriptions
- Đơn thuốc: doctor_id, patient_id, appointment_id (cross-service refs)
- prescription_date, diagnosis, notes, status (PENDING, APPROVED, DISPENSED, CANCELLED)
- dispensed_by_pharmacist_id, dispensed_at
- ABAC: hospital_id, sensitivity_level

### 3. Prescription Items
- Dòng đơn: medicine_id, dosage, frequency, duration_days, start_date, end_date
- quantity, quantity_dispensed, instructions, before_after_meal
- unit_price, total_price

### 4. Medicine Inventory Transactions
- Giao dịch tồn kho: IN, OUT, ADJUSTMENT, EXPIRED
- reference_id, reference_type (PRESCRIPTION, PURCHASE, ADJUSTMENT)
- performed_by_keycloak_id, hospital_id

---

## API Endpoints

### Medicines (`/pharmacy/medicines`)

| Method | Endpoint | Roles | Description |
|--------|----------|--------|-------------|
| GET | `/{medicineId}` | DOCTOR, NURSE, PHARMACIST, ADMIN | Lấy thuốc theo ID |
| GET | `/` | DOCTOR, NURSE, PHARMACIST, ADMIN | Danh sách thuốc |
| GET | `/hospital/{hospitalId}` | PHARMACIST, ADMIN | Thuốc theo bệnh viện |
| GET | `/active` | DOCTOR, NURSE, PHARMACIST, ADMIN | Thuốc đang active |
| GET | `/search?query=` | DOCTOR, NURSE, PHARMACIST, ADMIN | Tìm theo tên/generic |
| GET | `/category/{category}` | DOCTOR, NURSE, PHARMACIST, ADMIN | Thuốc theo category |
| GET | `/low-stock` | PHARMACIST, ADMIN | Thuốc sắp hết (≤ reorder_level) |
| GET | `/{medicineId}/inventory-transactions` | PHARMACIST, ADMIN | Lịch sử giao dịch tồn kho |
| GET | `/health` | Public | Health check |

### Prescriptions (`/pharmacy/prescriptions`)

| Method | Endpoint | Roles | Description |
|--------|----------|--------|-------------|
| GET | `/{prescriptionId}` | DOCTOR, NURSE, PHARMACIST, ADMIN | Chi tiết đơn thuốc (kèm items) |
| GET | `/patient/{patientId}` | DOCTOR, NURSE, PHARMACIST, ADMIN | Đơn thuốc theo bệnh nhân |
| GET | `/doctor/{doctorId}` | DOCTOR, NURSE, PHARMACIST, ADMIN | Đơn thuốc theo bác sĩ |
| GET | `/hospital/{hospitalId}` | PHARMACIST, ADMIN | Đơn thuốc theo bệnh viện |
| GET | `/status/{status}` | PHARMACIST, ADMIN | Đơn thuốc theo trạng thái |
| GET | `/` | PHARMACIST, ADMIN | Tất cả đơn thuốc |

---

## Running

```bash
# Start MySQL and run scripts
docker-compose up -d mysql
./scripts/setup-databases.sh

# Run Pharmacy Service
cd pharmacy-service
./mvnw spring-boot:run
```

## Test via Gateway

```bash
TOKEN="<your-jwt>"
curl -H "Authorization: Bearer $TOKEN" http://localhost:8083/api/pharmacy/medicines
curl -H "Authorization: Bearer $TOKEN" http://localhost:8083/api/pharmacy/medicines/1
curl -H "Authorization: Bearer $TOKEN" http://localhost:8083/api/pharmacy/prescriptions
```

---

## Database Tables

- `medicine` – Danh mục thuốc
- `prescriptions` – Đơn thuốc
- `prescription_items` – Dòng đơn (FK prescription_id, medicine_id)
- `medicine_inventory_transactions` – Giao dịch tồn kho

Seed data: 5 medicines (Paracetamol, Amoxicillin, Ibuprofen, Metformin, Omeprazole).
