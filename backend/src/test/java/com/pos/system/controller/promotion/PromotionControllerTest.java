package com.pos.system.controller.promotion;

import com.pos.system.dto.promotion.PromotionRequest;
import com.pos.system.dto.promotion.PromotionResponse;
import com.pos.system.entity.promotion.Promotion.PromotionType;
import com.pos.system.service.promotion.PromotionService;
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
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class PromotionControllerTest {

    @Mock
    private PromotionService promotionService;

    @InjectMocks
    private PromotionController promotionController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(promotionController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void testCreatePromotion() throws Exception {
        PromotionRequest request = new PromotionRequest(
            "Descuento 20%", "Descuento de bienvenida", "PERCENTAGE",
            new BigDecimal("20.00"), true,
            LocalDateTime.now(), LocalDateTime.now().plusDays(30),
            100, "DESCUENTO20"
        );

        PromotionResponse response = new PromotionResponse();
        response.setId(1L);
        response.setName("Descuento 20%");
        response.setActive(true);

        when(promotionService.createPromotion(anyLong(), any(PromotionRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/promotions/businesses/{businessId}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Descuento 20%"));

        verify(promotionService, times(1)).createPromotion(anyLong(), any(PromotionRequest.class));
    }

    @Test
    void testGetPromotionById() throws Exception {
        PromotionResponse response = new PromotionResponse();
        response.setId(1L);
        response.setName("Descuento 20%");
        response.setActive(true);

        when(promotionService.getPromotionById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/promotions/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Descuento 20%"));

        verify(promotionService, times(1)).getPromotionById(1L);
    }

    @Test
    void testGetPromotionsByBusiness() throws Exception {
        PromotionResponse p1 = new PromotionResponse();
        p1.setId(1L);
        p1.setName("Descuento 20%");
        p1.setActive(true);

        PromotionResponse p2 = new PromotionResponse();
        p2.setId(2L);
        p2.setName("Descuento 50%");
        p2.setActive(true);

        List<PromotionResponse> promotions = Arrays.asList(p1, p2);

        when(promotionService.getPromotionsByBusiness(1L)).thenReturn(promotions);

        mockMvc.perform(get("/api/promotions/businesses/{businessId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[1].id").value(2L));

        verify(promotionService, times(1)).getPromotionsByBusiness(1L);
    }

    @Test
    void testGetActivePromotions() throws Exception {
        PromotionResponse p1 = new PromotionResponse();
        p1.setId(1L);
        p1.setName("Descuento 20%");
        p1.setActive(true);

        PromotionResponse p2 = new PromotionResponse();
        p2.setId(2L);
        p2.setName("Descuento 50%");
        p2.setActive(true);

        List<PromotionResponse> promotions = Arrays.asList(p1, p2);

        when(promotionService.getActivePromotions()).thenReturn(promotions);

        mockMvc.perform(get("/api/promotions/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[1].id").value(2L));

        verify(promotionService, times(1)).getActivePromotions();
    }

    @Test
    void testUpdatePromotion() throws Exception {
        PromotionRequest request = new PromotionRequest(
            "Descuento 50%", "Descuento especial", "PERCENTAGE",
            new BigDecimal("50.00"), true,
            LocalDateTime.now(), LocalDateTime.now().plusDays(30),
            50, "DESCUENTO50"
        );

        PromotionResponse response = new PromotionResponse();
        response.setId(1L);
        response.setName("Descuento 50%");
        response.setActive(true);

        when(promotionService.updatePromotion(anyLong(), any(PromotionRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/promotions/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Descuento 50%"));

        verify(promotionService, times(1)).updatePromotion(anyLong(), any(PromotionRequest.class));
    }

    @Test
    void testDeletePromotion() throws Exception {
        when(promotionService.deletePromotion(1L)).thenReturn(new PromotionResponse());

        mockMvc.perform(delete("/api/promotions/{id}", 1L))
                .andExpect(status().isNoContent());

        verify(promotionService, times(1)).deletePromotion(1L);
    }
}
