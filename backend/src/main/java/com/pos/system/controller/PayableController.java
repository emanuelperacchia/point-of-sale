package com.pos.system.controller;

import com.pos.system.dto.request.PayablePaymentRequest;
import com.pos.system.dto.request.PayableRequest;
import com.pos.system.dto.response.PayablePaymentResponse;
import com.pos.system.dto.response.PayableResponse;
import com.pos.system.security.UserDetailsImpl;
import com.pos.system.service.PayableService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payables")
@RequiredArgsConstructor
public class PayableController {

    private final PayableService payableService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','CONTADOR')")
    public ResponseEntity<Page<PayableResponse>> list(
            @RequestParam(required = false) Long proveedorId,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String fechaVencimiento,
            Pageable pageable) {
        return ResponseEntity.ok(payableService.findByFilters(proveedorId, estado, fechaVencimiento, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','CONTADOR')")
    public ResponseEntity<PayableResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(payableService.getById(id));
    }

    @GetMapping("/{id}/payments")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','CONTADOR')")
    public ResponseEntity<List<PayablePaymentResponse>> getPayments(@PathVariable Long id) {
        return ResponseEntity.ok(payableService.getPayments(id));
    }

    @GetMapping("/upcoming")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','CONTADOR')")
    public ResponseEntity<List<PayableResponse>> getUpcoming(
            @RequestParam(defaultValue = "30") int dias) {
        return ResponseEntity.ok(payableService.getUpcoming(dias));
    }

    @PostMapping
    @PreAuthorize("hasRole('GERENTE')")
    public ResponseEntity<PayableResponse> create(@Valid @RequestBody PayableRequest request) {
        return ResponseEntity.ok(payableService.createPayable(request));
    }

    @PostMapping("/{id}/payments")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','CONTADOR')")
    public ResponseEntity<PayableResponse> registerPayment(
            @PathVariable Long id,
            @Valid @RequestBody PayablePaymentRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(payableService.registrarPago(id, request, userDetails.getUser().getId()));
    }
}
