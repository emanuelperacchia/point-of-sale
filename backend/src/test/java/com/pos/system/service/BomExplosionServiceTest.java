package com.pos.system.service;

import com.pos.system.dto.response.BomExplosionResponse;
import com.pos.system.dto.response.CostEstimateResponse;
import com.pos.system.entity.BomComponent;
import com.pos.system.entity.Product;
import com.pos.system.entity.Recipe;
import com.pos.system.exception.BadRequestException;
import com.pos.system.exception.ResourceNotFoundException;
import com.pos.system.repository.BomComponentRepository;
import com.pos.system.repository.ProductRepository;
import com.pos.system.repository.RecipeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BomExplosionServiceTest {

    @Mock private RecipeRepository recipeRepository;
    @Mock private BomComponentRepository bomComponentRepository;
    @Mock private ProductRepository productRepository;

    private BomExplosionService bomExplosionService;

    private final Long recipeId = 1L;
    private final Long ptId = 100L;
    private final Long mp1Id = 10L;
    private final Long mp2Id = 11L;

    @BeforeEach
    void setUp() {
        bomExplosionService = new BomExplosionService(recipeRepository, bomComponentRepository, productRepository);
    }

    // ── explode() ───────────────────────────────────────────────────────

    @Test
    void explode_WithRawMaterials_ShouldReturnMergedItems() {
        Recipe recipe = Recipe.builder()
                .id(recipeId)
                .nombre("Receta Test")
                .productoTerminadoId(ptId)
                .activa(true)
                .build();

        List<BomComponent> componentes = List.of(
                BomComponent.builder().id(1L).recipeId(recipeId).componenteId(mp1Id)
                        .cantidad(BigDecimal.valueOf(2)).unidadMedida("kg").build(),
                BomComponent.builder().id(2L).recipeId(recipeId).componenteId(mp2Id)
                        .cantidad(BigDecimal.valueOf(3)).unidadMedida("unid").build()
        );

        Product mp1 = Product.builder()
                .id(mp1Id).name("Harina").sku("HRN001")
                .price(BigDecimal.valueOf(100))
                .stock(50).stockReservado(0)
                .tipo(Product.Tipo.MATERIA_PRIMA)
                .build();

        Product mp2 = Product.builder()
                .id(mp2Id).name("Azucar").sku("AZC001")
                .price(BigDecimal.valueOf(200))
                .stock(10).stockReservado(0)
                .tipo(Product.Tipo.MATERIA_PRIMA)
                .build();

        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(recipe));
        when(bomComponentRepository.findByRecipeId(recipeId)).thenReturn(componentes);
        when(productRepository.findById(mp1Id)).thenReturn(Optional.of(mp1));
        when(productRepository.findById(mp2Id)).thenReturn(Optional.of(mp2));
        when(recipeRepository.findByProductoTerminadoId(mp1Id)).thenReturn(List.of());
        when(recipeRepository.findByProductoTerminadoId(mp2Id)).thenReturn(List.of());

        BomExplosionResponse response = bomExplosionService.explode(recipeId, BigDecimal.valueOf(10));

        assertNotNull(response);
        assertEquals(recipeId, response.getRecipeId());
        assertEquals("Receta Test", response.getRecipeNombre());
        assertEquals(BigDecimal.valueOf(10), response.getCantidadAProducir());
        assertEquals(2, response.getMateriales().size());

        var harina = response.getMateriales().stream()
                .filter(m -> m.getProductoNombre().equals("Harina")).findFirst().orElseThrow();
        assertEquals(BigDecimal.valueOf(20), harina.getCantidadTotal()); // 2 * 10
        assertEquals(BigDecimal.valueOf(2000), harina.getCostoTotal());  // 100 * 20
        assertTrue(harina.getStockSuficiente());

        var azucar = response.getMateriales().stream()
                .filter(m -> m.getProductoNombre().equals("Azucar")).findFirst().orElseThrow();
        assertEquals(BigDecimal.valueOf(30), azucar.getCantidadTotal()); // 3 * 10
        assertEquals(BigDecimal.valueOf(6000), azucar.getCostoTotal());  // 200 * 30
        assertFalse(azucar.getStockSuficiente()); // stock 10 < 30 needed
        assertEquals(20, azucar.getStockFaltante());
    }

    @Test
    void explode_WithSubRecipe_ShouldResolveRecursively() {
        Long subRecipeId = 2L;
        Long subMpId = 12L;

        Recipe mainRecipe = Recipe.builder()
                .id(recipeId).nombre("Torta").productoTerminadoId(ptId).activa(true).build();

        Recipe subRecipe = Recipe.builder()
                .id(subRecipeId).nombre("Masa").productoTerminadoId(mp1Id).activa(true).build();

        // Main recipe: uses "Masa" (product mp1Id) as component
        List<BomComponent> mainComponents = List.of(
                BomComponent.builder().id(1L).recipeId(recipeId).componenteId(mp1Id)
                        .cantidad(BigDecimal.ONE).unidadMedida("unid").build()
        );

        // Sub recipe: uses subMpId as raw material
        List<BomComponent> subComponents = List.of(
                BomComponent.builder().id(2L).recipeId(subRecipeId).componenteId(subMpId)
                        .cantidad(BigDecimal.valueOf(2)).unidadMedida("kg").build()
        );

        Product masa = Product.builder().id(mp1Id).name("Masa").sku("MSA001")
                .price(BigDecimal.valueOf(500)).stock(100).stockReservado(0)
                .tipo(Product.Tipo.PRODUCTO_TERMINADO).build();

        Product subMp = Product.builder().id(subMpId).name("Harina 000").sku("HRN002")
                .price(BigDecimal.valueOf(80)).stock(200).stockReservado(0)
                .tipo(Product.Tipo.MATERIA_PRIMA).build();

        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(mainRecipe));
        when(bomComponentRepository.findByRecipeId(recipeId)).thenReturn(mainComponents);
        when(productRepository.findById(mp1Id)).thenReturn(Optional.of(masa));
        // mp1Id is PT -> has sub-recipe
        when(recipeRepository.findByProductoTerminadoId(mp1Id)).thenReturn(List.of(subRecipe));

        // Sub-recipe explosion
        when(bomComponentRepository.findByRecipeId(subRecipeId)).thenReturn(subComponents);
        when(productRepository.findById(subMpId)).thenReturn(Optional.of(subMp));
        when(recipeRepository.findByProductoTerminadoId(subMpId)).thenReturn(List.of());

        BomExplosionResponse response = bomExplosionService.explode(recipeId, BigDecimal.valueOf(5));

        assertNotNull(response);
        assertEquals(1, response.getMateriales().size());
        assertEquals("Harina 000", response.getMateriales().get(0).getProductoNombre());
        // 1 (main qty) * 5 (cant) = 5 unids of "Masa" -> each requires 2 kg of Harina -> 10 kg total
        assertEquals(BigDecimal.valueOf(10), response.getMateriales().get(0).getCantidadTotal());
        assertEquals(BigDecimal.valueOf(800), response.getMateriales().get(0).getCostoTotal()); // 80 * 10
    }

    @Test
    void explode_WhenCycleDetected_ShouldThrowBadRequest() {
        Long subRecipeId = 2L;

        // mp1Id is PT from subRecipe, but subRecipe uses recipeId's PT -> cycle
        Recipe mainRecipe = Recipe.builder()
                .id(recipeId).nombre("Receta A").productoTerminadoId(ptId).activa(true).build();

        Recipe subRecipe = Recipe.builder()
                .id(subRecipeId).nombre("Receta B").productoTerminadoId(mp1Id).activa(true).build();

        List<BomComponent> mainComponents = List.of(
                BomComponent.builder().id(1L).recipeId(recipeId).componenteId(mp1Id)
                        .cantidad(BigDecimal.ONE).unidadMedida("unid").build()
        );

        List<BomComponent> subComponents = List.of(
                BomComponent.builder().id(2L).recipeId(subRecipeId).componenteId(ptId)
                        .cantidad(BigDecimal.ONE).unidadMedida("unid").build()
        );

        Product pt = Product.builder().id(ptId).name("Prod Terminado").sku("PT001")
                .price(BigDecimal.valueOf(1000)).stock(10).stockReservado(0)
                .tipo(Product.Tipo.PRODUCTO_TERMINADO).build();

        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(mainRecipe));
        when(bomComponentRepository.findByRecipeId(recipeId)).thenReturn(mainComponents);
        when(productRepository.findById(mp1Id)).thenReturn(Optional.of(pt));
        when(recipeRepository.findByProductoTerminadoId(mp1Id)).thenReturn(List.of(subRecipe));

        // Sub-recipe explosion - mp1Id triggers subRecipe
        when(bomComponentRepository.findByRecipeId(subRecipeId)).thenReturn(subComponents);
        when(productRepository.findById(ptId)).thenReturn(Optional.of(pt));
        when(recipeRepository.findByProductoTerminadoId(ptId)).thenReturn(List.of(mainRecipe));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> bomExplosionService.explode(recipeId, BigDecimal.valueOf(1)));
        assertTrue(ex.getMessage().contains("Ciclo detectado"));
    }

    @Test
    void explode_WhenRecipeNotFound_ShouldThrowResourceNotFound() {
        when(recipeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> bomExplosionService.explode(999L, BigDecimal.ONE));
    }

    @Test
    void explode_WhenComponentProductNotFound_ShouldSkip() {
        Recipe recipe = Recipe.builder()
                .id(recipeId).nombre("Receta").productoTerminadoId(ptId).activa(true).build();

        List<BomComponent> componentes = List.of(
                BomComponent.builder().id(1L).recipeId(recipeId).componenteId(999L)
                        .cantidad(BigDecimal.ONE).unidadMedida("kg").build()
        );

        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(recipe));
        when(bomComponentRepository.findByRecipeId(recipeId)).thenReturn(componentes);
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        BomExplosionResponse response = bomExplosionService.explode(recipeId, BigDecimal.ONE);

        assertNotNull(response);
        assertTrue(response.getMateriales().isEmpty());
    }

    // ── checkStockAvailability() ────────────────────────────────────────

    @Test
    void checkStockAvailability_WhenStockSufficient_ShouldReturnEmpty() {
        Recipe recipe = Recipe.builder()
                .id(recipeId).nombre("Receta").productoTerminadoId(ptId).activa(true).build();

        List<BomComponent> componentes = List.of(
                BomComponent.builder().id(1L).recipeId(recipeId).componenteId(mp1Id)
                        .cantidad(BigDecimal.ONE).unidadMedida("kg").build()
        );

        Product mp = Product.builder().id(mp1Id).name("Harina").sku("HRN")
                .price(BigDecimal.valueOf(100)).stock(100).stockReservado(0)
                .tipo(Product.Tipo.MATERIA_PRIMA).build();

        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(recipe));
        when(bomComponentRepository.findByRecipeId(recipeId)).thenReturn(componentes);
        when(productRepository.findById(mp1Id)).thenReturn(Optional.of(mp));
        when(recipeRepository.findByProductoTerminadoId(mp1Id)).thenReturn(List.of());

        var issues = bomExplosionService.checkStockAvailability(recipeId, BigDecimal.valueOf(10));

        assertTrue(issues.isEmpty());
    }

    @Test
    void checkStockAvailability_WhenStockInsufficient_ShouldReturnItems() {
        Recipe recipe = Recipe.builder()
                .id(recipeId).nombre("Receta").productoTerminadoId(ptId).activa(true).build();

        List<BomComponent> componentes = List.of(
                BomComponent.builder().id(1L).recipeId(recipeId).componenteId(mp1Id)
                        .cantidad(BigDecimal.ONE).unidadMedida("kg").build()
        );

        Product mp = Product.builder().id(mp1Id).name("Harina").sku("HRN")
                .price(BigDecimal.valueOf(100)).stock(5).stockReservado(0)
                .tipo(Product.Tipo.MATERIA_PRIMA).build();

        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(recipe));
        when(bomComponentRepository.findByRecipeId(recipeId)).thenReturn(componentes);
        when(productRepository.findById(mp1Id)).thenReturn(Optional.of(mp));
        when(recipeRepository.findByProductoTerminadoId(mp1Id)).thenReturn(List.of());

        var issues = bomExplosionService.checkStockAvailability(recipeId, BigDecimal.valueOf(10));

        assertEquals(1, issues.size());
        assertEquals("Harina", issues.get(0).getProductoNombre());
        assertEquals(5, issues.get(0).getStockFaltante()); // need 10, have 5
    }

    // ── getCostEstimate() ───────────────────────────────────────────────

    @Test
    void getCostEstimate_ShouldCalculateCosts() {
        Recipe recipe = Recipe.builder()
                .id(recipeId).nombre("Receta Costo").productoTerminadoId(ptId).activa(true).build();

        List<BomComponent> componentes = List.of(
                BomComponent.builder().id(1L).recipeId(recipeId).componenteId(mp1Id)
                        .cantidad(BigDecimal.valueOf(2)).unidadMedida("kg").build()
        );

        Product mp = Product.builder().id(mp1Id).name("Harina").sku("HRN")
                .price(BigDecimal.valueOf(150)).stock(100).stockReservado(0)
                .tipo(Product.Tipo.MATERIA_PRIMA).build();

        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(recipe));
        when(bomComponentRepository.findByRecipeId(recipeId)).thenReturn(componentes);
        when(productRepository.findById(mp1Id)).thenReturn(Optional.of(mp));
        when(recipeRepository.findByProductoTerminadoId(mp1Id)).thenReturn(List.of());

        CostEstimateResponse response = bomExplosionService.getCostEstimate(recipeId, BigDecimal.valueOf(5));

        assertNotNull(response);
        assertEquals(recipeId, response.getRecipeId());
        assertEquals("Receta Costo", response.getRecipeNombre());
        assertEquals(BigDecimal.valueOf(5), response.getCantidadAProducir());
        assertEquals(0, BigDecimal.valueOf(300).compareTo(response.getCostoUnitarioEstimado()));
        assertEquals(0, BigDecimal.valueOf(1500).compareTo(response.getCostoTotalEstimado()));

        assertEquals(1, response.getItems().size());
        assertEquals("Harina", response.getItems().get(0).getProductoNombre());
        assertEquals(0, BigDecimal.valueOf(10).compareTo(response.getItems().get(0).getCantidad()));
        assertEquals(0, BigDecimal.valueOf(150).compareTo(response.getItems().get(0).getPrecioUnitario()));
        assertEquals(0, BigDecimal.valueOf(1500).compareTo(response.getItems().get(0).getCostoTotal()));
    }

    @Test
    void getCostEstimate_WhenRecipeNotFound_ShouldThrowResourceNotFound() {
        when(recipeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> bomExplosionService.getCostEstimate(999L, BigDecimal.ONE));
    }
}
