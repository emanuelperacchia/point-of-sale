package com.pos.system.service;

import com.pos.system.dto.request.SupplierReturnRequest;
import com.pos.system.dto.response.SupplierReturnResponse;
import com.pos.system.entity.*;
import com.pos.system.exception.BadRequestException;
import com.pos.system.exception.ResourceNotFoundException;
import com.pos.system.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para la gestión de devoluciones a proveedores.
 * Registra salidas de stock por devolución y genera notas de crédito automáticas.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SupplierReturnService {

    private final SupplierReturnRepository supplierReturnRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final StockMovementService stockMovementService;

    /**
     * Obtiene todas las devoluciones.
     */
    @Transactional(readOnly = true)
    public List<SupplierReturnResponse> findAll() {
        log.debug("Finding all supplier returns");
        return supplierReturnRepository.findAll().stream()
                .map(SupplierReturnResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Busca una devolución por su ID.
     */
    @Transactional(readOnly = true)
    public SupplierReturnResponse findById(Long id) {
        log.debug("Finding supplier return by id: {}", id);
        SupplierReturn supplierReturn = supplierReturnRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Devolución no encontrada con id: " + id));
        return SupplierReturnResponse.fromEntity(supplierReturn);
    }

    /**
     * Busca devoluciones por proveedor.
     */
    @Transactional(readOnly = true)
    public List<SupplierReturnResponse> findBySupplier(Long supplierId) {
        log.debug("Finding returns for supplier: {}", supplierId);
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Proveedor no encontrado: " + supplierId));
        return supplierReturnRepository.findBySupplier(supplier).stream()
                .map(SupplierReturnResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Busca devoluciones por estado.
     */
    @Transactional(readOnly = true)
    public List<SupplierReturnResponse> findByStatus(SupplierReturn.ReturnStatus status) {
        log.debug("Finding returns with status: {}", status);
        return supplierReturnRepository.findByStatus(status).stream()
                .map(SupplierReturnResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Registra una nueva devolución a proveedor.
     * Descarga el stock del inventario y genera una nota de crédito.
     */
    @Transactional
    public SupplierReturnResponse create(SupplierReturnRequest request, User currentUser) {
        log.info("Creating supplier return for supplier: {}", request.getSupplierId());

        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Proveedor no encontrado: " + request.getSupplierId()));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Producto no encontrado: " + request.getProductId()));

        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Bodega no encontrada: " + request.getWarehouseId()));

        // Validar stock disponible
        stockMovementService.validateStockAvailable(
                request.getProductId(), request.getWarehouseId(), request.getQuantity());

        // Calcular subtotal
        BigDecimal subtotal = request.getQuantity().multiply(request.getUnitCost());

        // Generar número de devolución
        String returnNumber = generateReturnNumber();

        // Construir entidad
        SupplierReturn.SupplierReturnBuilder builder = SupplierReturn.builder()
                .returnNumber(returnNumber)
                .supplier(supplier)
                .product(product)
                .quantity(request.getQuantity())
                .unitCost(request.getUnitCost())
                .subtotal(subtotal)
                .returnDate(request.getReturnDate())
                .reason(request.getReason())
                .status(SupplierReturn.ReturnStatus.PENDING)
                .warehouse(warehouse)
                .createdBy(currentUser)
                .notes(request.getNotes());

        // Asociar orden de compra si se especificó
        if (request.getPurchaseOrderId() != null) {
            PurchaseOrder po = purchaseOrderRepository.findById(request.getPurchaseOrderId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Orden de compra no encontrada: " + request.getPurchaseOrderId()));
            builder.purchaseOrder(po);
        }

        SupplierReturn supplierReturn = supplierReturnRepository.save(builder.build());

        // Descargar del stock (salida por devolución a proveedor)
        stockMovementService.exitBySupplierReturn(
                buildStockMovementRequest(product.getId(), warehouse.getId(),
                        request.getQuantity(), request.getUnitCost(), returnNumber),
                currentUser
        );

        // Generar nota de crédito automática
        String creditNoteNumber = generateCreditNoteNumber();
        supplierReturn.setCreditNoteNumber(creditNoteNumber);
        supplierReturn.setCreditNoteAmount(subtotal);
        supplierReturn.setStatus(SupplierReturn.ReturnStatus.COMPLETED);

        // Actualizar deuda del proveedor (disminuye porque devolvemos mercadería)
        BigDecimal newDebt = supplier.getCurrentDebt().subtract(subtotal);
        if (newDebt.compareTo(BigDecimal.ZERO) < 0) {
            newDebt = BigDecimal.ZERO;
        }
        supplier.setCurrentDebt(newDebt);
        supplierRepository.save(supplier);

        supplierReturn = supplierReturnRepository.save(supplierReturn);

        log.info("Supplier return created: {} - Credit note: {} - Amount: {}",
                returnNumber, creditNoteNumber, subtotal);
        return SupplierReturnResponse.fromEntity(supplierReturn);
    }

    /**
     * Genera un número de devolución correlativo con formato SR-XXXXX.
     */
    private String generateReturnNumber() {
        Integer nextNumber = supplierReturnRepository.getNextReturnNumber("SR-%");
        if (nextNumber == null) {
            nextNumber = 0;
        }
        return String.format("SR-%05d", nextNumber + 1);
    }

    /**
     * Genera un número de nota de crédito correlativo con formato CN-XXXXX.
     */
    private String generateCreditNoteNumber() {
        Integer nextNumber = supplierReturnRepository.getNextCreditNoteNumber("CN-%");
        if (nextNumber == null) {
            nextNumber = 0;
        }
        return String.format("CN-%05d", nextNumber + 1);
    }

    /**
     * Construye un StockMovementRequest para la salida por devolución.
     */
    private com.pos.system.dto.request.StockMovementRequest buildStockMovementRequest(
            Long productId, Long warehouseId, BigDecimal quantity,
            BigDecimal unitCost, String returnNumber) {
        return com.pos.system.dto.request.StockMovementRequest.builder()
                .productId(productId)
                .warehouseId(warehouseId)
                .quantity(quantity)
                .unitCost(unitCost)
                .reason("Devolución a proveedor")
                .referenceDocument(returnNumber)
                .build();
    }
}
