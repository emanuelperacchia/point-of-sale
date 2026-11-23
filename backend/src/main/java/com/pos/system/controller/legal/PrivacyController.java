package com.pos.system.controller.legal;

import com.pos.system.dto.legal.LegalHoldResponse;
import com.pos.system.service.legal.LegalHoldService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class PrivacyController {

    @Autowired
    private LegalHoldService legalHoldService;

    @GetMapping("/me/data-export")
    public ResponseEntity<java.util.Map<String, Object>> exportMyData() {
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("userId", 1L);
        data.put("email", "admin@pos.com");
        data.put("fullName", "Admin System");
        response.setCreatedAt(java.time.LocalDateTime.now());
        response.setUpdatedAt(java.time.LocalDateTime.now());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/me/delete-request")
    public ResponseEntity<Void> requestAccountDeletion() {
        // Implementar lógica de baja de cuenta
        return ResponseEntity.noContent().build();
    }
}
