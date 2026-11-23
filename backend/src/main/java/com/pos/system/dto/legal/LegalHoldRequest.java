package com.pos.system.dto.legal;

import java.time.LocalDateTime;

public class LegalHoldRequest {

    private String holdType;
    private String reason;
    private String legalBasis;
    private Boolean active;
    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveUntil;
    private LocalDateTime dataDeletionDate;
    private Integer legalRetentionDays;

    public LegalHoldRequest() {}

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
}
