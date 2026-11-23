package com.pos.system.controller.business;

import com.pos.system.dto.business.BusinessCreateRequest;
import com.pos.system.dto.business.BusinessResponse;
import com.pos.system.entity.business.Branch;
import com.pos.system.entity.business.Branch.BusinessType;
import com.pos.system.entity.business.Branch.BusinessStatus;
import com.pos.system.entity.business.Branch.SubscriptionPlan;
import com.pos.system.entity.business.Branch.SubscriptionStatus;
import com.pos.system.repository.business.BranchRepository;
import com.pos.system.service.business.BusinessService;
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
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class BusinessControllerTest {

    @Mock
    private BusinessService businessService;

    @InjectMocks
    private BusinessController businessController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(businessController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void testCreateBusiness() throws Exception {
        BusinessCreateRequest request = new BusinessCreateRequest(
            "Mi Negocio", "mi-negocio", "Calle 123", "12345678",
            "negocio@test.com", "12345678-9", "RETAIL", "BASIC",
            new BigDecimal("10000.00"), true
        );

        BusinessResponse response = new BusinessResponse();
        response.setId(1L);
        response.setBranchName("Mi Negocio");
        response.setSlug("mi-negocio");
        response.setActive(true);

        when(businessService.createBusiness(any(BusinessCreateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/businesses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.branchName").value("Mi Negocio"));

        verify(businessService, times(1)).createBusiness(any(BusinessCreateRequest.class));
    }

    @Test
    void testGetBusinessById() throws Exception {
        BusinessResponse response = new BusinessResponse();
        response.setId(1L);
        response.setBranchName("Mi Negocio");
        response.setActive(true);

        when(businessService.getBusinessById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/businesses/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.branchName").value("Mi Negocio"));

        verify(businessService, times(1)).getBusinessById(1L);
    }

    @Test
    void testGetAllBusinesses() throws Exception {
        BusinessResponse business1 = new BusinessResponse();
        business1.setId(1L);
        business1.setBranchName("Negocio 1");
        business1.setActive(true);

        BusinessResponse business2 = new BusinessResponse();
        business2.setId(2L);
        business2.setBranchName("Negocio 2");
        business2.setActive(true);

        List<BusinessResponse> businesses = Arrays.asList(business1, business2);

        when(businessService.getAllBusinesses()).thenReturn(businesses);

        mockMvc.perform(get("/api/businesses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[1].id").value(2L));

        verify(businessService, times(1)).getAllBusinesses();
    }

    @Test
    void testUpdateBusiness() throws Exception {
        BusinessCreateRequest request = new BusinessCreateRequest(
            "Negocio Actualizado", "negocio-actualizado", "Calle 456", "98765432",
            "nuevo@test.com", "98765432-1", "GERENTE", "PROFESSIONAL",
            new BigDecimal("20000.00"), true
        );

        BusinessResponse response = new BusinessResponse();
        response.setId(1L);
        response.setBranchName("Negocio Actualizado");
        response.setActive(true);

        when(businessService.updateBusiness(anyLong(), any(BusinessCreateRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/businesses/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.branchName").value("Negocio Actualizado"));

        verify(businessService, times(1)).updateBusiness(anyLong(), any(BusinessCreateRequest.class));
    }

    @Test
    void testDeleteBusiness() throws Exception {
        when(businessService.deleteBusiness(1L)).thenReturn(new BusinessResponse());

        mockMvc.perform(delete("/api/businesses/{id}", 1L))
                .andExpect(status().isNoContent());

        verify(businessService, times(1)).deleteBusiness(1L);
    }
}
