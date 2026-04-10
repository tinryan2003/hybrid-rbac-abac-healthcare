-- Migration: Add missing is_active column to admins table
-- Error: Schema-validation: missing column [is_active] in table [admins]
-- Run this against hospital_users database

USE hospital_users;

-- Check if column exists, then add if it doesn't
SET @col_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = 'hospital_users' 
    AND TABLE_NAME = 'admins' 
    AND COLUMN_NAME = 'is_active'
);

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE admins ADD COLUMN is_active BOOLEAN DEFAULT TRUE',
    'SELECT "Column is_active already exists" AS message'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT 'Migration 001: is_active column check completed' AS status;
