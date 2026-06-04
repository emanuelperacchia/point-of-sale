package com.pos.system.dto.request;

import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class RedeemPointsRequest {
    @Positive
    private Long puntos;
}
