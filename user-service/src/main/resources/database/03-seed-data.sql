-- =====================================================
-- User Service - Sample Data Script
-- =====================================================
-- This script inserts sample employees for testing
-- Represents J.P. Morgan hierarchy: CEO > Director > Manager > Employee
-- =====================================================

USE banking_users;

-- Clear existing data
DELETE FROM employee_profiles;
ALTER TABLE employee_profiles AUTO_INCREMENT = 1;

-- =====================================================
-- Insert Sample Employees (Hierarchy Structure)
-- =====================================================

-- 1. CEO (Top Level - Unlimited Approval)
INSERT INTO employee_profiles (
    keycloak_user_id, employee_number, full_name, email, phone_number,
    role, department, job_title, branch_id,
    approval_limit, can_approve_transactions,
    reports_to_user_id, employment_status, hire_date
) VALUES (
    'ceo-keycloak-uuid', 'EMP001', 'Nguyễn Văn CEO', 'ceo@vgubank.com', '0901234567',
    'ROLE_CEO', 'Executive', 'CEO', 'HQ',
    99999999999.00, TRUE,
    NULL, 'ACTIVE', '2020-01-01'
);

-- 2. Finance Director (Reports to CEO - 5 tỷ VND)
INSERT INTO employee_profiles (
    keycloak_user_id, employee_number, full_name, email, phone_number,
    role, department, job_title, branch_id,
    approval_limit, can_approve_transactions,
    reports_to_user_id, employment_status, hire_date
) VALUES (
    'director-keycloak-uuid', 'EMP002', 'Trần Thị Director', 'director@vgubank.com', '0901234568',
    'ROLE_DIRECTOR', 'Finance', 'DIRECTOR_OF_FINANCE', 'BR001',
    5000000000.00, TRUE,
    1, 'ACTIVE', '2020-06-01'
);

-- 3. Branch Manager BR001 (Reports to Director - 100 triệu VND)
INSERT INTO employee_profiles (
    keycloak_user_id, employee_number, full_name, email, phone_number,
    role, department, job_title, branch_id,
    approval_limit, can_approve_transactions,
    reports_to_user_id, employment_status, hire_date
) VALUES (
    'manager-keycloak-uuid', 'EMP003', 'Lê Văn Manager', 'manager@vgubank.com', '0901234569',
    'ROLE_MANAGER', 'Finance', 'BRANCH_MANAGER', 'BR001',
    100000000.00, TRUE,
    2, 'ACTIVE', '2021-01-15'
);

-- 4. Teller (Reports to Manager - 10 triệu VND)
INSERT INTO employee_profiles (
    keycloak_user_id, employee_number, full_name, email, phone_number,
    role, department, job_title, branch_id,
    approval_limit, can_approve_transactions,
    reports_to_user_id, employment_status, hire_date
) VALUES (
    'employee-keycloak-uuid', 'EMP004', 'Phạm Thị Employee', 'employee@vgubank.com', '0901234570',
    'ROLE_EMPLOYEE', 'Finance', 'TELLER', 'BR001',
    10000000.00, FALSE,
    3, 'ACTIVE', '2022-03-20'
);

-- 5. Another Manager at BR002 (Reports to Director)
INSERT INTO employee_profiles (
    keycloak_user_id, employee_number, full_name, email, phone_number,
    role, department, job_title, branch_id,
    approval_limit, can_approve_transactions,
    reports_to_user_id, employment_status, hire_date
) VALUES (
    'manager2-keycloak-uuid', 'EMP005', 'Hoàng Văn Manager2', 'manager2@vgubank.com', '0901234571',
    'ROLE_MANAGER', 'Operations', 'OPERATIONS_MANAGER', 'BR002',
    100000000.00, TRUE,
    2, 'ACTIVE', '2021-06-01'
);

-- 6. Another Employee at BR002
INSERT INTO employee_profiles (
    keycloak_user_id, employee_number, full_name, email, phone_number,
    role, department, job_title, branch_id,
    approval_limit, can_approve_transactions,
    reports_to_user_id, employment_status, hire_date
) VALUES (
    'employee2-keycloak-uuid', 'EMP006', 'Võ Thị Employee2', 'employee2@vgubank.com', '0901234572',
    'ROLE_EMPLOYEE', 'Operations', 'CUSTOMER_SERVICE_REPRESENTATIVE', 'BR002',
    10000000.00, FALSE,
    5, 'ACTIVE', '2022-07-10'
);

-- 6a. Tin Tu (tinryan2003) - Real user from Keycloak
INSERT INTO employee_profiles (
    keycloak_user_id, employee_number, full_name, email, phone_number,
    role, department, job_title, branch_id,
    approval_limit, can_approve_transactions,
    reports_to_user_id, employment_status, hire_date
) VALUES (
    '28c5f85f-0d6a-48bc-b3cf-9f4c8cde3834', 'EMP101', 'Tin Tu', 'tinryan2003@gmail.com', '0909999999',
    'ROLE_EMPLOYEE', 'Finance', 'OPERATIONS_SPECIALIST', 'BR001',
    50000000.00, FALSE,
    3, 'ACTIVE', '2023-01-01'
);

-- 7. Operations Director (Reports to CEO)
INSERT INTO employee_profiles (
    keycloak_user_id, employee_number, full_name, email, phone_number,
    role, department, job_title, branch_id,
    approval_limit, can_approve_transactions,
    reports_to_user_id, employment_status, hire_date
) VALUES (
    'director2-keycloak-uuid', 'EMP007', 'Đặng Văn Director2', 'director2@vgubank.com', '0901234573',
    'ROLE_DIRECTOR', 'Operations', 'DIRECTOR_OF_OPERATIONS', 'HQ',
    5000000000.00, TRUE,
    1, 'ACTIVE', '2020-09-01'
);

-- =====================================================
-- Internal Transaction Users (for Hybrid RBAC/ABAC Demo)
-- =====================================================

-- 8. HR Manager (for INTERNAL_SALARY & INTERNAL_REIMBURSEMENT)
INSERT INTO employee_profiles (
    keycloak_user_id, employee_number, full_name, email, phone_number,
    role, department, job_title, branch_id,
    approval_limit, can_approve_transactions,
    reports_to_user_id, employment_status, hire_date
) VALUES (
    'hr-manager-keycloak-uuid', 'EMP008', 'Nguyễn Thị HR Manager', 'hr.manager@vgubank.com', '0901234574',
    'ROLE_HR', 'Human Resources', 'HR_MANAGER', 'HQ',
    1000000000.00, TRUE,  -- 1 tỷ VND (higher limit for manager)
    1, 'ACTIVE', '2021-01-15'
);

-- 9. HR Specialist (for INTERNAL_SALARY & INTERNAL_REIMBURSEMENT)
INSERT INTO employee_profiles (
    keycloak_user_id, employee_number, full_name, email, phone_number,
    role, department, job_title, branch_id,
    approval_limit, can_approve_transactions,
    reports_to_user_id, employment_status, hire_date
) VALUES (
    'hr-specialist-keycloak-uuid', 'EMP009', 'Trần Văn HR Specialist', 'hr.specialist@vgubank.com', '0901234575',
    'ROLE_HR', 'Human Resources', 'HR_SPECIALIST', 'HQ',
    100000000.00, FALSE,  -- 100 triệu VND (lower limit, no approval)
    8, 'ACTIVE', '2022-03-20'
);

-- 10. Payroll Manager (for INTERNAL_SALARY)
INSERT INTO employee_profiles (
    keycloak_user_id, employee_number, full_name, email, phone_number,
    role, department, job_title, branch_id,
    approval_limit, can_approve_transactions,
    reports_to_user_id, employment_status, hire_date
) VALUES (
    'payroll-manager-keycloak-uuid', 'EMP010', 'Lê Thị Payroll Manager', 'payroll.manager@vgubank.com', '0901234576',
    'ROLE_PAYROLL', 'Payroll', 'PAYROLL_MANAGER', 'HQ',
    5000000000.00, TRUE,  -- 5 tỷ VND (higher limit for payroll manager)
    1, 'ACTIVE', '2021-06-01'
);

-- 11. Payroll Specialist (for INTERNAL_SALARY)
INSERT INTO employee_profiles (
    keycloak_user_id, employee_number, full_name, email, phone_number,
    role, department, job_title, branch_id,
    approval_limit, can_approve_transactions,
    reports_to_user_id, employment_status, hire_date
) VALUES (
    'payroll-specialist-keycloak-uuid', 'EMP011', 'Phạm Văn Payroll Specialist', 'payroll.specialist@vgubank.com', '0901234577',
    'ROLE_PAYROLL', 'Payroll', 'PAYROLL_SPECIALIST', 'HQ',
    500000000.00, FALSE,  -- 500 triệu VND (lower limit, no approval)
    10, 'ACTIVE', '2022-07-10'
);

-- 12. Finance Director (for INTERNAL_TRANSFER)
INSERT INTO employee_profiles (
    keycloak_user_id, employee_number, full_name, email, phone_number,
    role, department, job_title, branch_id,
    approval_limit, can_approve_transactions,
    reports_to_user_id, employment_status, hire_date
) VALUES (
    'finance-director-keycloak-uuid', 'EMP012', 'Hoàng Thị Finance Director', 'finance.director@vgubank.com', '0901234578',
    'ROLE_FINANCE', 'Finance', 'FINANCE_DIRECTOR', 'HQ',
    2000000000.00, TRUE,  -- 2 tỷ VND
    1, 'ACTIVE', '2021-01-01'
);

-- 13. Finance Specialist (for INTERNAL_TRANSFER)
INSERT INTO employee_profiles (
    keycloak_user_id, employee_number, full_name, email, phone_number,
    role, department, job_title, branch_id,
    approval_limit, can_approve_transactions,
    reports_to_user_id, employment_status, hire_date
) VALUES (
    'finance-specialist-keycloak-uuid', 'EMP013', 'Võ Văn Finance Specialist', 'finance.specialist@vgubank.com', '0901234579',
    'ROLE_FINANCE', 'Finance', 'FINANCE_SPECIALIST', 'HQ',
    200000000.00, FALSE,  -- 200 triệu VND (lower limit, no approval)
    12, 'ACTIVE', '2022-05-15'
);

-- 14. Accounting Director (for INTERNAL_TRANSFER)
INSERT INTO employee_profiles (
    keycloak_user_id, employee_number, full_name, email, phone_number,
    role, department, job_title, branch_id,
    approval_limit, can_approve_transactions,
    reports_to_user_id, employment_status, hire_date
) VALUES (
    'accounting-director-keycloak-uuid', 'EMP014', 'Đặng Thị Accounting Director', 'accounting.director@vgubank.com', '0901234580',
    'ROLE_ACCOUNTING', 'Accounting', 'ACCOUNTING_DIRECTOR', 'HQ',
    1500000000.00, TRUE,  -- 1.5 tỷ VND
    1, 'ACTIVE', '2021-03-01'
);

-- 15. Accountant (for INTERNAL_TRANSFER)
INSERT INTO employee_profiles (
    keycloak_user_id, employee_number, full_name, email, phone_number,
    role, department, job_title, branch_id,
    approval_limit, can_approve_transactions,
    reports_to_user_id, employment_status, hire_date
) VALUES (
    'accountant-keycloak-uuid', 'EMP015', 'Bùi Văn Accountant', 'accountant@vgubank.com', '0901234581',
    'ROLE_ACCOUNTING', 'Accounting', 'ACCOUNTANT', 'HQ',
    150000000.00, FALSE,  -- 150 triệu VND (lower limit, no approval)
    14, 'ACTIVE', '2022-09-01'
);

-- 16. Department Head - Finance (for INTERNAL_EXPENSE)
INSERT INTO employee_profiles (
    keycloak_user_id, employee_number, full_name, email, phone_number,
    role, department, job_title, branch_id,
    approval_limit, can_approve_transactions,
    reports_to_user_id, employment_status, hire_date
) VALUES (
    'dept-head-finance-keycloak-uuid', 'EMP016', 'Nguyễn Thị Dept Head Finance', 'dept.head.finance@vgubank.com', '0901234582',
    'ROLE_DEPARTMENT_HEAD', 'Finance', 'DEPARTMENT_HEAD_FINANCE', 'HQ',
    500000000.00, TRUE,  -- 500 triệu VND
    12, 'ACTIVE', '2021-08-01'
);

-- 17. Department Head - Operations (for INTERNAL_EXPENSE)
INSERT INTO employee_profiles (
    keycloak_user_id, employee_number, full_name, email, phone_number,
    role, department, job_title, branch_id,
    approval_limit, can_approve_transactions,
    reports_to_user_id, employment_status, hire_date
) VALUES (
    'dept-head-operations-keycloak-uuid', 'EMP017', 'Trần Văn Dept Head Operations', 'dept.head.ops@vgubank.com', '0901234583',
    'ROLE_DEPARTMENT_HEAD', 'Operations', 'DEPARTMENT_HEAD_OPERATIONS', 'HQ',
    500000000.00, TRUE,  -- 500 triệu VND
    7, 'ACTIVE', '2021-08-01'
);

-- Display results
SELECT 'Sample data inserted successfully (including internal transaction users)' AS status;

SELECT 
    id,
    employee_number,
    full_name,
    role,
    department,
    branch_id,
    approval_limit,
    can_approve_transactions,
    reports_to_user_id,
    employment_status
FROM employee_profiles
ORDER BY id;

-- Display hierarchy
SELECT 
    e.employee_number AS 'Employee',
    e.full_name AS 'Name',
    e.role AS 'Role',
    e.approval_limit AS 'Approval Limit',
    m.employee_number AS 'Reports To',
    m.full_name AS 'Manager Name'
FROM employee_profiles e
LEFT JOIN employee_profiles m ON e.reports_to_user_id = m.id
ORDER BY e.id;

