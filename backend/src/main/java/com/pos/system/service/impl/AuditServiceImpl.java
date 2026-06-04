package com.pos.system.service.impl;

import com.pos.system.dto.response.AuditLogResponse;
import com.pos.system.entity.AuditLog;
import com.pos.system.entity.AuditLog.AuditAction;
import com.pos.system.repository.AuditLogRepository;
import com.pos.system.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(Long userId, AuditAction action, String entityType, Long entityId, String oldValue, String newValue, String ipAddress, String userAgent) {
        AuditLog auditLog = AuditLog.builder()
                .userId(userId)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .oldValue(oldValue)
                .newValue(newValue)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();
        auditLogRepository.save(auditLog);
        log.debug("Audit log created: {} - {} - {}", userId, action, entityType);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logLogin(Long userId, String ipAddress, String userAgent) {
        log(userId, AuditAction.LOGIN, "USER", userId, null, null, ipAddress, userAgent);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logLogout(Long userId, String ipAddress) {
        log(userId, AuditAction.LOGOUT, "USER", userId, null, null, ipAddress, null);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logFailedLogin(String email, String ipAddress) {
        AuditLog auditLog = AuditLog.builder()
                .userId(null)
                .action(AuditAction.FAILED_LOGIN)
                .entityType("USER")
                .newValue(email)
                .ipAddress(ipAddress)
                .build();
        auditLogRepository.save(auditLog);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAuditLogs(Long userId, AuditAction action, String entityType, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        Page<AuditLog> logs = auditLogRepository.findWithFilters(userId, action, entityType, startDate, endDate, pageable);
        return logs.map(this::mapToResponse);
    }

    private AuditLogResponse mapToResponse(AuditLog auditLog) {
        return AuditLogResponse.builder()
                .id(auditLog.getId())
                .userId(auditLog.getUserId())
                .action(auditLog.getAction())
                .entityType(auditLog.getEntityType())
                .entityId(auditLog.getEntityId())
                .oldValue(auditLog.getOldValue())
                .newValue(auditLog.getNewValue())
                .ipAddress(auditLog.getIpAddress())
                .userAgent(auditLog.getUserAgent())
                .createdAt(auditLog.getCreatedAt())
                .build();
    }
}
