package com.pos.system.controller;

import com.pos.system.dto.request.ProductionOrderRequest;
import com.pos.system.dto.response.CostAnalysisResponse;
import com.pos.system.dto.response.ProductionOrderResponse;
import com.pos.system.entity.ProductionOrderComponent;
import com.pos.system.service.CostAnalysisService;
import com.pos.system.service.ProductionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/production-orders")
@RequiredArgsConstructor
public class ProductionOrderController {

    private final ProductionService productionService;
    private final CostAnalysisService costAnalysisService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','INVENTARIO')")
    public ResponseEntity<List<ProductionOrderResponse>> listAll(
            @RequestParam(required = false) String estado) {
        if (estado != null) {
            return ResponseEntity.ok(productionService.listByEstado(estado));
        }
        return ResponseEntity.ok(productionService.listAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','INVENTARIO')")
    public ResponseEntity<ProductionOrderResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(productionService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public ResponseEntity<ProductionOrderResponse> create(@Valid @RequestBody ProductionOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productionService.create(request));
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','INVENTARIO')")
    public ResponseEntity<ProductionOrderResponse> start(@PathVariable Long id) {
        return ResponseEntity.ok(productionService.start(id));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','INVENTARIO')")
    public ResponseEntity<ProductionOrderResponse> complete(
            @PathVariable Long id,
            @RequestParam Integer cantidadProducida,
            @RequestBody(required = false) List<ProductionService.MermaEntry> mermaEntries) {
        return ResponseEntity.ok(productionService.complete(id, cantidadProducida, mermaEntries));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public ResponseEntity<ProductionOrderResponse> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(productionService.cancel(id));
    }

    @GetMapping("/{id}/cost-analysis")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','CONTADOR')")
    public ResponseEntity<CostAnalysisResponse> costAnalysis(@PathVariable Long id) {
        return ResponseEntity.ok(costAnalysisService.analyze(id));
    }
}
