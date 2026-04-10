# Dynamic Policies Guide - Role-Centric RBAC-A

This guide explains how to create and manage dynamic policies in the HyARBAC system using the "Policy as Data" pattern.

## 📋 Overview

The system implements **Role-centric RBAC-A** (Option 9 from Kuhn et al. 2010):
- **Roles** define maximum permissions (RBAC)
- **Attributes** constrain when/where those permissions apply (ABAC)
- **Policies** are stored in database and synced to OPA automatically

## 🔄 Architecture

```
Frontend (Policy Builder)
    ↓ (POST /api/policies)
Policy Service (Java)
    ↓ (Save to DB)
PostgreSQL Database
    ↓ (Auto Sync)
OPA Sync Service
    ↓ (PUT /v1/data/dynamic_policies/{id})
OPA Runtime
    ↓ (Evaluate with hospital_dynamic.rego)
Authorization Decision
```

## 📝 Policy Structure

### Basic Policy JSON

```json
{
  "tenantId": "hospital_001",
  "policyId": "nurse_working_hours",
  "policyName": "Nurse Working Hours Restriction",
  "description": "NURSE can only access during working hours",
  "effect": "Allow",
  "enabled": true,
  
  "subjects": {
    "roles": ["NURSE"]
  },
  
  "actions": ["read", "update"],
  
  "resources": {
    "object": "patient_record"
  },
  
  "conditions": {
    "working_hours_only": true,
    "same_department": true
  }
}
```

## 🎯 Creating Policies

### Example 1: Time-Based Restriction

**Requirement**: NURSE can only work 8:00 - 17:00

```bash
POST /api/policies
Content-Type: application/json

{
  "tenantId": "hospital_001",
  "policyId": "nurse_time_restriction",
  "policyName": "NURSE Working Hours",
  "effect": "Allow",
  "subjects": {"roles": ["NURSE"]},
  "actions": ["read", "update"],
  "resources": {"object": "patient_record"},
  "conditions": {
    "time_range": "08:00-17:00",
    "same_department": true,
    "same_hospital": true
  }
}
```

**What happens**:
1. Policy saved to DB
2. OpaSyncService automatically pushes to OPA
3. OPA evaluates using `hospital_dynamic.rego`
4. NURSE requests outside 8-17 are denied

### Example 2: Department Isolation

**Requirement**: DOCTOR can only access patients in their department

```json
{
  "tenantId": "hospital_001",
  "policyId": "doctor_department_isolation",
  "policyName": "Doctor Department Access",
  "effect": "Allow",
  "subjects": {"roles": ["DOCTOR"]},
  "actions": ["read", "update"],
  "resources": {"object": "patient_record"},
  "conditions": {
    "same_department": true,
    "same_hospital": true
  }
}
```

### Example 3: Position Level Requirement

**Requirement**: Only senior staff (level 3+) can approve

```json
{
  "tenantId": "hospital_001",
  "policyId": "senior_approval_only",
  "policyName": "Senior Staff Approval",
  "effect": "Allow",
  "subjects": {"roles": ["DOCTOR", "NURSE"]},
  "actions": ["approve"],
  "resources": {"object": "medical_procedure"},
  "conditions": {
    "min_position_level": 3
  }
}
```

### Example 4: Emergency Override

**Requirement**: Allow emergency access with extra logging

```json
{
  "tenantId": "hospital_001",
  "policyId": "emergency_override",
  "policyName": "Emergency Access Override",
  "effect": "Allow",
  "subjects": {"roles": ["DOCTOR", "NURSE"]},
  "actions": ["read", "update"],
  "resources": {"object": "patient_record"},
  "conditions": {
    "allow_emergency_override": true
  }
}
```

## 🔧 Supported Constraint Types

| Constraint | Description | Example |
|------------|-------------|---------|
| `time_range` | Custom time window | `"08:00-17:00"` |
| `working_hours_only` | Standard business hours (8-17) | `true` |
| `same_department` | User and resource same dept | `true` |
| `same_hospital` | User and resource same hospital | `true` |
| `min_position_level` | Minimum seniority level | `3` |
| `allow_emergency_override` | Allow in emergency | `true` |
| `allowed_ip_ranges` | IP whitelist | `["10.0.0.0/8"]` |

## 🧪 Testing Policies

### 1. Create Policy via API

```bash
curl -X POST http://localhost:8101/api/policies \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": "hospital_001",
    "policyId": "test_policy",
    "policyName": "Test Policy",
    "effect": "Allow",
    "subjects": {"roles": ["DOCTOR"]},
    "actions": ["read"],
    "resources": {"object": "patient_record"},
    "conditions": {"same_department": true}
  }'
```

### 2. Check OPA Sync Status

```bash
curl http://localhost:8101/api/policies/opa-sync/status
```

Response:
```json
{
  "sync_enabled": true,
  "opa_url": "http://localhost:8181",
  "opa_healthy": true,
  "total_policies": 10,
  "enabled_policies": 8
}
```

### 3. Verify in OPA

```bash
curl http://localhost:8181/v1/data/dynamic_policies/test_policy
```

### 4. Test Authorization

```bash
curl -X POST http://localhost:8181/v1/data/hospital/authz/allow \
  -H "Content-Type: application/json" \
  -d '{
    "input": {
      "user": {
        "role": "DOCTOR",
        "department_id": "dept_001",
        "hospital_id": "hosp_001"
      },
      "resource": {
        "object": "patient_record",
        "action": "read",
        "department_id": "dept_001",
        "hospital_id": "hosp_001"
      },
      "context": {
        "hour": 10
      }
    }
  }'
```

## 🔄 Updating Policies

When you update a policy:
1. **Update in DB** (PUT /api/policies/:id)
2. **Auto-synced to OPA** (no restart needed)
3. **Immediately effective** for new requests

```bash
curl -X PUT http://localhost:8101/api/policies/1 \
  -H "Content-Type: application/json" \
  -d '{...updated policy...}'
```

## 🗑️ Deleting Policies

When you delete a policy:
1. **Deleted from DB** (DELETE /api/policies/:id)
2. **Auto-removed from OPA**
3. **Immediately stops applying**

```bash
curl -X DELETE http://localhost:8101/api/policies/1
```

## 🔍 Troubleshooting

### Policy not working?

1. **Check if enabled**:
   ```sql
   SELECT policy_id, enabled FROM policies WHERE policy_id = 'your_policy';
   ```

2. **Check OPA sync status**:
   ```bash
   curl http://localhost:8101/api/policies/opa-sync/status
   ```

3. **Check OPA has the data**:
   ```bash
   curl http://localhost:8181/v1/data/dynamic_policies
   ```

4. **Trigger manual resync**:
   ```bash
   curl -X POST http://localhost:8101/api/policies/opa-sync/resync
   ```

### OPA not reachable?

Check logs:
```
❌ OPA server is not reachable at http://localhost:8181
```

Solution:
1. Start OPA: `docker-compose up -d opa`
2. Or run standalone: `opa run --server --log-level debug`

## 📊 Best Practices

### 1. Use Clear IDs
```json
"policyId": "doctor_dept_access_cardiology"
```

### 2. Add Descriptions
```json
"description": "Restricts doctors to only access patients in their assigned department for read/update operations"
```

### 4. Test Before Enabling
1. Create with `"enabled": false`
2. Test in staging
3. Set `"enabled": true` in production

### 5. Use Tags for Organization
```json
"tags": ["department-isolation", "hipaa-compliance", "cardiology"]
```

## 🎓 Role-Centric Pattern

This implementation follows **Role-centric RBAC-A**:

```
Permission = (Role Permissions) ∩ (Constraint Permissions)

Example:
- DOCTOR role grants: [read, write, approve] on patient_records
- Policy constraint: only same_department + working_hours
- Final permission: Can do [read, write, approve] BUT only in same dept during work hours
```

**Key principle**: Constraints can only **reduce** permissions, never expand them.

## 📚 References

- Kuhn, D. R., Coyne, E. J., & Weil, T. R. (2010). Adding Attributes to Role-Based Access Control. IEEE Computer, 43(6), 79-81.
- Pattern: Role-centric RBAC-A (Option 9: U → R → A₁, ..., Aₙ → perm)
