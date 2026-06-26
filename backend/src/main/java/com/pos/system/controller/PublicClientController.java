package com.pos.system.controller;

import com.pos.system.entity.Client;
import com.pos.system.repository.ClientRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/public/v1/clients")
@RequiredArgsConstructor
@Tag(name = "Public API - Clients", description = "Clientes via API pública")
public class PublicClientController {

    private final ClientRepository clientRepository;

    @GetMapping("/{document}")
    @Operation(summary = "Buscar cliente por número de documento")
    public ResponseEntity<ClientResponse> findByDocument(@PathVariable String document) {
        return clientRepository.findByDocumentNumber(document)
                .map(client -> ResponseEntity.ok(new ClientResponse(
                        client.getId(),
                        client.getName(),
                        client.getDocumentNumber(),
                        client.getEmail(),
                        client.getPhone()
                )))
                .orElse(ResponseEntity.notFound().build());
    }

    record ClientResponse(Long id, String name, String documentNumber, String email, String phone) {}
}
