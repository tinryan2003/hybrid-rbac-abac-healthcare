package org.vgu.reportingservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.vgu.reportingservice.enums.ReportType;
import org.vgu.reportingservice.model.Report;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Fetches real data from microservices for report generation.
 * Returns a ReportData record containing column headers, rows, and summary statistics.
 */
@Service
@Slf4j
public class ReportDataService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final RestClient auditClient;
    private final RestClient patientClient;
    private final RestClient appointmentClient;
    private final RestClient billingClient;
    private final RestClient labClient;
    private final RestClient pharmacyClient;

    public ReportDataService(
            @Qualifier("auditRestClient") RestClient auditClient,
            @Qualifier("patientRestClient") RestClient patientClient,
            @Qualifier("appointmentRestClient") RestClient appointmentClient,
            @Qualifier("billingRestClient") RestClient billingClient,
            @Qualifier("labRestClient") RestClient labClient,
            @Qualifier("pharmacyRestClient") RestClient pharmacyClient) {
        this.auditClient = auditClient;
        this.patientClient = patientClient;
        this.appointmentClient = appointmentClient;
        this.billingClient = billingClient;
        this.labClient = labClient;
        this.pharmacyClient = pharmacyClient;
    }

    public record ReportData(
            String title,
            List<String> headers,
            List<List<String>> rows,
            Map<String, Object> summary
    ) {}

    /** Main entry point: delegates to the appropriate fetcher by report type. */
    @SuppressWarnings("unchecked")
    public ReportData fetch(Report report) {
        LocalDateTime from = report.getStartDate() != null
                ? report.getStartDate() : LocalDateTime.now().minusDays(30);
        LocalDateTime to = report.getEndDate() != null
                ? report.getEndDate() : LocalDateTime.now();

        try {
            return switch (report.getType()) {
                case AUDIT_TRAIL, COMPLIANCE_REPORT, AUTHORIZATION_DECISIONS ->
                        fetchAuditTrail(report, from, to);
                case PATIENT_SUMMARY ->
                        fetchPatientSummary(report);
                case APPOINTMENT_REPORT ->
                        fetchAppointmentReport(report, from, to);
                case BILLING_REPORT ->
                        fetchBillingReport(report, from, to);
                case LAB_ORDER_REPORT ->
                        fetchLabOrderReport(report, from, to);
                case PRESCRIPTION_REPORT ->
                        fetchPrescriptionReport(report, from, to);
                case USER_ACTIVITY ->
                        fetchUserActivity(report, from, to);
                case DAILY_SUMMARY, WEEKLY_SUMMARY, MONTHLY_SUMMARY ->
                        fetchSummaryReport(report, from, to);
            };
        } catch (Exception e) {
            log.warn("Data fetch failed for report {} ({}): {}", report.getId(), report.getType(), e.getMessage());
            return emptyReport(report.getType().name());
        }
    }

    // -------------------------------------------------------------------------
    // AUDIT TRAIL
    // -------------------------------------------------------------------------
    @SuppressWarnings("unchecked")
    private ReportData fetchAuditTrail(Report report, LocalDateTime from, LocalDateTime to) {
        String token = getBearerToken();
        List<String> headers = List.of("ID", "Event Type", "Action", "Resource Type", "Resource ID",
                "User", "Result", "IP Address", "Timestamp");

        List<List<String>> rows = new ArrayList<>();
        Map<String, Object> summary = new LinkedHashMap<>();

        try {
            String url = String.format("/audit/logs/date-range?startDate=%s&endDate=%s&size=500&page=0",
                    encode(from.format(ISO)), encode(to.format(ISO)));

            Map<String, Object> response = auditClient.get()
                    .uri(url)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.containsKey("data")) {
                List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("data");
                long allowCount = 0, denyCount = 0, failCount = 0;
                for (Map<String, Object> item : items) {
                    String result = str(item.get("result"));
                    if ("ALLOWED".equalsIgnoreCase(result) || "SUCCESS".equalsIgnoreCase(result)) allowCount++;
                    else if ("DENIED".equalsIgnoreCase(result)) denyCount++;
                    else failCount++;

                    rows.add(List.of(
                            str(item.get("id")),
                            str(item.get("eventType")),
                            str(item.get("action")),
                            str(item.get("resourceType")),
                            str(item.get("resourceId")),
                            str(item.get("username")),
                            result,
                            str(item.get("ipAddress")),
                            str(item.get("timestamp"))
                    ));
                }
                summary.put("totalEvents", rows.size());
                summary.put("allowedCount", allowCount);
                summary.put("deniedCount", denyCount);
                summary.put("failedCount", failCount);
                summary.put("period", from.toLocalDate() + " → " + to.toLocalDate());
            }
        } catch (RestClientException e) {
            log.warn("Could not fetch audit logs: {}", e.getMessage());
        }

        return new ReportData("Audit Trail Report (" + from.toLocalDate() + " – " + to.toLocalDate() + ")",
                headers, rows, summary);
    }

    // -------------------------------------------------------------------------
    // PATIENT SUMMARY
    // -------------------------------------------------------------------------
    @SuppressWarnings("unchecked")
    private ReportData fetchPatientSummary(Report report) {
        String token = getBearerToken();
        List<String> headers = List.of("Patient ID", "Hospital ID", "First Name", "Last Name",
                "Gender", "Birthday", "Phone", "Registered At");
        List<List<String>> rows = new ArrayList<>();
        Map<String, Object> summary = new LinkedHashMap<>();

        try {
            List<Map<String, Object>> patients = patientClient.get()
                    .uri("/patients")
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .body(List.class);

            if (patients != null) {
                long maleCount = 0, femaleCount = 0;
                for (Map<String, Object> p : patients) {
                    String gender = str(p.get("gender"));
                    if ("MALE".equalsIgnoreCase(gender)) maleCount++;
                    else if ("FEMALE".equalsIgnoreCase(gender)) femaleCount++;

                    rows.add(List.of(
                            str(p.get("patientId")),
                            str(p.get("hospitalId")),
                            str(p.get("firstname")),
                            str(p.get("lastname")),
                            gender,
                            str(p.get("birthday")),
                            str(p.get("phoneNumber")),
                            str(p.get("createdAt"))
                    ));
                }
                summary.put("totalPatients", rows.size());
                summary.put("maleCount", maleCount);
                summary.put("femaleCount", femaleCount);
                summary.put("otherCount", rows.size() - maleCount - femaleCount);
            }
        } catch (RestClientException e) {
            log.warn("Could not fetch patients: {}", e.getMessage());
        }

        return new ReportData("Patient Summary Report", headers, rows, summary);
    }

    // -------------------------------------------------------------------------
    // APPOINTMENT REPORT
    // -------------------------------------------------------------------------
    @SuppressWarnings("unchecked")
    private ReportData fetchAppointmentReport(Report report, LocalDateTime from, LocalDateTime to) {
        String token = getBearerToken();
        List<String> headers = List.of("Appointment ID", "Patient ID", "Doctor ID", "Department",
                "Date", "Time", "Status", "Hospital ID");
        List<List<String>> rows = new ArrayList<>();
        Map<String, Object> summary = new LinkedHashMap<>();
        Map<String, Long> statusCounts = new LinkedHashMap<>();

        try {
            List<Map<String, Object>> appts = appointmentClient.get()
                    .uri("/appointments")
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .body(List.class);

            if (appts != null) {
                for (Map<String, Object> a : appts) {
                    String apptDateStr = str(a.get("appointmentDate"));
                    // Simple date range filter (compare string prefix)
                    if (!apptDateStr.isEmpty()) {
                        String fromDate = from.toLocalDate().toString();
                        String toDate = to.toLocalDate().toString();
                        if (apptDateStr.compareTo(fromDate) < 0 || apptDateStr.compareTo(toDate) > 0) continue;
                    }

                    String status = str(a.get("status"));
                    statusCounts.merge(status, 1L, Long::sum);

                    rows.add(List.of(
                            str(a.get("appointmentId")),
                            str(a.get("patientId")),
                            str(a.get("doctorId")),
                            str(a.get("doctorSpecialization")),
                            apptDateStr,
                            str(a.get("appointmentTime")),
                            status,
                            str(a.get("hospitalId"))
                    ));
                }
                summary.put("totalAppointments", rows.size());
                summary.putAll(statusCounts);
                summary.put("period", from.toLocalDate() + " → " + to.toLocalDate());
            }
        } catch (RestClientException e) {
            log.warn("Could not fetch appointments: {}", e.getMessage());
        }

        return new ReportData("Appointment Report (" + from.toLocalDate() + " – " + to.toLocalDate() + ")",
                headers, rows, summary);
    }

    // -------------------------------------------------------------------------
    // BILLING REPORT
    // -------------------------------------------------------------------------
    @SuppressWarnings("unchecked")
    private ReportData fetchBillingReport(Report report, LocalDateTime from, LocalDateTime to) {
        String token = getBearerToken();
        List<String> headers = List.of("Invoice ID", "Invoice Number", "Patient ID", "Total Amount",
                "Paid Amount", "Outstanding", "Status", "Invoice Date", "Hospital ID");
        List<List<String>> rows = new ArrayList<>();
        Map<String, Object> summary = new LinkedHashMap<>();
        double totalRevenue = 0, totalOutstanding = 0;

        try {
            List<Map<String, Object>> invoices = billingClient.get()
                    .uri("/billing/invoices")
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .body(List.class);

            if (invoices != null) {
                for (Map<String, Object> inv : invoices) {
                    String invDateStr = str(inv.get("invoiceDate"));
                    if (!invDateStr.isEmpty()) {
                        String fromDate = from.toLocalDate().toString();
                        String toDate = to.toLocalDate().toString();
                        if (invDateStr.compareTo(fromDate) < 0 || invDateStr.compareTo(toDate) > 0) continue;
                    }

                    double total = parseDouble(inv.get("totalAmount"));
                    double outstanding = parseDouble(inv.get("outstandingAmount"));
                    totalRevenue += total;
                    totalOutstanding += outstanding;

                    rows.add(List.of(
                            str(inv.get("invoiceId")),
                            str(inv.get("invoiceNumber")),
                            str(inv.get("patientId")),
                            String.format("%.2f", total),
                            String.format("%.2f", parseDouble(inv.get("paidAmount"))),
                            String.format("%.2f", outstanding),
                            str(inv.get("status")),
                            invDateStr,
                            str(inv.get("hospitalId"))
                    ));
                }
                summary.put("totalInvoices", rows.size());
                summary.put("totalRevenue", String.format("%.2f", totalRevenue));
                summary.put("totalOutstanding", String.format("%.2f", totalOutstanding));
                summary.put("collectionRate", totalRevenue > 0
                        ? String.format("%.1f%%", (totalRevenue - totalOutstanding) / totalRevenue * 100)
                        : "N/A");
            }
        } catch (RestClientException e) {
            log.warn("Could not fetch invoices: {}", e.getMessage());
        }

        return new ReportData("Billing Report (" + from.toLocalDate() + " – " + to.toLocalDate() + ")",
                headers, rows, summary);
    }

    // -------------------------------------------------------------------------
    // LAB ORDER REPORT
    // -------------------------------------------------------------------------
    @SuppressWarnings("unchecked")
    private ReportData fetchLabOrderReport(Report report, LocalDateTime from, LocalDateTime to) {
        String token = getBearerToken();
        List<String> headers = List.of("Order ID", "Patient ID", "Doctor ID", "Order Type",
                "Urgency", "Status", "Order Date", "Hospital ID", "Sensitivity");
        List<List<String>> rows = new ArrayList<>();
        Map<String, Object> summary = new LinkedHashMap<>();
        Map<String, Long> statusCounts = new LinkedHashMap<>();

        try {
            List<Map<String, Object>> orders = labClient.get()
                    .uri("/lab/orders")
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .body(List.class);

            if (orders != null) {
                for (Map<String, Object> o : orders) {
                    String status = str(o.get("status"));
                    statusCounts.merge(status, 1L, Long::sum);

                    rows.add(List.of(
                            str(o.get("labOrderId")),
                            str(o.get("patientId")),
                            str(o.get("doctorId")),
                            str(o.get("orderType")),
                            str(o.get("urgency")),
                            status,
                            str(o.get("orderDate")),
                            str(o.get("hospitalId")),
                            str(o.get("sensitivityLevel"))
                    ));
                }
                summary.put("totalOrders", rows.size());
                summary.putAll(statusCounts);
            }
        } catch (RestClientException e) {
            log.warn("Could not fetch lab orders: {}", e.getMessage());
        }

        return new ReportData("Lab Order Report (" + from.toLocalDate() + " – " + to.toLocalDate() + ")",
                headers, rows, summary);
    }

    // -------------------------------------------------------------------------
    // PRESCRIPTION REPORT
    // -------------------------------------------------------------------------
    @SuppressWarnings("unchecked")
    private ReportData fetchPrescriptionReport(Report report, LocalDateTime from, LocalDateTime to) {
        String token = getBearerToken();
        List<String> headers = List.of("Prescription ID", "Patient ID", "Doctor ID",
                "Status", "Dispensed At", "Hospital ID");
        List<List<String>> rows = new ArrayList<>();
        Map<String, Object> summary = new LinkedHashMap<>();

        try {
            List<Map<String, Object>> prescriptions = pharmacyClient.get()
                    .uri("/pharmacy/prescriptions")
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .body(List.class);

            if (prescriptions != null) {
                Map<String, Long> statusCounts = new LinkedHashMap<>();
                for (Map<String, Object> p : prescriptions) {
                    String status = str(p.get("status"));
                    statusCounts.merge(status, 1L, Long::sum);
                    rows.add(List.of(
                            str(p.get("prescriptionId")),
                            str(p.get("patientId")),
                            str(p.get("doctorId")),
                            status,
                            str(p.get("dispensedAt")),
                            str(p.get("hospitalId"))
                    ));
                }
                summary.put("totalPrescriptions", rows.size());
                summary.putAll(statusCounts);
            }
        } catch (RestClientException e) {
            log.warn("Could not fetch prescriptions: {}", e.getMessage());
        }

        return new ReportData("Prescription Report (" + from.toLocalDate() + " – " + to.toLocalDate() + ")",
                headers, rows, summary);
    }

    // -------------------------------------------------------------------------
    // USER ACTIVITY (via audit logs, filter by eventType = AUTH/LOGIN)
    // -------------------------------------------------------------------------
    @SuppressWarnings("unchecked")
    private ReportData fetchUserActivity(Report report, LocalDateTime from, LocalDateTime to) {
        String token = getBearerToken();
        List<String> headers = List.of("Event ID", "User", "Action", "Resource", "Result", "IP", "Timestamp");
        List<List<String>> rows = new ArrayList<>();
        Map<String, Object> summary = new LinkedHashMap<>();

        try {
            String url = String.format("/audit/logs/date-range?startDate=%s&endDate=%s&size=500&page=0",
                    encode(from.format(ISO)), encode(to.format(ISO)));
            Map<String, Object> response = auditClient.get()
                    .uri(url)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.containsKey("data")) {
                List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("data");
                Map<String, Long> userActivity = new LinkedHashMap<>();
                for (Map<String, Object> item : items) {
                    String username = str(item.get("username"));
                    userActivity.merge(username, 1L, Long::sum);
                    rows.add(List.of(
                            str(item.get("id")),
                            username,
                            str(item.get("action")),
                            str(item.get("resourceType")),
                            str(item.get("result")),
                            str(item.get("ipAddress")),
                            str(item.get("timestamp"))
                    ));
                }
                summary.put("totalEvents", rows.size());
                summary.put("uniqueUsers", userActivity.size());
                // Top 5 most active users
                userActivity.entrySet().stream()
                        .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                        .limit(5)
                        .forEach(e -> summary.put("top_" + e.getKey(), e.getValue()));
            }
        } catch (RestClientException e) {
            log.warn("Could not fetch user activity: {}", e.getMessage());
        }

        return new ReportData("User Activity Report (" + from.toLocalDate() + " – " + to.toLocalDate() + ")",
                headers, rows, summary);
    }

    // -------------------------------------------------------------------------
    // SUMMARY (multi-domain: appointment + billing + lab counts)
    // -------------------------------------------------------------------------
    private ReportData fetchSummaryReport(Report report, LocalDateTime from, LocalDateTime to) {
        String token = getBearerToken();

        // Fetch counts from each domain
        long appointmentCount = countItems(appointmentClient, "/appointments", token);
        long labOrderCount = countItems(labClient, "/lab/orders", token);
        long invoiceCount = countItems(billingClient, "/billing/invoices", token);
        long patientCount = countItems(patientClient, "/patients", token);

        String periodLabel = switch (report.getType()) {
            case DAILY_SUMMARY -> "Daily";
            case WEEKLY_SUMMARY -> "Weekly";
            default -> "Monthly";
        };

        List<String> headers = List.of("Metric", "Value");
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("Patients", String.valueOf(patientCount)));
        rows.add(List.of("Appointments (" + periodLabel + ")", String.valueOf(appointmentCount)));
        rows.add(List.of("Lab Orders", String.valueOf(labOrderCount)));
        rows.add(List.of("Invoices", String.valueOf(invoiceCount)));
        rows.add(List.of("Report Period Start", from.toLocalDate().toString()));
        rows.add(List.of("Report Period End", to.toLocalDate().toString()));

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("generatedAt", LocalDateTime.now().toString());
        summary.put("type", periodLabel + " Summary");

        return new ReportData(periodLabel + " Summary Report – " + from.toLocalDate(),
                headers, rows, summary);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String getBearerToken() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwt) {
            return jwt.getToken().getTokenValue();
        }
        return "";
    }

    @SuppressWarnings("unchecked")
    private long countItems(RestClient client, String path, String token) {
        try {
            Object body = client.get()
                    .uri(path)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .body(Object.class);
            if (body instanceof List<?> list) return list.size();
            if (body instanceof Map<?, ?> map && map.containsKey("totalItems")) {
                return Long.parseLong(map.get("totalItems").toString());
            }
        } catch (Exception e) {
            log.debug("Count request failed for {}: {}", path, e.getMessage());
        }
        return 0L;
    }

    private ReportData emptyReport(String typeName) {
        return new ReportData("Report: " + typeName,
                List.of("Message"),
                List.of(List.of("No data available")),
                Map.of("note", "Data could not be fetched from downstream services"));
    }

    private String str(Object o) {
        return o == null ? "" : o.toString();
    }

    private double parseDouble(Object o) {
        if (o == null) return 0;
        try { return Double.parseDouble(o.toString()); } catch (NumberFormatException e) { return 0; }
    }

    private String encode(String s) {
        return s.replace(":", "%3A");
    }
}
