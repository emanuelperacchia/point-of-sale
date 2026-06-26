package com.pos.system.service.ecommerce;

import com.pos.system.dto.request.SaleRequest;
import com.pos.system.dto.response.SaleResponse;
import com.pos.system.entity.*;
import com.pos.system.repository.*;
import com.pos.system.service.SaleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Importa pedidos desde el e-commerce y los convierte en ventas en el POS.
 * Ejecuta un job @Scheduled cada 5 minutos.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderImportService {

    private final EcommerceConfigRepository configRepository;
    private final EcommerceOrderRepository orderRepository;
    private final EcommerceSyncLogRepository syncLogRepository;
    private final ClientRepository clientRepository;
    private final SaleService saleService;
    private final EcommerceAdapter ecommerceAdapter;

    @Scheduled(fixedDelay = 300000) // cada 5 minutos
    @Transactional
    public void importPendingOrders() {
        List<EcommerceConfig> configs = configRepository.findByActivoTrue();

        for (EcommerceConfig config : configs) {
            try {
                List<Map<String, Object>> orders = ecommerceAdapter.getNewOrders(config);
                int imported = 0;
                int errors = 0;

                for (Map<String, Object> orderData : orders) {
                    String externalId = String.valueOf(orderData.get("id"));

                    // Evitar duplicados
                    if (orderRepository.findByConfigIdAndExternalOrderId(config.getId(), externalId).isPresent()) {
                        continue;
                    }

                    try {
                        importOrder(config, externalId, orderData);
                        imported++;
                    } catch (Exception e) {
                        errors++;
                        log.error("Error importando pedido {} de e-commerce {}: {}", externalId, config.getNombre(), e.getMessage());
                    }
                }

                // Actualizar última sincronización
                config.setUltimaSync(LocalDateTime.now());
                configRepository.save(config);

                syncLogRepository.save(EcommerceSyncLog.builder()
                        .configId(config.getId())
                        .tipo("ORDERS")
                        .resultado(errors > 0 && imported == 0 ? "ERROR" : "OK")
                        .mensaje("Importados: " + imported + ", errores: " + errors)
                        .itemsProcessed(imported + errors)
                        .build());

                if (imported > 0) {
                    log.info("Importados {} pedidos de e-commerce {}", imported, config.getNombre());
                }

            } catch (Exception e) {
                log.error("Error en importación de pedidos de e-commerce {}: {}", config.getNombre(), e.getMessage());
                syncLogRepository.save(EcommerceSyncLog.builder()
                        .configId(config.getId())
                        .tipo("ORDERS")
                        .resultado("ERROR")
                        .mensaje(e.getMessage())
                        .itemsProcessed(0)
                        .build());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void importOrder(EcommerceConfig config, String externalId, Map<String, Object> orderData) {
        // Buscar cliente por email
        String email = (String) orderData.get("email");
        Client client = null;
        if (email != null && !email.isBlank()) {
            List<Client> clients = clientRepository.findByActiveTrueAndEmailContainingIgnoreCase(email);
            if (!clients.isEmpty()) {
                client = clients.get(0);
            }
        }

        // Calcular total del pedido
        Number totalNum = (Number) orderData.get("total");
        BigDecimal total = totalNum != null ? BigDecimal.valueOf(totalNum.doubleValue()) : BigDecimal.ZERO;

        // Construir items del pedido
        List<Map<String, Object>> items = (List<Map<String, Object>>) orderData.get("items");
        List<SaleRequest.SaleItemRequest> saleItems = new ArrayList<>();

        if (items != null) {
            for (Map<String, Object> item : items) {
                Number productIdNum = (Number) item.get("product_id");
                Number quantityNum = (Number) item.get("quantity");
                if (productIdNum != null && quantityNum != null) {
                    SaleRequest.SaleItemRequest saleItem = new SaleRequest.SaleItemRequest();
                    saleItem.setProductId(productIdNum.longValue());
                    saleItem.setQuantity(quantityNum.intValue());
                    // Si el item trae descuento individual
                    Number discountNum = (Number) item.get("discount");
                    if (discountNum != null) {
                        saleItem.setDiscount(BigDecimal.valueOf(discountNum.doubleValue()));
                    }
                    saleItems.add(saleItem);
                }
            }
        }

        // Construir payment request
        SaleRequest.PaymentRequest payment = new SaleRequest.PaymentRequest();
        payment.setPaymentMethod("ONLINE");
        payment.setAmount(total);
        payment.setReference("ECOMMERCE-" + externalId);

        // Construir SaleRequest
        SaleRequest saleRequest = new SaleRequest();
        saleRequest.setItems(saleItems);
        saleRequest.setPayments(Collections.singletonList(payment));
        saleRequest.setClientId(client != null ? client.getId() : null);
        saleRequest.setNotes("Pedido web #" + externalId + " - " + config.getNombre());

        SaleResponse saleResponse = saleService.processSale(saleRequest, 1L);

        // Registrar el pedido importado
        EcommerceOrder ecomOrder = EcommerceOrder.builder()
                .configId(config.getId())
                .externalOrderId(externalId)
                .externalData(orderData.toString())
                .saleId(saleResponse.getId())
                .build();
        orderRepository.save(ecomOrder);

        // Actualizar estado en el e-commerce
        try {
            ecommerceAdapter.updateOrderStatus(config, externalId, "CONFIRMADO");
        } catch (Exception e) {
            log.warn("No se pudo actualizar estado del pedido {} en e-commerce: {}", externalId, e.getMessage());
        }
    }
}
