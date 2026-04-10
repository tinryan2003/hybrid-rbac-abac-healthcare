# ⚡ Quick Deploy Guide - Keycloak SPI to Docker

## 🚀 One-Command Deployment

### **Windows:**
```bash
cd keycloak-spi
deploy-spi.bat
```

### **Linux/Mac:**
```bash
cd keycloak-spi
./deploy-spi.sh
```

---

## 📋 Manual Deployment (3 Steps)

### **Step 1: Build**
```bash
cd keycloak-spi
./mvnw clean package
```

### **Step 2: Copy to Container**
```bash
docker cp target/keycloak-registration-spi.jar bank_hybrid_keycloak:/opt/keycloak/providers/
```

### **Step 3: Restart Keycloak**
```bash
docker restart bank_hybrid_keycloak
```

---

## ✅ Verify Deployment

### **Check JAR is in Container:**
```bash
docker exec bank_hybrid_keycloak ls -la /opt/keycloak/providers/ | grep keycloak-registration-spi
```

### **Check Keycloak Logs:**
```bash
docker logs bank_hybrid_keycloak | grep -i "registration\|webhook\|spi"
```

**Look for:**
```
Registration Webhook SPI initialized
   Provider ID: registration-webhook
```

---

## ⚙️ Enable in Keycloak Admin Console

1. **Open:** http://localhost:8180
2. **Login:** `admin` / `admin`
3. **Select Realm:** `customer-portal` (or your realm)
4. **Navigate:** Realm Settings → **Events** tab
5. **Event Listeners:**
   - Add: `registration-webhook`
   - Save
6. **User Events:**
   - ✅ Enable "Save Events"
   - ✅ Add "REGISTER" to event types
   - Save

---

## 🧪 Test

1. Register a new user
2. Check Keycloak logs:
   ```bash
   docker logs -f bank_hybrid_keycloak
   ```
3. Look for: `🔔 REGISTER Event Detected`

---

## 🔧 Troubleshooting

**JAR not found?**
```bash
docker cp target/keycloak-registration-spi.jar bank_hybrid_keycloak:/opt/keycloak/providers/
docker restart bank_hybrid_keycloak
```

**SPI not loading?**
```bash
docker logs bank_hybrid_keycloak | grep -i error
```

**Backend not receiving webhook?**
- Default URL: `http://host.docker.internal:8083/keycloak/events`
- Make sure backend is running on port 8083
- Check backend endpoint exists: `/keycloak/events`

---

**That's it!** 🎉

