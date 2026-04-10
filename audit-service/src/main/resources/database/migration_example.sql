-- Example Migration Script Template
-- Use this as a template for future schema changes
-- Naming convention: migration_YYYYMMDD_description.sql

-- Example: Add a new column
-- ALTER TABLE audit_logs 
-- ADD COLUMN new_field VARCHAR(100) NULL COMMENT 'New field description'
-- AFTER existing_field;

-- Example: Add a new index
-- CREATE INDEX idx_new_field ON audit_logs(new_field);

-- Example: Modify a column
-- ALTER TABLE audit_logs 
-- MODIFY COLUMN description TEXT COMMENT 'Updated description';

-- Example: Drop a column (be careful!)
-- ALTER TABLE audit_logs DROP COLUMN old_field;

-- Always backup before running migrations!
-- Always test on development database first!
