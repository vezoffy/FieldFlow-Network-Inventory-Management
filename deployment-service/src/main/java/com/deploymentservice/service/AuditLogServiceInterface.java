package com.deploymentservice.service;

import com.deploymentservice.entity.AuditLog;

import java.time.Instant;
import java.util.List;

public interface AuditLogServiceInterface {
    void logAction(String userId, String actionType, String description);
    List<AuditLog> getFilteredAuditLogs(String userId, String actionType, Instant startTime, Instant endTime);
}
