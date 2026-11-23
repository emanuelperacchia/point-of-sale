package com.pos.system.dto.promotion;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PromotionRequest {

    private String name;
    private String description;
    private String type;
    private BigDecimal value;
    private Boolean active;
    private LocalDateTime startsAt;
    private LocalDateTime expiresAt;
    private Integer maxUses;
    private String couponCode;

    public PromotionRequest() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public BigDecimal getValue() { return value; }
    public void setValue(BigDecimal value) { this.value = value; }
    public Boolean isActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public LocalDateTime getStartsAt() { return startsAt; }
    public void setStartsAt(LocalDateTime startsAt) { this.startsAt = startsAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public Integer getMaxUses() { return maxUses; }
    public void setMaxUses(Integer maxUses) { this.maxUses = maxUses; }
    public String getCouponCode() { return couponCode; }
    public void setCouponCode(String couponCode) { this.couponCode = couponCode; }
}
