-- =====================================================
-- Additional Seed Data for Hospital Users
-- Bổ sung seed data cho các role còn thiếu
--
-- LƯU Ý: Keycloak UUIDs cần được cập nhật để match với
-- users thực tế trong Keycloak realm sau khi tạo users
-- =====================================================

USE hospital_users;

-- =====================================================
-- Seed Data: pharmacist1, billing_clerk1, lab_tech1
-- Keycloak UUIDs must match users in Keycloak realm
-- =====================================================

-- 1. Pharmacist User
INSERT IGNORE INTO users (keycloak_user_id, email, phone_number) VALUES
('pharmacist1-keycloak-uuid', 'pharmacist1@hospital.com', '0904444444');

SET @pharmacist_user_id = (SELECT user_id FROM users WHERE email = 'pharmacist1@hospital.com' LIMIT 1);

INSERT INTO pharmacists (user_id, first_name, last_name, email, phone_number, hospital_id, license_number, is_active)
SELECT @pharmacist_user_id, 'Pharmacist', 'One', 'pharmacist1@hospital.com', '0904444444', 'HOSPITAL_A', 'PHARM-001', TRUE
WHERE @pharmacist_user_id IS NOT NULL
ON DUPLICATE KEY UPDATE first_name = 'Pharmacist', last_name = 'One';

-- 2. Billing Clerk User
INSERT IGNORE INTO users (keycloak_user_id, email, phone_number) VALUES
('billing_clerk1-keycloak-uuid', 'billing_clerk1@hospital.com', '0905555555');

SET @billing_clerk_user_id = (SELECT user_id FROM users WHERE email = 'billing_clerk1@hospital.com' LIMIT 1);

INSERT INTO billing_clerks (user_id, first_name, last_name, email, phone_number, hospital_id, is_active)
SELECT @billing_clerk_user_id, 'Billing', 'Clerk One', 'billing_clerk1@hospital.com', '0905555555', 'HOSPITAL_A', TRUE
WHERE @billing_clerk_user_id IS NOT NULL
ON DUPLICATE KEY UPDATE first_name = 'Billing', last_name = 'Clerk One';

-- 3. Lab Technician User
INSERT IGNORE INTO users (keycloak_user_id, email, phone_number) VALUES
('lab_tech1-keycloak-uuid', 'lab_tech1@hospital.com', '0906666666');

SET @lab_tech_user_id = (SELECT user_id FROM users WHERE email = 'lab_tech1@hospital.com' LIMIT 1);

INSERT INTO lab_technicians (user_id, first_name, last_name, email, phone_number, department_id, hospital_id, specialization, is_active)
SELECT @lab_tech_user_id, 'Lab', 'Tech One', 'lab_tech1@hospital.com', '0906666666', 6, 'HOSPITAL_A', 'Clinical Chemistry', TRUE
WHERE @lab_tech_user_id IS NOT NULL
ON DUPLICATE KEY UPDATE first_name = 'Lab', last_name = 'Tech One';

SELECT 'Additional users seed data added successfully!' AS status;
SELECT 'NOTE: Update keycloak_user_id values to match actual Keycloak UUIDs' AS warning;
