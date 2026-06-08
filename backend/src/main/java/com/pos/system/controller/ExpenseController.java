package com.pos.system.controller;

import com.pos.system.dto.request.ExpenseRequest;
import com.pos.system.dto.response.ExpenseResponse;
import com.pos.system.dto.response.ExpenseSummaryResponse;
import com.pos.system.entity.Expense;
import com.pos.system.entity.Expense.ExpenseCategory;
import com.pos.system.entity.Expense.ExpenseFrecuencia;
import com.pos.system.service.ExpenseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
@Tag(name = "Gastos", description = "CRUD de gastos con comprobantes adjuntos")
@SecurityRequirement(name = "bearerAuth")
public class ExpenseController {

    private final ExpenseService expenseService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    @Operation(summary = "Listar gastos con filtros")
    public ResponseEntity<List<ExpenseResponse>> getAll(
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Long proveedorId) {
        return ResponseEntity.ok(expenseService.getAll(categoria, estado, desde, hasta, proveedorId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    @Operation(summary = "Obtener gasto por ID")
    public ResponseEntity<ExpenseResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(expenseService.getById(id));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('GERENTE')")
    @Operation(summary = "Crear gasto con comprobante opcional")
    public ResponseEntity<ExpenseResponse> create(
            @RequestPart("data") @Valid ExpenseRequest request,
            @RequestPart(value = "comprobante", required = false) MultipartFile comprobante) {

        Expense expense = Expense.builder()
                .monto(request.monto())
                .fecha(request.fecha())
                .categoria(ExpenseCategory.valueOf(request.categoria()))
                .proveedorId(request.proveedorId())
                .descripcion(request.descripcion())
                .recurrente(request.isRecurrente())
                .frecuencia(request.isRecurrente() ? ExpenseFrecuencia.valueOf(request.frecuencia()) : null)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(expenseService.create(expense, comprobante));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('GERENTE')")
    @Operation(summary = "Actualizar gasto")
    public ResponseEntity<ExpenseResponse> update(
            @PathVariable Long id,
            @RequestPart("data") @Valid ExpenseRequest request,
            @RequestPart(value = "comprobante", required = false) MultipartFile comprobante) {

        Expense update = Expense.builder()
                .monto(request.monto())
                .fecha(request.fecha())
                .categoria(ExpenseCategory.valueOf(request.categoria()))
                .proveedorId(request.proveedorId())
                .descripcion(request.descripcion())
                .recurrente(request.isRecurrente())
                .frecuencia(request.isRecurrente() ? ExpenseFrecuencia.valueOf(request.frecuencia()) : null)
                .build();

        return ResponseEntity.ok(expenseService.update(id, update, comprobante));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('GERENTE')")
    @Operation(summary = "Eliminar gasto")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        expenseService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/pagar")
    @PreAuthorize("hasRole('GERENTE')")
    @Operation(summary = "Marcar gasto como pagado")
    public ResponseEntity<Void> marcarPagado(@PathVariable Long id) {
        expenseService.marcarPagado(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    @Operation(summary = "Resumen de gastos por categoría")
    public ResponseEntity<ExpenseSummaryResponse> getSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(expenseService.getSummary(desde, hasta));
    }
}
