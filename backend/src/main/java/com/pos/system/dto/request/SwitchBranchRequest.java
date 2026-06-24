package com.pos.system.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SwitchBranchRequest {
    @NotNull(message = "branchId es requerido")
    private Long branchId;
}
