-- =====================================================
-- Hospital Notifications Database
-- For Notification Service (Port 8088)
-- Hybrid RBAC/ABAC Role-Centric
-- =====================================================

CREATE DATABASE IF NOT EXISTS hospital_notifications CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE hospital_notifications;

-- =====================================================
-- 1. Notifications Table
-- Schema aligned with Java Notification entity
-- =====================================================
CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    -- Recipient (user_id = internal user DB id)
    user_id BIGINT NOT NULL COMMENT 'Internal user ID (from user-service)',
    user_type VARCHAR(20) NOT NULL COMMENT 'UserType enum: CUSTOMER|EMPLOYEE (extend as needed)',

    -- Notification Details
    type VARCHAR(50) NOT NULL COMMENT 'Free-text type: TRANSACTION, ACCOUNT, SECURITY, APPOINTMENT, etc.',
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,

    -- Email-specific content
    email_subject VARCHAR(200),
    email_body TEXT COMMENT 'HTML email body',

    -- Status & Channel
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'NotificationStatus: PENDING|SENT|DELIVERED|FAILED',
    channel VARCHAR(20) NOT NULL DEFAULT 'IN_APP' COMMENT 'NotificationChannel: IN_APP|EMAIL|BOTH',

    -- Timestamps
    read_at DATETIME,
    sent_at DATETIME,
    email_sent_at DATETIME,

    -- Metadata
    metadata JSON COMMENT 'Additional data as JSON string',

    -- Audit
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,

    INDEX idx_user_id (user_id),
    INDEX idx_user_type (user_type),
    INDEX idx_type (type),
    INDEX idx_status (status),
    INDEX idx_channel (channel),
    INDEX idx_created_at (created_at),
    INDEX idx_user_unread (user_id, status, created_at)
) ENGINE=InnoDB COMMENT='Notifications - schema matches Notification.java entity';

-- =====================================================
-- 2. Notification Preferences Table (future use - no Repository yet)
-- =====================================================
CREATE TABLE IF NOT EXISTS notification_preferences (
    preference_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    keycloak_user_id VARCHAR(255) UNIQUE NOT NULL,
    
    -- Channel Preferences
    email_enabled BOOLEAN DEFAULT TRUE,
    sms_enabled BOOLEAN DEFAULT TRUE,
    push_enabled BOOLEAN DEFAULT TRUE,
    in_app_enabled BOOLEAN DEFAULT TRUE,
    
    -- Type Preferences (JSON for flexibility)
    type_preferences JSON COMMENT '{"APPOINTMENT": {"email": true, "sms": false}, ...}',
    
    -- Quiet Hours
    quiet_hours_enabled BOOLEAN DEFAULT FALSE,
    quiet_hours_start TIME COMMENT 'e.g., 22:00',
    quiet_hours_end TIME COMMENT 'e.g., 08:00',
    
    -- Frequency
    digest_enabled BOOLEAN DEFAULT FALSE COMMENT 'Bundle notifications into digest',
    digest_frequency ENUM('HOURLY', 'DAILY', 'WEEKLY') DEFAULT 'DAILY',
    
    -- Audit
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_user (keycloak_user_id)
) ENGINE=InnoDB COMMENT='User notification preferences';

-- =====================================================
-- 3. Notification Templates Table
-- =====================================================
CREATE TABLE IF NOT EXISTS notification_templates (
    template_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    
    template_key VARCHAR(100) UNIQUE NOT NULL COMMENT 'APPOINTMENT_REMINDER, LAB_RESULT_READY, etc.',
    template_name VARCHAR(200) NOT NULL,
    notification_type ENUM('APPOINTMENT', 'LAB_RESULT', 'PRESCRIPTION', 'BILLING', 'ALERT', 'REMINDER', 'SYSTEM') NOT NULL,
    
    -- Template Content (with placeholders)
    subject_template TEXT COMMENT 'Email subject or SMS/Push title',
    body_template TEXT NOT NULL COMMENT 'Body with {{PATIENT_NAME}}, {{DATE}}, etc.',
    
    -- Channel-specific templates
    email_template TEXT,
    sms_template TEXT,
    push_template TEXT,
    
    -- Language
    language VARCHAR(10) DEFAULT 'en' COMMENT 'en, vi, etc.',
    
    -- Status
    is_active BOOLEAN DEFAULT TRUE,
    is_system_template BOOLEAN DEFAULT FALSE,
    
    -- Audit
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by_keycloak_id VARCHAR(255),
    
    INDEX idx_key (template_key),
    INDEX idx_type (notification_type),
    INDEX idx_active (is_active),
    INDEX idx_language (language)
) ENGINE=InnoDB COMMENT='Notification templates';

-- =====================================================
-- 4. Notification Batches Table (for bulk sending)
-- =====================================================
CREATE TABLE IF NOT EXISTS notification_batches (
    batch_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    
    batch_name VARCHAR(200),
    notification_type ENUM('APPOINTMENT', 'LAB_RESULT', 'PRESCRIPTION', 'BILLING', 'ALERT', 'REMINDER', 'SYSTEM') NOT NULL,
    
    -- Recipients
    total_recipients INT DEFAULT 0,
    sent_count INT DEFAULT 0,
    delivered_count INT DEFAULT 0,
    failed_count INT DEFAULT 0,
    
    -- Status
    status ENUM('PENDING', 'IN_PROGRESS', 'COMPLETED', 'FAILED') DEFAULT 'PENDING',
    
    -- Scheduling
    scheduled_at TIMESTAMP NULL,
    started_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    
    -- Created by
    created_by_keycloak_id VARCHAR(255),
    hospital_id VARCHAR(50),
    
    -- Audit
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_status (status),
    INDEX idx_scheduled_at (scheduled_at),
    INDEX idx_hospital (hospital_id)
) ENGINE=InnoDB COMMENT='Bulk notification batches';

-- =====================================================
-- 5. Contact Us / Feedback Table
-- =====================================================
CREATE TABLE IF NOT EXISTS contact_messages (
    message_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    
    -- Sender
    fullname VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    phone_number VARCHAR(20),
    
    -- Message
    subject VARCHAR(200),
    message TEXT NOT NULL,
    
    -- Status
    status ENUM('NEW', 'IN_PROGRESS', 'RESOLVED', 'CLOSED') DEFAULT 'NEW',
    priority ENUM('LOW', 'NORMAL', 'HIGH') DEFAULT 'NORMAL',
    
    -- Response
    response TEXT,
    responded_by_keycloak_id VARCHAR(255),
    responded_at TIMESTAMP NULL,
    
    -- Timestamps
    posting_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_updation TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_email (email),
    INDEX idx_status (status),
    INDEX idx_posting_date (posting_date)
) ENGINE=InnoDB COMMENT='Contact us messages and feedback';

-- =====================================================
-- Seed Data - Notification Templates
-- =====================================================

INSERT INTO notification_templates (template_key, template_name, notification_type, subject_template, body_template, email_template, sms_template, is_system_template) VALUES
('APPOINTMENT_REMINDER_24H', 'Appointment Reminder (24 hours)', 'APPOINTMENT',
 'Reminder: Appointment tomorrow with {{DOCTOR_NAME}}',
 'Dear {{PATIENT_NAME}},\n\nThis is a reminder that you have an appointment tomorrow:\n\nDoctor: {{DOCTOR_NAME}}\nDate: {{DATE}}\nTime: {{TIME}}\nLocation: {{LOCATION}}\n\nPlease arrive 15 minutes early.\n\nThank you!',
 '<html><body><h2>Appointment Reminder</h2><p>Dear {{PATIENT_NAME}},</p><p>Tomorrow: {{DATE}} at {{TIME}}</p><p>Doctor: {{DOCTOR_NAME}}</p></body></html>',
 'Reminder: Appt tomorrow {{TIME}} with Dr. {{DOCTOR_NAME}}. {{LOCATION}}',
 TRUE),

('LAB_RESULT_READY', 'Lab Result Ready', 'LAB_RESULT',
 'Your lab results are ready',
 'Dear {{PATIENT_NAME}},\n\nYour lab test results are now available. Please log in to view your results or contact your doctor.\n\nTest: {{TEST_NAME}}\nDate: {{DATE}}\n\nThank you!',
 '<html><body><h2>Lab Results Ready</h2><p>Dear {{PATIENT_NAME}},</p><p>Test: {{TEST_NAME}}</p><p><a href="{{ACTION_URL}}">View Results</a></p></body></html>',
 'Your lab results ({{TEST_NAME}}) are ready. Log in to view.',
 TRUE),

('PRESCRIPTION_READY', 'Prescription Ready for Pickup', 'PRESCRIPTION',
 'Your prescription is ready',
 'Dear {{PATIENT_NAME}},\n\nYour prescription is ready for pickup at the pharmacy.\n\nPrescription ID: {{PRESCRIPTION_ID}}\nPharmacy: {{PHARMACY_NAME}}\n\nPlease bring your ID.\n\nThank you!',
 '<html><body><h2>Prescription Ready</h2><p>Dear {{PATIENT_NAME}},</p><p>Ready for pickup at {{PHARMACY_NAME}}</p></body></html>',
 'Prescription ready at {{PHARMACY_NAME}}. Bring ID.',
 TRUE);

SELECT 'Hospital Notifications Database created successfully!' AS status;
