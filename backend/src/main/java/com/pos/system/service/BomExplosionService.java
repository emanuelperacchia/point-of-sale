package com.pos.system.service;

import com.pos.system.dto.response.BomExplosionResponse;
import com.pos.system.dto.response.BomExplosionResponse.BomExplosionItem;
import com.pos.system.dto.response.CostEstimateResponse;
import com.pos.system.dto.response.CostEstimateResponse.CostItem;
import com.pos.system.entity.BomComponent;
import com.pos.system.entity.Product;
import com.pos.system.entity.Recipe;
import com.pos.system.exception.BadRequestException;
import com.pos.system.exception.ResourceNotFoundException;
import com.pos.system.repository.BomComponentRepository;
import com.pos.system.repository.ProductRepository;
import com.pos.system.repository.RecipeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BomExplosionService {

    private final RecipeRepository recipeRepository;
    private final BomComponentRepository bomComponentRepository;
    private final ProductRepository productRepository;

    /**
     * Explosion completa del BOM: resuelve sub-recetas recursivamente
     * hasta llegar solo a materias primas, con deteccion de ciclos.
     */
    public BomExplosionResponse explode(Long recipeId, BigDecimal cantidad) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new ResourceNotFoundException("Receta no encontrada"));

        Set<Long> visited = new HashSet<>();
        visited.add(recipeId);
        List<BomExplosionItem> items = new ArrayList<>();
        explodeRecursive(recipeId, cantidad, visited, items);

        // Merge duplicados (misma MP aparece en sub-recetas diferentes)
        Map<Long, BomExplosionItem> merged = new LinkedHashMap<>();
        for (BomExplosionItem item : items) {
            merged.merge(item.getProductoId(), item, (a, b) -> {
                a.setCantidadTotal(a.getCantidadTotal().add(b.getCantidadTotal()));
                a.setCostoTotal(a.getCostoTotal().add(b.getCostoTotal()));
                // Re-verify stock
                Product p = productRepository.findById(item.getProductoId()).orElse(null);
                if (p != null) {
                    int disponible = p.getStock() - p.getStockReservado();
                    a.setStockSuficiente(disponible >= a.getCantidadTotal().intValue());
                    if (!a.getStockSuficiente()) {
                        a.setStockFaltante(a.getCantidadTotal().intValue() - disponible);
                    }
                }
                return a;
            });
        }

        Product pt = productRepository.findById(recipe.getProductoTerminadoId()).orElse(null);

        return BomExplosionResponse.builder()
                .recipeId(recipeId)
                .recipeNombre(recipe.getNombre())
                .cantidadAProducir(cantidad)
                .materiales(new ArrayList<>(merged.values()))
                .build();
    }

    private void explodeRecursive(Long recipeId, BigDecimal cantidad, Set<Long> visited, List<BomExplosionItem> items) {
        List<BomComponent> componentes = bomComponentRepository.findByRecipeId(recipeId);

        for (BomComponent bc : componentes) {
            Product componente = productRepository.findById(bc.getComponenteId())
                    .orElse(null);
            if (componente == null) continue;

            BigDecimal cantidadNecesaria = bc.getCantidad().multiply(cantidad);

            // Check if this component is a finished good with its own recipe (sub-recipe)
            List<Recipe> subRecipes = recipeRepository.findByProductoTerminadoId(bc.getComponenteId())
                    .stream().filter(Recipe::getActiva).toList();

            if (!subRecipes.isEmpty()) {
                // It's a sub-recipe - explode recursively
                for (Recipe subRecipe : subRecipes) {
                    if (visited.contains(subRecipe.getId())) {
                        throw new BadRequestException("Ciclo detectado en receta: " + subRecipe.getNombre());
                    }
                    visited.add(subRecipe.getId());
                    explodeRecursive(subRecipe.getId(), cantidadNecesaria, visited, items);
                }
            } else {
                // It's a raw material (or a product without recipe)
                int disponible = (componente.getStock() != null ? componente.getStock() : 0)
                        - (componente.getStockReservado() != null ? componente.getStockReservado() : 0);
                boolean stockSuficiente = disponible >= cantidadNecesaria.intValue();
                int stockFaltante = stockSuficiente ? 0 : cantidadNecesaria.intValue() - disponible;

                items.add(BomExplosionItem.builder()
                        .productoId(componente.getId())
                        .productoNombre(componente.getName())
                        .productoSku(componente.getSku())
                        .tipo(componente.getTipo() != null ? componente.getTipo().name() : "")
                        .cantidadTotal(cantidadNecesaria)
                        .unidadMedida(bc.getUnidadMedida())
                        .precioPromedio(componente.getPrice())
                        .costoTotal(componente.getPrice().multiply(cantidadNecesaria))
                        .stockSuficiente(stockSuficiente)
                        .stockActual(disponible)
                        .stockFaltante(stockFaltante)
                        .build());
            }
        }
    }

    /**
     * Verifica disponibilidad de stock para todas las MPs de una receta.
     * Retorna lista de productos con stock insuficiente (vacia si todo ok).
     */
    public List<BomExplosionItem> checkStockAvailability(Long recipeId, BigDecimal cantidad) {
        BomExplosionResponse explosion = explode(recipeId, cantidad);
        return explosion.getMateriales().stream()
                .filter(m -> !m.getStockSuficiente())
                .toList();
    }

    /**
     * Calcula costo estimado de producir N unidades.
     */
    public CostEstimateResponse getCostEstimate(Long recipeId, BigDecimal cantidad) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new ResourceNotFoundException("Receta no encontrada"));

        BomExplosionResponse explosion = explode(recipeId, cantidad);

        List<CostItem> costItems = explosion.getMateriales().stream()
                .map(m -> CostItem.builder()
                        .productoId(m.getProductoId())
                        .productoNombre(m.getProductoNombre())
                        .cantidad(m.getCantidadTotal())
                        .precioUnitario(m.getPrecioPromedio())
                        .costoTotal(m.getCostoTotal())
                        .build())
                .toList();

        BigDecimal costoTotal = costItems.stream()
                .map(CostItem::getCostoTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal costoUnitario = costoTotal.divide(cantidad, 2, RoundingMode.HALF_UP);

        return CostEstimateResponse.builder()
                .recipeId(recipeId)
                .recipeNombre(recipe.getNombre())
                .cantidadAProducir(cantidad)
                .costoTotalEstimado(costoTotal)
                .costoUnitarioEstimado(costoUnitario)
                .items(costItems)
                .build();
    }
}
