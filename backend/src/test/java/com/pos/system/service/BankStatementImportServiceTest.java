package com.pos.system.service;

import com.pos.system.entity.BankReconciliation;
import com.pos.system.entity.BankStatement;
import com.pos.system.exception.BadRequestException;
import com.pos.system.repository.BankReconciliationRepository;
import com.pos.system.repository.BankStatementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BankStatementImportServiceTest {

    @Mock private BankReconciliationRepository reconciliationRepository;
    @Mock private BankStatementRepository statementRepository;

    private BankStatementImportService importService;

    @BeforeEach
    void setUp() {
        importService = new BankStatementImportService(reconciliationRepository, statementRepository);
    }

    @Test
    void importCsv_WhenNewPeriodo_ShouldCreateReconciliationAndImport() throws Exception {
        // Given
        String csv = "fecha,descripcion,monto,tipo\n" +
                     "2026-06-01,Deposito cliente,150000,CREDITO\n" +
                     "2026-06-02,Comision bancaria,2500,DEBITO\n";
        MultipartFile file = mockFile(csv);

        when(reconciliationRepository.findByPeriodo("2026-06")).thenReturn(Optional.empty());
        when(reconciliationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(statementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        BankReconciliation result = importService.importCsv(file, "2026-06");

        // Then
        assertNotNull(result);
        assertEquals("2026-06", result.getPeriodo());
        assertEquals(BankReconciliation.Estado.ABIERTA, result.getEstado());

        // Total: 150000 - 2500 = 147500
        assertEquals(BigDecimal.valueOf(147500), result.getTotalExtracto());

        ArgumentCaptor<BankStatement> captor = ArgumentCaptor.forClass(BankStatement.class);
        verify(statementRepository, times(2)).save(captor.capture());

        var statements = captor.getAllValues();
        assertEquals(2, statements.size());
        assertEquals("Deposito cliente", statements.get(0).getDescripcion());
        assertEquals(BankStatement.TipoMovimiento.CREDITO, statements.get(0).getTipo());
        assertEquals(BankStatement.TipoMovimiento.DEBITO, statements.get(1).getTipo());
        assertEquals(BigDecimal.valueOf(2500), statements.get(1).getMonto());
    }

    @Test
    void importCsv_WhenExistingPeriodo_ShouldAppendLines() throws Exception {
        // Given
        String csv = "fecha,descripcion,monto,tipo\n" +
                     "15/06/2026,Pago proveedor,80000,DEBITO\n";
        MultipartFile file = mockFile(csv);

        BankReconciliation existing = BankReconciliation.builder()
                .id(1L).periodo("2026-06")
                .totalExtracto(BigDecimal.ZERO)
                .totalSistema(BigDecimal.ZERO)
                .diferencia(BigDecimal.ZERO)
                .estado(BankReconciliation.Estado.ABIERTA)
                .build();

        when(reconciliationRepository.findByPeriodo("2026-06")).thenReturn(Optional.of(existing));
        when(statementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        BankReconciliation result = importService.importCsv(file, "2026-06");

        // Then — should reuse existing reconciliation (save called to update totals)
        assertEquals(1L, result.getId());
        verify(reconciliationRepository).save(any()); // save updates totals at end of import
        verify(statementRepository).save(any());
    }

    @Test
    void importCsv_WhenPeriodoCerrada_ShouldThrow() throws Exception {
        // Given
        MultipartFile file = mock(MultipartFile.class);

        BankReconciliation cerrada = BankReconciliation.builder()
                .id(1L).periodo("2026-06")
                .estado(BankReconciliation.Estado.CERRADA)
                .build();

        when(reconciliationRepository.findByPeriodo("2026-06")).thenReturn(Optional.of(cerrada));

        // When / Then
        assertThrows(BadRequestException.class,
                () -> importService.importCsv(file, "2026-06"));
    }

    @Test
    void importCsv_WithInvalidPeriodo_ShouldThrow() throws Exception {
        // Given
        MultipartFile file = mock(MultipartFile.class);

        // When / Then
        assertThrows(BadRequestException.class,
                () -> importService.importCsv(file, "invalido"));
    }

    @Test
    void importCsv_WithDifferentDateFormats_ShouldParseAll() throws Exception {
        // Given — test all 4 formats
        String csv = "fecha,descripcion,monto,tipo\n" +
                     "2026-06-01,Formato1,100,CREDITO\n" +
                     "15/06/2026,Formato2,200,CREDITO\n" +
                     "06/15/2026,Formato3,300,CREDITO\n" +
                     "20260620,Formato4,400,CREDITO\n";
        MultipartFile file = mockFile(csv);

        when(reconciliationRepository.findByPeriodo("2026-06")).thenReturn(Optional.empty());
        when(reconciliationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(statementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        BankReconciliation result = importService.importCsv(file, "2026-06");

        // Then
        assertNotNull(result);
        verify(statementRepository, times(4)).save(any());
    }

    @Test
    void importCsv_WhenInvalidLine_ShouldSkip() throws Exception {
        // Given
        String csv = "fecha,descripcion,monto,tipo\n" +
                     "2026-06-01,linea valida,100,CREDITO\n" +
                     "linea_invalida\n" +
                     "2026-06-02,otra linea,200,CREDITO\n";
        MultipartFile file = mockFile(csv);

        when(reconciliationRepository.findByPeriodo("2026-06")).thenReturn(Optional.empty());
        when(reconciliationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(statementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        BankReconciliation result = importService.importCsv(file, "2026-06");

        // Then — only valid lines should be imported
        verify(statementRepository, times(2)).save(any());
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private MultipartFile mockFile(String csv) {
        MultipartFile file = mock(MultipartFile.class);
        InputStream stream = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
        try {
            when(file.getInputStream()).thenReturn(stream);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return file;
    }
}
