package com.pos.system.dto.response;

import com.pos.system.entity.PromotionScope;
import com.pos.system.entity.PromotionType;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Builder
public record PromotionResponse(
        Long id,
        String nombre,
        PromotionType tipo,
        BigDecimal valor,
        LocalDate fechaDesde,
        LocalDate fechaHasta,
        Integer prioridad,
        PromotionScope alcance,
        Boolean activa,
        Integer compraX,
        Integer llevaY,
        List<Long> productoIds,
        List<Long> categoriaIds,
        LocalDateTime createdAt
) {}
