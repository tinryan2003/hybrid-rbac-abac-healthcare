# 🚀 Deploy and Test Keycloak SPI

## ✅ Changes Made

1. **Enhanced Logging**: Added detailed logs with emojis for easier debugging
2. **Configurable Backend URL**: Can be set via `KEYCLOAK_BACKEND_URL` environment variable
3. **Better Error Messages**: More detailed error information

## 🔧 Rebuild and Deploy SPI

### Step 1: Rebuild SPI

```bash
cd keycloak-spi
./mvnw clean package
```

This creates: `target/keycloak-registration-spi.jar`

### Step 2: Deploy to Keycloak

**If Keycloak is in Docker:**
```bash
# Copy JAR to Keycloak container
docker cp target/keycloak-registration-spi.jar bank_hybrid_keycloak:/opt/keycloak/providers/

# Restart Keycloak
docker restart bank_hybrid_keycloak
```

**If Keycloak is Local:**
```bash
# Copy JAR to Keycloak providers directory
cp target/keycloak-registration-spi.jar /path/to/keycloak/providers/

# Restart Keycloak
```

### Step 3: Configure Backend URL (If Needed)

**If backend is NOT accessible via default URL**, set environment variable:

**For Docker Keycloak:**
```bash
# Add to docker-compose.yml or set in container
KEYCLOAK_BACKEND_URL=http://host.docker.internal:8083/keycloak/events
```

**For Local Keycloak:**
```bash
# Set environment variable before starting Keycloak
export KEYCLOAK_BACKEND_URL=http://localhost:8083/keycloak/events
```

## ✅ Verify SPI is Loaded

Check Keycloak startup logs for:
```
Registration Webhook SPI initialized
   Provider ID: registration-webhook
   Description: Listens to REGISTER events and sends webhooks to backend
```

## ✅ Enable SPI in Keycloak

1. **Login to Keycloak Admin**: `http://localhost:8180`
2. **Select Realm**: `customer-portal`
3. **Go to**: Realm Settings → **Events** tab
4. **Event Listeners** section:
   - Add `registration-webhook` to the list
   - Save
5. **User events** section:
   - Enable **"Save Events"**
   - Add **"REGISTER"** to event types
   - Save

## 🧪 Test Registration

### Step 1: Register New User

1. Go to registration page
2. Register with a **NEW email** (not existing in Keycloak)
3. Complete registration

### Step 2: Check Keycloak Logs

Look for:
```
🔔 ========== REGISTER Event Detected ==========
   Event Type: REGISTER
   User ID: ...
   Backend URL: http://...
📤 Sending registration webhook to backend:
   URL: http://...
📥 Webhook response received:
   Status Code: 200
✅ Registration webhook sent successfully!
```

### Step 3: Check Backend Logs

Look for:
```
🔔 ========== Keycloak Event Received ==========
Event Type: REGISTER
🚀 Calling customerService.createCustomerFromKeycloak()...
✅ Successfully saved customer: ID=..., User ID=...
```

### Step 4: Verify Database

```sql
SELECT * FROM users ORDER BY created_at DESC LIMIT 5;
SELECT * FROM customers ORDER BY created_at DESC LIMIT 5;
```

## 🔍 Troubleshooting

### Issue: SPI not loaded
**Check**: Keycloak logs for "Registration Webhook SPI initialized"
**Fix**: Verify JAR is in `/opt/keycloak/providers/` and Keycloak restarted

### Issue: SPI not enabled
**Check**: Keycloak Admin → Realm Settings → Events → Event Listeners
**Fix**: Add `registration-webhook` to the list

### Issue: REGISTER events not firing
**Check**: Keycloak Admin → Realm Settings → Events → User events
**Fix**: Enable "Save Events" and add "REGISTER" event type

### Issue: Webhook connection failed
**Check**: Keycloak logs for "IOException sending webhook"
**Fix**: 
- Verify backend is running on port 8083
- Check backend URL (use `host.docker.internal` for Docker, `localhost` for local)
- Set `KEYCLOAK_BACKEND_URL` environment variable if needed

### Issue: Backend not receiving webhook
**Check**: Backend logs for "Keycloak Event Received"
**Fix**: 
- Verify backend URL in SPI matches your setup
- Check network connectivity
- Verify backend endpoint `/keycloak/events` exists

## 📋 Expected Flow

1. ✅ User registers → Keycloak creates user
2. ✅ Keycloak fires REGISTER event
3. ✅ SPI catches event → Logs "REGISTER Event Detected"
4. ✅ SPI sends webhook → POST to backend
5. ✅ Backend receives → Logs "Keycloak Event Received"
6. ✅ Backend creates Customer → Database updated
7. ✅ User can login → Data is in database

