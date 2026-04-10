-- ============================================================================
-- Drop Indexes Script for Notification Service
-- ============================================================================
-- Description: Safely drops indexes if they exist (compatible with all MySQL versions)
-- Author: VGU Banking System
-- Date: 2025-12-17
-- Usage: Run this BEFORE 03-create-indexes.sql if you need to recreate indexes
-- ============================================================================

USE banking_notifications;

-- Drop indexes (will fail silently if they don't exist)
-- This script is safe to run multiple times

-- Note: In MySQL, DROP INDEX will fail if index doesn't exist
-- This is expected behavior - just ignore errors if indexes don't exist

DROP INDEX idx_user_id_created_at ON notifications;
DROP INDEX idx_user_id_read_at ON notifications;
DROP INDEX idx_user_id_status ON notifications;
DROP INDEX idx_user_id_type ON notifications;
DROP INDEX idx_status_created_at ON notifications;
DROP INDEX idx_user_type_created_at ON notifications;

-- Display success message
SELECT 'Indexes dropped successfully (or didn\'t exist)' AS status;

