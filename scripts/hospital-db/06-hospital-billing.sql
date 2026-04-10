-- =====================================================
-- Hospital Billing Database
-- For Billing Service (Port 8096)
-- Hybrid RBAC/ABAC Role-Centric
-- =====================================================

CREATE DATABASE IF NOT EXISTS hospital_billing CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE hospital_billing;

-- =====================================================
-- 1. Invoices Table
-- =====================================================
CREATE TABLE IF NOT EXISTS invoices (
    invoice_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    invoice_number VARCHAR(50) UNIQUE NOT NULL,
    
    -- Participant (Reference to other services)
    patient_id BIGINT NOT NULL COMMENT 'Patient from patient-service',
    appointment_id BIGINT COMMENT 'Related appointment',
    
    -- Invoice Details
    invoice_date DATE NOT NULL,
    due_date DATE,
    
    -- Amounts
    subtotal DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    discount_amount DECIMAL(12,2) DEFAULT 0.00,
    tax_amount DECIMAL(12,2) DEFAULT 0.00,
    total_amount DECIMAL(12,2) NOT NULL,
    
    -- Status
    status ENUM('PENDING', 'PAID', 'PARTIALLY_PAID', 'CANCELLED', 'REFUNDED') DEFAULT 'PENDING',
    
    -- Payment Information
    paid_amount DECIMAL(12,2) DEFAULT 0.00,
    outstanding_amount DECIMAL(12,2),
    paid_date TIMESTAMP NULL,
    
    -- Insurance
    insurance_company VARCHAR(100),
    insurance_policy_number VARCHAR(100),
    insurance_coverage_amount DECIMAL(12,2) DEFAULT 0.00,
    
    -- Notes
    notes TEXT,
    internal_notes TEXT COMMENT 'For staff only',
    
    -- ABAC Attributes
    hospital_id VARCHAR(50) DEFAULT 'HOSPITAL_A',
    created_by_keycloak_id VARCHAR(255),
    
    -- Audit
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_patient (patient_id),
    INDEX idx_appointment (appointment_id),
    INDEX idx_invoice_number (invoice_number),
    INDEX idx_status (status),
    INDEX idx_invoice_date (invoice_date),
    INDEX idx_hospital (hospital_id)
) ENGINE=InnoDB COMMENT='Patient invoices';

-- =====================================================
-- 2. Invoice Items Table (Line items)
-- =====================================================
CREATE TABLE IF NOT EXISTS invoice_items (
    item_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    invoice_id BIGINT NOT NULL,
    
    -- Item Details
    item_type ENUM('CONSULTATION', 'LAB_TEST', 'IMAGING', 'PROCEDURE', 'MEDICATION', 'ROOM_CHARGE', 'OTHER') NOT NULL,
    item_code VARCHAR(50),
    item_description VARCHAR(500) NOT NULL,
    
    -- Reference
    reference_id BIGINT COMMENT 'Lab order ID, Prescription ID, etc.',
    reference_type VARCHAR(50) COMMENT 'LAB_ORDER, PRESCRIPTION, PROCEDURE',
    
    -- Pricing
    quantity INT DEFAULT 1,
    unit_price DECIMAL(10,2) NOT NULL,
    discount_percent DECIMAL(5,2) DEFAULT 0.00,
    discount_amount DECIMAL(10,2) DEFAULT 0.00,
    tax_percent DECIMAL(5,2) DEFAULT 0.00,
    tax_amount DECIMAL(10,2) DEFAULT 0.00,
    total_price DECIMAL(10,2) NOT NULL,
    
    -- Service Provider
    provider_id BIGINT COMMENT 'Doctor/Lab Tech who provided service',
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (invoice_id) REFERENCES invoices(invoice_id) ON DELETE CASCADE,
    
    INDEX idx_invoice (invoice_id),
    INDEX idx_item_type (item_type),
    INDEX idx_reference (reference_type, reference_id)
) ENGINE=InnoDB COMMENT='Invoice line items';

-- =====================================================
-- 3. Payments Table
-- =====================================================
CREATE TABLE IF NOT EXISTS payments (
    payment_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    invoice_id BIGINT NOT NULL,
    
    -- Payment Details
    payment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    payment_method ENUM('CASH', 'CREDIT_CARD', 'DEBIT_CARD', 'BANK_TRANSFER', 'INSURANCE', 'OTHER') NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    
    -- Payment Reference
    transaction_id VARCHAR(100) COMMENT 'External transaction ID',
    payment_reference VARCHAR(200) COMMENT 'Check number, transfer reference',
    
    -- Status
    status ENUM('PENDING', 'COMPLETED', 'FAILED', 'REFUNDED') DEFAULT 'PENDING',
    
    -- Card Information (if applicable, encrypted)
    card_last_four VARCHAR(4),
    card_type VARCHAR(20),
    
    -- Personnel
    received_by_billing_clerk_id BIGINT,
    received_by_keycloak_id VARCHAR(255),
    
    -- Notes
    notes TEXT,
    
    -- Audit
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (invoice_id) REFERENCES invoices(invoice_id),
    
    INDEX idx_invoice (invoice_id),
    INDEX idx_payment_date (payment_date),
    INDEX idx_method (payment_method),
    INDEX idx_status (status),
    INDEX idx_transaction (transaction_id)
) ENGINE=InnoDB COMMENT='Payment records';

-- =====================================================
-- 4. Service Pricing Catalog Table
-- =====================================================
CREATE TABLE IF NOT EXISTS service_pricing (
    pricing_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    
    -- Service Details
    service_code VARCHAR(50) UNIQUE NOT NULL,
    service_name VARCHAR(200) NOT NULL,
    service_category ENUM('CONSULTATION', 'LAB', 'IMAGING', 'PROCEDURE', 'ROOM', 'OTHER') NOT NULL,
    description TEXT,
    
    -- Pricing
    base_price DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'VND',
    
    -- Insurance
    insurance_covered BOOLEAN DEFAULT FALSE,
    insurance_coverage_percent DECIMAL(5,2),
    
    -- Department
    department_id BIGINT,
    
    -- Status
    is_active BOOLEAN DEFAULT TRUE,
    effective_from DATE,
    effective_to DATE,
    
    -- ABAC
    hospital_id VARCHAR(50),
    
    -- Audit
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_code (service_code),
    INDEX idx_name (service_name),
    INDEX idx_category (service_category),
    INDEX idx_active (is_active),
    INDEX idx_hospital (hospital_id)
) ENGINE=InnoDB COMMENT='Service pricing catalog';

-- =====================================================
-- 5. Payment Refunds Table
-- =====================================================
CREATE TABLE IF NOT EXISTS payment_refunds (
    refund_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    payment_id BIGINT NOT NULL,
    invoice_id BIGINT NOT NULL,
    
    refund_amount DECIMAL(12,2) NOT NULL,
    refund_reason TEXT,
    refund_method ENUM('CASH', 'CREDIT_CARD', 'BANK_TRANSFER', 'OTHER'),
    
    status ENUM('PENDING', 'APPROVED', 'REJECTED', 'COMPLETED') DEFAULT 'PENDING',
    
    requested_by_keycloak_id VARCHAR(255),
    approved_by_keycloak_id VARCHAR(255),
    processed_by_keycloak_id VARCHAR(255),
    
    refund_date TIMESTAMP NULL,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (payment_id) REFERENCES payments(payment_id),
    FOREIGN KEY (invoice_id) REFERENCES invoices(invoice_id),
    
    INDEX idx_payment (payment_id),
    INDEX idx_invoice (invoice_id),
    INDEX idx_status (status)
) ENGINE=InnoDB COMMENT='Payment refunds';

-- =====================================================
-- Seed Data for Service Pricing
-- =====================================================
INSERT INTO service_pricing (service_code, service_name, service_category, base_price, hospital_id, is_active) VALUES
('CONS-GEN', 'General Consultation', 'CONSULTATION', 200000.00, 'HOSPITAL_A', TRUE),
('CONS-SPEC', 'Specialist Consultation', 'CONSULTATION', 500000.00, 'HOSPITAL_A', TRUE),
('ROOM-STANDARD', 'Standard Room (per day)', 'ROOM', 800000.00, 'HOSPITAL_A', TRUE),
('ROOM-VIP', 'VIP Room (per day)', 'ROOM', 2000000.00, 'HOSPITAL_A', TRUE),
('PROC-MINOR', 'Minor Procedure', 'PROCEDURE', 1000000.00, 'HOSPITAL_A', TRUE);

SELECT 'Hospital Billing Database created successfully!' AS status;
