-- ============================================================================
-- Complete Setup Script for Notification Service Database
-- ============================================================================
-- Description: Runs all setup scripts in the correct order
-- Author: VGU Banking System
-- Date: 2025-12-17
-- ============================================================================
-- Usage:
--   mysql -u root -p < 00-setup-complete.sql
-- Or from MySQL client:
--   source 00-setup-complete.sql
-- ============================================================================

-- Step 1: Create database
SOURCE 01-create-database.sql;

-- Step 2: Create tables
SOURCE 02-create-tables.sql;

-- Step 3: Create indexes
SOURCE 03-create-indexes.sql;

-- Step 4: Insert seed data
SOURCE 04-seed-data.sql;

-- Display final status
SELECT '========================================' AS '';
SELECT 'Database setup completed successfully!' AS status;
SELECT '========================================' AS '';
SELECT 'Database: banking_notifications' AS '';
SELECT 'Tables created: notifications' AS '';
SELECT '========================================' AS '';

