-- ============================================================================
-- Drop All Script for Notification Service
-- ============================================================================
-- Description: Drops all tables in the banking_notifications database
-- WARNING: This will delete all data! Use with extreme caution!
-- Author: VGU Banking System
-- Date: 2025-12-17
-- ============================================================================

USE banking_notifications;

-- Disable foreign key checks temporarily
SET FOREIGN_KEY_CHECKS = 0;

-- Drop tables
DROP TABLE IF EXISTS notifications;

-- Re-enable foreign key checks
SET FOREIGN_KEY_CHECKS = 1;

-- Display success message
SELECT 'All tables dropped successfully' AS status;

