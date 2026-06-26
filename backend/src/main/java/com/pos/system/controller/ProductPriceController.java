package com.pos.system.controller;

import com.pos.system.config.BranchContextHolder;
import com.pos.system.dto.response.PriceResolutionResponse;
import com.pos.system.service.PriceResolverService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Precios por Sucursal", description = "Resolución de precios según sucursal activa")
@SecurityRequirement(name = "bearerAuth")
public class ProductPriceController {

    private final PriceResolverService priceResolverService;

    @GetMapping("/{id}/price")
    @Operation(summary = "Resolver precio de producto",
              description = "Retorna el precio vigente para el producto según la sucursal del JWT activo. " +
                            "Si existe precio local y está vigente, lo usa. Si no, usa el precio global como fallback.")
    public ResponseEntity<PriceResolutionResponse> resolvePrice(@PathVariable Long id) {
        Long branchId = BranchContextHolder.getBranchId();
        PriceResolutionResponse response = priceResolverService.resolveWithDetail(id, branchId);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }
}
