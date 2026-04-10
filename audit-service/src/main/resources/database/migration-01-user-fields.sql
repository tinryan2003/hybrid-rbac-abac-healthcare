-- Migration: Update audit_logs table to use employee_number, email, and keycloak_id
-- Instead of user_id and username

USE banking_audit;

-- Add new columns
ALTER TABLE audit_logs 
    ADD COLUMN employee_number VARCHAR(50) COMMENT 'Employee number of the user who performed the action',
    ADD COLUMN email VARCHAR(255) COMMENT 'Email address of the user',
    ADD COLUMN keycloak_id VARCHAR(100) COMMENT 'Keycloak user ID (UUID)';

-- Create indexes for new columns
CREATE INDEX idx_employee_number ON audit_logs(employee_number);
CREATE INDEX idx_email ON audit_logs(email);
CREATE INDEX idx_keycloak_id ON audit_logs(keycloak_id);
CREATE INDEX idx_employee_number_timestamp ON audit_logs(employee_number, timestamp);

-- Note: We keep user_id and username columns for backward compatibility during migration
-- They can be dropped later after data migration is complete
