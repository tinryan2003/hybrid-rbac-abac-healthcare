# ✅ Quick Fix Checklist for Registration

## 🔍 Verify These Steps

### 1. ✅ SPI is Built and Deployed

```bash
cd keycloak-spi
./mvnw clean package
docker cp target/keycloak-registration-spi.jar bank_hybrid_keycloak:/opt/keycloak/providers/
docker restart bank_hybrid_keycloak
```

**Check Keycloak logs for:**
```
Registration Webhook SPI initialized
   Provider ID: registration-webhook
```

### 2. ✅ SPI is Enabled in Keycloak

1. Keycloak Admin → `customer-portal` realm
2. **Realm Settings** → **Events** tab
3. **Event Listeners**: Add `registration-webhook` ✅
4. **User events**: Enable "Save Events" ✅
5. **User events**: Add "REGISTER" to event types ✅
6. **Save**

### 3. ✅ Backend is Running

- Backend should be running on **port 8083**
- Verify: `http://localhost:8083/actuator/health` (if actuator enabled)
- Or: `curl http://localhost:8083/keycloak/events` (should return 405 Method Not Allowed, not 404)

### 4. ✅ Network Connectivity

**If Keycloak is in Docker:**
- Backend URL: `http://host.docker.internal:8083/keycloak/events` ✅
- Backend must be accessible from Docker container

**If Keycloak is Local:**
- Set environment variable: `KEYCLOAK_BACKEND_URL=http://localhost:8083/keycloak/events`
- Or change SPI code default URL

### 5. ✅ Test Registration

1. **Delete test user** from Keycloak (if exists)
2. **Register NEW user** via your app
3. **Check Keycloak logs** for:
   ```
   🔔 ========== REGISTER Event Detected ==========
   📤 Sending registration webhook to backend:
   📥 Webhook response received:
      Status Code: 200
   ✅ Registration webhook sent successfully!
   ```
4. **Check Backend logs** for:
   ```
   🔔 ========== Keycloak Event Received ==========
   ✅ Successfully saved customer: ID=..., User ID=...
   ```
5. **Check Database**:
   ```sql
   SELECT * FROM users ORDER BY created_at DESC LIMIT 1;
   SELECT * FROM customers ORDER BY created_at DESC LIMIT 1;
   ```

## 🚨 Common Issues

### Issue: "host.docker.internal" not resolving
**Symptom**: Keycloak logs show connection refused
**Fix**: 
- If backend is also in Docker: Use Docker service name
- If backend is local: Verify `host.docker.internal` works (Windows/Mac should work, Linux may need extra config)

### Issue: Backend not accessible
**Symptom**: Status Code 500 or connection refused
**Fix**: 
- Verify backend is running: `curl http://localhost:8083/keycloak/events`
- Check firewall/network settings
- Try accessing from Keycloak container: `docker exec bank_hybrid_keycloak curl http://host.docker.internal:8083/keycloak/events`

### Issue: REGISTER event not firing
**Symptom**: No logs from SPI
**Fix**: 
- Verify SPI is enabled in Keycloak
- Verify "Save Events" is enabled
- Verify "REGISTER" is in event types list
- Check if user already exists (REGISTER only fires for NEW users)

## 📋 Next Steps

After fixing the above:
1. **Rebuild SPI**: `./mvnw clean package`
2. **Redeploy**: Copy JAR to Keycloak and restart
3. **Test**: Register a NEW user
4. **Check logs**: Both Keycloak and Backend
5. **Verify database**: Check if records were created

