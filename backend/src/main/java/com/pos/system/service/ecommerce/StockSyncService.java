package com.pos.system.service.ecommerce;

import com.pos.system.entity.EcommerceConfig;
import com.pos.system.entity.EcommerceSyncLog;
import com.pos.system.repository.EcommerceConfigRepository;
import com.pos.system.repository.EcommerceSyncLogRepository;
import com.pos.system.repository.ProductStockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Sincroniza cambios de stock con el e-commerce.
 * Se invoca desde el webhook STOCK_UPDATED o directamente.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StockSyncService {

    private final EcommerceConfigRepository configRepository;
    private final EcommerceSyncLogRepository syncLogRepository;
    private final ProductStockRepository productStockRepository;
    private final EcommerceAdapter ecommerceAdapter;

    @Transactional
    public void syncStock(Long productId, int stock) {
        List<EcommerceConfig> configs = configRepository.findByActivoTrue();

        for (EcommerceConfig config : configs) {
            try {
                // Buscar el ID externo del producto en la sucursal asociada
                String externalProductId = String.valueOf(productId);
                ecommerceAdapter.updateProductStock(config, externalProductId, stock);

                syncLogRepository.save(EcommerceSyncLog.builder()
                        .configId(config.getId())
                        .tipo("STOCK")
                        .resultado("OK")
                        .itemsProcessed(1)
                        .build());

                log.info("Stock sync OK: producto {} en e-commerce {}", productId, config.getNombre());
            } catch (Exception e) {
                syncLogRepository.save(EcommerceSyncLog.builder()
                        .configId(config.getId())
                        .tipo("STOCK")
                        .resultado("ERROR")
                        .mensaje(e.getMessage())
                        .itemsProcessed(0)
                        .build());
                log.error("Stock sync ERROR: producto {} en e-commerce {}: {}", productId, config.getNombre(), e.getMessage());
            }
        }
    }
}
