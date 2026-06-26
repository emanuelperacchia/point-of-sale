package com.pos.system.service.impl;

import com.pos.system.dto.request.BranchPriceRequest;
import com.pos.system.dto.response.BranchPriceResponse;
import com.pos.system.entity.BranchPrice;
import com.pos.system.entity.Product;
import com.pos.system.exception.BadRequestException;
import com.pos.system.exception.ResourceNotFoundException;
import com.pos.system.repository.BranchPriceRepository;
import com.pos.system.repository.ProductRepository;
import com.pos.system.service.BranchPriceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BranchPriceServiceImpl implements BranchPriceService {

    private final BranchPriceRepository branchPriceRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional
    public BranchPriceResponse create(Long branchId, BranchPriceRequest request, Long userId) {
        Product product = productRepository.findByIdAndActiveTrue(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + request.getProductId()));

        if (request.getVigenciaHasta() != null && request.getVigenciaDesde() != null
                && request.getVigenciaHasta().isBefore(request.getVigenciaDesde())) {
            throw new BadRequestException("La fecha de vigencia hasta debe ser posterior a la fecha desde");
        }

        // Check for existing active price for same (branch, product)
        if (branchPriceRepository.findByBranchIdAndProductIdAndActivoTrue(branchId, product.getId()).isPresent()) {
            throw new BadRequestException("Ya existe un precio local activo para este producto en esta sucursal");
        }

        BranchPrice branchPrice = BranchPrice.builder()
                .branchId(branchId)
                .productId(product.getId())
                .precio(request.getPrecio())
                .vigenciaDesde(request.getVigenciaDesde())
                .vigenciaHasta(request.getVigenciaHasta())
                .creadoPor(userId)
                .activo(true)
                .build();

        return mapToResponse(branchPriceRepository.save(branchPrice), product);
    }

    @Override
    @Transactional
    public BranchPriceResponse update(Long branchId, Long priceId, BranchPriceRequest request) {
        BranchPrice bp = branchPriceRepository.findById(priceId)
                .orElseThrow(() -> new ResourceNotFoundException("Precio local no encontrado"));

        if (!bp.getBranchId().equals(branchId)) {
            throw new BadRequestException("El precio no pertenece a esta sucursal");
        }

        Product product = productRepository.findByIdAndActiveTrue(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + request.getProductId()));

        if (request.getVigenciaHasta() != null && request.getVigenciaDesde() != null
                && request.getVigenciaHasta().isBefore(request.getVigenciaDesde())) {
            throw new BadRequestException("La fecha de vigencia hasta debe ser posterior a la fecha desde");
        }

        bp.setProductId(product.getId());
        bp.setPrecio(request.getPrecio());
        bp.setVigenciaDesde(request.getVigenciaDesde());
        bp.setVigenciaHasta(request.getVigenciaHasta());

        return mapToResponse(branchPriceRepository.save(bp), product);
    }

    @Override
    @Transactional
    public void delete(Long branchId, Long priceId) {
        BranchPrice bp = branchPriceRepository.findById(priceId)
                .orElseThrow(() -> new ResourceNotFoundException("Precio local no encontrado"));

        if (!bp.getBranchId().equals(branchId)) {
            throw new BadRequestException("El precio no pertenece a esta sucursal");
        }

        bp.setActivo(false);
        branchPriceRepository.save(bp);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BranchPriceResponse> getByBranch(Long branchId) {
        List<BranchPrice> prices = branchPriceRepository.findByBranchIdAndActivoTrue(branchId);
        return prices.stream()
                .map(bp -> {
                    Product product = productRepository.findById(bp.getProductId()).orElse(null);
                    return mapToResponse(bp, product);
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BranchPriceResponse getById(Long branchId, Long priceId) {
        BranchPrice bp = branchPriceRepository.findById(priceId)
                .orElseThrow(() -> new ResourceNotFoundException("Precio local no encontrado"));

        if (!bp.getBranchId().equals(branchId)) {
            throw new BadRequestException("El precio no pertenece a esta sucursal");
        }

        Product product = productRepository.findById(bp.getProductId()).orElse(null);
        return mapToResponse(bp, product);
    }

    @Override
    @Transactional
    public void syncGlobal(Long branchId) {
        int desactivados = branchPriceRepository.desactivarTodosPorBranch(branchId);
        log.info("Sincronización global para sucursal {}: {} precios locales desactivados", branchId, desactivados);
    }

    /**
     * Tarea programada diaria: desactiva precios locales cuya vigencia_hasta < hoy.
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void desactivarVencidos() {
        int desactivados = branchPriceRepository.desactivarVencidos(LocalDate.now());
        if (desactivados > 0) {
            log.info("Limpieza automática: {} precios locales vencidos desactivados", desactivados);
        }
    }

    private BranchPriceResponse mapToResponse(BranchPrice bp, Product product) {
        return BranchPriceResponse.builder()
                .id(bp.getId())
                .branchId(bp.getBranchId())
                .productId(bp.getProductId())
                .productName(product != null ? product.getName() : null)
                .productSku(product != null ? product.getSku() : null)
                .precio(bp.getPrecio())
                .precioGlobal(product != null ? product.getPrice() : null)
                .vigenciaDesde(bp.getVigenciaDesde())
                .vigenciaHasta(bp.getVigenciaHasta())
                .activo(bp.getActivo())
                .createdAt(bp.getCreatedAt())
                .build();
    }
}
