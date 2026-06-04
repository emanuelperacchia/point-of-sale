package com.pos.system.service.impl;

import com.pos.system.dto.request.CartValidationRequest;
import com.pos.system.dto.request.SaleRequest;
import com.pos.system.dto.request.StockMovementRequest;
import com.pos.system.dto.response.CartValidationResponse;
import com.pos.system.dto.response.DiscountResult;
import com.pos.system.dto.response.PaymentResponse;
import com.pos.system.dto.response.SaleItemResponse;
import com.pos.system.dto.response.SaleResponse;
import com.pos.system.entity.*;
import com.pos.system.exception.BadRequestException;
import com.pos.system.exception.ResourceNotFoundException;
import com.pos.system.repository.*;
import com.pos.system.event.SaleCompletedEvent;
import com.pos.system.service.*;
import com.pos.system.service.promotion.PromotionEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SaleServiceImpl implements SaleService {

    private static final Long DEFAULT_WAREHOUSE_ID = 1L;
    private static final BigDecimal MAX_COMBINED_DISCOUNT_PERCENT = BigDecimal.valueOf(30);
    private static final long VALOR_PUNTO = 1; // 1 punto = $1 (debe coincidir con LoyaltyService)

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductStockRepository productStockRepository;
    private final StockMovementService stockMovementService;
    private final CashShiftRepository cashShiftRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Autowired(required = false)
    private PromotionEngine promotionEngine;

    @Autowired(required = false)
    private LoyaltyService loyaltyService;

    @Override
    @Transactional
    public SaleResponse processSale(SaleRequest request, Long userId) {
        // 1. Load user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        // 2. Load warehouse (default MAIN)
        Warehouse warehouse = warehouseRepository.findById(DEFAULT_WAREHOUSE_ID)
                .orElseThrow(() -> new ResourceNotFoundException("Bodega por defecto no encontrada"));

        // 3. Load client (optional)
        Client client = null;
        if (request.getClientId() != null) {
            client = clientRepository.findById(request.getClientId())
                    .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));
        }

        // 4. Build sale items with manual discounts
        List<SaleItem> items = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;
        BigDecimal totalManualDiscount = BigDecimal.ZERO;

        // Collect info for promotion evaluation
        List<PromotionEngine.CartItem> cartItems = new ArrayList<>();

        for (SaleRequest.SaleItemRequest itemReq : request.getItems()) {
            Product product = productRepository.findByIdAndActiveTrue(itemReq.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + itemReq.getProductId()));

            // Validate stock
            validateStockForSale(product.getId(), warehouse.getId(), itemReq.getQuantity());

            BigDecimal unitPrice = product.getPrice();
            BigDecimal quantity = BigDecimal.valueOf(itemReq.getQuantity());
            BigDecimal itemSubtotal = unitPrice.multiply(quantity);

            // Manual discount from cashier
            BigDecimal itemDiscount = itemReq.getDiscount() != null ? itemReq.getDiscount() : BigDecimal.ZERO;
            if (itemDiscount.compareTo(BigDecimal.ZERO) < 0) {
                throw new BadRequestException("El descuento no puede ser negativo");
            }
            if (itemDiscount.compareTo(itemSubtotal) > 0) {
                throw new BadRequestException("El descuento no puede ser mayor al subtotal del item");
            }

            // Calculate tax
            BigDecimal itemTax = BigDecimal.ZERO;
            if (product.getTaxes() != null && !product.getTaxes().isEmpty()) {
                BigDecimal taxableAmount = itemSubtotal.subtract(itemDiscount);
                for (Tax tax : product.getTaxes()) {
                    itemTax = itemTax.add(tax.calculateTax(taxableAmount));
                }
            }

            BigDecimal itemNet = itemSubtotal.subtract(itemDiscount).add(itemTax);

            Long categoryId = product.getCategory() != null ? product.getCategory().getId() : null;

            SaleItem item = SaleItem.builder()
                    .product(product)
                    .productName(product.getName())
                    .quantity(itemReq.getQuantity())
                    .unitPrice(unitPrice)
                    .discount(itemDiscount)
                    .taxAmount(itemTax)
                    .subtotal(itemNet)
                    .build();

            items.add(item);
            subtotal = subtotal.add(itemSubtotal);
            totalTax = totalTax.add(itemTax);
            totalManualDiscount = totalManualDiscount.add(itemDiscount);

            // Track for promotion evaluation
            cartItems.add(new PromotionEngine.CartItem(
                    product.getId(), product.getName(), itemReq.getQuantity(), unitPrice, categoryId));
        }

        // 5. Evaluate promotions (if engine available)
        BigDecimal totalAutoDiscount = BigDecimal.ZERO;
        BigDecimal totalCouponDiscount = BigDecimal.ZERO;
        String appliedCouponCode = null;

        if (promotionEngine != null) {
            DiscountResult promoResult = promotionEngine.evaluate(cartItems, request.getCouponCode());

            // Apply per-item auto discounts
            for (DiscountResult.ItemDiscount disc : promoResult.itemsDiscount()) {
                if (disc.saleItemIndex() >= 0 && disc.saleItemIndex() < items.size()) {
                    int idx = disc.saleItemIndex().intValue();
                    SaleItem item = items.get(idx);
                    BigDecimal existingDiscount = item.getDiscount();
                    BigDecimal combined = existingDiscount.add(disc.discountAmount());

                    // Cap combined discount at 30% of item subtotal
                    BigDecimal itemSubtotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                    BigDecimal maxDiscount = itemSubtotal.multiply(MAX_COMBINED_DISCOUNT_PERCENT)
                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                    BigDecimal finalDiscount = combined.min(maxDiscount);

                    item.setDiscount(finalDiscount);
                    totalAutoDiscount = totalAutoDiscount.add(finalDiscount.subtract(existingDiscount));
                }
            }

            // Apply coupon discount
            if (promoResult.couponDiscount() != null && promoResult.couponDiscount().compareTo(BigDecimal.ZERO) > 0) {
                totalCouponDiscount = promoResult.couponDiscount();
                appliedCouponCode = promoResult.appliedCouponCode();
            }
        }

        // 6. Calculate total with all discounts
        BigDecimal totalDiscount = totalManualDiscount.add(totalAutoDiscount).add(totalCouponDiscount);
        BigDecimal pointsDiscount = BigDecimal.ZERO;
        if (request.getPuntosCanje() != null && request.getPuntosCanje() > 0
                && client != null && loyaltyService != null) {
            pointsDiscount = BigDecimal.valueOf(request.getPuntosCanje() * VALOR_PUNTO);
            totalDiscount = totalDiscount.add(pointsDiscount);
        }
        BigDecimal total = subtotal.subtract(totalDiscount).add(totalTax);
        // Ensure total is never negative
        if (total.compareTo(BigDecimal.ZERO) < 0) total = BigDecimal.ZERO;

        // 7. Validate payments match total
        BigDecimal paymentTotal = BigDecimal.ZERO;
        List<Payment> payments = new ArrayList<>();
        for (SaleRequest.PaymentRequest payReq : request.getPayments()) {
            PaymentMethod method;
            try {
                method = PaymentMethod.valueOf(payReq.getPaymentMethod());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Método de pago inválido: " + payReq.getPaymentMethod());
            }

            Payment payment = Payment.builder()
                    .paymentMethod(method)
                    .amount(payReq.getAmount())
                    .reference(payReq.getReference())
                    .build();

            payments.add(payment);
            paymentTotal = paymentTotal.add(payReq.getAmount());
        }

        if (paymentTotal.compareTo(total) != 0) {
            throw new BadRequestException(
                    "El total de pagos (" + paymentTotal + ") no coincide con el total de la venta (" + total + ")"
            );
        }

        // 8. Vincular pagos en efectivo al turno activo del cajero
        cashShiftRepository.findByCajeroIdAndEstado(user.getId(), ShiftStatus.ABIERTO)
                .ifPresent(activeShift -> {
                    for (Payment payment : payments) {
                        if (payment.getPaymentMethod() == PaymentMethod.CASH) {
                            payment.setShiftId(activeShift.getId());
                        }
                    }
                });

        // 9. Create and save Sale
        Sale sale = Sale.builder()
                .client(client)
                .user(user)
                .warehouse(warehouse)
                .status(SaleStatus.COMPLETED)
                .subtotal(subtotal)
                .taxAmount(totalTax)
                .discount(totalDiscount)
                .total(total)
                .notes(request.getNotes())
                .build();

        for (SaleItem item : items) {
            sale.addItem(item);
        }
        for (Payment payment : payments) {
            sale.addPayment(payment);
        }

        sale = saleRepository.save(sale);

        // 10. Deduct stock for each item
        for (SaleItem item : items) {
            StockMovementRequest movementReq = new StockMovementRequest();
            movementReq.setProductId(item.getProduct().getId());
            movementReq.setWarehouseId(warehouse.getId());
            movementReq.setQuantity(BigDecimal.valueOf(item.getQuantity()));
            movementReq.setReason("Venta #" + sale.getId());
            movementReq.setReferenceDocument("SALE-" + sale.getId());

            stockMovementService.exitBySale(movementReq, user);
        }

        // 11. Register coupon usage
        if (promotionEngine != null && appliedCouponCode != null) {
            var couponValidation = promotionEngine.validateCoupon(appliedCouponCode);
            if (couponValidation.valido() && couponValidation.id() != null) {
                Long clientIdForCoupon = client != null ? client.getId() : null;
                promotionEngine.registerCouponUsage(couponValidation.id(), sale.getId(), clientIdForCoupon);
            }
        }

        // 12. Canjear puntos (del saldo existente) antes de acumular los nuevos
        if (loyaltyService != null && client != null
                && request.getPuntosCanje() != null && request.getPuntosCanje() > 0) {
            loyaltyService.canjearPuntos(client.getId(), request.getPuntosCanje(), sale.getId());
        }

        // 13. Accumulate loyalty points for this sale
        if (loyaltyService != null && client != null) {
            loyaltyService.acumularPuntos(client.getId(), sale.getId(), total);
        }

        // 14. Emitir comprobante electrónico (asíncrono, no bloquea la respuesta)
        eventPublisher.publishEvent(new SaleCompletedEvent(sale.getId()));

        return mapToResponse(sale);
    }

    @Override
    @Transactional(readOnly = true)
    public SaleResponse getById(Long id) {
        Sale sale = saleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venta no encontrada"));
        return mapToResponse(sale);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SaleResponse> getSalesByUser(Long userId, Pageable pageable) {
        return saleRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public CartValidationResponse validateCart(CartValidationRequest request) {
        List<CartValidationResponse.ItemStatus> itemsStatus = new ArrayList<>();
        boolean allValid = true;

        for (var item : request.getItems()) {
            Product product = productRepository.findById(item.getProductId())
                    .orElse(null);

            if (product == null || !product.getActive()) {
                itemsStatus.add(CartValidationResponse.ItemStatus.builder()
                        .productId(item.getProductId())
                        .productName("Producto no encontrado")
                        .requested(item.getQuantity())
                        .available(0)
                        .enoughStock(false)
                        .message("Producto no encontrado o inactivo")
                        .build());
                allValid = false;
                continue;
            }

            int available = getAvailableStock(product.getId(), request.getWarehouseId());
            boolean enough = available >= item.getQuantity();

            if (!enough) {
                allValid = false;
            }

            itemsStatus.add(CartValidationResponse.ItemStatus.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .requested(item.getQuantity())
                    .available(available)
                    .enoughStock(enough)
                    .message(enough ? "Stock suficiente" : "Stock insuficiente. Disponible: " + available)
                    .build());
        }

        return CartValidationResponse.builder()
                .valid(allValid)
                .items(itemsStatus)
                .build();
    }

    // ---- Private helpers ----

    private void validateStockForSale(Long productId, Long warehouseId, int quantity) {
        int available = getAvailableStock(productId, warehouseId);
        if (available < quantity) {
            throw new BadRequestException(
                    "Stock insuficiente para el producto ID " + productId +
                    ". Disponible: " + available + ", solicitado: " + quantity
            );
        }
    }

    private int getAvailableStock(Long productId, Long warehouseId) {
        Product product = productRepository.getReferenceById(productId);
        Warehouse warehouse = warehouseRepository.getReferenceById(warehouseId);

        return productStockRepository.findByProductAndWarehouse(product, warehouse)
                .map(ps -> ps.getAvailableStock().intValue())
                .orElse(0);
    }

    private SaleResponse mapToResponse(Sale sale) {
        return SaleResponse.builder()
                .id(sale.getId())
                .status(sale.getStatus().name())
                .client(sale.getClient() != null
                        ? SaleResponse.ClientInfo.builder()
                                .id(sale.getClient().getId())
                                .name(sale.getClient().getName())
                                .documentNumber(sale.getClient().getDocumentNumber())
                                .build()
                        : null)
                .user(SaleResponse.UserInfo.builder()
                        .id(sale.getUser().getId())
                        .username(sale.getUser().getEmail())
                        .fullName(sale.getUser().getFullName())
                        .build())
                .subtotal(sale.getSubtotal())
                .taxAmount(sale.getTaxAmount())
                .discount(sale.getDiscount())
                .total(sale.getTotal())
                .notes(sale.getNotes())
                .items(sale.getItems().stream().map(this::mapItemToResponse).collect(Collectors.toList()))
                .payments(sale.getPayments().stream().map(this::mapPaymentToResponse).collect(Collectors.toList()))
                .createdAt(sale.getCreatedAt())
                .build();
    }

    private SaleItemResponse mapItemToResponse(SaleItem item) {
        return SaleItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProductName())
                .productSku(item.getProduct().getSku())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .discount(item.getDiscount())
                .taxAmount(item.getTaxAmount())
                .subtotal(item.getSubtotal())
                .build();
    }

    private PaymentResponse mapPaymentToResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .paymentMethod(payment.getPaymentMethod().name())
                .amount(payment.getAmount())
                .reference(payment.getReference())
                .build();
    }
}
