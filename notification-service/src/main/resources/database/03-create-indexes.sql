-- ============================================================================
-- Index Creation Script for Notification Service
-- ============================================================================
-- Description: Creates additional indexes for performance optimization
-- Author: VGU Banking System
-- Date: 2025-12-17
-- ============================================================================
-- NOTE: If indexes already exist, you'll get an error. 
-- To recreate indexes, first drop them manually:
--   DROP INDEX idx_user_id_created_at ON notifications;
--   DROP INDEX idx_user_id_read_at ON notifications;
--   (etc...)
-- Or run: 05-drop-all.sql to drop all tables and recreate
-- ============================================================================

USE banking_notifications;

-- Composite indexes for common queries
-- Note: MySQL doesn't support IF NOT EXISTS for CREATE INDEX
-- If you get "Duplicate key name" error, indexes already exist
-- Drop them first or ignore the error if you want to keep existing indexes

-- Index for getting user notifications
CREATE INDEX idx_user_id_created_at 
ON notifications(user_id, created_at DESC);

-- Index for unread notifications
CREATE INDEX idx_user_id_read_at 
ON notifications(user_id, read_at);

-- Index for notification by status and user
CREATE INDEX idx_user_id_status 
ON notifications(user_id, status);

-- Index for notification type filtering
CREATE INDEX idx_user_id_type 
ON notifications(user_id, type);

-- Index for finding failed notifications
CREATE INDEX idx_status_created_at 
ON notifications(status, created_at DESC);

-- Index for user type filtering
CREATE INDEX idx_user_type_created_at 
ON notifications(user_type, created_at DESC);

-- Display success message
SELECT 'Indexes created successfully' AS status;

