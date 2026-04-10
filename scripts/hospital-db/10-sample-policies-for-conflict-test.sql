-- =====================================================
-- Sample Policies for Conflict Detection Testing
-- Uses normalized schema: policies (container) + policy_rules
-- =====================================================

USE hospital_policies;

-- Clear existing test policies (cascade deletes policy_rules)
DELETE FROM policies WHERE policy_id LIKE 'test-%';

-- =====================================================
-- Non-Conflicting Policies (Same Effect, Different Actions)
-- =====================================================

-- Policy 1: DOCTOR read access in department 1
INSERT INTO policies (tenant_id, policy_id, policy_name, description, combining_algorithm, priority, enabled)
VALUES (
    'HOSPITAL_A',
    'test-doctor-read-dept1',
    'Doctor Read Department 1',
    'Allow doctors in department 1 to read patient records',
    'deny-overrides', 10, TRUE
);
INSERT INTO policy_rules (policy_id, rule_id, rule_name, effect, subjects, actions, resources, conditions, priority, enabled)
VALUES (
    'test-doctor-read-dept1', 'r1', 'Allow doctor read same dept', 'Allow',
    '{"roles": ["DOCTOR"], "department_ids": ["1"]}',
    '["read"]',
    '{"type": "patient_record", "department_ids": ["1"]}',
    '{"same_department": true}',
    10, TRUE
);

-- Policy 2: DOCTOR write access in department 1 (no conflict - different action)
INSERT INTO policies (tenant_id, policy_id, policy_name, description, combining_algorithm, priority, enabled)
VALUES (
    'HOSPITAL_A',
    'test-doctor-write-dept1',
    'Doctor Write Department 1',
    'Allow doctors in department 1 to write patient records',
    'deny-overrides', 10, TRUE
);
INSERT INTO policy_rules (policy_id, rule_id, rule_name, effect, subjects, actions, resources, conditions, priority, enabled)
VALUES (
    'test-doctor-write-dept1', 'r1', 'Allow doctor write same dept', 'Allow',
    '{"roles": ["DOCTOR"], "department_ids": ["1"]}',
    '["write"]',
    '{"type": "patient_record", "department_ids": ["1"]}',
    '{"same_department": true}',
    10, TRUE
);

-- =====================================================
-- Conflicting Policies (Different Effect, Overlapping Scope)
-- =====================================================

-- Policy 3: DOCTOR read/write in departments 1,2 (ALLOW)
INSERT INTO policies (tenant_id, policy_id, policy_name, description, combining_algorithm, priority, enabled)
VALUES (
    'HOSPITAL_A',
    'test-conflict-allow',
    'Doctor Read Departments 1-2 ALLOW',
    'Allow doctors in departments 1 or 2 to read patient records',
    'deny-overrides', 5, TRUE
);
INSERT INTO policy_rules (policy_id, rule_id, rule_name, effect, subjects, actions, resources, conditions, priority, enabled)
VALUES (
    'test-conflict-allow', 'r1', 'Allow doctor read dept 1-2', 'Allow',
    '{"roles": ["DOCTOR"], "department_ids": ["1", "2"]}',
    '["read", "write"]',
    '{"type": "patient_record", "department_ids": ["1", "2"]}',
    NULL,
    5, TRUE
);

-- Policy 4: DOCTOR read in departments 2,3 (DENY) — CONFLICTS with Policy 3 on dept 2
INSERT INTO policies (tenant_id, policy_id, policy_name, description, combining_algorithm, priority, enabled)
VALUES (
    'HOSPITAL_A',
    'test-conflict-deny',
    'Doctor Read Departments 2-3 DENY',
    'Deny doctors in departments 2 or 3 from reading sensitive patient records',
    'deny-overrides', 20, TRUE
);
INSERT INTO policy_rules (policy_id, rule_id, rule_name, effect, subjects, actions, resources, conditions, priority, enabled)
VALUES (
    'test-conflict-deny', 'r1', 'Deny doctor read dept 2-3', 'Deny',
    '{"roles": ["DOCTOR"], "department_ids": ["2", "3"]}',
    '["read"]',
    '{"type": "patient_record", "department_ids": ["2", "3"]}',
    NULL,
    20, TRUE
);
-- Expected conflict: DOCTOR in dept 2 reading patient_record → Policy 3 ALLOW vs Policy 4 DENY

-- =====================================================
-- Exception Conflict
-- =====================================================

-- Policy 5: NURSE read vitals (general ALLOW for depts 1,2,3)
INSERT INTO policies (tenant_id, policy_id, policy_name, description, combining_algorithm, priority, enabled)
VALUES (
    'HOSPITAL_A',
    'test-nurse-general-allow',
    'Nurse General Read Access',
    'Allow nurses to read patient vitals',
    'deny-overrides', 5, TRUE
);
INSERT INTO policy_rules (policy_id, rule_id, rule_name, effect, subjects, actions, resources, conditions, priority, enabled)
VALUES (
    'test-nurse-general-allow', 'r1', 'Allow nurse read vitals general', 'Allow',
    '{"roles": ["NURSE"], "department_ids": ["1", "2", "3"]}',
    '["read"]',
    '{"type": "patient_vitals"}',
    '{"working_hours": true}',
    5, TRUE
);

-- Policy 6: NURSE read vitals in department 2 (specific DENY exception) — CONFLICTS with Policy 5
INSERT INTO policies (tenant_id, policy_id, policy_name, description, combining_algorithm, priority, enabled)
VALUES (
    'HOSPITAL_A',
    'test-nurse-dept2-deny',
    'Nurse Department 2 Restricted',
    'Deny nurses in department 2 from reading patient vitals (exception)',
    'deny-overrides', 20, TRUE
);
INSERT INTO policy_rules (policy_id, rule_id, rule_name, effect, subjects, actions, resources, conditions, priority, enabled)
VALUES (
    'test-nurse-dept2-deny', 'r1', 'Deny nurse read vitals dept 2', 'Deny',
    '{"roles": ["NURSE"], "department_ids": ["2"]}',
    '["read"]',
    '{"type": "patient_vitals"}',
    NULL,
    20, TRUE
);
-- Expected conflict: NURSE in dept 2 reading patient_vitals → Policy 5 ALLOW vs Policy 6 DENY

-- =====================================================
-- Non-Conflicting: Disjoint Departments
-- =====================================================

-- Policy 7: DOCTOR dept 1 ALLOW
INSERT INTO policies (tenant_id, policy_id, policy_name, description, combining_algorithm, priority, enabled)
VALUES (
    'HOSPITAL_A',
    'test-doctor-dept1-allow',
    'Doctor Department 1 Allow',
    'Allow doctors in department 1',
    'deny-overrides', 10, TRUE
);
INSERT INTO policy_rules (policy_id, rule_id, rule_name, effect, subjects, actions, resources, conditions, priority, enabled)
VALUES (
    'test-doctor-dept1-allow', 'r1', 'Allow doctor dept 1', 'Allow',
    '{"roles": ["DOCTOR"], "department_ids": ["1"]}',
    '["read"]',
    '{"type": "patient_record"}',
    NULL,
    10, TRUE
);

-- Policy 8: DOCTOR dept 3 DENY (no conflict — disjoint with dept 1)
INSERT INTO policies (tenant_id, policy_id, policy_name, description, combining_algorithm, priority, enabled)
VALUES (
    'HOSPITAL_A',
    'test-doctor-dept3-deny',
    'Doctor Department 3 Deny',
    'Deny doctors in department 3',
    'deny-overrides', 10, TRUE
);
INSERT INTO policy_rules (policy_id, rule_id, rule_name, effect, subjects, actions, resources, conditions, priority, enabled)
VALUES (
    'test-doctor-dept3-deny', 'r1', 'Deny doctor dept 3', 'Deny',
    '{"roles": ["DOCTOR"], "department_ids": ["3"]}',
    '["read"]',
    '{"type": "patient_record"}',
    NULL,
    10, TRUE
);

-- =====================================================
-- Expected Results Summary
-- =====================================================
SELECT
    'Expected Conflicts' AS test_result,
    '2 conflicts should be detected:' AS description;

SELECT
    '1. test-conflict-allow vs test-conflict-deny' AS conflict_1,
    'DOCTOR dept 2 reading patient_record: r1=ALLOW, r1=DENY' AS reason_1;

SELECT
    '2. test-nurse-general-allow vs test-nurse-dept2-deny' AS conflict_2,
    'NURSE dept 2 reading patient_vitals: r1=ALLOW, r1=DENY (exception conflict)' AS reason_2;

SELECT
    'No conflict between test-doctor-dept1-allow and test-doctor-dept3-deny' AS non_conflict,
    'Disjoint departments [1] vs [3] — no overlap' AS reason;

COMMIT;
