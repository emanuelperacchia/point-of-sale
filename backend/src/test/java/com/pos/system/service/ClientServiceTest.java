package com.pos.system.service;

import com.pos.system.dto.request.ClientRequest;
import com.pos.system.dto.response.ClientResponse;
import com.pos.system.entity.Client;
import com.pos.system.exception.ResourceNotFoundException;
import com.pos.system.repository.ClientRepository;
import com.pos.system.service.impl.ClientServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock private ClientRepository clientRepository;

    private ClientService clientService;

    private Client client;
    private ClientRequest request;

    @BeforeEach
    void setUp() {
        clientService = new ClientServiceImpl(clientRepository);

        client = Client.builder()
                .id(1L)
                .name("Juan Pérez")
                .documentType("RUT")
                .documentNumber("12.345.678-9")
                .email("juan@email.com")
                .phone("099123456")
                .address("Av. Siempre Viva 123")
                .active(true)
                .build();

        request = new ClientRequest();
        request.setName("Juan Pérez");
        request.setDocumentType("RUT");
        request.setDocumentNumber("12.345.678-9");
        request.setEmail("juan@email.com");
        request.setPhone("099123456");
        request.setAddress("Av. Siempre Viva 123");
    }

    @Test
    void create_ShouldReturnCreatedClient() {
        when(clientRepository.save(any(Client.class))).thenReturn(client);

        ClientResponse result = clientService.create(request);

        assertNotNull(result);
        assertEquals("Juan Pérez", result.getName());
        assertEquals("12.345.678-9", result.getDocumentNumber());
        assertEquals("juan@email.com", result.getEmail());
        verify(clientRepository).save(any(Client.class));
    }

    @Test
    void getById_WhenClientExists_ShouldReturnClient() {
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));

        ClientResponse result = clientService.getById(1L);

        assertEquals("Juan Pérez", result.getName());
        verify(clientRepository).findById(1L);
    }

    @Test
    void getById_WhenClientNotFound_ShouldThrowException() {
        when(clientRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> clientService.getById(99L));
    }

    @Test
    void update_ShouldModifyAndReturnClient() {
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(clientRepository.save(any(Client.class))).thenReturn(client);

        ClientRequest updateReq = new ClientRequest();
        updateReq.setName("Juan Updated");
        updateReq.setDocumentType("RUT");
        updateReq.setDocumentNumber("98.765.432-1");
        updateReq.setEmail("juan2@email.com");

        ClientResponse result = clientService.update(1L, updateReq);

        assertEquals("Juan Updated", result.getName());
        verify(clientRepository).save(any(Client.class));
    }

    @Test
    void search_WithValidQuery_ShouldReturnResults() {
        when(clientRepository.findByActiveTrueAndNameContainingIgnoreCase("Juan"))
                .thenReturn(List.of(client));

        List<ClientResponse> results = clientService.search("Juan");

        assertEquals(1, results.size());
        assertEquals("Juan Pérez", results.get(0).getName());
    }

    @Test
    void search_WithEmptyQuery_ShouldReturnEmptyList() {
        List<ClientResponse> results = clientService.search("");

        assertTrue(results.isEmpty());
        verifyNoInteractions(clientRepository);
    }

    @Test
    void search_WithNullQuery_ShouldReturnEmptyList() {
        List<ClientResponse> results = clientService.search(null);

        assertTrue(results.isEmpty());
        verifyNoInteractions(clientRepository);
    }

    @Test
    void delete_ShouldSoftDelete() {
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));

        clientService.delete(1L);

        assertFalse(client.getActive());
        verify(clientRepository).save(client);
    }

    @Test
    void delete_WhenClientNotFound_ShouldThrowException() {
        when(clientRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> clientService.delete(99L));
    }
}
