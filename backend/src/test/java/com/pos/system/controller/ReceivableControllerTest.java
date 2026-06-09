package com.pos.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pos.system.dto.request.ReceivablePaymentRequest;
import com.pos.system.dto.response.AgingReportResponse;
import com.pos.system.dto.response.ReceivablePaymentResponse;
import com.pos.system.dto.response.ReceivableResponse;
import com.pos.system.entity.User;
import com.pos.system.exception.GlobalExceptionHandler;
import com.pos.system.security.UserDetailsImpl;
import com.pos.system.service.AgingReportService;
import com.pos.system.service.ExcelExportService;
import com.pos.system.service.ReceivableService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ReceivableControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private ReceivableService receivableService;
    private AgingReportService agingReportService;
    private ExcelExportService excelExportService;
    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

    private UserDetailsImpl userDetails;

    @BeforeEach
    void setUp() {
        receivableService = mock(ReceivableService.class);
        agingReportService = mock(AgingReportService.class);
        excelExportService = mock(ExcelExportService.class);

        ReceivableController controller = new ReceivableController(receivableService, agingReportService, excelExportService);
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(exceptionHandler)
                .setCustomArgumentResolvers(
                        new AuthenticationPrincipalArgumentResolver(),
                        new PageableHandlerMethodArgumentResolver())
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
    void list_ShouldReturn200() throws Exception {
        // Given
        ReceivableResponse response = ReceivableResponse.builder()
                .id(1L).clientId(1L).clientName("Juan")
                .saleId(10L).montoOriginal(BigDecimal.valueOf(50000))
                .saldoPendiente(BigDecimal.valueOf(50000))
                .fechaEmision(LocalDate.now()).fechaVencimiento(LocalDate.now().plusDays(30))
                .estado("PENDIENTE").interesesAcumulados(BigDecimal.ZERO)
                .build();

        when(receivableService.findByFilters(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(response), PageRequest.of(0, 20), 1));

        // When / Then
        mockMvc.perform(get("/api/receivables")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getById_ShouldReturn200() throws Exception {
        ReceivableResponse response = ReceivableResponse.builder()
                .id(1L).clientId(1L).clientName("Juan")
                .saleId(10L).montoOriginal(BigDecimal.valueOf(50000))
                .saldoPendiente(BigDecimal.valueOf(50000))
                .fechaEmision(LocalDate.now()).fechaVencimiento(LocalDate.now().plusDays(30))
                .estado("PENDIENTE").interesesAcumulados(BigDecimal.ZERO)
                .build();

        when(receivableService.getById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/receivables/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.clientName").value("Juan"));
    }

    @Test
    void getPayments_ShouldReturn200() throws Exception {
        ReceivablePaymentResponse payment = ReceivablePaymentResponse.builder()
                .id(1L).receivableId(1L)
                .monto(BigDecimal.valueOf(10000))
                .metodoPago("TRANSFER")
                .fecha(LocalDateTime.now()).registradoPor(1L)
                .build();

        when(receivableService.getPayments(1L)).thenReturn(List.of(payment));

        mockMvc.perform(get("/api/receivables/1/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].monto").value(10000))
                .andExpect(jsonPath("$[0].metodoPago").value("TRANSFER"));
    }

    @Test
    void registerPayment_ShouldReturn200() throws Exception {
        ReceivablePaymentRequest request = ReceivablePaymentRequest.builder()
                .monto(BigDecimal.valueOf(50000))
                .metodoPago("TRANSFER")
                .build();

        ReceivableResponse response = ReceivableResponse.builder()
                .id(1L).clientId(1L)
                .saldoPendiente(BigDecimal.ZERO)
                .estado("COBRADA")
                .build();

        when(receivableService.registrarPago(eq(1L), any(), eq(1L))).thenReturn(response);

        mockMvc.perform(post("/api/receivables/1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("COBRADA"));
    }

    @Test
    void agingReport_ShouldReturn200() throws Exception {
        AgingReportResponse.ResumenGeneral resumen = AgingReportResponse.ResumenGeneral.builder()
                .corriente(BigDecimal.valueOf(10000))
                .tramo1a30(BigDecimal.valueOf(20000))
                .tramo31a60(BigDecimal.valueOf(0))
                .tramo61a90(BigDecimal.valueOf(0))
                .masDe90(BigDecimal.valueOf(0))
                .total(BigDecimal.valueOf(30000))
                .build();

        AgingReportResponse report = AgingReportResponse.builder()
                .resumenGeneral(resumen)
                .porCliente(List.of())
                .build();

        when(agingReportService.generateReport()).thenReturn(report);

        mockMvc.perform(get("/api/receivables/aging-report"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resumenGeneral.total").value(30000));
    }

    @Test
    void exportAgingReport_ShouldReturnExcel() throws Exception {
        AgingReportResponse.ResumenGeneral resumen = AgingReportResponse.ResumenGeneral.builder()
                .corriente(BigDecimal.ZERO).tramo1a30(BigDecimal.ZERO)
                .tramo31a60(BigDecimal.ZERO).tramo61a90(BigDecimal.ZERO)
                .masDe90(BigDecimal.ZERO).total(BigDecimal.ZERO)
                .build();

        AgingReportResponse report = AgingReportResponse.builder()
                .resumenGeneral(resumen)
                .porCliente(List.of())
                .build();

        when(agingReportService.generateReport()).thenReturn(report);
        when(excelExportService.generate(anyString(), anyList(), anyList(), any(), any()))
                .thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/api/receivables/aging-report/export"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=aging-report.xlsx"))
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }
}
