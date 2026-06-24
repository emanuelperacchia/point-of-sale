package com.pos.system.controller;

import com.pos.system.dto.request.CreateTransferRequest;
import com.pos.system.dto.request.ReceiveTransferRequest;
import com.pos.system.dto.response.StockTransferResponse;
import com.pos.system.entity.StockTransfer;
import com.pos.system.security.UserDetailsImpl;
import com.pos.system.service.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
@Tag(name = "Transferencias", description = "Transferencias de stock entre sucursales")
public class TransferController {

    private final TransferService transferService;

    @PostMapping
    @Operation(summary = "Crear solicitud de transferencia")
    public ResponseEntity<StockTransferResponse> crear(
            @Valid @RequestBody CreateTransferRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(transferService.crear(request, userDetails.getId()));
    }

    @PostMapping("/{id}/dispatch")
    @Operation(summary = "Despachar transferencia (origen confirma envío)")
    public ResponseEntity<StockTransferResponse> despachar(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(transferService.despachar(id, userDetails.getId()));
    }

    @PostMapping("/{id}/receive")
    @Operation(summary = "Recibir transferencia (destino confirma recepción)")
    public ResponseEntity<StockTransferResponse> recibir(
            @PathVariable Long id,
            @Valid @RequestBody ReceiveTransferRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(transferService.recibir(id, request, userDetails.getId()));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancelar transferencia (solo en estado SOLICITADA)")
    public ResponseEntity<StockTransferResponse> cancelar(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(transferService.cancelar(id, userDetails.getId()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener transferencia por ID")
    public ResponseEntity<StockTransferResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(transferService.obtener(id));
    }

    @GetMapping
    @Operation(summary = "Listar transferencias con filtros")
    public ResponseEntity<List<StockTransferResponse>> listar(
            @RequestParam(required = false) Long sucursalOrigenId,
            @RequestParam(required = false) Long sucursalDestinoId,
            @RequestParam(required = false) StockTransfer.Estado estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta) {
        return ResponseEntity.ok(transferService.listar(sucursalOrigenId, sucursalDestinoId, estado, desde, hasta));
    }
}
