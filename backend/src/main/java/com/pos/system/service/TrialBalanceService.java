package com.pos.system.service;

import com.pos.system.dto.response.TrialBalanceResponse;
import com.pos.system.repository.AccountingJournalLineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrialBalanceService {

    private final AccountingJournalLineRepository lineRepository;

    @Transactional(readOnly = true)
    public TrialBalanceResponse calculate(LocalDate fecha) {
        List<Object[]> rawData = lineRepository.calculateTrialBalanceRaw(fecha);

        List<TrialBalanceResponse.TrialBalanceRow> cuentas = rawData.stream()
                .map(row -> {
                    Long cuentaId = (Long) row[0];
                    String codigo = (String) row[1];
                    String nombre = (String) row[2];
                    BigDecimal debe = (BigDecimal) row[3];
                    BigDecimal haber = (BigDecimal) row[4];
                    BigDecimal saldo = debe.subtract(haber).setScale(2, RoundingMode.HALF_UP);

                    return TrialBalanceResponse.TrialBalanceRow.builder()
                            .cuentaId(cuentaId)
                            .codigo(codigo)
                            .nombre(nombre)
                            .debe(debe)
                            .haber(haber)
                            .saldo(saldo)
                            .build();
                })
                .collect(Collectors.toList());

        BigDecimal totalDebe = cuentas.stream()
                .map(TrialBalanceResponse.TrialBalanceRow::getDebe)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalHaber = cuentas.stream()
                .map(TrialBalanceResponse.TrialBalanceRow::getHaber)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        TrialBalanceResponse.Totales totales = TrialBalanceResponse.Totales.builder()
                .totalDebe(totalDebe)
                .totalHaber(totalHaber)
                .diferencia(totalDebe.subtract(totalHaber).setScale(2, RoundingMode.HALF_UP))
                .build();

        return TrialBalanceResponse.builder()
                .fecha(fecha)
                .cuentas(cuentas)
                .totales(totales)
                .build();
    }
}
