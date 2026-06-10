package com.pos.system.controller;

import com.pos.system.dto.request.CreateEvaluationRequest;
import com.pos.system.dto.request.EvaluationTemplateRequest;
import com.pos.system.dto.response.EvaluationTemplateResponse;
import com.pos.system.dto.response.PerformanceEvaluationResponse;
import com.pos.system.security.UserDetailsImpl;
import com.pos.system.service.EvaluationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/evaluations")
@RequiredArgsConstructor
public class EvaluationController {

    private final EvaluationService evaluationService;

    // ── Templates ───────────────────────────────────────────────────

    @GetMapping("/templates")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public ResponseEntity<List<EvaluationTemplateResponse>> listTemplates() {
        return ResponseEntity.ok(evaluationService.listarTemplates());
    }

    @PostMapping("/templates")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EvaluationTemplateResponse> createTemplate(
            @Valid @RequestBody EvaluationTemplateRequest request) {
        return ResponseEntity.ok(evaluationService.crearTemplate(request));
    }

    // ── Evaluations ─────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasRole('GERENTE')")
    public ResponseEntity<PerformanceEvaluationResponse> create(
            @Valid @RequestBody CreateEvaluationRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(evaluationService.crearEvaluacion(request, userDetails.getUser().getId()));
    }

    @PutMapping("/{id}/finalize")
    @PreAuthorize("hasRole('GERENTE')")
    public ResponseEntity<PerformanceEvaluationResponse> finalize(@PathVariable Long id) {
        return ResponseEntity.ok(evaluationService.finalizarEvaluacion(id));
    }

    @GetMapping("/employees/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public ResponseEntity<List<PerformanceEvaluationResponse>> getByEmployee(@PathVariable Long employeeId) {
        return ResponseEntity.ok(evaluationService.getEvaluationsByEmployee(employeeId));
    }

    @GetMapping("/{id}/calculate")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public ResponseEntity<java.util.Map<String, java.math.BigDecimal>> calculateScore(@PathVariable Long id) {
        return ResponseEntity.ok(java.util.Map.of("puntuacionFinal", evaluationService.calcularPuntuacion(id)));
    }
}
