package com.pos.system.dto.payment;

import java.math.BigDecimal;

public class PaymentRequest {

    private String paymentType;
    private BigDecimal amount;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private String paymentMethod;
    private String transactionReference;
    private String description;

    public PaymentRequest() {}

    public PaymentRequest(String paymentType, BigDecimal amount, BigDecimal taxAmount,
                          BigDecimal totalAmount, String paymentMethod,
                          String transactionReference, String description) {
        this.paymentType = paymentType;
        this.amount = amount;
        this.taxAmount = taxAmount;
        this.totalAmount = totalAmount;
        this.paymentMethod = paymentMethod;
        this.transactionReference = transactionReference;
        this.description = description;
    }

    public String getPaymentType() { return paymentType; }
    public void setPaymentType(String paymentType) { this.paymentType = paymentType; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getTransactionReference() { return transactionReference; }
    public void setTransactionReference(String transactionReference) { this.transactionReference = transactionReference; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
