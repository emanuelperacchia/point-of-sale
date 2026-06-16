package com.pos.system.service;

import com.pos.system.dto.response.CostAnalysisResponse;
import com.pos.system.dto.response.CostEstimateResponse;
import com.pos.system.entity.*;
import com.pos.system.exception.ResourceNotFoundException;
import com.pos.system.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CostAnalysisServiceTest {

    @Mock private ProductionOrderRepository productionOrderRepository;
    @Mock private ProductionOrderComponentRepository poComponentRepository;
    @Mock private RecipeRepository recipeRepository;
    @Mock private BomComponentRepository bomComponentRepository;
    @Mock private ProductRepository productRepository;
    @Mock private BomExplosionService bomExplosionService;

    private CostAnalysisService costAnalysisService;

    private final Long orderId = 50L;
    private final Long recipeId = 1L;
    private final Long ptId = 100L;
    private final Long mpId = 10L;

    private ProductionOrder order;
    private Recipe recipe;
    private Product mp;
    private CostEstimateResponse costEstimate;

    @BeforeEach
    void setUp() {
        costAnalysisService = new CostAnalysisService(
                productionOrderRepository, poComponentRepository, recipeRepository,
                bomComponentRepository, productRepository, bomExplosionService);

        recipe = Recipe.builder()
                .id(recipeId).nombre("Receta Test").productoTerminadoId(ptId).activa(true).build();

        mp = Product.builder().id(mpId).name("Harina").sku("HRN001")
                .price(BigDecimal.valueOf(100))
                .stock(50).stockReservado(0)
                .tipo(Product.Tipo.MATERIA_PRIMA).build();

        order = ProductionOrder.builder()
                .id(orderId).recipeId(recipeId)
                .cantidadPlanificada(10)
                .cantidadProducida(8)
                .fechaPlanificada(LocalDate.now())
                .responsableId(200L)
                .estado(ProductionOrder.Estado.COMPLETADA)
                .build();

        costEstimate = CostEstimateResponse.builder()
                .recipeId(recipeId)
                .recipeNombre("Receta Test")
                .cantidadAProducir(BigDecimal.valueOf(10))
                .costoTotalEstimado(new BigDecimal("2000.00"))
                .costoUnitarioEstimado(new BigDecimal("200.00"))
                .items(List.of(
                        CostEstimateResponse.CostItem.builder()
                                .productoId(mpId)
                                .productoNombre("Harina")
                                .cantidad(BigDecimal.valueOf(20))
                                .precioUnitario(BigDecimal.valueOf(100))
                                .costoTotal(BigDecimal.valueOf(2000))
                                .build()
                ))
                .build();
    }

    @Test
    void analyze_ShouldCalculateDeviations() {
        ProductionOrderComponent poc = ProductionOrderComponent.builder()
                .id(1L).productionOrderId(orderId).bomComponentId(1L)
                .cantidadPlanificada(BigDecimal.valueOf(20))
                .cantidadConsumida(BigDecimal.valueOf(16))
                .mermaReal(BigDecimal.valueOf(2))
                .build();

        BomComponent bc = BomComponent.builder()
                .id(1L).recipeId(recipeId).componenteId(mpId)
                .cantidad(BigDecimal.valueOf(2)).unidadMedida("kg")
                .build();

        when(productionOrderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(recipe));
        when(bomExplosionService.getCostEstimate(recipeId, BigDecimal.valueOf(10)))
                .thenReturn(costEstimate);
        when(poComponentRepository.findByProductionOrderId(orderId)).thenReturn(List.of(poc));
        when(bomComponentRepository.findById(1L)).thenReturn(Optional.of(bc));
        when(productRepository.findById(mpId)).thenReturn(Optional.of(mp));

        CostAnalysisResponse response = costAnalysisService.analyze(orderId);

        assertNotNull(response);
        assertEquals(orderId, response.getProductionOrderId());
        assertEquals(recipeId, response.getRecipeId());
        assertEquals("Receta Test", response.getRecipeNombre());
        assertEquals(8, response.getCantidadProducida());

        // Estimated: 2000
        assertEquals(0, new BigDecimal("2000").compareTo(response.getCostoEstimadoTotal()));

        // Real: (16 + 2) * 100 = 18 * 100 = 1800
        assertEquals(0, new BigDecimal("1800").compareTo(response.getCostoRealTotal()));

        // Deviation: 1800 - 2000 = -200
        assertEquals(0, new BigDecimal("-200").compareTo(response.getDesviacion()));

        // Unit estimated: 200
        assertEquals(0, new BigDecimal("200").compareTo(response.getCostoUnitarioEstimado()));

        // Unit real: 1800 / 8 = 225
        assertEquals(0, new BigDecimal("225").compareTo(response.getCostoUnitarioReal()));

        // Components
        assertEquals(1, response.getComponentes().size());
        var comp = response.getComponentes().get(0);
        assertEquals("Harina", comp.getComponenteNombre());
        assertEquals(0, BigDecimal.valueOf(20).compareTo(comp.getCantidadPlanificada()));
        assertEquals(0, BigDecimal.valueOf(18).compareTo(comp.getCantidadConsumida())); // 16 + 2 merma
        assertEquals(0, BigDecimal.valueOf(100).compareTo(comp.getPrecioUnitario()));
        assertEquals(0, BigDecimal.valueOf(2000).compareTo(comp.getCostoEstimado())); // 20 * 100
        assertEquals(0, BigDecimal.valueOf(1800).compareTo(comp.getCostoReal()));     // 18 * 100
        assertEquals(0, BigDecimal.valueOf(-200).compareTo(comp.getDesviacion()));    // 1800 - 2000
    }

    @Test
    void analyze_WhenNoConsumption_ShouldUseZero() {
        ProductionOrderComponent poc = ProductionOrderComponent.builder()
                .id(1L).productionOrderId(orderId).bomComponentId(1L)
                .cantidadPlanificada(BigDecimal.valueOf(20))
                .cantidadConsumida(null)
                .mermaReal(null)
                .build();

        BomComponent bc = BomComponent.builder()
                .id(1L).recipeId(recipeId).componenteId(mpId)
                .cantidad(BigDecimal.valueOf(2)).unidadMedida("kg")
                .build();

        when(productionOrderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(recipe));
        when(bomExplosionService.getCostEstimate(recipeId, BigDecimal.valueOf(10)))
                .thenReturn(costEstimate);
        when(poComponentRepository.findByProductionOrderId(orderId)).thenReturn(List.of(poc));
        when(bomComponentRepository.findById(1L)).thenReturn(Optional.of(bc));
        when(productRepository.findById(mpId)).thenReturn(Optional.of(mp));

        CostAnalysisResponse response = costAnalysisService.analyze(orderId);

        assertNotNull(response);
        assertEquals(BigDecimal.ZERO, response.getComponentes().get(0).getCantidadConsumida());
        assertEquals(BigDecimal.ZERO, response.getComponentes().get(0).getCostoReal());
    }

    @Test
    void analyze_WhenOrderNotCompletada_ShouldThrowIllegalState() {
        order.setEstado(ProductionOrder.Estado.EN_PROCESO);

        when(productionOrderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(recipe));

        assertThrows(IllegalStateException.class, () -> costAnalysisService.analyze(orderId));
    }

    @Test
    void analyze_WhenOrderNotFound_ShouldThrowResourceNotFound() {
        when(productionOrderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> costAnalysisService.analyze(999L));
    }

    @Test
    void analyze_WhenBomComponentNotFound_ShouldSkipComponent() {
        ProductionOrderComponent poc = ProductionOrderComponent.builder()
                .id(1L).productionOrderId(orderId).bomComponentId(999L)
                .cantidadPlanificada(BigDecimal.valueOf(20))
                .cantidadConsumida(BigDecimal.valueOf(16))
                .build();

        when(productionOrderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(recipe));
        when(bomExplosionService.getCostEstimate(recipeId, BigDecimal.valueOf(10)))
                .thenReturn(costEstimate);
        when(poComponentRepository.findByProductionOrderId(orderId)).thenReturn(List.of(poc));
        when(bomComponentRepository.findById(999L)).thenReturn(Optional.empty());

        CostAnalysisResponse response = costAnalysisService.analyze(orderId);

        assertNotNull(response);
        assertTrue(response.getComponentes().isEmpty());
        assertEquals(BigDecimal.ZERO, response.getCostoRealTotal());
    }

    @Test
    void analyze_WhenProductNotFound_ShouldSkipComponent() {
        ProductionOrderComponent poc = ProductionOrderComponent.builder()
                .id(1L).productionOrderId(orderId).bomComponentId(1L)
                .cantidadPlanificada(BigDecimal.valueOf(20))
                .cantidadConsumida(BigDecimal.valueOf(16))
                .build();

        BomComponent bc = BomComponent.builder()
                .id(1L).recipeId(recipeId).componenteId(mpId)
                .cantidad(BigDecimal.valueOf(2)).unidadMedida("kg")
                .build();

        when(productionOrderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(recipe));
        when(bomExplosionService.getCostEstimate(recipeId, BigDecimal.valueOf(10)))
                .thenReturn(costEstimate);
        when(poComponentRepository.findByProductionOrderId(orderId)).thenReturn(List.of(poc));
        when(bomComponentRepository.findById(1L)).thenReturn(Optional.of(bc));
        when(productRepository.findById(mpId)).thenReturn(Optional.empty());

        CostAnalysisResponse response = costAnalysisService.analyze(orderId);

        assertNotNull(response);
        assertTrue(response.getComponentes().isEmpty());
    }

    @Test
    void analyze_WithMultipleComponents_ShouldCalculateCorrectly() {
        Long mp2Id = 11L;
        Product mp2 = Product.builder().id(mp2Id).name("Azucar").sku("AZC001")
                .price(BigDecimal.valueOf(200))
                .stock(30).stockReservado(0)
                .tipo(Product.Tipo.MATERIA_PRIMA).build();

        ProductionOrderComponent poc1 = ProductionOrderComponent.builder()
                .id(1L).productionOrderId(orderId).bomComponentId(1L)
                .cantidadPlanificada(BigDecimal.valueOf(20))
                .cantidadConsumida(BigDecimal.valueOf(16))
                .mermaReal(BigDecimal.valueOf(2))
                .build();

        ProductionOrderComponent poc2 = ProductionOrderComponent.builder()
                .id(2L).productionOrderId(orderId).bomComponentId(2L)
                .cantidadPlanificada(BigDecimal.valueOf(15))
                .cantidadConsumida(BigDecimal.valueOf(12))
                .mermaReal(BigDecimal.ZERO)
                .build();

        BomComponent bc1 = BomComponent.builder()
                .id(1L).recipeId(recipeId).componenteId(mpId)
                .cantidad(BigDecimal.valueOf(2)).unidadMedida("kg").build();

        BomComponent bc2 = BomComponent.builder()
                .id(2L).recipeId(recipeId).componenteId(mp2Id)
                .cantidad(BigDecimal.valueOf(1.5)).unidadMedida("kg").build();

        CostEstimateResponse.CostItem item2 = CostEstimateResponse.CostItem.builder()
                .productoId(mp2Id).productoNombre("Azucar")
                .cantidad(BigDecimal.valueOf(15))
                .precioUnitario(BigDecimal.valueOf(200))
                .costoTotal(BigDecimal.valueOf(3000))
                .build();

        CostEstimateResponse multiEstimate = CostEstimateResponse.builder()
                .recipeId(recipeId).recipeNombre("Receta Test")
                .cantidadAProducir(BigDecimal.valueOf(10))
                .costoTotalEstimado(new BigDecimal("5000.00"))
                .costoUnitarioEstimado(new BigDecimal("500.00"))
                .items(List.of(
                        CostEstimateResponse.CostItem.builder()
                                .productoId(mpId).productoNombre("Harina")
                                .cantidad(BigDecimal.valueOf(20))
                                .precioUnitario(BigDecimal.valueOf(100))
                                .costoTotal(BigDecimal.valueOf(2000))
                                .build(),
                        item2
                ))
                .build();

        when(productionOrderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(recipe));
        when(bomExplosionService.getCostEstimate(recipeId, BigDecimal.valueOf(10)))
                .thenReturn(multiEstimate);
        when(poComponentRepository.findByProductionOrderId(orderId))
                .thenReturn(List.of(poc1, poc2));
        when(bomComponentRepository.findById(1L)).thenReturn(Optional.of(bc1));
        when(bomComponentRepository.findById(2L)).thenReturn(Optional.of(bc2));
        when(productRepository.findById(mpId)).thenReturn(Optional.of(mp));
        when(productRepository.findById(mp2Id)).thenReturn(Optional.of(mp2));

        CostAnalysisResponse response = costAnalysisService.analyze(orderId);

        assertNotNull(response);
        assertEquals(0, new BigDecimal("5000").compareTo(response.getCostoEstimadoTotal()));
        // Real: (16+2)*100 + (12+0)*200 = 1800 + 2400 = 4200
        assertEquals(0, new BigDecimal("4200").compareTo(response.getCostoRealTotal()));
        // Deviation: 4200 - 5000 = -800
        assertEquals(0, new BigDecimal("-800").compareTo(response.getDesviacion()));
        // Unit real: 4200 / 8 = 525
        assertEquals(0, new BigDecimal("525").compareTo(response.getCostoUnitarioReal()));

        assertEquals(2, response.getComponentes().size());
    }
}
