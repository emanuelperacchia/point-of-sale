package com.pos.system.service.ecommerce;

import com.pos.system.entity.EcommerceConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Implementación genérica REST de EcommerceAdapter.
 * Espera una API REST estándar con endpoints configurables.
 * Endpoints por defecto:
 *   GET  {baseUrl}/orders?status=PENDING
 *   PUT  {baseUrl}/products/{id}/stock
 *   PUT  {baseUrl}/products/{id}
 *   PUT  {baseUrl}/orders/{id}/status
 */
@Component
@Slf4j
public class GenericRestEcommerceAdapter implements EcommerceAdapter {

    private final RestTemplate restTemplate = new RestTemplate();

    private HttpHeaders createHeaders(EcommerceConfig config) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-Key", config.getApiKey());
        return headers;
    }

    @Override
    public List<Map<String, Object>> getNewOrders(EcommerceConfig config) {
        try {
            String url = config.getBaseUrl() + "/orders?status=PENDING";
            HttpEntity<Void> entity = new HttpEntity<>(createHeaders(config));
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            if (response.getBody() != null && response.getBody().containsKey("data")) {
                return (List<Map<String, Object>>) response.getBody().get("data");
            }
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Error fetching orders from e-commerce {}: {}", config.getNombre(), e.getMessage());
            throw new RuntimeException("Error fetching orders: " + e.getMessage(), e);
        }
    }

    @Override
    public void updateProductStock(EcommerceConfig config, String externalProductId, int stock) {
        try {
            String url = config.getBaseUrl() + "/products/" + externalProductId + "/stock";
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(
                    Map.of("stock", stock), createHeaders(config));
            restTemplate.exchange(url, HttpMethod.PUT, entity, Void.class);
            log.info("Stock actualizado en e-commerce {}: producto {} -> {}", config.getNombre(), externalProductId, stock);
        } catch (Exception e) {
            log.error("Error updating stock in e-commerce {}: {}", config.getNombre(), e.getMessage());
            throw new RuntimeException("Error updating stock: " + e.getMessage(), e);
        }
    }

    @Override
    public void updateProduct(EcommerceConfig config, String externalProductId, Map<String, Object> productData) {
        try {
            String url = config.getBaseUrl() + "/products/" + externalProductId;
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(productData, createHeaders(config));
            restTemplate.exchange(url, HttpMethod.PUT, entity, Void.class);
            log.info("Producto actualizado en e-commerce {}: {}", config.getNombre(), externalProductId);
        } catch (Exception e) {
            log.error("Error updating product in e-commerce {}: {}", config.getNombre(), e.getMessage());
            throw new RuntimeException("Error updating product: " + e.getMessage(), e);
        }
    }

    @Override
    public void updateOrderStatus(EcommerceConfig config, String externalOrderId, String status) {
        try {
            String url = config.getBaseUrl() + "/orders/" + externalOrderId + "/status";
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(
                    Map.of("status", status), createHeaders(config));
            restTemplate.exchange(url, HttpMethod.PUT, entity, Void.class);
            log.info("Estado de pedido actualizado en e-commerce {}: {} -> {}", config.getNombre(), externalOrderId, status);
        } catch (Exception e) {
            log.error("Error updating order status in e-commerce {}: {}", config.getNombre(), e.getMessage());
            throw new RuntimeException("Error updating order status: " + e.getMessage(), e);
        }
    }
}
