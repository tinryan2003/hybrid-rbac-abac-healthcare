-- ============================================================================
-- Table Creation Script for Notification Service
-- ============================================================================
-- Description: Creates the notifications table and related structures
-- Author: VGU Banking System
-- Date: 2025-12-17
-- ============================================================================

USE banking_notifications;

-- Drop table if exists (use with caution in production)
-- DROP TABLE IF EXISTS notifications;

-- Create notifications table
CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    
    -- User information
    user_id BIGINT NOT NULL COMMENT 'ID of the user receiving the notification',
    user_type VARCHAR(20) NOT NULL COMMENT 'CUSTOMER or EMPLOYEE',
    
    -- Notification content
    type VARCHAR(50) NOT NULL COMMENT 'Notification type: TRANSACTION, ACCOUNT, SECURITY, SYSTEM, MARKETING',
    title VARCHAR(200) NOT NULL COMMENT 'Short notification title',
    message TEXT NOT NULL COMMENT 'Notification message',
    
    -- Email specific
    email_subject VARCHAR(200) NULL COMMENT 'Email subject line',
    email_body TEXT NULL COMMENT 'HTML email body',
    
    -- Status and delivery
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING, SENT, DELIVERED, FAILED',
    channel VARCHAR(20) NOT NULL DEFAULT 'BOTH' COMMENT 'IN_APP, EMAIL, BOTH',
    
    -- Timestamps
    read_at DATETIME NULL COMMENT 'When notification was read (in-app)',
    sent_at DATETIME NULL COMMENT 'When in-app notification was sent',
    email_sent_at DATETIME NULL COMMENT 'When email was sent',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
    
    -- Metadata
    metadata JSON NULL COMMENT 'Additional data in JSON format',
    
    -- Constraints
    CONSTRAINT chk_user_type CHECK (user_type IN ('CUSTOMER', 'EMPLOYEE')),
    CONSTRAINT chk_status CHECK (status IN ('PENDING', 'SENT', 'DELIVERED', 'FAILED')),
    CONSTRAINT chk_channel CHECK (channel IN ('IN_APP', 'EMAIL', 'BOTH')),
    
    -- Indexes (defined separately in 03-create-indexes.sql)
    INDEX idx_user_id (user_id),
    INDEX idx_user_type (user_type),
    INDEX idx_type (type),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Notifications table for banking system';

-- Display success message
SELECT 'Table notifications created successfully' AS status;

