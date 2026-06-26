package com.pos.system.service;

import com.pos.system.dto.response.PriceDifferenceReportResponse;
import com.pos.system.entity.BranchPrice;
import com.pos.system.entity.Product;
import com.pos.system.repository.BranchPriceRepository;
import com.pos.system.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PriceDifferenceReportService {

    private final BranchPriceRepository branchPriceRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<PriceDifferenceReportResponse> getDifferences(Long branchId) {
        List<BranchPrice> branchPrices = branchPriceRepository.findVigentesByBranchId(branchId, LocalDate.now());

        if (branchPrices.isEmpty()) {
            return List.of();
        }

        Map<Long, Product> products = productRepository.findAllById(
                branchPrices.stream().map(BranchPrice::getProductId).toList()
        ).stream().collect(Collectors.toMap(Product::getId, p -> p));

        List<PriceDifferenceReportResponse> result = new ArrayList<>();

        for (BranchPrice bp : branchPrices) {
            Product product = products.get(bp.getProductId());
            if (product == null) continue;

            BigDecimal global = product.getPrice();
            BigDecimal local = bp.getPrecio();

            if (global.compareTo(local) == 0) continue; // sin diferencia

            BigDecimal diferenciaMonto = local.subtract(global);
            BigDecimal diferenciaPorcentaje = global.compareTo(BigDecimal.ZERO) > 0
                    ? diferenciaMonto.multiply(BigDecimal.valueOf(100)).divide(global, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            result.add(PriceDifferenceReportResponse.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .productSku(product.getSku())
                    .precioGlobal(global)
                    .precioLocal(local)
                    .diferenciaMonto(diferenciaMonto)
                    .diferenciaPorcentaje(diferenciaPorcentaje)
                    .build());
        }

        return result;
    }
}
