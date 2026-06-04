package com.pos.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pos.system.dto.request.CreateReturnRequest;
import com.pos.system.dto.response.ReturnItemResponse;
import com.pos.system.dto.response.SaleReturnResponse;
import com.pos.system.entity.*;
import com.pos.system.exception.BadRequestException;
import com.pos.system.exception.GlobalExceptionHandler;
import com.pos.system.exception.ResourceNotFoundException;
import com.pos.system.security.UserDetailsImpl;
import com.pos.system.service.SaleReturnService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SaleReturnControllerTest {

    private final SaleReturnService saleReturnService = mock(SaleReturnService.class);
    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private MockMvc mockMvc;
    private UserDetailsImpl userDetails;

    @BeforeEach
    void setUp() {
        SaleReturnController controller = new SaleReturnController(saleReturnService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(exceptionHandler)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
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

    private SaleReturnResponse sampleResponse(ReturnStatus status) {
        return new SaleReturnResponse(
                1L, 1L, status, "Producto defectuoso", null,
                BigDecimal.valueOf(2000), PaymentMethod.CASH, null, null,
                List.of(new ReturnItemResponse(1L, 1L, 2, BigDecimal.valueOf(1000), BigDecimal.valueOf(2000))),
                LocalDateTime.now()
        );
    }

    @Test
    void createReturn_ShouldReturn201() throws Exception {
        CreateReturnRequest request = new CreateReturnRequest();
        request.setSaleId(1L);
        request.setMotivo("Defectuoso");
        CreateReturnRequest.ReturnItemRequest item = new CreateReturnRequest.ReturnItemRequest();
        item.setSaleItemId(1L);
        item.setCantidad(2);
        request.setItems(List.of(item));

        when(saleReturnService.createReturn(any(), eq(1L)))
                .thenReturn(sampleResponse(ReturnStatus.APROBADA));

        mockMvc.perform(post("/api/returns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.estado").value("APROBADA"))
                .andExpect(jsonPath("$.montoTotal").value(2000))
                .andExpect(jsonPath("$.items.length()").value(1));
    }

    @Test
    void createReturn_WhenBadRequest_ShouldReturn400() throws Exception {
        CreateReturnRequest request = new CreateReturnRequest();
        request.setSaleId(1L);
        request.setMotivo("Excede cantidad");
        CreateReturnRequest.ReturnItemRequest item = new CreateReturnRequest.ReturnItemRequest();
        item.setSaleItemId(1L);
        item.setCantidad(99);
        request.setItems(List.of(item));

        when(saleReturnService.createReturn(any(), eq(1L)))
                .thenThrow(new BadRequestException("Cantidad máxima a devolver: 5"));

        mockMvc.perform(post("/api/returns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void approveReturn_ShouldReturn200() throws Exception {
        when(saleReturnService.approveReturn(1L, 1L))
                .thenReturn(sampleResponse(ReturnStatus.APROBADA));

        mockMvc.perform(post("/api/returns/{id}/approve", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("APROBADA"));
    }

    @Test
    void approveReturn_WhenNotFound_ShouldReturn404() throws Exception {
        when(saleReturnService.approveReturn(99L, 1L))
                .thenThrow(new ResourceNotFoundException("Devolución no encontrada"));

        mockMvc.perform(post("/api/returns/{id}/approve", 99L))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectReturn_ShouldReturn200() throws Exception {
        when(saleReturnService.rejectReturn(1L, 1L))
                .thenReturn(sampleResponse(ReturnStatus.RECHAZADA));

        mockMvc.perform(post("/api/returns/{id}/reject", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RECHAZADA"));
    }

    @Test
    void getById_ShouldReturn200() throws Exception {
        when(saleReturnService.getById(1L))
                .thenReturn(sampleResponse(ReturnStatus.APROBADA));

        mockMvc.perform(get("/api/returns/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void findBySaleId_ShouldReturn200() throws Exception {
        when(saleReturnService.findBySaleId(1L))
                .thenReturn(List.of(sampleResponse(ReturnStatus.APROBADA)));

        mockMvc.perform(get("/api/returns")
                        .param("saleId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getById_WhenNotFound_ShouldReturn404() throws Exception {
        when(saleReturnService.getById(99L))
                .thenThrow(new ResourceNotFoundException("Devolución no encontrada"));

        mockMvc.perform(get("/api/returns/{id}", 99L))
                .andExpect(status().isNotFound());
    }
}
