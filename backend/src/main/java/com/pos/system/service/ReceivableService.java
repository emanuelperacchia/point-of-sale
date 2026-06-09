package com.pos.system.service;

import com.pos.system.dto.request.ReceivablePaymentRequest;
import com.pos.system.dto.response.ReceivablePaymentResponse;
import com.pos.system.dto.response.ReceivableResponse;
import com.pos.system.entity.Client;
import com.pos.system.entity.PaymentMethod;
import com.pos.system.entity.Receivable;
import com.pos.system.entity.ReceivablePayment;
import com.pos.system.exception.BadRequestException;
import com.pos.system.exception.ResourceNotFoundException;
import com.pos.system.repository.ClientRepository;
import com.pos.system.repository.ReceivablePaymentRepository;
import com.pos.system.repository.ReceivableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReceivableService {

    private static final String MORA_TASA_KEY = "mora.tasa";
    private static final BigDecimal DIAS_ANO = BigDecimal.valueOf(365);

    private final ReceivableRepository receivableRepository;
    private final ReceivablePaymentRepository paymentRepository;
    private final ClientRepository clientRepository;
    private final ConfigurationService configService;

    // ── Creación (llamada desde SaleService) ──────────────────────────
    @Transactional
    public Receivable createReceivable(Long clientId, Long saleId, BigDecimal monto,
                                       LocalDate fechaVencimiento) {
        Receivable r = Receivable.builder()
                .clientId(clientId)
                .saleId(saleId)
                .montoOriginal(monto)
                .saldoPendiente(monto)
                .fechaEmision(LocalDate.now())
                .fechaVencimiento(fechaVencimiento)
                .estado(Receivable.Estado.PENDIENTE)
                .interesesAcumulados(BigDecimal.ZERO)
                .build();
        return receivableRepository.save(r);
    }

    // ── Validación para SaleService ───────────────────────────────────
    public void validarClienteParaCredito(Long clientId) {
        boolean tieneDeudaVencida = receivableRepository
                .existsByClientIdAndEstadoIn(clientId, List.of(Receivable.Estado.VENCIDA));
        if (tieneDeudaVencida) {
            throw new BadRequestException(
                    "El cliente tiene cuentas por cobrar vencidas. No puede generar nuevas ventas a crédito.");
        }
    }

    // ── Consultas ─────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public Page<ReceivableResponse> findByFilters(Long clienteId, String estado,
                                                   String fechaVencimiento, Pageable pageable) {
        Receivable.Estado est = estado != null ? Receivable.Estado.valueOf(estado) : null;
        LocalDate fechaLimite = fechaVencimiento != null ? LocalDate.parse(fechaVencimiento) : null;

        List<Receivable> all = receivableRepository.findByFilters(clienteId, est, fechaLimite);

        // Manual pagination over the filtered list
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), all.size());
        List<Receivable> page = start >= all.size() ? List.of() : all.subList(start, end);

        return new PageImpl<>(
                page.stream().map(this::mapToResponse).toList(),
                pageable,
                all.size()
        );
    }

    @Transactional(readOnly = true)
    public ReceivableResponse getById(Long id) {
        Receivable r = receivableRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta por cobrar no encontrada: " + id));
        return mapToResponse(r);
    }

    @Transactional(readOnly = true)
    public List<ReceivablePaymentResponse> getPayments(Long receivableId) {
        return paymentRepository.findByReceivableId(receivableId)
                .stream().map(this::mapPaymentToResponse).toList();
    }

    // ── Registro de pagos ─────────────────────────────────────────────
    @Transactional
    public ReceivableResponse registrarPago(Long id, ReceivablePaymentRequest request, Long userId) {
        Receivable r = receivableRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta por cobrar no encontrada: " + id));

        if (r.getEstado() == Receivable.Estado.COBRADA || r.getEstado() == Receivable.Estado.INCOBRABLE) {
            throw new BadRequestException("La cuenta por cobrar ya está " + r.getEstado().name().toLowerCase());
        }

        PaymentMethod metodo;
        try {
            metodo = PaymentMethod.valueOf(request.getMetodoPago());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Método de pago inválido: " + request.getMetodoPago());
        }

        if (request.getMonto().compareTo(r.getSaldoPendiente()) > 0) {
            throw new BadRequestException(
                    "El pago ($" + request.getMonto() + ") supera el saldo pendiente ($" + r.getSaldoPendiente() + ")");
        }

        // Registrar pago
        ReceivablePayment payment = ReceivablePayment.builder()
                .receivableId(r.getId())
                .monto(request.getMonto())
                .metodoPago(metodo)
                .fecha(LocalDateTime.now())
                .registradoPor(userId)
                .build();
        paymentRepository.save(payment);

        // Actualizar saldo pendiente
        BigDecimal nuevoSaldo = r.getSaldoPendiente().subtract(request.getMonto());
        r.setSaldoPendiente(nuevoSaldo);

        // Actualizar estado
        if (nuevoSaldo.compareTo(BigDecimal.ZERO) == 0) {
            r.setEstado(Receivable.Estado.COBRADA);
        } else {
            r.setEstado(Receivable.Estado.PARCIAL);
        }

        receivableRepository.save(r);
        return mapToResponse(r);
    }

    // ── @Scheduled diario — actualizar estados a VENCIDA ─────────────
    @Scheduled(cron = "0 0 2 * * *") // 2 AM daily
    @Transactional
    public void actualizarEstados() {
        LocalDate hoy = LocalDate.now();
        List<Receivable> vencidas = receivableRepository.findVencidas(hoy);
        for (Receivable r : vencidas) {
            r.setEstado(Receivable.Estado.VENCIDA);
            receivableRepository.save(r);
        }
        if (!vencidas.isEmpty()) {
            log.info("Actualizadas {} cuentas por cobrar a estado VENCIDA", vencidas.size());
        }
    }

    // ── @Scheduled diario — calcular intereses por mora ──────────────
    @Scheduled(cron = "0 0 3 * * *") // 3 AM daily
    @Transactional
    public void calcularIntereses() {
        BigDecimal tasaAnual;
        try {
            tasaAnual = BigDecimal.valueOf(configService.getDoubleConfig(MORA_TASA_KEY));
        } catch (Exception e) {
            log.warn("Tasa de mora (mora.tasa) no configurada. Se usa 0%.");
            tasaAnual = BigDecimal.ZERO;
        }

        BigDecimal tasaDiaria = tasaAnual.divide(DIAS_ANO, 10, RoundingMode.HALF_UP);

        List<Receivable> vencidas = receivableRepository.findByEstado(Receivable.Estado.VENCIDA);
        LocalDate hoy = LocalDate.now();

        for (Receivable r : vencidas) {
            long diasMora = ChronoUnit.DAYS.between(r.getFechaVencimiento(), hoy);
            if (diasMora <= 0) continue;

            BigDecimal interes = r.getSaldoPendiente()
                    .multiply(tasaDiaria)
                    .multiply(BigDecimal.valueOf(diasMora))
                    .setScale(2, RoundingMode.HALF_UP);

            r.setInteresesAcumulados(r.getInteresesAcumulados().add(interes));
            receivableRepository.save(r);
        }

        if (!vencidas.isEmpty()) {
            log.info("Intereses calculados para {} cuentas vencidas", vencidas.size());
        }
    }

    // ── Mappers ───────────────────────────────────────────────────────
    private ReceivableResponse mapToResponse(Receivable r) {
        String clientName = null;
        String clientDocument = null;
        try {
            Client c = clientRepository.findById(r.getClientId()).orElse(null);
            if (c != null) {
                clientName = c.getName();
                clientDocument = c.getDocumentNumber();
            }
        } catch (Exception e) {
            // silencioso — campos opcionales
        }

        return ReceivableResponse.builder()
                .id(r.getId())
                .clientId(r.getClientId())
                .clientName(clientName)
                .clientDocument(clientDocument)
                .saleId(r.getSaleId())
                .montoOriginal(r.getMontoOriginal())
                .saldoPendiente(r.getSaldoPendiente())
                .fechaEmision(r.getFechaEmision())
                .fechaVencimiento(r.getFechaVencimiento())
                .estado(r.getEstado().name())
                .interesesAcumulados(r.getInteresesAcumulados())
                .build();
    }

    private ReceivablePaymentResponse mapPaymentToResponse(ReceivablePayment p) {
        return ReceivablePaymentResponse.builder()
                .id(p.getId())
                .receivableId(p.getReceivableId())
                .monto(p.getMonto())
                .metodoPago(p.getMetodoPago().name())
                .fecha(p.getFecha())
                .registradoPor(p.getRegistradoPor())
                .build();
    }
}
