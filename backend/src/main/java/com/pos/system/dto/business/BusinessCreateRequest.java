package com.pos.system.dto.business;

import java.math.BigDecimal;

public class BusinessCreateRequest {

    private String businessName;
    private String slug;
    private String address;
    private String phone;
    private String email;
    private String taxId;
    private String businessType;
    private String subscriptionPlan;
    private BigDecimal monthlyFee;
    private Boolean termsAccepted;

    public BusinessCreateRequest() {}

    public BusinessCreateRequest(String businessName, String slug, String address, String phone,
                                 String email, String taxId, String businessType,
                                 String subscriptionPlan, BigDecimal monthlyFee, Boolean termsAccepted) {
        this.businessName = businessName;
        this.slug = slug;
        this.address = address;
        this.phone = phone;
        this.email = email;
        this.taxId = taxId;
        this.businessType = businessType;
        this.subscriptionPlan = subscriptionPlan;
        this.monthlyFee = monthlyFee;
        this.termsAccepted = termsAccepted;
    }

    public String getBusinessName() { return businessName; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getTaxId() { return taxId; }
    public void setTaxId(String taxId) { this.taxId = taxId; }
    public String getBusinessType() { return businessType; }
    public void setBusinessType(String businessType) { this.businessType = businessType; }
    public String getSubscriptionPlan() { return subscriptionPlan; }
    public void setSubscriptionPlan(String subscriptionPlan) { this.subscriptionPlan = subscriptionPlan; }
    public BigDecimal getMonthlyFee() { return monthlyFee; }
    public void setMonthlyFee(BigDecimal monthlyFee) { this.monthlyFee = monthlyFee; }
    public Boolean isTermsAccepted() { return termsAccepted; }
    public void setTermsAccepted(Boolean termsAccepted) { this.termsAccepted = termsAccepted; }
}
