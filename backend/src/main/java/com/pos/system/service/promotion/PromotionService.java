package com.pos.system.service.promotion;

import com.pos.system.dto.promotion.PromotionRequest;
import com.pos.system.dto.promotion.PromotionResponse;
import com.pos.system.entity.business.Branch;
import com.pos.system.entity.promotion.Promotion;
import com.pos.system.entity.promotion.Promotion.PromotionType;
import com.pos.system.repository.business.BranchRepository;
import com.pos.system.repository.promotion.PromotionRepository;
import com.pos.system.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class PromotionService {

    @Autowired
    private PromotionRepository promotionRepository;

    @Autowired
    private BranchRepository branchRepository;

    public PromotionResponse createPromotion(Long businessId, PromotionRequest request) {
        Branch business = branchRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Negocio no encontrado con ID: " + businessId));

        Promotion promotion = Promotion.builder()
                .business(business)
                .name(request.getName())
                .description(request.getDescription())
                .type(PromotionType.valueOf(request.getType()))
                .value(request.getValue())
                .active(request.isActive())
                .startsAt(request.getStartsAt())
                .expiresAt(request.getExpiresAt())
                .maxUses(request.getMaxUses())
                .couponCode(request.getCouponCode())
                .build();

        Promotion saved = promotionRepository.save(promotion);
        return mapToResponse(saved);
    }

    public PromotionResponse getPromotionById(Long id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promoción no encontrada con ID: " + id));
        return mapToResponse(promotion);
    }

    public List<PromotionResponse> getPromotionsByBusiness(Long businessId) {
        return promotionRepository.findByBusinessId(businessId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<PromotionResponse> getActivePromotions() {
        return promotionRepository.findByActiveTrue().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public PromotionResponse updatePromotion(Long id, PromotionRequest request) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promoción no encontrada con ID: " + id));

        promotion.setName(request.getName());
        promotion.setDescription(request.getDescription());
        promotion.setType(PromotionType.valueOf(request.getType()));
        promotion.setValue(request.getValue());
        promotion.setActive(request.isActive());
        promotion.setStartsAt(request.getStartsAt());
        promotion.setExpiresAt(request.getExpiresAt());
        promotion.setMaxUses(request.getMaxUses());
        promotion.setCouponCode(request.getCouponCode());

        Promotion saved = promotionRepository.save(promotion);
        return mapToResponse(saved);
    }

    public PromotionResponse deletePromotion(Long id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promoción no encontrada con ID: " + id));
        promotionRepository.delete(promotion);
        return mapToResponse(promotion);
    }

    private PromotionResponse mapToResponse(Promotion promotion) {
        PromotionResponse response = new PromotionResponse();
        response.setId(promotion.getId());
        response.setBusinessId(promotion.getBusiness().getId());
        response.setBusinessName(promotion.getBusiness().getBranchName());
        response.setName(promotion.getName());
        response.setDescription(promotion.getDescription());
        response.setType(promotion.getType().name());
        response.setValue(promotion.getValue());
        response.setActive(promotion.isActive());
        response.setStartsAt(promotion.getStartsAt());
        response.setExpiresAt(promotion.getExpiresAt());
        response.setMaxUses(promotion.getMaxUses());
        response.setCurrentUses(promotion.getCurrentUses());
        response.setCouponCode(promotion.getCouponCode());
        response.setCreatedAt(promotion.getCreatedAt());
        return response;
    }
}
