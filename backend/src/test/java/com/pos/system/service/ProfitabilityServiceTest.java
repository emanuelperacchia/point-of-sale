package com.pos.system.service;

import com.pos.system.repository.ExpenseRepository;
import com.pos.system.repository.ProductRepository;
import com.pos.system.repository.SaleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfitabilityServiceTest {

    @Mock private SaleRepository saleRepository;
    @Mock private ExpenseRepository expenseRepository;
    @Mock private ProductRepository productRepository;

    private ProfitabilityService profitabilityService;

    private final LocalDate desde = LocalDate.of(2026, 1, 1);
    private final LocalDate hasta = LocalDate.of(2026, 6, 18);

    @BeforeEach
    void setUp() {
        profitabilityService = new ProfitabilityService(saleRepository, expenseRepository, productRepository);
    }

    @Test
    void analyzeProfitability_ShouldReturnMargins() {
        when(saleRepository.sumTotalByCreatedAtBetween(any(), any())).thenReturn(BigDecimal.valueOf(100000));
        when(expenseRepository.sumExpensesByPeriod(any(), any())).thenReturn(BigDecimal.valueOf(60000));
        when(saleRepository.findAllProductSales(any(), any())).thenReturn(java.util.List.of());

        var res = profitabilityService.analyzeProfitability(desde, hasta);

        assertNotNull(res);
        assertEquals("OK", res.getStatus());
        assertNotNull(res.getMargenGeneral());
        assertEquals(0, BigDecimal.valueOf(100000).compareTo(res.getMargenGeneral().getIngresos()));
        assertEquals(0, BigDecimal.valueOf(60000).compareTo(res.getMargenGeneral().getGastosOperativos()));
        assertEquals(0, BigDecimal.valueOf(40000).compareTo(res.getMargenGeneral().getGananciaNeta()));
        assertEquals(0, BigDecimal.valueOf(40.0).compareTo(res.getMargenGeneral().getMargenNetoPct()));
        assertNotNull(res.getPuntoEquilibrio());
    }

    @Test
    void analyzeProfitability_WhenRepoFails_ShouldReturnError() {
        when(saleRepository.sumTotalByCreatedAtBetween(any(), any()))
                .thenThrow(new RuntimeException("DB Error"));

        var res = profitabilityService.analyzeProfitability(desde, hasta);
        assertEquals("ERROR", res.getStatus());
    }
}
