package com.pos.system.controller.legal;

import com.pos.system.dto.legal.LegalHoldRequest;
import com.pos.system.dto.legal.LegalHoldResponse;
import com.pos.system.service.legal.LegalHoldService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/legal-holds")
public class LegalHoldController {

    @Autowired
    private LegalHoldService legalHoldService;

    @PostMapping("/businesses/{businessId}/users/{userId}")
    public ResponseEntity<LegalHoldResponse> createLegalHold(
            @PathVariable Long businessId,
            @PathVariable Long userId,
            @RequestBody LegalHoldRequest request) {
        LegalHoldResponse legalHold = legalHoldService.createLegalHold(businessId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(legalHold);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LegalHoldResponse> getLegalHoldById(@PathVariable Long id) {
        LegalHoldResponse legalHold = legalHoldService.getLegalHoldById(id);
        return ResponseEntity.ok(legalHold);
    }

    @GetMapping("/businesses/{businessId}")
    public ResponseEntity<List<LegalHoldResponse>> getLegalHoldsByBusiness(@PathVariable Long businessId) {
        List<LegalHoldResponse> legalHolds = legalHoldService.getLegalHoldsByBusiness(businessId);
        return ResponseEntity.ok(legalHolds);
    }

    @GetMapping("/active")
    public ResponseEntity<List<LegalHoldResponse>> getActiveLegalHolds() {
        List<LegalHoldResponse> legalHolds = legalHoldService.getActiveLegalHolds();
        return ResponseEntity.ok(legalHolds);
    }

    @PutMapping("/{id}")
    public ResponseEntity<LegalHoldResponse> updateLegalHold(
            @PathVariable Long id,
            @RequestBody LegalHoldRequest request) {
        LegalHoldResponse legalHold = legalHoldService.updateLegalHold(id, request);
        return ResponseEntity.ok(legalHold);
    }

    @PutMapping("/{id}/resolve")
    public ResponseEntity<LegalHoldResponse> resolveLegalHold(@PathVariable Long id) {
        LegalHoldResponse legalHold = legalHoldService.resolveLegalHold(id);
        return ResponseEntity.ok(legalHold);
    }
}
