package com.pos.system.service;

import com.pos.system.dto.response.AuditLogResponse;
import com.pos.system.entity.AuditLog.AuditAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface AuditService {
    void log(Long userId, AuditAction action, String entityType, Long entityId, String oldValue, String newValue, String ipAddress, String userAgent);
    void logLogin(Long userId, String ipAddress, String userAgent);
    void logLogout(Long userId, String ipAddress);
    void logFailedLogin(String email, String ipAddress);
    Page<AuditLogResponse> getAuditLogs(Long userId, AuditAction action, String entityType, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
}
