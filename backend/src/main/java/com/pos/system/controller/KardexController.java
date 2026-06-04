package com.pos.system.controller;

import com.pos.system.dto.response.KardexResponse;
import com.pos.system.dto.response.StockStatusResponse;
import com.pos.system.service.KardexService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/inventory/kardex")
@RequiredArgsConstructor
@Tag(name = "Kardex", description = "Consulta de kardex y stock de productos")
public class KardexController {

    private final KardexService kardexService;

    @GetMapping("/products/{productId}")
    @Operation(summary = "Obtener kardex de producto", description = "Historial de movimientos de un producto")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CASHIER')")
    public ResponseEntity<Page<KardexResponse>> getProductKardex(
            @PathVariable Long productId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(kardexService.getProductKardex(productId, pageable));
    }

    @GetMapping("/products/{productId}/warehouses/{warehouseId}")
    @Operation(summary = "Obtener kardex por bodega", description = "Historial de movimientos en una bodega específica")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CASHIER')")
    public ResponseEntity<Page<KardexResponse>> getProductKardexByWarehouse(
            @PathVariable Long productId,
            @PathVariable Long warehouseId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(kardexService.getProductKardexByWarehouse(productId, warehouseId, pageable));
    }

    @GetMapping("/products/{productId}/stock")
    @Operation(summary = "Estado de stock", description = "Stock actual en todas las bodegas")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CASHIER')")
    public ResponseEntity<List<StockStatusResponse>> getProductStock(
            @PathVariable Long productId) {
        return ResponseEntity.ok(kardexService.getProductStockStatus(productId));
    }

    @GetMapping("/products/{productId}/stock/{warehouseId}")
    @Operation(summary = "Stock en bodega", description = "Stock actual en una bodega específica")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CASHIER')")
    public ResponseEntity<StockStatusResponse> getProductStockByWarehouse(
            @PathVariable Long productId,
            @PathVariable Long warehouseId) {
        return ResponseEntity.ok(kardexService.getProductStockByWarehouse(productId, warehouseId));
    }

    @GetMapping("/products/{productId}/total")
    @Operation(summary = "Stock total", description = "Stock total del producto en todas las bodegas")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CASHIER')")
    public ResponseEntity<BigDecimal> getTotalStock(@PathVariable Long productId) {
        return ResponseEntity.ok(kardexService.getTotalStock(productId));
    }

    @GetMapping("/latest")
    @Operation(summary = "Últimos movimientos", description = "Últimos 10 movimientos de inventario")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<KardexResponse>> getLatestMovements() {
        return ResponseEntity.ok(kardexService.getLatestMovements());
    }
}