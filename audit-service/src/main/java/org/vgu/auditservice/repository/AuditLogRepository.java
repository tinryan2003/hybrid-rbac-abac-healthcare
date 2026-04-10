package org.vgu.auditservice.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.vgu.auditservice.enums.AuditEventType;
import org.vgu.auditservice.enums.AuditSeverity;
import org.vgu.auditservice.enums.ResourceType;
import org.vgu.auditservice.model.AuditLog;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    // Find by employee number
    List<AuditLog> findByEmployeeNumber(String employeeNumber);
    Page<AuditLog> findByEmployeeNumber(String employeeNumber, Pageable pageable);

    // Find by email
    List<AuditLog> findByEmail(String email);
    Page<AuditLog> findByEmail(String email, Pageable pageable);

    // Find by keycloak ID
    List<AuditLog> findByKeycloakId(String keycloakId);
    Page<AuditLog> findByKeycloakId(String keycloakId, Pageable pageable);

    // Legacy methods (kept for backward compatibility)
    List<AuditLog> findByUserId(Long userId);
    Page<AuditLog> findByUserId(Long userId, Pageable pageable);
    List<AuditLog> findByUsername(String username);
    Page<AuditLog> findByUsername(String username, Pageable pageable);

    // Find by event type
    List<AuditLog> findByEventType(AuditEventType eventType);

    Page<AuditLog> findByEventType(AuditEventType eventType, Pageable pageable);

    // Find by resource
    List<AuditLog> findByResourceTypeAndResourceId(ResourceType resourceType, Long resourceId);

    Page<AuditLog> findByResourceTypeAndResourceId(ResourceType resourceType, Long resourceId, Pageable pageable);

    // Find by severity
    Page<AuditLog> findBySeverity(AuditSeverity severity, Pageable pageable);

    // Find by success status
    Page<AuditLog> findBySuccess(Boolean success, Pageable pageable);

    // Find by date range
    @Query("SELECT a FROM AuditLog a WHERE a.timestamp BETWEEN :startDate AND :endDate")
    Page<AuditLog> findByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    // Find by employee number and date range
    @Query("SELECT a FROM AuditLog a WHERE a.employeeNumber = :employeeNumber AND a.timestamp BETWEEN :startDate AND :endDate")
    Page<AuditLog> findByEmployeeNumberAndDateRange(
            @Param("employeeNumber") String employeeNumber,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    // Find by email and date range
    @Query("SELECT a FROM AuditLog a WHERE a.email = :email AND a.timestamp BETWEEN :startDate AND :endDate")
    Page<AuditLog> findByEmailAndDateRange(
            @Param("email") String email,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    // Find by keycloak ID and date range
    @Query("SELECT a FROM AuditLog a WHERE a.keycloakId = :keycloakId AND a.timestamp BETWEEN :startDate AND :endDate")
    Page<AuditLog> findByKeycloakIdAndDateRange(
            @Param("keycloakId") String keycloakId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    // Legacy method (kept for backward compatibility)
    @Query("SELECT a FROM AuditLog a WHERE a.userId = :userId AND a.timestamp BETWEEN :startDate AND :endDate")
    Page<AuditLog> findByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    // Find failed actions
    @Query("SELECT a FROM AuditLog a WHERE a.success = false ORDER BY a.timestamp DESC")
    Page<AuditLog> findFailedActions(Pageable pageable);

    // Find high/critical severity events
    @Query("SELECT a FROM AuditLog a WHERE a.severity IN ('HIGH', 'CRITICAL') ORDER BY a.timestamp DESC")
    Page<AuditLog> findHighSeverityEvents(Pageable pageable);

    // Find by correlation ID (for tracing related events)
    List<AuditLog> findByCorrelationId(String correlationId);

    // Complex search
    @Query("SELECT a FROM AuditLog a WHERE " +
            "(:employeeNumber IS NULL OR a.employeeNumber = :employeeNumber) AND " +
            "(:email IS NULL OR a.email = :email) AND " +
            "(:keycloakId IS NULL OR a.keycloakId = :keycloakId) AND " +
            "(:eventType IS NULL OR a.eventType = :eventType) AND " +
            "(:resourceType IS NULL OR a.resourceType = :resourceType) AND " +
            "(:severity IS NULL OR a.severity = :severity) AND " +
            "(:success IS NULL OR a.success = :success) AND " +
            "(:startDate IS NULL OR a.timestamp >= :startDate) AND " +
            "(:endDate IS NULL OR a.timestamp <= :endDate)")
    Page<AuditLog> searchAuditLogs(
            @Param("employeeNumber") String employeeNumber,
            @Param("email") String email,
            @Param("keycloakId") String keycloakId,
            @Param("eventType") AuditEventType eventType,
            @Param("resourceType") ResourceType resourceType,
            @Param("severity") AuditSeverity severity,
            @Param("success") Boolean success,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    // Count events by employee number
    long countByEmployeeNumber(String employeeNumber);
    long countByEmployeeNumberAndSuccess(String employeeNumber, Boolean success);

    // Count events by email
    long countByEmail(String email);
    long countByEmailAndSuccess(String email, Boolean success);

    // Count events by keycloak ID
    long countByKeycloakId(String keycloakId);
    long countByKeycloakIdAndSuccess(String keycloakId, Boolean success);

    // Legacy methods (kept for backward compatibility)
    long countByUserId(Long userId);
    long countByUserIdAndSuccess(Long userId, Boolean success);

    // Find the last audit log entry (for tamper-evident hash chain)
    // Used to get the previous hash when creating a new log entry
    AuditLog findTopByOrderByTimestampDesc();
}
