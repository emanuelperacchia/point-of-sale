package com.pos.system.service.impl;

import com.pos.system.dto.request.ClientRequest;
import com.pos.system.dto.response.ClientResponse;
import com.pos.system.entity.Client;
import com.pos.system.entity.CondicionIva;
import com.pos.system.exception.ResourceNotFoundException;
import com.pos.system.repository.ClientRepository;
import com.pos.system.service.ClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;

    @Override
    @Transactional
    public ClientResponse create(ClientRequest request) {
        Client client = Client.builder()
                .name(request.getName())
                .documentType(request.getDocumentType())
                .documentNumber(request.getDocumentNumber())
                .email(request.getEmail())
                .phone(request.getPhone())
                .address(request.getAddress())
                .businessName(request.getBusinessName())
                .condicionIva(request.getCondicionIva() != null
                        ? request.getCondicionIva()
                        : CondicionIva.CONSUMIDOR_FINAL)
                .taxAddress(request.getTaxAddress())
                .active(true)
                .build();

        return mapToResponse(clientRepository.save(client));
    }

    @Override
    @Transactional(readOnly = true)
    public ClientResponse getById(Long id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));
        return mapToResponse(client);
    }

    @Override
    @Transactional
    public ClientResponse update(Long id, ClientRequest request) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));

        client.setName(request.getName());
        client.setDocumentType(request.getDocumentType());
        client.setDocumentNumber(request.getDocumentNumber());
        client.setEmail(request.getEmail());
        client.setPhone(request.getPhone());
        client.setAddress(request.getAddress());
        client.setBusinessName(request.getBusinessName());
        if (request.getCondicionIva() != null) {
            client.setCondicionIva(request.getCondicionIva());
        }
        client.setTaxAddress(request.getTaxAddress());

        return mapToResponse(clientRepository.save(client));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClientResponse> search(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        return clientRepository.findByActiveTrueAndNameContainingIgnoreCase(query)
                .stream()
                .limit(5)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));
        client.setActive(false);
        clientRepository.save(client);
    }

    private ClientResponse mapToResponse(Client client) {
        return ClientResponse.builder()
                .id(client.getId())
                .name(client.getName())
                .documentType(client.getDocumentType())
                .documentNumber(client.getDocumentNumber())
                .email(client.getEmail())
                .phone(client.getPhone())
                .address(client.getAddress())
                .businessName(client.getBusinessName())
                .condicionIva(client.getCondicionIva())
                .taxAddress(client.getTaxAddress())
                .active(client.getActive())
                .createdAt(client.getCreatedAt())
                .build();
    }
}
