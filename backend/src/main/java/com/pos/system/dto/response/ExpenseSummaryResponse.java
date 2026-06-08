package com.pos.system.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ExpenseSummaryResponse(
        LocalDate desde,
        LocalDate hasta,
        List<CategoriaTotal> categorias,
        BigDecimal total
) {
    public record CategoriaTotal(
            String categoria,
            BigDecimal total
    ) {}
}
