package com.pos.system.service;

import com.pos.system.dto.response.AgingReportResponse;
import com.pos.system.entity.Receivable;
import com.pos.system.entity.Client;
import com.pos.system.repository.ClientRepository;
import com.pos.system.repository.ReceivableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AgingReportService {

    private final ReceivableRepository receivableRepository;
    private final ClientRepository clientRepository;

    @Transactional(readOnly = true)
    public AgingReportResponse generateReport() {
        List<Receivable> active = receivableRepository.findAllActive();
        LocalDate hoy = LocalDate.now();

        // Resumen general
        AgingReportResponse.ResumenGeneral resumen = calcularResumen(active, hoy);

        // Por cliente
        Map<Long, List<Receivable>> porCliente = active.stream()
                .collect(Collectors.groupingBy(Receivable::getClientId));

        List<AgingReportResponse.TramoCliente> tramosCliente = new ArrayList<>();
        for (Map.Entry<Long, List<Receivable>> entry : porCliente.entrySet()) {
            Client client = clientRepository.findById(entry.getKey()).orElse(null);
            if (client == null) continue;

            AgingReportResponse.ResumenGeneral clienteResumen = calcularResumen(entry.getValue(), hoy);
            tramosCliente.add(AgingReportResponse.TramoCliente.builder()
                    .clientId(client.getId())
                    .clientName(client.getName())
                    .clientDocument(client.getDocumentNumber())
                    .corriente(clienteResumen.getCorriente())
                    .tramo1a30(clienteResumen.getTramo1a30())
                    .tramo31a60(clienteResumen.getTramo31a60())
                    .tramo61a90(clienteResumen.getTramo61a90())
                    .masDe90(clienteResumen.getMasDe90())
                    .total(clienteResumen.getTotal())
                    .build());
        }

        tramosCliente.sort(Comparator.comparing(AgingReportResponse.TramoCliente::getTotal).reversed());

        return AgingReportResponse.builder()
                .resumenGeneral(resumen)
                .porCliente(tramosCliente)
                .build();
    }

    private AgingReportResponse.ResumenGeneral calcularResumen(List<Receivable> receivables, LocalDate hoy) {
        BigDecimal corriente = BigDecimal.ZERO;
        BigDecimal tramo1a30 = BigDecimal.ZERO;
        BigDecimal tramo31a60 = BigDecimal.ZERO;
        BigDecimal tramo61a90 = BigDecimal.ZERO;
        BigDecimal masDe90 = BigDecimal.ZERO;

        for (Receivable r : receivables) {
            long dias = ChronoUnit.DAYS.between(r.getFechaVencimiento(), hoy);
            BigDecimal saldo = r.getSaldoPendiente();

            if (dias <= 0) {
                corriente = corriente.add(saldo);
            } else if (dias <= 30) {
                tramo1a30 = tramo1a30.add(saldo);
            } else if (dias <= 60) {
                tramo31a60 = tramo31a60.add(saldo);
            } else if (dias <= 90) {
                tramo61a90 = tramo61a90.add(saldo);
            } else {
                masDe90 = masDe90.add(saldo);
            }
        }

        BigDecimal total = corriente.add(tramo1a30).add(tramo31a60).add(tramo61a90).add(masDe90);

        return AgingReportResponse.ResumenGeneral.builder()
                .corriente(corriente)
                .tramo1a30(tramo1a30)
                .tramo31a60(tramo31a60)
                .tramo61a90(tramo61a90)
                .masDe90(masDe90)
                .total(total)
                .build();
    }
}
