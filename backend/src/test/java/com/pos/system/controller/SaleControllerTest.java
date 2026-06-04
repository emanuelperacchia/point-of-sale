package com.pos.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pos.system.dto.request.CartValidationRequest;
import com.pos.system.dto.request.SaleRequest;
import com.pos.system.dto.response.CartValidationResponse;
import com.pos.system.dto.response.SaleResponse;
import com.pos.system.entity.User;
import com.pos.system.exception.GlobalExceptionHandler;
import com.pos.system.exception.ResourceNotFoundException;
import com.pos.system.security.UserDetailsImpl;
import com.pos.system.service.SaleService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SaleControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private SaleService saleService;
    private SaleController saleController;
    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

    private UserDetailsImpl userDetails;

    @BeforeEach
    void setUp() {
        saleService = mock(SaleService.class);
        saleController = new SaleController(saleService);
        objectMapper = new ObjectMapper();

        mockMvc = MockMvcBuilders.standaloneSetup(saleController)
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

    @Test
    void processSale_ShouldReturn201() throws Exception {
        SaleRequest request = buildSaleRequest();
        SaleResponse response = SaleResponse.builder()
                .id(1L).status("COMPLETED")
                .subtotal(BigDecimal.valueOf(3000))
                .total(BigDecimal.valueOf(3000))
                .build();

        when(saleService.processSale(any(SaleRequest.class), eq(1L))).thenReturn(response);

        mockMvc.perform(post("/api/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void validateCart_ShouldReturn200() throws Exception {
        CartValidationRequest request = new CartValidationRequest();
        request.setWarehouseId(1L);

        CartValidationRequest.CartItem item = new CartValidationRequest.CartItem();
        item.setProductId(1L);
        item.setQuantity(2);
        request.setItems(List.of(item));

        CartValidationResponse response = CartValidationResponse.builder()
                .valid(true)
                .items(List.of(
                        CartValidationResponse.ItemStatus.builder()
                                .productId(1L).productName("Producto")
                                .requested(2).available(50).enoughStock(true)
                                .message("Stock suficiente").build()
                ))
                .build();

        when(saleService.validateCart(any(CartValidationRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/sales/validate-cart")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));
    }

    @Test
    void getById_ShouldReturn200() throws Exception {
        SaleResponse response = SaleResponse.builder()
                .id(1L).status("COMPLETED")
                .total(BigDecimal.valueOf(3000))
                .build();

        when(saleService.getById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/sales/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getById_WhenNotFound_ShouldReturn404() throws Exception {
        when(saleService.getById(99L)).thenThrow(new ResourceNotFoundException("Venta no encontrada"));

        mockMvc.perform(get("/api/sales/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getMySales_ShouldReturn200() throws Exception {
        Page<SaleResponse> page = new PageImpl<>(
                List.of(SaleResponse.builder().id(1L).status("COMPLETED").build()),
                PageRequest.of(0, 20), 1
        );

        when(saleService.getSalesByUser(eq(1L), any())).thenReturn(page);

        mockMvc.perform(get("/api/sales"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1));
    }

    private SaleRequest buildSaleRequest() {
        SaleRequest request = new SaleRequest();

        SaleRequest.SaleItemRequest item = new SaleRequest.SaleItemRequest();
        item.setProductId(1L);
        item.setQuantity(2);
        item.setDiscount(BigDecimal.ZERO);
        request.setItems(List.of(item));

        SaleRequest.PaymentRequest payment = new SaleRequest.PaymentRequest();
        payment.setPaymentMethod("CASH");
        payment.setAmount(BigDecimal.valueOf(3000));
        request.setPayments(List.of(payment));

        return request;
    }
}
