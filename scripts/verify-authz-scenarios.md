# Verify authorization scenarios (reproducibility)

This document describes how to reproduce the scenario-based correctness validation (Section 4.7.1 of the thesis). You need the gateway, authorization service, OPA, and Keycloak running (e.g. via Docker Compose or local startup).

## Prerequisites

- Gateway base URL, e.g. `GATEWAY_URL=http://localhost:8089` (or 8083, depending on your setup).
- A JWT for each role: obtain from Keycloak (e.g. login via UI or use the Keycloak token endpoint with client credentials or resource owner password grant).

## Example commands

Replace `$GATEWAY_URL` and `$JWT_DOCTOR`, `$JWT_RECEPTIONIST`, etc. with your values.

### Scenario 5: Receptionist reads patient record → 403 Forbidden

Receptionist has no baseline permission for `patient_record / read`. Expect **403** and body containing `"Access denied"` and `"RECEPTIONIST"`.

```bash
curl -s -w "\nHTTP_STATUS:%{http_code}\n" \
  -H "Authorization: Bearer $JWT_RECEPTIONIST" \
  "$GATEWAY_URL/api/patients/1"
```

Expected: `HTTP_STATUS:403` and JSON like `{"status":403,"error":"Forbidden","message":"Access denied: role RECEPTIONIST not allowed for patient_record read","path":"/api/patients/1"}`.

### Scenario 6: No JWT → 401 Unauthorized

Send the same request without the `Authorization` header. Expect **401**.

```bash
curl -s -w "\nHTTP_STATUS:%{http_code}\n" \
  "$GATEWAY_URL/api/patients/1"
```

Expected: `HTTP_STATUS:401` and JSON like `{"status":401,"error":"Unauthorized","message":"Authentication required","path":"/api/patients/1"}`.

### Scenario 1: Doctor reads patient record (same department) → 200 Permit

With a valid doctor JWT and a patient in the same department (or assigned to the doctor), expect **200** and the patient payload from the patient service.

```bash
curl -s -w "\nHTTP_STATUS:%{http_code}\n" \
  -H "Authorization: Bearer $JWT_DOCTOR" \
  "$GATEWAY_URL/api/patients/1"
```

Expected: `HTTP_STATUS:200` and JSON body from the patient service.

## Checking audit logs

After running a scenario, query the audit API (with a user that has permission to read audit logs) to confirm an entry for the same request:

```bash
curl -s -H "Authorization: Bearer $JWT_ADMIN" \
  "$GATEWAY_URL/api/audit?subject=<user_id>&from=...&to=..."
```

Look for an entry with the expected `allowed` value and matching resource/action.

## Summary

| Scenario | Role / Auth   | Request              | Expected status | Evidence                    |
|----------|----------------|----------------------|-----------------|-----------------------------|
| 5        | Receptionist   | GET /api/patients/1  | 403             | Response body + audit entry |
| 6        | No JWT         | GET /api/patients/1  | 401             | Response body               |
| 1        | Doctor         | GET /api/patients/1  | 200             | Response body + audit entry |

Running these and recording the HTTP status and response body provides the same evidence used in the thesis table and listings (Listings 4.x and 4.x).
