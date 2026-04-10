# Migration: name → first_name, last_name

Lỗi **missing column [first_name] in table [admins]** xảy ra vì bảng trong MySQL vẫn dùng cột `name`, trong khi code đã đổi sang `first_name` và `last_name`.

## Cách chạy migration

### 1. Dùng MySQL client (command line)

Nếu đã cài MySQL và có `mysql` trong PATH:

```bash
# Từ thư mục gốc project (hybrid-rbac-abac)
mysql -u root -p hospital_users < scripts/hospital-db/migrations/01-name-to-first-last-name.sql
```

(Nhập password khi được hỏi. Nếu user không phải `root`, thay `-u root` bằng `-u your_username`.)

### 2. Dùng MySQL Workbench / DBeaver / HeidiSQL

1. Kết nối tới MySQL, chọn database **hospital_users**.
2. Mở file `scripts/hospital-db/migrations/01-name-to-first-last-name.sql`.
3. Chạy toàn bộ script (Execute).

### 3. Copy-paste SQL trực tiếp

Chạy lần lượt trong tab Query (database **hospital_users**). Nếu bảng **admins** đã migrate trước đó, bỏ qua khối ADMINS và chỉ chạy DOCTORS và NURSES.

**Chỉ còn lỗi bảng doctors:** chạy khối DOCTORS + NURSES bên dưới.

```sql
USE hospital_users;

-- ADMINS (bỏ qua nếu đã chạy rồi)
ALTER TABLE admins ADD COLUMN first_name VARCHAR(50) NULL AFTER user_id, ADD COLUMN last_name VARCHAR(50) NULL AFTER first_name;
UPDATE admins SET first_name = TRIM(SUBSTRING_INDEX(name, ' ', 1)), last_name = TRIM(IF(LOCATE(' ', name) > 0, SUBSTRING(name, LOCATE(' ', name) + 1), ''));
UPDATE admins SET last_name = '' WHERE last_name IS NULL;
ALTER TABLE admins MODIFY first_name VARCHAR(50) NOT NULL, MODIFY last_name VARCHAR(50) NOT NULL, DROP COLUMN name;

-- DOCTORS
ALTER TABLE doctors ADD COLUMN first_name VARCHAR(50) NULL AFTER user_id, ADD COLUMN last_name VARCHAR(50) NULL AFTER first_name;
UPDATE doctors SET first_name = TRIM(SUBSTRING_INDEX(name, ' ', 1)), last_name = TRIM(IF(LOCATE(' ', name) > 0, SUBSTRING(name, LOCATE(' ', name) + 1), ''));
UPDATE doctors SET last_name = '' WHERE last_name IS NULL;
ALTER TABLE doctors MODIFY first_name VARCHAR(50) NOT NULL, MODIFY last_name VARCHAR(50) NOT NULL, DROP COLUMN name;

-- NURSES
ALTER TABLE nurses ADD COLUMN first_name VARCHAR(50) NULL AFTER user_id, ADD COLUMN last_name VARCHAR(50) NULL AFTER first_name;
UPDATE nurses SET first_name = TRIM(SUBSTRING_INDEX(name, ' ', 1)), last_name = TRIM(IF(LOCATE(' ', name) > 0, SUBSTRING(name, LOCATE(' ', name) + 1), ''));
UPDATE nurses SET last_name = '' WHERE last_name IS NULL;
ALTER TABLE nurses MODIFY first_name VARCHAR(50) NOT NULL, MODIFY last_name VARCHAR(50) NOT NULL, DROP COLUMN name;
```

Sau khi chạy xong, khởi động lại **user-service** (`./mvnw spring-boot:run`).
