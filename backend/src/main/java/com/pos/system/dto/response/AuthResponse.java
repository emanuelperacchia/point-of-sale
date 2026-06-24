package com.pos.system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private Long userId;
    private String email;
    private String fullName;
    private Long branchId;
    private List<BranchInfo> branches;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BranchInfo {
        private Long id;
        private String nombre;
        private String direccion;
    }
}
