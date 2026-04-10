# 📧 Gmail SMTP Configuration Guide

## Quick Setup Guide for VGU Banking Notification Service

---

## 📋 Table of Contents
1. [Prerequisites](#prerequisites)
2. [Generate Gmail App Password](#generate-gmail-app-password)
3. [Configure Application](#configure-application)
4. [Test Email Configuration](#test-email-configuration)
5. [Troubleshooting](#troubleshooting)

---

## Prerequisites

- ✅ Gmail account (personal or workspace)
- ✅ 2-Factor Authentication enabled
- ✅ Notification Service built successfully

---

## 🔐 Generate Gmail App Password

### Step 1: Enable 2-Factor Authentication (2FA)

1. Go to: **https://myaccount.google.com/security**
2. Find **"2-Step Verification"** section
3. Click **"Get Started"** and follow the setup wizard
4. Verify your phone number
5. Complete the 2FA setup

### Step 2: Create App Password

1. Return to: **https://myaccount.google.com/security**
2. Scroll down to **"App passwords"** section
3. Click on **"App passwords"**
4. You may need to sign in again
5. Select app: **"Mail"**
6. Select device: **"Other (Custom name)"**
7. Enter name: **"VGU Banking Notification"**
8. Click **"Generate"**

### Step 3: Save Your App Password

You'll see a 16-character password like:

```
abcd efgh ijkl mnop
```

**⚠️ IMPORTANT:**
- Copy this password immediately
- Store it securely (you won't see it again!)
- Use this password (NOT your Gmail password) in the application

---

## ⚙️ Configure Application

### Option 1: Direct Configuration (Development)

Edit `notification-service/src/main/resources/application.yml`:

```yaml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: your-email@gmail.com        # ← Your Gmail address
    password: abcd efgh ijkl mnop         # ← Your app password (16 chars)
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
            required: true
          connectiontimeout: 5000
          timeout: 5000
          writetimeout: 5000

notification:
  email:
    from: your-email@gmail.com            # ← Same as username
    from-name: VGU Banking System
```

**Example:**
```yaml
spring:
  mail:
    username: tinhnguyen.bank@gmail.com
    password: wxyz abcd efgh ijkl
```

---

### Option 2: Environment Variables (Production - Recommended)

Keep `application.yml` with placeholders:

```yaml
spring:
  mail:
    username: ${MAIL_USERNAME:your-email@gmail.com}
    password: ${MAIL_PASSWORD:your-app-password}
```

#### Windows (Command Prompt):
```cmd
set MAIL_USERNAME=your-email@gmail.com
set MAIL_PASSWORD=abcd efgh ijkl mnop
cd notification-service
mvnw spring-boot:run
```

#### Windows (PowerShell):
```powershell
$env:MAIL_USERNAME="your-email@gmail.com"
$env:MAIL_PASSWORD="abcd efgh ijkl mnop"
cd notification-service
.\mvnw spring-boot:run
```

#### Linux/Mac:
```bash
export MAIL_USERNAME=your-email@gmail.com
export MAIL_PASSWORD="abcd efgh ijkl mnop"
cd notification-service
./mvnw spring-boot:run
```

---

### Option 3: IDE Configuration (IntelliJ/Eclipse)

#### IntelliJ IDEA:
1. Run → Edit Configurations
2. Select your Spring Boot run configuration
3. Add Environment Variables:
   ```
   MAIL_USERNAME=your-email@gmail.com
   MAIL_PASSWORD=abcd efgh ijkl mnop
   ```

#### Eclipse:
1. Run → Run Configurations
2. Select your Spring Boot run configuration
3. Go to "Environment" tab
4. Add variables (same as above)

---

## 🧪 Test Email Configuration

### Step 1: Start the Service

```bash
cd notification-service
./mvnw spring-boot:run
```

Wait for:
```
Started NotificationServiceApplication in X seconds
```

### Step 2: Send Test Email

Open your browser or use curl:

**Browser:**
```
http://localhost:8087/api/test/email?to=your-test-email@gmail.com
```

**Curl:**
```bash
curl "http://localhost:8087/api/test/email?to=your-test-email@gmail.com"
```

**Expected Response:**
```json
{
  "status": "success",
  "message": "Test email sent successfully to your-test-email@gmail.com",
  "info": "Check your inbox (and spam folder)"
}
```

### Step 3: Check Your Email

1. Open your Gmail inbox
2. Look for email with subject: **"🔔 Test Email from VGU Banking System"**
3. If not in inbox, check **Spam/Junk** folder
4. You should see a nicely formatted HTML email

---

## 🔍 Verify Configuration

### Check Application Logs

Look for these log messages:

**✅ Success:**
```
DEBUG o.v.n.service.EmailService : Sending email to: your-email@gmail.com
DEBUG o.v.n.service.EmailService : Email sent successfully to: your-email@gmail.com
```

**❌ Error:**
```
ERROR o.v.n.service.EmailService : Failed to send email: Authentication failed
```

### Check Health Endpoint

```bash
curl http://localhost:8087/actuator/health
```

Look for `mail` component:
```json
{
  "status": "UP",
  "components": {
    "mail": {
      "status": "UP",
      "details": {
        "location": "smtp.gmail.com:587"
      }
    }
  }
}
```

---

## 🛠️ Troubleshooting

### Problem 1: "Authentication Failed"

**Symptoms:**
```
AuthenticationFailedException: 535-5.7.8 Username and Password not accepted
```

**Solutions:**
1. ✅ Verify you're using **App Password** (not Gmail password)
2. ✅ Check for spaces in the password (should be: `abcd efgh ijkl mnop`)
3. ✅ Ensure 2FA is enabled on your Google account
4. ✅ Try generating a new app password
5. ✅ Make sure you're not rate limited (too many failed attempts)

---

### Problem 2: "Connection Timeout"

**Symptoms:**
```
MailConnectException: Couldn't connect to host, port: smtp.gmail.com, 587
```

**Solutions:**
1. ✅ Check your internet connection
2. ✅ Verify firewall isn't blocking port 587
3. ✅ Try port 465 with SSL instead:
   ```yaml
   spring:
     mail:
       port: 465
       properties:
         mail:
           smtp:
             ssl:
               enable: true
   ```
4. ✅ Check if your organization blocks SMTP

---

### Problem 3: "Email Goes to Spam"

**Solutions:**
1. ✅ Mark the email as "Not Spam" in your inbox
2. ✅ Add sender to contacts
3. ✅ For production: Set up SPF, DKIM, DMARC records
4. ✅ Use a custom domain email (not @gmail.com)

---

### Problem 4: "Less Secure App Access"

**Note:** Google deprecated "Less Secure Apps" access.

**Solution:**
- ✅ **Always use App Passwords** (required for 2FA accounts)
- ❌ Don't try to disable "Less Secure Apps" (no longer available)

---

### Problem 5: "Service Not Started"

**Symptoms:**
```
Connection refused: localhost:8087
```

**Solutions:**
1. ✅ Check if service is running: `jps -l` or Task Manager
2. ✅ Check logs for startup errors
3. ✅ Verify port 8087 is not already in use
4. ✅ Ensure MySQL is running (if required)

---

## 📊 Test Results Checklist

- [ ] Gmail App Password generated
- [ ] Application configured with credentials
- [ ] Service starts without errors
- [ ] Test email endpoint accessible
- [ ] Test email received in inbox
- [ ] Email is properly formatted (HTML)
- [ ] Health endpoint shows mail status UP

---

## 🔒 Security Best Practices

### ✅ DO:
- Use environment variables for credentials
- Use App Passwords (never use real Gmail password)
- Rotate app passwords periodically
- Use different app passwords for dev/staging/production
- Add `.env` files to `.gitignore`

### ❌ DON'T:
- Commit passwords to Git
- Share app passwords
- Use same password for multiple applications
- Disable 2FA
- Use "Less Secure Apps" (deprecated)

---

## 📝 Example Configuration Files

### .env file (for local development)
```env
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=abcd efgh ijkl mnop
```

### application-dev.yml
```yaml
spring:
  mail:
    username: ${MAIL_USERNAME}
    password: ${MAIL_PASSWORD}
    host: smtp.gmail.com
    port: 587
```

### application-prod.yml
```yaml
spring:
  mail:
    username: ${MAIL_USERNAME}
    password: ${MAIL_PASSWORD}
    host: smtp.gmail.com
    port: 587
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
            required: true
          ssl:
            trust: smtp.gmail.com
```

---

## 🚀 Next Steps

After successful configuration:

1. ✅ **Remove Test Controller** (production):
   - Delete `EmailTestController.java` or secure it with admin role

2. ✅ **Set up RabbitMQ** for event-driven notifications

3. ✅ **Integrate with Transaction Service**:
   - Transaction Service publishes events
   - Notification Service consumes and sends emails

4. ✅ **Configure WebSocket** for real-time notifications

5. ✅ **Test end-to-end flow**:
   - Create transaction → Receive notification

---

## 📞 Support

If you encounter issues:

1. **Check Logs**: `logs/notification-service.log`
2. **Verify SMTP**: Use online SMTP tester
3. **Google Account**: https://myaccount.google.com/security
4. **Gmail Help**: https://support.google.com/mail

---

## 📚 Additional Resources

- [Gmail SMTP Settings](https://support.google.com/a/answer/176600)
- [Google App Passwords](https://support.google.com/accounts/answer/185833)
- [Spring Boot Mail Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/io.html#io.email)
- [JavaMail API](https://javaee.github.io/javamail/)

---

**Last Updated**: 2025-12-17  
**Service Version**: 0.0.1-SNAPSHOT  
**Port**: 8087

---

