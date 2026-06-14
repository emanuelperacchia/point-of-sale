package com.pos.system.controller;

import com.pos.system.dto.response.PayrollAdjustmentResponse;
import com.pos.system.dto.response.PayrollResponse;
import com.pos.system.service.BankFileExportService;
import com.pos.system.service.PayrollPdfService;
import com.pos.system.service.PayrollService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/payroll")
@RequiredArgsConstructor
public class PayrollController {

    private final PayrollService payrollService;
    private final PayrollPdfService payrollPdfService;
    private final BankFileExportService bankFileExportService;

    // ── CRUD ────────────────────────────────────────────────────────

    @PostMapping("/calculate/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','CONTADOR')")
    public ResponseEntity<PayrollResponse> calculateAndSave(
            @PathVariable Long employeeId,
            @RequestParam int mes,
            @RequestParam int anio) {
        return ResponseEntity.ok(payrollService.calcularYGuardar(employeeId, mes, anio));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','CONTADOR','EMPLEADO')")
    public ResponseEntity<PayrollResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(payrollService.obtenerPorId(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','CONTADOR')")
    public ResponseEntity<List<PayrollResponse>> list(
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false) Long employeeId) {
        if (employeeId != null && anio != null) {
            return ResponseEntity.ok(payrollService.listarPorEmpleadoYAnio(employeeId, anio));
        }
        if (mes != null && anio != null) {
            return ResponseEntity.ok(payrollService.listarPorPeriodo(mes, anio));
        }
        return ResponseEntity.ok(payrollService.listarPorPeriodo(
                java.time.LocalDate.now().getMonthValue(),
                java.time.LocalDate.now().getYear()));
    }

    // ── Approval ────────────────────────────────────────────────────

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public ResponseEntity<PayrollResponse> approve(
            @PathVariable Long id,
            @RequestParam Long aprobadoPor) {
        return ResponseEntity.ok(payrollService.aprobar(id, aprobadoPor));
    }

    // ── Adjustments ─────────────────────────────────────────────────

    @PostMapping("/{payrollId}/adjustments")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','CONTADOR')")
    public ResponseEntity<PayrollAdjustmentResponse> addAdjustment(
            @PathVariable Long payrollId,
            @RequestParam String concepto,
            @RequestParam BigDecimal monto,
            @RequestParam(required = false) String justificacion,
            @RequestParam Long creadoPor) {
        return ResponseEntity.ok(payrollService.agregarAjuste(payrollId, concepto, monto, justificacion, creadoPor));
    }

    @GetMapping("/{payrollId}/adjustments")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','CONTADOR')")
    public ResponseEntity<List<PayrollAdjustmentResponse>> listAdjustments(
            @PathVariable Long payrollId) {
        return ResponseEntity.ok(payrollService.listarAjustes(payrollId));
    }

    // ── PDF Export ──────────────────────────────────────────────────

    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','CONTADOR','EMPLEADO')")
    public ResponseEntity<byte[]> generatePdf(@PathVariable Long id) {
        // El servicio necesita el nombre del empleado y CUIL
        // Por ahora usamos datos básicos del payroll
        var payroll = payrollService.obtenerPorId(id);
        byte[] pdf = payrollPdfService.generateReceipt(
                payrollService.obtenerEntity(id),
                "Empleado ID: " + payroll.getEmployeeId(),
                null);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=recibo-sueldo-" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    // ── Bank Export ─────────────────────────────────────────────────

    @PostMapping("/export/csv")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','CONTADOR')")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam int mes,
            @RequestParam int anio) {
        var payrolls = payrollService.listarPorPeriodo(mes, anio)
                .stream().map(r -> payrollService.obtenerEntity(r.getId())).toList();
        byte[] csv = bankFileExportService.generateCsv(payrolls);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=sueldos-" + mes + "-" + anio + ".csv")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }
}
