# Policy Service - Governance Features

## Overview
Policy Service now includes governance mechanisms to prevent authorization conflicts and track policy accountability.

## Features

### 1. Conflict-on-Save Validation ✅

Automatically validates policies for conflicts **before** saving to database.

**Configuration** (`application.yml`):
```yaml
policy:
  conflict-on-save: strict  # strict|warn|off
  reject-wildcard: true     # Blocks resource="*" AND action=["*"]
  require-justification: false  # Enforces justification field
```

**Modes**:
- `strict`: Rejects policy creation if `AUTH_CONFLICT` detected → 409 Conflict
- `warn`: Allows policy creation but logs warning if conflict detected
- `off`: No validation (backward compatible mode)

**Example Request** (Will Conflict):
```bash
# Existing policy: Allow DOCTOR read patient
# This will FAIL (409 Conflict):
curl -X POST http://localhost:8101/api/policies \
  -H "Content-Type: application/json" \
  -d '{
    "policyId": "deny-doctor-read",
    "effect": "Deny",
    "subjects": {"roles": ["DOCTOR"]},
    "actions": ["read"],
    "resources": {"type": "patient"}
  }'
```

**Response** (409 Conflict):
```json
{
  "statusCode": 409,
  "error": "Policy would introduce authorization conflicts. Resolve AUTH_CONFLICT before saving.",
  "conflictReport": {
    "rulesAnalyzed": 2,
    "conflictCount": 1,
    "hasAuthConflicts": true,
    "hasRedundancy": false,
    "conflicts": [{
      "policyId1": "baseline-doctor-read",
      "policyId2": "deny-doctor-read",
      "reason": "AUTH_CONFLICT: Allow vs Deny on overlapping scope",
      "conflictType": "AUTH_CONFLICT",
      "overlapActions": ["read"],
      "resourceType": "patient",
      "witnessRequest": {
        "role": "DOCTOR",
        "action": "read",
        "resourceType": "patient"
      }
    }],
    "durationMs": 12
  }
}
```

---

### 2. Wildcard Guardrails ✅

Prevents overly permissive wildcard policies.

**Validation Rules**:
- ❌ `resource: "*"` AND `action: ["*"]` → **Rejected** (too permissive)
- ✅ `resource: "patient"` AND `action: ["*"]` → **Allowed** (specific resource)
- ✅ `resource: "*"` AND `action: ["read"]` → **Allowed** (specific action)

**Configuration**:
```yaml
policy:
  reject-wildcard: true  # Blocks dangerous wildcards
```

**Example** (Will Fail):
```bash
curl -X POST http://localhost:8101/api/policies \
  -H "Content-Type: application/json" \
  -d '{
    "policyId": "admin-all-access",
    "rules": [{
      "effect": "Allow",
      "subjects": {"roles": ["ADMIN"]},
      "actions": ["*"],
      "resources": {"type": "*"}
    }]
  }'
```

**Response** (400 Bad Request):
```json
{
  "statusCode": 400,
  "error": "Wildcard validation failed: resource '*' AND action '*' not allowed (too permissive)"
}
```

---

### 3. Policy Metadata Tracking ✅

Track governance metadata for accountability.

**New Fields**:
- `justification` (TEXT): Why this policy was created
- `ticketId` (VARCHAR 100): Reference to JIRA/incident ticket
- `businessOwner` (VARCHAR 255): Responsible person

**Database Schema**:
```sql
ALTER TABLE policies 
  ADD COLUMN justification TEXT NULL,
  ADD COLUMN ticket_id VARCHAR(100) NULL,
  ADD COLUMN business_owner VARCHAR(255) NULL;

CREATE INDEX idx_policies_ticket_id ON policies(ticket_id);
CREATE INDEX idx_policies_business_owner ON policies(business_owner);
```

**Example Request** (With Metadata):
```bash
curl -X POST http://localhost:8101/api/policies \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{
    "policyId": "nurse-view-appointments",
    "policyName": "Nurses Can View Appointments",
    "effect": "Allow",
    "subjects": {"roles": ["NURSE"]},
    "actions": ["read"],
    "resources": {"type": "appointment"},
    "justification": "Nurses need appointment access for scheduling tasks",
    "ticketId": "HOSP-2026-123",
    "businessOwner": "Dr. Smith, Head Nurse Department",
    "enabled": true
  }'
```

**Response** (201 Created):
```json
{
  "id": 42,
  "policyId": "nurse-view-appointments",
  "policyName": "Nurses Can View Appointments",
  "effect": "Allow",
  "subjects": {"roles": ["NURSE"]},
  "actions": ["read"],
  "resources": {"type": "appointment"},
  "justification": "Nurses need appointment access for scheduling tasks",
  "ticketId": "HOSP-2026-123",
  "businessOwner": "Dr. Smith, Head Nurse Department",
  "enabled": true,
  "createdAt": "2026-02-25T14:30:00Z",
  "updatedAt": "2026-02-25T14:30:00Z"
}
```

---

## API Examples

### Create Policy (No Conflicts)
```bash
curl -X POST http://localhost:8101/api/policies \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": "DEFAULT",
    "policyId": "doctor-read-patient",
    "policyName": "Doctors Can Read Patient Records",
    "effect": "Allow",
    "subjects": {"roles": ["DOCTOR"]},
    "actions": ["read"],
    "resources": {"type": "patient"},
    "justification": "Standard permission for medical staff",
    "ticketId": "SETUP-001",
    "businessOwner": "Medical Director",
    "enabled": true
  }'
```
**Response**: `201 Created`

---

### Create Policy (Conflict Detected)
```bash
curl -X POST http://localhost:8101/api/policies \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": "DEFAULT",
    "policyId": "doctor-deny-patient",
    "policyName": "Deny Doctors Patient Access",
    "effect": "Deny",
    "subjects": {"roles": ["DOCTOR"]},
    "actions": ["read"],
    "resources": {"type": "patient"},
    "enabled": true
  }'
```
**Response**: `409 Conflict` with detailed conflict report

---

### Update Policy (With Metadata)
```bash
curl -X PUT http://localhost:8101/api/policies/42 \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": "DEFAULT",
    "policyId": "nurse-view-appointments",
    "policyName": "Nurses Can Manage Appointments",
    "effect": "Allow",
    "subjects": {"roles": ["NURSE"]},
    "actions": ["read", "update"],
    "resources": {"type": "appointment"},
    "justification": "Extended to allow appointment updates per security review",
    "ticketId": "HOSP-2026-456",
    "businessOwner": "Jane Doe, Security Officer",
    "enabled": true
  }'
```
**Response**: `200 OK` with updated metadata

---

### Query Policies by Metadata
```sql
-- Find all policies by ticket
SELECT * FROM policies WHERE ticket_id = 'HOSP-2026-123';

-- Find all policies by business owner
SELECT * FROM policies WHERE business_owner LIKE '%Dr. Smith%';

-- Find policies needing justification
SELECT policy_id, policy_name FROM policies 
WHERE justification IS NULL OR justification = '';
```

---

## Configuration Reference

### application.yml
```yaml
policy:
  # Conflict validation mode
  conflict-on-save: strict  # strict|warn|off
  
  # Wildcard guardrails
  reject-wildcard: true     # Blocks resource="*" AND action=["*"]
  
  # Governance tracking
  require-justification: false  # When true, rejects policies without justification
```

### Mode Reference
| Mode | Behavior | Use Case |
|------|----------|----------|
| `strict` | Rejects on AUTH_CONFLICT (409) | Production environments |
| `warn` | Logs warning, allows save | Development/testing |
| `off` | No validation | Legacy compatibility |

---

## Error Codes

| Code | Error | Cause | Solution |
|------|-------|-------|----------|
| 409 | Policy Conflict | AUTH_CONFLICT detected | Resolve conflicting policy or change scope |
| 400 | Wildcard Rejected | Unsafe wildcards | Make policy more specific |
| 400 | Missing Justification | `require-justification: true` | Add justification field |
| 500 | Database Error | Migration not applied | Run V1__add_governance_metadata.sql |

---

## Testing

### Conflict-on-Save Test
```bash
# Create baseline
curl -X POST http://localhost:8101/api/policies -d '{"policyId": "allow-doctor", "effect": "Allow", ...}'

# Try conflict (should FAIL with 409)
curl -X POST http://localhost:8101/api/policies -d '{"policyId": "deny-doctor", "effect": "Deny", ...}'
```

### Wildcard Guardrails Test
```bash
# Should FAIL with 400
curl -X POST http://localhost:8101/api/policies -d '{"actions": ["*"], "resources": {"type": "*"}, ...}'
```

### Metadata Tracking Test
```bash
# Create with metadata
curl -X POST http://localhost:8101/api/policies -d '{
  "justification": "Test reason",
  "ticketId": "TEST-001",
  "businessOwner": "Test Owner",
  ...
}'

# Verify in database
mysql> SELECT justification, ticket_id, business_owner FROM policies WHERE policy_id = 'test';
```

---

## Migration Guide

### Apply Database Migration
```bash
# MySQL CLI
mysql -u root -p hospital_policies < src/main/resources/db/migration/V1__add_governance_metadata.sql

# Or MySQL Workbench
# Open V1__add_governance_metadata.sql → Execute
```

### Verify Migration
```sql
DESCRIBE policies;
-- Should show: justification, ticket_id, business_owner columns
```

---

## Architecture

```
PolicyController
    ↓
PolicyCrudService.create()
    ↓
[1] Normalize rules
    ↓
[2] Conflict Detection (if conflict-on-save != off)
    ↓
[3] Wildcard Validation (if reject-wildcard = true)
    ↓
[4] Justification Check (if require-justification = true)
    ↓
[5] Save to Database (with metadata)
    ↓
[6] Sync to OPA
    ↓
[7] Publish Audit Event
    ↓
201 Created (or 409/400 if validation failed)
```

---

## Performance

- **Conflict Detection**: 10-20ms for 50-100 policies
- **Wildcard Validation**: <1ms (rule-level check)
- **Database Impact**: 3 new columns + 2 indexes (minimal overhead)
- **Authorization Runtime**: 0ms (validation happens at create-time, not request-time)

---

## Audit Logging

All policy operations include governance metadata in audit events:

```json
{
  "eventType": "POLICY_CREATED",
  "timestamp": "2026-02-25T14:30:00Z",
  "userId": "admin@hospital.com",
  "policyId": "nurse-view-appointments",
  "metadata": {
    "justification": "Nurses need appointment access for scheduling",
    "ticketId": "HOSP-2026-123",
    "businessOwner": "Dr. Smith"
  }
}
```

---

## Troubleshooting

### Issue: 409 Conflict but no conflicts exist
**Solution**: Check if policies have same `tenantId` and both are `enabled: true`

### Issue: Wildcard validation not working
**Solution**: Verify `reject-wildcard: true` in application.yml

### Issue: Column 'justification' not found
**Solution**: Apply database migration script

### Issue: Metadata not saved
**Solution**: Check `PolicyCrudService.toEntity()` and `update()` methods map metadata fields

---

## Future Enhancements

Potential improvements for Master-level work:
- Break-glass TTL (temporal elevated access)
- Separation of Duty (multi-party authorization)
- Policy approval workflows
- Chinese Wall patterns
- Automatic conflict resolution suggestions

---

**Version**: 1.0  
**Last Updated**: February 25, 2026  
**Implementation Status**: ✅ Complete - Ready for Testing
