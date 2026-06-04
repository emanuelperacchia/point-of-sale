package com.pos.system.service;

import com.pos.system.dto.request.SupplierRequest;
import com.pos.system.dto.response.SupplierResponse;
import com.pos.system.entity.Supplier;
import com.pos.system.entity.SupplierCategory;
import com.pos.system.exception.BadRequestException;
import com.pos.system.exception.ResourceNotFoundException;
import com.pos.system.repository.SupplierCategoryRepository;
import com.pos.system.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para la gestión de proveedores.
 * Provee operaciones CRUD, búsqueda y consultas de deuda/rating.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final SupplierCategoryRepository categoryRepository;

    /**
     * Obtiene todos los proveedores.
     */
    @Transactional(readOnly = true)
    public List<SupplierResponse> findAll() {
        log.debug("Finding all suppliers");
        return supplierRepository.findAll().stream()
                .map(SupplierResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene solo los proveedores activos.
     */
    @Transactional(readOnly = true)
    public List<SupplierResponse> findAllActive() {
        log.debug("Finding all active suppliers");
        return supplierRepository.findByActiveTrue().stream()
                .map(SupplierResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Busca un proveedor por su ID.
     */
    @Transactional(readOnly = true)
    public SupplierResponse findById(Long id) {
        log.debug("Finding supplier by id: {}", id);
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado con id: " + id));
        return SupplierResponse.fromEntity(supplier);
    }

    /**
     * Busca un proveedor por su código.
     */
    @Transactional(readOnly = true)
    public SupplierResponse findByCode(String code) {
        log.debug("Finding supplier by code: {}", code);
        Supplier supplier = supplierRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado con código: " + code));
        return SupplierResponse.fromEntity(supplier);
    }

    /**
     * Crea un nuevo proveedor.
     */
    @Transactional
    public SupplierResponse create(SupplierRequest request) {
        log.info("Creating new supplier with code: {}", request.getCode());

        if (supplierRepository.existsByCode(request.getCode())) {
            throw new BadRequestException("Ya existe un proveedor con el código: " + request.getCode());
        }
        if (supplierRepository.existsByTaxId(request.getTaxId())) {
            throw new BadRequestException("Ya existe un proveedor con el RUT/CUIT: " + request.getTaxId());
        }

        Supplier supplier = mapToEntity(request);
        supplier = supplierRepository.save(supplier);
        log.info("Supplier created successfully with id: {}", supplier.getId());
        return SupplierResponse.fromEntity(supplier);
    }

    /**
     * Actualiza un proveedor existente.
     */
    @Transactional
    public SupplierResponse update(Long id, SupplierRequest request) {
        log.info("Updating supplier with id: {}", id);
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado con id: " + id));

        if (!supplier.getCode().equals(request.getCode())
                && supplierRepository.existsByCode(request.getCode())) {
            throw new BadRequestException("Ya existe un proveedor con el código: " + request.getCode());
        }
        if (!supplier.getTaxId().equals(request.getTaxId())
                && supplierRepository.existsByTaxId(request.getTaxId())) {
            throw new BadRequestException("Ya existe un proveedor con el RUT/CUIT: " + request.getTaxId());
        }

        updateEntity(supplier, request);
        supplier = supplierRepository.save(supplier);
        log.info("Supplier updated successfully with id: {}", supplier.getId());
        return SupplierResponse.fromEntity(supplier);
    }

    /**
     * Elimina un proveedor (soft delete) si no tiene deuda pendiente.
     */
    @Transactional
    public void delete(Long id) {
        log.info("Deleting (soft) supplier with id: {}", id);
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado con id: " + id));

        if (supplier.getCurrentDebt().compareTo(BigDecimal.ZERO) > 0) {
            throw new BadRequestException("No se puede eliminar un proveedor con deuda pendiente");
        }

        supplier.setActive(false);
        supplierRepository.save(supplier);
        log.info("Supplier soft deleted with id: {}", id);
    }

    /**
     * Busca proveedores por razón social o nombre comercial.
     */
    @Transactional(readOnly = true)
    public List<SupplierResponse> search(String searchTerm) {
        log.debug("Searching suppliers with term: {}", searchTerm);
        return supplierRepository.searchByName(searchTerm).stream()
                .map(SupplierResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene proveedores con deuda pendiente.
     */
    @Transactional(readOnly = true)
    public List<SupplierResponse> findSuppliersWithDebt() {
        log.debug("Finding suppliers with debt");
        return supplierRepository.findSuppliersWithDebt().stream()
                .map(SupplierResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene los proveedores mejor calificados (top 10).
     */
    @Transactional(readOnly = true)
    public List<SupplierResponse> findTopRatedSuppliers() {
        log.debug("Finding top rated suppliers");
        return supplierRepository.findTopRatedSuppliers().stream()
                .limit(10)
                .map(SupplierResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // -- Helper methods --

    private Supplier mapToEntity(SupplierRequest request) {
        Supplier.SupplierBuilder builder = Supplier.builder()
                .code(request.getCode())
                .taxId(request.getTaxId())
                .businessName(request.getBusinessName())
                .tradeName(request.getTradeName())
                .address(request.getAddress())
                .city(request.getCity())
                .state(request.getState())
                .postalCode(request.getPostalCode())
                .country(request.getCountry())
                .phone(request.getPhone())
                .email(request.getEmail())
                .website(request.getWebsite())
                .paymentTerm(request.getPaymentTerm())
                .discountPercentage(request.getDiscountPercentage() != null
                        ? request.getDiscountPercentage() : BigDecimal.ZERO)
                .creditLimit(request.getCreditLimit() != null
                        ? request.getCreditLimit() : BigDecimal.ZERO)
                .notes(request.getNotes())
                .active(request.getActive());

        if (request.getCategoryId() != null) {
            SupplierCategory category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Categoría no encontrada con id: " + request.getCategoryId()));
            builder.category(category);
        }

        return builder.build();
    }

    private void updateEntity(Supplier supplier, SupplierRequest request) {
        supplier.setCode(request.getCode());
        supplier.setTaxId(request.getTaxId());
        supplier.setBusinessName(request.getBusinessName());
        supplier.setTradeName(request.getTradeName());
        supplier.setAddress(request.getAddress());
        supplier.setCity(request.getCity());
        supplier.setState(request.getState());
        supplier.setPostalCode(request.getPostalCode());
        supplier.setCountry(request.getCountry());
        supplier.setPhone(request.getPhone());
        supplier.setEmail(request.getEmail());
        supplier.setWebsite(request.getWebsite());
        supplier.setPaymentTerm(request.getPaymentTerm());
        supplier.setDiscountPercentage(request.getDiscountPercentage());
        supplier.setCreditLimit(request.getCreditLimit());
        supplier.setNotes(request.getNotes());
        supplier.setActive(request.getActive());

        if (request.getCategoryId() != null) {
            SupplierCategory category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Categoría no encontrada con id: " + request.getCategoryId()));
            supplier.setCategory(category);
        } else {
            supplier.setCategory(null);
        }
    }
}
