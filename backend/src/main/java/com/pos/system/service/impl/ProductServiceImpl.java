package com.pos.system.service.impl;

import com.pos.system.dto.request.ProductRequest;
import com.pos.system.dto.response.ProductResponse;
import com.pos.system.dto.response.ProductSearchResponse;
import com.pos.system.entity.Category;
import com.pos.system.entity.Product;
import com.pos.system.exception.BadRequestException;
import com.pos.system.exception.ResourceNotFoundException;
import com.pos.system.repository.CategoryRepository;
import com.pos.system.repository.ProductRepository;
import com.pos.system.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public ProductResponse create(ProductRequest request) {
        if (productRepository.existsByName(request.getName())) {
            throw new BadRequestException("Ya existe un producto con ese nombre");
        }
        if (productRepository.existsBySku(request.getSku())) {
            throw new BadRequestException("Ya existe un producto con ese SKU");
        }

        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada"));
        }

        Product.Tipo tipo = request.getTipo() != null
                ? Product.Tipo.valueOf(request.getTipo())
                : Product.Tipo.PRODUCTO_TERMINADO;

        Product product = Product.builder()
                .name(request.getName())
                .sku(request.getSku())
                .description(request.getDescription())
                .price(request.getPrice())
                .stock(request.getStock())
                .tipo(tipo)
                .costoProduccion(request.getCostoProduccion())
                .category(category)
                .active(true)
                .build();

        return mapToResponse(productRepository.save(product));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getByTipo(String tipo, Pageable pageable) {
        return productRepository.findByActiveTrueAndTipo(Product.Tipo.valueOf(tipo), pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getAll(Pageable pageable) {
        return productRepository.findByActiveTrue(pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> search(String search, Pageable pageable) {
        return productRepository.findByActiveTrueAndNameContainingIgnoreCase(search, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        Product product = productRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
        return mapToResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = productRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));

        if (!product.getName().equals(request.getName()) 
                && productRepository.existsByName(request.getName())) {
            throw new BadRequestException("Ya existe un producto con ese nombre");
        }
        if (!product.getSku().equals(request.getSku())
                && productRepository.existsBySku(request.getSku())) {
            throw new BadRequestException("Ya existe un producto con ese SKU");
        }

        product.setName(request.getName());
        product.setSku(request.getSku());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        if (request.getTipo() != null) {
            product.setTipo(Product.Tipo.valueOf(request.getTipo()));
        }
        if (request.getCostoProduccion() != null) {
            product.setCostoProduccion(request.getCostoProduccion());
        }

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada"));
            product.setCategory(category);
        } else {
            product.setCategory(null);
        }

        return mapToResponse(productRepository.save(product));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Product product = productRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
        product.setActive(false);
        productRepository.save(product);
    }

    @Override
    @Transactional
    public void updateStock(Long id, Integer quantity) {
        Product product = productRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));

        int newStock = product.getStock() + quantity;
        if (newStock < 0) {
            throw new BadRequestException("Stock no puede ser negativo");
        }

        product.setStock(newStock);
        productRepository.save(product);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductSearchResponse> searchForPos(String query, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return productRepository.searchForPos(query, pageable)
                .stream()
                .map(this::mapToSearchResponse)
                .collect(Collectors.toList());
    }

    private ProductSearchResponse mapToSearchResponse(Product product) {
        return ProductSearchResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .sku(product.getSku())
                .price(product.getPrice())
                .stock(product.getStock())
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .active(product.getActive())
                .build();
    }

    private ProductResponse mapToResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .sku(product.getSku())
                .description(product.getDescription())
                .price(product.getPrice())
                .stock(product.getStock())
                .tipo(product.getTipo() != null ? product.getTipo().name() : null)
                .costoProduccion(product.getCostoProduccion())
                .stockReservado(product.getStockReservado())
                .active(product.getActive())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .category(product.getCategory() != null 
                        ? ProductResponse.CategoryResponse.builder()
                                .id(product.getCategory().getId())
                                .name(product.getCategory().getName())
                                .build()
                        : null)
                .build();
    }
}
