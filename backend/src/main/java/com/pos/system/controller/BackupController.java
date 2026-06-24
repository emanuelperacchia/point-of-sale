package com.pos.system.controller;

import com.pos.system.entity.SystemBackup;
import com.pos.system.security.UserDetailsImpl;
import com.pos.system.service.BackupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/backups")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Backups", description = "Gestión de backups automáticos (solo ADMIN)")
public class BackupController {

    private final BackupService backupService;

    @GetMapping
    @Operation(summary = "Listar backups disponibles")
    public ResponseEntity<List<SystemBackup>> listar() {
        return ResponseEntity.ok(backupService.listarBackups());
    }

    @PostMapping("/trigger")
    @Operation(summary = "Forzar backup manual")
    public ResponseEntity<SystemBackup> triggerBackup(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(backupService.ejecutarBackup("MANUAL", userDetails.getId()));
    }
}
