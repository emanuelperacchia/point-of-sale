package com.pos.system.service;

import com.pos.system.dto.request.CreateExpenseFromStatementRequest;
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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReconciliationMatchingService {

    private final BankReconciliationRepository reconciliationRepository;
    private final BankStatementRepository statementRepository;
    private final ReceivablePaymentRepository receivablePaymentRepository;
    private final PayablePaymentRepository payablePaymentRepository;
    private final ExpenseService expenseService;

    /**
     * Matching automático: busca coincidencias exactas por fecha (±1 día) y monto exacto
     * en receivable_payments y payable_payments. Si hay coincidencia única, marca ambos.
     */
    @Transactional
    public int autoMatch(Long reconciliationId) {
        BankReconciliation reconciliation = reconciliationRepository.findById(reconciliationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conciliación no encontrada: " + reconciliationId));

        if (reconciliation.getEstado() == BankReconciliation.Estado.CERRADA) {
            throw new BadRequestException("La conciliación ya está cerrada");
        }

        List<BankStatement> pendientes = statementRepository
                .findByReconciliationIdAndEstado(reconciliationId, BankStatement.EstadoConciliacion.PENDIENTE);

        int matched = 0;
        for (BankStatement st : pendientes) {
            LocalDate desde = st.getFecha().minusDays(1);
            LocalDate hasta = st.getFecha().plusDays(1);

            // Buscar en receivable_payments
            List<ReceivablePayment> receivableMatches = receivablePaymentRepository
                    .findByFechaBetween(desde.atStartOfDay(), hasta.atTime(LocalTime.MAX))
                    .stream()
                    .filter(p -> p.getMonto().compareTo(st.getMonto()) == 0)
                    .toList();

            // Buscar en payable_payments
            List<PayablePayment> payableMatches = payablePaymentRepository
                    .findByFechaBetween(desde.atStartOfDay(), hasta.atTime(LocalTime.MAX))
                    .stream()
                    .filter(p -> p.getMonto().compareTo(st.getMonto()) == 0)
                    .toList();

            // Si hay EXACTAMENTE una coincidencia total entre ambos grupos
            long totalMatches = receivableMatches.size() + payableMatches.size();

            if (totalMatches == 1) {
                // Match único — conciliar
                if (!receivableMatches.isEmpty()) {
                    st.setPaymentId(receivableMatches.get(0).getId());
                } else {
                    st.setPaymentId(payableMatches.get(0).getId());
                }
                st.setEstado(BankStatement.EstadoConciliacion.CONCILIADO);
                st.setObservacion("Match automático");
                statementRepository.save(st);
                matched++;
            }
            // Si hay 0 o múltiples matches, queda PENDIENTE para revisión manual
        }

        // Actualizar total del sistema y diferencia
        actualizarTotales(reconciliationId);

        log.info("Auto-match: {} líneas conciliadas de {} pendientes", matched, pendientes.size());
        return matched;
    }

    /**
     * Matching manual: el contador vincula una línea del extracto a un pago específico.
     */
    @Transactional
    public BankStatement manualMatch(Long statementId, Long paymentId, String tipo) {
        BankStatement st = statementRepository.findById(statementId)
                .orElseThrow(() -> new ResourceNotFoundException("Línea de extracto no encontrada: " + statementId));

        // Verificar que el pago exista según el tipo
        if ("RECEIVABLE_PAYMENT".equals(tipo)) {
            receivablePaymentRepository.findById(paymentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Pago de receivable no encontrado: " + paymentId));
        } else if ("PAYABLE_PAYMENT".equals(tipo)) {
            payablePaymentRepository.findById(paymentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Pago de payable no encontrado: " + paymentId));
        } else {
            throw new BadRequestException("Tipo inválido: " + tipo + ". Use RECEIVABLE_PAYMENT o PAYABLE_PAYMENT");
        }

        st.setPaymentId(paymentId);
        st.setEstado(BankStatement.EstadoConciliacion.CONCILIADO);
        st.setObservacion("Match manual (" + tipo + " #" + paymentId + ")");
        statementRepository.save(st);

        actualizarTotales(st.getReconciliationId());
        return st;
    }

    /**
     * Crea un gasto desde una línea de extracto sin contrapartida (ej. comisión bancaria).
     */
    @Transactional
    public BankStatement createExpenseFromStatement(CreateExpenseFromStatementRequest request, Long userId) {
        BankStatement st = statementRepository.findById(request.getStatementId())
                .orElseThrow(() -> new ResourceNotFoundException("Línea de extracto no encontrada: " + request.getStatementId()));

        if (st.getEstado() != BankStatement.EstadoConciliacion.PENDIENTE) {
            throw new BadRequestException("La línea ya está " + st.getEstado().name().toLowerCase());
        }

        if (st.getTipo() != BankStatement.TipoMovimiento.DEBITO) {
            throw new BadRequestException("Solo se pueden crear gastos desde líneas de tipo DEBITO");
        }

        // Crear el gasto como ajuste contable (sin comprobante)
        Expense expense = Expense.builder()
                .monto(request.getMonto() != null ? request.getMonto() : st.getMonto())
                .fecha(request.getFecha() != null ? request.getFecha() : st.getFecha())
                .categoria(Expense.ExpenseCategory.valueOf(request.getCategoria()))
                .descripcion(request.getDescripcion())
                .estado(Expense.ExpenseEstado.PAGADO)
                .recurrente(false)
                .build();

        expenseService.create(expense, null);

        // Marcar la línea como AJUSTE_MANUAL
        st.setEstado(BankStatement.EstadoConciliacion.AJUSTE_MANUAL);
        st.setObservacion("Gasto creado manualmente: " + request.getDescripcion());
        statementRepository.save(st);

        actualizarTotales(st.getReconciliationId());
        return st;
    }

    private void actualizarTotales(Long reconciliationId) {
        BankReconciliation reconciliation = reconciliationRepository.findById(reconciliationId).orElse(null);
        if (reconciliation == null) return;

        List<BankStatement> statements = statementRepository.findByReconciliationId(reconciliationId);

        BigDecimal totalExtracto = statements.stream()
                .map(s -> s.getTipo() == BankStatement.TipoMovimiento.CREDITO ? s.getMonto() : s.getMonto().negate())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Total sistema: suma de montos de líneas conciliadas
        BigDecimal totalSistema = statements.stream()
                .filter(s -> s.getEstado() == BankStatement.EstadoConciliacion.CONCILIADO)
                .map(s -> s.getTipo() == BankStatement.TipoMovimiento.CREDITO ? s.getMonto() : s.getMonto().negate())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        reconciliation.setTotalExtracto(totalExtracto);
        reconciliation.setTotalSistema(totalSistema);
        reconciliation.setDiferencia(totalExtracto.subtract(totalSistema));
        reconciliationRepository.save(reconciliation);
    }
}
