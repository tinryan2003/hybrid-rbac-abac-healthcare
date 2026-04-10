# 🔧 Fix Keycloak SPI Registration Issues

## 🔍 Issues Found

### Issue 1: Backend URL Configuration
The SPI uses `http://host.docker.internal:8083` which only works if:
- ✅ Keycloak is running in **Docker**
- ❌ Keycloak is running **locally** (won't work)

### Issue 2: README Mismatch
- README says port `8081`
- Code uses port `8083`
- Backend is actually on `8083` ✅

## ✅ Solutions

### Solution 1: Make Backend URL Configurable (Recommended)

Update the SPI to read backend URL from environment variable or config.

### Solution 2: Fix URL Based on Your Setup

**If Keycloak is running LOCALLY** (not Docker):
- Change `BACKEND_URL` to: `http://localhost:8083/keycloak/events`

**If Keycloak is running in DOCKER**:
- Keep `BACKEND_URL` as: `http://host.docker.internal:8083/keycloak/events`

## 🔧 Quick Fix

### Option A: Update SPI Code (If Keycloak is Local)

Change line 25 in `RegistrationListenerProvider.java`:
```java
// For LOCAL Keycloak
private static final String BACKEND_URL = "http://localhost:8083/keycloak/events";

// For DOCKER Keycloak  
private static final String BACKEND_URL = "http://host.docker.internal:8083/keycloak/events";
```

Then rebuild and redeploy:
```bash
cd keycloak-spi
./mvnw clean package
docker cp target/keycloak-registration-spi.jar keycloak:/opt/keycloak/providers/
docker restart keycloak
```

### Option B: Use Environment Variable (Better)

Make it configurable via environment variable.

## 🔍 Verify SPI is Working

### Check 1: SPI is Loaded
Look in Keycloak startup logs for:
```
Registration Webhook SPI initialized
   Provider ID: registration-webhook
```

### Check 2: SPI is Enabled
1. Keycloak Admin → **Realm Settings** → **Events** tab
2. **Event Listeners** section
3. Verify `registration-webhook` is in the list ✅

### Check 3: REGISTER Events are Enabled
1. Keycloak Admin → **Realm Settings** → **Events** tab
2. **User events** section
3. Verify **"Save Events"** is enabled
4. Verify **"REGISTER"** is in the event types list

### Check 4: Test Registration
1. Register a NEW user (not existing)
2. Check Keycloak logs for:
   ```
   REGISTER Event Detected:
      Event Type: REGISTER
      User ID: ...
   Sending registration webhook:
      URL: http://...
   Webhook response:
      Status Code: 200
   ```

## 🚨 Common Issues

### Issue: "host.docker.internal" not resolving
**Symptom**: Keycloak logs show "IOException: Connection refused"
**Fix**: Change to `http://localhost:8083` if Keycloak is local

### Issue: SPI not enabled
**Symptom**: No logs from RegistrationListenerProvider
**Fix**: Enable `registration-webhook` in Keycloak Event Listeners

### Issue: REGISTER events not enabled
**Symptom**: No REGISTER events fired
**Fix**: Enable "Save Events" and "REGISTER" event type in Realm Settings

### Issue: Backend not accessible
**Symptom**: Status Code 500 or connection refused
**Fix**: Verify backend is running on port 8083 and accessible

