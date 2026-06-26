package com.pos.system.service;

import com.pos.system.dto.response.PriceResolutionResponse;
import com.pos.system.entity.BranchPrice;
import com.pos.system.entity.Product;
import com.pos.system.repository.BranchPriceRepository;
import com.pos.system.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Resuelve el precio de un producto para una sucursal determinada.
 * Si existe un BranchPrice activo y vigente, usa ese.
 * Si no, usa el precio global del producto (Product.price).
 */
@Service
@RequiredArgsConstructor
public class PriceResolverService {

    private final BranchPriceRepository branchPriceRepository;
    private final ProductRepository productRepository;

    /**
     * Resuelve el precio de UN producto para la sucursal indicada.
     *
     * @return el precioLocal si existe y está vigente, o el precioGlobal como fallback.
     */
    @Transactional(readOnly = true)
    public BigDecimal resolve(Long productId, Long branchId) {
        if (branchId == null) {
            // ADMIN ve precio global
            return productRepository.findById(productId)
                    .map(Product::getPrice)
                    .orElse(BigDecimal.ZERO);
        }

        Optional<BranchPrice> bp = branchPriceRepository.findVigente(branchId, productId, LocalDate.now());
        if (bp.isPresent()) {
            return bp.get().getPrecio();
        }

        return productRepository.findById(productId)
                .map(Product::getPrice)
                .orElse(BigDecimal.ZERO);
    }

    /**
     * Resuelve precio con metadata: saber si es local o global, vigencia, etc.
     */
    @Transactional(readOnly = true)
    public PriceResolutionResponse resolveWithDetail(Long productId, Long branchId) {
        Product product = productRepository.findByIdAndActiveTrue(productId)
                .orElse(null);
        if (product == null) {
            return null;
        }

        PriceResolutionResponse.PriceResolutionResponseBuilder builder = PriceResolutionResponse.builder()
                .productId(product.getId())
                .productName(product.getName())
                .productSku(product.getSku())
                .precioGlobal(product.getPrice())
                .sucursalId(branchId);

        if (branchId != null) {
            Optional<BranchPrice> bp = branchPriceRepository.findVigente(branchId, productId, LocalDate.now());
            if (bp.isPresent()) {
                return builder
                        .precioLocal(bp.get().getPrecio())
                        .precioFinal(bp.get().getPrecio())
                        .esLocal(true)
                        .vigenciaHasta(bp.get().getVigenciaHasta())
                        .build();
            }
        }

        // Fallback al precio global
        return builder
                .precioLocal(null)
                .precioFinal(product.getPrice())
                .esLocal(false)
                .vigenciaHasta(null)
                .build();
    }

    /**
     * Resuelve los precios de TODOS los productos activos para una sucursal.
     * Usa LEFT JOIN para evitar N+1 queries.
     *
     * @return Map de productId → precio resuelto
     */
    @Transactional(readOnly = true)
    public Map<Long, BigDecimal> resolveAll(Long branchId) {
        List<Product> products = productRepository.findAll().stream()
                .filter(Product::getActive)
                .toList();

        if (branchId == null) {
            return products.stream()
                    .collect(Collectors.toMap(Product::getId, Product::getPrice));
        }

        List<BranchPrice> branchPrices = branchPriceRepository.findVigentesByBranchId(branchId, LocalDate.now());
        Map<Long, BigDecimal> priceMap = branchPrices.stream()
                .collect(Collectors.toMap(BranchPrice::getProductId, BranchPrice::getPrecio));

        return products.stream()
                .collect(Collectors.toMap(
                        Product::getId,
                        p -> priceMap.getOrDefault(p.getId(), p.getPrice())
                ));
    }
}
