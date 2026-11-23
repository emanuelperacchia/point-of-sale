package com.pos.system.controller.legal;

import com.pos.system.dto.legal.LegalHoldRequest;
import com.pos.system.dto.legal.LegalHoldResponse;
import com.pos.system.service.legal.LegalHoldService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class LegalHoldControllerTest {

    @Mock
    private LegalHoldService legalHoldService;

    @InjectMocks
    private LegalHoldController legalHoldController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(legalHoldController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void testCreateLegalHold() throws Exception {
        LegalHoldRequest request = new LegalHoldRequest(
            "BLOQUEO_DATOS", "Solicitud de baja de usuario",
            "Ley 25.326 Artículo 16", true,
            LocalDateTime.now(), LocalDateTime.now().plusDays(30),
            null, 5
        );

        LegalHoldResponse response = new LegalHoldResponse();
        response.setId(1L);
        response.setHoldType("BLOQUEO_DATOS");
        response.setActive(true);

        when(legalHoldService.createLegalHold(anyLong(), anyLong(), any(LegalHoldRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/legal-holds/businesses/{businessId}/users/{userId}", 1L, 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.holdType").value("BLOQUEO_DATOS"));

        verify(legalHoldService, times(1)).createLegalHold(anyLong(), anyLong(), any(LegalHoldRequest.class));
    }

    @Test
    void testGetLegalHoldById() throws Exception {
        LegalHoldResponse response = new LegalHoldResponse();
        response.setId(1L);
        response.setHoldType("BLOQUEO_DATOS");
        response.setActive(true);

        when(legalHoldService.getLegalHoldById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/legal-holds/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.holdType").value("BLOQUEO_DATOS"));

        verify(legalHoldService, times(1)).getLegalHoldById(1L);
    }

    @Test
    void testGetLegalHoldsByBusiness() throws Exception {
        LegalHoldResponse h1 = new LegalHoldResponse();
        h1.setId(1L);
        h1.setHoldType("BLOQUEO_DATOS");
        h1.setActive(true);

        LegalHoldResponse h2 = new LegalHoldResponse();
        h2.setId(2L);
        h2.setHoldType("SUSPENSION_CUENTA");
        h2.setActive(true);

        List<LegalHoldResponse> legalHolds = Arrays.asList(h1, h2);

        when(legalHoldService.getLegalHoldsByBusiness(1L)).thenReturn(legalHolds);

        mockMvc.perform(get("/api/legal-holds/businesses/{businessId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[1].id").value(2L));

        verify(legalHoldService, times(1)).getLegalHoldsByBusiness(1L);
    }

    @Test
    void testUpdateLegalHold() throws Exception {
        LegalHoldRequest request = new LegalHoldRequest(
            "SUSPENSION_CUENTA", "Solicitud de suspensión temporal",
            "Ley 25.326 Artículo 16", true,
            LocalDateTime.now(), LocalDateTime.now().plusDays(30),
            null, 5
        );

        LegalHoldResponse response = new LegalHoldResponse();
        response.setId(1L);
        response.setHoldType("SUSPENSION_CUENTA");
        response.setActive(true);

        when(legalHoldService.updateLegalHold(anyLong(), any(LegalHoldRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/legal-holds/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.holdType").value("SUSPENSION_CUENTA"));

        verify(legalHoldService, times(1)).updateLegalHold(anyLong(), any(LegalHoldRequest.class));
    }

    @Test
    void testResolveLegalHold() throws Exception {
        LegalHoldResponse response = new LegalHoldResponse();
        response.setId(1L);
        response.setResolvedAt(java.time.LocalDateTime.now());

        when(legalHoldService.resolveLegalHold(1L)).thenReturn(response);

        mockMvc.perform(put("/api/legal-holds/{id}/resolve", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.resolvedAt").isNotEmpty());

        verify(legalHoldService, times(1)).resolveLegalHold(1L);
    }
}
