-- Add priority to policies (run once). If column exists, ignore error.
USE hospital_policies;
ALTER TABLE policies ADD COLUMN priority INT DEFAULT 0 COMMENT 'Higher = evaluated first' AFTER conditions;
CREATE INDEX idx_priority ON policies (priority DESC);
