package com.pos.system.service;

import com.pos.system.dto.request.ProductionOrderRequest;
import com.pos.system.dto.response.BomExplosionResponse;
import com.pos.system.dto.response.ProductionOrderResponse;
import com.pos.system.entity.*;
import com.pos.system.exception.BadRequestException;
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
class ProductionServiceTest {

    @Mock private ProductionOrderRepository productionOrderRepository;
    @Mock private ProductionOrderComponentRepository poComponentRepository;
    @Mock private RecipeRepository recipeRepository;
    @Mock private BomComponentRepository bomComponentRepository;
    @Mock private ProductRepository productRepository;
    @Mock private LoteProduccionRepository loteProduccionRepository;
    @Mock private NotificationService notificationService;
    @Mock private BomExplosionService bomExplosionService;

    private ProductionService productionService;

    private final Long recipeId = 1L;
    private final Long ptId = 100L;
    private final Long mpId = 10L;
    private final Long orderId = 50L;
    private final Long responsableId = 200L;

    private Recipe recipe;
    private Product pt;
    private Product mp;
    private ProductionOrder order;
    private List<BomComponent> componentes;
    private BomExplosionResponse explosionResponse;

    @BeforeEach
    void setUp() {
        productionService = new ProductionService(
                productionOrderRepository, poComponentRepository, recipeRepository,
                bomComponentRepository, productRepository, loteProduccionRepository,
                notificationService, bomExplosionService);

        recipe = Recipe.builder()
                .id(recipeId).nombre("Receta Test").productoTerminadoId(ptId)
                .cantidadProducida(BigDecimal.ONE).unidadMedida("unid")
                .activa(true).build();

        pt = Product.builder().id(ptId).name("Prod Terminado").sku("PT001")
                .price(BigDecimal.valueOf(1000)).stock(10).stockReservado(0)
                .tipo(Product.Tipo.PRODUCTO_TERMINADO)
                .costoProduccion(BigDecimal.ZERO).build();

        mp = Product.builder().id(mpId).name("Harina").sku("HRN001")
                .price(BigDecimal.valueOf(100)).stock(50).stockReservado(0)
                .tipo(Product.Tipo.MATERIA_PRIMA).build();

        componentes = List.of(
                BomComponent.builder().id(1L).recipeId(recipeId).componenteId(mpId)
                        .cantidad(BigDecimal.valueOf(2)).unidadMedida("kg").build()
        );

        order = ProductionOrder.builder()
                .id(orderId).recipeId(recipeId)
                .cantidadPlanificada(10)
                .fechaPlanificada(LocalDate.now())
                .responsableId(responsableId)
                .estado(ProductionOrder.Estado.PLANIFICADA)
                .build();

        BomExplosionResponse.BomExplosionItem explosionItem =
                BomExplosionResponse.BomExplosionItem.builder()
                        .productoId(mpId)
                        .productoNombre("Harina")
                        .cantidadTotal(BigDecimal.valueOf(20)) // 2 * 10
                        .precioPromedio(BigDecimal.valueOf(100))
                        .costoTotal(BigDecimal.valueOf(2000))
                        .stockSuficiente(true)
                        .stockActual(50)
                        .stockFaltante(0)
                        .build();

        explosionResponse = BomExplosionResponse.builder()
                .recipeId(recipeId).recipeNombre("Receta Test")
                .cantidadAProducir(BigDecimal.TEN)
                .materiales(List.of(explosionItem))
                .build();
    }

    // ── create ──────────────────────────────────────────────────────────

    @Test
    void create_ShouldCreateOrderAndComponents() {
        ProductionOrderRequest request = new ProductionOrderRequest();
        request.setRecipeId(recipeId);
        request.setCantidadPlanificada(10);
        request.setFechaPlanificada(LocalDate.now());
        request.setResponsableId(responsableId);

        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(recipe));
        when(productionOrderRepository.save(any(ProductionOrder.class))).thenReturn(order);
        when(bomComponentRepository.findByRecipeId(recipeId)).thenReturn(componentes);
        when(bomExplosionService.explode(anyLong(), any(BigDecimal.class))).thenReturn(explosionResponse);
        when(bomExplosionService.checkStockAvailability(anyLong(), any(BigDecimal.class)))
                .thenReturn(List.of());
        when(poComponentRepository.findByProductionOrderId(orderId)).thenReturn(List.of());
        when(productRepository.findById(ptId)).thenReturn(Optional.of(pt));
        when(loteProduccionRepository.findByProductionOrderId(orderId)).thenReturn(null);

        ProductionOrderResponse response = productionService.create(request);

        assertNotNull(response);
        assertEquals(orderId, response.getId());
        assertEquals(ProductionOrder.Estado.PLANIFICADA.name(), response.getEstado());

        verify(productionOrderRepository).save(any(ProductionOrder.class));
        verify(poComponentRepository, times(1)).save(any(ProductionOrderComponent.class));
    }

    private ProductionOrderRequest makeRequest(Long recipeIdVal) {
        ProductionOrderRequest r = new ProductionOrderRequest();
        r.setRecipeId(recipeIdVal);
        r.setCantidadPlanificada(10);
        r.setFechaPlanificada(LocalDate.now());
        r.setResponsableId(responsableId);
        return r;
    }

    @Test
    void create_WhenRecipeNotActive_ShouldThrowBadRequest() {
        recipe.setActiva(false);
        ProductionOrderRequest request = makeRequest(recipeId);

        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(recipe));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> productionService.create(request));
        assertTrue(ex.getMessage().contains("activa"));
        verify(productionOrderRepository, never()).save(any());
    }

    @Test
    void create_WhenRecipeNotFound_ShouldThrowResourceNotFound() {
        ProductionOrderRequest request = makeRequest(999L);

        when(recipeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productionService.create(request));
    }

    @Test
    void create_WithStockIssues_ShouldStillCreate() {
        ProductionOrderRequest request = makeRequest(recipeId);

        BomExplosionResponse.BomExplosionItem stockIssue =
                BomExplosionResponse.BomExplosionItem.builder()
                        .productoId(mpId).productoNombre("Harina")
                        .cantidadTotal(BigDecimal.valueOf(20))
                        .stockSuficiente(false).stockActual(5).stockFaltante(15)
                        .build();

        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(recipe));
        when(productionOrderRepository.save(any(ProductionOrder.class))).thenReturn(order);
        when(bomComponentRepository.findByRecipeId(recipeId)).thenReturn(componentes);
        when(bomExplosionService.explode(anyLong(), any(BigDecimal.class))).thenReturn(explosionResponse);
        when(bomExplosionService.checkStockAvailability(anyLong(), any(BigDecimal.class)))
                .thenReturn(List.of(stockIssue));
        when(poComponentRepository.findByProductionOrderId(orderId)).thenReturn(List.of());
        when(productRepository.findById(ptId)).thenReturn(Optional.of(pt));
        when(loteProduccionRepository.findByProductionOrderId(orderId)).thenReturn(null);

        ProductionOrderResponse response = productionService.create(request);

        assertNotNull(response);
        assertEquals(orderId, response.getId());
        // Should still create even with stock issues
        verify(productionOrderRepository).save(any(ProductionOrder.class));
    }

    // ── start ────────────────────────────────────────────────────────────

    @Test
    void start_ShouldReserveStockAndSetEstadoEnProceso() {
        order.setEstado(ProductionOrder.Estado.PLANIFICADA);

        when(productionOrderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(recipe));
        when(bomExplosionService.explode(anyLong(), any(BigDecimal.class))).thenReturn(explosionResponse);
        when(productRepository.findById(mpId)).thenReturn(Optional.of(mp));
        when(productionOrderRepository.save(any(ProductionOrder.class))).thenAnswer(inv -> inv.getArgument(0));
        when(poComponentRepository.findByProductionOrderId(orderId)).thenReturn(List.of());
        when(productRepository.findById(ptId)).thenReturn(Optional.of(pt));
        when(loteProduccionRepository.findByProductionOrderId(orderId)).thenReturn(null);

        ProductionOrderResponse response = productionService.start(orderId);

        assertEquals(ProductionOrder.Estado.EN_PROCESO.name(), response.getEstado());
        assertEquals(20, mp.getStockReservado()); // reserved 20 units
        verify(productRepository).save(mp);
    }

    @Test
    void start_WhenNotPlanificada_ShouldThrowBadRequest() {
        order.setEstado(ProductionOrder.Estado.EN_PROCESO);

        when(productionOrderRepository.findById(orderId)).thenReturn(Optional.of(order));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> productionService.start(orderId));
        assertTrue(ex.getMessage().contains("PLANIFICADAS"));
        verify(productRepository, never()).save(any());
    }

    @Test
    void start_WhenOrderNotFound_ShouldThrowResourceNotFound() {
        when(productionOrderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productionService.start(999L));
    }

    // ── complete ─────────────────────────────────────────────────────────

    @Test
    void complete_ShouldDeductStockAndCreateLot() {
        order.setEstado(ProductionOrder.Estado.EN_PROCESO);
        order.setCantidadPlanificada(10);

        ProductionOrderComponent poc = ProductionOrderComponent.builder()
                .id(1L).productionOrderId(orderId).bomComponentId(1L)
                .cantidadPlanificada(BigDecimal.valueOf(20))
                .build();

        when(productionOrderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(recipe));
        when(productRepository.findById(ptId)).thenReturn(Optional.of(pt));
        when(poComponentRepository.findByProductionOrderId(orderId)).thenReturn(List.of(poc));
        when(bomComponentRepository.findById(1L)).thenReturn(Optional.of(componentes.get(0)));
        when(productRepository.findById(mpId)).thenReturn(Optional.of(mp));
        when(loteProduccionRepository.count()).thenReturn(0L);
        when(loteProduccionRepository.save(any(LoteProduccion.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(productionOrderRepository.save(any(ProductionOrder.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(poComponentRepository.save(any(ProductionOrderComponent.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        // For mapToResponse:
        when(poComponentRepository.findByProductionOrderId(orderId)).thenReturn(List.of(poc));
        when(bomComponentRepository.findById(1L)).thenReturn(Optional.of(componentes.get(0)));
        when(productRepository.findById(mpId)).thenReturn(Optional.of(mp));
        when(productRepository.findById(ptId)).thenReturn(Optional.of(pt));
        when(loteProduccionRepository.findByProductionOrderId(orderId)).thenReturn(null);

        ProductionOrderResponse response = productionService.complete(orderId, 8, null);

        assertEquals(ProductionOrder.Estado.COMPLETADA.name(), response.getEstado());
        assertEquals(8, response.getCantidadProducida());

        assertEquals(34, mp.getStock());    // 50 - 16 consumed
        assertEquals(0, mp.getStockReservado()); // was never reserved in this test

        // PT stock: 10 + 8 = 18
        assertEquals(18, pt.getStock());

        // Lot should be created
        verify(loteProduccionRepository).save(any(LoteProduccion.class));
    }

    @Test
    void complete_WithMerma_ShouldDeductExtraStock() {
        order.setEstado(ProductionOrder.Estado.EN_PROCESO);
        order.setCantidadPlanificada(10);

        ProductionOrderComponent poc = ProductionOrderComponent.builder()
                .id(1L).productionOrderId(orderId).bomComponentId(1L)
                .cantidadPlanificada(BigDecimal.valueOf(20))
                .build();

        // This component expects merma (2% expected)
        componentes.get(0).setEsMermaEsperada(true);
        componentes.get(0).setPorcentajeMermaEsperado(BigDecimal.valueOf(2));

        List<ProductionService.MermaEntry> mermaEntries = List.of(
                new ProductionService.MermaEntry(1L, BigDecimal.valueOf(3),
                        ProductionOrderComponent.MotivoMerma.PROCESO)
        );

        when(productionOrderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(recipe));
        when(productRepository.findById(ptId)).thenReturn(Optional.of(pt));
        when(poComponentRepository.findByProductionOrderId(orderId)).thenReturn(List.of(poc));
        when(bomComponentRepository.findById(1L)).thenReturn(Optional.of(componentes.get(0)));
        when(productRepository.findById(mpId)).thenReturn(Optional.of(mp));
        when(loteProduccionRepository.count()).thenReturn(0L);
        when(loteProduccionRepository.save(any(LoteProduccion.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(productionOrderRepository.save(any(ProductionOrder.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(poComponentRepository.save(any(ProductionOrderComponent.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        // For mapToResponse:
        when(poComponentRepository.findByProductionOrderId(orderId)).thenReturn(List.of(poc));
        when(bomComponentRepository.findById(1L)).thenReturn(Optional.of(componentes.get(0)));
        when(productRepository.findById(mpId)).thenReturn(Optional.of(mp));
        when(productRepository.findById(ptId)).thenReturn(Optional.of(pt));
        when(loteProduccionRepository.findByProductionOrderId(orderId)).thenReturn(null);

        ProductionOrderResponse response = productionService.complete(orderId, 8, mermaEntries);

        // Stock after: 50 - 16 (consumed) - 3 (merma) = 31
        assertEquals(31, mp.getStock());
    }

    @Test
    void complete_WhenNotEnProceso_ShouldThrowBadRequest() {
        order.setEstado(ProductionOrder.Estado.PLANIFICADA);
        when(productionOrderRepository.findById(orderId)).thenReturn(Optional.of(order));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> productionService.complete(orderId, 8, null));
        assertTrue(ex.getMessage().contains("EN_PROCESO"));
    }

    @Test
    void complete_WhenOrderNotFound_ShouldThrowResourceNotFound() {
        when(productionOrderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> productionService.complete(999L, 8, null));
    }

    // ── cancel ───────────────────────────────────────────────────────────

    @Test
    void cancel_WhenPlanificada_ShouldSetEstadoCancelada() {
        order.setEstado(ProductionOrder.Estado.PLANIFICADA);

        when(productionOrderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(productionOrderRepository.save(any(ProductionOrder.class))).thenAnswer(inv -> inv.getArgument(0));
        // mapToResponse needs recipe
        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(recipe));
        when(poComponentRepository.findByProductionOrderId(orderId)).thenReturn(List.of());
        when(productRepository.findById(ptId)).thenReturn(Optional.of(pt));
        when(loteProduccionRepository.findByProductionOrderId(orderId)).thenReturn(null);

        ProductionOrderResponse response = productionService.cancel(orderId);

        assertEquals(ProductionOrder.Estado.CANCELADA.name(), response.getEstado());
        verify(productRepository, never()).save(any()); // no stock release for PLANIFICADA
    }

    @Test
    void cancel_WhenEnProceso_ShouldReleaseStockAndCancel() {
        order.setEstado(ProductionOrder.Estado.EN_PROCESO);

        when(productionOrderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(recipe));
        when(bomExplosionService.explode(anyLong(), any(BigDecimal.class))).thenReturn(explosionResponse);
        when(productRepository.findById(mpId)).thenReturn(Optional.of(mp));
        when(productionOrderRepository.save(any(ProductionOrder.class))).thenAnswer(inv -> inv.getArgument(0));
        // mapToResponse
        when(poComponentRepository.findByProductionOrderId(orderId)).thenReturn(List.of());
        when(productRepository.findById(ptId)).thenReturn(Optional.of(pt));
        when(loteProduccionRepository.findByProductionOrderId(orderId)).thenReturn(null);

        ProductionOrderResponse response = productionService.cancel(orderId);

        assertEquals(ProductionOrder.Estado.CANCELADA.name(), response.getEstado());
        // Stock reserved should be released: 0 (was 0 before, didn't set any reserved in setUp)
        assertEquals(0, mp.getStockReservado());
        verify(productRepository).save(mp);
    }

    @Test
    void cancel_WhenCompletada_ShouldThrowBadRequest() {
        order.setEstado(ProductionOrder.Estado.COMPLETADA);

        when(productionOrderRepository.findById(orderId)).thenReturn(Optional.of(order));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> productionService.cancel(orderId));
        assertTrue(ex.getMessage().contains("COMPLETADA"));
    }

    @Test
    void cancel_WhenOrderNotFound_ShouldThrowResourceNotFound() {
        when(productionOrderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productionService.cancel(999L));
    }

    // ── getById / list ──────────────────────────────────────────────────

    @Test
    void getById_ShouldReturnResponse() {
        when(productionOrderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(recipe));
        when(poComponentRepository.findByProductionOrderId(orderId)).thenReturn(List.of());
        when(productRepository.findById(ptId)).thenReturn(Optional.of(pt));
        when(loteProduccionRepository.findByProductionOrderId(orderId)).thenReturn(null);

        ProductionOrderResponse response = productionService.getById(orderId);

        assertNotNull(response);
        assertEquals(orderId, response.getId());
    }

    @Test
    void getById_WhenNotFound_ShouldThrowResourceNotFound() {
        when(productionOrderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productionService.getById(999L));
    }

    @Test
    void listAll_ShouldReturnAllOrders() {
        when(productionOrderRepository.findAll()).thenReturn(List.of(order));
        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(recipe));
        when(poComponentRepository.findByProductionOrderId(orderId)).thenReturn(List.of());
        when(productRepository.findById(ptId)).thenReturn(Optional.of(pt));
        when(loteProduccionRepository.findByProductionOrderId(orderId)).thenReturn(null);

        List<ProductionOrderResponse> list = productionService.listAll();

        assertEquals(1, list.size());
        assertEquals(orderId, list.get(0).getId());
    }

    @Test
    void listByEstado_ShouldFilterByEstado() {
        when(productionOrderRepository.findByEstado(ProductionOrder.Estado.PLANIFICADA))
                .thenReturn(List.of(order));
        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(recipe));
        when(poComponentRepository.findByProductionOrderId(orderId)).thenReturn(List.of());
        when(productRepository.findById(ptId)).thenReturn(Optional.of(pt));
        when(loteProduccionRepository.findByProductionOrderId(orderId)).thenReturn(null);

        List<ProductionOrderResponse> list = productionService.listByEstado("PLANIFICADA");

        assertEquals(1, list.size());
    }
}
