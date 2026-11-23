package com.pos.system.controller.refund;

import com.pos.system.dto.refund.RefundRequest;
import com.pos.system.dto.refund.RefundResponse;
import com.pos.system.entity.refund.Refund.RefundStatus;
import com.pos.system.service.refund.RefundService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/refunds")
public class RefundController {

    @Autowired
    private RefundService refundService;

    @PostMapping("/businesses/{businessId}/users/{userId}")
    public ResponseEntity<RefundResponse> createRefund(
            @PathVariable Long businessId,
            @PathVariable Long userId,
            @RequestBody RefundRequest request) {
        RefundResponse refund = refundService.createRefund(businessId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(refund);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RefundResponse> getRefundById(@PathVariable Long id) {
        RefundResponse refund = refundService.getRefundById(id);
        return ResponseEntity.ok(refund);
    }

    @GetMapping("/businesses/{businessId}")
    public ResponseEntity<List<RefundResponse>> getRefundsByBusiness(@PathVariable Long businessId) {
        List<RefundResponse> refunds = refundService.getRefundsByBusiness(businessId);
        return ResponseEntity.ok(refunds);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<RefundResponse>> getRefundsByStatus(@PathVariable String status) {
        List<RefundResponse> refunds = refundService.getRefundsByStatus(status);
        return ResponseEntity.ok(refunds);
    }

    @PutMapping("/{refundId}/status")
    public ResponseEntity<RefundResponse> updateRefundStatus(
            @PathVariable Long refundId,
            @PathVariable RefundStatus status) {
        RefundResponse refund = refundService.updateRefundStatus(refundId, status);
        return ResponseEntity.ok(refund);
    }
}
