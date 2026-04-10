-- ============================================================================
-- Migration Script: Simplify Job Titles for Internal Transactions
-- ============================================================================
-- Description: Updates old job titles to simplified versions for thesis demo
-- Author: VGU Banking System
-- Date: 2026-01-04
-- ============================================================================
-- This migration updates job titles that were removed during simplification:
-- - HR_DIRECTOR, HR_COORDINATOR, HR_ASSISTANT -> HR_MANAGER or HR_SPECIALIST
-- - PAYROLL_COORDINATOR, PAYROLL_ANALYST -> PAYROLL_MANAGER or PAYROLL_SPECIALIST
-- - FINANCIAL_ANALYST, FINANCE_COORDINATOR, TREASURY_MANAGER, TREASURY_ANALYST -> FINANCE_DIRECTOR or FINANCE_SPECIALIST
-- - ACCOUNTING_SUPERVISOR, SENIOR_ACCOUNTANT, ACCOUNTING_CLERK, ACCOUNTS_PAYABLE_SPECIALIST, ACCOUNTS_RECEIVABLE_SPECIALIST -> ACCOUNTING_DIRECTOR or ACCOUNTANT
-- - DEPARTMENT_HEAD_IT, DEPARTMENT_HEAD_MARKETING, DEPARTMENT_HEAD_SALES -> DEPARTMENT_HEAD_FINANCE or DEPARTMENT_HEAD_OPERATIONS
-- ============================================================================

USE banking_users;

-- Temporarily disable safe update mode for this migration
SET SQL_SAFE_UPDATES = 0;

-- Update HR job titles (Director/Coordinator/Assistant -> Manager or Specialist)
UPDATE employee_profiles 
SET job_title = 'HR_MANAGER'
WHERE job_title IN ('HR_DIRECTOR', 'HR_COORDINATOR');

UPDATE employee_profiles 
SET job_title = 'HR_SPECIALIST'
WHERE job_title = 'HR_ASSISTANT';

-- Update Payroll job titles (Coordinator/Analyst -> Manager or Specialist)
UPDATE employee_profiles 
SET job_title = 'PAYROLL_MANAGER'
WHERE job_title = 'PAYROLL_COORDINATOR';

UPDATE employee_profiles 
SET job_title = 'PAYROLL_SPECIALIST'
WHERE job_title = 'PAYROLL_ANALYST';

-- Update Finance job titles (Analyst/Coordinator/Treasury -> Director or Specialist)
UPDATE employee_profiles 
SET job_title = 'FINANCE_DIRECTOR'
WHERE job_title IN ('FINANCIAL_ANALYST', 'TREASURY_MANAGER');

UPDATE employee_profiles 
SET job_title = 'FINANCE_SPECIALIST'
WHERE job_title IN ('FINANCE_COORDINATOR', 'TREASURY_ANALYST');

-- Update Accounting job titles (Supervisor/Senior/Clerk/Specialists -> Director or Accountant)
UPDATE employee_profiles 
SET job_title = 'ACCOUNTING_DIRECTOR'
WHERE job_title IN ('ACCOUNTING_SUPERVISOR', 'SENIOR_ACCOUNTANT');

UPDATE employee_profiles 
SET job_title = 'ACCOUNTANT'
WHERE job_title IN ('ACCOUNTING_CLERK', 'ACCOUNTS_PAYABLE_SPECIALIST', 'ACCOUNTS_RECEIVABLE_SPECIALIST');

-- Update Department Head job titles (IT/Marketing/Sales -> Finance or Operations)
UPDATE employee_profiles 
SET job_title = 'DEPARTMENT_HEAD_FINANCE'
WHERE job_title = 'DEPARTMENT_HEAD_IT';

UPDATE employee_profiles 
SET job_title = 'DEPARTMENT_HEAD_OPERATIONS'
WHERE job_title IN ('DEPARTMENT_HEAD_MARKETING', 'DEPARTMENT_HEAD_SALES');

-- Re-enable safe update mode
SET SQL_SAFE_UPDATES = 1;

-- Display summary
SELECT 'Job titles simplified successfully' AS status;

-- Show updated job titles
SELECT 
    employee_number,
    full_name,
    job_title,
    role,
    department
FROM employee_profiles
WHERE job_title IN (
    'HR_MANAGER', 'HR_SPECIALIST',
    'PAYROLL_MANAGER', 'PAYROLL_SPECIALIST',
    'FINANCE_DIRECTOR', 'FINANCE_SPECIALIST',
    'ACCOUNTING_DIRECTOR', 'ACCOUNTANT',
    'DEPARTMENT_HEAD_FINANCE', 'DEPARTMENT_HEAD_OPERATIONS'
)
ORDER BY job_title, employee_number;
