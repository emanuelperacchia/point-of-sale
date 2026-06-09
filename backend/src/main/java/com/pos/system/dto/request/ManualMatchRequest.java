package com.pos.system.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManualMatchRequest {
    @NotNull
    private Long statementId;

    @NotNull
    private Long paymentId;

    @NotBlank
    private String tipo; // RECEIVABLE_PAYMENT or PAYABLE_PAYMENT
}
