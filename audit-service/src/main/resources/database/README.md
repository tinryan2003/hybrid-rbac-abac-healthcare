# Audit Service Database Scripts

This directory contains SQL scripts for setting up the audit service database schema.

## Files

- **schema.sql** - Main database schema creation script
  - Creates the `audit_logs` table
  - Creates indexes for performance
  - Creates optional views for reporting

## Database Requirements

- **Database**: MySQL 8.0 or higher
- **Database Name**: `bank_hybrid` (shared with other services)
- **Engine**: InnoDB
- **Charset**: utf8mb4

## Setup Instructions

### Option 1: Manual Setup

1. Ensure MySQL is running and the `bank_hybrid` database exists:
   ```sql
   CREATE DATABASE IF NOT EXISTS bank_hybrid;
   USE bank_hybrid;
   ```

2. Run the schema script:
   ```bash
   mysql -u root -p bank_hybrid < src/main/resources/database/schema.sql
   ```

   Or from MySQL client:
   ```sql
   SOURCE src/main/resources/database/schema.sql;
   ```

### Option 2: Automatic Setup (JPA)

If you're using JPA with `ddl-auto: update` in `application.yml`, the table will be created automatically when the service starts. However, the manual script provides:
- Better control over indexes
- Optional views for reporting
- Documentation of the schema

## Table Structure

### audit_logs

The main table storing all audit trail records.

**Key Fields:**
- `id` - Primary key (auto-increment)
- `event_type` - Type of event (ACCOUNT_APPROVED, TRANSACTION_CREATED, etc.)
- `severity` - Severity level (LOW, MEDIUM, HIGH, CRITICAL)
- `user_id`, `username`, `user_role` - User information
- `resource_type`, `resource_id` - Resource information
- `action` - Action performed
- `success` - Success/failure status
- `before_state`, `after_state` - JSON state tracking
- `metadata` - Additional JSON data
- `correlation_id` - For tracing related events
- `timestamp` - When the event occurred

**Indexes:**
- `idx_event_type` - For filtering by event type
- `idx_user_id` - For filtering by user
- `idx_resource_type`, `idx_resource_id` - For filtering by resource
- `idx_timestamp` - For date range queries
- `idx_severity` - For filtering by severity
- `idx_success` - For filtering failed actions
- `idx_correlation_id` - For tracing related events
- Composite indexes for common query patterns

## Views

The schema includes three optional views:

1. **vw_high_severity_audit_logs** - Quick access to HIGH/CRITICAL events
2. **vw_failed_audit_logs** - Quick access to failed actions
3. **vw_user_audit_summary** - User activity summary for reporting

## Data Retention

**Important**: Audit logs can grow very large over time. Consider implementing:

1. **Data Retention Policy**: Archive or delete old logs (e.g., older than 7 years)
2. **Partitioning**: Partition by date for better performance
3. **Archiving**: Move old logs to archive tables or separate database

Example retention script:
```sql
-- Delete logs older than 7 years (adjust as needed)
DELETE FROM audit_logs 
WHERE timestamp < DATE_SUB(NOW(), INTERVAL 7 YEAR);
```

## Performance Considerations

1. **Indexes**: All frequently queried fields are indexed
2. **JSON Columns**: MySQL 8.0+ supports JSON indexing if needed
3. **Partitioning**: Consider partitioning by date for very large datasets
4. **Archiving**: Archive old data to maintain query performance

## Backup Recommendations

Audit logs are critical for compliance. Ensure:
- Regular backups (daily recommended)
- Long-term retention (7+ years for banking)
- Secure storage of backups
- Test restore procedures

## Migration Notes

If you need to modify the schema later:

1. Create a migration script in this directory
2. Test on a development database first
3. Backup production data before applying
4. Use transactions for safety

Example migration naming: `migration_YYYYMMDD_description.sql`

## Firebase? No!

**You do NOT need Firebase.** This service uses:
- **MySQL** - For relational data storage (audit logs)
- **RabbitMQ** - For event messaging
- **Keycloak** - For authentication/authorization

Firebase is a NoSQL cloud database service and is not part of this architecture.

## Verification

After running the schema script, verify the table was created:

```sql
-- Check table exists
SHOW TABLES LIKE 'audit_logs';

-- Check table structure
DESCRIBE audit_logs;

-- Check indexes
SHOW INDEXES FROM audit_logs;

-- Check views
SHOW FULL TABLES WHERE Table_type = 'VIEW';
```

## Sample Queries

```sql
-- Get recent audit logs
SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 10;

-- Get logs for a specific user
SELECT * FROM audit_logs WHERE user_id = 1 ORDER BY timestamp DESC;

-- Get failed actions
SELECT * FROM vw_failed_audit_logs LIMIT 20;

-- Get high severity events
SELECT * FROM vw_high_severity_audit_logs LIMIT 20;

-- Get user summary
SELECT * FROM vw_user_audit_summary WHERE user_id = 1;
```
