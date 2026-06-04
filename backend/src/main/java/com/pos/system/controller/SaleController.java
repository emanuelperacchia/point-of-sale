package com.pos.system.controller;

import com.pos.system.dto.request.CartValidationRequest;
import com.pos.system.dto.request.SaleRequest;
import com.pos.system.dto.response.CartValidationResponse;
import com.pos.system.dto.response.SaleResponse;
import com.pos.system.security.UserDetailsImpl;
import com.pos.system.service.SaleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
@Tag(name = "Ventas", description = "Punto de Venta — procesar ventas")
@SecurityRequirement(name = "bearerAuth")
public class SaleController {

    private final SaleService saleService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'CAJERO')")
    @Operation(summary = "Procesar venta", description = "Registra una venta completa con items, pagos y descuento de stock")
    public ResponseEntity<SaleResponse> processSale(
            @Valid @RequestBody SaleRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        SaleResponse response = saleService.processSale(request, userDetails.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/validate-cart")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'CAJERO')")
    @Operation(summary = "Validar carrito", description = "Verifica disponibilidad de stock antes de confirmar la venta")
    public ResponseEntity<CartValidationResponse> validateCart(
            @Valid @RequestBody CartValidationRequest request) {
        return ResponseEntity.ok(saleService.validateCart(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'CAJERO', 'VENDEDOR')")
    @Operation(summary = "Obtener venta", description = "Obtiene una venta por su ID")
    public ResponseEntity<SaleResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(saleService.getById(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'CAJERO')")
    @Operation(summary = "Mis ventas", description = "Obtiene las ventas del usuario autenticado con paginación")
    public ResponseEntity<Page<SaleResponse>> getMySales(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(saleService.getSalesByUser(userDetails.getId(), pageable));
    }
}
