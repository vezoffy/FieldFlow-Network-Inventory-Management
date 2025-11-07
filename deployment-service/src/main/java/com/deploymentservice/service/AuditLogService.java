package com.deploymentservice.service;

import com.deploymentservice.entity.AuditLog;
import com.deploymentservice.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class AuditLogService implements AuditLogServiceInterface {

    private final AuditLogRepository auditLogRepository;

    @Autowired
    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(String userId, String actionType, String description) {
        AuditLog auditLog = new AuditLog();
        auditLog.setUserId(userId);
        auditLog.setActionType(actionType);
        auditLog.setDescription(description);
        auditLog.setTimestamp(Instant.now());
        auditLogRepository.save(auditLog);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getFilteredAuditLogs(String userId, String actionType, Instant startTime, Instant endTime) {
        if (userId != null && actionType != null && startTime != null && endTime != null) {
            return auditLogRepository.findByUserIdAndActionTypeAndTimestampBetween(userId, actionType, startTime, endTime);
        } else if (userId != null && startTime != null && endTime != null) {
            return auditLogRepository.findByUserIdAndTimestampBetween(userId, startTime, endTime);
        } else if (actionType != null && startTime != null && endTime != null) {
            return auditLogRepository.findByActionTypeAndTimestampBetween(actionType, startTime, endTime);
        } else if (startTime != null && endTime != null) {
            return auditLogRepository.findByTimestampBetween(startTime, endTime);
        } else if (userId != null) {
            return auditLogRepository.findByUserId(userId);
        } else if (actionType != null) {
            return auditLogRepository.findByActionType(actionType);
        } else {
            return auditLogRepository.findAll();
        }
    }
}
