package com.pos.system.service.supplier;

import com.pos.system.dto.supplier.SupplierRequest;
import com.pos.system.dto.supplier.SupplierResponse;
import com.pos.system.entity.supplier.Supplier;
import com.pos.system.repository.supplier.SupplierRepository;
import com.pos.system.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class SupplierService {

    @Autowired
    private SupplierRepository supplierRepository;

    public SupplierResponse createSupplier(SupplierRequest request) {
        if (supplierRepository.existsByCode(request.getCode())) {
            throw new RuntimeException("Ya existe un proveedor con este código");
        }

        Supplier supplier = Supplier.builder()
                .code(request.getCode())
                .taxId(request.getTaxId())
                .businessName(request.getBusinessName())
                .tradeName(request.getTradeName())
                .address(request.getAddress())
                .city(request.getCity())
                .state(request.getState())
                .postalCode(request.getPostalCode())
                .phone(request.getPhone())
                .email(request.getEmail())
                .website(request.getWebsite())
                .notes(request.getNotes())
                .active(true)
                .build();

        Supplier saved = supplierRepository.save(supplier);
        return mapToResponse(saved);
    }

    public SupplierResponse getSupplierById(Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado con ID: " + id));
        return mapToResponse(supplier);
    }

    public List<SupplierResponse> getAllSuppliers() {
        return supplierRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public SupplierResponse updateSupplier(Long id, SupplierRequest request) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado con ID: " + id));

        supplier.setCode(request.getCode());
        supplier.setTaxId(request.getTaxId());
        supplier.setBusinessName(request.getBusinessName());
        supplier.setTradeName(request.getTradeName());
        supplier.setAddress(request.getAddress());
        supplier.setPhone(request.getPhone());
        supplier.setEmail(request.getEmail());
        supplier.setActive(request.getActive());

        Supplier saved = supplierRepository.save(supplier);
        return mapToResponse(saved);
    }

    public SupplierResponse deleteSupplier(Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado con ID: " + id));
        supplier.setActive(false);
        Supplier saved = supplierRepository.save(supplier);
        return mapToResponse(supplier);
    }

    private SupplierResponse mapToResponse(Supplier supplier) {
        SupplierResponse response = new SupplierResponse();
        response.setId(supplier.getId());
        response.setCode(supplier.getCode());
        response.setTaxId(supplier.getTaxId());
        response.setBusinessName(supplier.getBusinessName());
        response.setTradeName(supplier.getTradeName());
        response.setAddress(supplier.getAddress());
        response.setCity(supplier.getCity());
        response.setState(supplier.getState());
        response.setPostalCode(supplier.getPostalCode());
        response.setPhone(supplier.getPhone());
        response.setEmail(supplier.getEmail());
        response.setWebsite(supplier.getWebsite());
        response.setNotes(supplier.getNotes());
        response.setActive(supplier.isActive());
        response.setCreatedAt(supplier.getCreatedAt());
        response.setUpdatedAt(supplier.getUpdatedAt());
        return response;
    }
}
