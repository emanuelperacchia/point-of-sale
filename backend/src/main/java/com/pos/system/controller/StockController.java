package com.pos.system.controller;

import com.pos.system.dto.request.StockAdjustmentRequest;
import com.pos.system.dto.request.StockMovementRequest;
import com.pos.system.dto.response.KardexResponse;
import com.pos.system.security.UserDetailsImpl;
import com.pos.system.service.StockMovementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/inventory/stock")
@RequiredArgsConstructor
@Tag(name = "Stock", description = "Movimientos de inventario (entradas, salidas, ajustes)")
public class StockController {

    private final StockMovementService stockMovementService;

    @PostMapping("/entry/purchase")
    @Operation(summary = "Entrada por compra", description = "Registra entrada de stock por compra a proveedor")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<KardexResponse> entryByPurchase(
            @Valid @RequestBody StockMovementRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(stockMovementService.entryByPurchase(request, userDetails.getUser()));
    }

    @PostMapping("/entry/return")
    @Operation(summary = "Entrada por devolución", description = "Registra entrada por devolución de cliente")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<KardexResponse> entryByReturn(
            @Valid @RequestBody StockMovementRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(stockMovementService.entryByReturn(request, userDetails.getUser()));
    }

    @PostMapping("/exit/sale")
    @Operation(summary = "Salida por venta", description = "Registra salida de stock por venta")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CASHIER')")
    public ResponseEntity<KardexResponse> exitBySale(
            @Valid @RequestBody StockMovementRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(
                stockMovementService.exitBySale(request, userDetails.getUser()));
    }

    @PostMapping("/exit/return-supplier")
    @Operation(summary = "Devolución a proveedor", description = "Registra devolución de producto a proveedor")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<KardexResponse> exitBySupplierReturn(
            @Valid @RequestBody StockMovementRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(
                stockMovementService.exitBySupplierReturn(request, userDetails.getUser()));
    }

    @PostMapping("/adjust")
    @Operation(summary = "Ajuste de inventario", description = "Ajuste positivo o negativo de inventario")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<KardexResponse> adjustStock(
            @Valid @RequestBody StockAdjustmentRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(
                stockMovementService.adjustStock(request, userDetails.getUser()));
    }

    @PostMapping("/transfer")
    @Operation(summary = "Transferencia entre bodegas", description = "Transfiere stock de una bodega a otra")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> transferStock(
            @RequestParam Long productId,
            @RequestParam Long fromWarehouseId,
            @RequestParam Long toWarehouseId,
            @RequestParam BigDecimal quantity,
            @RequestParam(required = false) String reason,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        stockMovementService.transferStock(productId, fromWarehouseId, toWarehouseId,
                quantity, reason, userDetails.getUser());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/waste")
    @Operation(summary = "Registrar merma", description = "Registra pérdida o merma de inventario")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<KardexResponse> registerWaste(
            @Valid @RequestBody StockMovementRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(
                stockMovementService.registerWaste(request, userDetails.getUser()));
    }
}