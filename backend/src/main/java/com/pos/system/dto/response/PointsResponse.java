package com.pos.system.dto.response;

import com.pos.system.entity.ClientTier;
import lombok.Builder;

import java.util.List;

@Builder
public record PointsResponse(
        Long clientId,
        String clientName,
        Long saldoActual,
        ClientTier tier,
        List<PointsTransactionItem> historial
) {
    @Builder
    public record PointsTransactionItem(
            Long id,
            Long saleId,
            String tipo,
            Long puntos,
            Long saldoPrevio,
            Long saldoPosterior,
            String descripcion,
            String fecha
    ) {}
}
