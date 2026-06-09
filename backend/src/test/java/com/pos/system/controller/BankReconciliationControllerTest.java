package com.pos.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pos.system.dto.request.CreateExpenseFromStatementRequest;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import com.pos.system.dto.request.ManualMatchRequest;
import com.pos.system.dto.response.BankReconciliationResponse;
import com.pos.system.dto.response.BankStatementResponse;
import com.pos.system.entity.*;
import com.pos.system.exception.GlobalExceptionHandler;
import com.pos.system.repository.BankReconciliationRepository;
import com.pos.system.repository.BankStatementRepository;
import com.pos.system.security.UserDetailsImpl;
import com.pos.system.service.BankStatementImportService;
import com.pos.system.service.ExcelExportService;
import com.pos.system.service.ReconciliationMatchingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class BankReconciliationControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private BankReconciliationRepository reconciliationRepository;
    private BankStatementRepository statementRepository;
    private BankStatementImportService importService;
    private ReconciliationMatchingService matchingService;
    private ExcelExportService excelExportService;
    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

    private UserDetailsImpl userDetails;

    @BeforeEach
    void setUp() {
        reconciliationRepository = mock(BankReconciliationRepository.class);
        statementRepository = mock(BankStatementRepository.class);
        importService = mock(BankStatementImportService.class);
        matchingService = mock(ReconciliationMatchingService.class);
        excelExportService = mock(ExcelExportService.class);

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        MappingJackson2HttpMessageConverter jacksonConverter =
                new MappingJackson2HttpMessageConverter(objectMapper);

        BankReconciliationController controller = new BankReconciliationController(
                reconciliationRepository, statementRepository, importService,
                matchingService, excelExportService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(exceptionHandler)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setMessageConverters(
                        new ByteArrayHttpMessageConverter(),
                        new StringHttpMessageConverter(),
                        jacksonConverter)
                .build();

        User user = User.builder()
                .id(1L).firstName("Admin").lastName("User")
                .email("admin@test.com")
                .build();
        userDetails = new UserDetailsImpl(user);

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void importCsv_ShouldReturn200() throws Exception {
        // Given
        BankReconciliation rec = BankReconciliation.builder()
                .id(1L).periodo("2026-06")
                .totalExtracto(BigDecimal.valueOf(100000))
                .totalSistema(BigDecimal.ZERO)
                .diferencia(BigDecimal.valueOf(100000))
                .estado(BankReconciliation.Estado.ABIERTA)
                .build();

        when(importService.importCsv(any(), eq("2026-06"))).thenReturn(rec);
        when(statementRepository.countByReconciliationIdAndEstado(anyLong(), any()))
                .thenReturn(0L);

        MockMultipartFile file = new MockMultipartFile(
                "file", "extracto.csv", "text/csv", "fecha,desc,monto,tipo\n2026-06-01,test,100,CREDITO\n".getBytes());

        // When / Then
        mockMvc.perform(multipart("/api/bank-reconciliation/import")
                        .file(file)
                        .param("periodo", "2026-06"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodo").value("2026-06"))
                .andExpect(jsonPath("$.totalExtracto").value(100000));
    }

    @Test
    void getSummary_WhenExists_ShouldReturn200() throws Exception {
        // Given
        BankReconciliation rec = BankReconciliation.builder()
                .id(1L).periodo("2026-06")
                .totalExtracto(BigDecimal.valueOf(150000))
                .totalSistema(BigDecimal.valueOf(140000))
                .diferencia(BigDecimal.valueOf(10000))
                .estado(BankReconciliation.Estado.ABIERTA)
                .build();

        when(reconciliationRepository.findByPeriodo("2026-06")).thenReturn(Optional.of(rec));
        when(statementRepository.countByReconciliationIdAndEstado(eq(1L), any()))
                .thenReturn(2L); // PENDIENTE

        // When / Then
        mockMvc.perform(get("/api/bank-reconciliation/summary")
                        .param("periodo", "2026-06"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodo").value("2026-06"))
                .andExpect(jsonPath("$.totalExtracto").value(150000))
                .andExpect(jsonPath("$.diferencia").value(10000));
    }

    @Test
    void getSummary_WhenNotExists_ShouldReturnNoIniciada() throws Exception {
        // Given
        when(reconciliationRepository.findByPeriodo("2026-07")).thenReturn(Optional.empty());

        // When / Then
        mockMvc.perform(get("/api/bank-reconciliation/summary")
                        .param("periodo", "2026-07"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("NO_INICIADA"))
                .andExpect(jsonPath("$.totalExtracto").value(0));
    }

    @Test
    void getStatements_ShouldReturn200() throws Exception {
        // Given
        BankStatement st = BankStatement.builder()
                .id(10L).reconciliationId(1L)
                .fecha(LocalDate.now()).descripcion("Deposito")
                .monto(BigDecimal.valueOf(50000))
                .tipo(BankStatement.TipoMovimiento.CREDITO)
                .estado(BankStatement.EstadoConciliacion.PENDIENTE)
                .observacion(null)
                .build();

        when(statementRepository.findByReconciliationId(1L)).thenReturn(List.of(st));

        // When / Then
        mockMvc.perform(get("/api/bank-reconciliation/1/statements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].tipo").value("CREDITO"))
                .andExpect(jsonPath("$[0].estado").value("PENDIENTE"));
    }

    @Test
    void autoMatch_ShouldReturn200() throws Exception {
        // Given
        when(matchingService.autoMatch(1L)).thenReturn(3);

        // When / Then
        mockMvc.perform(post("/api/bank-reconciliation/1/auto-match"))
                .andExpect(status().isOk())
                .andExpect(content().string("Conciliadas 3 líneas automáticamente"));
    }

    @Test
    void manualMatch_ShouldReturn200() throws Exception {
        // Given
        ManualMatchRequest request = new ManualMatchRequest(null, null, null);
        request.setStatementId(10L);
        request.setPaymentId(99L);
        request.setTipo("RECEIVABLE_PAYMENT");

        BankStatement matched = BankStatement.builder()
                .id(10L).reconciliationId(1L)
                .fecha(LocalDate.now()).descripcion("Deposito")
                .monto(BigDecimal.valueOf(50000))
                .tipo(BankStatement.TipoMovimiento.CREDITO)
                .estado(BankStatement.EstadoConciliacion.CONCILIADO)
                .paymentId(99L)
                .build();

        when(matchingService.manualMatch(eq(10L), eq(99L), eq("RECEIVABLE_PAYMENT")))
                .thenReturn(matched);

        // When / Then
        mockMvc.perform(post("/api/bank-reconciliation/statements/10/manual-match")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CONCILIADO"));
    }

    @Test
    void createExpense_ShouldReturn200() throws Exception {
        // Given
        CreateExpenseFromStatementRequest request = new CreateExpenseFromStatementRequest();
        request.setStatementId(10L);
        request.setMonto(BigDecimal.valueOf(2500));
        request.setCategoria("SERVICIOS");
        request.setDescripcion("Comision bancaria");
        request.setFecha(LocalDate.now());

        BankStatement st = BankStatement.builder()
                .id(10L).reconciliationId(1L)
                .fecha(LocalDate.now()).descripcion("Comision")
                .monto(BigDecimal.valueOf(2500))
                .tipo(BankStatement.TipoMovimiento.DEBITO)
                .estado(BankStatement.EstadoConciliacion.AJUSTE_MANUAL)
                .observacion("Gasto creado manualmente: Comision bancaria")
                .build();

        when(matchingService.createExpenseFromStatement(any(), eq(1L)))
                .thenReturn(st);

        // When / Then
        mockMvc.perform(post("/api/bank-reconciliation/statements/10/create-expense")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("AJUSTE_MANUAL"));
    }

    @Test
    void exportSummary_ShouldReturnExcel() throws Exception {
        // Given
        BankReconciliation rec = BankReconciliation.builder()
                .id(1L).periodo("2026-06")
                .totalExtracto(BigDecimal.valueOf(100000))
                .totalSistema(BigDecimal.valueOf(95000))
                .diferencia(BigDecimal.valueOf(5000))
                .estado(BankReconciliation.Estado.ABIERTA)
                .build();

        when(reconciliationRepository.findByPeriodo("2026-06")).thenReturn(Optional.of(rec));
        when(statementRepository.findByReconciliationId(1L)).thenReturn(List.of());
        when(excelExportService.generate(anyString(), anyList(), anyList(), any(), any()))
                .thenReturn(new byte[]{1, 2, 3});

        // When / Then
        mockMvc.perform(get("/api/bank-reconciliation/summary/export")
                        .param("periodo", "2026-06"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=conciliacion-2026-06.xlsx"));
    }
}
