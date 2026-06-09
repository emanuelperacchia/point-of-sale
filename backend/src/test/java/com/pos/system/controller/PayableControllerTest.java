package com.pos.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pos.system.dto.request.PayablePaymentRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import com.pos.system.dto.request.PayableRequest;
import com.pos.system.dto.response.PayablePaymentResponse;
import com.pos.system.dto.response.PayableResponse;
import com.pos.system.entity.User;
import com.pos.system.exception.GlobalExceptionHandler;
import com.pos.system.security.UserDetailsImpl;
import com.pos.system.service.PayableService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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

class PayableControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private PayableService payableService;
    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

    private UserDetailsImpl userDetails;

    @BeforeEach
    void setUp() {
        payableService = mock(PayableService.class);
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        PayableController controller = new PayableController(payableService);

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
        PayableResponse response = PayableResponse.builder()
                .id(1L).supplierId(1L).supplierName("Proveedor S.A.")
                .purchaseOrderId(50L).montoOriginal(BigDecimal.valueOf(100000))
                .saldoPendiente(BigDecimal.valueOf(100000))
                .fechaEmision(LocalDate.now()).fechaVencimiento(LocalDate.now().plusDays(30))
                .estado("PENDIENTE").referenciaBancaria(null)
                .build();

        when(payableService.findByFilters(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(response), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/payables")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getById_ShouldReturn200() throws Exception {
        PayableResponse response = PayableResponse.builder()
                .id(1L).supplierId(1L).supplierName("Proveedor S.A.")
                .montoOriginal(BigDecimal.valueOf(50000))
                .saldoPendiente(BigDecimal.valueOf(50000))
                .fechaEmision(LocalDate.now()).fechaVencimiento(LocalDate.now().plusDays(30))
                .estado("PENDIENTE").referenciaBancaria(null)
                .build();

        when(payableService.getById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/payables/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.supplierName").value("Proveedor S.A."));
    }

    @Test
    void getPayments_ShouldReturn200() throws Exception {
        PayablePaymentResponse payment = PayablePaymentResponse.builder()
                .id(1L).payableId(1L)
                .monto(BigDecimal.valueOf(50000))
                .metodoPago("TRANSFER")
                .fecha(LocalDateTime.now()).registradoPor(1L)
                .build();

        when(payableService.getPayments(1L)).thenReturn(List.of(payment));

        mockMvc.perform(get("/api/payables/1/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].monto").value(50000))
                .andExpect(jsonPath("$[0].metodoPago").value("TRANSFER"));
    }

    @Test
    void getUpcoming_ShouldReturn200() throws Exception {
        PayableResponse response = PayableResponse.builder()
                .id(1L).supplierId(1L)
                .montoOriginal(BigDecimal.valueOf(30000))
                .saldoPendiente(BigDecimal.valueOf(30000))
                .fechaEmision(LocalDate.now()).fechaVencimiento(LocalDate.now().plusDays(15))
                .estado("PENDIENTE").referenciaBancaria(null)
                .build();

        when(payableService.getUpcoming(30)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/payables/upcoming")
                        .param("dias", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void create_ShouldReturn200() throws Exception {
        PayableRequest request = PayableRequest.builder()
                .supplierId(1L)
                .montoOriginal(BigDecimal.valueOf(50000))
                .fechaEmision(LocalDate.now())
                .fechaVencimiento(LocalDate.now().plusDays(30))
                .build();

        PayableResponse response = PayableResponse.builder()
                .id(1L).supplierId(1L).supplierName("Proveedor S.A.")
                .montoOriginal(BigDecimal.valueOf(50000))
                .saldoPendiente(BigDecimal.valueOf(50000))
                .fechaEmision(LocalDate.now()).fechaVencimiento(LocalDate.now().plusDays(30))
                .estado("PENDIENTE")
                .build();

        when(payableService.createPayable(any())).thenReturn(response);

        mockMvc.perform(post("/api/payables")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void registerPayment_ShouldReturn200() throws Exception {
        PayablePaymentRequest request = PayablePaymentRequest.builder()
                .monto(BigDecimal.valueOf(50000))
                .metodoPago("TRANSFER")
                .build();

        PayableResponse response = PayableResponse.builder()
                .id(1L).supplierId(1L)
                .saldoPendiente(BigDecimal.ZERO)
                .estado("PAGADA")
                .build();

        when(payableService.registrarPago(eq(1L), any(), eq(1L))).thenReturn(response);

        mockMvc.perform(post("/api/payables/1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PAGADA"));
    }
}
