package com.pos.system.controller;

import com.pos.system.dto.request.ReceivablePaymentRequest;
import com.pos.system.dto.response.AgingReportResponse;
import com.pos.system.dto.response.ReceivablePaymentResponse;
import com.pos.system.dto.response.ReceivableResponse;
import com.pos.system.security.UserDetailsImpl;
import com.pos.system.service.AgingReportService;
import com.pos.system.service.ExcelExportService;
import com.pos.system.service.ReceivableService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/receivables")
@RequiredArgsConstructor
public class ReceivableController {

    private final ReceivableService receivableService;
    private final AgingReportService agingReportService;
    private final ExcelExportService excelExportService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','CONTADOR')")
    public ResponseEntity<Page<ReceivableResponse>> list(
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String fechaVencimiento,
            Pageable pageable) {
        return ResponseEntity.ok(receivableService.findByFilters(clienteId, estado, fechaVencimiento, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','CONTADOR')")
    public ResponseEntity<ReceivableResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(receivableService.getById(id));
    }

    @GetMapping("/{id}/payments")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','CONTADOR','CAJERO')")
    public ResponseEntity<List<ReceivablePaymentResponse>> getPayments(@PathVariable Long id) {
        return ResponseEntity.ok(receivableService.getPayments(id));
    }

    @PostMapping("/{id}/payments")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','CAJERO')")
    public ResponseEntity<ReceivableResponse> registerPayment(
            @PathVariable Long id,
            @Valid @RequestBody ReceivablePaymentRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(receivableService.registrarPago(id, request, userDetails.getUser().getId()));
    }

    @GetMapping("/aging-report")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','CONTADOR')")
    public ResponseEntity<AgingReportResponse> agingReport() {
        return ResponseEntity.ok(agingReportService.generateReport());
    }

    @GetMapping("/aging-report/export")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','CONTADOR')")
    public ResponseEntity<byte[]> exportAgingReport() {
        AgingReportResponse report = agingReportService.generateReport();

        List<String> headers = List.of("Cliente", "Documento", "Corriente", "1-30 días", "31-60 días", "61-90 días", "+90 días");
        List<AgingReportResponse.TramoCliente> rows = report.getPorCliente();

        byte[] workbook = excelExportService.generate(
                "Reporte de Antigüedad",
                headers,
                rows,
                (row, index, tc) -> {
                    ExcelExportService.cellString(row, 0, tc.getClientName());
                    ExcelExportService.cellString(row, 1, tc.getClientDocument());
                    ExcelExportService.cellString(row, 2, "$" + tc.getCorriente());
                    ExcelExportService.cellString(row, 3, "$" + tc.getTramo1a30());
                    ExcelExportService.cellString(row, 4, "$" + tc.getTramo31a60());
                    ExcelExportService.cellString(row, 5, "$" + tc.getTramo61a90());
                    ExcelExportService.cellString(row, 6, "$" + tc.getMasDe90());
                },
                wb -> {
                    Sheet sheet = wb.getSheetAt(0);
                    int lastRow = rows.size() + 1;
                    Row totalRow = sheet.createRow(lastRow);
                    var g = report.getResumenGeneral();
                    ExcelExportService.cellString(totalRow, 0, "TOTAL");
                    ExcelExportService.cellString(totalRow, 2, "$" + g.getCorriente());
                    ExcelExportService.cellString(totalRow, 3, "$" + g.getTramo1a30());
                    ExcelExportService.cellString(totalRow, 4, "$" + g.getTramo31a60());
                    ExcelExportService.cellString(totalRow, 5, "$" + g.getTramo61a90());
                    ExcelExportService.cellString(totalRow, 6, "$" + g.getMasDe90());
                }
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=aging-report.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(workbook);
    }
}
