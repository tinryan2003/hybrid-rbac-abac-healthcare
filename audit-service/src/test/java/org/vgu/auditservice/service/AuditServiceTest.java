package org.vgu.auditservice.service;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.vgu.auditservice.dto.AuditEvent;
import org.vgu.auditservice.dto.AuditLogRequest;
import org.vgu.auditservice.dto.AuditLogResponse;
import org.vgu.auditservice.enums.AuditEventType;
import org.vgu.auditservice.enums.AuditSeverity;
import org.vgu.auditservice.enums.ResourceType;
import org.vgu.auditservice.model.AuditLog;
import org.vgu.auditservice.repository.AuditLogRepository;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditService auditService;

    private AuditLog sampleAuditLog;

    @BeforeEach
    void setUp() {
        sampleAuditLog = AuditLog.builder()
                .id(1L)
                .eventType(AuditEventType.ACCOUNT_APPROVED)
                .severity(AuditSeverity.HIGH)
                .userId(100L)
                .username("test.user")
                .userRole("ROLE_MANAGER")
                .resourceType(ResourceType.ACCOUNT)
                .resourceId(200L)
                .action("APPROVE_ACCOUNT")
                .description("Account approved by manager")
                .success(true)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Test
    void testCreateAuditLog() {
        // Arrange
        AuditLogRequest request = AuditLogRequest.builder()
                .eventType(AuditEventType.ACCOUNT_APPROVED)
                .severity(AuditSeverity.HIGH)
                .userId(100L)
                .username("test.user")
                .action("APPROVE_ACCOUNT")
                .description("Account approved")
                .success(true)
                .build();

        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(sampleAuditLog);

        // Act
        AuditLogResponse response = auditService.createAuditLog(request);

        // Assert
        assertNotNull(response);
        assertEquals(AuditEventType.ACCOUNT_APPROVED, response.getEventType());
        assertEquals("test.user", response.getUsername());
        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    @Test
    void testProcessAuditEvent() {
        // Arrange
        AuditEvent event = AuditEvent.builder()
                .eventType(AuditEventType.TRANSACTION_CREATED)
                .userId(100L)
                .username("test.user")
                .resourceType(ResourceType.TRANSACTION)
                .resourceId(300L)
                .action("CREATE_TRANSACTION")
                .description("Transaction created")
                .success(true)
                .timestamp(LocalDateTime.now())
                .build();

        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(sampleAuditLog);

        // Act
        auditService.processAuditEvent(event);

        // Assert
        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    @Test
    void testGetAuditLogById() {
        // Arrange
        when(auditLogRepository.findById(1L)).thenReturn(java.util.Optional.of(sampleAuditLog));

        // Act
        AuditLogResponse response = auditService.getAuditLogById(1L);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(AuditEventType.ACCOUNT_APPROVED, response.getEventType());
        verify(auditLogRepository, times(1)).findById(1L);
    }

    @Test
    void testGetAuditLogByIdNotFound() {
        // Arrange
        when(auditLogRepository.findById(999L)).thenReturn(java.util.Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> auditService.getAuditLogById(999L));
        verify(auditLogRepository, times(1)).findById(999L);
    }

    @Test
    void testCountEventsByUser() {
        // Arrange
        when(auditLogRepository.countByUserId(100L)).thenReturn(42L);

        // Act
        long count = auditService.countEventsByUser(100L);

        // Assert
        assertEquals(42L, count);
        verify(auditLogRepository, times(1)).countByUserId(100L);
    }

    @Test
    void testCountFailedEventsByUser() {
        // Arrange
        when(auditLogRepository.countByUserIdAndSuccess(100L, false)).thenReturn(5L);

        // Act
        long count = auditService.countFailedEventsByUser(100L);

        // Assert
        assertEquals(5L, count);
        verify(auditLogRepository, times(1)).countByUserIdAndSuccess(100L, false);
    }
}
