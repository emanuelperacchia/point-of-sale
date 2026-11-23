package com.pos.system.dto.legal;

import java.time.LocalDateTime;

public class LegalHoldResponse {

    private Long id;
    private Long businessId;
    private String businessName;
    private Long userId;
    private String userName;
    private String holdType;
    private String reason;
    private String legalBasis;
    private Boolean active;
    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveUntil;
    private LocalDateTime dataDeletionDate;
    private Integer legalRetentionDays;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
    private String resolvedBy;

    public LegalHoldResponse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getBusinessId() { return businessId; }
    public void setBusinessId(Long businessId) { this.businessId = businessId; }
    public String getBusinessName() { return businessName; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getHoldType() { return holdType; }
    public void setHoldType(String holdType) { this.holdType = holdType; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getLegalBasis() { return legalBasis; }
    public void setLegalBasis(String legalBasis) { this.legalBasis = legalBasis; }
    public Boolean isActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public LocalDateTime getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDateTime effectiveFrom) { this.effectiveFrom = effectiveFrom; }
    public LocalDateTime getEffectiveUntil() { return effectiveUntil; }
    public void setEffectiveUntil(LocalDateTime effectiveUntil) { this.effectiveUntil = effectiveUntil; }
    public LocalDateTime getDataDeletionDate() { return dataDeletionDate; }
    public void setDataDeletionDate(LocalDateTime dataDeletionDate) { this.dataDeletionDate = dataDeletionDate; }
    public Integer getLegalRetentionDays() { return legalRetentionDays; }
    public void setLegalRetentionDays(Integer legalRetentionDays) { this.legalRetentionDays = legalRetentionDays; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
    public String getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(String resolvedBy) { this.resolvedBy = resolvedBy; }
}
