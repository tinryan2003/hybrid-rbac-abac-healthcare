package org.vgu.auditservice.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vgu.auditservice.dto.AuditEvent;
import org.vgu.auditservice.dto.AuditLogRequest;
import org.vgu.auditservice.dto.AuditLogResponse;
import org.vgu.auditservice.enums.AuditEventType;
import org.vgu.auditservice.enums.AuditSeverity;
import org.vgu.auditservice.enums.ResourceType;
import org.vgu.auditservice.model.AuditLog;
import org.vgu.auditservice.repository.AuditLogRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Create an audit log entry
     */
    @Transactional
    public AuditLogResponse createAuditLog(AuditLogRequest request) {
        log.info("Creating audit log: eventType={}, userId={}, action={}",
                request.getEventType(), request.getUserId(), request.getAction());

        AuditLog auditLog = AuditLog.builder()
                .eventType(request.getEventType())
                .severity(request.getSeverity() != null ? request.getSeverity() : AuditSeverity.MEDIUM)
                .employeeNumber(request.getEmployeeNumber())
                .email(request.getEmail())
                .keycloakId(request.getKeycloakId())
                .userRole(request.getUserRole())
                .resourceType(request.getResourceType())
                .resourceId(request.getResourceId())
                .action(request.getAction())
                .description(request.getDescription())
                .ipAddress(request.getIpAddress())
                .userAgent(request.getUserAgent())
                .success(request.getSuccess() != null ? request.getSuccess() : true)
                .failureReason(request.getFailureReason())
                .metadata(request.getMetadata())
                .beforeState(request.getBeforeState())
                .afterState(request.getAfterState())
                .sessionId(request.getSessionId())
                .correlationId(request.getCorrelationId())
                // Legacy fields (for backward compatibility)
                .userId(request.getUserId())
                .username(request.getUsername())
                .build();

        // Calculate tamper-evident hash before saving
        calculateAndSetHash(auditLog);

        AuditLog saved = auditLogRepository.save(auditLog);
        log.debug("Audit log created with ID: {}, hash: {}", saved.getId(), saved.getCurrHash());

        return mapToResponse(saved);
    }

    /**
     * Process audit event from RabbitMQ
     */
    @Transactional
    public void processAuditEvent(AuditEvent event) {
        log.info("Processing audit event: eventType={}, action={}",
                event.getEventType(), event.getAction());

        AuditLog auditLog = AuditLog.builder()
                .eventType(event.getEventType())
                .severity(determineSeverity(event))
                .employeeNumber(event.getEmployeeNumber())
                .email(event.getEmail())
                .keycloakId(event.getKeycloakId())
                .userRole(event.getUserRole()) // RBAC
                .jobTitle(event.getJobTitle()) // ABAC
                .resourceType(event.getResourceType())
                .resourceId(event.getResourceId())
                .action(event.getAction())
                .description(event.getDescription())
                .ipAddress(event.getIpAddress()) // Set IP address from event
                .success(event.getSuccess() != null ? event.getSuccess() : true)
                .failureReason(event.getFailureReason())
                .metadata(event.getMetadata())
                .beforeState(event.getBeforeState())
                .afterState(event.getAfterState())
                .correlationId(event.getCorrelationId())
                // Legacy fields (for backward compatibility)
                .userId(event.getUserId())
                .username(event.getUsername())
                .build();

        // Calculate tamper-evident hash before saving
        calculateAndSetHash(auditLog);

        auditLogRepository.save(auditLog);
        log.debug("Audit event processed and saved with hash: {}", auditLog.getCurrHash());
    }

    /**
     * Get audit logs by employee number
     */
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAuditLogsByEmployeeNumber(String employeeNumber, Pageable pageable) {
        log.info("Fetching audit logs for employee number: {}", employeeNumber);
        Page<AuditLog> logs = auditLogRepository.findByEmployeeNumber(employeeNumber, pageable);
        return logs.map(this::mapToResponse);
    }

    /**
     * Get audit logs by email
     */
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAuditLogsByEmail(String email, Pageable pageable) {
        log.info("Fetching audit logs for email: {}", email);
        Page<AuditLog> logs = auditLogRepository.findByEmail(email, pageable);
        return logs.map(this::mapToResponse);
    }

    /**
     * Get audit logs by keycloak ID
     */
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAuditLogsByKeycloakId(String keycloakId, Pageable pageable) {
        log.info("Fetching audit logs for keycloak ID: {}", keycloakId);
        Page<AuditLog> logs = auditLogRepository.findByKeycloakId(keycloakId, pageable);
        return logs.map(this::mapToResponse);
    }

    /**
     * Get audit logs by user ID (legacy - kept for backward compatibility)
     */
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAuditLogsByUserId(Long userId, Pageable pageable) {
        log.info("Fetching audit logs for user ID: {}", userId);
        Page<AuditLog> logs = auditLogRepository.findByUserId(userId, pageable);
        return logs.map(this::mapToResponse);
    }

    /**
     * Get audit logs by username (legacy - kept for backward compatibility)
     */
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAuditLogsByUsername(String username, Pageable pageable) {
        log.info("Fetching audit logs for username: {}", username);
        Page<AuditLog> logs = auditLogRepository.findByUsername(username, pageable);
        return logs.map(this::mapToResponse);
    }

    /**
     * Get audit logs by resource
     */
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAuditLogsByResource(
            ResourceType resourceType, Long resourceId, Pageable pageable) {
        log.info("Fetching audit logs for resource: type={}, id={}", resourceType, resourceId);
        Page<AuditLog> logs = auditLogRepository.findByResourceTypeAndResourceId(
                resourceType, resourceId, pageable);
        return logs.map(this::mapToResponse);
    }

    /**
     * Get audit logs by event type
     */
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAuditLogsByEventType(
            AuditEventType eventType, Pageable pageable) {
        log.info("Fetching audit logs for event type: {}", eventType);
        Page<AuditLog> logs = auditLogRepository.findByEventType(eventType, pageable);
        return logs.map(this::mapToResponse);
    }

    /**
     * Get audit logs by date range
     */
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAuditLogsByDateRange(
            LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        log.info("Fetching audit logs for date range: {} to {}", startDate, endDate);
        Page<AuditLog> logs = auditLogRepository.findByDateRange(startDate, endDate, pageable);
        return logs.map(this::mapToResponse);
    }

    /**
     * Get failed audit logs
     */
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getFailedActions(Pageable pageable) {
        log.info("Fetching failed audit logs");
        Page<AuditLog> logs = auditLogRepository.findFailedActions(pageable);
        return logs.map(this::mapToResponse);
    }

    /**
     * Get high severity events
     */
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getHighSeverityEvents(Pageable pageable) {
        log.info("Fetching high severity audit logs");
        Page<AuditLog> logs = auditLogRepository.findHighSeverityEvents(pageable);
        return logs.map(this::mapToResponse);
    }

    /**
     * Search audit logs with multiple filters
     */
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> searchAuditLogs(
            String employeeNumber,
            String email,
            String keycloakId,
            AuditEventType eventType,
            ResourceType resourceType,
            AuditSeverity severity,
            Boolean success,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable) {
        log.info("Searching audit logs with filters");
        Page<AuditLog> logs = auditLogRepository.searchAuditLogs(
                employeeNumber, email, keycloakId, eventType, resourceType, severity, success, startDate, endDate, pageable);
        return logs.map(this::mapToResponse);
    }

    /**
     * Get audit logs by correlation ID (for tracing related events)
     */
    @Transactional(readOnly = true)
    public List<AuditLogResponse> getAuditLogsByCorrelationId(String correlationId) {
        log.info("Fetching audit logs for correlation ID: {}", correlationId);
        List<AuditLog> logs = auditLogRepository.findByCorrelationId(correlationId);
        return logs.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get audit log by ID
     */
    @Transactional(readOnly = true)
    public AuditLogResponse getAuditLogById(Long id) {
        log.info("Fetching audit log by ID: {}", id);
        AuditLog log = auditLogRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Audit log not found with id: " + id));
        return mapToResponse(log);
    }

    /**
     * Get all audit logs (paginated)
     */
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAllAuditLogs(Pageable pageable) {
        log.info("Fetching all audit logs");
        Page<AuditLog> logs = auditLogRepository.findAll(pageable);
        return logs.map(this::mapToResponse);
    }

    /**
     * Count events by employee number
     */
    @Transactional(readOnly = true)
    public long countEventsByEmployeeNumber(String employeeNumber) {
        return auditLogRepository.countByEmployeeNumber(employeeNumber);
    }

    @Transactional(readOnly = true)
    public long countEventsByEmail(String email) {
        return auditLogRepository.countByEmail(email);
    }

    @Transactional(readOnly = true)
    public long countEventsByKeycloakId(String keycloakId) {
        return auditLogRepository.countByKeycloakId(keycloakId);
    }

    /**
     * Count failed events by employee number
     */
    @Transactional(readOnly = true)
    public long countFailedEventsByEmployeeNumber(String employeeNumber) {
        return auditLogRepository.countByEmployeeNumberAndSuccess(employeeNumber, false);
    }

    @Transactional(readOnly = true)
    public long countFailedEventsByEmail(String email) {
        return auditLogRepository.countByEmailAndSuccess(email, false);
    }

    @Transactional(readOnly = true)
    public long countFailedEventsByKeycloakId(String keycloakId) {
        return auditLogRepository.countByKeycloakIdAndSuccess(keycloakId, false);
    }

    /**
     * Legacy methods (kept for backward compatibility)
     */
    @Transactional(readOnly = true)
    public long countEventsByUser(Long userId) {
        return auditLogRepository.countByUserId(userId);
    }

    @Transactional(readOnly = true)
    public long countFailedEventsByUser(Long userId) {
        return auditLogRepository.countByUserIdAndSuccess(userId, false);
    }

    /**
     * Delete audit log by ID
     * WARNING: Deleting audit logs breaks the tamper-evident hash chain
     * Only use for testing or data cleanup
     */
    @Transactional
    public void deleteAuditLog(Long id) {
        log.warn("Deleting audit log with ID: {} - This will break the hash chain!", id);
        if (!auditLogRepository.existsById(id)) {
            throw new RuntimeException("Audit log not found with id: " + id);
        }
        auditLogRepository.deleteById(id);
        log.info("Audit log deleted: {}", id);
    }

    /**
     * Delete multiple audit logs by IDs
     */
    @Transactional
    public void deleteAuditLogs(List<Long> ids) {
        log.warn("Deleting {} audit logs - This will break the hash chain!", ids.size());
        auditLogRepository.deleteAllById(ids);
        log.info("Deleted {} audit logs", ids.size());
    }

    /**
     * Delete all audit logs (DANGEROUS - use with caution)
     */
    @Transactional
    public void deleteAllAuditLogs() {
        log.warn("DELETING ALL AUDIT LOGS - This will completely break the hash chain!");
        auditLogRepository.deleteAll();
        log.warn("All audit logs have been deleted");
    }

    /**
     * Delete audit logs by date range
     */
    @Transactional
    public void deleteAuditLogsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        log.warn("Deleting audit logs from {} to {} - This will break the hash chain!", startDate, endDate);
        List<AuditLog> logs = auditLogRepository.findByDateRange(startDate, endDate, Pageable.unpaged()).getContent();
        auditLogRepository.deleteAll(logs);
        log.info("Deleted {} audit logs in date range", logs.size());
    }

    /**
     * Determine severity based on event characteristics
     */
    private AuditSeverity determineSeverity(AuditEvent event) {
        // High/Critical severity for security events
        if (event.getEventType() != null) {
            String eventName = event.getEventType().name();
            if (eventName.contains("DENIED") || eventName.contains("FAILED") ||
                    eventName.contains("SUSPICIOUS") || eventName.contains("CRITICAL")) {
                return AuditSeverity.CRITICAL;
            }
            if (eventName.contains("APPROVED") || eventName.contains("REJECTED") ||
                    eventName.contains("DELETED")) {
                return AuditSeverity.HIGH;
            }
        }

        // Failed operations are high severity
        if (Boolean.FALSE.equals(event.getSuccess())) {
            return AuditSeverity.HIGH;
        }

        return AuditSeverity.MEDIUM;
    }

    /**
     * Map AuditLog entity to response DTO
     */
    private AuditLogResponse mapToResponse(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .eventType(log.getEventType())
                .severity(log.getSeverity())
                .employeeNumber(log.getEmployeeNumber())
                .email(log.getEmail())
                .keycloakId(log.getKeycloakId())
                .userRole(log.getUserRole()) // RBAC
                .jobTitle(log.getJobTitle()) // ABAC
                .resourceType(log.getResourceType())
                .resourceId(log.getResourceId())
                // Legacy fields (for backward compatibility)
                .userId(log.getUserId())
                .username(log.getUsername())
                .action(log.getAction())
                .description(log.getDescription())
                .ipAddress(log.getIpAddress())
                .success(log.getSuccess())
                .failureReason(log.getFailureReason())
                .metadata(log.getMetadata())
                .timestamp(log.getTimestamp())
                .correlationId(log.getCorrelationId())
                .prevHash(log.getPrevHash())
                .currHash(log.getCurrHash())
                .build();
    }

    /**
     * Calculate tamper-evident hash for audit log entry
     * Implements hash chain (like blockchain) to detect tampering
     * 
     * Formula: currHash =
     * SHA256(timestamp|username|action|success|failureReason|prevHash)
     * 
     * If any previous entry is modified, all subsequent hashes will be invalid
     */
    private void calculateAndSetHash(AuditLog auditLog) {
        try {
            // 1. Get the previous log entry to retrieve its hash
            AuditLog lastLog = auditLogRepository.findTopByOrderByTimestampDesc();
            String prevHash = (lastLog != null && lastLog.getCurrHash() != null)
                    ? lastLog.getCurrHash()
                    : "GENESIS_HASH"; // First entry uses genesis hash

            auditLog.setPrevHash(prevHash);

            // 2. Build data string to hash (include all critical fields)
            // Order matters - must be consistent for verification
            // Using new fields: employeeNumber, email, keycloakId
            StringBuilder dataToHash = new StringBuilder();
            dataToHash.append(auditLog.getTimestamp() != null ? auditLog.getTimestamp().toString() : "")
                    .append("|")
                    .append(auditLog.getEmployeeNumber() != null ? auditLog.getEmployeeNumber() : "")
                    .append("|")
                    .append(auditLog.getEmail() != null ? auditLog.getEmail() : "")
                    .append("|")
                    .append(auditLog.getKeycloakId() != null ? auditLog.getKeycloakId() : "")
                    .append("|")
                    .append(auditLog.getAction() != null ? auditLog.getAction() : "")
                    .append("|")
                    .append(auditLog.getSuccess() != null ? auditLog.getSuccess() : "")
                    .append("|")
                    .append(auditLog.getFailureReason() != null ? auditLog.getFailureReason() : "")
                    .append("|")
                    .append(auditLog.getEventType() != null ? auditLog.getEventType().name() : "")
                    .append("|")
                    .append(auditLog.getResourceType() != null ? auditLog.getResourceType().name() : "")
                    .append("|")
                    .append(auditLog.getResourceId() != null ? auditLog.getResourceId() : "")
                    .append("|")
                    .append(prevHash); // Link to previous entry

            // 3. Calculate SHA-256 hash
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(dataToHash.toString().getBytes(StandardCharsets.UTF_8));
            String currHash = HexFormat.of().formatHex(hashBytes);

            auditLog.setCurrHash(currHash);

            log.debug("Calculated hash for audit log: prevHash={}, currHash={}",
                    prevHash.substring(0, Math.min(16, prevHash.length())) + "...",
                    currHash.substring(0, Math.min(16, currHash.length())) + "...");

        } catch (Exception e) {
            log.error("Failed to calculate hash for audit log", e);
            // Set default hash to indicate error
            auditLog.setPrevHash("ERROR");
            auditLog.setCurrHash("ERROR");
        }
    }

    /**
     * Verify integrity of audit log chain
     * Recalculates hashes for all logs and detects tampering
     * 
     * @return Map with verification results:
     *         - valid: true if all hashes are valid
     *         - totalEntries: total number of entries checked
     *         - violations: list of entries with invalid hashes
     */
    @Transactional(readOnly = true)
    public Map<String, Object> verifyIntegrity() {
        log.info("Verifying audit log chain integrity");

        List<AuditLog> allLogs = auditLogRepository.findAll(Sort.by("timestamp").ascending());
        List<Map<String, Object>> violations = new java.util.ArrayList<>();
        int totalEntries = allLogs.size();
        boolean allValid = true;

        String previousHash = "GENESIS_HASH";

        for (int i = 0; i < allLogs.size(); i++) {
            AuditLog auditLogEntry = allLogs.get(i);

            // Check if prevHash matches previous entry's currHash
            if (i > 0) {
                if (!previousHash.equals(auditLogEntry.getPrevHash())) {
                    allValid = false;
                    Map<String, Object> violation = new java.util.HashMap<>();
                    violation.put("entryId", auditLogEntry.getId());
                    violation.put("expectedPrevHash", previousHash);
                    violation.put("actualPrevHash", auditLogEntry.getPrevHash());
                    violation.put("issue", "Previous hash mismatch");
                    violations.add(violation);
                    log.warn("Hash chain violation detected at entry ID {}: expected prevHash={}, actual={}",
                            auditLogEntry.getId(), previousHash, auditLogEntry.getPrevHash());
                }
            } else {
                // First entry should have GENESIS_HASH
                if (!"GENESIS_HASH".equals(auditLogEntry.getPrevHash())) {
                    allValid = false;
                    Map<String, Object> violation = new java.util.HashMap<>();
                    violation.put("entryId", auditLogEntry.getId());
                    violation.put("expectedPrevHash", "GENESIS_HASH");
                    violation.put("actualPrevHash", auditLogEntry.getPrevHash());
                    violation.put("issue", "First entry should have GENESIS_HASH");
                    violations.add(violation);
                }
            }

            // Recalculate hash for current entry
            String recalculatedHash = recalculateHash(auditLogEntry, previousHash);

            // Check if currHash matches recalculated hash
            if (!recalculatedHash.equals(auditLogEntry.getCurrHash())) {
                allValid = false;
                Map<String, Object> violation = new java.util.HashMap<>();
                violation.put("entryId", auditLogEntry.getId());
                violation.put("expectedCurrHash", recalculatedHash);
                violation.put("actualCurrHash", auditLogEntry.getCurrHash());
                violation.put("issue", "Current hash mismatch - data may have been tampered");
                violations.add(violation);
                log.warn("Hash mismatch detected at entry ID {}: expected={}, actual={}",
                        auditLogEntry.getId(), recalculatedHash, auditLogEntry.getCurrHash());
            }

            previousHash = auditLogEntry.getCurrHash();
        }

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("valid", allValid);
        result.put("totalEntries", totalEntries);
        result.put("violations", violations);
        result.put("violationCount", violations.size());
        result.put("timestamp", LocalDateTime.now());

        if (allValid) {
            log.info("✅ Audit log chain integrity verified: {} entries, all valid", totalEntries);
        } else {
            log.warn("⚠️ Audit log chain integrity check failed: {} violations detected out of {} entries",
                    violations.size(), totalEntries);
        }

        return result;
    }

    /**
     * Recalculate hash for a given audit log entry
     * Uses the same formula as calculateAndSetHash()
     */
    private String recalculateHash(AuditLog auditLog, String prevHash) {
        try {
            // Use the same formula as calculateAndSetHash() with new fields
            StringBuilder dataToHash = new StringBuilder();
            dataToHash.append(auditLog.getTimestamp() != null ? auditLog.getTimestamp().toString() : "")
                    .append("|")
                    .append(auditLog.getEmployeeNumber() != null ? auditLog.getEmployeeNumber() : "")
                    .append("|")
                    .append(auditLog.getEmail() != null ? auditLog.getEmail() : "")
                    .append("|")
                    .append(auditLog.getKeycloakId() != null ? auditLog.getKeycloakId() : "")
                    .append("|")
                    .append(auditLog.getAction() != null ? auditLog.getAction() : "")
                    .append("|")
                    .append(auditLog.getSuccess() != null ? auditLog.getSuccess() : "")
                    .append("|")
                    .append(auditLog.getFailureReason() != null ? auditLog.getFailureReason() : "")
                    .append("|")
                    .append(auditLog.getEventType() != null ? auditLog.getEventType().name() : "")
                    .append("|")
                    .append(auditLog.getResourceType() != null ? auditLog.getResourceType().name() : "")
                    .append("|")
                    .append(auditLog.getResourceId() != null ? auditLog.getResourceId() : "")
                    .append("|")
                    .append(prevHash);

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(dataToHash.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (Exception e) {
            log.error("Failed to recalculate hash for audit log ID {}", auditLog.getId(), e);
            return "ERROR";
        }
    }
}
