package com.pos.system.controller;

import com.pos.system.dto.request.SaleRequest;
import com.pos.system.dto.response.SaleResponse;
import com.pos.system.entity.ApiKey;
import com.pos.system.service.SaleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/public/v1/sales")
@RequiredArgsConstructor
@Tag(name = "Public API - Sales", description = "Ventas via API pública")
public class PublicSaleController {

    private final SaleService saleService;

    @PostMapping
    @Operation(summary = "Registrar una venta", description = "Crea una nueva venta usando un ID de usuario de sistema (1)")
    public ResponseEntity<SaleResponse> createSale(@Valid @RequestBody SaleRequest request) {
        // Las claves API de integración usan usuario de sistema con ID 1
        // El usuario autenticado se obtiene del atributo "apiKey"
        SaleResponse response = saleService.processSale(request, 1L);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener detalle de una venta")
    public ResponseEntity<SaleResponse> getSale(@PathVariable Long id) {
        return ResponseEntity.ok(saleService.getById(id));
    }
}
