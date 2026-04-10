-- =====================================================
-- Hospital Users Database
-- For User Service (Port 8090)
-- Hybrid RBAC/ABAC Role-Centric
-- =====================================================

CREATE DATABASE IF NOT EXISTS hospital_users CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE hospital_users;

-- =====================================================
-- 1. Users Table (Common - Link with Keycloak)
-- =====================================================
CREATE TABLE IF NOT EXISTS users (
    user_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    keycloak_user_id VARCHAR(255) UNIQUE NOT NULL COMMENT 'Keycloak UUID',
    email VARCHAR(100) UNIQUE NOT NULL,
    phone_number VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_keycloak_user (keycloak_user_id),
    INDEX idx_email (email)
) ENGINE=InnoDB COMMENT='Common users table linked with Keycloak';

-- =====================================================
-- 2. Departments Table
-- =====================================================
CREATE TABLE IF NOT EXISTS departments (
    department_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    location VARCHAR(100),
    hospital_id VARCHAR(50) COMMENT 'For multi-hospital support',
    description TEXT,
    creation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_hospital (hospital_id),
    INDEX idx_name (name)
) ENGINE=InnoDB COMMENT='Hospital departments/khoa';

-- =====================================================
-- 3. Doctors Table (ROLE_DOCTOR)
-- =====================================================
CREATE TABLE IF NOT EXISTS doctors (
    doctor_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    gender ENUM('Male', 'Female', 'Other'),
    field VARCHAR(100) COMMENT 'Specialization/Chuyên khoa',
    birthday DATE,
    email_address VARCHAR(100),
    phone_number VARCHAR(20),
    
    -- ABAC Attributes
    department_id BIGINT,
    hospital_id VARCHAR(50) COMMENT 'ABAC: Hospital ID',
    position_level INT DEFAULT 2 COMMENT 'ABAC: 1=Junior, 2=Senior, 3=Head',
    
    -- Employment
    is_active BOOLEAN DEFAULT TRUE,
    
    -- Audit
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (department_id) REFERENCES departments(department_id) ON DELETE SET NULL,
    
    INDEX idx_user (user_id),
    INDEX idx_department (department_id),
    INDEX idx_hospital (hospital_id),
    INDEX idx_field (field),
    INDEX idx_active (is_active)
) ENGINE=InnoDB COMMENT='Doctors - ROLE_DOCTOR';

-- =====================================================
-- 4. Nurses Table (ROLE_NURSE)
-- =====================================================
CREATE TABLE IF NOT EXISTS nurses (
    nurse_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    gender ENUM('Male', 'Female', 'Other'),
    birthday DATE,
    phone_number VARCHAR(20),
    email VARCHAR(100),
    
    -- ABAC Attributes
    department_id BIGINT,
    hospital_id VARCHAR(50) COMMENT 'ABAC: Hospital ID',
    position_level INT DEFAULT 1 COMMENT 'ABAC: 1=Staff, 2=Senior, 3=Head Nurse',
    
    -- Employment
    is_active BOOLEAN DEFAULT TRUE,
    
    -- Audit
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (department_id) REFERENCES departments(department_id) ON DELETE SET NULL,
    
    INDEX idx_user (user_id),
    INDEX idx_department (department_id),
    INDEX idx_hospital (hospital_id),
    INDEX idx_active (is_active)
) ENGINE=InnoDB COMMENT='Nurses - ROLE_NURSE';

-- =====================================================
-- 5. Admins Table (ROLE_ADMIN)
-- =====================================================
CREATE TABLE IF NOT EXISTS admins (
    admin_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    birthday DATE,
    phone_number VARCHAR(20),
    email VARCHAR(100),
    gender ENUM('Male', 'Female', 'Other'),
    
    -- ABAC Attributes
    hospital_id VARCHAR(50) COMMENT 'ABAC: Hospital ID (NULL = system admin)',
    admin_level ENUM('SYSTEM', 'HOSPITAL', 'DEPARTMENT') DEFAULT 'HOSPITAL',
    
    -- Employment (required by BaseStaffProfile)
    is_active BOOLEAN DEFAULT TRUE,
    
    -- Audit
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    
    INDEX idx_user (user_id),
    INDEX idx_hospital (hospital_id),
    INDEX idx_admin_level (admin_level)
) ENGINE=InnoDB COMMENT='System and Hospital Admins';

-- =====================================================
-- 6. Lab Technicians Table (ROLE_LAB_TECH)
-- =====================================================
CREATE TABLE IF NOT EXISTS lab_technicians (
    lab_tech_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(100),
    phone_number VARCHAR(20),
    
    -- ABAC Attributes
    department_id BIGINT COMMENT 'Lab Department',
    hospital_id VARCHAR(50),
    specialization VARCHAR(100) COMMENT 'Lab specialization',
    
    is_active BOOLEAN DEFAULT TRUE,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (department_id) REFERENCES departments(department_id) ON DELETE SET NULL,
    
    INDEX idx_user (user_id),
    INDEX idx_hospital (hospital_id)
) ENGINE=InnoDB COMMENT='Lab Technicians - ROLE_LAB_TECH';

-- =====================================================
-- 7. Pharmacists Table (ROLE_PHARMACIST)
-- =====================================================
CREATE TABLE IF NOT EXISTS pharmacists (
    pharmacist_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(100),
    phone_number VARCHAR(20),
    
    -- ABAC Attributes
    hospital_id VARCHAR(50),
    license_number VARCHAR(50),
    
    is_active BOOLEAN DEFAULT TRUE,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    
    INDEX idx_user (user_id),
    INDEX idx_hospital (hospital_id)
) ENGINE=InnoDB COMMENT='Pharmacists - ROLE_PHARMACIST';

-- =====================================================
-- 8. Receptionists Table (ROLE_RECEPTIONIST)
-- =====================================================
CREATE TABLE IF NOT EXISTS receptionists (
    receptionist_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(100),
    phone_number VARCHAR(20),
    
    -- ABAC Attributes
    hospital_id VARCHAR(50),
    
    is_active BOOLEAN DEFAULT TRUE,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    
    INDEX idx_user (user_id),
    INDEX idx_hospital (hospital_id)
) ENGINE=InnoDB COMMENT='Receptionists - ROLE_RECEPTIONIST';

-- =====================================================
-- 9. Billing Clerks Table (ROLE_BILLING_CLERK)
-- =====================================================
CREATE TABLE IF NOT EXISTS billing_clerks (
    billing_clerk_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(100),
    phone_number VARCHAR(20),
    
    -- ABAC Attributes
    hospital_id VARCHAR(50),
    
    is_active BOOLEAN DEFAULT TRUE,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    
    INDEX idx_user (user_id),
    INDEX idx_hospital (hospital_id)
) ENGINE=InnoDB COMMENT='Billing Clerks - ROLE_BILLING_CLERK';

-- =====================================================
-- Seed Data for Departments
-- =====================================================
INSERT INTO departments (name, location, hospital_id, description) VALUES
('Cardiology', 'Building A - Floor 3', 'HOSPITAL_A', 'Khoa Tim mạch'),
('Neurology', 'Building A - Floor 4', 'HOSPITAL_A', 'Khoa Thần kinh'),
('Oncology', 'Building B - Floor 2', 'HOSPITAL_A', 'Khoa Ung bướu'),
('Pediatrics', 'Building C - Floor 1', 'HOSPITAL_A', 'Khoa Nhi'),
('Emergency', 'Building A - Floor 1', 'HOSPITAL_A', 'Khoa Cấp cứu'),
('Laboratory', 'Building D - Floor 1', 'HOSPITAL_A', 'Phòng Xét nghiệm'),
('Pharmacy', 'Building E - Floor 1', 'HOSPITAL_A', 'Nhà thuốc');

-- =====================================================
-- Seed Data: 3 Test Users (doctor1, nurse1, patient1)
-- Keycloak UUIDs must match users in Keycloak realm
-- =====================================================

-- 1. Users (common table - link with Keycloak)
INSERT INTO users (keycloak_user_id, email, phone_number) VALUES
('8255d43b-59a1-47a7-8e60-32bb9b0c525c', 'doctor1@hospital.com', '0901111111'),
('73ffelbe-8082-43f3-9334-656fb08db67d', 'nurse1@hospital.com', '0902222222'),
('ccb3c2cf-f647-44a6-b416-fc1faaf0a307', 'patient1@hospital.com', '0903333333');

-- 2. Doctor (user_id=1, department_id=1 Cardiology)
INSERT INTO doctors (user_id, first_name, last_name, gender, field, email_address, phone_number, department_id, hospital_id, position_level, is_active) VALUES
(1, 'Tieu', 'Van', 'Male', 'Cardiology', 'doctor1@hospital.com', '0901111111', 1, 'HOSPITAL_A', 2, TRUE);

-- 3. Nurse (user_id=2, department_id=1 Cardiology)
INSERT INTO nurses (user_id, first_name, last_name, gender, email, phone_number, department_id, hospital_id, position_level, is_active) VALUES
(2, 'Tho Ly', 'Truong', 'Female', 'nurse1@hospital.com', '0902222222', 1, 'HOSPITAL_A', 1, TRUE);

-- patient1: only in users table (patient record is in hospital_patients DB with keycloak_user_id link)

-- =====================================================
-- Seed Data: admin1 (Keycloak hospital-realm admin)
-- Keycloak ID: c915c63d-6a14-4dd7-a73b-f9d545ea50f6
-- =====================================================
INSERT INTO users (keycloak_user_id, email, phone_number) VALUES
('c915c63d-6a14-4dd7-a73b-f9d545ea50f6', 'admin1@hospital.com', NULL);

INSERT INTO admins (user_id, first_name, last_name, hospital_id, admin_level) VALUES
(LAST_INSERT_ID(), 'Tin', 'Tu', 'HOSPITAL_A', 'HOSPITAL');

SELECT 'Hospital Users Database created successfully!' AS status;
