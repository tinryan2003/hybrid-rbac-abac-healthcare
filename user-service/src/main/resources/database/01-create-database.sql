-- =====================================================
-- User Service - Database Creation Script
-- =====================================================
-- This script creates the database for User/Employee Service
-- Usage: mysql -u root -p < 01-create-database.sql
-- =====================================================

-- Create database
CREATE DATABASE IF NOT EXISTS banking_users
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE banking_users;

SELECT 'Database banking_users created successfully' AS status;

