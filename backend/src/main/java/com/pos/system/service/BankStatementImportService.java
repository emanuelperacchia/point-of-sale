package com.pos.system.service;

import com.pos.system.entity.BankReconciliation;
import com.pos.system.entity.BankStatement;
import com.pos.system.exception.BadRequestException;
import com.pos.system.repository.BankReconciliationRepository;
import com.pos.system.repository.BankStatementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BankStatementImportService {

    private static final DateTimeFormatter[] FORMATS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("yyyyMMdd")
    };

    private final BankReconciliationRepository reconciliationRepository;
    private final BankStatementRepository statementRepository;

    /**
     * Importa un archivo CSV de extracto bancario.
     * Formato esperado: fecha,descripción,monto,tipo (CREDITO/DEBITO)
     * Primera línea es encabezado y se salta.
     */
    @Transactional
    public BankReconciliation importCsv(MultipartFile file, String periodo) {
        // Validar período
        YearMonth ym;
        try {
            ym = YearMonth.parse(periodo);
        } catch (DateTimeParseException e) {
            throw new BadRequestException("Formato de período inválido. Use YYYY-MM");
        }

        // Buscar o crear reconciliación
        BankReconciliation reconciliation = reconciliationRepository.findByPeriodo(periodo)
                .orElseGet(() -> {
                    BankReconciliation br = BankReconciliation.builder()
                            .periodo(periodo)
                            .totalExtracto(BigDecimal.ZERO)
                            .totalSistema(BigDecimal.ZERO)
                            .diferencia(BigDecimal.ZERO)
                            .estado(BankReconciliation.Estado.ABIERTA)
                            .build();
                    return reconciliationRepository.save(br);
                });

        if (reconciliation.getEstado() == BankReconciliation.Estado.CERRADA) {
            throw new BadRequestException("La conciliación del período " + periodo + " ya está cerrada");
        }

        // Parsear CSV
        List<BankStatement> statements = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            int lineNum = 0;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                if (lineNum == 1) continue; // skip header

                String[] parts = line.split(",");
                if (parts.length < 4) {
                    log.warn("Línea {} ignorada: formato inválido", lineNum);
                    continue;
                }

                LocalDate fecha = parseFecha(parts[0].trim());
                String descripcion = parts[1].trim();
                BigDecimal monto = new BigDecimal(parts[2].trim());
                BankStatement.TipoMovimiento tipo = parseTipo(parts[3].trim());

                BankStatement st = BankStatement.builder()
                        .reconciliationId(reconciliation.getId())
                        .fecha(fecha)
                        .descripcion(descripcion)
                        .monto(monto)
                        .tipo(tipo)
                        .estado(BankStatement.EstadoConciliacion.PENDIENTE)
                        .build();
                statements.add(statementRepository.save(st));
            }
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Error al leer el archivo CSV: " + e.getMessage());
        }

        // Actualizar total del extracto
        BigDecimal totalExtracto = statements.stream()
                .map(s -> s.getTipo() == BankStatement.TipoMovimiento.CREDITO ? s.getMonto() : s.getMonto().negate())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        reconciliation.setTotalExtracto(totalExtracto);
        reconciliation.setTotalSistema(BigDecimal.ZERO);
        reconciliation.setDiferencia(totalExtracto);
        reconciliationRepository.save(reconciliation);

        log.info("Importadas {} líneas de extracto bancario para período {}", statements.size(), periodo);
        return reconciliation;
    }

    private LocalDate parseFecha(String value) {
        for (DateTimeFormatter fmt : FORMATS) {
            try {
                return LocalDate.parse(value, fmt);
            } catch (DateTimeParseException ignored) {
            }
        }
        throw new BadRequestException("Formato de fecha inválido: " + value);
    }

    private BankStatement.TipoMovimiento parseTipo(String value) {
        try {
            return BankStatement.TipoMovimiento.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Tipo de movimiento inválido: " + value + ". Use CREDITO o DEBITO");
        }
    }
}
