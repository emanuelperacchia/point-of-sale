package com.pos.system.service;

import com.pos.system.dto.response.AgingReportResponse;
import com.pos.system.entity.Client;
import com.pos.system.entity.Receivable;
import com.pos.system.repository.ClientRepository;
import com.pos.system.repository.ReceivableRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgingReportServiceTest {

    @Mock private ReceivableRepository receivableRepository;
    @Mock private ClientRepository clientRepository;

    private AgingReportService agingReportService;

    private Client client;
    private final LocalDate today = LocalDate.now();

    @BeforeEach
    void setUp() {
        agingReportService = new AgingReportService(receivableRepository, clientRepository);

        client = Client.builder()
                .id(1L).name("Juan Pérez").documentNumber("30-12345678-9")
                .build();
    }

    @Test
    void generateReport_WithReceivablesInDifferentTramos_ShouldGroupCorrectly() {
        // Current (vencimiento futuro)
        Receivable corriente = Receivable.builder()
                .id(1L).clientId(1L).saleId(10L)
                .montoOriginal(BigDecimal.valueOf(10000))
                .saldoPendiente(BigDecimal.valueOf(10000))
                .fechaEmision(today.minusDays(20))
                .fechaVencimiento(today.plusDays(10))
                .estado(Receivable.Estado.PENDIENTE)
                .interesesAcumulados(BigDecimal.ZERO)
                .build();

        // Tramo 1-30 días de vencida
        Receivable tramo1a30 = Receivable.builder()
                .id(2L).clientId(1L).saleId(20L)
                .montoOriginal(BigDecimal.valueOf(20000))
                .saldoPendiente(BigDecimal.valueOf(20000))
                .fechaEmision(today.minusDays(50))
                .fechaVencimiento(today.minusDays(10))
                .estado(Receivable.Estado.VENCIDA)
                .interesesAcumulados(BigDecimal.ZERO)
                .build();

        // Tramo 31-60 días de vencida
        Receivable tramo31a60 = Receivable.builder()
                .id(3L).clientId(1L).saleId(30L)
                .montoOriginal(BigDecimal.valueOf(30000))
                .saldoPendiente(BigDecimal.valueOf(30000))
                .fechaEmision(today.minusDays(90))
                .fechaVencimiento(today.minusDays(45))
                .estado(Receivable.Estado.VENCIDA)
                .interesesAcumulados(BigDecimal.ZERO)
                .build();

        when(receivableRepository.findAllActive())
                .thenReturn(List.of(corriente, tramo1a30, tramo31a60));
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));

        // When
        AgingReportResponse report = agingReportService.generateReport();

        // Then
        assertNotNull(report);
        assertEquals(BigDecimal.valueOf(10000), report.getResumenGeneral().getCorriente());
        assertEquals(BigDecimal.valueOf(20000), report.getResumenGeneral().getTramo1a30());
        assertEquals(BigDecimal.valueOf(30000), report.getResumenGeneral().getTramo31a60());
        assertEquals(BigDecimal.ZERO, report.getResumenGeneral().getTramo61a90());
        assertEquals(BigDecimal.ZERO, report.getResumenGeneral().getMasDe90());
        assertEquals(BigDecimal.valueOf(60000), report.getResumenGeneral().getTotal());

        assertEquals(1, report.getPorCliente().size());
        assertEquals("Juan Pérez", report.getPorCliente().get(0).getClientName());
    }

    @Test
    void generateReport_WithMasDe90Dias_ShouldGroupCorrectly() {
        // Más de 90 días vencida
        Receivable muyVencida = Receivable.builder()
                .id(4L).clientId(1L).saleId(40L)
                .montoOriginal(BigDecimal.valueOf(50000))
                .saldoPendiente(BigDecimal.valueOf(50000))
                .fechaEmision(today.minusDays(200))
                .fechaVencimiento(today.minusDays(120))
                .estado(Receivable.Estado.VENCIDA)
                .interesesAcumulados(BigDecimal.valueOf(2000))
                .build();

        when(receivableRepository.findAllActive()).thenReturn(List.of(muyVencida));
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));

        // When
        AgingReportResponse report = agingReportService.generateReport();

        // Then
        assertEquals(BigDecimal.ZERO, report.getResumenGeneral().getCorriente());
        assertEquals(BigDecimal.ZERO, report.getResumenGeneral().getTramo1a30());
        assertEquals(BigDecimal.ZERO, report.getResumenGeneral().getTramo31a60());
        assertEquals(BigDecimal.ZERO, report.getResumenGeneral().getTramo61a90());
        assertEquals(BigDecimal.valueOf(50000), report.getResumenGeneral().getMasDe90());
    }

    @Test
    void generateReport_WhenNoActiveReceivables_ShouldReturnZeros() {
        // Given
        when(receivableRepository.findAllActive()).thenReturn(List.of());

        // When
        AgingReportResponse report = agingReportService.generateReport();

        // Then
        assertNotNull(report);
        assertEquals(BigDecimal.ZERO, report.getResumenGeneral().getCorriente());
        assertEquals(BigDecimal.ZERO, report.getResumenGeneral().getTotal());
        assertTrue(report.getPorCliente().isEmpty());
    }
}
