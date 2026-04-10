-- =====================================================
-- Announcements Table (Hospital-wide or department/ward-specific)
-- Part of notification-service or can be standalone
-- =====================================================

USE hospital_notifications;

CREATE TABLE IF NOT EXISTS announcements (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    
    -- Content
    title VARCHAR(500) NOT NULL,
    content TEXT NOT NULL,
    
    -- Target audience (optional filters)
    -- If all null → announcement for everyone
    target_hospital_id BIGINT COMMENT 'Target specific hospital (null = all hospitals)',
    target_department_id BIGINT COMMENT 'Target specific department (null = all departments)',
    target_ward_id BIGINT COMMENT 'Target specific ward (null = all wards)',
    target_roles JSON COMMENT '["DOCTOR","NURSE"] or null for all roles',
    
    -- Priority and status
    priority ENUM('LOW','MEDIUM','HIGH','URGENT') DEFAULT 'MEDIUM',
    status ENUM('DRAFT','PUBLISHED','ARCHIVED') DEFAULT 'DRAFT',
    
    -- Publishing schedule
    published_at TIMESTAMP NULL COMMENT 'When it was published (null if still draft)',
    expires_at TIMESTAMP NULL COMMENT 'Optional expiration date',
    
    -- Creator tracking
    created_by_keycloak_id VARCHAR(255) NOT NULL,
    created_by_name VARCHAR(255) COMMENT 'Name for display',
    
    -- Audit
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_status (status),
    INDEX idx_published (published_at),
    INDEX idx_hospital (target_hospital_id),
    INDEX idx_department (target_department_id),
    INDEX idx_created_by (created_by_keycloak_id)
) ENGINE=InnoDB COMMENT='Hospital announcements (general, department, ward-specific)';

-- =====================================================
-- Seed Data - Sample Announcements
-- =====================================================

INSERT INTO announcements (
    title,
    content,
    target_hospital_id,
    target_department_id,
    target_ward_id,
    target_roles,
    priority,
    status,
    published_at,
    created_by_keycloak_id,
    created_by_name
) VALUES 
(
    'Hospital-wide Staff Meeting - March 2026',
    'All staff are required to attend the quarterly meeting on March 15, 2026 at 2:00 PM in the main conference hall. Topics: Q1 review, new policies, and upcoming initiatives.',
    NULL, NULL, NULL, NULL,
    'HIGH',
    'PUBLISHED',
    NOW(),
    'system',
    'Admin'
),
(
    'New Patient Safety Protocol - Emergency Department',
    'Effective immediately: all Emergency Department staff must complete the updated Patient Safety Protocol training module by end of week. Link: https://training.hospital.local/patient-safety',
    1, 1, NULL, '["DOCTOR","NURSE"]',
    'URGENT',
    'PUBLISHED',
    NOW(),
    'system',
    'Emergency Dept Head'
),
(
    'IT System Maintenance - Saturday 10 PM',
    'The Electronic Health Records (EHR) system will be offline for scheduled maintenance on Saturday, March 20 from 10:00 PM to 2:00 AM. Please complete all documentation before 9:45 PM.',
    NULL, NULL, NULL, NULL,
    'MEDIUM',
    'PUBLISHED',
    DATE_SUB(NOW(), INTERVAL 2 DAY),
    'system',
    'IT Department'
),
(
    'Congratulations - Research Award',
    'Dr. Sarah Johnson from Cardiology has been awarded the National Research Excellence Award for her work on heart disease prevention. Please join us in congratulating her!',
    NULL, 2, NULL, NULL,
    'LOW',
    'PUBLISHED',
    DATE_SUB(NOW(), INTERVAL 7 DAY),
    'system',
    'Admin'
);

SELECT 'Announcements table created and seeded successfully!' AS status;
