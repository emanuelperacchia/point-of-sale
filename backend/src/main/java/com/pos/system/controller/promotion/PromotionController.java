package com.pos.system.controller.promotion;

import com.pos.system.dto.promotion.PromotionRequest;
import com.pos.system.dto.promotion.PromotionResponse;
import com.pos.system.service.promotion.PromotionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/promotions")
public class PromotionController {

    @Autowired
    private PromotionService promotionService;

    @PostMapping("/businesses/{businessId}")
    public ResponseEntity<PromotionResponse> createPromotion(
            @PathVariable Long businessId,
            @RequestBody PromotionRequest request) {
        PromotionResponse promotion = promotionService.createPromotion(businessId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(promotion);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PromotionResponse> getPromotionById(@PathVariable Long id) {
        PromotionResponse promotion = promotionService.getPromotionById(id);
        return ResponseEntity.ok(promotion);
    }

    @GetMapping("/businesses/{businessId}")
    public ResponseEntity<List<PromotionResponse>> getPromotionsByBusiness(@PathVariable Long businessId) {
        List<PromotionResponse> promotions = promotionService.getPromotionsByBusiness(businessId);
        return ResponseEntity.ok(promotions);
    }

    @GetMapping("/active")
    public ResponseEntity<List<PromotionResponse>> getActivePromotions() {
        List<PromotionResponse> promotions = promotionService.getActivePromotions();
        return ResponseEntity.ok(promotions);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PromotionResponse> updatePromotion(
            @PathVariable Long id,
            @RequestBody PromotionRequest request) {
        PromotionResponse promotion = promotionService.updatePromotion(id, request);
        return ResponseEntity.ok(promotion);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePromotion(@PathVariable Long id) {
        promotionService.deletePromotion(id);
        return ResponseEntity.noContent().build();
    }
}
