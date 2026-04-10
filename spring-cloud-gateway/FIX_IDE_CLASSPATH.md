# 🔧 Fix IDE Classpath Error - SpringApplication cannot be resolved

## Problem

IDE shows: `SpringApplication cannot be resolved`

But Maven build works fine! ✅

## Root Cause

The IDE (Cursor/VS Code Java Language Server) is trying to run from its own workspace classpath, not from Maven's resolved dependencies.

---

## ✅ Solution 1: Refresh Java Language Server (Recommended)

### **In Cursor/VS Code:**

1. **Open Command Palette:**
   - Windows: `Ctrl+Shift+P`
   - Mac: `Cmd+Shift+P`

2. **Clean Java Workspace:**
   ```
   Java: Clean Java Language Server Workspace
   ```
   - Select this command
   - Click "Restart and delete"

3. **Reload Projects:**
   ```
   Java: Reload Projects
   ```
   - Wait for Maven dependencies to download
   - Check bottom-right status bar for progress

4. **Wait for Indexing:**
   - Look for "Indexing..." in status bar
   - Wait until it completes

5. **Try Running Again**

---

## ✅ Solution 2: Run via Maven (Always Works)

Instead of running from IDE, use Maven:

### **Windows:**
```bash
cd spring-cloud-gateway
mvnw.cmd spring-boot:run
```

### **Linux/Mac:**
```bash
cd spring-cloud-gateway
./mvnw spring-boot:run
```

This will:
- ✅ Use correct classpath
- ✅ Read application.yml correctly
- ✅ Use port 8081 as configured

---

## ✅ Solution 3: Build JAR and Run

```bash
cd spring-cloud-gateway

# Build
./mvnw clean package

# Run
java -jar target/spring-cloud-gateway-0.0.1-SNAPSHOT.jar
```

---

## ✅ Solution 4: Check Java Extension Settings

1. Open Settings: `Ctrl+,` (or `Cmd+,`)
2. Search: `java.configuration.maven`
3. Ensure Maven is enabled
4. Search: `java.import.maven`
5. Ensure Maven import is enabled

---

## 🔍 Verify Configuration

After fixing, verify the port is correct:

```bash
# Check if gateway starts on port 8081
curl http://localhost:8081/actuator/health
```

---

## 📝 Why This Happens

- IDE uses its own classpath (workspace storage)
- Maven uses resolved dependencies from `.m2` repository
- IDE needs to sync with Maven dependencies
- Sometimes the sync fails or is incomplete

---

## ✅ Quick Fix Summary

**Best approach:**
1. Run via Maven: `./mvnw spring-boot:run`
2. Or refresh Java Language Server workspace

**The code is correct!** It's just an IDE classpath sync issue.

