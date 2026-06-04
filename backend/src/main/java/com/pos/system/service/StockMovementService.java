package com.pos.system.service;

import com.pos.system.dto.request.StockAdjustmentRequest;
import com.pos.system.dto.request.StockMovementRequest;
import com.pos.system.dto.response.KardexResponse;
import com.pos.system.entity.*;
import com.pos.system.exception.BadRequestException;
import com.pos.system.exception.ResourceNotFoundException;
import com.pos.system.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class StockMovementService {

    private final ProductStockRepository productStockRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;

    /**
     * Entrada de stock por compra
     */
    @Transactional
    public KardexResponse entryByPurchase(StockMovementRequest request, User user) {
        return createMovement(
                request.getProductId(),
                request.getWarehouseId(),
                StockMovement.MovementType.ENTRADA_COMPRA,
                request.getQuantity(),
                request.getUnitCost(),
                request.getReason(),
                request.getReferenceDocument(),
                user
        );
    }

    /**
     * Entrada de stock por devolución de cliente
     */
    @Transactional
    public KardexResponse entryByReturn(StockMovementRequest request, User user) {
        return createMovement(
                request.getProductId(),
                request.getWarehouseId(),
                StockMovement.MovementType.ENTRADA_DEVOLUCION,
                request.getQuantity(),
                request.getUnitCost(),
                request.getReason(),
                request.getReferenceDocument(),
                user
        );
    }

    /**
     * Salida de stock por venta
     */
    @Transactional
    public KardexResponse exitBySale(StockMovementRequest request, User user) {
        validateStockAvailable(request.getProductId(), request.getWarehouseId(), request.getQuantity());

        return createMovement(
                request.getProductId(),
                request.getWarehouseId(),
                StockMovement.MovementType.SALIDA_VENTA,
                request.getQuantity().negate(),
                request.getUnitCost(),
                request.getReason(),
                request.getReferenceDocument(),
                user
        );
    }

    /**
     * Salida de stock por devolución a proveedor
     */
    @Transactional
    public KardexResponse exitBySupplierReturn(StockMovementRequest request, User user) {
        validateStockAvailable(request.getProductId(), request.getWarehouseId(), request.getQuantity());

        return createMovement(
                request.getProductId(),
                request.getWarehouseId(),
                StockMovement.MovementType.SALIDA_DEVOLUCION,
                request.getQuantity().negate(),
                request.getUnitCost(),
                request.getReason(),
                request.getReferenceDocument(),
                user
        );
    }

    /**
     * Ajuste de inventario (positivo o negativo)
     */
    @Transactional
    public KardexResponse adjustStock(StockAdjustmentRequest request, User user) {
        boolean isPositive = "POSITIVO".equalsIgnoreCase(request.getAdjustmentType());

        if (!isPositive) {
            validateStockAvailable(request.getProductId(), request.getWarehouseId(), request.getQuantity());
        }

        StockMovement.MovementType type = isPositive
                ? StockMovement.MovementType.ENTRADA_AJUSTE
                : StockMovement.MovementType.SALIDA_AJUSTE;

        BigDecimal quantity = isPositive ? request.getQuantity() : request.getQuantity().negate();

        return createMovement(
                request.getProductId(),
                request.getWarehouseId(),
                type,
                quantity,
                request.getUnitCost(),
                request.getReason(),
                request.getReferenceDocument(),
                user
        );
    }

    /**
     * Transferencia entre bodegas
     */
    @Transactional
    public void transferStock(Long productId, Long fromWarehouseId, Long toWarehouseId,
                              BigDecimal quantity, String reason, User user) {
        validateStockAvailable(productId, fromWarehouseId, quantity);

        // Salida de bodega origen
        createMovement(
                productId, fromWarehouseId,
                StockMovement.MovementType.SALIDA_TRANSFERENCIA,
                quantity.negate(), null, reason, null, user
        );

        // Entrada a bodega destino
        createMovement(
                productId, toWarehouseId,
                StockMovement.MovementType.ENTRADA_TRANSFERENCIA,
                quantity, null, reason, null, user
        );
    }

    /**
     * Registra merma/pérdida
     */
    @Transactional
    public KardexResponse registerWaste(StockMovementRequest request, User user) {
        validateStockAvailable(request.getProductId(), request.getWarehouseId(), request.getQuantity());

        return createMovement(
                request.getProductId(),
                request.getWarehouseId(),
                StockMovement.MovementType.SALIDA_MERMA,
                request.getQuantity().negate(),
                request.getUnitCost(),
                request.getReason(),
                request.getReferenceDocument(),
                user
        );
    }

    /**
     * Método central que crea el movimiento y actualiza el stock
     */
    @Transactional
    public KardexResponse createMovement(Long productId, Long warehouseId,
                                          StockMovement.MovementType type,
                                          BigDecimal quantity, BigDecimal unitCost,
                                          String reason, String referenceDocument,
                                          User user) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + productId));
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Bodega no encontrada: " + warehouseId));

        // Obtener o crear registro de stock
        ProductStock productStock = productStockRepository
                .findByProductAndWarehouse(product, warehouse)
                .orElseGet(() -> createInitialStock(product, warehouse));

        BigDecimal previousStock = productStock.getCurrentStock();
        BigDecimal newStock = previousStock.add(quantity);

        // Validar stock negativo
        if (newStock.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException(
                    "Stock insuficiente. Stock actual: " + previousStock +
                    ", cantidad solicitada: " + quantity.abs()
            );
        }

        // Actualizar costo promedio
        BigDecimal newAvgCost = calculateAverageCost(productStock, quantity, unitCost, type);
        BigDecimal newTotalValue = newStock.multiply(newAvgCost).setScale(2, RoundingMode.HALF_UP);

        // Actualizar stock
        productStock.setCurrentStock(newStock);
        productStock.setAverageCost(newAvgCost);
        productStock.setTotalValue(newTotalValue);

        productStockRepository.save(productStock);

        // Crear movimiento
        StockMovement movement = StockMovement.builder()
                .product(product)
                .warehouse(warehouse)
                .type(type)
                .quantity(quantity)
                .previousStock(previousStock)
                .currentStock(newStock)
                .unitCost(unitCost != null ? unitCost : productStock.getAverageCost())
                .reason(reason)
                .referenceDocument(referenceDocument)
                .createdBy(user)
                .build();

        movement = stockMovementRepository.save(movement);

        return toKardexResponse(movement);
    }

    /**
     * Valida que haya stock suficiente
     */
    public void validateStockAvailable(Long productId, Long warehouseId, BigDecimal quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + productId));
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Bodega no encontrada: " + warehouseId));

        ProductStock productStock = productStockRepository
                .findByProductAndWarehouse(product, warehouse)
                .orElseThrow(() -> new BadRequestException(
                        "El producto " + product.getName() + " no tiene stock en " + warehouse.getName()));

        BigDecimal available = productStock.getAvailableStock();

        if (available.compareTo(quantity) < 0) {
            throw new BadRequestException(
                    "Stock insuficiente de " + product.getName() +
                    ". Disponible: " + available + ", solicitado: " + quantity
            );
        }
    }

    private ProductStock createInitialStock(Product product, Warehouse warehouse) {
        ProductStock productStock = ProductStock.builder()
                .product(product)
                .warehouse(warehouse)
                .currentStock(BigDecimal.ZERO)
                .reservedStock(BigDecimal.ZERO)
                .minimumStock(BigDecimal.ZERO)
                .maximumStock(BigDecimal.ZERO)
                .averageCost(BigDecimal.ZERO)
                .totalValue(BigDecimal.ZERO)
                .build();

        return productStockRepository.save(productStock);
    }

    private BigDecimal calculateAverageCost(ProductStock productStock,
                                            BigDecimal quantity,
                                            BigDecimal unitCost,
                                            StockMovement.MovementType type) {
        if (isExit(type) || unitCost == null || unitCost.compareTo(BigDecimal.ZERO) == 0) {
            return productStock.getAverageCost();
        }

        BigDecimal currentQty = productStock.getCurrentStock();
        BigDecimal currentCost = productStock.getAverageCost();

        if (currentQty.compareTo(BigDecimal.ZERO) == 0) {
            return unitCost.setScale(4, RoundingMode.HALF_UP);
        }

        BigDecimal totalCost = (currentQty.multiply(currentCost))
                .add(quantity.multiply(unitCost));
        BigDecimal totalQty = currentQty.add(quantity);

        return totalCost.divide(totalQty, 4, RoundingMode.HALF_UP);
    }

    private boolean isEntry(StockMovement.MovementType type) {
        return type.name().startsWith("ENTRADA");
    }

    private boolean isExit(StockMovement.MovementType type) {
        return type.name().startsWith("SALIDA");
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
}