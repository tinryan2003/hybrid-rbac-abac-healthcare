# 🚀 Deploy Keycloak SPI to Docker Container

## 📋 Prerequisites

1. ✅ Keycloak Docker container is running
2. ✅ Maven is installed (or use `./mvnw`)
3. ✅ Docker is running

---

## 🔧 Step-by-Step Deployment

### **Step 1: Build the SPI JAR**

```bash
cd keycloak-spi
./mvnw clean package
```

**Output:** `target/keycloak-registration-spi.jar`

**Verify build:**
```bash
ls -la target/keycloak-registration-spi.jar
```

---

### **Step 2: Copy JAR to Keycloak Container**

```bash
# Copy JAR to Keycloak providers directory
docker cp target/keycloak-registration-spi.jar bank_hybrid_keycloak:/opt/keycloak/providers/

# Verify JAR is in container
docker exec bank_hybrid_keycloak ls -la /opt/keycloak/providers/ | grep keycloak-registration-spi
```

**Expected output:**
```
-rw-r--r-- 1 keycloak keycloak 12345 Jan 01 12:00 keycloak-registration-spi.jar
```

---

### **Step 3: Restart Keycloak**

```bash
# Restart Keycloak to load the new provider
docker restart bank_hybrid_keycloak

# Wait for Keycloak to start (takes ~30-60 seconds)
docker logs -f bank_hybrid_keycloak
```

**Look for this in logs:**
```
Registration Webhook SPI initialized
   Provider ID: registration-webhook
   Description: Listens to REGISTER events and sends webhooks to backend
```

---

### **Step 4: Verify SPI is Loaded**

```bash
# Check Keycloak logs for SPI initialization
docker logs bank_hybrid_keycloak | grep -i "registration\|webhook\|spi"

# Or check providers directory
docker exec bank_hybrid_keycloak ls -la /opt/keycloak/providers/
```

---

### **Step 5: Enable SPI in Keycloak Admin Console**

1. **Open Keycloak Admin:** http://localhost:8180
2. **Login:** 
   - Username: `admin`
   - Password: `admin`
3. **Select Realm:** `customer-portal` (or your realm)
4. **Navigate:** Realm Settings → **Events** tab
5. **Event Listeners section:**
   - Click **"Add"** or **"Add listener"**
   - Type: `registration-webhook`
   - Click **"Save"**
6. **User events section:**
   - ✅ Enable **"Save Events"**
   - ✅ Add **"REGISTER"** to event types
   - Click **"Save"**

---

## 🔄 Quick Deployment Script

Create a script to automate deployment:

**Windows (deploy-spi.bat):**
```batch
@echo off
echo Building Keycloak SPI...
call mvnw.cmd clean package

echo Copying JAR to Keycloak container...
docker cp target\keycloak-registration-spi.jar bank_hybrid_keycloak:/opt/keycloak/providers/

echo Restarting Keycloak...
docker restart bank_hybrid_keycloak

echo Waiting for Keycloak to start...
timeout /t 10

echo Checking Keycloak logs...
docker logs --tail 50 bank_hybrid_keycloak | findstr /i "registration webhook spi"

echo Done! Check Keycloak logs to verify SPI is loaded.
```

**Linux/Mac (deploy-spi.sh):**
```bash
#!/bin/bash
echo "Building Keycloak SPI..."
./mvnw clean package

echo "Copying JAR to Keycloak container..."
docker cp target/keycloak-registration-spi.jar bank_hybrid_keycloak:/opt/keycloak/providers/

echo "Restarting Keycloak..."
docker restart bank_hybrid_keycloak

echo "Waiting for Keycloak to start..."
sleep 10

echo "Checking Keycloak logs..."
docker logs --tail 50 bank_hybrid_keycloak | grep -i "registration\|webhook\|spi"

echo "Done! Check Keycloak logs to verify SPI is loaded."
```

**Make executable:**
```bash
chmod +x deploy-spi.sh
```

---

## 🧪 Test the SPI

### **1. Register a New User**

1. Go to your registration page
2. Register with a new email
3. Complete registration

### **2. Check Keycloak Logs**

```bash
docker logs -f bank_hybrid_keycloak
```

**Look for:**
```
🔔 ========== REGISTER Event Detected ==========
   Event Type: REGISTER
   User ID: ...
📤 Sending registration webhook to backend...
✅ Registration webhook sent successfully!
```

### **3. Check Backend Logs**

Your backend should receive the webhook and log:
```
🔔 ========== Keycloak Event Received ==========
Event Type: REGISTER
```

---

## 🔍 Troubleshooting

### **Issue: JAR not found in container**

```bash
# Check if JAR exists
docker exec bank_hybrid_keycloak ls -la /opt/keycloak/providers/

# If missing, copy again
docker cp target/keycloak-registration-spi.jar bank_hybrid_keycloak:/opt/keycloak/providers/
docker restart bank_hybrid_keycloak
```

### **Issue: SPI not loading**

```bash
# Check Keycloak logs for errors
docker logs bank_hybrid_keycloak | grep -i error

# Verify JAR is valid
docker exec bank_hybrid_keycloak jar -tf /opt/keycloak/providers/keycloak-registration-spi.jar

# Check Keycloak version compatibility
docker exec bank_hybrid_keycloak cat /opt/keycloak/version.txt
```

### **Issue: SPI not enabled in Admin Console**

1. Go to Keycloak Admin Console
2. Realm Settings → Events → Event Listeners
3. Verify `registration-webhook` is in the list
4. If not, add it manually

### **Issue: REGISTER events not firing**

1. Go to Keycloak Admin Console
2. Realm Settings → Events → User events
3. Verify:
   - ✅ "Save Events" is enabled
   - ✅ "REGISTER" is in the event types list

### **Issue: Webhook not reaching backend**

```bash
# Check backend URL in SPI code
# Default: http://host.docker.internal:8083/keycloak/events

# For Docker Keycloak, use:
# http://host.docker.internal:8083/keycloak/events

# For local Keycloak, use:
# http://localhost:8083/keycloak/events
```

**Set custom backend URL:**
```bash
# Add to docker-compose.yml environment:
KEYCLOAK_BACKEND_URL=http://host.docker.internal:8083/keycloak/events
```

---

## 📝 Configuration

### **Backend URL**

The SPI sends webhooks to your backend. Default URL:
- **Docker Keycloak:** `http://host.docker.internal:8083/keycloak/events`
- **Local Keycloak:** `http://localhost:8083/keycloak/events`

**To change backend URL:**
1. Edit `RegistrationListenerProvider.java`
2. Rebuild: `./mvnw clean package`
3. Redeploy: `docker cp ... && docker restart ...`

Or set environment variable:
```yaml
# docker-compose.yml
environment:
  KEYCLOAK_BACKEND_URL: http://host.docker.internal:8083/keycloak/events
```

---

## ✅ Verification Checklist

- [ ] SPI JAR built successfully (`target/keycloak-registration-spi.jar` exists)
- [ ] JAR copied to container (`docker exec ... ls /opt/keycloak/providers/`)
- [ ] Keycloak restarted (`docker restart bank_hybrid_keycloak`)
- [ ] SPI appears in Keycloak logs ("Registration Webhook SPI initialized")
- [ ] SPI enabled in Admin Console (Event Listeners)
- [ ] REGISTER events enabled (User events)
- [ ] Backend endpoint exists (`/keycloak/events`)
- [ ] Test registration triggers webhook

---

## 🎯 Summary

**Quick Deploy:**
```bash
cd keycloak-spi
./mvnw clean package
docker cp target/keycloak-registration-spi.jar bank_hybrid_keycloak:/opt/keycloak/providers/
docker restart bank_hybrid_keycloak
```

**Then enable in Keycloak Admin Console:**
1. Realm Settings → Events
2. Add `registration-webhook` to Event Listeners
3. Enable "Save Events" and add "REGISTER" event type

**Done!** 🚀

