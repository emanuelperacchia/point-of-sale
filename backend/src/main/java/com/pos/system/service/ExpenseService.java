package com.pos.system.service;

import com.pos.system.dto.response.ExpenseResponse;
import com.pos.system.dto.response.ExpenseSummaryResponse;
import com.pos.system.entity.Expense;
import com.pos.system.entity.Expense.ExpenseCategory;
import com.pos.system.entity.Expense.ExpenseEstado;
import com.pos.system.entity.Expense.ExpenseFrecuencia;
import com.pos.system.exception.ResourceNotFoundException;
import com.pos.system.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio de control de gastos: CRUD, adjuntos, recurrentes, resumen.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final FileStorageService fileStorageService;

    // ── CRUD ───────────────────────────────────────────────────────────

    @Transactional
    public ExpenseResponse create(Expense expense, MultipartFile comprobante) {
        String fileUrl = null;
        if (comprobante != null && !comprobante.isEmpty()) {
            fileUrl = fileStorageService.save(comprobante, "expenses");
        }

        expense.setComprobanteUrl(fileUrl);
        expense.setEstado(ExpenseEstado.PENDIENTE);
        if (expense.getRecurrente() == null) expense.setRecurrente(false);
        if (expense.getRecurrente()) {
            expense.setProximaFecha(calcularProximaFecha(expense.getFecha(), expense.getFrecuencia()));
        }

        return toResponse(expenseRepository.save(expense));
    }

    @Transactional(readOnly = true)
    public ExpenseResponse getById(Long id) {
        return toResponse(expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Gasto no encontrado: " + id)));
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> getAll(String categoria, String estado, LocalDate desde, LocalDate hasta, Long proveedorId) {
        ExpenseCategory cat = categoria != null ? ExpenseCategory.valueOf(categoria) : null;
        ExpenseEstado est = estado != null ? ExpenseEstado.valueOf(estado) : null;
        return expenseRepository.findByFilters(cat, est, desde, hasta, proveedorId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public ExpenseResponse update(Long id, Expense update, MultipartFile comprobante) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Gasto no encontrado: " + id));

        if (update.getMonto() != null) expense.setMonto(update.getMonto());
        if (update.getFecha() != null) expense.setFecha(update.getFecha());
        if (update.getCategoria() != null) expense.setCategoria(update.getCategoria());
        if (update.getProveedorId() != null) expense.setProveedorId(update.getProveedorId());
        if (update.getDescripcion() != null) expense.setDescripcion(update.getDescripcion());
        if (update.getRecurrente() != null) {
            expense.setRecurrente(update.getRecurrente());
            if (update.getRecurrente() && update.getFrecuencia() != null) {
                expense.setFrecuencia(update.getFrecuencia());
                expense.setProximaFecha(calcularProximaFecha(expense.getFecha(), update.getFrecuencia()));
            }
        }

        if (comprobante != null && !comprobante.isEmpty()) {
            if (expense.getComprobanteUrl() != null) {
                fileStorageService.delete(expense.getComprobanteUrl());
            }
            expense.setComprobanteUrl(fileStorageService.save(comprobante, "expenses"));
        }

        return toResponse(expenseRepository.save(expense));
    }

    @Transactional
    public void delete(Long id) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Gasto no encontrado: " + id));
        if (expense.getComprobanteUrl() != null) {
            fileStorageService.delete(expense.getComprobanteUrl());
        }
        expenseRepository.deleteById(id);
    }

    @Transactional
    public void marcarPagado(Long id) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Gasto no encontrado: " + id));
        expense.setEstado(ExpenseEstado.PAGADO);
        expenseRepository.save(expense);
    }

    // ── Resumen ────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ExpenseSummaryResponse getSummary(LocalDate desde, LocalDate hasta) {
        List<Object[]> raw = expenseRepository.findTotalsByCategoria(desde, hasta);
        List<ExpenseSummaryResponse.CategoriaTotal> items = new ArrayList<>();
        for (Object[] row : raw) {
            items.add(new ExpenseSummaryResponse.CategoriaTotal(
                    (String) row[0], (BigDecimal) row[1]));
        }
        BigDecimal total = items.stream()
                .map(ExpenseSummaryResponse.CategoriaTotal::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ExpenseSummaryResponse(desde, hasta, items, total);
    }

    // ── Gastos Recurrentes ─────────────────────────────────────────────

    @Scheduled(cron = "0 0 6 * * *") // todos los días a las 06:00
    @Transactional
    public void generarRecurrentes() {
        LocalDate hoy = LocalDate.now();
        List<Expense> vencidos = expenseRepository.findRecurrentesVencidos(hoy);

        for (Expense original : vencidos) {
            Expense nuevo = Expense.builder()
                    .monto(original.getMonto())
                    .fecha(hoy)
                    .categoria(original.getCategoria())
                    .proveedorId(original.getProveedorId())
                    .descripcion(original.getDescripcion() + " (recurrente)")
                    .estado(ExpenseEstado.PENDIENTE)
                    .recurrente(false) // la instancia generada no es recurrente
                    .build();
            expenseRepository.save(nuevo);

            // Avanzar próxima fecha del original
            original.setProximaFecha(calcularProximaFecha(hoy, original.getFrecuencia()));
            expenseRepository.save(original);

            log.info("Gasto recurrente generado: {} - ${}", nuevo.getDescripcion(), nuevo.getMonto());
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────

    private LocalDate calcularProximaFecha(LocalDate desde, ExpenseFrecuencia frecuencia) {
        if (frecuencia == null) return null;
        return switch (frecuencia) {
            case MENSUAL -> desde.plusMonths(1);
            case TRIMESTRAL -> desde.plusMonths(3);
            case ANUAL -> desde.plusYears(1);
        };
    }

    private ExpenseResponse toResponse(Expense e) {
        return new ExpenseResponse(
                e.getId(), e.getMonto(), e.getFecha(),
                e.getCategoria().name(), e.getProveedorId(),
                e.getDescripcion(), e.getEstado().name(),
                e.getComprobanteUrl(), e.getRecurrente(),
                e.getFrecuencia() != null ? e.getFrecuencia().name() : null,
                e.getProximaFecha()
        );
    }
}
