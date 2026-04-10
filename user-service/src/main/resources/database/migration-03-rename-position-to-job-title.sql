-- ============================================================================
-- Migration Script: Rename position column to job_title
-- ============================================================================
-- Description: Renames position column to job_title for clarity
-- Author: VGU Banking System
-- Date: 2026-01-04
-- ============================================================================

USE banking_users;

-- Check if column exists before renaming (MySQL-compatible approach)
SET @col_exists = (
    SELECT COUNT(*) 
    FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = 'banking_users'
    AND TABLE_NAME = 'employee_profiles'
    AND COLUMN_NAME = 'position'
);

SET @sql = IF(@col_exists > 0,
    'ALTER TABLE employee_profiles CHANGE COLUMN position job_title VARCHAR(100) NULL COMMENT ''Job title (enum: JobTitle)''',
    'SELECT ''Column position does not exist, skipping rename'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Display success message
SELECT 'Column position renamed to job_title successfully' AS status;
