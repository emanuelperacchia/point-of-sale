package com.pos.system.controller;

import com.pos.system.dto.response.DigitalCertificateResponse;
import com.pos.system.service.DigitalCertificateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/fiscal/certificates")
@RequiredArgsConstructor
public class FiscalCertificateController {

    private final DigitalCertificateService digitalCertificateService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DigitalCertificateResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("storePassword") String storePassword,
            @RequestParam("alias") String alias) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(digitalCertificateService.upload(file, storePassword, alias));
    }

    @GetMapping("/active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DigitalCertificateResponse> getActive() {
        return ResponseEntity.ok(digitalCertificateService.getActive());
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<DigitalCertificateResponse>> listAll() {
        return ResponseEntity.ok(digitalCertificateService.listAll());
    }
}