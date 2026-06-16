package com.pos.system.service;

import com.pos.system.dto.request.RecipeRequest;
import com.pos.system.dto.response.BomExplosionResponse;
import com.pos.system.dto.response.CostEstimateResponse;
import com.pos.system.dto.response.RecipeResponse;
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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecipeService {

    private final RecipeRepository recipeRepository;
    private final BomComponentRepository bomComponentRepository;
    private final ProductRepository productRepository;
    private final BomExplosionService bomExplosionService;

    @Transactional
    public RecipeResponse create(RecipeRequest request) {
        // Validate PT exists
        Product pt = productRepository.findById(request.getProductoTerminadoId())
                .orElseThrow(() -> new ResourceNotFoundException("Producto terminado no encontrado"));

        if (pt.getTipo() != Product.Tipo.PRODUCTO_TERMINADO) {
            throw new BadRequestException("El producto debe ser de tipo PRODUCTO_TERMINADO");
        }

        // Validate no cycle: component cannot be the same as PT
        if (request.getComponentes() != null) {
            for (var comp : request.getComponentes()) {
                if (comp.getComponenteId().equals(request.getProductoTerminadoId())) {
                    throw new BadRequestException("Un componente no puede ser el mismo producto terminado de la receta");
                }
            }
        }

        Recipe recipe = Recipe.builder()
                .nombre(request.getNombre())
                .descripcion(request.getDescripcion())
                .productoTerminadoId(request.getProductoTerminadoId())
                .cantidadProducida(request.getCantidadProducida())
                .unidadMedida(request.getUnidadMedida())
                .tiempoProduccionMinutos(request.getTiempoProduccionMinutos())
                .activa(true)
                .build();
        recipe = recipeRepository.save(recipe);

        if (request.getComponentes() != null) {
            saveComponents(recipe.getId(), request.getComponentes());
        }

        return mapToResponse(recipe);
    }

    @Transactional(readOnly = true)
    public RecipeResponse getById(Long id) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Receta no encontrada"));
        return mapToResponse(recipe);
    }

    @Transactional(readOnly = true)
    public List<RecipeResponse> listAll() {
        return recipeRepository.findByActivaTrue().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public RecipeResponse update(Long id, RecipeRequest request) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Receta no encontrada"));

        recipe.setNombre(request.getNombre());
        recipe.setDescripcion(request.getDescripcion());
        recipe.setCantidadProducida(request.getCantidadProducida());
        recipe.setUnidadMedida(request.getUnidadMedida());
        recipe.setTiempoProduccionMinutos(request.getTiempoProduccionMinutos());

        if (request.getProductoTerminadoId() != null) {
            recipe.setProductoTerminadoId(request.getProductoTerminadoId());
        }

        recipe = recipeRepository.save(recipe);

        // Replace components
        bomComponentRepository.deleteByRecipeId(recipe.getId());
        if (request.getComponentes() != null) {
            saveComponents(recipe.getId(), request.getComponentes());
        }

        return mapToResponse(recipe);
    }

    @Transactional
    public void delete(Long id) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Receta no encontrada"));
        recipe.setActiva(false);
        recipeRepository.save(recipe);
    }

    @Transactional(readOnly = true)
    public BomExplosionResponse getBomExplosion(Long recipeId, BigDecimal cantidad) {
        return bomExplosionService.explode(recipeId, cantidad);
    }

    @Transactional(readOnly = true)
    public CostEstimateResponse getCostEstimate(Long recipeId, BigDecimal cantidad) {
        return bomExplosionService.getCostEstimate(recipeId, cantidad);
    }

    private void saveComponents(Long recipeId, List<RecipeRequest.BomComponentRequest> componentes) {
        for (var comp : componentes) {
            Product prod = productRepository.findById(comp.getComponenteId())
                    .orElseThrow(() -> new ResourceNotFoundException("Componente no encontrado: " + comp.getComponenteId()));

            BomComponent bc = BomComponent.builder()
                    .recipeId(recipeId)
                    .componenteId(comp.getComponenteId())
                    .cantidad(comp.getCantidad())
                    .unidadMedida(comp.getUnidadMedida())
                    .esMermaEsperada(comp.getEsMermaEsperada() != null ? comp.getEsMermaEsperada() : false)
                    .porcentajeMermaEsperado(comp.getPorcentajeMermaEsperado())
                    .build();
            bomComponentRepository.save(bc);
        }
    }

    private RecipeResponse mapToResponse(Recipe recipe) {
        Product pt = productRepository.findById(recipe.getProductoTerminadoId()).orElse(null);

        List<BomComponent> componentes = bomComponentRepository.findByRecipeId(recipe.getId());
        List<RecipeResponse.BomComponentResponse> comps = componentes.stream().map(bc -> {
            Product prod = productRepository.findById(bc.getComponenteId()).orElse(null);
            return RecipeResponse.BomComponentResponse.builder()
                    .id(bc.getId())
                    .componenteId(bc.getComponenteId())
                    .componenteNombre(prod != null ? prod.getName() : null)
                    .componenteSku(prod != null ? prod.getSku() : null)
                    .componenteTipo(prod != null && prod.getTipo() != null ? prod.getTipo().name() : null)
                    .cantidad(bc.getCantidad())
                    .unidadMedida(bc.getUnidadMedida())
                    .esMermaEsperada(bc.getEsMermaEsperada())
                    .porcentajeMermaEsperado(bc.getPorcentajeMermaEsperado())
                    .build();
        }).toList();

        // Calculate estimated cost
        BigDecimal costoEstimado = BigDecimal.ZERO;
        try {
            CostEstimateResponse costEst = bomExplosionService.getCostEstimate(recipe.getId(), recipe.getCantidadProducida());
            costoEstimado = costEst.getCostoTotalEstimado();
        } catch (Exception ignored) {}

        return RecipeResponse.builder()
                .id(recipe.getId())
                .nombre(recipe.getNombre())
                .descripcion(recipe.getDescripcion())
                .productoTerminadoId(recipe.getProductoTerminadoId())
                .productoTerminadoNombre(pt != null ? pt.getName() : null)
                .cantidadProducida(recipe.getCantidadProducida())
                .unidadMedida(recipe.getUnidadMedida())
                .tiempoProduccionMinutos(recipe.getTiempoProduccionMinutos())
                .activa(recipe.getActiva())
                .costoEstimado(costoEstimado)
                .componentes(comps)
                .createdAt(recipe.getCreatedAt())
                .updatedAt(recipe.getUpdatedAt())
                .build();
    }
}
