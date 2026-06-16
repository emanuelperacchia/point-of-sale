package com.pos.system.service;

import com.pos.system.dto.request.ProductionOrderRequest;
import com.pos.system.dto.response.ProductionOrderResponse;
import com.pos.system.entity.*;
import com.pos.system.exception.BadRequestException;
import com.pos.system.exception.ResourceNotFoundException;
import com.pos.system.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductionService {

    private final ProductionOrderRepository productionOrderRepository;
    private final ProductionOrderComponentRepository poComponentRepository;
    private final RecipeRepository recipeRepository;
    private final BomComponentRepository bomComponentRepository;
    private final ProductRepository productRepository;
    private final LoteProduccionRepository loteProduccionRepository;
    private final NotificationService notificationService;
    private final BomExplosionService bomExplosionService;

    @Transactional
    public ProductionOrderResponse create(ProductionOrderRequest request) {
        Recipe recipe = recipeRepository.findById(request.getRecipeId())
                .orElseThrow(() -> new ResourceNotFoundException("Receta no encontrada"));

        if (!recipe.getActiva()) {
            throw new BadRequestException("La receta no esta activa");
        }

        ProductionOrder order = ProductionOrder.builder()
                .recipeId(request.getRecipeId())
                .cantidadPlanificada(request.getCantidadPlanificada())
                .fechaPlanificada(request.getFechaPlanificada())
                .responsableId(request.getResponsableId())
                .sucursalId(request.getSucursalId())
                .estado(ProductionOrder.Estado.PLANIFICADA)
                .observaciones(request.getObservaciones())
                .build();
        order = productionOrderRepository.save(order);

        // Create PO components from BOM
        BigDecimal cantidadBig = BigDecimal.valueOf(request.getCantidadPlanificada());
        List<BomComponent> bomComponents = bomComponentRepository.findByRecipeId(recipe.getId());

        // First explode recursively to get flattened BOM
        var explosion = bomExplosionService.explode(recipe.getId(), cantidadBig);

        // Map exploded items to PO components (simplified: use BOM components directly)
        for (BomComponent bc : bomComponents) {
            BigDecimal totalCantidad = bc.getCantidad().multiply(cantidadBig);
            ProductionOrderComponent poc = ProductionOrderComponent.builder()
                    .productionOrderId(order.getId())
                    .bomComponentId(bc.getId())
                    .cantidadPlanificada(totalCantidad)
                    .build();
            poComponentRepository.save(poc);
        }

        // Check stock and warn if insufficient
        var stockIssues = bomExplosionService.checkStockAvailability(recipe.getId(), cantidadBig);
        if (!stockIssues.isEmpty()) {
            String msg = stockIssues.stream()
                    .map(m -> m.getProductoNombre() + " (falta: " + m.getStockFaltante() + ")")
                    .collect(Collectors.joining(", "));
            // Notify responsible (if userId available)
            // notificationService.crear(...) would go here with actual userId
        }

        return mapToResponse(order);
    }

    @Transactional
    public ProductionOrderResponse start(Long orderId) {
        ProductionOrder order = productionOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Orden de produccion no encontrada"));

        if (order.getEstado() != ProductionOrder.Estado.PLANIFICADA) {
            throw new BadRequestException("Solo se pueden iniciar ordenes PLANIFICADAS");
        }

        Recipe recipe = recipeRepository.findById(order.getRecipeId())
                .orElseThrow(() -> new ResourceNotFoundException("Receta no encontrada"));

        BigDecimal cantidadBig = BigDecimal.valueOf(order.getCantidadPlanificada());
        var explosion = bomExplosionService.explode(recipe.getId(), cantidadBig);

        // Reserve stock for all raw materials
        for (var item : explosion.getMateriales()) {
            Product product = productRepository.findById(item.getProductoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + item.getProductoNombre()));
            int cantidadNecesaria = item.getCantidadTotal().intValue();
            int nuevoReservado = (product.getStockReservado() != null ? product.getStockReservado() : 0) + cantidadNecesaria;
            product.setStockReservado(nuevoReservado);
            productRepository.save(product);
        }

        order.setEstado(ProductionOrder.Estado.EN_PROCESO);
        order = productionOrderRepository.save(order);
        return mapToResponse(order);
    }

    @Transactional
    public ProductionOrderResponse complete(Long orderId, Integer cantidadProducida,
                                              List<MermaEntry> mermaEntries) {
        ProductionOrder order = productionOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Orden de produccion no encontrada"));

        if (order.getEstado() != ProductionOrder.Estado.EN_PROCESO) {
            throw new BadRequestException("Solo se pueden completar ordenes EN_PROCESO");
        }

        Recipe recipe = recipeRepository.findById(order.getRecipeId())
                .orElseThrow(() -> new ResourceNotFoundException("Receta no encontrada"));

        Product pt = productRepository.findById(recipe.getProductoTerminadoId())
                .orElseThrow(() -> new ResourceNotFoundException("Producto terminado no encontrado"));

        // Update PO components with actual consumption and waste
        List<ProductionOrderComponent> components = poComponentRepository.findByProductionOrderId(orderId);
        Map<Long, MermaEntry> mermaMap = mermaEntries != null
                ? mermaEntries.stream().collect(Collectors.toMap(MermaEntry::getBomComponentId, m -> m))
                : Map.of();

        BigDecimal costoRealTotal = BigDecimal.ZERO;
        boolean mermaExcedida = false;
        StringBuilder alertMsg = new StringBuilder();

        for (ProductionOrderComponent poc : components) {
            BomComponent bc = bomComponentRepository.findById(poc.getBomComponentId()).orElse(null);
            if (bc == null) continue;

            Product mp = productRepository.findById(bc.getComponenteId()).orElse(null);
            if (mp == null) continue;

            // Calculate actual proportion based on produced vs planned
            BigDecimal proporcion = BigDecimal.valueOf(cantidadProducida)
                    .divide(BigDecimal.valueOf(order.getCantidadPlanificada()), 4, RoundingMode.HALF_UP);
            BigDecimal cantidadConsumir = poc.getCantidadPlanificada().multiply(proporcion);

            // Deduct from stock (release from reserved, deduct from real)
            int consumirInt = cantidadConsumir.intValue();
            int nuevoReservado = Math.max(0, (mp.getStockReservado() != null ? mp.getStockReservado() : 0) - consumirInt);
            int nuevoStock = (mp.getStock() != null ? mp.getStock() : 0) - consumirInt;
            if (nuevoStock < 0) {
                throw new BadRequestException("Stock insuficiente de " + mp.getName() +
                        " para completar la produccion");
            }
            mp.setStockReservado(nuevoReservado);
            mp.setStock(nuevoStock);

            // Apply merma if any
            MermaEntry merma = mermaMap.get(poc.getBomComponentId());
            BigDecimal mermaReal = BigDecimal.ZERO;
            ProductionOrderComponent.MotivoMerma motivo = null;
            if (merma != null) {
                mermaReal = merma.getCantidad();
                motivo = merma.getMotivo();
                // Additional stock deduction for waste
                int mermaInt = mermaReal.intValue();
                nuevoStock = mp.getStock() - mermaInt;
                if (nuevoStock < 0) {
                    throw new BadRequestException("Stock insuficiente de " + mp.getName() +
                            " para cubrir la merma");
                }
                mp.setStock(nuevoStock);

                // Check if merma exceeds expected
                if (bc.getEsMermaEsperada() && bc.getPorcentajeMermaEsperado() != null
                        && mermaReal.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal mermaEsperada = cantidadConsumir
                            .multiply(bc.getPorcentajeMermaEsperado())
                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                    if (mermaReal.compareTo(mermaEsperada) > 0) {
                        mermaExcedida = true;
                        alertMsg.append("Merma excedida en ").append(mp.getName()).append(". ");
                    }
                }
            }

            productRepository.save(mp);

            // Calculate cost
            BigDecimal costoComponente = mp.getPrice().multiply(cantidadConsumir.add(mermaReal));
            costoRealTotal = costoRealTotal.add(costoComponente);

            poc.setCantidadConsumida(cantidadConsumir);
            poc.setMermaReal(mermaReal);
            poc.setMotivoMerma(motivo);
            poComponentRepository.save(poc);
        }

        // Generate alert if merma exceeded
        if (mermaExcedida) {
            // Could notify production manager
        }

        // Add PT to stock
        int nuevoStockPt = (pt.getStock() != null ? pt.getStock() : 0) + cantidadProducida;
        pt.setStock(nuevoStockPt);

        // Update costoProduccion on PT (average with existing)
        if (pt.getCostoProduccion() == null || pt.getCostoProduccion().compareTo(BigDecimal.ZERO) == 0) {
            pt.setCostoProduccion(costoRealTotal.divide(BigDecimal.valueOf(cantidadProducida), 2, RoundingMode.HALF_UP));
        } else {
            // Weighted average
            BigDecimal existingTotal = pt.getCostoProduccion().multiply(BigDecimal.valueOf(pt.getStock() - cantidadProducida));
            BigDecimal newTotal = costoRealTotal;
            BigDecimal combinedTotal = existingTotal.add(newTotal);
            int combinedQty = (pt.getStock() - cantidadProducida) + cantidadProducida;
            if (combinedQty > 0) {
                pt.setCostoProduccion(combinedTotal.divide(BigDecimal.valueOf(combinedQty), 2, RoundingMode.HALF_UP));
            }
        }
        productRepository.save(pt);

        // Generate lot number
        long lotCount = loteProduccionRepository.count() + 1;
        String numeroLote = String.format("LOT-%d-%04d", LocalDate.now().getYear(), lotCount);

        LoteProduccion lote = LoteProduccion.builder()
                .productionOrderId(orderId)
                .numeroLote(numeroLote)
                .fechaProduccion(LocalDate.now())
                .cantidad(cantidadProducida)
                .productoTerminadoId(recipe.getProductoTerminadoId())
                .build();
        loteProduccionRepository.save(lote);

        order.setEstado(ProductionOrder.Estado.COMPLETADA);
        order.setCantidadProducida(cantidadProducida);
        order = productionOrderRepository.save(order);

        return mapToResponse(order);
    }

    @Transactional
    public ProductionOrderResponse cancel(Long orderId) {
        ProductionOrder order = productionOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Orden de produccion no encontrada"));

        if (order.getEstado() == ProductionOrder.Estado.COMPLETADA) {
            throw new BadRequestException("No se puede cancelar una orden COMPLETADA");
        }

        // Release reserved stock if was in progress
        if (order.getEstado() == ProductionOrder.Estado.EN_PROCESO) {
            Recipe recipe = recipeRepository.findById(order.getRecipeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Receta no encontrada"));

            BigDecimal cantidadBig = BigDecimal.valueOf(order.getCantidadPlanificada());
            var explosion = bomExplosionService.explode(recipe.getId(), cantidadBig);

            for (var item : explosion.getMateriales()) {
                Product product = productRepository.findById(item.getProductoId()).orElse(null);
                if (product != null) {
                    int nuevoReservado = Math.max(0,
                            (product.getStockReservado() != null ? product.getStockReservado() : 0)
                                    - item.getCantidadTotal().intValue());
                    product.setStockReservado(nuevoReservado);
                    productRepository.save(product);
                }
            }
        }

        order.setEstado(ProductionOrder.Estado.CANCELADA);
        order = productionOrderRepository.save(order);
        return mapToResponse(order);
    }

    @Transactional(readOnly = true)
    public ProductionOrderResponse getById(Long id) {
        ProductionOrder order = productionOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Orden de produccion no encontrada"));
        return mapToResponse(order);
    }

    @Transactional(readOnly = true)
    public List<ProductionOrderResponse> listAll() {
        return productionOrderRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductionOrderResponse> listByEstado(String estado) {
        return productionOrderRepository.findByEstado(ProductionOrder.Estado.valueOf(estado))
                .stream().map(this::mapToResponse)
                .toList();
    }

    private ProductionOrderResponse mapToResponse(ProductionOrder order) {
        Recipe recipe = recipeRepository.findById(order.getRecipeId()).orElse(null);
        Product pt = recipe != null ? productRepository.findById(recipe.getProductoTerminadoId()).orElse(null) : null;

        List<ProductionOrderResponse.ProductionOrderComponentResponse> comps =
                poComponentRepository.findByProductionOrderId(order.getId()).stream().map(poc -> {
                    BomComponent bc = bomComponentRepository.findById(poc.getBomComponentId()).orElse(null);
                    Product prod = bc != null ? productRepository.findById(bc.getComponenteId()).orElse(null) : null;
                    return ProductionOrderResponse.ProductionOrderComponentResponse.builder()
                            .id(poc.getId())
                            .componenteId(prod != null ? prod.getId() : null)
                            .componenteNombre(prod != null ? prod.getName() : null)
                            .cantidadPlanificada(poc.getCantidadPlanificada())
                            .cantidadConsumida(poc.getCantidadConsumida())
                            .mermaReal(poc.getMermaReal())
                            .motivoMerma(poc.getMotivoMerma() != null ? poc.getMotivoMerma().name() : null)
                            .build();
                }).toList();

        LoteProduccion lote = null;
        try {
            lote = loteProduccionRepository.findByProductionOrderId(order.getId());
        } catch (Exception ignored) {}

        return ProductionOrderResponse.builder()
                .id(order.getId())
                .recipeId(order.getRecipeId())
                .recipeNombre(recipe != null ? recipe.getNombre() : null)
                .productoTerminadoNombre(pt != null ? pt.getName() : null)
                .cantidadPlanificada(order.getCantidadPlanificada())
                .cantidadProducida(order.getCantidadProducida())
                .fechaPlanificada(order.getFechaPlanificada())
                .responsableId(order.getResponsableId())
                .sucursalId(order.getSucursalId())
                .estado(order.getEstado().name())
                .observaciones(order.getObservaciones())
                .componentes(comps)
                .numeroLote(lote != null ? lote.getNumeroLote() : null)
                .createdAt(order.getCreatedAt())
                .build();
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class MermaEntry {
        private Long bomComponentId;
        private BigDecimal cantidad;
        private ProductionOrderComponent.MotivoMerma motivo;
    }
}
