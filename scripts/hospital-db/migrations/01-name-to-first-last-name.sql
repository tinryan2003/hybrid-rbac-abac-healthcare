-- =====================================================
-- Migration: Replace [name] with [first_name], [last_name]
-- For existing database created with old schema (name column).
-- Run this on database hospital_users.
--
-- If you get "missing column first_name in table [doctors]" or "[nurses]",
-- run the corresponding section below for that table.
-- =====================================================

USE hospital_users;

-- -----------------------------------------------
-- 1. ADMINS
-- -----------------------------------------------
ALTER TABLE admins
  ADD COLUMN first_name VARCHAR(50) NULL AFTER user_id,
  ADD COLUMN last_name VARCHAR(50) NULL AFTER first_name;

UPDATE admins
SET
  first_name = TRIM(SUBSTRING_INDEX(name, ' ', 1)),
  last_name = TRIM(IF(LOCATE(' ', name) > 0, SUBSTRING(name, LOCATE(' ', name) + 1), ''));

UPDATE admins SET last_name = '' WHERE last_name IS NULL;

ALTER TABLE admins
  MODIFY first_name VARCHAR(50) NOT NULL,
  MODIFY last_name VARCHAR(50) NOT NULL,
  DROP COLUMN name;

-- -----------------------------------------------
-- 2. DOCTORS
-- -----------------------------------------------
ALTER TABLE doctors
  ADD COLUMN first_name VARCHAR(50) NULL AFTER user_id,
  ADD COLUMN last_name VARCHAR(50) NULL AFTER first_name;

UPDATE doctors
SET
  first_name = TRIM(SUBSTRING_INDEX(name, ' ', 1)),
  last_name = TRIM(IF(LOCATE(' ', name) > 0, SUBSTRING(name, LOCATE(' ', name) + 1), ''));

UPDATE doctors SET last_name = '' WHERE last_name IS NULL;

ALTER TABLE doctors
  MODIFY first_name VARCHAR(50) NOT NULL,
  MODIFY last_name VARCHAR(50) NOT NULL,
  DROP COLUMN name;

-- -----------------------------------------------
-- 3. NURSES
-- -----------------------------------------------
ALTER TABLE nurses
  ADD COLUMN first_name VARCHAR(50) NULL AFTER user_id,
  ADD COLUMN last_name VARCHAR(50) NULL AFTER first_name;

UPDATE nurses
SET
  first_name = TRIM(SUBSTRING_INDEX(name, ' ', 1)),
  last_name = TRIM(IF(LOCATE(' ', name) > 0, SUBSTRING(name, LOCATE(' ', name) + 1), ''));

UPDATE nurses SET last_name = '' WHERE last_name IS NULL;

ALTER TABLE nurses
  MODIFY first_name VARCHAR(50) NOT NULL,
  MODIFY last_name VARCHAR(50) NOT NULL,
  DROP COLUMN name;

SELECT 'Migration name -> first_name, last_name (admins, doctors, nurses) completed.' AS status;
