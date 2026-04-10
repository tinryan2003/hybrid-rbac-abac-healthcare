# 📬 Corporate Banking Notification System

**Note:** This handles notifications for Corporate Banking users (employees of corporate clients), 
not retail customers.

## ✅ Implementation Complete

The notification service now sends notifications to corporate users when they:
- ✅ **Create a transaction** (booking confirmation)
- ✅ **Transaction is approved** (approval notification)
- ✅ **Transaction is rejected** (rejection notification)

---

## 🏗️ Architecture

```
Customer creates transaction
    ↓
Backend publishes event to RabbitMQ
    ↓
Notification Service consumes event
    ├─→ Sends in-app notification (WebSocket)
    ├─→ Sends email notification
    └─→ Saves notification to database
```

---

## 📦 Components Created

### **1. DTOs**
- `EmailDetailDTO.java` - Email message structure
- `TransactionEvent.java` - Transaction event from backend

### **2. Services**
- `NotificationService.java` - Main service that:
  - Listens to RabbitMQ transaction events
  - Creates notification records
  - Sends in-app notifications via WebSocket
  - Sends email notifications
  - Handles all customer notification scenarios

### **3. RabbitMQ Configuration**
- Updated `RabbitMQConfig.java` with:
  - `transaction.created.queue`
  - `transaction.approved.queue`
  - `transaction.rejected.queue`
  - `transaction.exchange` (Topic Exchange)

---

## 🔄 Notification Flow

### **1. Transaction Created (Booking)**

**When:** Customer creates a transaction

**What happens:**
1. Backend publishes `TransactionEvent` to `transaction.created.queue`
2. NotificationService receives event
3. Creates notification record in database
4. Sends in-app notification via WebSocket to customer
5. Sends booking confirmation email

**Email Subject:** "Transaction Booking Confirmation"

**Email Content:**
- Transaction ID
- Transaction type
- Amount
- Status: Pending Approval
- Message: "Your transaction has been submitted and is pending approval"

---

### **2. Transaction Approved**

**When:** Manager approves customer's transaction

**What happens:**
1. Backend publishes `TransactionEvent` to `transaction.approved.queue`
2. NotificationService receives event
3. Creates notification record
4. Sends in-app notification via WebSocket
5. Sends approval confirmation email

**Email Subject:** "Transaction Approved Successfully"

**Email Content:**
- Transaction ID
- Transaction type
- Amount
- Status: Approved
- Approved date
- Message: "Your transaction has been approved and completed successfully"

---

### **3. Transaction Rejected**

**When:** Manager rejects customer's transaction

**What happens:**
1. Backend publishes `TransactionEvent` to `transaction.rejected.queue`
2. NotificationService receives event
3. Creates notification record
4. Sends in-app notification via WebSocket
5. Sends rejection email

**Email Subject:** "Transaction Rejected"

**Email Content:**
- Transaction ID
- Transaction type
- Amount
- Status: Rejected
- Message: "Your transaction has been rejected. Please contact support."

---

## 📋 RabbitMQ Queues

| Queue Name | Routing Key | Purpose |
|------------|-------------|---------|
| `transaction.created.queue` | `transaction.created` | Customer creates transaction |
| `transaction.approved.queue` | `transaction.approved` | Transaction approved |
| `transaction.rejected.queue` | `transaction.rejected` | Transaction rejected |

**Exchange:** `transaction.exchange` (Topic Exchange)

---

## 🔧 Configuration

### **application.properties**

```properties
# RabbitMQ Configuration
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=admin
spring.rabbitmq.password=admin

# Transaction Queues
rabbitmq.queue.transaction.created=transaction.created.queue
rabbitmq.queue.transaction.approved=transaction.approved.queue
rabbitmq.queue.transaction.rejected=transaction.rejected.queue
rabbitmq.exchange.transaction.name=transaction.exchange
```

---

## 📧 Email Templates

All emails are HTML formatted with:
- Professional styling
- Transaction details
- Clear status indicators
- Support contact information

**Templates are built dynamically** (no external template files needed)

---

## 🔌 WebSocket Notifications

**Endpoint:** `/ws` (SockJS enabled)

**Destination:** `/topic/notifications/{userId}`

**Message Format:**
```json
{
  "id": 1,
  "userId": 123,
  "userType": "CUSTOMER",
  "type": "TRANSACTION",
  "title": "Transaction Approved",
  "message": "Your transaction has been approved...",
  "status": "SENT",
  "channel": "BOTH",
  "createdAt": "2025-01-01T12:00:00"
}
```

---

## 📊 Database Storage

All notifications are stored in `notifications` table:
- Notification record created
- Status tracked (PENDING → SENT)
- Email sent timestamp recorded
- In-app notification sent timestamp recorded
- Read status tracked

---

## 🚀 Backend Integration Required

Your backend needs to publish events to RabbitMQ when:

### **1. Transaction Created**
```java
// In TransactionService.createTransaction()
TransactionEvent event = TransactionEvent.builder()
    .transactionId(transaction.getId())
    .customerId(transaction.getFromAccount().getCustomer().getId())
    .customerEmail(transaction.getFromAccount().getCustomer().getEmail())
    .customerName(transaction.getFromAccount().getCustomer().getFirstName() + " " + 
                 transaction.getFromAccount().getCustomer().getLastName())
    .transactionType(transaction.getType().name())
    .amount(transaction.getAmount())
    .status(transaction.getStatus().name())
    .description(transaction.getDescription())
    .createdAt(transaction.getCreatedAt())
    .eventType("TRANSACTION_CREATED")
    .build();

rabbitTemplate.convertAndSend("transaction.exchange", "transaction.created", event);
```

### **2. Transaction Approved**
```java
// In TransactionService.approveTransaction()
TransactionEvent event = TransactionEvent.builder()
    .transactionId(transaction.getId())
    .customerId(transaction.getFromAccount().getCustomer().getId())
    .customerEmail(transaction.getFromAccount().getCustomer().getEmail())
    .customerName(transaction.getFromAccount().getCustomer().getFirstName() + " " + 
                 transaction.getFromAccount().getCustomer().getLastName())
    .transactionType(transaction.getType().name())
    .amount(transaction.getAmount())
    .status("APPROVED")
    .approvedAt(transaction.getApprovedAt())
    .approvedByEmployeeId(transaction.getApprovedByEmployeeId())
    .eventType("TRANSACTION_APPROVED")
    .build();

rabbitTemplate.convertAndSend("transaction.exchange", "transaction.approved", event);
```

### **3. Transaction Rejected**
```java
// In TransactionService.rejectTransaction()
TransactionEvent event = TransactionEvent.builder()
    .transactionId(transaction.getId())
    .customerId(transaction.getFromAccount().getCustomer().getId())
    .customerEmail(transaction.getFromAccount().getCustomer().getEmail())
    .customerName(transaction.getFromAccount().getCustomer().getFirstName() + " " + 
                 transaction.getFromAccount().getCustomer().getLastName())
    .transactionType(transaction.getType().name())
    .amount(transaction.getAmount())
    .status("REJECTED")
    .eventType("TRANSACTION_REJECTED")
    .build();

rabbitTemplate.convertAndSend("transaction.exchange", "transaction.rejected", event);
```

---

## ✅ Testing

### **1. Start Services**
```bash
# Start RabbitMQ
docker-compose up -d rabbitmq

# Start Notification Service
cd notification-service
./mvnw spring-boot:run
```

### **2. Test Transaction Created**
- Create a transaction in your backend
- Check notification service logs for: `📬 Processing transaction created event`
- Check customer receives:
  - In-app notification (if WebSocket connected)
  - Email notification

### **3. Test Transaction Approved**
- Approve a transaction
- Check logs for: `📬 Processing transaction approved event`
- Verify customer receives notification

### **4. Check RabbitMQ**
- Open: http://localhost:15672 (admin/admin)
- Check queues have messages
- Verify consumers are connected

---

## 🎯 Summary

**Customer notifications are now fully implemented!**

✅ In-app notifications via WebSocket
✅ Email notifications for all transaction events
✅ Database storage for notification history
✅ RabbitMQ integration for async processing
✅ Professional HTML email templates

**Next step:** Integrate event publishing in your backend `TransactionService`! 🚀

