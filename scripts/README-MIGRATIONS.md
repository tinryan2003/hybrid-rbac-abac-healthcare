# Migrations cho MySQL (Docker)

## Cập nhật DB khi đã chạy MySQL trong Docker

Script trong `hospital-db/migrations/` dùng để cập nhật schema trên DB đang chạy (không cần xóa volume).

### Thêm cột `priority` vào bảng `policies`

**Cách 1 – Migration an toàn (chỉ thêm nếu chưa có):**
```bash
docker exec -i hospital-mysql mysql -uroot -pS@l19092003 hospital_policies < scripts/hospital-db/migrations/add-priority-to-policies.sql
```

**Cách 2 – Đơn giản (chạy một lần; nếu cột đã có sẽ báo lỗi, bỏ qua):**
```bash
docker exec -i hospital-mysql mysql -uroot -pS@l19092003 hospital_policies < scripts/hospital-db/migrations/add-priority-simple.sql
```

### Chạy từ thư mục gốc project

Đảm bảo đang ở thư mục `e:\hybrid-rbac-abac` (hoặc đường dẫn tương đương). Nếu dùng đường dẫn tuyệt đối Windows, dùng format phù hợp với shell (ví dụ Git Bash).
