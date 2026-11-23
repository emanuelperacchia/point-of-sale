package com.pos.system.controller.refund;

import com.pos.system.dto.refund.RefundRequest;
import com.pos.system.dto.refund.RefundResponse;
import com.pos.system.entity.refund.Refund.RefundStatus;
import com.pos.system.service.refund.RefundService;
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

public class RefundControllerTest {

    @Mock
    private RefundService refundService;

    @InjectMocks
    private RefundController refundController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(refundController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void testCreateRefund() throws Exception {
        RefundRequest request = new RefundRequest(
            1L, new BigDecimal("10000.00", "Producto defectuoso"
        );

        RefundResponse response = new RefundResponse();
        response.setId(1L);
        response.setReferenceNumber("REF-123");
        response.setAmount(new BigDecimal("10000.00"));
        response.setRefundStatus(RefundStatus.PENDING.name());

        when(refundService.createRefund(anyLong(), anyLong(), any(RefundRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/refunds/businesses/{businessId}/users/{userId}", 1L, 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.amount").value(10000.00));

        verify(refundService, times(1)).createRefund(anyLong(), anyLong(), any(RefundRequest.class));
    }

    @Test
    void testGetRefundById() throws Exception {
        RefundResponse response = new RefundResponse();
        response.setId(1L);
        response.setAmount(new BigDecimal("10000.00"));
        response.setRefundStatus(RefundStatus.PENDING.name());

        when(refundService.getRefundById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/refunds/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.amount").value(10000.00));

        verify(refundService, times(1)).getRefundById(1L);
    }

    @Test
    void testGetRefundsByBusiness() throws Exception {
        RefundResponse r1 = new RefundResponse();
        r1.setId(1L);
        r1.setAmount(new BigDecimal("10000.00"));
        r1.setRefundStatus(RefundStatus.PENDING.name());

        RefundResponse r2 = new RefundResponse();
        r2.setId(2L);
        r2.setAmount(new BigDecimal("20000.00"));
        r2.setRefundStatus(RefundStatus.COMPLETED.name());

        List<RefundResponse> refunds = Arrays.asList(r1, r2);

        when(refundService.getRefundsByBusiness(1L)).thenReturn(refunds);

        mockMvc.perform(get("/api/refunds/businesses/{businessId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[1].id").value(2L));

        verify(refundService, times(1)).getRefundsByBusiness(1L);
    }

    @Test
    void testUpdateRefundStatus() throws Exception {
        RefundResponse response = new RefundResponse();
        response.setId(1L);
        response.setRefundStatus(RefundStatus.APPROVED.name());

        when(refundService.updateRefundStatus(1L, RefundStatus.APPROVED)).thenReturn(response);

        mockMvc.perform(put("/api/refunds/{refundId}/status", 1L)
                .param("status", "APROBADO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.refundStatus").value("APROBADO"));

        verify(refundService, times(1)).updateRefundStatus(1L, RefundStatus.APPROVED);
    }
}
