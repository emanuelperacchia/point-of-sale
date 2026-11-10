package com.pos.system.controller;

import com.pos.system.service.FileStorageService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Descarga de archivos con control de acceso por rol.
 * <p>
 * Reemplaza al resource handler estático de {@code /uploads/**} (que era
 * público): los documentos de empleados (DNI, contratos, certificados) solo
 * los pueden bajar ADMIN/GERENTE, los comprobantes de gastos suman CONTADOR,
 * y el resto queda restringido a ADMIN.
 * </p>
 */
@RestController
@RequestMapping("/uploads")
@RequiredArgsConstructor
public class FileDownloadController {

    private final FileStorageService fileStorageService;

    @GetMapping("/employees/**")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public ResponseEntity<Resource> downloadEmployeeDocument(HttpServletRequest request) {
        return serve(request.getRequestURI());
    }

    @GetMapping("/expenses/**")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','CONTADOR')")
    public ResponseEntity<Resource> downloadExpenseReceipt(HttpServletRequest request) {
        return serve(request.getRequestURI());
    }

    @GetMapping("/**")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Resource> downloadOther(HttpServletRequest request) {
        return serve(request.getRequestURI());
    }

    private ResponseEntity<Resource> serve(String fileUrl) {
        Resource resource = fileStorageService.load(fileUrl);

        MediaType mediaType = MediaTypeFactory.getMediaType(resource)
                .orElse(MediaType.APPLICATION_OCTET_STREAM);

        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(resource);
    }
}