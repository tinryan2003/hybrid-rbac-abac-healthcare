-- =====================================================
-- Hospital Policies Database (NORMALIZED)
-- For Policy Service (Port 8101)
-- Hybrid RBAC/ABAC Role-Centric - IAM-style Policies
-- ARCHITECTURE: Policy (container) → PolicyRules (1-to-many)
-- =====================================================

DROP DATABASE IF EXISTS hospital_policies;
CREATE DATABASE IF NOT EXISTS hospital_policies CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE hospital_policies;

-- =====================================================
-- 1. Policies Table (Policy = container with combining algorithm)
-- =====================================================
CREATE TABLE IF NOT EXISTS policies (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    
    -- Policy Identification
    tenant_id VARCHAR(50) NOT NULL COMMENT 'HOSPITAL_A, HOSPITAL_B, etc.',
    policy_id VARCHAR(100) UNIQUE NOT NULL COMMENT 'policy-001, policy-doctor-view-patients, etc.',
    policy_name VARCHAR(200) NOT NULL,
    description TEXT,
    
    -- Combining algorithm: how to resolve conflicts WITHIN this policy's rules
    -- deny-overrides (default): if any rule denies, policy denies
    -- allow-overrides: if any rule allows, policy allows (deny rules suppressed)
    -- first-applicable: use highest-priority matching rule only
    combining_algorithm VARCHAR(50) DEFAULT 'deny-overrides' COMMENT 'deny-overrides | allow-overrides | first-applicable',

    -- Priority (higher = evaluated first at policy level)
    priority INT DEFAULT 0,
    
    -- Status
    enabled BOOLEAN DEFAULT TRUE,
    
    -- Version Control
    version INT DEFAULT 1,
    
    -- Tags for organization
    tags JSON COMMENT '["production", "emergency-department"]',
    
    -- Audit
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by_keycloak_id VARCHAR(255),
    updated_by_keycloak_id VARCHAR(255),
    
    INDEX idx_tenant (tenant_id),
    INDEX idx_policy_id (policy_id),
    INDEX idx_enabled (enabled),
    INDEX idx_priority (priority DESC),
    INDEX idx_tenant_enabled (tenant_id, enabled),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB COMMENT='Policy containers with combining algorithms';

-- =====================================================
-- 2. Policy Rules Table (Rules belong to a Policy)
-- =====================================================
CREATE TABLE IF NOT EXISTS policy_rules (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    
    -- Rule Identification
    rule_id VARCHAR(100) NOT NULL COMMENT 'r1, r2, rule-allow-read, etc.',
    policy_id VARCHAR(100) NOT NULL COMMENT 'Foreign key to policies.policy_id',
    rule_name VARCHAR(200) COMMENT 'Human-readable rule name',
    
    -- Effect: Allow or Deny
    effect ENUM('Allow', 'Deny') NOT NULL DEFAULT 'Allow',
    
    -- Subjects (WHO) - JSON object with roles, users, groups
    subjects JSON NOT NULL COMMENT '{"roles": ["DOCTOR"], "departments": ["CARDIOLOGY"]}',
    
    -- Actions (WHAT) - JSON array of actions
    actions JSON NOT NULL COMMENT '["patient:view", "patient:edit", "prescription:create"]',
    
    -- Resources (ON WHAT) - JSON object
    resources JSON NOT NULL COMMENT '{"type": "patient_record", "hospital_ids": ["HOSPITAL_A"]}',
    
    -- Conditions (WHEN/WHERE) - JSON object (ABAC constraints)
    conditions JSON COMMENT '{"same_department": true, "time_range": "8:00-17:00", ...}',
    
    -- Priority (higher = evaluated first within the policy; relevant for first-applicable)
    priority INT DEFAULT 0,
    
    -- Status
    enabled BOOLEAN DEFAULT TRUE,
    
    -- Audit
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (policy_id) REFERENCES policies(policy_id) ON DELETE CASCADE,
    
    UNIQUE KEY uk_policy_rule (policy_id, rule_id),
    INDEX idx_policy (policy_id),
    INDEX idx_effect (effect),
    INDEX idx_enabled (enabled),
    INDEX idx_priority (priority DESC)
) ENGINE=InnoDB COMMENT='Authorization rules belonging to policies';

-- =====================================================
-- 3. Policy Versions Table (History)
-- =====================================================
CREATE TABLE IF NOT EXISTS policy_versions (
    version_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    policy_id VARCHAR(100) NOT NULL,
    version INT NOT NULL,
    
    -- Full policy data snapshot (includes rules as JSON for history)
    policy_data JSON NOT NULL COMMENT 'Complete policy + rules at this version',
    
    -- Change tracking
    change_type ENUM('CREATED', 'UPDATED', 'DELETED', 'ENABLED', 'DISABLED') NOT NULL,
    change_summary TEXT,
    
    -- Audit
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by_keycloak_id VARCHAR(255),
    
    FOREIGN KEY (policy_id) REFERENCES policies(policy_id) ON DELETE CASCADE,
    
    INDEX idx_policy (policy_id),
    INDEX idx_version (policy_id, version),
    INDEX idx_created_at (created_at),
    UNIQUE KEY uk_policy_version (policy_id, version)
) ENGINE=InnoDB COMMENT='Policy version history for audit and rollback';

-- =====================================================
-- 4. Policy Conflicts Table (Detected conflicts)
-- =====================================================
CREATE TABLE IF NOT EXISTS policy_conflicts (
    conflict_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    
    -- Conflicting policies (can be policyId or policyId__ruleId)
    policy_id_1 VARCHAR(100) NOT NULL,
    policy_id_2 VARCHAR(100) NOT NULL,
    
    -- Conflict details
    conflict_type ENUM('ALLOW_DENY_SAME_ACTION', 'OVERLAPPING_CONDITIONS', 'AMBIGUOUS_PRIORITY') NOT NULL,
    conflict_description TEXT,
    
    -- Severity
    severity ENUM('LOW', 'MEDIUM', 'HIGH', 'CRITICAL') DEFAULT 'MEDIUM',
    
    -- Resolution
    resolved BOOLEAN DEFAULT FALSE,
    resolution_notes TEXT,
    resolved_at TIMESTAMP NULL,
    resolved_by_keycloak_id VARCHAR(255),
    
    -- Detection
    detected_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    detected_by VARCHAR(100) COMMENT 'System or user',
    
    INDEX idx_policy_1 (policy_id_1),
    INDEX idx_policy_2 (policy_id_2),
    INDEX idx_resolved (resolved),
    INDEX idx_severity (severity),
    INDEX idx_detected_at (detected_at)
) ENGINE=InnoDB COMMENT='Detected policy conflicts';

-- =====================================================
-- 5. Policy Evaluation Logs (for testing and debugging)
-- =====================================================
CREATE TABLE IF NOT EXISTS policy_evaluation_logs (
    log_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    
    -- Request Context
    request_id VARCHAR(100),
    user_keycloak_id VARCHAR(255),
    user_role VARCHAR(50),
    action VARCHAR(100) NOT NULL,
    resource_type VARCHAR(50),
    resource_id VARCHAR(100),
    
    -- Evaluation Result
    decision ENUM('ALLOW', 'DENY', 'NOT_APPLICABLE') NOT NULL,
    matched_policies JSON COMMENT 'Array of policy IDs that matched',
    evaluation_time_ms INT COMMENT 'Time taken to evaluate',
    
    -- Context (ABAC attributes)
    context JSON COMMENT 'hospital_id, department_id, etc.',
    
    -- Audit
    evaluated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_user (user_keycloak_id),
    INDEX idx_action (action),
    INDEX idx_resource (resource_type, resource_id),
    INDEX idx_decision (decision),
    INDEX idx_evaluated_at (evaluated_at),
    INDEX idx_request (request_id)
) ENGINE=InnoDB COMMENT='Policy evaluation logs for debugging';

-- =====================================================
-- 6. Policy Templates Table (Reusable templates)
-- =====================================================
CREATE TABLE IF NOT EXISTS policy_templates (
    template_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    
    template_name VARCHAR(200) NOT NULL,
    template_category VARCHAR(100) COMMENT 'Clinical, Administrative, Emergency',
    description TEXT,
    
    -- Template structure (will have rules array format)
    template_json JSON NOT NULL COMMENT 'Policy template with placeholders',
    
    -- Usage
    usage_count INT DEFAULT 0,
    is_system_template BOOLEAN DEFAULT FALSE COMMENT 'Cannot be deleted',
    
    -- Status
    is_active BOOLEAN DEFAULT TRUE,
    
    -- Audit
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by_keycloak_id VARCHAR(255),
    
    INDEX idx_name (template_name),
    INDEX idx_category (template_category),
    INDEX idx_active (is_active)
) ENGINE=InnoDB COMMENT='Policy templates for quick policy creation';

-- =====================================================
-- Seed Data - Example Policies (Normalized Structure)
-- =====================================================

-- Policy 1: Doctors can view patients in same department (single rule)
INSERT INTO policies (tenant_id, policy_id, policy_name, description, combining_algorithm, priority, enabled)
VALUES ('HOSPITAL_A', 'policy-doctor-view-same-dept', 'Doctors View Same Department Patients',
        'Doctors can view patient records in their own department', 'deny-overrides', 10, TRUE);

INSERT INTO policy_rules (policy_id, rule_id, rule_name, effect, subjects, actions, resources, conditions, priority, enabled)
VALUES ('policy-doctor-view-same-dept', 'r1', 'Allow view same department patients', 'Allow',
        '{"roles": ["DOCTOR"]}',
        '["patient:view", "medical_history:view"]',
        '{"type": "patient_record", "hospital_ids": ["HOSPITAL_A"]}',
        '{"same_department": true}',
        10, TRUE);

-- Policy 2: Nurses can view and update vital signs (single rule)
INSERT INTO policies (tenant_id, policy_id, policy_name, description, combining_algorithm, priority, enabled)
VALUES ('HOSPITAL_A', 'policy-nurse-vital-signs', 'Nurses Manage Vital Signs',
        'Nurses can view and update vital signs for patients in their ward', 'deny-overrides', 10, TRUE);

INSERT INTO policy_rules (policy_id, rule_id, rule_name, effect, subjects, actions, resources, conditions, priority, enabled)
VALUES ('policy-nurse-vital-signs', 'r1', 'Allow nurse vital signs access', 'Allow',
        '{"roles": ["NURSE"]}',
        '["medical_history:view", "medical_history:update_vitals"]',
        '{"type": "medical_history", "hospital_ids": ["HOSPITAL_A"]}',
        '{"same_ward": true, "only_vital_signs": true}',
        10, TRUE);

-- Policy 3: Lab Techs can only view lab orders assigned to them (single rule)
INSERT INTO policies (tenant_id, policy_id, policy_name, description, combining_algorithm, priority, enabled)
VALUES ('HOSPITAL_A', 'policy-labtech-assigned-orders', 'Lab Tech Assigned Orders',
        'Lab technicians can only access lab orders assigned to them', 'deny-overrides', 10, TRUE);

INSERT INTO policy_rules (policy_id, rule_id, rule_name, effect, subjects, actions, resources, conditions, priority, enabled)
VALUES ('policy-labtech-assigned-orders', 'r1', 'Allow lab tech assigned orders', 'Allow',
        '{"roles": ["LAB_TECH"]}',
        '["lab_order:view", "lab_order:update", "lab_result:create"]',
        '{"type": "lab_order", "hospital_ids": ["HOSPITAL_A"]}',
        '{"assigned_to_user": true}',
        10, TRUE);

-- Policy 4: Deny access to critical sensitivity data without proper level (single rule)
INSERT INTO policies (tenant_id, policy_id, policy_name, description, combining_algorithm, priority, enabled)
VALUES ('HOSPITAL_A', 'policy-deny-critical-data', 'Deny Critical Sensitivity Access',
        'Deny access to critical sensitivity data unless user has level 3+ position', 'deny-overrides', 100, TRUE);

INSERT INTO policy_rules (policy_id, rule_id, rule_name, effect, subjects, actions, resources, conditions, priority, enabled)
VALUES ('policy-deny-critical-data', 'r1', 'Deny critical data low-level access', 'Deny',
        '{"roles": ["DOCTOR", "NURSE"]}',
        '["patient:view", "medical_history:view"]',
        '{"type": "patient_record", "sensitivity_levels": ["CRITICAL"]}',
        '{"position_level_less_than": 3}',
        100, TRUE);

-- Policy 5: Admin can do everything (single rule)
INSERT INTO policies (tenant_id, policy_id, policy_name, description, combining_algorithm, priority, enabled)
VALUES ('HOSPITAL_A', 'policy-admin-all', 'Admin Full Access',
        'Administrators have full access', 'deny-overrides', 1000, TRUE);

INSERT INTO policy_rules (policy_id, rule_id, rule_name, effect, subjects, actions, resources, conditions, priority, enabled)
VALUES ('policy-admin-all', 'r1', 'Allow admin full access', 'Allow',
        '{"roles": ["ADMIN"]}',
        '["*"]',
        '{"type": "*"}',
        NULL,
        1000, TRUE);

-- =====================================================
-- Multi-Rule Example Policies
-- =====================================================

-- Policy 6: Doctor full patient access (multi-rule with deny-overrides)
-- Rule r1: Allow read/update in same department during working hours
-- Rule r2: Deny delete always (deny-overrides → r2 will block delete even if r1 allows)
INSERT INTO policies (tenant_id, policy_id, policy_name, description, combining_algorithm, priority, enabled)
VALUES ('HOSPITAL_A', 'policy-doctor-patient-multi', 'Doctor Patient Access (Multi-Rule)',
        'Multi-rule policy: allow read/update within same dept during working hours; deny delete always',
        'deny-overrides', 10, TRUE);

INSERT INTO policy_rules (policy_id, rule_id, rule_name, effect, subjects, actions, resources, conditions, priority, enabled)
VALUES 
('policy-doctor-patient-multi', 'r1', 'Allow read/update in same department (working hours)', 'Allow',
 '{"roles": ["DOCTOR"]}',
 '["read", "update"]',
 '{"type": "patient_record"}',
 '{"same_department": true, "working_hours_only": true}',
 10, TRUE),
('policy-doctor-patient-multi', 'r2', 'Deny delete patient records', 'Deny',
 '{"roles": ["DOCTOR"]}',
 '["delete"]',
 '{"type": "patient_record"}',
 NULL,
 20, TRUE);

-- Policy 7: Pharmacist medicine/prescription access (multi-rule with allow-overrides)
-- Rule r-medicine-crud: Allow full CRUD on medicines
-- Rule r-prescription-crud: Allow full CRUD on prescriptions
INSERT INTO policies (tenant_id, policy_id, policy_name, description, combining_algorithm, priority, enabled)
VALUES ('HOSPITAL_A', 'policy-pharmacist-multi', 'Pharmacist Medicine & Prescription Access (Multi-Rule)',
        'Multi-rule: pharmacist manages medicines and prescriptions',
        'allow-overrides', 10, TRUE);

INSERT INTO policy_rules (policy_id, rule_id, rule_name, effect, subjects, actions, resources, conditions, priority, enabled)
VALUES 
('policy-pharmacist-multi', 'r-medicine-crud', 'Pharmacist full CRUD on medicines', 'Allow',
 '{"roles": ["PHARMACIST"]}',
 '["read", "create", "update", "delete"]',
 '{"type": "medicine"}',
 '{"same_hospital": true}',
 10, TRUE),
('policy-pharmacist-multi', 'r-prescription-crud', 'Pharmacist full CRUD on prescriptions', 'Allow',
 '{"roles": ["PHARMACIST"]}',
 '["read", "create", "update", "delete"]',
 '{"type": "prescription"}',
 '{"same_hospital": true}',
 10, TRUE);

-- =====================================================
-- Seed Data - Policy Templates
-- =====================================================

INSERT INTO policy_templates (template_name, template_category, description, template_json, is_system_template) VALUES
('Role-based View Access', 'Clinical', 'Template for role-based view permissions',
 '{"rules": [{"ruleId": "r1", "effect": "Allow", "subjects": {"roles": ["{{ROLE}}"]}, "actions": ["{{RESOURCE}}:view"], "resources": {"type": "{{RESOURCE}}"}, "conditions": null}]}',
 TRUE),
('Same Department Access', 'Clinical', 'Template for same-department access control',
 '{"rules": [{"ruleId": "r1", "effect": "Allow", "subjects": {"roles": ["{{ROLE}}"]}, "actions": ["{{RESOURCE}}:{{ACTION}}"], "resources": {"type": "{{RESOURCE}}"}, "conditions": {"same_department": true}}]}',
 TRUE),
('Emergency Override', 'Emergency', 'Template for emergency access override',
 '{"rules": [{"ruleId": "r1", "effect": "Allow", "subjects": {"roles": ["{{ROLE}}"]}, "actions": ["*"], "resources": {"type": "{{RESOURCE}}"}, "conditions": {"emergency_mode": true}}]}',
 TRUE);

SELECT 'Hospital Policies Database (NORMALIZED) created successfully!' AS status;
