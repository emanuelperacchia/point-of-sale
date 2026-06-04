package com.pos.system.controller;

import com.pos.system.dto.request.PurchaseOrderRequest;
import com.pos.system.dto.response.PurchaseOrderResponse;
import com.pos.system.security.UserDetailsImpl;
import com.pos.system.service.PurchaseOrderService;
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
import java.util.List;

/**
 * Controlador REST para la gestión de órdenes de compra.
 */
@RestController
@RequestMapping("/api/purchase-orders")
@RequiredArgsConstructor
@Tag(name = "Purchase Orders", description = "API de gestión de órdenes de compra")
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    @GetMapping
    @PreAuthorize("hasAuthority('COMPRAS_READ')")
    @Operation(summary = "Listar todas las órdenes de compra")
    public ResponseEntity<List<PurchaseOrderResponse>> findAll() {
        return ResponseEntity.ok(purchaseOrderService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('COMPRAS_READ')")
    @Operation(summary = "Obtener orden de compra por ID")
    public ResponseEntity<PurchaseOrderResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(purchaseOrderService.findById(id));
    }

    @GetMapping("/number/{orderNumber}")
    @PreAuthorize("hasAuthority('COMPRAS_READ')")
    @Operation(summary = "Obtener orden de compra por número")
    public ResponseEntity<PurchaseOrderResponse> findByOrderNumber(
            @PathVariable String orderNumber) {
        return ResponseEntity.ok(purchaseOrderService.findByOrderNumber(orderNumber));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAuthority('COMPRAS_READ')")
    @Operation(summary = "Listar órdenes pendientes de recepción")
    public ResponseEntity<List<PurchaseOrderResponse>> findPending() {
        return ResponseEntity.ok(purchaseOrderService.findPendingOrders());
    }

    @GetMapping("/overdue")
    @PreAuthorize("hasAuthority('COMPRAS_READ')")
    @Operation(summary = "Listar órdenes vencidas")
    public ResponseEntity<List<PurchaseOrderResponse>> findOverdue() {
        return ResponseEntity.ok(purchaseOrderService.findOverdueOrders());
    }

    @GetMapping("/by-supplier/{supplierId}")
    @PreAuthorize("hasAuthority('COMPRAS_READ')")
    @Operation(summary = "Listar órdenes por proveedor")
    public ResponseEntity<List<PurchaseOrderResponse>> findBySupplier(
            @PathVariable Long supplierId) {
        return ResponseEntity.ok(purchaseOrderService.findBySupplier(supplierId));
    }

    @GetMapping("/by-supplier/{supplierId}/total")
    @PreAuthorize("hasAuthority('COMPRAS_READ')")
    @Operation(summary = "Obtener total de compras por proveedor")
    public ResponseEntity<BigDecimal> getTotalBySupplier(@PathVariable Long supplierId) {
        return ResponseEntity.ok(
                purchaseOrderService.getTotalPurchasesBySupplier(supplierId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('COMPRAS_CREATE')")
    @Operation(summary = "Crear nueva orden de compra")
    public ResponseEntity<PurchaseOrderResponse> create(
            @Valid @RequestBody PurchaseOrderRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        PurchaseOrderResponse order = purchaseOrderService.create(request, userDetails.getUser());
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('COMPRAS_UPDATE')")
    @Operation(summary = "Actualizar orden de compra")
    public ResponseEntity<PurchaseOrderResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody PurchaseOrderRequest request) {
        return ResponseEntity.ok(purchaseOrderService.update(id, request));
    }

    @PostMapping("/{id}/send")
    @PreAuthorize("hasAuthority('COMPRAS_UPDATE')")
    @Operation(summary = "Enviar orden de compra (BORRADOR → ENVIADA)")
    public ResponseEntity<PurchaseOrderResponse> send(@PathVariable Long id) {
        return ResponseEntity.ok(purchaseOrderService.send(id));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('COMPRAS_UPDATE')")
    @Operation(summary = "Aprobar orden de compra")
    public ResponseEntity<PurchaseOrderResponse> approve(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(purchaseOrderService.approve(id, userDetails.getUser()));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('COMPRAS_UPDATE')")
    @Operation(summary = "Cancelar orden de compra")
    public ResponseEntity<PurchaseOrderResponse> cancel(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(purchaseOrderService.cancel(id, reason));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('COMPRAS_DELETE')")
    @Operation(summary = "Eliminar orden de compra (solo BORRADOR)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        purchaseOrderService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
