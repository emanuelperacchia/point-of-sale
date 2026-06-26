package com.pos.system.service.ecommerce;

import com.pos.system.dto.response.ProductResponse;
import com.pos.system.entity.EcommerceConfig;
import com.pos.system.entity.EcommerceSyncLog;
import com.pos.system.repository.EcommerceConfigRepository;
import com.pos.system.repository.EcommerceSyncLogRepository;
import com.pos.system.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Sincroniza el catálogo de productos con el e-commerce.
 * Se ejecuta cuando se crea o modifica un producto.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CatalogSyncService {

    private final EcommerceConfigRepository configRepository;
    private final EcommerceSyncLogRepository syncLogRepository;
    private final ProductService productService;
    private final EcommerceAdapter ecommerceAdapter;

    @Transactional
    public void syncProduct(Long productId) {
        List<EcommerceConfig> configs = configRepository.findByActivoTrue();
        if (configs.isEmpty()) return;

        ProductResponse product;
        try {
            product = productService.getById(productId);
        } catch (Exception e) {
            log.warn("Producto {} no encontrado para sync", productId);
            return;
        }

        String externalProductId = String.valueOf(productId);
        Map<String, Object> productData = Map.of(
                "name", product.getName() != null ? product.getName() : "",
                "description", product.getDescription() != null ? product.getDescription() : "",
                "price", product.getPrice() != null ? product.getPrice().doubleValue() : 0,
                "stock", product.getStock() != null ? product.getStock() : 0,
                "active", product.getActive() != null ? product.getActive() : true
        );

        for (EcommerceConfig config : configs) {
            try {
                ecommerceAdapter.updateProduct(config, externalProductId, productData);

                syncLogRepository.save(EcommerceSyncLog.builder()
                        .configId(config.getId())
                        .tipo("CATALOG")
                        .resultado("OK")
                        .itemsProcessed(1)
                        .build());
            } catch (Exception e) {
                syncLogRepository.save(EcommerceSyncLog.builder()
                        .configId(config.getId())
                        .tipo("CATALOG")
                        .resultado("ERROR")
                        .mensaje(e.getMessage())
                        .itemsProcessed(0)
                        .build());
                log.error("Catalog sync ERROR: producto {} en e-commerce {}: {}", productId, config.getNombre(), e.getMessage());
            }
        }
    }
}
