package org.vgu.auditservice.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.vgu.auditservice.dto.AuditLogRequest;
import org.vgu.auditservice.dto.AuditLogResponse;
import org.vgu.auditservice.enums.AuditEventType;
import org.vgu.auditservice.enums.AuditSeverity;
import org.vgu.auditservice.enums.ResourceType;
import org.vgu.auditservice.service.AuditService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "audit-service");
        return ResponseEntity.ok(response);
    }

    /**
     * Create an audit log entry manually
     * Typically used for testing or manual logging
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> createAuditLog(@RequestBody AuditLogRequest request) {
        log.info("Manual audit log creation requested");

        AuditLogResponse response = auditService.createAuditLog(request);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Audit log created successfully");
        result.put("data", response);

        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    /**
     * Get all audit logs (paginated)
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getAllAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "timestamp") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        log.info("Fetching all audit logs: page={}, size={}", page, size);

        Sort sort = sortDirection.equalsIgnoreCase("ASC")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<AuditLogResponse> logs = auditService.getAllAuditLogs(pageable);

        return buildPageResponse(logs);
    }

    /**
     * Get audit log by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getAuditLogById(@PathVariable Long id) {
        log.info("Fetching audit log by ID: {}", id);

        AuditLogResponse response = auditService.getAuditLogById(id);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", response);

        return ResponseEntity.ok(result);
    }

    /**
     * Get audit logs by employee number
     */
    @GetMapping("/employee/{employeeNumber}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getAuditLogsByEmployeeNumber(
            @PathVariable String employeeNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("Fetching audit logs for employee number: {}", employeeNumber);

        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        Page<AuditLogResponse> logs = auditService.getAuditLogsByEmployeeNumber(employeeNumber, pageable);

        return buildPageResponse(logs);
    }

    /**
     * Get audit logs by email
     */
    @GetMapping("/email/{email}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getAuditLogsByEmail(
            @PathVariable String email,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("Fetching audit logs for email: {}", email);

        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        Page<AuditLogResponse> logs = auditService.getAuditLogsByEmail(email, pageable);

        return buildPageResponse(logs);
    }

    /**
     * Get audit logs by keycloak ID
     */
    @GetMapping("/keycloak/{keycloakId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getAuditLogsByKeycloakId(
            @PathVariable String keycloakId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("Fetching audit logs for keycloak ID: {}", keycloakId);

        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        Page<AuditLogResponse> logs = auditService.getAuditLogsByKeycloakId(keycloakId, pageable);

        return buildPageResponse(logs);
    }

    /**
     * Get audit logs by user ID (legacy - kept for backward compatibility)
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getAuditLogsByUserId(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("Fetching audit logs for user ID: {}", userId);

        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        Page<AuditLogResponse> logs = auditService.getAuditLogsByUserId(userId, pageable);

        return buildPageResponse(logs);
    }

    /**
     * Get audit logs by username (legacy - kept for backward compatibility)
     */
    @GetMapping("/username/{username}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getAuditLogsByUsername(
            @PathVariable String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("Fetching audit logs for username: {}", username);

        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        Page<AuditLogResponse> logs = auditService.getAuditLogsByUsername(username, pageable);

        return buildPageResponse(logs);
    }

    /**
     * Get audit logs by resource
     */
    @GetMapping("/resource/{resourceType}/{resourceId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getAuditLogsByResource(
            @PathVariable ResourceType resourceType,
            @PathVariable Long resourceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("Fetching audit logs for resource: {}:{}", resourceType, resourceId);

        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        Page<AuditLogResponse> logs = auditService.getAuditLogsByResource(resourceType, resourceId, pageable);

        return buildPageResponse(logs);
    }

    /**
     * Get audit logs by event type
     */
    @GetMapping("/event-type/{eventType}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getAuditLogsByEventType(
            @PathVariable AuditEventType eventType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("Fetching audit logs for event type: {}", eventType);

        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        Page<AuditLogResponse> logs = auditService.getAuditLogsByEventType(eventType, pageable);

        return buildPageResponse(logs);
    }

    /**
     * Get audit logs by date range
     */
    @GetMapping("/date-range")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getAuditLogsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("Fetching audit logs for date range: {} to {}", startDate, endDate);

        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        Page<AuditLogResponse> logs = auditService.getAuditLogsByDateRange(startDate, endDate, pageable);

        return buildPageResponse(logs);
    }

    /**
     * Get failed actions
     */
    @GetMapping("/failed")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getFailedActions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("Fetching failed audit logs");

        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        Page<AuditLogResponse> logs = auditService.getFailedActions(pageable);

        return buildPageResponse(logs);
    }

    /**
     * Get high severity events
     */
    @GetMapping("/high-severity")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getHighSeverityEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("Fetching high severity audit logs");

        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        Page<AuditLogResponse> logs = auditService.getHighSeverityEvents(pageable);

        return buildPageResponse(logs);
    }

    /**
     * Search audit logs with multiple filters
     */
    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> searchAuditLogs(
            @RequestParam(required = false) String employeeNumber,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String keycloakId,
            @RequestParam(required = false) AuditEventType eventType,
            @RequestParam(required = false) ResourceType resourceType,
            @RequestParam(required = false) AuditSeverity severity,
            @RequestParam(required = false) Boolean success,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("Searching audit logs with filters");

        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        Page<AuditLogResponse> logs = auditService.searchAuditLogs(
                employeeNumber, email, keycloakId, eventType, resourceType, severity, success, startDate, endDate, pageable);

        return buildPageResponse(logs);
    }

    /**
     * Verify integrity of audit log chain (Tamper-evident verification)
     * Recalculates all hashes and detects any tampering
     * 
     * This endpoint demonstrates the tamper-evident logging feature:
     * - Scans all audit logs in chronological order
     * - Recalculates hash for each entry
     * - Detects if any entry has been modified
     * - Returns list of violations if any
     */
    @GetMapping("/verify-integrity")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> verifyIntegrity() {
        log.info("Verifying audit log chain integrity");
        
        Map<String, Object> result = auditService.verifyIntegrity();
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", result);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get audit logs by correlation ID
     */
    @GetMapping("/correlation/{correlationId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getAuditLogsByCorrelationId(
            @PathVariable String correlationId) {

        log.info("Fetching audit logs for correlation ID: {}", correlationId);

        List<AuditLogResponse> logs = auditService.getAuditLogsByCorrelationId(correlationId);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", logs);
        result.put("count", logs.size());

        return ResponseEntity.ok(result);
    }

    /**
     * Get employee statistics by employee number
     */
    @GetMapping("/stats/employee/{employeeNumber}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getEmployeeStatistics(@PathVariable String employeeNumber) {
        log.info("Fetching statistics for employee number: {}", employeeNumber);

        long totalEvents = auditService.countEventsByEmployeeNumber(employeeNumber);
        long failedEvents = auditService.countFailedEventsByEmployeeNumber(employeeNumber);

        Map<String, Object> stats = new HashMap<>();
        stats.put("employeeNumber", employeeNumber);
        stats.put("totalEvents", totalEvents);
        stats.put("failedEvents", failedEvents);
        stats.put("successfulEvents", totalEvents - failedEvents);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", stats);

        return ResponseEntity.ok(response);
    }

    /**
     * Get employee statistics by email
     */
    @GetMapping("/stats/email/{email}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getEmailStatistics(@PathVariable String email) {
        log.info("Fetching statistics for email: {}", email);

        long totalEvents = auditService.countEventsByEmail(email);
        long failedEvents = auditService.countFailedEventsByEmail(email);

        Map<String, Object> stats = new HashMap<>();
        stats.put("email", email);
        stats.put("totalEvents", totalEvents);
        stats.put("failedEvents", failedEvents);
        stats.put("successfulEvents", totalEvents - failedEvents);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", stats);

        return ResponseEntity.ok(response);
    }

    /**
     * Get employee statistics by keycloak ID
     */
    @GetMapping("/stats/keycloak/{keycloakId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getKeycloakStatistics(@PathVariable String keycloakId) {
        log.info("Fetching statistics for keycloak ID: {}", keycloakId);

        long totalEvents = auditService.countEventsByKeycloakId(keycloakId);
        long failedEvents = auditService.countFailedEventsByKeycloakId(keycloakId);

        Map<String, Object> stats = new HashMap<>();
        stats.put("keycloakId", keycloakId);
        stats.put("totalEvents", totalEvents);
        stats.put("failedEvents", failedEvents);
        stats.put("successfulEvents", totalEvents - failedEvents);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", stats);

        return ResponseEntity.ok(response);
    }

    /**
     * Get user statistics (legacy - kept for backward compatibility)
     */
    @GetMapping("/stats/user/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getUserStatistics(@PathVariable Long userId) {
        log.info("Fetching statistics for user ID: {}", userId);

        long totalEvents = auditService.countEventsByUser(userId);
        long failedEvents = auditService.countFailedEventsByUser(userId);

        Map<String, Object> stats = new HashMap<>();
        stats.put("userId", userId);
        stats.put("totalEvents", totalEvents);
        stats.put("failedEvents", failedEvents);
        stats.put("successRate",
                totalEvents > 0 ? String.format("%.2f%%", (totalEvents - failedEvents) * 100.0 / totalEvents) : "N/A");

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", stats);

        return ResponseEntity.ok(result);
    }

    /**
     * Delete audit log by ID
     * WARNING: This will break the tamper-evident hash chain
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> deleteAuditLog(@PathVariable Long id) {
        log.warn("Delete audit log requested for ID: {}", id);
        
        try {
            auditService.deleteAuditLog(id);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Audit log deleted successfully");
            result.put("id", id);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to delete audit log: {}", e.getMessage(), e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "Failed to delete audit log: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    /**
     * Delete multiple audit logs by IDs
     */
    @DeleteMapping("/batch")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> deleteAuditLogs(@RequestBody List<Long> ids) {
        log.warn("Batch delete audit logs requested for {} IDs", ids.size());
        
        try {
            auditService.deleteAuditLogs(ids);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", String.format("Deleted %d audit logs successfully", ids.size()));
            result.put("deletedCount", ids.size());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to delete audit logs: {}", e.getMessage(), e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "Failed to delete audit logs: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    /**
     * Delete all audit logs (DANGEROUS - use with extreme caution)
     */
    @DeleteMapping("/all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> deleteAllAuditLogs() {
        log.error("DELETE ALL AUDIT LOGS requested - This is a dangerous operation!");
        
        try {
            auditService.deleteAllAuditLogs();
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "All audit logs deleted successfully");
            result.put("warning", "Hash chain has been broken!");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to delete all audit logs: {}", e.getMessage(), e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "Failed to delete all audit logs: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    /**
     * Delete audit logs by date range
     */
    @DeleteMapping("/date-range")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> deleteAuditLogsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        log.warn("Delete audit logs by date range requested: {} to {}", startDate, endDate);
        
        try {
            auditService.deleteAuditLogsByDateRange(startDate, endDate);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Audit logs deleted successfully for the specified date range");
            result.put("startDate", startDate);
            result.put("endDate", endDate);
            result.put("warning", "Hash chain may be broken!");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to delete audit logs by date range: {}", e.getMessage(), e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "Failed to delete audit logs: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    /**
     * Helper method to build paginated response
     */
    private ResponseEntity<Map<String, Object>> buildPageResponse(Page<AuditLogResponse> page) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", page.getContent());
        result.put("currentPage", page.getNumber());
        result.put("totalItems", page.getTotalElements());
        result.put("totalPages", page.getTotalPages());
        result.put("pageSize", page.getSize());

        return ResponseEntity.ok(result);
    }
}
