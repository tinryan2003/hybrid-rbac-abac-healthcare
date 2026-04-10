-- =====================================================
-- Hospital Appointments Database
-- For Appointment Service (Port 8092)
-- Hybrid RBAC/ABAC Role-Centric
-- =====================================================

CREATE DATABASE IF NOT EXISTS hospital_appointments CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE hospital_appointments;

-- =====================================================
-- 1. Appointments Table
-- =====================================================
CREATE TABLE IF NOT EXISTS appointments (
    appointment_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    
    -- Participants (Reference to other services)
    doctor_id BIGINT NOT NULL COMMENT 'Doctor from user-service',
    patient_id BIGINT NOT NULL COMMENT 'Patient from patient-service',
    doctor_specialization VARCHAR(100) COMMENT 'Doctor field/specialization',
    
    -- Appointment Details
    appointment_date DATE NOT NULL,
    appointment_time TIME NOT NULL,
    duration_minutes INT DEFAULT 30 COMMENT 'Appointment duration',
    
    -- Reason
    reason TEXT COMMENT 'Reason for appointment',
    notes TEXT COMMENT 'Additional notes',
    
    -- Status
    status ENUM('PENDING', 'CONFIRMED', 'CANCELLED', 'COMPLETED', 'NO_SHOW') DEFAULT 'PENDING',
    
    -- ABAC Attributes
    hospital_id VARCHAR(50) DEFAULT 'HOSPITAL_A' COMMENT 'ABAC: Hospital',
    department_id BIGINT COMMENT 'Department',
    
    -- Timestamps
    create_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    approve_date TIMESTAMP NULL COMMENT 'When confirmed',
    cancel_date TIMESTAMP NULL COMMENT 'When cancelled',
    completed_date TIMESTAMP NULL COMMENT 'When completed',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Created by
    created_by_keycloak_id VARCHAR(255) COMMENT 'Who created (patient/receptionist)',
    
    INDEX idx_doctor (doctor_id),
    INDEX idx_patient (patient_id),
    INDEX idx_date (appointment_date),
    INDEX idx_status (status),
    INDEX idx_hospital (hospital_id),
    INDEX idx_department (department_id),
    INDEX idx_doctor_date (doctor_id, appointment_date)
) ENGINE=InnoDB COMMENT='Appointments';

-- =====================================================
-- 2. Appointment Slots Table (for scheduling)
-- =====================================================
CREATE TABLE IF NOT EXISTS appointment_slots (
    slot_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    doctor_id BIGINT NOT NULL,
    
    slot_date DATE NOT NULL,
    slot_time TIME NOT NULL,
    duration_minutes INT DEFAULT 30,
    
    is_available BOOLEAN DEFAULT TRUE,
    max_patients INT DEFAULT 1,
    booked_count INT DEFAULT 0,
    
    hospital_id VARCHAR(50),
    department_id BIGINT,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_doctor_date (doctor_id, slot_date),
    INDEX idx_available (is_available),
    INDEX idx_hospital (hospital_id),
    UNIQUE KEY uk_doctor_slot (doctor_id, slot_date, slot_time)
) ENGINE=InnoDB COMMENT='Available appointment slots';

-- =====================================================
-- 3. Appointment Reminders Table
-- =====================================================
CREATE TABLE IF NOT EXISTS appointment_reminders (
    reminder_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    appointment_id BIGINT NOT NULL,
    
    reminder_type ENUM('EMAIL', 'SMS', 'PUSH') NOT NULL,
    reminder_time TIMESTAMP NOT NULL,
    sent BOOLEAN DEFAULT FALSE,
    sent_at TIMESTAMP NULL,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (appointment_id) REFERENCES appointments(appointment_id) ON DELETE CASCADE,
    
    INDEX idx_appointment (appointment_id),
    INDEX idx_reminder_time (reminder_time),
    INDEX idx_sent (sent)
) ENGINE=InnoDB COMMENT='Appointment reminders';

-- =====================================================
-- 4. Appointment History/Changes Table (Audit)
-- =====================================================
CREATE TABLE IF NOT EXISTS appointment_history (
    history_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    appointment_id BIGINT NOT NULL,
    
    changed_by_keycloak_id VARCHAR(255),
    action ENUM('CREATED', 'CONFIRMED', 'CANCELLED', 'RESCHEDULED', 'COMPLETED') NOT NULL,
    previous_status VARCHAR(20),
    new_status VARCHAR(20),
    
    previous_date DATE,
    previous_time TIME,
    new_date DATE,
    new_time TIME,
    
    reason TEXT COMMENT 'Reason for change',
    notes TEXT,
    
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (appointment_id) REFERENCES appointments(appointment_id) ON DELETE CASCADE,
    
    INDEX idx_appointment (appointment_id),
    INDEX idx_changed_at (changed_at)
) ENGINE=InnoDB COMMENT='Appointment change history';

SELECT 'Hospital Appointments Database created successfully!' AS status;
