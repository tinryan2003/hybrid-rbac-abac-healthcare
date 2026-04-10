-- Migration: Add job_title column to audit_logs table for ABAC support
-- This column stores JobTitleType enum value (e.g., "HR_MANAGER", "HR_SPECIALIST")
-- to support hybrid RBAC/ABAC authorization tracking

USE banking_audit;

-- Add job_title column
ALTER TABLE audit_logs 
    ADD COLUMN job_title VARCHAR(100) NULL COMMENT 'Job title of the user (HR_MANAGER, HR_SPECIALIST, etc.) - ABAC';

-- Create index for job_title
CREATE INDEX idx_job_title ON audit_logs(job_title);

-- Create composite index for job_title and timestamp (for reporting)
CREATE INDEX idx_job_title_timestamp ON audit_logs(job_title, timestamp);

SELECT 'Job title column added successfully' AS status;
