# Policy không hoạt động (Access denied dù đã tạo policy)

## Nguyên nhân thường gặp

1. **Policy chưa lên OPA** – Policy trong DB chưa được sync sang OPA (`data.dynamic_policies`).
2. **OPA chưa load Rego mới** – File `hospital.rego` đổi nhưng OPA chưa restart / chưa load lại bundle.
3. **Role không khớp** – Keycloak trả role khác (ví dụ "Admin" thay vì "ADMIN"); gateway đã chuẩn hóa "Admin" → "ADMIN".

## Cách xử lý

### 1. Đồng bộ policy từ DB lên OPA

Policy trong DB chỉ có hiệu lực sau khi được đẩy lên OPA:

```bash
# Gọi API resync (cần token admin)
curl -X POST http://localhost:8089/api/policies/opa-sync/resync \
  -H "Authorization: Bearer <access_token>"
```

Hoặc restart **policy-service**: lúc khởi động nó sẽ sync toàn bộ policy enabled lên OPA (nếu OPA đang chạy).

### 2. Restart OPA nếu đã sửa Rego

Nếu bạn đổi file `policies/hospital.rego`:

- Chạy OPA bằng file: restart OPA để load file mới.
- Chạy OPA bằng bundle: build và deploy bundle mới.

### 3. Kiểm tra role trong Keycloak

User cần có realm role **ADMIN** (hoặc `Admin`/`admin` — gateway chuẩn hóa thành ADMIN trước khi gửi sang OPA).

### 4. Policy "admins-can-see-all" với resource staff_record

- Gateway gửi `/api/users` (doctors, employees, wards) với **resource = "user"**.
- Rego đã alias: policy **staff_record** vẫn match request **user**.
- Static fallback: ADMIN luôn được allow cho resource **user** (read/create/update/delete).

Nếu vẫn Access denied: kiểm tra log gateway (role được gửi) và authorization-service (OPA input + result).
