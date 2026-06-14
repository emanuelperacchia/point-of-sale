package com.pos.system.controller;

import com.pos.system.dto.request.CommissionSchemeRequest;
import com.pos.system.dto.response.CommissionResultResponse;
import com.pos.system.dto.response.CommissionSchemeResponse;
import com.pos.system.service.CommissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/commissions")
@RequiredArgsConstructor
public class CommissionController {

    private final CommissionService commissionService;

    @GetMapping("/schemes")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public ResponseEntity<List<CommissionSchemeResponse>> listSchemes() {
        return ResponseEntity.ok(commissionService.listarEsquemas());
    }

    @PostMapping("/schemes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommissionSchemeResponse> createScheme(
            @Valid @RequestBody CommissionSchemeRequest request) {
        return ResponseEntity.ok(commissionService.crearEsquema(request));
    }

    @PostMapping("/calculate")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public ResponseEntity<CommissionResultResponse> calculate(
            @RequestParam Long employeeId,
            @RequestParam int mes,
            @RequestParam int anio) {
        return ResponseEntity.ok(commissionService.calculate(employeeId, mes, anio));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public ResponseEntity<CommissionResultResponse> summary(
            @RequestParam Long employeeId,
            @RequestParam int mes,
            @RequestParam int anio) {
        return ResponseEntity.ok(commissionService.getSummary(employeeId, mes, anio));
    }

    @GetMapping("/ranking")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public ResponseEntity<List<CommissionResultResponse>> ranking(
            @RequestParam int mes,
            @RequestParam int anio) {
        return ResponseEntity.ok(commissionService.getRanking(mes, anio));
    }
}
