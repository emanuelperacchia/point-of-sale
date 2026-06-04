package com.pos.system.service;

import com.pos.system.dto.response.KardexResponse;
import com.pos.system.dto.response.StockStatusResponse;
import com.pos.system.entity.Product;
import com.pos.system.entity.ProductStock;
import com.pos.system.entity.StockMovement;
import com.pos.system.entity.Warehouse;
import com.pos.system.exception.ResourceNotFoundException;
import com.pos.system.repository.ProductRepository;
import com.pos.system.repository.ProductStockRepository;
import com.pos.system.repository.StockMovementRepository;
import com.pos.system.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KardexService {

    private final StockMovementRepository stockMovementRepository;
    private final ProductStockRepository productStockRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;

    /**
     * Obtiene el kardex (historial de movimientos) de un producto
     */
    public Page<KardexResponse> getProductKardex(Long productId, Pageable pageable) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + productId));

        return stockMovementRepository
                .findByProductOrderByCreatedAtDesc(product, pageable)
                .map(this::toKardexResponse);
    }

    /**
     * Obtiene el kardex de un producto en una bodega específica
     */
    public Page<KardexResponse> getProductKardexByWarehouse(Long productId, Long warehouseId, Pageable pageable) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + productId));
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Bodega no encontrada: " + warehouseId));

        return stockMovementRepository
                .findByProductAndWarehouseOrderByCreatedAtDesc(product, warehouse, pageable)
                .map(this::toKardexResponse);
    }

    /**
     * Obtiene el stock actual de un producto en todas las bodegas
     */
    public List<StockStatusResponse> getProductStockStatus(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + productId));

        List<ProductStock> stocks = productStockRepository.findByProduct(product);

        return stocks.stream()
                .map(this::toStockStatusResponse)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene el stock actual de un producto en una bodega específica
     */
    public StockStatusResponse getProductStockByWarehouse(Long productId, Long warehouseId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + productId));
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Bodega no encontrada: " + warehouseId));

        ProductStock productStock = productStockRepository
                .findByProductAndWarehouse(product, warehouse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Stock no encontrado para el producto " + productId + " en bodega " + warehouseId));

        return toStockStatusResponse(productStock);
    }

    /**
     * Obtiene stock total de un producto en todas las bodegas
     */
    public BigDecimal getTotalStock(Long productId) {
        BigDecimal total = productStockRepository.getTotalStockByProduct(productId);
        return total != null ? total : BigDecimal.ZERO;
    }

    /**
     * Obtiene últimos movimientos del sistema
     */
    public List<KardexResponse> getLatestMovements() {
        return stockMovementRepository.findTop10ByOrderByCreatedAtDesc().stream()
                .map(this::toKardexResponse)
                .collect(Collectors.toList());
    }

    private KardexResponse toKardexResponse(StockMovement movement) {
        return KardexResponse.builder()
                .id(movement.getId())
                .productId(movement.getProduct().getId())
                .productName(movement.getProduct().getName())
                .productSku(movement.getProduct().getSku())
                .warehouseId(movement.getWarehouse().getId())
                .warehouseName(movement.getWarehouse().getName())
                .type(movement.getType().name())
                .typeDescription(getMovementTypeDescription(movement.getType()))
                .quantity(movement.getQuantity())
                .previousStock(movement.getPreviousStock())
                .currentStock(movement.getCurrentStock())
                .unitCost(movement.getUnitCost())
                .reason(movement.getReason())
                .referenceDocument(movement.getReferenceDocument())
                .createdBy(movement.getCreatedBy() != null ? movement.getCreatedBy().getFullName() : null)
                .createdAt(movement.getCreatedAt())
                .build();
    }

    private StockStatusResponse toStockStatusResponse(ProductStock productStock) {
        return StockStatusResponse.builder()
                .productId(productStock.getProduct().getId())
                .productName(productStock.getProduct().getName())
                .productSku(productStock.getProduct().getSku())
                .warehouseId(productStock.getWarehouse().getId())
                .warehouseName(productStock.getWarehouse().getName())
                .currentStock(productStock.getCurrentStock())
                .reservedStock(productStock.getReservedStock())
                .availableStock(productStock.getAvailableStock())
                .minimumStock(productStock.getMinimumStock())
                .maximumStock(productStock.getMaximumStock())
                .averageCost(productStock.getAverageCost())
                .totalValue(productStock.getTotalValue())
                .isBelowMinimum(productStock.isBelowMinimum())
                .isAboveMaximum(productStock.isAboveMaximum())
                .suggestedPurchaseQuantity(productStock.getSuggestedPurchaseQuantity())
                .lastMovement(productStock.getLastMovement())
                .build();
    }

    private String getMovementTypeDescription(StockMovement.MovementType type) {
        return switch (type) {
            case ENTRADA_COMPRA -> "Entrada por compra";
            case ENTRADA_DEVOLUCION -> "Entrada por devolución de cliente";
            case ENTRADA_AJUSTE -> "Ajuste positivo de inventario";
            case ENTRADA_TRANSFERENCIA -> "Transferencia desde otra bodega";
            case SALIDA_VENTA -> "Salida por venta";
            case SALIDA_DEVOLUCION -> "Devolución a proveedor";
            case SALIDA_AJUSTE -> "Ajuste negativo de inventario";
            case SALIDA_TRANSFERENCIA -> "Transferencia a otra bodega";
            case SALIDA_MERMA -> "Merma o pérdida";
        };
    }
}