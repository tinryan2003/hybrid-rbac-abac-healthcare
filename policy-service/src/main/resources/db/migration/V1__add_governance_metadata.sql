-- Migration script to add governance metadata columns to policies table
-- Bachelor Thesis Enhancement: Policy Accountability & Tracking
-- Date: February 25, 2026

-- Add governance metadata columns
ALTER TABLE policies 
  ADD COLUMN justification TEXT NULL COMMENT 'Reason for creating/updating this policy',
  ADD COLUMN ticket_id VARCHAR(100) NULL COMMENT 'Reference ticket (e.g., JIRA-123, INCIDENT-456)',
  ADD COLUMN business_owner VARCHAR(255) NULL COMMENT 'Person responsible for this policy';

-- Add indexes for querying
CREATE INDEX idx_policies_ticket_id ON policies(ticket_id);
CREATE INDEX idx_policies_business_owner ON policies(business_owner);

-- Verify columns exist
DESCRIBE policies;
