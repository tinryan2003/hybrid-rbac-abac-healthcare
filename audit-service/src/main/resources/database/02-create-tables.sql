-- Audit Service Database Schema
-- MySQL 8.0
-- This script creates the audit_logs table for storing all audit trail records

-- Ensure we're using the correct database
USE banking_audit;

-- Drop table if exists (for clean setup - remove in production)
-- DROP TABLE IF EXISTS audit_logs;

-- Audit Logs table
-- Stores comprehensive audit trail of all system operations
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    
    -- Event Information
    event_type VARCHAR(50) NOT NULL COMMENT 'Type of audit event (ACCOUNT_APPROVED, TRANSACTION_CREATED, etc.)',
    severity VARCHAR(20) NOT NULL DEFAULT 'MEDIUM' COMMENT 'Severity level: LOW, MEDIUM, HIGH, CRITICAL',
    action VARCHAR(100) NOT NULL COMMENT 'Action performed (APPROVE_ACCOUNT, CREATE_TRANSACTION, etc.)',
    description TEXT COMMENT 'Detailed description of the event',
    
    -- User Information
    user_id BIGINT COMMENT 'ID of the user who performed the action',
    username VARCHAR(100) COMMENT 'Username of the user',
    user_role VARCHAR(50) COMMENT 'Role of the user (ROLE_ADMIN, ROLE_MANAGER, etc.)',
    
    -- Resource Information
    resource_type VARCHAR(50) COMMENT 'Type of resource (ACCOUNT, TRANSACTION, CUSTOMER, etc.)',
    resource_id BIGINT COMMENT 'ID of the resource affected',
    
    -- Request Information
    ip_address VARCHAR(45) COMMENT 'IP address of the requester (supports IPv6)',
    user_agent VARCHAR(255) COMMENT 'User agent string from the request',
    session_id VARCHAR(100) COMMENT 'Session identifier',
    
    -- Result Information
    success BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Whether the action was successful',
    failure_reason TEXT COMMENT 'Reason for failure if success = false',
    
    -- State Tracking
    before_state JSON COMMENT 'State of the resource before the action (JSON format)',
    after_state JSON COMMENT 'State of the resource after the action (JSON format)',
    metadata JSON COMMENT 'Additional metadata about the event (JSON format)',
    
    -- Correlation and Tracing
    correlation_id VARCHAR(100) COMMENT 'Correlation ID for tracing related events',
    
    -- Timestamp
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'When the event occurred',
    
    -- Indexes for performance optimization
    INDEX idx_event_type (event_type),
    INDEX idx_user_id (user_id),
    INDEX idx_username (username),
    INDEX idx_resource_type (resource_type),
    INDEX idx_resource_id (resource_id),
    INDEX idx_timestamp (timestamp),
    INDEX idx_severity (severity),
    INDEX idx_success (success),
    INDEX idx_correlation_id (correlation_id),
    INDEX idx_user_id_timestamp (user_id, timestamp),
    INDEX idx_resource_type_id (resource_type, resource_id),
    INDEX idx_event_type_timestamp (event_type, timestamp)
    
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Comprehensive audit trail of all system operations for compliance and security';

-- Create a view for recent high-severity events (optional, for quick access)
CREATE OR REPLACE VIEW vw_high_severity_audit_logs AS
SELECT 
    id,
    event_type,
    severity,
    username,
    action,
    description,
    resource_type,
    resource_id,
    success,
    timestamp
FROM audit_logs
WHERE severity IN ('HIGH', 'CRITICAL')
ORDER BY timestamp DESC;

-- Create a view for failed actions (optional, for quick access)
CREATE OR REPLACE VIEW vw_failed_audit_logs AS
SELECT 
    id,
    event_type,
    severity,
    username,
    action,
    description,
    failure_reason,
    resource_type,
    resource_id,
    timestamp
FROM audit_logs
WHERE success = FALSE
ORDER BY timestamp DESC;

-- Create a view for user activity summary (optional, for reporting)
CREATE OR REPLACE VIEW vw_user_audit_summary AS
SELECT 
    user_id,
    username,
    COUNT(*) as total_events,
    SUM(CASE WHEN success = TRUE THEN 1 ELSE 0 END) as successful_events,
    SUM(CASE WHEN success = FALSE THEN 1 ELSE 0 END) as failed_events,
    SUM(CASE WHEN severity = 'CRITICAL' THEN 1 ELSE 0 END) as critical_events,
    MIN(timestamp) as first_event,
    MAX(timestamp) as last_event
FROM audit_logs
WHERE user_id IS NOT NULL
GROUP BY user_id, username;

