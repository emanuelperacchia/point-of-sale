package com.pos.system.controller;

import com.pos.system.dto.request.GoodsReceiptRequest;
import com.pos.system.dto.response.GoodsReceiptResponse;
import com.pos.system.security.UserDetailsImpl;
import com.pos.system.service.GoodsReceiptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para la recepción de mercadería.
 */
@RestController
@RequestMapping("/api/goods-receipts")
@RequiredArgsConstructor
@Tag(name = "Goods Receipts", description = "API de recepción de mercadería")
public class GoodsReceiptController {

    private final GoodsReceiptService goodsReceiptService;

    @GetMapping
    @PreAuthorize("hasAuthority('COMPRAS_READ')")
    @Operation(summary = "Listar todas las recepciones")
    public ResponseEntity<List<GoodsReceiptResponse>> findAll() {
        return ResponseEntity.ok(goodsReceiptService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('COMPRAS_READ')")
    @Operation(summary = "Obtener recepción por ID")
    public ResponseEntity<GoodsReceiptResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(goodsReceiptService.findById(id));
    }

    @GetMapping("/by-purchase-order/{purchaseOrderId}")
    @PreAuthorize("hasAuthority('COMPRAS_READ')")
    @Operation(summary = "Obtener recepciones por orden de compra")
    public ResponseEntity<List<GoodsReceiptResponse>> findByPurchaseOrder(
            @PathVariable Long purchaseOrderId) {
        return ResponseEntity.ok(goodsReceiptService.findByPurchaseOrder(purchaseOrderId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('COMPRAS_CREATE')")
    @Operation(summary = "Registrar recepción de mercadería",
            description = "Registra la entrada de productos, actualiza stock y estado de la orden")
    public ResponseEntity<GoodsReceiptResponse> create(
            @Valid @RequestBody GoodsReceiptRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        GoodsReceiptResponse receipt = goodsReceiptService.create(request, userDetails.getUser());
        return ResponseEntity.status(HttpStatus.CREATED).body(receipt);
    }
}
