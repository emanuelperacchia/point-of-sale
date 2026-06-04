package com.pos.system.service;

import com.pos.system.dto.request.GoodsReceiptRequest;
import com.pos.system.dto.response.GoodsReceiptResponse;
import com.pos.system.entity.*;
import com.pos.system.exception.BadRequestException;
import com.pos.system.exception.ResourceNotFoundException;
import com.pos.system.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para la recepción de mercadería.
 * Registra entradas de inventario contra órdenes de compra, actualiza
 * stocks, costos promedios y el estado de la orden.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GoodsReceiptService {

    private final GoodsReceiptRepository goodsReceiptRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderDetailRepository purchaseOrderDetailRepository;
    private final ProductRepository productRepository;
    private final StockMovementService stockMovementService;

    /**
     * Obtiene todas las recepciones.
     */
    @Transactional(readOnly = true)
    public List<GoodsReceiptResponse> findAll() {
        log.debug("Finding all goods receipts");
        return goodsReceiptRepository.findAll().stream()
                .map(GoodsReceiptResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Busca una recepción por su ID.
     */
    @Transactional(readOnly = true)
    public GoodsReceiptResponse findById(Long id) {
        log.debug("Finding goods receipt by id: {}", id);
        GoodsReceipt receipt = goodsReceiptRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recepción no encontrada con id: " + id));
        return GoodsReceiptResponse.fromEntity(receipt);
    }

    /**
     * Busca recepciones por orden de compra.
     */
    @Transactional(readOnly = true)
    public List<GoodsReceiptResponse> findByPurchaseOrder(Long purchaseOrderId) {
        log.debug("Finding goods receipts for purchase order: {}", purchaseOrderId);
        PurchaseOrder order = purchaseOrderRepository.findById(purchaseOrderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Orden de compra no encontrada: " + purchaseOrderId));
        return goodsReceiptRepository.findByPurchaseOrder(order).stream()
                .map(GoodsReceiptResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Registra una recepción de mercadería contra una orden de compra.
     * Actualiza automáticamente el stock, costos promedios y estado de la orden.
     */
    @Transactional
    public GoodsReceiptResponse create(GoodsReceiptRequest request, User currentUser) {
        log.info("Creating goods receipt for purchase order: {}", request.getPurchaseOrderId());

        PurchaseOrder order = purchaseOrderRepository.findById(request.getPurchaseOrderId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Orden de compra no encontrada: " + request.getPurchaseOrderId()));

        // Validar que la orden sea apta para recepción
        if (order.getStatus() == PurchaseOrder.OrderStatus.BORRADOR) {
            throw new BadRequestException(
                    "No se puede recibir una orden en estado BORRADOR. Debe enviarse o confirmarse primero.");
        }
        if (order.getStatus() == PurchaseOrder.OrderStatus.CANCELADA) {
            throw new BadRequestException("No se puede recibir una orden cancelada.");
        }
        if (order.getStatus() == PurchaseOrder.OrderStatus.RECIBIDA_COMPLETA) {
            throw new BadRequestException("La orden ya fue recibida completamente.");
        }

        String receiptNumber = generateReceiptNumber();

        GoodsReceipt receipt = GoodsReceipt.builder()
                .receiptNumber(receiptNumber)
                .purchaseOrder(order)
                .receiptDate(request.getReceiptDate())
                .createdBy(currentUser)
                .notes(request.getNotes())
                .build();

        boolean allFullyReceived = true;

        for (GoodsReceiptRequest.GoodsReceiptDetailRequest detailReq : request.getDetails()) {
            // Validar detalle de orden de compra
            PurchaseOrderDetail pod = purchaseOrderDetailRepository.findById(
                            detailReq.getPurchaseOrderDetailId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Detalle de orden no encontrado: " + detailReq.getPurchaseOrderDetailId()));

            // Validar que el producto coincida
            if (!pod.getProduct().getId().equals(detailReq.getProductId())) {
                throw new BadRequestException(
                        "El producto del detalle no coincide con la orden de compra");
            }

            // Validar cantidades
            BigDecimal pendingQuantity = pod.getPendingQuantity();
            BigDecimal totalReceiving = detailReq.getReceivedQuantity()
                    .add(detailReq.getDamagedQuantity())
                    .add(detailReq.getMissingQuantity());

            if (totalReceiving.compareTo(detailReq.getExpectedQuantity()) != 0) {
                throw new BadRequestException(
                        "La suma de recibido + dañado + faltante debe igualar la cantidad esperada. "
                                + "Esperado: " + detailReq.getExpectedQuantity()
                                + ", suma: " + totalReceiving);
            }

            if (totalReceiving.compareTo(pendingQuantity) > 0) {
                throw new BadRequestException(
                        "La cantidad a recibir (" + totalReceiving
                                + ") excede la pendiente (" + pendingQuantity + ")");
            }

            Product product = productRepository.findById(detailReq.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Producto no encontrado: " + detailReq.getProductId()));

            // Crear detalle de recepción
            GoodsReceiptDetail detail = GoodsReceiptDetail.builder()
                    .purchaseOrderDetail(pod)
                    .product(product)
                    .expectedQuantity(detailReq.getExpectedQuantity())
                    .receivedQuantity(detailReq.getReceivedQuantity())
                    .damagedQuantity(detailReq.getDamagedQuantity() != null
                            ? detailReq.getDamagedQuantity() : BigDecimal.ZERO)
                    .missingQuantity(detailReq.getMissingQuantity() != null
                            ? detailReq.getMissingQuantity() : BigDecimal.ZERO)
                    .unitCost(detailReq.getUnitCost())
                    .batchNumber(detailReq.getBatchNumber())
                    .expirationDate(detailReq.getExpirationDate())
                    .notes(detailReq.getNotes())
                    .build();

            receipt.addDetail(detail);

            // Actualizar cantidad recibida en el detalle de la orden
            pod.setQuantityReceived(pod.getQuantityReceived().add(detailReq.getReceivedQuantity()));

            // Actualizar stock si hay cantidad aceptada (recibida - dañada)
            BigDecimal acceptedQuantity = detailReq.getReceivedQuantity()
                    .subtract(detailReq.getDamagedQuantity() != null
                            ? detailReq.getDamagedQuantity() : BigDecimal.ZERO);

            if (acceptedQuantity.compareTo(BigDecimal.ZERO) > 0) {
                // Crear movimiento de entrada por compra
                stockMovementService.entryByPurchase(
                        buildStockMovementRequest(product.getId(), order.getWarehouse().getId(),
                                acceptedQuantity, detailReq.getUnitCost(), receiptNumber),
                        currentUser
                );
            }

            // Verificar si este detalle quedó completamente recibido
            if (!pod.isFullyReceived()) {
                allFullyReceived = false;
            }
        }

        // Actualizar estado de la orden
        if (allFullyReceived) {
            order.setStatus(PurchaseOrder.OrderStatus.RECIBIDA_COMPLETA);
            order.setDeliveryDate(request.getReceiptDate());
        } else {
            order.setStatus(PurchaseOrder.OrderStatus.RECIBIDA_PARCIAL);
        }

        purchaseOrderRepository.save(order);
        receipt = goodsReceiptRepository.save(receipt);

        log.info("Goods receipt created: {} for PO: {}",
                receiptNumber, order.getOrderNumber());
        return GoodsReceiptResponse.fromEntity(receipt);
    }

    /**
     * Construye un StockMovementRequest para la entrada de stock.
     */
    private com.pos.system.dto.request.StockMovementRequest buildStockMovementRequest(
            Long productId, Long warehouseId, BigDecimal quantity,
            BigDecimal unitCost, String receiptNumber) {
        return com.pos.system.dto.request.StockMovementRequest.builder()
                .productId(productId)
                .warehouseId(warehouseId)
                .quantity(quantity)
                .unitCost(unitCost)
                .reason("Recepción de mercadería")
                .referenceDocument(receiptNumber)
                .build();
    }

    /**
     * Genera un número de recepción correlativo con formato GR-XXXXX.
     */
    private String generateReceiptNumber() {
        Integer nextNumber = goodsReceiptRepository.getNextReceiptNumber("GR-%");
        if (nextNumber == null) {
            nextNumber = 0;
        }
        return String.format("GR-%05d", nextNumber + 1);
    }
}
