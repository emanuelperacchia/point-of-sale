package com.pos.system.controller;

import com.pos.system.dto.request.AccountingAccountRequest;
import com.pos.system.dto.response.AccountingAccountResponse;
import com.pos.system.dto.response.JournalEntryResponse;
import com.pos.system.dto.response.TrialBalanceResponse;
import com.pos.system.dto.request.AccountingAccountRequest;
import com.pos.system.dto.response.AccountingAccountResponse;
import com.pos.system.dto.response.JournalEntryResponse;
import com.pos.system.dto.response.TrialBalanceResponse;
import com.pos.system.service.AccountingAccountService;
import com.pos.system.service.AccountingService;
import com.pos.system.service.ExcelExportService;
import com.pos.system.service.TrialBalanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/accounting")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Accounting", description = "Módulo contable: plan de cuentas, asientos, balance")
public class AccountingController {

    private final AccountingAccountService accountService;
    private final AccountingService accountingService;
    private final TrialBalanceService trialBalanceService;
    private final ExcelExportService excelExportService;

    // ==================== PLAN DE CUENTAS ====================

    @PostMapping("/accounts")
    @Operation(summary = "Crear cuenta contable")
    public ResponseEntity<AccountingAccountResponse> createAccount(@Valid @RequestBody AccountingAccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.create(request));
    }

    @GetMapping("/accounts")
    @Operation(summary = "Listar cuentas contables activas")
    public ResponseEntity<List<AccountingAccountResponse>> listAccounts() {
        return ResponseEntity.ok(accountService.listActive());
    }

    @GetMapping("/accounts/{id}")
    @Operation(summary = "Obtener cuenta contable por ID")
    public ResponseEntity<AccountingAccountResponse> getAccount(@PathVariable Long id) {
        return ResponseEntity.ok(accountService.getById(id));
    }

    @PutMapping("/accounts/{id}")
    @Operation(summary = "Actualizar cuenta contable")
    public ResponseEntity<AccountingAccountResponse> updateAccount(@PathVariable Long id,
                                                                    @Valid @RequestBody AccountingAccountRequest request) {
        return ResponseEntity.ok(accountService.update(id, request));
    }

    // ==================== LIBRO DIARIO ====================

    @GetMapping("/journal")
    @Operation(summary = "Libro diario", description = "Retorna los asientos contables del período")
    public ResponseEntity<List<JournalEntryResponse>> getJournal(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(accountingService.getJournal(desde, hasta));
    }

    // ==================== BALANCE DE COMPROBACIÓN ====================

    @GetMapping("/trial-balance")
    @Operation(summary = "Balance de comprobación", description = "Saldo de cada cuenta a la fecha")
    public ResponseEntity<TrialBalanceResponse> getTrialBalance(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(trialBalanceService.calculate(fecha));
    }

    // ==================== EXPORTACIÓN ====================

    @GetMapping("/journal/export")
    @Operation(summary = "Exportar libro diario", description = "Exporta asientos contables del período a Excel")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'CONTADOR')")
    public ResponseEntity<byte[]> exportJournal(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {

        List<JournalEntryResponse> entries = accountingService.getJournal(desde, hasta);

        // Aplanar: una fila por línea de asiento
        List<JournalLineFlat> rows = new ArrayList<>();
        for (JournalEntryResponse entry : entries) {
            for (JournalEntryResponse.JournalLineResponse line : entry.getLineas()) {
                rows.add(new JournalLineFlat(
                        entry.getFecha(),
                        entry.getDescripcion(),
                        entry.getReferenciaType(),
                        line.getCuentaCodigo(),
                        line.getCuentaNombre(),
                        "DEBE".equals(line.getTipo()) ? line.getMonto() : java.math.BigDecimal.ZERO,
                        "HABER".equals(line.getTipo()) ? line.getMonto() : java.math.BigDecimal.ZERO,
                        entry.getEstado()
                ));
            }
        }

        byte[] excel = excelExportService.generate(
                "Libro Diario",
                List.of("Fecha", "Glosa", "Tipo Ref.", "Código Cuenta", "Nombre Cuenta",
                        "Debe", "Haber", "Estado"),
                rows,
                (row, idx, item) -> {
                    ExcelExportService.cellString(row, 0, item.fecha().toString());
                    ExcelExportService.cellString(row, 1, item.glosa());
                    ExcelExportService.cellString(row, 2, item.tipoReferencia());
                    ExcelExportService.cellString(row, 3, item.cuentaCodigo());
                    ExcelExportService.cellString(row, 4, item.cuentaNombre());
                    ExcelExportService.cellNumeric(row, 5, item.debe());
                    ExcelExportService.cellNumeric(row, 6, item.haber());
                    ExcelExportService.cellString(row, 7, item.estado());
                }
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=libro-diario.xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excel);
    }

    /**
     * DTO interno para filas planas del libro diario exportable.
     */
    private record JournalLineFlat(
            LocalDate fecha,
            String glosa,
            String tipoReferencia,
            String cuentaCodigo,
            String cuentaNombre,
            java.math.BigDecimal debe,
            java.math.BigDecimal haber,
            String estado
    ) {}
}
