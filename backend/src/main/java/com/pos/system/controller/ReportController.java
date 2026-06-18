package com.pos.system.controller;

import com.pos.system.dto.response.CashFlowResponse;
import com.pos.system.dto.response.SalesBookResponse;
import com.pos.system.dto.response.SalesBookRow;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import com.pos.system.dto.response.SalesReportResponse;
import com.pos.system.service.CashFlowService;
import com.pos.system.service.ExcelExportService;
import com.pos.system.service.SalesBookReportService;
import com.pos.system.service.SalesReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "Reportes", description = "Libro de ventas, flujo de caja y exportaciones")
@SecurityRequirement(name = "bearerAuth")
public class ReportController {

    private final SalesBookReportService salesBookReportService;
    private final CashFlowService cashFlowService;
    private final ExcelExportService excelExportService;
    private final SalesReportService salesReportService;

    // ── US-022: Libro de Ventas ────────────────────────────────────────

    @GetMapping("/sales-book")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'CONTADOR')")
    @Operation(summary = "Libro de ventas del período", description = "Retorna comprobantes con detección de saltos en numeración")
    public ResponseEntity<SalesBookResponse> getSalesBook(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String tipo) {
        return ResponseEntity.ok(salesBookReportService.getSalesBook(desde, hasta, tipo));
    }

    @GetMapping("/sales-book/export")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'CONTADOR')")
    @Operation(summary = "Exportar libro de ventas")
    public ResponseEntity<byte[]> exportSalesBook(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String tipo,
            @RequestParam(defaultValue = "xlsx") String format) {

        var rows = salesBookReportService.getRowsForExport(desde, hasta, tipo);

        if ("csv".equalsIgnoreCase(format)) {
            return exportSalesBookCsv(rows);
        }

        // Excel
        List<String> headers = List.of("Fecha", "Tipo", "Número", "CUIT", "Razón Social",
                "Neto Gravado", "IVA", "Total", "Estado");

        byte[] excel = excelExportService.generate(
                "Libro Ventas", headers, rows,
                (row, idx, item) -> {
                    ExcelExportService.cellString(row, 0, item.fecha().toString());
                    ExcelExportService.cellString(row, 1, item.tipoComprobante().name());
                    ExcelExportService.cellString(row, 2, item.getNumeroFormateado());
                    ExcelExportService.cellString(row, 3, item.cuitReceptor());
                    ExcelExportService.cellString(row, 4, item.razonSocial());
                    ExcelExportService.cellNumeric(row, 5, item.netoGravado());
                    ExcelExportService.cellNumeric(row, 6, item.iva());
                    ExcelExportService.cellNumeric(row, 7, item.total());
                    ExcelExportService.cellString(row, 8, item.estado().name());
                },
                workbook -> {
                    // Totales al pie
                    Sheet sheet = workbook.getSheetAt(0);
                    int lastRow = sheet.getLastRowNum() + 2;
                    Row totalRow = sheet.createRow(lastRow);
                    ExcelExportService.cellString(totalRow, 0, "TOTALES");
                    BigDecimal totalNeto = rows.stream()
                            .map(SalesBookRow::netoGravado).reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal totalIva = rows.stream()
                            .map(SalesBookRow::iva).reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal totalGeneral = rows.stream()
                            .map(SalesBookRow::total).reduce(BigDecimal.ZERO, BigDecimal::add);
                    ExcelExportService.cellNumeric(totalRow, 5, totalNeto);
                    ExcelExportService.cellNumeric(totalRow, 6, totalIva);
                    ExcelExportService.cellNumeric(totalRow, 7, totalGeneral);
                }
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=libro-ventas.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excel);
    }

    private ResponseEntity<byte[]> exportSalesBookCsv(List<SalesBookRow> rows) {
        StringBuilder sb = new StringBuilder();
        // Header
        sb.append("Fecha;Tipo;Numero;CUIT;RazonSocial;NetoGravado;IVA;Total;Estado\n");
        for (var row : rows) {
            sb.append(row.fecha()).append(";")
                    .append(row.tipoComprobante()).append(";")
                    .append(row.getNumeroFormateado()).append(";")
                    .append(row.cuitReceptor()).append(";")
                    .append(row.razonSocial()).append(";")
                    .append(row.netoGravado()).append(";")
                    .append(row.iva()).append(";")
                    .append(row.total()).append(";")
                    .append(row.estado()).append("\n");
        }
        byte[] csv = sb.toString().getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=libro-ventas.csv")
                .contentType(MediaType.parseMediaType("text/csv; charset=ISO-8859-1"))
                .body(csv);
    }

    // ── US-027: Flujo de Caja ──────────────────────────────────────────

    @GetMapping("/cash-flow")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    @Operation(summary = "Flujo de caja real y proyectado")
    public ResponseEntity<CashFlowResponse> getCashFlow(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "false") boolean incluirProyeccion,
            @RequestParam(defaultValue = "30") int diasProyeccion) {
        return ResponseEntity.ok(cashFlowService.getCashFlow(desde, hasta, incluirProyeccion, diasProyeccion));
    }

    // ── US-036: Reportes de Ventas Avanzados ──────────────────────────

    @GetMapping("/sales-advanced")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'CONTADOR')")
    @Operation(summary = "Reporte avanzado de ventas con métricas desglosadas y comparativa")
    public ResponseEntity<SalesReportResponse> getSalesAdvanced(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(salesReportService.advancedReport(desde, hasta));
    }

    @GetMapping("/cash-flow/export")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    @Operation(summary = "Exportar flujo de caja a Excel")
    public ResponseEntity<byte[]> exportCashFlow(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "false") boolean incluirProyeccion,
            @RequestParam(defaultValue = "30") int diasProyeccion) {

        var flow = cashFlowService.getCashFlow(desde, hasta, incluirProyeccion, diasProyeccion);

        List<String> headers = List.of("Fecha", "Ingresos", "Egresos", "Saldo Día", "Saldo Acumulado", "Tipo");

        byte[] excel = excelExportService.generate(
                "Flujo Caja", headers, flow.dias(),
                (row, idx, item) -> {
                    ExcelExportService.cellString(row, 0, item.fecha().toString());
                    ExcelExportService.cellNumeric(row, 1, item.ingresos());
                    ExcelExportService.cellNumeric(row, 2, item.egresos());
                    ExcelExportService.cellNumeric(row, 3, item.saldoDia());
                    ExcelExportService.cellNumeric(row, 4, item.saldoAcumulado());
                    ExcelExportService.cellString(row, 5, item.esProyectado() ? "Proyectado" : "Real");
                }
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=flujo-caja.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excel);
    }
}
