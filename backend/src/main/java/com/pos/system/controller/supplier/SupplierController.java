package com.pos.system.controller.supplier;

import com.pos.system.dto.supplier.SupplierRequest;
import com.pos.system.dto.supplier.SupplierResponse;
import com.pos.system.entity.supplier.Supplier;
import com.pos.system.repository.supplier.SupplierRepository;
import com.pos.system.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/suppliers")
public class SupplierController {

    @Autowired
    private SupplierRepository supplierRepository;

    @PostMapping
    public ResponseEntity<SupplierResponse> createSupplier(@RequestBody SupplierRequest request) {
        if (supplierRepository.existsByCode(request.getCode())) {
            throw new ResourceNotFoundException("Ya existe un proveedor con este código");
        }

        Supplier supplier = Supplier.builder()
                .code(request.getCode())
                .taxId(request.getTaxId())
                .businessName(request.getBusinessName())
                .tradeName(request.getTradeName())
                .address(request.getAddress())
                .phone(request.getPhone())
                .email(request.getEmail())
                .active(request.getActive())
                .build();

        Supplier saved = supplierRepository.save(supplier);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(saved));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SupplierResponse> getSupplierById(@PathVariable Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado con ID: " + id));
        return ResponseEntity.ok(mapToResponse(supplier));
    }

    @GetMapping
    public ResponseEntity<List<SupplierResponse>> getAllSuppliers() {
        List<SupplierResponse> suppliers = supplierRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(suppliers);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SupplierResponse> updateSupplier(@PathVariable Long id, @RequestBody SupplierRequest request) {
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
        return ResponseEntity.ok(mapToResponse(saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSupplier(@PathVariable Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado con ID: " + id));
        supplier.setActive(false);
        Supplier saved = supplierRepository.save(supplier);
        return ResponseEntity.ok(mapToResponse(supplier));
    }

    private SupplierResponse mapToResponse(Supplier supplier) {
        SupplierResponse response = new SupplierResponse();
        response.setId(supplier.getId());
        response.setCode(supplier.getCode());
        response.setTaxId(supplier.getTaxId());
        response.setBusinessName(supplier.getBusinessName());
        response.setTradeName(supplier.getTradeName());
        response.setAddress(supplier.getAddress());
        response.setPhone(supplier.getPhone());
        response.setEmail(supplier.getEmail());
        response.setActive(supplier.isActive());
        response.setCreatedAt(supplier.getCreatedAt());
        response.setUpdatedAt(supplier.getUpdatedAt());
        return response;
    }
}
