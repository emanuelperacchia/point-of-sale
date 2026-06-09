package com.pos.system.service;

import com.pos.system.dto.request.PayablePaymentRequest;
import com.pos.system.dto.request.PayableRequest;
import com.pos.system.dto.response.PayablePaymentResponse;
import com.pos.system.dto.response.PayableResponse;
import com.pos.system.entity.PaymentMethod;
import com.pos.system.entity.Payable;
import com.pos.system.entity.PayablePayment;
import com.pos.system.entity.Supplier;
import com.pos.system.exception.BadRequestException;
import com.pos.system.exception.ResourceNotFoundException;
import com.pos.system.repository.PayablePaymentRepository;
import com.pos.system.repository.PayableRepository;
import com.pos.system.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayableService {

    private final PayableRepository payableRepository;
    private final PayablePaymentRepository paymentRepository;
    private final SupplierRepository supplierRepository;

    // ── Creación (manual o desde PurchaseOrderService) ───────────────
    @Transactional
    public PayableResponse createPayable(PayableRequest request) {
        supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado: " + request.getSupplierId()));

        Payable p = Payable.builder()
                .supplierId(request.getSupplierId())
                .purchaseOrderId(request.getPurchaseOrderId())
                .montoOriginal(request.getMontoOriginal())
                .saldoPendiente(request.getMontoOriginal())
                .fechaEmision(request.getFechaEmision())
                .fechaVencimiento(request.getFechaVencimiento())
                .estado(Payable.Estado.PENDIENTE)
                .referenciaBancaria(request.getReferenciaBancaria())
                .build();
        return mapToResponse(payableRepository.save(p));
    }

    // ── Consultas ─────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public Page<PayableResponse> findByFilters(Long supplierId, String estado,
                                                String fechaVencimiento, Pageable pageable) {
        Payable.Estado est = estado != null ? Payable.Estado.valueOf(estado) : null;
        LocalDate fechaLimite = fechaVencimiento != null ? LocalDate.parse(fechaVencimiento) : null;

        List<Payable> all = payableRepository.findByFilters(supplierId, est, fechaLimite);
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), all.size());
        List<Payable> page = start >= all.size() ? List.of() : all.subList(start, end);

        return new PageImpl<>(
                page.stream().map(this::mapToResponse).toList(),
                pageable,
                all.size()
        );
    }

    @Transactional(readOnly = true)
    public PayableResponse getById(Long id) {
        return mapToResponse(payableRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta por pagar no encontrada: " + id)));
    }

    @Transactional(readOnly = true)
    public List<PayablePaymentResponse> getPayments(Long payableId) {
        return paymentRepository.findByPayableId(payableId)
                .stream().map(this::mapPaymentToResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<PayableResponse> getUpcoming(int dias) {
        LocalDate hoy = LocalDate.now();
        LocalDate hasta = hoy.plusDays(dias);
        return payableRepository.findProximosVencer(hoy, hasta)
                .stream().map(this::mapToResponse).toList();
    }

    // ── Pagos ─────────────────────────────────────────────────────────
    @Transactional
    public PayableResponse registrarPago(Long id, PayablePaymentRequest request, Long userId) {
        Payable p = payableRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta por pagar no encontrada: " + id));

        if (p.getEstado() == Payable.Estado.PAGADA) {
            throw new BadRequestException("La cuenta por pagar ya está pagada");
        }

        PaymentMethod metodo;
        try {
            metodo = PaymentMethod.valueOf(request.getMetodoPago());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Método de pago inválido: " + request.getMetodoPago());
        }

        if (request.getMonto().compareTo(p.getSaldoPendiente()) > 0) {
            throw new BadRequestException(
                    "El pago ($" + request.getMonto() + ") supera el saldo pendiente ($" + p.getSaldoPendiente() + ")");
        }

        PayablePayment payment = PayablePayment.builder()
                .payableId(p.getId())
                .monto(request.getMonto())
                .metodoPago(metodo)
                .referenciaBancaria(request.getReferenciaBancaria())
                .fecha(LocalDateTime.now())
                .registradoPor(userId)
                .build();
        paymentRepository.save(payment);

        BigDecimal nuevoSaldo = p.getSaldoPendiente().subtract(request.getMonto());
        p.setSaldoPendiente(nuevoSaldo);

        if (nuevoSaldo.compareTo(BigDecimal.ZERO) == 0) {
            p.setEstado(Payable.Estado.PAGADA);
        } else {
            p.setEstado(Payable.Estado.PARCIAL);
        }

        payableRepository.save(p);
        return mapToResponse(p);
    }

    // ── @Scheduled diario — marcar vencidas ───────────────────────────
    @Scheduled(cron = "0 0 2 * * *") // 2 AM daily
    @Transactional
    public void actualizarEstados() {
        LocalDate hoy = LocalDate.now();
        List<Payable> vencidas = payableRepository.findVencidas(hoy);
        for (Payable p : vencidas) {
            p.setEstado(Payable.Estado.VENCIDA);
            payableRepository.save(p);
        }
        if (!vencidas.isEmpty()) {
            log.info("Actualizadas {} cuentas por pagar a estado VENCIDA", vencidas.size());
        }
    }

    // ── Totales para CashFlowService ──────────────────────────────────
    public BigDecimal totalPendienteEntreFechas(LocalDate desde, LocalDate hasta) {
        return payableRepository.totalPendienteEntreFechas(desde, hasta);
    }

    // ── Mappers ───────────────────────────────────────────────────────
    private PayableResponse mapToResponse(Payable p) {
        String supplierName = null;
        try {
            Supplier s = supplierRepository.findById(p.getSupplierId()).orElse(null);
            if (s != null) supplierName = s.getBusinessName();
        } catch (Exception e) {
            // silencioso
        }

        return PayableResponse.builder()
                .id(p.getId())
                .supplierId(p.getSupplierId())
                .supplierName(supplierName)
                .purchaseOrderId(p.getPurchaseOrderId())
                .montoOriginal(p.getMontoOriginal())
                .saldoPendiente(p.getSaldoPendiente())
                .fechaEmision(p.getFechaEmision())
                .fechaVencimiento(p.getFechaVencimiento())
                .estado(p.getEstado().name())
                .referenciaBancaria(p.getReferenciaBancaria())
                .build();
    }

    private PayablePaymentResponse mapPaymentToResponse(PayablePayment p) {
        return PayablePaymentResponse.builder()
                .id(p.getId())
                .payableId(p.getPayableId())
                .monto(p.getMonto())
                .metodoPago(p.getMetodoPago().name())
                .referenciaBancaria(p.getReferenciaBancaria())
                .fecha(p.getFecha())
                .registradoPor(p.getRegistradoPor())
                .build();
    }
}
