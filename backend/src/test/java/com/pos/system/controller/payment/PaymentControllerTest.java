package com.pos.system.controller.payment;

import com.pos.system.dto.payment.PaymentRequest;
import com.pos.system.dto.payment.PaymentResponse;
import com.pos.system.entity.payment.Payment.PaymentStatus;
import com.pos.system.entity.payment.Payment.PaymentType;
import com.pos.system.service.payment.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class PaymentControllerTest {

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentController paymentController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(paymentController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void testCreatePayment() throws Exception {
        PaymentRequest request = new PaymentRequest(
            "CREDIT_CARD", new BigDecimal("10000.00"),
            new BigDecimal("1900.00"), new BigDecimal("11900.00"),
            "CREDIT_CARD", "REF-12345", "Pago de suscripción"
        );

        PaymentResponse response = new PaymentResponse();
        response.setId(1L);
        response.setTransactionId("TXN-123");
        response.setAmount(new BigDecimal("10000.00"));
        response.setTotalAmount(new BigDecimal("11900.00"));
        response.setPaymentStatus(PaymentStatus.PENDING.name());

        when(paymentService.createPayment(anyLong(), anyLong(), any(PaymentRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/payments/businesses/{businessId}/users/{userId}", 1L, 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.amount").value(10000.00));

        verify(paymentService, times(1)).createPayment(anyLong(), anyLong(), any(PaymentRequest.class));
    }

    @Test
    void testGetPaymentById() throws Exception {
        PaymentResponse response = new PaymentResponse();
        response.setId(1L);
        response.setAmount(new BigDecimal("10000.00"));
        response.setTotalAmount(new BigDecimal("11900.00"));
        response.setPaymentStatus(PaymentStatus.PENDING.name());

        when(paymentService.getPaymentById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/payments/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.amount").value(10000.00));

        verify(paymentService, times(1)).getPaymentById(1L);
    }

    @Test
    void testGetPaymentsByBusiness() throws Exception {
        PaymentResponse p1 = new PaymentResponse();
        p1.setId(1L);
        p1.setAmount(new BigDecimal("10000.00"));
        p1.setPaymentStatus(PaymentStatus.PENDING.name());

        PaymentResponse p2 = new PaymentResponse();
        p2.setId(2L);
        p2.setAmount(new BigDecimal("20000.00"));
        p2.setPaymentStatus(PaymentStatus.COMPLETED.name());

        List<PaymentResponse> payments = Arrays.asList(p1, p2);

        when(paymentService.getPaymentsByBusiness(1L)).thenReturn(payments);

        mockMvc.perform(get("/api/payments/businesses/{businessId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[1].id").value(2L));

        verify(paymentService, times(1)).getPaymentsByBusiness(1L);
    }

    @Test
    void testUpdatePaymentStatus() throws Exception {
        PaymentResponse response = new PaymentResponse();
        response.setId(1L);
        response.setPaymentStatus(PaymentStatus.COMPLETED.name());

        when(paymentService.updatePaymentStatus(1L, PaymentStatus.COMPLETED)).thenReturn(response);

        mockMvc.perform(put("/api/payments/{paymentId}/status", 1L)
                .param("status", "COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.paymentStatus").value("COMPLETED"));

        verify(paymentService, times(1)).updatePaymentStatus(1L, PaymentStatus.COMPLETED);
    }

    @Test
    void testGetTotalPaymentsByBusiness() throws Exception {
        BigDecimal total = new BigDecimal("100000.00");
        when(paymentService.getTotalPaymentsByBusiness(1L)).thenReturn(total);

        mockMvc.perform(get("/api/payments/businesses/{businessId}/total", 1L))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").value(100000.00));

        verify(paymentService, times(1)).getTotalPaymentsByBusiness(1L);
    }
}
