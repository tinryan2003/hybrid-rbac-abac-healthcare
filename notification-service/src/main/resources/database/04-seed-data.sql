-- ============================================================================
-- Seed Data Script for Notification Service
-- ============================================================================
-- Description: Inserts sample notification data for testing
-- Author: VGU Banking System
-- Date: 2025-12-17
-- ============================================================================

USE banking_notifications;

-- Insert sample notifications
INSERT INTO notifications (
    user_id, user_type, type, title, message, 
    email_subject, status, channel, sent_at, created_at
) VALUES
-- Customer transaction notifications
(1, 'CUSTOMER', 'TRANSACTION', 'Transaction Created', 
 'Your transfer of 5,000,000 VND has been submitted for approval.',
 'Transaction Booking Confirmation', 'SENT', 'BOTH', NOW(), NOW()),

(1, 'CUSTOMER', 'TRANSACTION', 'Transaction Approved', 
 'Your transfer of 5,000,000 VND has been approved and completed.',
 'Transaction Approved Successfully', 'SENT', 'BOTH', NOW(), NOW()),

(2, 'CUSTOMER', 'TRANSACTION', 'Transaction Rejected', 
 'Your withdrawal of 10,000,000 VND has been rejected. Please contact support.',
 'Transaction Rejected', 'SENT', 'BOTH', NOW(), NOW()),

-- Account notifications
(1, 'CUSTOMER', 'ACCOUNT', 'Account Created', 
 'Your new savings account has been created successfully.',
 'Welcome to VGU Banking', 'SENT', 'BOTH', NOW(), NOW() - INTERVAL 1 DAY),

(2, 'CUSTOMER', 'ACCOUNT', 'Daily Limit Updated', 
 'Your daily transaction limit has been updated to 20,000,000 VND.',
 'Account Settings Updated', 'SENT', 'IN_APP', NOW(), NOW() - INTERVAL 2 HOUR),

-- Security notifications
(1, 'CUSTOMER', 'SECURITY', 'New Login Detected', 
 'A new login was detected from a new device. If this wasn\'t you, please contact us immediately.',
 'Security Alert: New Login', 'SENT', 'BOTH', NOW(), NOW() - INTERVAL 3 HOUR),

(3, 'CUSTOMER', 'SECURITY', 'Password Changed', 
 'Your password was successfully changed. If you didn\'t make this change, contact support immediately.',
 'Password Change Confirmation', 'SENT', 'BOTH', NOW(), NOW() - INTERVAL 1 HOUR),

-- Employee notifications
(101, 'EMPLOYEE', 'TRANSACTION', 'High-Value Transaction Pending', 
 'A high-value transaction of 50,000,000 VND is pending your approval.',
 'Action Required: Transaction Approval', 'SENT', 'IN_APP', NOW(), NOW()),

(102, 'EMPLOYEE', 'ACCOUNT', 'New Account Application', 
 'A new customer account application requires review.',
 'New Account Application', 'SENT', 'IN_APP', NOW(), NOW() - INTERVAL 30 MINUTE),

-- System notifications
(1, 'CUSTOMER', 'SYSTEM', 'System Maintenance', 
 'Scheduled system maintenance on Dec 20, 2025 from 2:00 AM to 4:00 AM.',
 'Scheduled Maintenance Notice', 'SENT', 'BOTH', NOW(), NOW() - INTERVAL 1 DAY),

-- Unread notification
(1, 'CUSTOMER', 'TRANSACTION', 'New Transaction Received', 
 'You have received a transfer of 2,000,000 VND from account 1000000002.',
 'Money Received', 'SENT', 'BOTH', NOW(), NOW());

-- Display success message
SELECT 'Sample notification data inserted successfully' AS status;
SELECT COUNT(*) AS total_notifications FROM notifications;
SELECT status, COUNT(*) AS count FROM notifications GROUP BY status;

