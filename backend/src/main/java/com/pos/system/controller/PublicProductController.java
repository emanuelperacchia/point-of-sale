package com.pos.system.controller;

import com.pos.system.dto.response.ProductResponse;
import com.pos.system.entity.ApiKey;
import com.pos.system.entity.Product;
import com.pos.system.repository.ProductRepository;
import com.pos.system.repository.ProductStockRepository;
import com.pos.system.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/public/v1/products")
@RequiredArgsConstructor
@Tag(name = "Public API - Products", description = "Productos disponibles via API pública")
public class PublicProductController {

    private final ProductService productService;
    private final ProductRepository productRepository;
    private final ProductStockRepository productStockRepository;

    @GetMapping
    @Operation(summary = "Listar productos activos", description = "Retorna productos activos disponibles")
    public ResponseEntity<Page<ProductResponse>> listProducts(Pageable pageable) {
        return ResponseEntity.ok(productService.getAll(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener detalle de un producto")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getById(id));
    }

    @GetMapping("/{id}/stock")
    @Operation(summary = "Consultar stock actual de un producto")
    public ResponseEntity<StockResponse> getStock(@PathVariable Long id,
                                                   @RequestParam(required = false) Long warehouseId) {
        Product product = productRepository.findById(id)
                .orElse(null);
        if (product == null || !product.getActive()) {
            return ResponseEntity.notFound().build();
        }

        int stock;
        if (warehouseId != null) {
            stock = productStockRepository.findByProductIdAndWarehouseId(id, warehouseId)
                    .map(ps -> ps.getCurrentStock() != null ? ps.getCurrentStock().intValue() : 0)
                    .orElse(0);
        } else {
            stock = product.getStock() != null ? product.getStock() : 0;
        }

        return ResponseEntity.ok(new StockResponse(id, stock));
    }

    @GetMapping("/{id}/price")
    @Operation(summary = "Consultar precio actual de un producto")
    public ResponseEntity<PriceResponse> getPrice(@PathVariable Long id) {
        ProductResponse product = productService.getById(id);
        return ResponseEntity.ok(new PriceResponse(id, product.getPrice()));
    }

    // --- DTOs internos ---

    record StockResponse(Long productId, Integer stock) {}
    record PriceResponse(Long productId, BigDecimal price) {}
}
