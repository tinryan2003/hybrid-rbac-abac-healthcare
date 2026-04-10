-- =====================================================
-- Hospital Lab & Orders Database
-- For Order Entry Service (Port 8093) & Lab Service (Port 8094)
-- Hybrid RBAC/ABAC Role-Centric
-- =====================================================

CREATE DATABASE IF NOT EXISTS hospital_lab CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE hospital_lab;

-- =====================================================
-- 1. Lab Orders Table
-- =====================================================
CREATE TABLE IF NOT EXISTS lab_orders (
    lab_order_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    
    -- Participants (Reference to other services)
    patient_id BIGINT NOT NULL COMMENT 'Patient from patient-service',
    doctor_id BIGINT NOT NULL COMMENT 'Ordering doctor from user-service',
    appointment_id BIGINT COMMENT 'Related appointment',
    
    -- Order Details
    order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    order_type ENUM('LAB', 'IMAGING', 'PATHOLOGY') DEFAULT 'LAB',
    
    -- Clinical Information
    clinical_diagnosis TEXT COMMENT 'Provisional diagnosis',
    clinical_notes TEXT COMMENT 'Clinical notes',
    urgency ENUM('ROUTINE', 'URGENT', 'STAT') DEFAULT 'ROUTINE',
    
    -- Status
    status ENUM('PENDING', 'COLLECTED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED') DEFAULT 'PENDING',
    
    -- Specimen Collection
    specimen_collected_at TIMESTAMP NULL,
    specimen_collected_by_keycloak_id VARCHAR(255),
    
    -- Processing
    processed_by_lab_tech_id BIGINT COMMENT 'Lab technician',
    started_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    
    -- ABAC Attributes
    hospital_id VARCHAR(50) DEFAULT 'HOSPITAL_A',
    department_id BIGINT,
    sensitivity_level ENUM('NORMAL', 'HIGH', 'CRITICAL') DEFAULT 'NORMAL',
    created_by_keycloak_id VARCHAR(255) NULL COMMENT 'Keycloak subject who created order',
    
    -- Audit
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_patient (patient_id),
    INDEX idx_doctor (doctor_id),
    INDEX idx_appointment (appointment_id),
    INDEX idx_status (status),
    INDEX idx_urgency (urgency),
    INDEX idx_order_date (order_date),
    INDEX idx_hospital (hospital_id)
) ENGINE=InnoDB COMMENT='Lab and imaging orders';

-- =====================================================
-- 2. Lab Tests Catalog Table
-- =====================================================
CREATE TABLE IF NOT EXISTS lab_tests_catalog (
    test_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    
    -- Test Information
    test_code VARCHAR(50) UNIQUE NOT NULL,
    test_name VARCHAR(200) NOT NULL,
    test_category VARCHAR(100) COMMENT 'Hematology, Chemistry, Microbiology, etc.',
    
    -- Details
    description TEXT,
    specimen_type VARCHAR(100) COMMENT 'Blood, Urine, etc.',
    specimen_volume VARCHAR(50),
    
    -- Processing
    turnaround_time_hours INT COMMENT 'Expected TAT',
    requires_fasting BOOLEAN DEFAULT FALSE,
    special_preparation TEXT,
    
    -- Pricing
    price DECIMAL(10,2),
    
    -- Status
    is_active BOOLEAN DEFAULT TRUE,
    
    -- Audit
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_code (test_code),
    INDEX idx_name (test_name),
    INDEX idx_category (test_category),
    INDEX idx_active (is_active)
) ENGINE=InnoDB COMMENT='Catalog of available lab tests';

-- =====================================================
-- 3. Lab Order Items Table (Line items)
-- =====================================================
CREATE TABLE IF NOT EXISTS lab_order_items (
    order_item_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    lab_order_id BIGINT NOT NULL,
    test_id BIGINT NOT NULL,
    
    -- Status per test
    status ENUM('PENDING', 'COLLECTED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED') DEFAULT 'PENDING',
    
    -- Priority
    priority INT DEFAULT 1 COMMENT 'Order of processing',
    
    -- Pricing
    price DECIMAL(10,2),
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (lab_order_id) REFERENCES lab_orders(lab_order_id) ON DELETE CASCADE,
    FOREIGN KEY (test_id) REFERENCES lab_tests_catalog(test_id),
    
    INDEX idx_order (lab_order_id),
    INDEX idx_test (test_id),
    INDEX idx_status (status)
) ENGINE=InnoDB COMMENT='Individual tests in a lab order';

-- =====================================================
-- 4. Lab Results Table
-- =====================================================
CREATE TABLE IF NOT EXISTS lab_results (
    result_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    lab_order_id BIGINT NOT NULL,
    order_item_id BIGINT NOT NULL,
    test_id BIGINT NOT NULL,
    
    -- Result Details
    result_value VARCHAR(500) COMMENT 'Test result value',
    result_unit VARCHAR(50) COMMENT 'Unit of measurement',
    reference_range VARCHAR(200) COMMENT 'Normal range',
    
    -- Interpretation
    result_status ENUM('NORMAL', 'ABNORMAL', 'CRITICAL', 'PENDING') DEFAULT 'PENDING',
    interpretation TEXT COMMENT 'Lab technician interpretation',
    flags VARCHAR(50) COMMENT 'H (High), L (Low), C (Critical)',
    
    -- Quality Control
    specimen_adequacy ENUM('ADEQUATE', 'INADEQUATE', 'HEMOLYZED', 'CLOTTED'),
    comments TEXT,
    
    -- Personnel
    performed_by_lab_tech_id BIGINT COMMENT 'Lab technician who performed test',
    verified_by_lab_tech_id BIGINT COMMENT 'Lab technician who verified',
    approved_by_pathologist_id BIGINT COMMENT 'Pathologist who approved',
    
    -- ABAC
    sensitivity_level ENUM('NORMAL', 'HIGH', 'CRITICAL') DEFAULT 'NORMAL',
    
    -- Timestamps
    result_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    verified_at TIMESTAMP NULL,
    approved_at TIMESTAMP NULL,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (lab_order_id) REFERENCES lab_orders(lab_order_id) ON DELETE CASCADE,
    FOREIGN KEY (order_item_id) REFERENCES lab_order_items(order_item_id) ON DELETE CASCADE,
    FOREIGN KEY (test_id) REFERENCES lab_tests_catalog(test_id),
    
    INDEX idx_order (lab_order_id),
    INDEX idx_test (test_id),
    INDEX idx_status (result_status),
    INDEX idx_result_date (result_date),
    INDEX idx_sensitivity (sensitivity_level)
) ENGINE=InnoDB COMMENT='Lab test results';

-- =====================================================
-- Seed Data for Lab Tests Catalog
-- =====================================================
INSERT INTO lab_tests_catalog (test_code, test_name, test_category, description, specimen_type, turnaround_time_hours, price, requires_fasting) VALUES
('CBC', 'Complete Blood Count', 'Hematology', 'Full blood count analysis', 'Blood', 2, 150000.00, FALSE),
('FBS', 'Fasting Blood Sugar', 'Chemistry', 'Blood glucose level', 'Blood', 1, 50000.00, TRUE),
('HbA1c', 'Hemoglobin A1c', 'Chemistry', 'Average blood sugar over 3 months', 'Blood', 24, 200000.00, FALSE),
('LIPID', 'Lipid Profile', 'Chemistry', 'Cholesterol and triglycerides', 'Blood', 4, 180000.00, TRUE),
('LFT', 'Liver Function Test', 'Chemistry', 'Liver enzymes', 'Blood', 4, 250000.00, FALSE),
('RFT', 'Renal Function Test', 'Chemistry', 'Kidney function', 'Blood', 4, 220000.00, FALSE),
('TSH', 'Thyroid Stimulating Hormone', 'Chemistry', 'Thyroid function', 'Blood', 24, 180000.00, FALSE),
('UA', 'Urinalysis', 'Chemistry', 'Urine analysis', 'Urine', 1, 80000.00, FALSE);

SELECT 'Hospital Lab & Orders Database created successfully!' AS status;
