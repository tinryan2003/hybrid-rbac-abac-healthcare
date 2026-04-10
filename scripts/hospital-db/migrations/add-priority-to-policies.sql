-- Migration: add priority column to policies (if missing)
-- Run against MySQL in Docker: see scripts/README-MIGRATIONS.md or run:
--   docker exec -i hospital-mysql mysql -uroot -pS@l19092003 hospital_policies < scripts/hospital-db/migrations/add-priority-to-policies.sql

USE hospital_policies;

-- Add priority column if it does not exist (MySQL 8.0: no IF NOT EXISTS for ADD COLUMN, use procedure)
SET @dbname = DATABASE();
SET @tablename = 'policies';
SET @columnname = 'priority';
SET @prepared = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = @columnname);

SET @sql = IF(@prepared = 0,
  'ALTER TABLE policies ADD COLUMN priority INT DEFAULT 0 COMMENT ''Higher = evaluated first'' AFTER conditions;',
  'SELECT ''Column priority already exists.'' AS msg;');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Ensure index for priority (optional, for ORDER BY priority DESC)
SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND INDEX_NAME = 'idx_priority');
SET @sql2 = IF(@idx_exists = 0,
  'CREATE INDEX idx_priority ON policies (priority DESC);',
  'SELECT ''Index idx_priority already exists.'' AS msg;');
PREPARE stmt2 FROM @sql2;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;

SELECT 'Migration add-priority-to-policies completed.' AS result;
