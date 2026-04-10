-- =====================================================
-- Hospital Pharmacy Database
-- For Pharmacy Service (Port 8095)
-- Hybrid RBAC/ABAC Role-Centric
-- =====================================================

CREATE DATABASE IF NOT EXISTS hospital_pharmacy CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE hospital_pharmacy;

-- =====================================================
-- 1. Medicine Table
-- =====================================================
CREATE TABLE IF NOT EXISTS medicine (
    medicine_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    
    -- Basic Information
    name VARCHAR(200) NOT NULL,
    generic_name VARCHAR(200),
    brand_name VARCHAR(200),
    
    -- Details
    description TEXT,
    side_effect TEXT COMMENT 'Side effects',
    
    -- Classification
    category VARCHAR(100) COMMENT 'Antibiotic, Painkiller, etc.',
    dosage_form VARCHAR(50) COMMENT 'Tablet, Capsule, Syrup, Injection',
    strength VARCHAR(50) COMMENT 'e.g., 500mg, 10ml',
    
    -- Inventory
    unit VARCHAR(20) DEFAULT 'PIECE' COMMENT 'PIECE, BOX, BOTTLE',
    unit_price DECIMAL(10,2),
    stock_quantity INT DEFAULT 0,
    reorder_level INT DEFAULT 10,
    
    -- Regulation
    requires_prescription BOOLEAN DEFAULT TRUE,
    controlled_substance BOOLEAN DEFAULT FALSE,
    
    -- ABAC
    hospital_id VARCHAR(50) DEFAULT 'HOSPITAL_A' COMMENT 'If hospital-specific',
    
    -- Status
    is_active BOOLEAN DEFAULT TRUE,
    
    -- Audit
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_name (name),
    INDEX idx_generic (generic_name),
    INDEX idx_category (category),
    INDEX idx_active (is_active),
    INDEX idx_stock (stock_quantity)
) ENGINE=InnoDB COMMENT='Medicine/Drug catalog';

-- =====================================================
-- 2. Prescriptions Table
-- =====================================================
CREATE TABLE IF NOT EXISTS prescriptions (
    prescription_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    
    -- Participants (Reference to other services)
    doctor_id BIGINT NOT NULL COMMENT 'Doctor from user-service',
    patient_id BIGINT NOT NULL COMMENT 'Patient from patient-service',
    appointment_id BIGINT COMMENT 'Related appointment',
    
    -- Prescription Details
    prescription_date DATE NOT NULL,
    diagnosis TEXT COMMENT 'Diagnosis for this prescription',
    notes TEXT COMMENT 'Additional instructions',
    
    -- Status
    status ENUM('PENDING', 'APPROVED', 'DISPENSED', 'CANCELLED') DEFAULT 'PENDING',
    
    -- Dispensing Information
    dispensed_by_pharmacist_id BIGINT COMMENT 'Pharmacist who dispensed',
    dispensed_at TIMESTAMP NULL,
    
    -- ABAC Attributes
    hospital_id VARCHAR(50) DEFAULT 'HOSPITAL_A',
    sensitivity_level ENUM('NORMAL', 'HIGH', 'CRITICAL') DEFAULT 'NORMAL' COMMENT 'For controlled substances',
    created_by_keycloak_id VARCHAR(255) NULL COMMENT 'Keycloak subject who created prescription',
    
    -- Audit
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_doctor (doctor_id),
    INDEX idx_patient (patient_id),
    INDEX idx_appointment (appointment_id),
    INDEX idx_status (status),
    INDEX idx_date (prescription_date),
    INDEX idx_hospital (hospital_id)
) ENGINE=InnoDB COMMENT='Medical prescriptions';

-- =====================================================
-- 3. Prescription Items Table (Line items)
-- =====================================================
CREATE TABLE IF NOT EXISTS prescription_items (
    item_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    prescription_id BIGINT NOT NULL,
    medicine_id BIGINT NOT NULL,
    
    -- Dosage Instructions
    dosage VARCHAR(100) NOT NULL COMMENT 'e.g., 500mg',
    frequency VARCHAR(100) NOT NULL COMMENT 'e.g., 3 times daily',
    duration_days INT COMMENT 'Number of days',
    
    -- Timing
    start_date DATE NOT NULL,
    end_date DATE,
    
    -- Quantity
    quantity INT NOT NULL COMMENT 'Number of units to dispense',
    quantity_dispensed INT DEFAULT 0,
    
    -- Instructions
    instructions TEXT COMMENT 'How to take the medicine',
    before_after_meal ENUM('BEFORE', 'AFTER', 'WITH', 'ANYTIME'),
    
    -- Pricing
    unit_price DECIMAL(10,2),
    total_price DECIMAL(10,2),
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (prescription_id) REFERENCES prescriptions(prescription_id) ON DELETE CASCADE,
    FOREIGN KEY (medicine_id) REFERENCES medicine(medicine_id),
    
    INDEX idx_prescription (prescription_id),
    INDEX idx_medicine (medicine_id)
) ENGINE=InnoDB COMMENT='Prescription line items';

-- =====================================================
-- 4. Medicine Inventory Transactions Table
-- =====================================================
CREATE TABLE IF NOT EXISTS medicine_inventory_transactions (
    transaction_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    medicine_id BIGINT NOT NULL,
    
    transaction_type ENUM('IN', 'OUT', 'ADJUSTMENT', 'EXPIRED') NOT NULL,
    quantity INT NOT NULL COMMENT 'Positive for IN, Negative for OUT',
    
    reference_id BIGINT COMMENT 'Prescription ID or Purchase Order ID',
    reference_type VARCHAR(50) COMMENT 'PRESCRIPTION, PURCHASE, ADJUSTMENT',
    
    performed_by_keycloak_id VARCHAR(255),
    hospital_id VARCHAR(50),
    
    notes TEXT,
    transaction_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (medicine_id) REFERENCES medicine(medicine_id),
    
    INDEX idx_medicine (medicine_id),
    INDEX idx_type (transaction_type),
    INDEX idx_date (transaction_date)
) ENGINE=InnoDB COMMENT='Medicine stock movements';

-- =====================================================
-- Seed Data for Medicine
-- =====================================================
INSERT INTO medicine (name, generic_name, brand_name, description, side_effect, category, dosage_form, strength, unit_price, stock_quantity, requires_prescription, hospital_id) VALUES
('Paracetamol 500mg', 'Paracetamol', 'Tylenol', 'Pain reliever and fever reducer', 'Nausea, stomach pain', 'Painkiller', 'Tablet', '500mg', 2000.00, 1000, FALSE, 'HOSPITAL_A'),
('Amoxicillin 500mg', 'Amoxicillin', 'Amoxil', 'Antibiotic for bacterial infections', 'Diarrhea, nausea', 'Antibiotic', 'Capsule', '500mg', 5000.00, 500, TRUE, 'HOSPITAL_A'),
('Ibuprofen 400mg', 'Ibuprofen', 'Advil', 'Anti-inflammatory drug', 'Stomach upset, dizziness', 'NSAID', 'Tablet', '400mg', 3000.00, 800, FALSE, 'HOSPITAL_A'),
('Metformin 500mg', 'Metformin', 'Glucophage', 'Diabetes medication', 'Nausea, diarrhea', 'Antidiabetic', 'Tablet', '500mg', 4000.00, 600, TRUE, 'HOSPITAL_A'),
('Omeprazole 20mg', 'Omeprazole', 'Prilosec', 'Reduces stomach acid', 'Headache, stomach pain', 'PPI', 'Capsule', '20mg', 3500.00, 400, TRUE, 'HOSPITAL_A');

-- Ensure existing rows (if any) are associated with HOSPITAL_A when hospital_id is NULL
UPDATE medicine SET hospital_id = 'HOSPITAL_A' WHERE hospital_id IS NULL;

SELECT 'Hospital Pharmacy Database created successfully!' AS status;
