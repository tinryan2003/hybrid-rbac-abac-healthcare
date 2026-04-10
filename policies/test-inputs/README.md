# OPA Test Inputs

Sample input JSON files for testing OPA hospital.authz policies.

## Usage

### Test with OPA CLI

```bash
# Install OPA: https://www.openpolicyagent.org/docs/latest/#running-opa

# Test a specific input
opa eval -d policies/hospital.rego \
    -i policies/test-inputs/doctor-same-dept.json \
    "data.hospital.authz.allow"

# Expected output:
# {
#   "result": [
#     {
#       "expressions": [
#         {
#           "value": {
#             "allowed": true,
#             "reason": "Access granted: same department access for doctor",
#             "obligations": []
#           }
#         }
#       ]
#     }
#   ]
# }
```

### Test with OPA Server

```bash
# Start OPA server
opa run --server --log-level=debug policies/

# Test via HTTP
curl -X POST http://localhost:8181/v1/data/hospital/authz/allow \
    -H "Content-Type: application/json" \
    -d @policies/test-inputs/doctor-same-dept.json

# Expected: {"result": {"allowed": true, "reason": "...", "obligations": []}}
```

### Test with Docker OPA

```bash
# OPA in docker-compose
curl -X POST http://localhost:8181/v1/data/hospital/authz/allow \
    -H "Content-Type: application/json" \
    -d @policies/test-inputs/doctor-same-dept.json
```

## Test Files

| File | Scenario | Expected Result |
|------|----------|----------------|
| `doctor-same-dept.json` | DOCTOR reading patient in same department | ✅ Allow |
| `doctor-cross-dept.json` | DOCTOR reading patient in different department | ❌ Deny (not your department) |
| `nurse-working-hours.json` | NURSE at 10:00 reading patient vitals | ✅ Allow |
| `nurse-outside-hours.json` | NURSE at 18:00 reading patient vitals | ❌ Deny (role not active) |
| `primary-doctor-cross-dept.json` | PRIMARY_DOCTOR reading patient in different dept | ✅ Allow (cross-dept read) |
| `emergency-access.json` | DOCTOR with emergency flag accessing different dept | ✅ Allow (emergency override) |
| `patient-own-data.json` | PATIENT viewing own record | ✅ Allow (owns resource) |

## Modifying Test Inputs

To create new test cases:

1. Copy an existing JSON file
2. Modify the attributes:
   - `user.role`: DOCTOR, NURSE, PATIENT, PRIMARY_DOCTOR, etc.
   - `user.department_id`: "1", "2", "3", etc.
   - `resource.object`: patient_record, appointment, prescription, lab_order
   - `resource.action`: read, write, create, delete
   - `resource.department_id`: "1", "2", etc. (for same-department checks)
   - `context.hour`: 0-23 (for working hours checks)
   - `context.emergency`: true/false (for emergency override)
3. Test with OPA CLI or server
4. Add to automated test script (`scripts/test-opa-policies.sh`)

## Debugging Failed Tests

If a test returns unexpected result:

### 1. Check OPA Logs

```bash
# If using Docker
docker logs hospital-opa

# If using OPA CLI
opa run --server --log-level=debug policies/
```

### 2. Use OPA Trace

```bash
curl -X POST http://localhost:8181/v1/data/hospital/authz/allow?explain=full \
    -H "Content-Type: application/json" \
    -d @policies/test-inputs/doctor-cross-dept.json | jq '.'

# Returns detailed evaluation trace showing which rules matched
```

### 3. Check Input Values

```bash
# Pretty-print the input
cat policies/test-inputs/doctor-cross-dept.json | jq '.'

# Verify:
# - user.department_id != resource.department_id (should trigger deny)
# - user.role matches expected role
# - resource.object and action are valid
```

### 4. Interactive Testing with OPA REPL

```bash
opa run policies/hospital.rego

# In REPL:
> import input
> input := <paste JSON>
> data.hospital.authz.allow

# Or test individual helpers:
> data.hospital.authz.is_work_hours with input as {"context": {"hour": 10}}
> data.hospital.authz.same_department with input as {"user": {"department_id": "1"}, "resource": {"department_id": "1"}}
```

## Integration with Automated Tests

These test inputs are used by:

- `scripts/test-opa-policies.sh` - Automated OPA unit tests
- `scripts/test-authorization-service.sh` - Integration tests
- CI/CD pipeline (future) - Pre-deployment policy validation

## References

- OPA Documentation: https://www.openpolicyagent.org/docs/latest/
- Rego Language: https://www.openpolicyagent.org/docs/latest/policy-language/
- HyARBAC Model: [../../docs/HYARBAC_MODEL.md](../../docs/HYARBAC_MODEL.md)
