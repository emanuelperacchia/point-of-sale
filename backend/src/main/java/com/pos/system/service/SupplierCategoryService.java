package com.pos.system.service;

import com.pos.system.dto.request.SupplierCategoryRequest;
import com.pos.system.dto.response.SupplierCategoryResponse;
import com.pos.system.entity.SupplierCategory;
import com.pos.system.exception.BadRequestException;
import com.pos.system.exception.ResourceNotFoundException;
import com.pos.system.repository.SupplierCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para la gestión de categorías de proveedores.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SupplierCategoryService {

    private final SupplierCategoryRepository categoryRepository;

    /**
     * Obtiene todas las categorías.
     */
    @Transactional(readOnly = true)
    public List<SupplierCategoryResponse> findAll() {
        log.debug("Finding all supplier categories");
        return categoryRepository.findAll().stream()
                .map(SupplierCategoryResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene solo las categorías activas.
     */
    @Transactional(readOnly = true)
    public List<SupplierCategoryResponse> findAllActive() {
        log.debug("Finding all active supplier categories");
        return categoryRepository.findByActiveTrue().stream()
                .map(SupplierCategoryResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Busca una categoría por su ID.
     */
    @Transactional(readOnly = true)
    public SupplierCategoryResponse findById(Long id) {
        log.debug("Finding supplier category by id: {}", id);
        SupplierCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Categoría de proveedor no encontrada con id: " + id));
        return SupplierCategoryResponse.fromEntity(category);
    }

    /**
     * Busca una categoría por su código.
     */
    @Transactional(readOnly = true)
    public SupplierCategoryResponse findByCode(String code) {
        log.debug("Finding supplier category by code: {}", code);
        SupplierCategory category = categoryRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Categoría de proveedor no encontrada con código: " + code));
        return SupplierCategoryResponse.fromEntity(category);
    }

    /**
     * Crea una nueva categoría de proveedor.
     */
    @Transactional
    public SupplierCategoryResponse create(SupplierCategoryRequest request) {
        log.info("Creating new supplier category with code: {}", request.getCode());

        if (categoryRepository.existsByCode(request.getCode())) {
            throw new BadRequestException(
                    "Ya existe una categoría con el código: " + request.getCode());
        }

        SupplierCategory category = SupplierCategory.builder()
                .code(request.getCode())
                .name(request.getName())
                .description(request.getDescription())
                .active(request.getActive() != null ? request.getActive() : true)
                .build();

        category = categoryRepository.save(category);
        log.info("Supplier category created with id: {}", category.getId());
        return SupplierCategoryResponse.fromEntity(category);
    }

    /**
     * Actualiza una categoría de proveedor existente.
     */
    @Transactional
    public SupplierCategoryResponse update(Long id, SupplierCategoryRequest request) {
        log.info("Updating supplier category with id: {}", id);
        SupplierCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Categoría de proveedor no encontrada con id: " + id));

        if (!category.getCode().equals(request.getCode())
                && categoryRepository.existsByCode(request.getCode())) {
            throw new BadRequestException(
                    "Ya existe una categoría con el código: " + request.getCode());
        }

        category.setCode(request.getCode());
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        if (request.getActive() != null) {
            category.setActive(request.getActive());
        }

        category = categoryRepository.save(category);
        log.info("Supplier category updated with id: {}", category.getId());
        return SupplierCategoryResponse.fromEntity(category);
    }

    /**
     * Elimina una categoría de proveedor (soft delete).
     */
    @Transactional
    public void delete(Long id) {
        log.info("Deleting supplier category with id: {}", id);
        SupplierCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Categoría de proveedor no encontrada con id: " + id));

        category.setActive(false);
        categoryRepository.save(category);
        log.info("Supplier category soft deleted with id: {}", id);
    }
}
