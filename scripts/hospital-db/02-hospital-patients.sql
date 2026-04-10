-- =====================================================
-- Hospital Patients Database
-- For Patient Service (Port 8085)
-- Based on ERD - Patients, MedicalHistory
-- =====================================================

CREATE DATABASE IF NOT EXISTS hospital_patients CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE hospital_patients;

-- =====================================================
-- 1. Patients Table (theo ERD)
-- =====================================================
CREATE TABLE IF NOT EXISTS patients (
    patient_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    firstname VARCHAR(50) NOT NULL,
    lastname VARCHAR(50) NOT NULL,
    address TEXT,
    birthday DATE NOT NULL,
    gender VARCHAR(50) NOT NULL,
    phone_number VARCHAR(50) NOT NULL,
    emergency_contact VARCHAR(50),
    photo_image LONGBLOB,
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    last_visited DATETIME,
    -- Link account (không lưu Password, dùng Keycloak)
    keycloak_user_id VARCHAR(255) UNIQUE,
    hospital_id VARCHAR(50) DEFAULT 'HOSPITAL_A',
    created_by_keycloak_id VARCHAR(255) NULL COMMENT 'Keycloak subject of creator',
    INDEX idx_keycloak_user (keycloak_user_id),
    INDEX idx_phone (phone_number),
    INDEX idx_name (lastname, firstname)
) ENGINE=InnoDB COMMENT='Patients - theo ERD';

-- =====================================================
-- 2. Medical History Table (theo ERD)
-- =====================================================
CREATE TABLE IF NOT EXISTS medical_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    patient_id BIGINT NOT NULL,
    blood_pressure FLOAT,
    blood_sugar FLOAT,
    weight FLOAT,
    height FLOAT,
    temperature VARCHAR(100),
    medical_pres TEXT,
    creation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (patient_id) REFERENCES patients(patient_id) ON DELETE CASCADE,
    INDEX idx_patient (patient_id),
    INDEX idx_creation_date (creation_date)
) ENGINE=InnoDB COMMENT='Medical history - theo ERD';

-- =====================================================
-- 3. Patient Allergies (chuẩn hóa từ ERD)
-- =====================================================
CREATE TABLE IF NOT EXISTS patient_allergies (
    allergy_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    patient_id BIGINT NOT NULL,
    allergen VARCHAR(100) NOT NULL,
    severity VARCHAR(50) DEFAULT 'MILD',
    reaction TEXT,
    diagnosed_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (patient_id) REFERENCES patients(patient_id) ON DELETE CASCADE,
    INDEX idx_patient (patient_id)
) ENGINE=InnoDB COMMENT='Patient allergies';

-- =====================================================
-- Seed Data
-- =====================================================
INSERT INTO patients (firstname, lastname , gender, birthday, address, phone_number, emergency_contact, created_date, hospital_id) VALUES
('Tin', 'Tu Trung', 'Male', '2003-09-19', '106 Nguyen Van Tao St, Ho Chi Minh City', '0389951641', '0906925926', NOW(), 'HOSPITAL_A');

SELECT 'Hospital Patients Database created successfully!' AS status;
