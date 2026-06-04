package com.pos.system.service.impl;

import com.pos.system.dto.request.CategoryRequest;
import com.pos.system.dto.response.CategoryResponse;
import com.pos.system.entity.Category;
import com.pos.system.exception.BadRequestException;
import com.pos.system.exception.ResourceNotFoundException;
import com.pos.system.repository.CategoryRepository;
import com.pos.system.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new BadRequestException("Ya existe una categoría con ese nombre");
        }

        Category category = Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .active(true)
                .build();

        return mapToResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CategoryResponse> getAll(Pageable pageable) {
        return categoryRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllActive() {
        return categoryRepository.findAll().stream()
                .filter(Category::getActive)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada"));
        return mapToResponse(category);
    }

    @Override
    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada"));

        if (!category.getName().equals(request.getName()) 
                && categoryRepository.existsByName(request.getName())) {
            throw new BadRequestException("Ya existe una categoría con ese nombre");
        }

        category.setName(request.getName());
        category.setDescription(request.getDescription());

        return mapToResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada"));
        category.setActive(false);
        categoryRepository.save(category);
    }

    private CategoryResponse mapToResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .active(category.getActive())
                .productCount(category.getProducts() != null ? category.getProducts().size() : 0)
                .build();
    }
}
