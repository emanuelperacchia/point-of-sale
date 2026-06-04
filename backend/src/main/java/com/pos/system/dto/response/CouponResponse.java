package com.pos.system.dto.response;

import com.pos.system.entity.PromotionType;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record CouponResponse(
        Long id,
        String codigo,
        PromotionType tipo,
        BigDecimal valor,
        Boolean valido,
        String mensaje
) {}
