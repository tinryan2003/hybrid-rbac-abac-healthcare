-- =====================================================
-- Hospital Audit Database
-- For Audit Service (Port 8091)
-- Hybrid RBAC/ABAC Role-Centric
-- =====================================================
DROP DATABASE IF EXISTS hospital_audit;
CREATE DATABASE IF NOT EXISTS hospital_audit CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE hospital_audit;

-- =====================================================
-- 1. Audit Logs Table
-- Schema aligned with Java AuditLog entity
-- =====================================================
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    -- Event Information
    event_type VARCHAR(50) NOT NULL COMMENT 'AuditEventType enum: AUTHORIZATION_SUCCESS, LOGIN_FAILED, etc.',
    severity VARCHAR(20) NOT NULL DEFAULT 'MEDIUM' COMMENT 'AuditSeverity enum stored as String: LOW|MEDIUM|HIGH|CRITICAL',
    action VARCHAR(100) NOT NULL COMMENT 'CREATE, READ, UPDATE, DELETE, APPROVE, etc.',
    description TEXT,

    -- User Information (from Keycloak + our DB)
    employee_number VARCHAR(50) COMMENT 'Staff employee number',
    email VARCHAR(255) COMMENT 'User email',
    keycloak_id VARCHAR(100) COMMENT 'Keycloak UUID',
    user_role VARCHAR(50) COMMENT 'RBAC: ROLE_DOCTOR, ROLE_NURSE, etc.',
    job_title VARCHAR(100) COMMENT 'ABAC: Job title/position',

    -- Legacy fields (backward compat)
    user_id BIGINT COMMENT 'Internal user ID',
    username VARCHAR(100) COMMENT 'Username',

    -- Resource Information
    resource_type VARCHAR(50) COMMENT 'ResourceType enum: PATIENT_RECORD, APPOINTMENT, etc.',
    resource_id BIGINT COMMENT 'ID of the resource',

    -- Request Information
    ip_address VARCHAR(45) COMMENT 'IPv4/IPv6',
    user_agent VARCHAR(255) COMMENT 'Browser/client',
    session_id VARCHAR(100),

    -- Result Information
    success BOOLEAN NOT NULL DEFAULT TRUE,
    failure_reason TEXT,

    -- State Tracking (JSON)
    before_state JSON COMMENT 'State before the action',
    after_state JSON COMMENT 'State after the action',
    metadata JSON COMMENT 'Additional metadata',

    -- Correlation and Tracing
    correlation_id VARCHAR(100) COMMENT 'Trace related events',

    -- Tamper-evident hash chain
    prev_hash VARCHAR(128) COMMENT 'SHA-256 hash of previous log entry',
    curr_hash VARCHAR(128) COMMENT 'SHA-256 hash of current entry (data + prevHash)',

    -- Timestamp
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_event_type (event_type),
    INDEX idx_employee_number (employee_number),
    INDEX idx_email (email),
    INDEX idx_keycloak_id (keycloak_id),
    INDEX idx_resource_type (resource_type),
    INDEX idx_resource_id (resource_id),
    INDEX idx_timestamp (timestamp),
    INDEX idx_severity (severity)
) ENGINE=InnoDB COMMENT='Audit trail - schema matches AuditLog.java entity';

-- =====================================================
-- 2. Security Events Table (Failed logins, suspicious activities)
-- =====================================================
CREATE TABLE IF NOT EXISTS security_events (
    security_event_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    
    event_type ENUM('FAILED_LOGIN', 'SUSPICIOUS_ACTIVITY', 'UNAUTHORIZED_ACCESS', 'DATA_BREACH_ATTEMPT', 'PRIVILEGE_ESCALATION') NOT NULL,
    severity ENUM('LOW', 'MEDIUM', 'HIGH', 'CRITICAL') DEFAULT 'HIGH',
    
    -- User attempting
    attempted_username VARCHAR(100),
    keycloak_user_id VARCHAR(255),
    
    -- Context
    ip_address VARCHAR(45) NOT NULL,
    user_agent VARCHAR(500),
    
    -- Details
    description TEXT,
    resource_attempted VARCHAR(200),
    
    -- Response
    action_taken VARCHAR(200) COMMENT 'Account locked, IP blocked, etc.',
    resolved BOOLEAN DEFAULT FALSE,
    resolved_at TIMESTAMP NULL,
    resolved_by_keycloak_id VARCHAR(255),
    
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_type (event_type),
    INDEX idx_severity (severity),
    INDEX idx_ip (ip_address),
    INDEX idx_timestamp (timestamp),
    INDEX idx_resolved (resolved)
) ENGINE=InnoDB COMMENT='Security-related audit events';

-- =====================================================
-- 3. Data Access Logs (PHI/Sensitive data access)
-- =====================================================
CREATE TABLE IF NOT EXISTS data_access_logs (
    access_log_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    
    -- User accessing
    keycloak_user_id VARCHAR(255) NOT NULL,
    user_email VARCHAR(100),
    user_role VARCHAR(50),
    
    -- Data accessed
    data_type ENUM('PATIENT_RECORD', 'MEDICAL_HISTORY', 'LAB_RESULT', 'PRESCRIPTION', 'IMAGING', 'BILLING') NOT NULL,
    patient_id BIGINT NOT NULL COMMENT 'Patient whose data was accessed',
    record_id BIGINT COMMENT 'Specific record ID',
    
    -- Access details
    access_type ENUM('VIEW', 'EXPORT', 'PRINT', 'DOWNLOAD') NOT NULL,
    sensitivity_level ENUM('NORMAL', 'HIGH', 'CRITICAL'),
    
    -- Context
    reason TEXT COMMENT 'Reason for access',
    hospital_id VARCHAR(50),
    department_id VARCHAR(50),
    
    -- Technical
    ip_address VARCHAR(45),
    session_id VARCHAR(100),
    
    access_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_user (keycloak_user_id),
    INDEX idx_patient (patient_id),
    INDEX idx_data_type (data_type),
    INDEX idx_access_type (access_type),
    INDEX idx_sensitivity (sensitivity_level),
    INDEX idx_timestamp (access_timestamp),
    INDEX idx_patient_timestamp (patient_id, access_timestamp)
) ENGINE=InnoDB COMMENT='Detailed PHI/sensitive data access logs for HIPAA compliance';

-- =====================================================
-- 4. Compliance Reports Table
-- =====================================================
CREATE TABLE IF NOT EXISTS compliance_reports (
    report_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    
    report_type ENUM('DAILY', 'WEEKLY', 'MONTHLY', 'QUARTERLY', 'ANNUAL', 'ADHOC') NOT NULL,
    report_period_start DATE NOT NULL,
    report_period_end DATE NOT NULL,
    
    -- Metrics
    total_events BIGINT,
    failed_events BIGINT,
    security_incidents BIGINT,
    sensitive_data_accesses BIGINT,
    
    -- Report Data (JSON)
    report_data JSON COMMENT 'Detailed report data',
    
    -- Generation
    generated_by_keycloak_id VARCHAR(255),
    generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- File
    report_file_path VARCHAR(500) COMMENT 'Path to generated report file',
    
    INDEX idx_type (report_type),
    INDEX idx_period (report_period_start, report_period_end),
    INDEX idx_generated_at (generated_at)
) ENGINE=InnoDB COMMENT='Compliance and audit reports';

-- =====================================================
-- Views for Quick Access
-- =====================================================

-- High Severity Events (last 30 days)
CREATE OR REPLACE VIEW vw_high_severity_events AS
SELECT
    id, event_type, severity, action, description,
    email, user_role, resource_type, resource_id,
    success, timestamp
FROM audit_logs
WHERE severity IN ('HIGH', 'CRITICAL')
    AND timestamp >= DATE_SUB(NOW(), INTERVAL 30 DAY)
ORDER BY timestamp DESC;

-- Failed Actions (last 7 days)
CREATE OR REPLACE VIEW vw_failed_actions AS
SELECT
    id, event_type, action, email, user_role,
    resource_type, resource_id, failure_reason,
    timestamp
FROM audit_logs
WHERE success = FALSE
    AND timestamp >= DATE_SUB(NOW(), INTERVAL 7 DAY)
ORDER BY timestamp DESC;

-- User Activity Summary
CREATE OR REPLACE VIEW vw_user_activity_summary AS
SELECT
    keycloak_id,
    email,
    user_role,
    COUNT(*) AS total_events,
    SUM(CASE WHEN success = TRUE THEN 1 ELSE 0 END) AS successful_events,
    SUM(CASE WHEN success = FALSE THEN 1 ELSE 0 END) AS failed_events,
    SUM(CASE WHEN severity = 'CRITICAL' THEN 1 ELSE 0 END) AS critical_events,
    MIN(timestamp) AS first_event,
    MAX(timestamp) AS last_event
FROM audit_logs
WHERE keycloak_id IS NOT NULL
    AND timestamp >= DATE_SUB(NOW(), INTERVAL 30 DAY)
GROUP BY keycloak_id, email, user_role;

SELECT 'Hospital Audit Database created successfully!' AS status;
