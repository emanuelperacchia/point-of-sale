package com.pos.system.service;

import com.pos.system.dto.request.SupplierRequest;
import com.pos.system.dto.response.SupplierResponse;
import com.pos.system.entity.Supplier;
import com.pos.system.entity.SupplierCategory;
import com.pos.system.exception.BadRequestException;
import com.pos.system.exception.ResourceNotFoundException;
import com.pos.system.repository.SupplierCategoryRepository;
import com.pos.system.repository.SupplierRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupplierServiceTest {

    @Mock private SupplierRepository supplierRepository;
    @Mock private SupplierCategoryRepository categoryRepository;

    private SupplierService supplierService;

    private Supplier supplier;
    private SupplierCategory category;
    private SupplierRequest request;

    @BeforeEach
    void setUp() {
        supplierService = new SupplierService(supplierRepository, categoryRepository);

        category = SupplierCategory.builder()
                .id(1L).code("MP").name("Materia Prima").active(true)
                .build();

        supplier = Supplier.builder()
                .id(1L).code("PROV001").taxId("30-12345678-9")
                .businessName("Proveedor S.A.").tradeName("Proveedor")
                .paymentTerm(Supplier.PaymentTerm.NET_30)
                .discountPercentage(BigDecimal.ZERO)
                .creditLimit(BigDecimal.valueOf(100000))
                .currentDebt(BigDecimal.ZERO)
                .rating(BigDecimal.valueOf(4.5))
                .active(true)
                .category(category)
                .build();

        request = SupplierRequest.builder()
                .code("PROV001").taxId("30-12345678-9")
                .businessName("Proveedor S.A.")
                .paymentTerm(Supplier.PaymentTerm.NET_30)
                .active(true)
                .build();
    }

    @Test
    void findAll_ShouldReturnList() {
        when(supplierRepository.findAll()).thenReturn(List.of(supplier));

        List<SupplierResponse> result = supplierService.findAll();

        assertEquals(1, result.size());
        assertEquals("Proveedor S.A.", result.get(0).getBusinessName());
    }

    @Test
    void findAllActive_ShouldReturnOnlyActive() {
        when(supplierRepository.findByActiveTrue()).thenReturn(List.of(supplier));

        List<SupplierResponse> result = supplierService.findAllActive();

        assertEquals(1, result.size());
        assertTrue(result.get(0).getActive());
    }

    @Test
    void findById_WhenExists_ShouldReturnSupplier() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));

        SupplierResponse result = supplierService.findById(1L);

        assertEquals("PROV001", result.getCode());
        assertEquals("Proveedor S.A.", result.getBusinessName());
    }

    @Test
    void findById_WhenNotExists_ShouldThrow() {
        when(supplierRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> supplierService.findById(99L));
    }

    @Test
    void findByCode_WhenExists_ShouldReturnSupplier() {
        when(supplierRepository.findByCode("PROV001")).thenReturn(Optional.of(supplier));

        SupplierResponse result = supplierService.findByCode("PROV001");

        assertEquals("30-12345678-9", result.getTaxId());
    }

    @Test
    void create_WithDuplicateCode_ShouldThrow() {
        when(supplierRepository.existsByCode("PROV001")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> supplierService.create(request));
    }

    @Test
    void create_WithDuplicateTaxId_ShouldThrow() {
        when(supplierRepository.existsByCode("PROV001")).thenReturn(false);
        when(supplierRepository.existsByTaxId("30-12345678-9")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> supplierService.create(request));
    }

    @Test
    void create_WithValidData_ShouldSucceed() {
        when(supplierRepository.existsByCode("PROV001")).thenReturn(false);
        when(supplierRepository.existsByTaxId("30-12345678-9")).thenReturn(false);
        when(supplierRepository.save(any())).thenReturn(supplier);

        SupplierResponse result = supplierService.create(request);

        assertEquals("PROV001", result.getCode());
        assertEquals("Proveedor S.A.", result.getBusinessName());
        verify(supplierRepository).save(any());
    }

    @Test
    void delete_WithDebt_ShouldThrow() {
        supplier.setCurrentDebt(BigDecimal.valueOf(50000));
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));

        assertThrows(BadRequestException.class, () -> supplierService.delete(1L));
    }

    @Test
    void delete_WithoutDebt_ShouldSoftDelete() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));

        supplierService.delete(1L);

        assertFalse(supplier.getActive());
        verify(supplierRepository).save(supplier);
    }

    @Test
    void search_ShouldReturnMatchingSuppliers() {
        when(supplierRepository.searchByName("Proveedor")).thenReturn(List.of(supplier));

        List<SupplierResponse> result = supplierService.search("Proveedor");

        assertEquals(1, result.size());
    }

    @Test
    void findSuppliersWithDebt_ShouldReturnList() {
        supplier.setCurrentDebt(BigDecimal.valueOf(10000));
        when(supplierRepository.findSuppliersWithDebt()).thenReturn(List.of(supplier));

        List<SupplierResponse> result = supplierService.findSuppliersWithDebt();

        assertEquals(1, result.size());
        assertTrue(result.get(0).getCurrentDebt().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void findTopRatedSuppliers_ShouldReturnRated() {
        when(supplierRepository.findTopRatedSuppliers()).thenReturn(List.of(supplier));

        List<SupplierResponse> result = supplierService.findTopRatedSuppliers();

        assertEquals(1, result.size());
        assertEquals(0, BigDecimal.valueOf(4.5).compareTo(result.get(0).getRating()));
    }
}
