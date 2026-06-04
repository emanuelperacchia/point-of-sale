package com.pos.system.controller;

import com.pos.system.dto.response.InvoiceResponse;
import com.pos.system.entity.InvoiceStatus;
import com.pos.system.entity.TipoComprobante;
import com.pos.system.security.UserDetailsImpl;
import com.pos.system.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
@Tag(name = "Facturación Electrónica", description = "Comprobantes electrónicos — consulta, descarga y reintento")
@SecurityRequirement(name = "bearerAuth")
public class InvoiceController {

    private final InvoiceService invoiceService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    @Operation(summary = "Listar comprobantes",
              description = "Obtiene todos los comprobantes con filtros opcionales por tipo, estado y fechas")
    public ResponseEntity<List<InvoiceResponse>> getAll(
            @RequestParam(required = false) TipoComprobante tipo,
            @RequestParam(required = false) InvoiceStatus estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            @RequestParam(required = false) Long saleId) {
        return ResponseEntity.ok(invoiceService.findByFilters(tipo, estado, desde, hasta, saleId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'CAJERO', 'VENDEDOR')")
    @Operation(summary = "Obtener comprobante", description = "Obtiene un comprobante por su ID")
    public ResponseEntity<InvoiceResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(invoiceService.getById(id));
    }

    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'CAJERO')")
    @Operation(summary = "Descargar PDF", description = "Descarga el PDF del comprobante")
    public ResponseEntity<Resource> downloadPdf(@PathVariable Long id) {
        Resource pdf = invoiceService.getPdf(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"comprobante-" + id + ".pdf\"")
                .body(pdf);
    }

    @PostMapping("/{id}/retry")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    @Operation(summary = "Reintentar emisión",
              description = "Reintenta la emisión de un comprobante en estado PENDIENTE")
    public ResponseEntity<InvoiceResponse> retryEmission(@PathVariable Long id) {
        return ResponseEntity.ok(invoiceService.retryEmission(id));
    }
}
