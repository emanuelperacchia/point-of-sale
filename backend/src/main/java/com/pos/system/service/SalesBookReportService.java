package com.pos.system.service;

import com.pos.system.dto.response.SalesBookResponse;
import com.pos.system.dto.response.SalesBookRow;
import com.pos.system.dto.response.SalesBookTotals;
import com.pos.system.entity.InvoiceStatus;
import com.pos.system.entity.TipoComprobante;
import com.pos.system.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio para el libro de ventas: consulta, detección de saltos, totales.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SalesBookReportService {

    private final InvoiceRepository invoiceRepository;

    /**
     * Consulta el libro de ventas para un período.
     */
    public SalesBookResponse getSalesBook(LocalDate desde, LocalDate hasta, String tipo) {
        LocalDateTime desdeDt = desde.atStartOfDay();
        LocalDateTime hastaDt = hasta.plusDays(1).atStartOfDay();

        List<Object[]> raw = invoiceRepository.findSalesBookRaw(tipo, desdeDt, hastaDt);

        List<SalesBookRow> rows = new ArrayList<>();
        BigDecimal totalNeto = BigDecimal.ZERO;
        BigDecimal totalIva = BigDecimal.ZERO;
        BigDecimal totalComprobantes = BigDecimal.ZERO;

        for (Object[] row : raw) {
            LocalDateTime fecha = (LocalDateTime) row[0];
            String tipoStr = (String) row[1];
            Integer ptoVta = (Integer) row[2];
            Long numero = (Long) row[3];
            String cuit = (String) row[4];
            String razonSocial = (String) row[5];
            BigDecimal subtotal = (BigDecimal) row[6];
            BigDecimal discount = (BigDecimal) row[7];
            BigDecimal iva = (BigDecimal) row[8];
            BigDecimal total = (BigDecimal) row[9];
            String estadoStr = (String) row[10];

            TipoComprobante tipoComp = TipoComprobante.valueOf(tipoStr);
            InvoiceStatus estado = InvoiceStatus.valueOf(estadoStr);

            // Neto gravado = subtotal - descuento (base imponible)
            BigDecimal neto = subtotal.subtract(discount);
            if (neto.compareTo(BigDecimal.ZERO) < 0) neto = BigDecimal.ZERO;

            // Anulados: importe cero pero conservan el número
            BigDecimal importeRow = total;
            BigDecimal netoRow = neto;
            BigDecimal ivaRow = iva;
            if (estado == InvoiceStatus.ANULADO) {
                importeRow = BigDecimal.ZERO;
                netoRow = BigDecimal.ZERO;
                ivaRow = BigDecimal.ZERO;
            }

            rows.add(new SalesBookRow(
                    fecha, tipoComp, ptoVta, numero,
                    cuit, razonSocial, netoRow, ivaRow,
                    BigDecimal.ZERO, importeRow, estado
            ));

            totalNeto = totalNeto.add(netoRow);
            totalIva = totalIva.add(ivaRow);
            totalComprobantes = totalComprobantes.add(importeRow);
        }

        long cantidad = rows.size();
        SalesBookTotals totals = cantidad > 0
                ? new SalesBookTotals(totalNeto, totalIva, totalComprobantes, cantidad)
                : SalesBookTotals.empty();

        // Detección de saltos
        List<Long> gaps = detectGaps(raw);

        return new SalesBookResponse(rows, totals, gaps, 0, rows.size(), rows.size(), 1);
    }

    /**
     * Detecta números faltantes en la secuencia de cada tipo de comprobante.
     */
    public List<Long> detectGaps(List<Object[]> rawRows) {
        List<Long> gaps = new ArrayList<>();
        if (rawRows == null || rawRows.isEmpty()) return gaps;

        String currentTipo = null;
        Long lastNumero = null;

        for (Object[] row : rawRows) {
            String tipo = (String) row[1];
            Long numero = (Long) row[3];

            if (!tipo.equals(currentTipo)) {
                currentTipo = tipo;
                lastNumero = numero;
                continue;
            }

            // Deberían ser consecutivos: si hay salto, marcar faltantes
            if (lastNumero != null && numero - lastNumero > 1) {
                for (long n = lastNumero + 1; n < numero; n++) {
                    gaps.add(n);
                }
            }
            lastNumero = numero;
        }

        return gaps;
    }

    /**
     * Retorna las filas del libro para exportación (sin paginación).
     */
    public List<SalesBookRow> getRowsForExport(LocalDate desde, LocalDate hasta, String tipo) {
        return getSalesBook(desde, hasta, tipo).filas();
    }
}
