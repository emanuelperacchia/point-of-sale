package com.pos.system.service.ecommerce;

import com.pos.system.entity.EcommerceConfig;

import java.util.List;
import java.util.Map;

/**
 * Interfaz agnóstica para integrar con cualquier plataforma de e-commerce.
 * Implementaciones concretas: WooCommerceAdapter, ShopifyAdapter, etc.
 */
public interface EcommerceAdapter {

    /**
     * Obtiene pedidos nuevos/no procesados desde el e-commerce.
     *
     * @return lista de mapas con datos del pedido: id externo, total, items, cliente, etc.
     */
    List<Map<String, Object>> getNewOrders(EcommerceConfig config);

    /**
     * Actualiza el stock de un producto en el e-commerce.
     */
    void updateProductStock(EcommerceConfig config, String externalProductId, int stock);

    /**
     * Actualiza datos del producto en el e-commerce (nombre, precio, descripción, activo).
     */
    void updateProduct(EcommerceConfig config, String externalProductId, Map<String, Object> productData);

    /**
     * Actualiza el estado de un pedido en el e-commerce.
     */
    void updateOrderStatus(EcommerceConfig config, String externalOrderId, String status);
}
