package com.deploymentservice.service;

import com.deploymentservice.entity.AuditLog;
import com.deploymentservice.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceUnitTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogService auditLogService;

    // Test data
    private AuditLog auditLog;
    private List<AuditLog> auditLogList;
    private String testUserId;
    private String testActionType;
    private Instant testStartTime;
    private Instant testEndTime;

    @BeforeEach
    void setUp() {
        auditLog = new AuditLog();
        auditLog.setId(1L);
        auditLog.setUserId("admin");
        auditLog.setActionType("TEST_ACTION");
        auditLog.setDescription("This is a test log.");
        auditLog.setTimestamp(Instant.now());

        auditLogList = Collections.singletonList(auditLog);

        testUserId = "user123";
        testActionType = "LOGIN";
        testStartTime = Instant.now().minus(1, ChronoUnit.DAYS);
        testEndTime = Instant.now();
    }

    @Test
    void logAction_ShouldSaveCorrectlyFormattedLog() {
        // Arrange
        String userId = "testUser";
        String actionType = "CREATE";
        String description = "User created a new item";
        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);

        // Act
        auditLogService.logAction(userId, actionType, description);

        // Assert
        verify(auditLogRepository).save(auditLogCaptor.capture());
        AuditLog capturedLog = auditLogCaptor.getValue();

        assertEquals(userId, capturedLog.getUserId());
        assertEquals(actionType, capturedLog.getActionType());
        assertEquals(description, capturedLog.getDescription());
        assertNotNull(capturedLog.getTimestamp()); // Check that a timestamp was set
    }

    @Test
    void getFilteredAuditLogs_AllParametersProvided() {
        // Arrange
        when(auditLogRepository.findByUserIdAndActionTypeAndTimestampBetween(testUserId, testActionType, testStartTime, testEndTime))
                .thenReturn(auditLogList);

        // Act
        List<AuditLog> result = auditLogService.getFilteredAuditLogs(testUserId, testActionType, testStartTime, testEndTime);

        // Assert
        assertEquals(1, result.size());
        verify(auditLogRepository).findByUserIdAndActionTypeAndTimestampBetween(testUserId, testActionType, testStartTime, testEndTime);
        verifyNoMoreInteractions(auditLogRepository); // Ensure no other methods were called
    }

    @Test
    void getFilteredAuditLogs_UserAndTimestampProvided() {
        // Arrange
        when(auditLogRepository.findByUserIdAndTimestampBetween(testUserId, testStartTime, testEndTime))
                .thenReturn(auditLogList);

        // Act
        List<AuditLog> result = auditLogService.getFilteredAuditLogs(testUserId, null, testStartTime, testEndTime);

        // Assert
        assertEquals(1, result.size());
        verify(auditLogRepository).findByUserIdAndTimestampBetween(testUserId, testStartTime, testEndTime);
        verifyNoMoreInteractions(auditLogRepository);
    }

    @Test
    void getFilteredAuditLogs_ActionTypeAndTimestampProvided() {
        // Arrange
        when(auditLogRepository.findByActionTypeAndTimestampBetween(testActionType, testStartTime, testEndTime))
                .thenReturn(auditLogList);

        // Act
        List<AuditLog> result = auditLogService.getFilteredAuditLogs(null, testActionType, testStartTime, testEndTime);

        // Assert
        assertEquals(1, result.size());
        verify(auditLogRepository).findByActionTypeAndTimestampBetween(testActionType, testStartTime, testEndTime);
        verifyNoMoreInteractions(auditLogRepository);
    }

    @Test
    void getFilteredAuditLogs_TimestampOnlyProvided() {
        // Arrange
        when(auditLogRepository.findByTimestampBetween(testStartTime, testEndTime))
                .thenReturn(auditLogList);

        // Act
        List<AuditLog> result = auditLogService.getFilteredAuditLogs(null, null, testStartTime, testEndTime);

        // Assert
        assertEquals(1, result.size());
        verify(auditLogRepository).findByTimestampBetween(testStartTime, testEndTime);
        verifyNoMoreInteractions(auditLogRepository);
    }

    @Test
    void getFilteredAuditLogs_UserOnlyProvided() {
        // Arrange
        when(auditLogRepository.findByUserId(testUserId))
                .thenReturn(auditLogList);

        // Act
        List<AuditLog> result = auditLogService.getFilteredAuditLogs(testUserId, null, null, null);

        // Assert
        assertEquals(1, result.size());
        verify(auditLogRepository).findByUserId(testUserId);
        verifyNoMoreInteractions(auditLogRepository);
    }

    @Test
    void getFilteredAuditLogs_ActionTypeOnlyProvided() {
        // Arrange
        when(auditLogRepository.findByActionType(testActionType))
                .thenReturn(auditLogList);

        // Act
        List<AuditLog> result = auditLogService.getFilteredAuditLogs(null, testActionType, null, null);

        // Assert
        assertEquals(1, result.size());
        verify(auditLogRepository).findByActionType(testActionType);
        verifyNoMoreInteractions(auditLogRepository);
    }

    @Test
    void getFilteredAuditLogs_NoParametersProvided() {
        // Arrange
        when(auditLogRepository.findAll())
                .thenReturn(auditLogList);

        // Act
        List<AuditLog> result = auditLogService.getFilteredAuditLogs(null, null, null, null);

        // Assert
        assertEquals(1, result.size());
        verify(auditLogRepository).findAll();
        verifyNoMoreInteractions(auditLogRepository);
    }
}