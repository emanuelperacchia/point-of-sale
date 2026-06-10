package com.pos.system.controller;

import com.pos.system.dto.request.EmployeeRequest;
import com.pos.system.dto.response.EmployeeHistoryResponse;
import com.pos.system.dto.response.EmployeeResponse;
import com.pos.system.security.UserDetailsImpl;
import com.pos.system.service.EmployeeService;
import com.pos.system.service.FileStorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;
    private final FileStorageService fileStorageService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public ResponseEntity<List<EmployeeResponse>> list(
            @RequestParam(required = false) String departamento,
            @RequestParam(required = false) String cargo,
            @RequestParam(required = false) Long sucursalId,
            @RequestParam(required = false) Boolean activo) {
        return ResponseEntity.ok(employeeService.findByFilters(departamento, cargo, sucursalId, activo));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public ResponseEntity<EmployeeResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EmployeeResponse> create(
            @Valid @RequestBody EmployeeRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        EmployeeResponse response = employeeService.crearEmpleado(request, userDetails.getUser().getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EmployeeResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody EmployeeRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(employeeService.actualizarEmpleado(id, request, userDetails.getUser().getId()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        employeeService.darDeBaja(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public ResponseEntity<List<EmployeeHistoryResponse>> getHistory(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.getHistory(id));
    }

    @PostMapping(value = "/{id}/document", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EmployeeResponse> uploadDocument(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        String url = fileStorageService.save(file, "employees/" + id);
        employeeService.actualizarDocumentoUrl(id, url);
        return ResponseEntity.ok(employeeService.getById(id));
    }
}
