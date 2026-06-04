package com.pos.system.service;

import com.pos.system.dto.request.CategoryRequest;
import com.pos.system.dto.response.CategoryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CategoryService {
    
    CategoryResponse create(CategoryRequest request);
    
    Page<CategoryResponse> getAll(Pageable pageable);
    
    List<CategoryResponse> getAllActive();
    
    CategoryResponse getById(Long id);
    
    CategoryResponse update(Long id, CategoryRequest request);
    
    void delete(Long id);
}
