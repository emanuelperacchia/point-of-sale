package com.pos.system.controller;

import com.pos.system.dto.request.ClientRequest;
import com.pos.system.dto.response.ClientResponse;
import com.pos.system.dto.response.PointsResponse;
import com.pos.system.entity.*;
import com.pos.system.repository.PointsTransactionRepository;
import com.pos.system.service.ClientService;
import com.pos.system.service.LoyaltyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
@Tag(name = "Clientes", description = "Gestión de clientes del POS")
@SecurityRequirement(name = "bearerAuth")
public class ClientController {

    private final ClientService clientService;
    private final LoyaltyService loyaltyService;
    private final PointsTransactionRepository pointsTransactionRepository;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'CAJERO')")
    @Operation(summary = "Crear cliente", description = "Registra un nuevo cliente en el sistema")
    public ResponseEntity<ClientResponse> create(@Valid @RequestBody ClientRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clientService.create(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'CAJERO', 'VENDEDOR')")
    @Operation(summary = "Obtener cliente", description = "Obtiene un cliente por su ID")
    public ResponseEntity<ClientResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(clientService.getById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    @Operation(summary = "Actualizar cliente", description = "Actualiza los datos de un cliente")
    public ResponseEntity<ClientResponse> update(@PathVariable Long id, @Valid @RequestBody ClientRequest request) {
        return ResponseEntity.ok(clientService.update(id, request));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'CAJERO', 'VENDEDOR')")
    @Operation(summary = "Buscar clientes", description = "Busca clientes por nombre, documento o email")
    public ResponseEntity<List<ClientResponse>> search(@RequestParam String q) {
        return ResponseEntity.ok(clientService.search(q));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Eliminar cliente", description = "Elimina un cliente (soft delete)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        clientService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ── Puntos de fidelización ────────────────────────────────

    @GetMapping("/{id}/points")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'CAJERO', 'VENDEDOR')")
    @Operation(summary = "Obtener puntos", description = "Saldo de puntos, tier e historial del cliente")
    public ResponseEntity<PointsResponse> getPoints(@PathVariable Long id,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "20") int size) {
        Client client = loyaltyService.getClientPointsInfo(id);
        Page<PointsTransaction> txPage = pointsTransactionRepository
                .findByClientIdOrderByFechaDesc(id, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "fecha")));

        List<PointsResponse.PointsTransactionItem> historial = txPage.getContent().stream()
                .map(tx -> PointsResponse.PointsTransactionItem.builder()
                        .id(tx.getId())
                        .saleId(tx.getSaleId())
                        .tipo(tx.getTipo().name())
                        .puntos(tx.getPuntos())
                        .saldoPrevio(tx.getSaldoPrevio())
                        .saldoPosterior(tx.getSaldoPosterior())
                        .descripcion(tx.getDescripcion())
                        .fecha(tx.getFecha() != null ? tx.getFecha().toString() : null)
                        .build())
                .collect(Collectors.toList());

        PointsResponse response = PointsResponse.builder()
                .clientId(client.getId())
                .clientName(client.getName())
                .saldoActual(client.getPuntosAcumulados())
                .tier(client.getTier())
                .historial(historial)
                .build();

        return ResponseEntity.ok(response);
    }
}
