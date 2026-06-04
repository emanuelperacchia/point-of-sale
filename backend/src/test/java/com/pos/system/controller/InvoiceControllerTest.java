package com.pos.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pos.system.dto.response.InvoiceResponse;
import com.pos.system.entity.*;
import com.pos.system.exception.GlobalExceptionHandler;
import com.pos.system.exception.ResourceNotFoundException;
import com.pos.system.security.UserDetailsImpl;
import com.pos.system.service.InvoiceService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class InvoiceControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private InvoiceService invoiceService;
    private InvoiceController invoiceController;
    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();
    private UserDetailsImpl userDetails;

    @BeforeEach
    void setUp() {
        invoiceService = mock(InvoiceService.class);
        invoiceController = new InvoiceController(invoiceService);
        objectMapper = new ObjectMapper();

        mockMvc = MockMvcBuilders.standaloneSetup(invoiceController)
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

    private InvoiceResponse createSampleResponse(Long id) {
        return InvoiceResponse.builder()
                .id(id)
                .saleId(1L)
                .tipoComprobante(TipoComprobante.BOLETA)
                .puntoVenta(1)
                .numero(1L)
                .cae("12345678901")
                .estado(InvoiceStatus.EMITIDO)
                .receptorNombre("Juan Pérez")
                .receptorDocumento("12.345.678")
                .receptorCondicionIva(CondicionIva.CONSUMIDOR_FINAL)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void getAll_ShouldReturn200() throws Exception {
        when(invoiceService.findByFilters(null, null, null, null, null))
                .thenReturn(List.of(createSampleResponse(1L)));

        mockMvc.perform(get("/api/invoices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].tipoComprobante").value("BOLETA"))
                .andExpect(jsonPath("$[0].estado").value("EMITIDO"));
    }

    @Test
    void getAll_WithFilters_ShouldPassFilters() throws Exception {
        mockMvc.perform(get("/api/invoices")
                        .param("tipo", "BOLETA")
                        .param("estado", "EMITIDO"))
                .andExpect(status().isOk());

        verify(invoiceService).findByFilters(TipoComprobante.BOLETA, InvoiceStatus.EMITIDO, null, null, null);
    }

    @Test
    void getById_ShouldReturn200() throws Exception {
        when(invoiceService.getById(1L)).thenReturn(createSampleResponse(1L));

        mockMvc.perform(get("/api/invoices/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.cae").value("12345678901"));
    }

    @Test
    void getById_WhenNotFound_ShouldReturn404() throws Exception {
        when(invoiceService.getById(99L)).thenThrow(new ResourceNotFoundException("Comprobante no encontrado"));

        mockMvc.perform(get("/api/invoices/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void retryEmission_ShouldReturn200() throws Exception {
        when(invoiceService.retryEmission(1L)).thenReturn(createSampleResponse(1L));

        mockMvc.perform(post("/api/invoices/1/retry"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void downloadPdf_ShouldReturn200() throws Exception {
        ByteArrayResource pdfResource = new ByteArrayResource("fake-pdf-content".getBytes());
        when(invoiceService.getPdf(1L)).thenReturn(pdfResource);

        mockMvc.perform(get("/api/invoices/1/pdf"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().exists("Content-Disposition"));
    }

    @Test
    void downloadPdf_WhenNotFound_ShouldReturn404() throws Exception {
        when(invoiceService.getPdf(99L)).thenThrow(new ResourceNotFoundException("PDF no encontrado"));

        mockMvc.perform(get("/api/invoices/99/pdf"))
                .andExpect(status().isNotFound());
    }
}
