package com.pos.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pos.system.dto.request.ClientRequest;
import com.pos.system.dto.response.ClientResponse;
import com.pos.system.exception.GlobalExceptionHandler;
import com.pos.system.exception.ResourceNotFoundException;
import com.pos.system.repository.PointsTransactionRepository;
import com.pos.system.service.ClientService;
import com.pos.system.service.LoyaltyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ClientControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private ClientService clientService;
    private LoyaltyService loyaltyService;
    private PointsTransactionRepository pointsTransactionRepository;
    private ClientController clientController;
    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

    @BeforeEach
    void setUp() {
        clientService = mock(ClientService.class);
        loyaltyService = mock(LoyaltyService.class);
        pointsTransactionRepository = mock(PointsTransactionRepository.class);
        clientController = new ClientController(clientService, loyaltyService, pointsTransactionRepository);
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(clientController)
                .setControllerAdvice(exceptionHandler)
                .build();
    }

    @Test
    void create_ShouldReturn201() throws Exception {
        ClientRequest request = new ClientRequest();
        request.setName("Juan Pérez");
        request.setDocumentType("RUT");
        request.setDocumentNumber("12.345.678-9");

        ClientResponse response = ClientResponse.builder()
                .id(1L).name("Juan Pérez")
                .documentType("RUT").documentNumber("12.345.678-9")
                .build();

        when(clientService.create(any(ClientRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Juan Pérez"));
    }

    @Test
    void getById_ShouldReturn200() throws Exception {
        ClientResponse response = ClientResponse.builder()
                .id(1L).name("Juan Pérez").build();

        when(clientService.getById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/clients/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Juan Pérez"));
    }

    @Test
    void getById_WhenNotFound_ShouldReturn404() throws Exception {
        when(clientService.getById(99L)).thenThrow(new ResourceNotFoundException("Cliente no encontrado"));

        mockMvc.perform(get("/api/clients/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void search_ShouldReturn200() throws Exception {
        ClientResponse response = ClientResponse.builder()
                .id(1L).name("Juan Pérez").build();

        when(clientService.search("Juan")).thenReturn(List.of(response));

        mockMvc.perform(get("/api/clients/search").param("q", "Juan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Juan Pérez"));
    }

    @Test
    void update_ShouldReturn200() throws Exception {
        ClientRequest request = new ClientRequest();
        request.setName("Juan Updated");

        ClientResponse response = ClientResponse.builder()
                .id(1L).name("Juan Updated").build();

        when(clientService.update(anyLong(), any(ClientRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/clients/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Juan Updated"));
    }

    @Test
    void delete_ShouldReturn204() throws Exception {
        doNothing().when(clientService).delete(1L);

        mockMvc.perform(delete("/api/clients/1"))
                .andExpect(status().isNoContent());
    }
}
