package com.pos.system.service;

import com.pos.system.dto.request.ProductRequest;
import com.pos.system.dto.response.ProductResponse;
import com.pos.system.dto.response.ProductSearchResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductService {
    
    ProductResponse create(ProductRequest request);
    
    Page<ProductResponse> getAll(Pageable pageable);
    
    Page<ProductResponse> search(String search, Pageable pageable);
    
    ProductResponse getById(Long id);
    
    ProductResponse update(Long id, ProductRequest request);
    
    void delete(Long id);
    
    void updateStock(Long id, Integer quantity);

    // POS lightweight search
    List<ProductSearchResponse> searchForPos(String query, int limit);
}
