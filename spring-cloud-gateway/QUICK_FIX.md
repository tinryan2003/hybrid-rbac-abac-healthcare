# ⚡ Quick Fix: IDE Classpath Error

## ❌ Error
```
SpringApplication cannot be resolved
```

## ✅ Solution: Use Maven (Recommended)

**Don't run from IDE!** Use Maven instead:

```bash
cd spring-cloud-gateway
./mvnw spring-boot:run
```

**Windows:**
```bash
cd spring-cloud-gateway
mvnw.cmd spring-boot:run
```

This will:
- ✅ Use correct classpath
- ✅ Read application.yml
- ✅ Start on port 8081

---

## 🔄 Alternative: Fix IDE

### **Step 1: Clean Java Workspace**
1. Press `Ctrl+Shift+P` (or `Cmd+Shift+P`)
2. Type: `Java: Clean Java Language Server Workspace`
3. Click "Restart and delete"

### **Step 2: Reload Projects**
1. Press `Ctrl+Shift+P`
2. Type: `Java: Reload Projects`
3. Wait for Maven dependencies to download

### **Step 3: Wait for Indexing**
- Look at bottom-right status bar
- Wait for "Indexing..." to complete

---

## 🎯 Why This Happens

- IDE uses its own classpath (workspace storage)
- Maven uses resolved dependencies from `.m2` repository
- IDE needs to sync with Maven
- Sometimes sync fails

---

## ✅ Best Practice

**Always use Maven to run Spring Boot apps:**
```bash
./mvnw spring-boot:run
```

This ensures:
- ✅ Correct dependencies
- ✅ Correct configuration
- ✅ No IDE issues

---

**The code is correct!** Just use Maven to run it. 🚀

