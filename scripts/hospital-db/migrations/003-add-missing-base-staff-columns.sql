-- Migration: Add missing BaseStaffProfile columns to all staff tables
-- Error: Schema-validation: missing columns in billing_clerks, receptionists, pharmacists, lab_technicians
-- BaseStaffProfile requires: first_name, last_name, email, phone_number, hospital_id, is_active, created_at, updated_at
-- Run this against hospital_users database

USE hospital_users;

-- =====================================================
-- Helper: Add column if it doesn't exist
-- =====================================================
DELIMITER $$

DROP PROCEDURE IF EXISTS AddColumnIfNotExists$$
CREATE PROCEDURE AddColumnIfNotExists(
    IN tableName VARCHAR(100),
    IN columnName VARCHAR(100),
    IN columnDefinition TEXT
)
BEGIN
    SET @col_exists = (
        SELECT COUNT(*) 
        FROM INFORMATION_SCHEMA.COLUMNS 
        WHERE TABLE_SCHEMA = 'hospital_users' 
        AND TABLE_NAME = tableName 
        AND COLUMN_NAME = columnName
    );

    IF @col_exists = 0 THEN
        SET @sql = CONCAT('ALTER TABLE ', tableName, ' ADD COLUMN ', columnName, ' ', columnDefinition);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
        SELECT CONCAT('Added column ', columnName, ' to ', tableName) AS result;
    ELSE
        SELECT CONCAT('Column ', columnName, ' already exists in ', tableName) AS result;
    END IF;
END$$

DELIMITER ;

-- =====================================================
-- 1. Billing Clerks
-- =====================================================
CALL AddColumnIfNotExists('billing_clerks', 'first_name', 'VARCHAR(50) NOT NULL DEFAULT ""');
CALL AddColumnIfNotExists('billing_clerks', 'last_name', 'VARCHAR(50) NOT NULL DEFAULT ""');
CALL AddColumnIfNotExists('billing_clerks', 'email', 'VARCHAR(100)');
CALL AddColumnIfNotExists('billing_clerks', 'phone_number', 'VARCHAR(20)');
CALL AddColumnIfNotExists('billing_clerks', 'hospital_id', 'VARCHAR(50)');
CALL AddColumnIfNotExists('billing_clerks', 'is_active', 'BOOLEAN DEFAULT TRUE');
CALL AddColumnIfNotExists('billing_clerks', 'created_at', 'TIMESTAMP DEFAULT CURRENT_TIMESTAMP');
CALL AddColumnIfNotExists('billing_clerks', 'updated_at', 'TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP');

-- =====================================================
-- 2. Receptionists
-- =====================================================
CALL AddColumnIfNotExists('receptionists', 'first_name', 'VARCHAR(50) NOT NULL DEFAULT ""');
CALL AddColumnIfNotExists('receptionists', 'last_name', 'VARCHAR(50) NOT NULL DEFAULT ""');
CALL AddColumnIfNotExists('receptionists', 'email', 'VARCHAR(100)');
CALL AddColumnIfNotExists('receptionists', 'phone_number', 'VARCHAR(20)');
CALL AddColumnIfNotExists('receptionists', 'hospital_id', 'VARCHAR(50)');
CALL AddColumnIfNotExists('receptionists', 'is_active', 'BOOLEAN DEFAULT TRUE');
CALL AddColumnIfNotExists('receptionists', 'created_at', 'TIMESTAMP DEFAULT CURRENT_TIMESTAMP');
CALL AddColumnIfNotExists('receptionists', 'updated_at', 'TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP');

-- =====================================================
-- 3. Pharmacists
-- =====================================================
CALL AddColumnIfNotExists('pharmacists', 'first_name', 'VARCHAR(50) NOT NULL DEFAULT ""');
CALL AddColumnIfNotExists('pharmacists', 'last_name', 'VARCHAR(50) NOT NULL DEFAULT ""');
CALL AddColumnIfNotExists('pharmacists', 'email', 'VARCHAR(100)');
CALL AddColumnIfNotExists('pharmacists', 'phone_number', 'VARCHAR(20)');
CALL AddColumnIfNotExists('pharmacists', 'hospital_id', 'VARCHAR(50)');
CALL AddColumnIfNotExists('pharmacists', 'is_active', 'BOOLEAN DEFAULT TRUE');
CALL AddColumnIfNotExists('pharmacists', 'created_at', 'TIMESTAMP DEFAULT CURRENT_TIMESTAMP');
CALL AddColumnIfNotExists('pharmacists', 'updated_at', 'TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP');

-- =====================================================
-- 4. Lab Technicians
-- =====================================================
CALL AddColumnIfNotExists('lab_technicians', 'first_name', 'VARCHAR(50) NOT NULL DEFAULT ""');
CALL AddColumnIfNotExists('lab_technicians', 'last_name', 'VARCHAR(50) NOT NULL DEFAULT ""');
CALL AddColumnIfNotExists('lab_technicians', 'email', 'VARCHAR(100)');
CALL AddColumnIfNotExists('lab_technicians', 'phone_number', 'VARCHAR(20)');
CALL AddColumnIfNotExists('lab_technicians', 'hospital_id', 'VARCHAR(50)');
CALL AddColumnIfNotExists('lab_technicians', 'is_active', 'BOOLEAN DEFAULT TRUE');
CALL AddColumnIfNotExists('lab_technicians', 'created_at', 'TIMESTAMP DEFAULT CURRENT_TIMESTAMP');
CALL AddColumnIfNotExists('lab_technicians', 'updated_at', 'TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP');

-- =====================================================
-- 5. Admins (already has most, but check is_active)
-- =====================================================
CALL AddColumnIfNotExists('admins', 'is_active', 'BOOLEAN DEFAULT TRUE');
CALL AddColumnIfNotExists('admins', 'created_at', 'TIMESTAMP DEFAULT CURRENT_TIMESTAMP');
CALL AddColumnIfNotExists('admins', 'updated_at', 'TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP');

-- =====================================================
-- 6. Doctors (check created_at, updated_at)
-- =====================================================
CALL AddColumnIfNotExists('doctors', 'created_at', 'TIMESTAMP DEFAULT CURRENT_TIMESTAMP');
CALL AddColumnIfNotExists('doctors', 'updated_at', 'TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP');

-- =====================================================
-- 7. Nurses (check created_at, updated_at)
-- =====================================================
CALL AddColumnIfNotExists('nurses', 'created_at', 'TIMESTAMP DEFAULT CURRENT_TIMESTAMP');
CALL AddColumnIfNotExists('nurses', 'updated_at', 'TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP');

-- Cleanup
DROP PROCEDURE IF EXISTS AddColumnIfNotExists;

SELECT 'Migration 003: BaseStaffProfile columns added to all staff tables' AS status;
