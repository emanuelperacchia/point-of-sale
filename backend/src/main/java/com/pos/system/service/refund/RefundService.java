package com.pos.system.service.refund;

import com.pos.system.dto.refund.RefundRequest;
import com.pos.system.dto.refund.RefundResponse;
import com.pos.system.entity.payment.Payment;
import com.pos.system.entity.refund.Refund;
import com.pos.system.entity.refund.Refund.RefundStatus;
import com.pos.system.entity.system.User;
import com.pos.system.repository.payment.PaymentRepository;
import com.pos.system.repository.refund.RefundRepository;
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
public class RefundService {

    @Autowired
    private RefundRepository refundRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private UserRepository userRepository;

    public RefundResponse createRefund(Long businessId, Long userId, RefundRequest request) {
        Payment payment = paymentRepository.findById(request.getPaymentId())
                .orElseThrow(() -> new ResourceNotFoundException("Pago no encontrado con ID: " + request.getPaymentId()));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + userId));

        Refund refund = Refund.builder()
                .referenceNumber(generateReferenceNumber())
                .payment(payment)
                .user(user)
                .amount(request.getAmount())
                .reason(request.getReason())
                .refundStatus(RefundStatus.PENDING)
                .build();

        Refund saved = refundRepository.save(refund);
        return mapToResponse(saved);
    }

    public RefundResponse getRefundById(Long id) {
        Refund refund = refundRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reembolso no encontrado con ID: " + id));
        return mapToResponse(refund);
    }

    public List<RefundResponse> getRefundsByBusiness(Long businessId) {
        return refundRepository.findByBusinessId(businessId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<RefundResponse> getRefundsByStatus(String status) {
        return refundRepository.findByStatus(status).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public RefundResponse updateRefundStatus(Long refundId, RefundStatus status) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new ResourceNotFoundException("Reembolso no encontrado con ID: " + refundId));

        refund.setRefundStatus(status);
        refund.setProcessedAt(LocalDateTime.now());

        Refund saved = refundRepository.save(refund);
        return mapToResponse(saved);
    }

    private RefundResponse mapToResponse(Refund refund) {
        RefundResponse response = new RefundResponse();
        response.setId(refund.getId());
        response.setReferenceNumber(refund.getReferenceNumber());
        response.setPaymentId(refund.getPayment().getId());
        response.setPaymentTransactionId(refund.getPayment().getTransactionId());
        response.setUserId(refund.getUser().getId());
        response.setUserName(refund.getUser().getFullName());
        response.setAmount(refund.getAmount());
        response.setReason(refund.getReason());
        response.setRefundStatus(refund.getRefundStatus().name());
        response.setAdminNotes(refund.getAdminNotes());
        response.setCreatedAt(refund.getCreatedAt());
        response.setProcessedAt(refund.getProcessedAt());
        response.setCompletedAt(refund.getCompletedAt());
        return response;
    }

    private String generateReferenceNumber() {
        return "REF-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
