package com.pos.system.controller;

import com.pos.system.dto.request.CouponValidateRequest;
import com.pos.system.dto.response.CouponResponse;
import com.pos.system.dto.response.DiscountResult;
import com.pos.system.dto.request.ValidateCartDiscountsRequest;
import com.pos.system.service.promotion.PromotionEngine;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Descuentos", description = "Validación de promociones y cupones")
@SecurityRequirement(name = "bearerAuth")
public class DiscountController {

    private final PromotionEngine promotionEngine;

    @PostMapping("/promotions/validate-cart")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'CAJERO')")
    @Operation(summary = "Validar descuentos del carrito",
               description = "Evalúa promociones activas y cupón opcional contra el carrito")
    public ResponseEntity<DiscountResult> validateCart(@Valid @RequestBody ValidateCartDiscountsRequest request) {
        List<PromotionEngine.CartItem> items = new ArrayList<>();
        if (request.getItems() != null) {
            for (var dto : request.getItems()) {
                items.add(new PromotionEngine.CartItem(
                        dto.getProductId(),
                        dto.getProductName(),
                        dto.getQuantity(),
                        dto.getUnitPrice(),
                        dto.getCategoryId()
                ));
            }
        }

        DiscountResult result = promotionEngine.evaluate(items, request.getCouponCode());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/coupons/validate")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'CAJERO')")
    @Operation(summary = "Validar cupón", description = "Valida un código de cupón y retorna el descuento")
    public ResponseEntity<CouponResponse> validateCoupon(@Valid @RequestBody CouponValidateRequest request) {
        CouponResponse response = promotionEngine.validateCoupon(request.getCodigo());
        return ResponseEntity.ok(response);
    }
}
