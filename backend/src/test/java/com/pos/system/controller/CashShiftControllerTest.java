package com.pos.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pos.system.dto.response.ShiftMovementResponse;
import com.pos.system.dto.response.ShiftReportResponse;
import com.pos.system.dto.response.ShiftResponse;
import com.pos.system.entity.*;
import com.pos.system.exception.BadRequestException;
import com.pos.system.exception.GlobalExceptionHandler;
import com.pos.system.exception.ResourceNotFoundException;
import com.pos.system.security.UserDetailsImpl;
import com.pos.system.service.CashShiftService;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CashShiftControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private CashShiftService cashShiftService;
    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();
    private UserDetailsImpl userDetails;

    @BeforeEach
    void setUp() {
        cashShiftService = mock(CashShiftService.class);
        objectMapper = new ObjectMapper();

        mockMvc = MockMvcBuilders.standaloneSetup(new CashShiftController(cashShiftService))
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
    void openShift_Success() throws Exception {
        ShiftResponse response = ShiftResponse.builder()
                .id(1L).cajeroId(1L).cajeroNombre("Admin User")
                .sucursalId(1L).estado(ShiftStatus.ABIERTO)
                .montoApertura(BigDecimal.valueOf(50000))
                .fechaApertura(LocalDateTime.now())
                .build();

        when(cashShiftService.openShift(eq(1L), eq(1L), any(BigDecimal.class)))
                .thenReturn(response);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("sucursalId", 1);
        body.put("montoApertura", 50000);

        mockMvc.perform(post("/api/shifts/open")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.estado").value("ABIERTO"))
                .andExpect(jsonPath("$.cajeroId").value(1));
    }

    @Test
    void openShift_WhenConflict_ShouldReturn409() throws Exception {
        when(cashShiftService.openShift(eq(1L), eq(1L), any(BigDecimal.class)))
                .thenThrow(new BadRequestException("El cajero ya tiene un turno abierto"));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("sucursalId", 1);
        body.put("montoApertura", 50000);

        mockMvc.perform(post("/api/shifts/open")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void closeShift_Success() throws Exception {
        ShiftResponse response = ShiftResponse.builder()
                .id(1L).estado(ShiftStatus.CERRADO)
                .montoApertura(BigDecimal.valueOf(50000))
                .montoCierre(BigDecimal.valueOf(80000))
                .diferencia(BigDecimal.ZERO)
                .fechaApertura(LocalDateTime.now())
                .fechaCierre(LocalDateTime.now())
                .build();

        when(cashShiftService.closeShift(1L, BigDecimal.valueOf(80000)))
                .thenReturn(response);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("montoCierre", 80000);

        mockMvc.perform(post("/api/shifts/1/close")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CERRADO"))
                .andExpect(jsonPath("$.diferencia").value(0));
    }

    @Test
    void addMovement_Success() throws Exception {
        ShiftMovementResponse movement = ShiftMovementResponse.builder()
                .id(1L).shiftId(1L).tipo(ShiftMovementType.INGRESO)
                .monto(BigDecimal.valueOf(10000)).motivo("Pago proveedor")
                .usuarioNombre("Admin User")
                .createdAt(LocalDateTime.now())
                .build();

        when(cashShiftService.addMovement(eq(1L), eq("INGRESO"),
                eq(BigDecimal.valueOf(10000)), eq("Pago proveedor"), eq(1L)))
                .thenReturn(movement);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("tipo", "INGRESO");
        body.put("monto", 10000);
        body.put("motivo", "Pago proveedor");

        mockMvc.perform(post("/api/shifts/1/movements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("INGRESO"))
                .andExpect(jsonPath("$.monto").value(10000));
    }

    @Test
    void getReport_Success() throws Exception {
        ShiftReportResponse report = ShiftReportResponse.builder()
                .shiftId(1L).estado(ShiftStatus.CERRADO)
                .montoApertura(BigDecimal.valueOf(50000))
                .totalVentasEfectivo(BigDecimal.valueOf(30000))
                .ventasPorMetodoPago(Map.of("CASH", BigDecimal.valueOf(30000)))
                .totalIngresos(BigDecimal.ZERO)
                .totalRetiros(BigDecimal.ZERO)
                .movimientos(List.of())
                .montoEsperado(BigDecimal.valueOf(80000))
                .montoCierreDeclarado(BigDecimal.valueOf(80000))
                .diferencia(BigDecimal.ZERO)
                .fechaApertura(LocalDateTime.now())
                .fechaCierre(LocalDateTime.now())
                .build();

        when(cashShiftService.getReport(1L)).thenReturn(report);

        mockMvc.perform(get("/api/shifts/1/report"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CERRADO"))
                .andExpect(jsonPath("$.totalVentasEfectivo").value(30000))
                .andExpect(jsonPath("$.diferencia").value(0));
    }

    @Test
    void getById_Success() throws Exception {
        ShiftResponse response = ShiftResponse.builder()
                .id(1L).estado(ShiftStatus.ABIERTO)
                .montoApertura(BigDecimal.valueOf(50000))
                .fechaApertura(LocalDateTime.now())
                .build();

        when(cashShiftService.getById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/shifts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.estado").value("ABIERTO"));
    }

    @Test
    void getById_WhenNotFound_ShouldReturn404() throws Exception {
        when(cashShiftService.getById(99L))
                .thenThrow(new ResourceNotFoundException("Turno no encontrado"));

        mockMvc.perform(get("/api/shifts/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void findByFilters_Success() throws Exception {
        ShiftResponse response = ShiftResponse.builder()
                .id(1L).estado(ShiftStatus.CERRADO)
                .montoApertura(BigDecimal.valueOf(50000))
                .fechaApertura(LocalDateTime.now())
                .build();

        when(cashShiftService.findByFilters(1L, ShiftStatus.CERRADO))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/shifts")
                        .param("cajeroId", "1")
                        .param("estado", "CERRADO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].estado").value("CERRADO"));
    }
}
