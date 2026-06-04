package com.pos.system.dto.response;

import com.pos.system.entity.AuditLog.AuditAction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuditLogResponse {
    private Long id;
    private Long userId;
    private AuditAction action;
    private String entityType;
    private Long entityId;
    private String oldValue;
    private String newValue;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime createdAt;
}
