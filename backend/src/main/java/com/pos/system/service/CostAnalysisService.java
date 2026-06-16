package com.pos.system.service;

import com.pos.system.dto.response.CostAnalysisResponse;
import com.pos.system.entity.*;
import com.pos.system.exception.ResourceNotFoundException;
import com.pos.system.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CostAnalysisService {

    private final ProductionOrderRepository productionOrderRepository;
    private final ProductionOrderComponentRepository poComponentRepository;
    private final RecipeRepository recipeRepository;
    private final BomComponentRepository bomComponentRepository;
    private final ProductRepository productRepository;
    private final BomExplosionService bomExplosionService;

    @Transactional(readOnly = true)
    public CostAnalysisResponse analyze(Long productionOrderId) {
        ProductionOrder order = productionOrderRepository.findById(productionOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Orden de produccion no encontrada"));

        Recipe recipe = recipeRepository.findById(order.getRecipeId())
                .orElseThrow(() -> new ResourceNotFoundException("Receta no encontrada"));

        if (order.getEstado() != ProductionOrder.Estado.COMPLETADA) {
            throw new IllegalStateException("La orden debe estar COMPLETADA para analizar costos");
        }

        // Estimated cost from BOM explosion
        BigDecimal cantidadBig = BigDecimal.valueOf(order.getCantidadPlanificada());
        var costEstimate = bomExplosionService.getCostEstimate(recipe.getId(), cantidadBig);
        BigDecimal costoEstimadoTotal = costEstimate.getCostoTotalEstimado();
        BigDecimal costoUnitarioEstimado = costEstimate.getCostoUnitarioEstimado();

        // Real cost from consumed components
        List<ProductionOrderComponent> components = poComponentRepository.findByProductionOrderId(productionOrderId);

        List<CostAnalysisResponse.CostDeviationItem> items = components.stream().map(poc -> {
            BomComponent bc = bomComponentRepository.findById(poc.getBomComponentId()).orElse(null);
            if (bc == null) return null;

            Product prod = productRepository.findById(bc.getComponenteId()).orElse(null);
            if (prod == null) return null;

            BigDecimal cantidadReal = poc.getCantidadConsumida() != null
                    ? poc.getCantidadConsumida().add(poc.getMermaReal() != null ? poc.getMermaReal() : BigDecimal.ZERO)
                    : BigDecimal.ZERO;

            BigDecimal costoReal = cantidadReal.multiply(prod.getPrice());
            BigDecimal costoEstimadoItem = poc.getCantidadPlanificada().multiply(prod.getPrice());
            BigDecimal desviacion = costoReal.subtract(costoEstimadoItem);

            return CostAnalysisResponse.CostDeviationItem.builder()
                    .componenteNombre(prod.getName())
                    .cantidadPlanificada(poc.getCantidadPlanificada())
                    .cantidadConsumida(cantidadReal)
                    .precioUnitario(prod.getPrice())
                    .costoEstimado(costoEstimadoItem)
                    .costoReal(costoReal)
                    .desviacion(desviacion)
                    .build();
        }).filter(java.util.Objects::nonNull).toList();

        BigDecimal costoRealTotal = items.stream()
                .map(CostAnalysisResponse.CostDeviationItem::getCostoReal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal desviacion = costoRealTotal.subtract(costoEstimadoTotal);

        Integer qty = order.getCantidadProducida() != null ? order.getCantidadProducida() : 1;
        BigDecimal costoUnitarioReal = costoRealTotal.divide(BigDecimal.valueOf(qty), 2, RoundingMode.HALF_UP);

        return CostAnalysisResponse.builder()
                .productionOrderId(productionOrderId)
                .recipeId(recipe.getId())
                .recipeNombre(recipe.getNombre())
                .cantidadProducida(order.getCantidadProducida())
                .costoEstimadoTotal(costoEstimadoTotal)
                .costoRealTotal(costoRealTotal)
                .desviacion(desviacion)
                .costoUnitarioEstimado(costoUnitarioEstimado)
                .costoUnitarioReal(costoUnitarioReal)
                .componentes(items)
                .build();
    }
}
