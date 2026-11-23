package com.pos.system.dto.refund;

import java.math.BigDecimal;

public class RefundRequest {

    private Long paymentId;
    private BigDecimal amount;
    private String reason;

    public RefundRequest() {}

    public RefundRequest(Long paymentId, BigDecimal amount, String reason) {
        this.paymentId = paymentId;
        this.amount = amount;
        this.reason = reason;
    }

    public Long getPaymentId() { return paymentId; }
    public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
