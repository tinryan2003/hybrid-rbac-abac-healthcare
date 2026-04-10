-- =====================================================
-- Seed: Admin full access to patient records
-- Run when Policies list is empty and you need Admin to
-- view/create/update/delete patients (e.g. list/patients page).
-- Uses normalized schema: policies (container) + policy_rules
-- =====================================================
-- Usage: docker exec -i hospital-mysql mysql -uroot -pS@l19092003 hospital_policies < scripts/hospital-db/13-seed-admin-patient-policy.sql
-- =====================================================

USE hospital_policies;

-- Avoid duplicate: remove if exists (cascade deletes policy_rules)
DELETE FROM policies WHERE policy_id = 'admin-patient-record-full';

-- 1. Insert policy container
INSERT INTO policies (
    tenant_id,
    policy_id,
    policy_name,
    description,
    combining_algorithm,
    priority,
    enabled
) VALUES (
    'HOSPITAL_A',
    'admin-patient-record-full',
    'Admin full access to patient records',
    'Allow ADMIN role to read, create, update, and delete patient records (baseline for HMS)',
    'deny-overrides',
    50,
    TRUE
);

-- 2. Insert the rule under that policy
INSERT INTO policy_rules (
    policy_id,
    rule_id,
    rule_name,
    effect,
    subjects,
    actions,
    resources,
    conditions,
    priority,
    enabled
) VALUES (
    'admin-patient-record-full',
    'r1',
    'Allow admin full CRUD on patient records',
    'Allow',
    '{"roles": ["ADMIN"]}',
    '["read", "create", "update", "delete"]',
    '{"type": "patient_record"}',
    NULL,
    50,
    TRUE
);

SELECT 'Seed policy admin-patient-record-full inserted. Restart policy-service or wait for OPA sync to apply.' AS result;
