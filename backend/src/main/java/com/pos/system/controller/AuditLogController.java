package com.pos.system.controller;

import com.pos.system.dto.response.AuditLogResponse;
import com.pos.system.entity.AuditLog.AuditAction;
import com.pos.system.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
@Tag(name = "Auditoría", description = "Logs de auditoría del sistema")
@SecurityRequirement(name = "bearerAuth")
public class AuditLogController {

    private final AuditService auditService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('GERENTE')")
    @Operation(summary = "Listar logs de auditoría", description = "Obtiene logs con filtros y paginación")
    public ResponseEntity<Page<AuditLogResponse>> getAuditLogs(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<AuditLogResponse> logs = auditService.getAuditLogs(userId, action, entityType, startDate, endDate, pageable);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/export")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Exportar logs a CSV", description = "Descarga un CSV con los logs filtrados")
    public ResponseEntity<String> exportAuditLogs(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        Page<AuditLogResponse> logs = auditService.getAuditLogs(userId, action, entityType, startDate, endDate, PageRequest.of(0, 10000));

        StringBuilder csv = new StringBuilder();
        csv.append("ID,User ID,Action,Entity Type,Entity ID,IP Address,Created At\n");

        for (AuditLogResponse log : logs.getContent()) {
            csv.append(String.format("%d,%s,%s,%s,%s,%s,%s\n",
                    log.getId(),
                    log.getUserId(),
                    log.getAction(),
                    log.getEntityType(),
                    log.getEntityId(),
                    log.getIpAddress(),
                    log.getCreatedAt()));
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=audit_logs.csv")
                .contentType(MediaType.TEXT_PLAIN)
                .body(csv.toString());
    }

    @GetMapping("/actions")
    @Operation(summary = "Listar tipos de acciones", description = "Obtiene todos los tipos de acciones disponibles")
    public ResponseEntity<List<String>> getAuditActions() {
        return ResponseEntity.ok(Arrays.stream(AuditAction.values()).map(Enum::name).collect(Collectors.toList()));
    }
}
