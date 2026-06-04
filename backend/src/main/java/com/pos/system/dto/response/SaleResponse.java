package com.pos.system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SaleResponse {

    private Long id;
    private String status;
    private ClientInfo client;
    private UserInfo user;
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal discount;
    private BigDecimal total;
    private String notes;
    private List<SaleItemResponse> items;
    private List<PaymentResponse> payments;
    private LocalDateTime createdAt;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ClientInfo {
        private Long id;
        private String name;
        private String documentNumber;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserInfo {
        private Long id;
        private String username;
        private String fullName;
    }
}
