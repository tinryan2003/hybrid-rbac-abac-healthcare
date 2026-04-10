-- Initialize all databases for Hospital Management System
-- This script runs automatically when MySQL container starts

-- Create databases
CREATE DATABASE IF NOT EXISTS keycloak_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS hospital_users CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS hospital_patients CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS hospital_appointments CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS hospital_orders CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS hospital_lab CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS hospital_pharmacy CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS hospital_billing CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS hospital_audit CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS hospital_notifications CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS hospital_reporting CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS hospital_policies CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS hospital_authorization CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Grant privileges (MySQL does not support wildcard in GRANT)
GRANT ALL PRIVILEGES ON keycloak_db.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON hospital_users.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON hospital_patients.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON hospital_appointments.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON hospital_orders.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON hospital_lab.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON hospital_pharmacy.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON hospital_billing.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON hospital_audit.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON hospital_notifications.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON hospital_reporting.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON hospital_policies.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON hospital_authorization.* TO 'root'@'%';
FLUSH PRIVILEGES;
