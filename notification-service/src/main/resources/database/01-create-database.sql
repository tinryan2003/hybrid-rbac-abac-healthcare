-- ============================================================================
-- Database Creation Script for Notification Service
-- ============================================================================
-- Description: Creates the banking_notifications database
-- Author: VGU Banking System
-- Date: 2025-12-17
-- ============================================================================

-- Drop database if exists (use with caution in production)
-- DROP DATABASE IF EXISTS banking_notifications;

-- Create database
CREATE DATABASE IF NOT EXISTS banking_notifications
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- Use the database
USE banking_notifications;

-- Display success message
SELECT 'Database banking_notifications created successfully' AS status;

