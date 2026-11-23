package com.pos.system.controller.payment;

import com.pos.system.dto.payment.PaymentRequest;
import com.pos.system.dto.payment.PaymentResponse;
import com.pos.system.entity.payment.Payment.PaymentStatus;
import com.pos.system.service.payment.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping("/businesses/{businessId}/users/{userId}")
    public ResponseEntity<PaymentResponse> createPayment(
            @PathVariable Long businessId,
            @PathVariable Long userId,
            @RequestBody PaymentRequest request) {
        PaymentResponse payment = paymentService.createPayment(businessId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(payment);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getPaymentById(@PathVariable Long id) {
        PaymentResponse payment = paymentService.getPaymentById(id);
        return ResponseEntity.ok(payment);
    }

    @GetMapping("/businesses/{businessId}")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByBusiness(@PathVariable Long businessId) {
        List<PaymentResponse> payments = paymentService.getPaymentsByBusiness(businessId);
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByStatus(@PathVariable String status) {
        List<PaymentResponse> payments = paymentService.getPaymentsByStatus(status);
        return ResponseEntity.ok(payments);
    }

    @PutMapping("/{paymentId}/status")
    public ResponseEntity<PaymentResponse> updatePaymentStatus(
            @PathVariable Long paymentId,
            @PathVariable PaymentStatus status) {
        PaymentResponse payment = paymentService.updatePaymentStatus(paymentId, status);
        return ResponseEntity.ok(payment);
    }

    @GetMapping("/businesses/{businessId}/total")
    public ResponseEntity<BigDecimal> getTotalPaymentsByBusiness(@PathVariable Long businessId) {
        BigDecimal total = paymentService.getTotalPaymentsByBusiness(businessId);
        return ResponseEntity.ok(total);
    }
}
