# Notification Service Database Scripts

This directory contains SQL scripts for setting up and managing the Notification Service database.

## Overview

The Notification Service uses a MySQL database (`banking_notifications`) to store notification records for both customers and employees in the hybrid RBAC/ABAC banking system.

## Database Structure

### Database: `banking_notifications`

### Tables:
1. **notifications** - All notification records (in-app and email)

## Scripts

### Setup Scripts (Run in Order)

1. **`01-create-database.sql`**
   - Creates the `banking_notifications` database
   - Sets character set to utf8mb4

2. **`02-create-tables.sql`**
   - Creates the `notifications` table
   - Defines columns, constraints, and basic indexes

3. **`03-create-indexes.sql`**
   - Creates additional indexes for performance
   - Composite indexes for common query patterns

4. **`04-seed-data.sql`**
   - Inserts sample notifications for testing
   - Various notification types and scenarios

### Utility Scripts

5. **`05-drop-all.sql`**
   - **⚠️ WARNING**: Drops all tables and data
   - Use only for cleaning up test environments

### Master Script

6. **`00-setup-complete.sql`**
   - Runs all setup scripts in order
   - One-command database setup

## Usage

### Option 1: Run Master Script (Recommended)

```bash
mysql -u root -p < 00-setup-complete.sql
```

### Option 2: Run Individual Scripts

```bash
mysql -u root -p < 01-create-database.sql
mysql -u root -p < 02-create-tables.sql
mysql -u root -p < 03-create-indexes.sql
mysql -u root -p < 04-seed-data.sql
```

## Notifications Table Schema

### Key Columns

- **id**: Primary key
- **user_id**: ID of user receiving notification
- **user_type**: CUSTOMER or EMPLOYEE
- **type**: TRANSACTION, ACCOUNT, SECURITY, SYSTEM, MARKETING
- **title**: Short notification title
- **message**: Notification message (TEXT)
- **email_subject**: Email subject line
- **email_body**: HTML email body
- **status**: PENDING, SENT, DELIVERED, FAILED
- **channel**: IN_APP, EMAIL, BOTH
- **read_at**: When notification was read
- **sent_at**: When in-app notification was sent
- **email_sent_at**: When email was sent
- **created_at**: Creation timestamp
- **updated_at**: Last update timestamp
- **metadata**: Additional JSON data

## Notification Types

1. **TRANSACTION**: Transaction-related notifications
2. **ACCOUNT**: Account management notifications
3. **SECURITY**: Security alerts
4. **SYSTEM**: System announcements
5. **MARKETING**: Promotional notifications

## Notification Channels

1. **IN_APP**: Only in-app notification (WebSocket)
2. **EMAIL**: Only email notification
3. **BOTH**: Both in-app and email

## Notification Status

1. **PENDING**: Created but not sent yet
2. **SENT**: Successfully sent
3. **DELIVERED**: Confirmed delivered (future enhancement)
4. **FAILED**: Failed to send

## Configuration

Update `application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/banking_notifications?createDatabaseIfNotExist=true
    username: root
    password: root
```

## Support

For issues:
- Check logs: `logs/notification-service.log`
- Review schema: `02-create-tables.sql`
- Test with sample data: `04-seed-data.sql`

