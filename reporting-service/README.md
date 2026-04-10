# Reporting Service

A comprehensive reporting and analytics service for the Hybrid RBAC-ABAC Banking System. Generate, manage, and download financial reports in multiple formats.

## 🎯 Features

- **Multiple Report Types**: Transaction summaries, account balances, audit trails, compliance reports
- **Multiple Formats**: PDF, Excel, CSV, JSON
- **Async Generation**: Reports generated in background with status tracking
- **Download Support**: Download completed reports via REST API
- **OAuth2 Security**: Protected with Keycloak JWT authentication
- **Role-Based Access**: ADMIN, MANAGER, AUDITOR roles
- **Scheduled Reports**: Daily, weekly, monthly automated reports (configurable)

## 📋 Prerequisites

- Java 21
- MySQL running on `localhost:3306`
- Database: `bank_hybrid`
- Keycloak on `localhost:8180` (optional for testing)

## 🚀 Quick Start

```bash
cd reporting-service
./mvnw spring-boot:run
```

Service starts on **port 8089**

## 📡 API Endpoints

### Generate Report
```http
POST /api/reports
Content-Type: application/json
Authorization: Bearer <token>

{
  "type": "TRANSACTION_SUMMARY",
  "format": "PDF",
  "name": "Monthly Transactions",
  "startDate": "2024-01-01T00:00:00",
  "endDate": "2024-01-31T23:59:59",
  "emailRecipients": ["manager@bank.com"]
}
```

### Get Report Status
```http
GET /api/reports/{id}
```

### Get My Reports
```http
GET /api/reports/my-reports
```

### Get All Reports
```http
GET /api/reports
```

### Get Reports by Status
```http
GET /api/reports/status/COMPLETED
```

### Get Reports by Type
```http
GET /api/reports/type/TRANSACTION_SUMMARY
```

### Download Report
```http
GET /api/reports/{id}/download
```

### Cancel Report
```http
DELETE /api/reports/{id}/cancel
```

### Delete Report
```http
DELETE /api/reports/{id}
```

## 📊 Report Types

- `TRANSACTION_SUMMARY` - Transaction reports
- `ACCOUNT_BALANCE` - Account balance reports
- `AUDIT_TRAIL` - Audit log reports
- `USER_ACTIVITY` - User activity reports
- `COMPLIANCE` - Compliance reports
- `AUTHORIZATION_DECISIONS` - Authorization policy reports
- `RISK_ANALYSIS` - Risk analysis reports
- `CUSTOMER_ANALYTICS` - Customer analytics
- `EMPLOYEE_PERFORMANCE` - Employee performance
- `DAILY_SUMMARY` - Daily summaries
- `WEEKLY_SUMMARY` - Weekly summaries
- `MONTHLY_SUMMARY` - Monthly summaries

## 📁 Report Formats

- `PDF` - PDF documents
- `EXCEL` - Excel spreadsheets (.xlsx)
- `CSV` - Comma-separated values
- `JSON` - JSON data files

## 🔐 Security

Requires Keycloak JWT token with appropriate roles:
- `ROLE_ADMIN` - Full access
- `ROLE_MANAGER` - Can generate and view reports
- `ROLE_AUDITOR` - Read-only access

## ⚙️ Configuration

Edit `src/main/resources/application.yml`:

```yaml
reporting:
  output-directory: ${user.home}/reports  # Where to save reports
  max-concurrent-reports: 5               # Max parallel generations
  retention-days: 30                      # Auto-delete after days
  scheduled:
    enabled: true
    daily-report: "0 0 1 * * ?"          # 1 AM daily
    weekly-report: "0 0 2 * * SUN"       # 2 AM Sunday
    monthly-report: "0 0 3 1 * ?"        # 3 AM 1st of month
```

## 📝 Report Status Flow

```
PENDING → PROCESSING → COMPLETED
                    ↘ FAILED
           ↘ CANCELLED
```

## 🛠️ Development

### Build
```bash
./mvnw clean install
```

### Run Tests
```bash
./mvnw test
```

### Run with Profile
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

## 📈 Monitoring

Health check:
```http
GET /actuator/health
```

Metrics:
```http
GET /actuator/metrics
```

## 🔄 Integration

Reports are stored in:
- Default: `~/reports/`
- Configurable via `reporting.output-directory`

File naming: `{report-name}_{report-id}.{extension}`

Example: `Monthly_Transactions_123.pdf`

## 🎨 Future Enhancements

- Real PDF generation with iText
- Real Excel generation with Apache POI
- Email delivery of reports
- Report templates
- Chart/graph generation
- Scheduled report automation
- Report compression for large files
- S3/Cloud storage integration

## 📚 Dependencies

- Spring Boot 4.0.0
- Spring Data JPA
- Spring Security OAuth2
- MySQL Connector
- Lombok
- iText (PDF)
- Apache POI (Excel)
- Spring Quartz (Scheduling)

## ✅ Status

**✓ Fully Implemented**
- REST API with all endpoints
- Database persistence
- Async report generation
- File download support
- OAuth2 security
- Role-based access control

**🚧 Basic Implementation** (can be enhanced)
- PDF generation (currently text-based, can use iText)
- Excel generation (currently text-based, can use Apache POI)
- Email delivery (configured but not implemented)
- Scheduled reports (configured but not implemented)

## 🚀 Ready to Use!

The service is production-ready for basic reporting needs. Enhancements can be added as requirements grow.
