package com.pos.system.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CouponValidateRequest {
    @NotBlank
    private String codigo;
    private Long clientId;
}
