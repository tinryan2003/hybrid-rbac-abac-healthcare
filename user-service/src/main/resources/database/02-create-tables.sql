-- =====================================================
-- User Service - Table Creation Script
-- =====================================================
-- This script creates employee_profiles table
-- For J.P. Morgan Hierarchy-based Approval Workflow
-- =====================================================

USE banking_users;

-- Drop existing table
DROP TABLE IF EXISTS employee_profiles;

-- =====================================================
-- Employee Profiles Table
-- =====================================================
CREATE TABLE employee_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    
    -- Keycloak Integration
    keycloak_user_id VARCHAR(50) UNIQUE NOT NULL COMMENT 'Keycloak user UUID',
    
    -- Employee Information
    employee_number VARCHAR(20) UNIQUE NOT NULL COMMENT 'Employee ID (e.g., EMP001)',
    full_name VARCHAR(100) NOT NULL COMMENT 'Full name',
    email VARCHAR(100) NOT NULL UNIQUE COMMENT 'Email address',
    phone_number VARCHAR(20) NULL COMMENT 'Phone number',
    
    -- Role & Department
    role VARCHAR(50) NOT NULL COMMENT 'ROLE_EMPLOYEE, ROLE_MANAGER, ROLE_DIRECTOR, ROLE_CEO',
    department VARCHAR(50) NULL COMMENT 'Finance, Operations, IT, etc.',
    job_title VARCHAR(100) NULL COMMENT 'Job title (enum: JobTitle)',
    branch_id VARCHAR(10) NULL COMMENT 'Branch identifier',
    
    -- Approval Settings (Critical for J.P. Morgan workflow)
    approval_limit DECIMAL(19,2) NOT NULL DEFAULT 0 COMMENT 'Max amount user can self-approve (VND)',
    can_approve_transactions BOOLEAN DEFAULT FALSE COMMENT 'Can approve others transactions',
    
    -- Hierarchy
    reports_to_user_id BIGINT NULL COMMENT 'Manager/Supervisor ID',
    
    -- Employment Status
    employment_status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT 'ACTIVE, INACTIVE, SUSPENDED, TERMINATED',
    hire_date DATE NULL COMMENT 'Date of hire',
    termination_date DATE NULL COMMENT 'Date of termination',
    
    -- Audit Fields
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    
    -- Constraints
    CONSTRAINT chk_approval_limit_positive CHECK (approval_limit >= 0),
    CONSTRAINT chk_employment_status CHECK (
        employment_status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'TERMINATED')
    ),
    CONSTRAINT fk_reports_to FOREIGN KEY (reports_to_user_id) 
        REFERENCES employee_profiles(id) ON DELETE SET NULL,
    
    -- Indexes for Performance
    INDEX idx_keycloak_user_id (keycloak_user_id),
    INDEX idx_employee_number (employee_number),
    INDEX idx_email (email),
    INDEX idx_role (role),
    INDEX idx_department (department),
    INDEX idx_branch_id (branch_id),
    INDEX idx_employment_status (employment_status),
    INDEX idx_reports_to (reports_to_user_id),
    INDEX idx_role_branch (role, branch_id),
    INDEX idx_can_approve (can_approve_transactions)
    
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Employee profiles with approval limits for hierarchy-based workflow';

SELECT 'Table employee_profiles created successfully' AS status;

