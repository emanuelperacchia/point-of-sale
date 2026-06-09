package com.pos.system.controller;

import com.pos.system.dto.request.CreateExpenseFromStatementRequest;
import com.pos.system.dto.request.ManualMatchRequest;
import com.pos.system.dto.response.BankReconciliationResponse;
import com.pos.system.dto.response.BankStatementResponse;
import com.pos.system.entity.BankReconciliation;
import com.pos.system.entity.BankStatement;
import com.pos.system.repository.BankReconciliationRepository;
import com.pos.system.repository.BankStatementRepository;
import com.pos.system.security.UserDetailsImpl;
import com.pos.system.service.BankStatementImportService;
import com.pos.system.service.ExcelExportService;
import com.pos.system.service.ReconciliationMatchingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/bank-reconciliation")
@RequiredArgsConstructor
public class BankReconciliationController {

    private final BankReconciliationRepository reconciliationRepository;
    private final BankStatementRepository statementRepository;
    private final BankStatementImportService importService;
    private final ReconciliationMatchingService matchingService;
    private final ExcelExportService excelExportService;

    @PostMapping("/import")
    @PreAuthorize("hasAnyRole('ADMIN','CONTADOR')")
    public ResponseEntity<BankReconciliationResponse> importCsv(
            @RequestParam("file") MultipartFile file,
            @RequestParam("periodo") String periodo) {
        BankReconciliation rec = importService.importCsv(file, periodo);
        return ResponseEntity.ok(mapReconciliation(rec));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','CONTADOR')")
    public ResponseEntity<BankReconciliationResponse> getSummary(
            @RequestParam("periodo") String periodo) {
        BankReconciliation rec = reconciliationRepository.findByPeriodo(periodo)
                .orElse(null);
        if (rec == null) {
            return ResponseEntity.ok(BankReconciliationResponse.builder()
                    .periodo(periodo)
                    .totalExtracto(BigDecimal.ZERO)
                    .totalSistema(BigDecimal.ZERO)
                    .diferencia(BigDecimal.ZERO)
                    .estado("NO_INICIADA")
                    .totalLineas(0)
                    .conciliadas(0)
                    .pendientes(0)
                    .build());
        }
        return ResponseEntity.ok(mapReconciliation(rec));
    }

    @GetMapping("/{id}/statements")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','CONTADOR')")
    public ResponseEntity<List<BankStatementResponse>> getStatements(@PathVariable Long id) {
        List<BankStatement> statements = statementRepository.findByReconciliationId(id);
        return ResponseEntity.ok(statements.stream().map(this::mapStatement).toList());
    }

    @PostMapping("/{id}/auto-match")
    @PreAuthorize("hasAnyRole('ADMIN','CONTADOR')")
    public ResponseEntity<String> autoMatch(@PathVariable Long id) {
        int matched = matchingService.autoMatch(id);
        return ResponseEntity.ok("Conciliadas " + matched + " líneas automáticamente");
    }

    @PostMapping("/statements/{id}/manual-match")
    @PreAuthorize("hasAnyRole('ADMIN','CONTADOR')")
    public ResponseEntity<BankStatementResponse> manualMatch(
            @PathVariable Long id,
            @Valid @RequestBody ManualMatchRequest request) {
        BankStatement st = matchingService.manualMatch(id, request.getPaymentId(), request.getTipo());
        return ResponseEntity.ok(mapStatement(st));
    }

    @PostMapping("/statements/{id}/create-expense")
    @PreAuthorize("hasAnyRole('ADMIN','CONTADOR')")
    public ResponseEntity<BankStatementResponse> createExpense(
            @PathVariable Long id,
            @Valid @RequestBody CreateExpenseFromStatementRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        // Ensure the statement ID in path matches the request
        request.setStatementId(id);
        BankStatement st = matchingService.createExpenseFromStatement(request, userDetails.getUser().getId());
        return ResponseEntity.ok(mapStatement(st));
    }

    @GetMapping("/summary/export")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','CONTADOR')")
    public ResponseEntity<byte[]> exportSummary(@RequestParam("periodo") String periodo) {
        BankReconciliation rec = reconciliationRepository.findByPeriodo(periodo).orElse(null);
        if (rec == null) {
            return ResponseEntity.noContent().build();
        }

        List<BankStatement> statements = statementRepository.findByReconciliationId(rec.getId());

        List<String> headers = List.of("Fecha", "Descripción", "Monto", "Tipo", "Estado", "Observación");

        byte[] workbook = excelExportService.generate(
                "Conciliación " + periodo,
                headers,
                statements,
                (row, index, st) -> {
                    ExcelExportService.cellString(row, 0, st.getFecha().toString());
                    ExcelExportService.cellString(row, 1, st.getDescripcion());
                    ExcelExportService.cellNumeric(row, 2, st.getMonto());
                    ExcelExportService.cellString(row, 3, st.getTipo().name());
                    ExcelExportService.cellString(row, 4, st.getEstado().name());
                    ExcelExportService.cellString(row, 5, st.getObservacion());
                },
                wb -> {
                    Sheet sheet = wb.getSheetAt(0);
                    int lastRow = statements.size() + 1;
                    Row totalRow = sheet.createRow(lastRow);
                    ExcelExportService.cellString(totalRow, 0, "DIFERENCIA");
                    ExcelExportService.cellNumeric(totalRow, 2, rec.getDiferencia());
                }
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=conciliacion-" + periodo + ".xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(workbook);
    }

    private BankReconciliationResponse mapReconciliation(BankReconciliation rec) {
        long total = statementRepository.countByReconciliationIdAndEstado(
                rec.getId(), BankStatement.EstadoConciliacion.PENDIENTE);
        long conciliadas = statementRepository.countByReconciliationIdAndEstado(
                rec.getId(), BankStatement.EstadoConciliacion.CONCILIADO);
        long pendientes = total - conciliadas;

        return BankReconciliationResponse.builder()
                .id(rec.getId())
                .periodo(rec.getPeriodo())
                .totalExtracto(rec.getTotalExtracto())
                .totalSistema(rec.getTotalSistema())
                .diferencia(rec.getDiferencia())
                .estado(rec.getEstado().name())
                .totalLineas(total + conciliadas)
                .conciliadas(conciliadas)
                .pendientes(pendientes)
                .build();
    }

    private BankStatementResponse mapStatement(BankStatement st) {
        return BankStatementResponse.builder()
                .id(st.getId())
                .reconciliationId(st.getReconciliationId())
                .fecha(st.getFecha())
                .descripcion(st.getDescripcion())
                .monto(st.getMonto())
                .tipo(st.getTipo().name())
                .estado(st.getEstado().name())
                .paymentId(st.getPaymentId())
                .observacion(st.getObservacion())
                .build();
    }
}
