package com.pos.system.service.payment;

import com.pos.system.dto.payment.PaymentRequest;
import com.pos.system.dto.payment.PaymentResponse;
import com.pos.system.entity.business.Branch;
import com.pos.system.entity.payment.Payment;
import com.pos.system.entity.payment.Payment.PaymentStatus;
import com.pos.system.entity.payment.Payment.PaymentType;
import com.pos.system.entity.system.User;
import com.pos.system.repository.business.BranchRepository;
import com.pos.system.repository.payment.PaymentRepository;
import com.pos.system.repository.system.UserRepository;
import com.pos.system.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private UserRepository userRepository;

    public PaymentResponse createPayment(Long businessId, Long userId, PaymentRequest request) {
        Branch business = branchRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Negocio no encontrado con ID: " + businessId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + userId));

        Payment payment = Payment.builder()
                .transactionId(generateTransactionId())
                .business(business)
                .user(user)
                .paymentType(PaymentType.valueOf(request.getPaymentType()))
                .paymentStatus(PaymentStatus.PENDING)
                .amount(request.getAmount())
                .taxAmount(request.getTaxAmount())
                .totalAmount(request.getTotalAmount())
                .paymentMethod(request.getPaymentMethod())
                .transactionReference(request.getTransactionReference())
                .description(request.getDescription())
                .build();

        Payment saved = paymentRepository.save(payment);
        return mapToResponse(saved);
    }

    public PaymentResponse getPaymentById(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pago no encontrado con ID: " + id));
        return mapToResponse(payment);
    }

    public List<PaymentResponse> getPaymentsByBusiness(Long businessId) {
        return paymentRepository.findByBusinessId(businessId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<PaymentResponse> getPaymentsByStatus(String status) {
        return paymentRepository.findByStatus(status).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public PaymentResponse updatePaymentStatus(Long paymentId, PaymentStatus status) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Pago no encontrado con ID: " + paymentId));

        payment.setPaymentStatus(status);
        payment.setProcessedAt(LocalDateTime.now());

        Payment saved = paymentRepository.save(payment);
        return mapToResponse(saved);
    }

    public BigDecimal getTotalPaymentsByBusiness(Long businessId) {
        return paymentRepository.sumByBusinessIdAndStatus(businessId, "COMPLETED");
    }

    private PaymentResponse mapToResponse(Payment payment) {
        PaymentResponse response = new PaymentResponse();
        response.setId(payment.getId());
        response.setTransactionId(payment.getTransactionId());
        response.setBusinessId(payment.getBusiness().getId());
        response.setBusinessName(payment.getBusiness().getBranchName());
        response.setUserId(payment.getUser().getId());
        response.setUserName(payment.getUser().getFullName());
        response.setPaymentType(payment.getPaymentType().name());
        response.setPaymentStatus(payment.getPaymentStatus().name());
        response.setAmount(payment.getAmount());
        response.setTaxAmount(payment.getTaxAmount());
        response.setTotalAmount(payment.getTotalAmount());
        response.setPaymentMethod(payment.getPaymentMethod());
        response.setTransactionReference(payment.getTransactionReference());
        response.setDescription(payment.getDescription());
        response.setCreatedAt(payment.getCreatedAt());
        response.setProcessedAt(payment.getProcessedAt());
        return response;
    }

    private String generateTransactionId() {
        return "TXN-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
