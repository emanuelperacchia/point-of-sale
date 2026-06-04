package com.pos.system.service;

import com.pos.system.entity.Product;
import com.pos.system.entity.Tax;
import com.pos.system.exception.BadRequestException;
import com.pos.system.exception.ResourceNotFoundException;
import com.pos.system.repository.ProductRepository;
import com.pos.system.repository.TaxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaxServiceTest {

    @Mock private TaxRepository taxRepository;
    @Mock private ProductRepository productRepository;

    private TaxService taxService;

    private Tax iva;

    @BeforeEach
    void setUp() {
        taxService = new TaxService(taxRepository, productRepository);
        iva = Tax.builder()
                .id(1L).name("IVA").code("IVA")
                .rate(BigDecimal.valueOf(21))
                .type(Tax.TaxType.ADDED)
                .build();
    }

    @Test
    void getAllActiveTaxes_ShouldReturnList() {
        when(taxRepository.findByActiveTrue()).thenReturn(List.of(iva));
        assertEquals(1, taxService.getAllActiveTaxes().size());
    }

    @Test
    void createTax_WithDuplicateCode_ShouldThrow() {
        when(taxRepository.existsByCode("IVA")).thenReturn(true);
        assertThrows(BadRequestException.class, () -> taxService.createTax(iva));
    }

    @Test
    void calculateProductFinalPrice_ShouldIncludeTaxes() {
        Product product = Product.builder()
                .id(1L).name("Test").sku("TST")
                .price(BigDecimal.valueOf(100))
                .taxes(Set.of(iva))
                .build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        BigDecimal finalPrice = taxService.calculateProductFinalPrice(1L);
        assertEquals(0, BigDecimal.valueOf(121.00).compareTo(finalPrice));
    }

    @Test
    void calculateTax_WithExemptType_ShouldReturnZero() {
        Tax exempt = Tax.builder()
                .name("Exento").code("EX")
                .rate(BigDecimal.TEN).type(Tax.TaxType.EXEMPT)
                .build();
        assertEquals(0, BigDecimal.ZERO.compareTo(exempt.calculateTax(BigDecimal.valueOf(100))));
    }

    @Test
    void calculateTax_WithAddedType_ShouldCalculate() {
        assertEquals(0, BigDecimal.valueOf(21.00).compareTo(iva.calculateTax(BigDecimal.valueOf(100))));
    }

    @Test
    void getTaxById_WhenNotFound_ShouldThrow() {
        when(taxRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> taxService.getTaxById(99L));
    }

    @Test
    void assignTaxesToProduct_ShouldSetTaxes() {
        Product product = Product.builder().id(1L).name("Test").sku("TST").build();
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(taxRepository.findAllById(List.of(1L))).thenReturn(List.of(iva));
        when(productRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Product result = taxService.assignTaxesToProduct(1L, List.of(1L));
        assertEquals(1, result.getTaxes().size());
    }
}