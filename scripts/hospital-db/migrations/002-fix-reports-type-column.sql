-- Migration: Fix reports.type column to support new hospital ReportType enum values
-- Error: Data truncated for column 'type' at row 1
-- Issue: Column 'type' may be ENUM with old banking values or VARCHAR too small
-- Solution: ALTER to VARCHAR(50) to accommodate longest value: AUTHORIZATION_DECISIONS (23 chars)
-- Run this against hospital_reporting database

USE hospital_reporting;

-- Create table if it doesn't exist
CREATE TABLE IF NOT EXISTS reports (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL COMMENT 'ReportType enum: PATIENT_SUMMARY, APPOINTMENT_REPORT, etc.',
    format VARCHAR(20) NOT NULL COMMENT 'ReportFormat enum: CSV, JSON, PDF, EXCEL',
    status VARCHAR(20) NOT NULL COMMENT 'ReportStatus enum: PENDING, PROCESSING, COMPLETED, FAILED, CANCELLED',
    file_path VARCHAR(500),
    file_size BIGINT,
    created_by VARCHAR(255),
    created_at DATETIME,
    completed_at DATETIME,
    error_message TEXT,
    start_date DATETIME,
    end_date DATETIME,
    filters TEXT,
    email_recipients TEXT,
    scheduled BOOLEAN DEFAULT FALSE,
    
    INDEX idx_type (type),
    INDEX idx_status (status),
    INDEX idx_created_by (created_by),
    INDEX idx_created_at (created_at),
    INDEX idx_scheduled (scheduled)
) ENGINE=InnoDB COMMENT='Report generation metadata';

-- If table already exists, alter the type column
-- This will work whether it's ENUM or VARCHAR
ALTER TABLE reports 
    MODIFY COLUMN type VARCHAR(50) NOT NULL COMMENT 'ReportType enum: PATIENT_SUMMARY, APPOINTMENT_REPORT, etc.',
    MODIFY COLUMN format VARCHAR(20) NOT NULL COMMENT 'ReportFormat enum: CSV, JSON, PDF, EXCEL',
    MODIFY COLUMN status VARCHAR(20) NOT NULL COMMENT 'ReportStatus enum: PENDING, PROCESSING, COMPLETED, FAILED, CANCELLED';

SELECT 'Migration 002: reports.type column updated to VARCHAR(50)' AS status;
