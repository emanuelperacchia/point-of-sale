package com.pos.system.service;

import com.pos.system.dto.request.PurchaseOrderRequest;
import com.pos.system.dto.response.PurchaseOrderResponse;
import com.pos.system.entity.*;
import com.pos.system.exception.BadRequestException;
import com.pos.system.exception.ResourceNotFoundException;
import com.pos.system.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para la gestión de órdenes de compra.
 * Maneja el ciclo de vida completo: creación, modificación, aprobación, cancelación.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderDetailRepository detailRepository;
    private final SupplierRepository supplierRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;

    /**
     * Obtiene todas las órdenes de compra.
     */
    @Transactional(readOnly = true)
    public List<PurchaseOrderResponse> findAll() {
        log.debug("Finding all purchase orders");
        return purchaseOrderRepository.findAll().stream()
                .map(PurchaseOrderResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Busca una orden por su ID.
     */
    @Transactional(readOnly = true)
    public PurchaseOrderResponse findById(Long id) {
        log.debug("Finding purchase order by id: {}", id);
        return PurchaseOrderResponse.fromEntity(findOrderById(id));
    }

    /**
     * Busca una orden por su número.
     */
    @Transactional(readOnly = true)
    public PurchaseOrderResponse findByOrderNumber(String orderNumber) {
        log.debug("Finding purchase order by number: {}", orderNumber);
        PurchaseOrder order = purchaseOrderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Orden de compra no encontrada: " + orderNumber));
        return PurchaseOrderResponse.fromEntity(order);
    }

    /**
     * Obtiene órdenes pendientes de recepción.
     */
    @Transactional(readOnly = true)
    public List<PurchaseOrderResponse> findPendingOrders() {
        log.debug("Finding pending purchase orders");
        return purchaseOrderRepository.findPendingOrders().stream()
                .map(PurchaseOrderResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene órdenes vencidas (fecha estimada pasada sin recibir).
     */
    @Transactional(readOnly = true)
    public List<PurchaseOrderResponse> findOverdueOrders() {
        log.debug("Finding overdue purchase orders");
        return purchaseOrderRepository.findOverdueOrders(LocalDate.now()).stream()
                .map(PurchaseOrderResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene órdenes de compra por proveedor.
     */
    @Transactional(readOnly = true)
    public List<PurchaseOrderResponse> findBySupplier(Long supplierId) {
        log.debug("Finding purchase orders for supplier: {}", supplierId);
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Proveedor no encontrado: " + supplierId));
        return purchaseOrderRepository.findBySupplier(supplier, Pageable.unpaged()).stream()
                .map(PurchaseOrderResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene el total de compras realizadas a un proveedor.
     */
    @Transactional(readOnly = true)
    public BigDecimal getTotalPurchasesBySupplier(Long supplierId) {
        log.debug("Getting total purchases for supplier: {}", supplierId);
        return purchaseOrderRepository.getTotalPurchasesBySupplier(supplierId);
    }

    /**
     * Crea una nueva orden de compra en estado BORRADOR.
     */
    @Transactional
    public PurchaseOrderResponse create(PurchaseOrderRequest request, User currentUser) {
        log.info("Creating new purchase order");

        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Proveedor no encontrado: " + request.getSupplierId()));
        if (!supplier.getActive()) {
            throw new BadRequestException("El proveedor está inactivo");
        }

        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Bodega no encontrada: " + request.getWarehouseId()));

        String orderNumber = generateOrderNumber();

        PurchaseOrder order = PurchaseOrder.builder()
                .orderNumber(orderNumber)
                .supplier(supplier)
                .warehouse(warehouse)
                .orderDate(request.getOrderDate())
                .expectedDeliveryDate(request.getExpectedDeliveryDate())
                .status(PurchaseOrder.OrderStatus.BORRADOR)
                .paymentTerm(request.getPaymentTerm())
                .discountAmount(request.getDiscountAmount() != null
                        ? request.getDiscountAmount() : BigDecimal.ZERO)
                .taxAmount(request.getTaxAmount() != null
                        ? request.getTaxAmount() : BigDecimal.ZERO)
                .notes(request.getNotes())
                .contactPerson(request.getContactPerson())
                .createdBy(currentUser)
                .build();

        for (PurchaseOrderRequest.PurchaseOrderDetailRequest detailReq : request.getDetails()) {
            Product product = productRepository.findById(detailReq.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Producto no encontrado: " + detailReq.getProductId()));

            PurchaseOrderDetail detail = PurchaseOrderDetail.builder()
                    .product(product)
                    .quantity(detailReq.getQuantity())
                    .unitPrice(detailReq.getUnitPrice())
                    .discountPercentage(detailReq.getDiscountPercentage() != null
                            ? detailReq.getDiscountPercentage() : BigDecimal.ZERO)
                    .taxAmount(detailReq.getTaxAmount() != null
                            ? detailReq.getTaxAmount() : BigDecimal.ZERO)
                    .notes(detailReq.getNotes())
                    .build();

            order.addDetail(detail);
        }

        order = purchaseOrderRepository.save(order);
        log.info("Purchase order created: {}", order.getOrderNumber());
        return PurchaseOrderResponse.fromEntity(order);
    }

    /**
     * Actualiza una orden de compra (solo si está en BORRADOR).
     */
    @Transactional
    public PurchaseOrderResponse update(Long id, PurchaseOrderRequest request) {
        log.info("Updating purchase order: {}", id);
        PurchaseOrder order = findOrderById(id);

        if (!order.canBeModified()) {
            throw new BadRequestException("Solo se pueden modificar órdenes en estado BORRADOR");
        }

        order.setOrderDate(request.getOrderDate());
        order.setExpectedDeliveryDate(request.getExpectedDeliveryDate());
        order.setPaymentTerm(request.getPaymentTerm());
        order.setDiscountAmount(request.getDiscountAmount() != null
                ? request.getDiscountAmount() : BigDecimal.ZERO);
        order.setTaxAmount(request.getTaxAmount() != null
                ? request.getTaxAmount() : BigDecimal.ZERO);
        order.setNotes(request.getNotes());
        order.setContactPerson(request.getContactPerson());

        // Reemplazar detalles
        order.getDetails().clear();
        for (PurchaseOrderRequest.PurchaseOrderDetailRequest detailReq : request.getDetails()) {
            Product product = productRepository.findById(detailReq.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Producto no encontrado: " + detailReq.getProductId()));

            PurchaseOrderDetail detail = PurchaseOrderDetail.builder()
                    .product(product)
                    .quantity(detailReq.getQuantity())
                    .unitPrice(detailReq.getUnitPrice())
                    .discountPercentage(detailReq.getDiscountPercentage() != null
                            ? detailReq.getDiscountPercentage() : BigDecimal.ZERO)
                    .taxAmount(detailReq.getTaxAmount() != null
                            ? detailReq.getTaxAmount() : BigDecimal.ZERO)
                    .notes(detailReq.getNotes())
                    .build();

            order.addDetail(detail);
        }

        order = purchaseOrderRepository.save(order);
        log.info("Purchase order updated: {}", order.getOrderNumber());
        return PurchaseOrderResponse.fromEntity(order);
    }

    /**
     * Aprueba una orden de compra (cambia a CONFIRMADA y registra quién aprobó).
     */
    @Transactional
    public PurchaseOrderResponse approve(Long id, User currentUser) {
        log.info("Approving purchase order: {}", id);
        PurchaseOrder order = findOrderById(id);

        if (order.getStatus() != PurchaseOrder.OrderStatus.BORRADOR
                && order.getStatus() != PurchaseOrder.OrderStatus.ENVIADA) {
            throw new BadRequestException(
                    "Solo se pueden aprobar órdenes en estado BORRADOR o ENVIADA");
        }

        order.setStatus(PurchaseOrder.OrderStatus.CONFIRMADA);
        order.setApprovedBy(currentUser);
        order.setApprovedAt(LocalDateTime.now());

        order = purchaseOrderRepository.save(order);
        log.info("Purchase order approved: {}", order.getOrderNumber());
        return PurchaseOrderResponse.fromEntity(order);
    }

    /**
     * Cancela una orden de compra.
     */
    @Transactional
    public PurchaseOrderResponse cancel(Long id, String reason) {
        log.info("Cancelling purchase order: {}", id);
        PurchaseOrder order = findOrderById(id);

        if (!order.canBeCancelled()) {
            throw new BadRequestException(
                    "No se puede cancelar una orden " + order.getStatus().getDescription());
        }

        order.setStatus(PurchaseOrder.OrderStatus.CANCELADA);
        if (reason != null) {
            order.setNotes(order.getNotes() != null
                    ? order.getNotes() + " | Cancelación: " + reason
                    : "Cancelación: " + reason);
        }

        order = purchaseOrderRepository.save(order);
        log.info("Purchase order cancelled: {}", order.getOrderNumber());
        return PurchaseOrderResponse.fromEntity(order);
    }

    /**
     * Envía una orden (cambia de BORRADOR a ENVIADA).
     */
    @Transactional
    public PurchaseOrderResponse send(Long id) {
        log.info("Sending purchase order: {}", id);
        PurchaseOrder order = findOrderById(id);

        if (order.getStatus() != PurchaseOrder.OrderStatus.BORRADOR) {
            throw new BadRequestException(
                    "Solo se pueden enviar órdenes en estado BORRADOR");
        }

        order.setStatus(PurchaseOrder.OrderStatus.ENVIADA);
        order = purchaseOrderRepository.save(order);
        log.info("Purchase order sent: {}", order.getOrderNumber());
        return PurchaseOrderResponse.fromEntity(order);
    }

    /**
     * Elimina físicamente una orden de compra en estado BORRADOR.
     */
    @Transactional
    public void delete(Long id) {
        log.info("Deleting purchase order: {}", id);
        PurchaseOrder order = findOrderById(id);

        if (order.getStatus() != PurchaseOrder.OrderStatus.BORRADOR) {
            throw new BadRequestException(
                    "Solo se pueden eliminar órdenes en estado BORRADOR");
        }

        purchaseOrderRepository.delete(order);
        log.info("Purchase order deleted: {}", order.getOrderNumber());
    }

    /**
     * Obtiene una orden por ID lanzando excepción si no existe.
     */
    private PurchaseOrder findOrderById(Long id) {
        return purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Orden de compra no encontrada con id: " + id));
    }

    /**
     * Genera un número de orden correlativo con formato PO-XXXXX.
     */
    private String generateOrderNumber() {
        Integer nextNumber = purchaseOrderRepository.getNextOrderNumber("PO-%");
        if (nextNumber == null) {
            nextNumber = 0;
        }
        return String.format("PO-%05d", nextNumber + 1);
    }
}
