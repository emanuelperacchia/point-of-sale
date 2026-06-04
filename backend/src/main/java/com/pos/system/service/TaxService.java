package com.pos.system.service;

import com.pos.system.entity.Product;
import com.pos.system.entity.Tax;
import com.pos.system.exception.BadRequestException;
import com.pos.system.exception.ResourceNotFoundException;
import com.pos.system.repository.ProductRepository;
import com.pos.system.repository.TaxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TaxService {

    private final TaxRepository taxRepository;
    private final ProductRepository productRepository;

    /**
     * CRUD - Listar impuestos activos
     */
    public List<Tax> getAllActiveTaxes() {
        return taxRepository.findByActiveTrue();
    }

    /**
     * CRUD - Obtener impuesto por ID
     */
    public Tax getTaxById(Long id) {
        return taxRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Impuesto no encontrado: " + id));
    }

    /**
     * CRUD - Crear impuesto
     */
    @Transactional
    public Tax createTax(Tax tax) {
        if (taxRepository.existsByCode(tax.getCode())) {
            throw new BadRequestException("Ya existe un impuesto con el código: " + tax.getCode());
        }
        if (taxRepository.existsByName(tax.getName())) {
            throw new BadRequestException("Ya existe un impuesto con el nombre: " + tax.getName());
        }
        return taxRepository.save(tax);
    }

    /**
     * CRUD - Actualizar impuesto
     */
    @Transactional
    public Tax updateTax(Long id, Tax taxData) {
        Tax tax = taxRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Impuesto no encontrado: " + id));
        tax.setName(taxData.getName());
        tax.setCode(taxData.getCode());
        tax.setDescription(taxData.getDescription());
        tax.setRate(taxData.getRate());
        tax.setType(taxData.getType());
        tax.setRegion(taxData.getRegion());
        tax.setActive(taxData.getActive());
        return taxRepository.save(tax);
    }

    /**
     * CRUD - Desactivar impuesto
     */
    @Transactional
    public void deactivateTax(Long id) {
        Tax tax = taxRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Impuesto no encontrado: " + id));
        tax.setActive(false);
        taxRepository.save(tax);
    }

    /**
     * Asigna impuestos a un producto
     */
    @Transactional
    public Product assignTaxesToProduct(Long productId, List<Long> taxIds) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + productId));

        List<Tax> taxes = taxRepository.findAllById(taxIds);
        product.setTaxes(Set.copyOf(taxes));
        return productRepository.save(product);
    }

    /**
     * Calcula el precio final de un producto incluyendo todos sus impuestos
     */
    public BigDecimal calculateProductFinalPrice(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + productId));

        BigDecimal basePrice = product.getPrice();
        BigDecimal totalTaxes = BigDecimal.ZERO;

        if (product.getTaxes() != null) {
            for (Tax tax : product.getTaxes()) {
                totalTaxes = totalTaxes.add(tax.calculateTax(basePrice));
            }
        }

        return basePrice.add(totalTaxes);
    }

    /**
     * Obtiene impuestos por región
     */
    public List<Tax> getTaxesByRegion(String region) {
        return taxRepository.findByActiveTrueAndRegion(region);
    }
}