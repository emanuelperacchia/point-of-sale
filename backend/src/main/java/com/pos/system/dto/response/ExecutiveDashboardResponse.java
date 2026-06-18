package com.pos.system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Respuesta única del Dashboard Ejecutivo (US-035).
 * Orquestado por DashboardService con CompletableFuture.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExecutiveDashboardResponse {

    private SalesKPI sales;
    private InventoryKPI inventory;
    private FinancialKPI financial;
    private HRKPI hr;
    private ProductionKPI production;
    private List<DailySalesPoint> dailySales;
    private List<ProductRankingItem> topProducts;
    private List<SellerRankingItem> topSellers;
    private List<DashboardAlert> alerts;

    // ── KPIs anidados ─────────────────────────────────────────────

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SalesKPI {
        private BigDecimal totalSales;
        private Long transactionCount;
        private BigDecimal averageTicket;
        private BigDecimal salesVariation;        // % vs periodo anterior
        private BigDecimal transactionVariation;
        private String status;                     // "OK" | "CALCULATING" | "ERROR"
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InventoryKPI {
        private BigDecimal totalStockValue;
        private Long criticalStockCount;
        private Long noMovementCount;
        private String status;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FinancialKPI {
        private BigDecimal income;
        private BigDecimal expenses;
        private BigDecimal projectedBalance;
        private BigDecimal overdueReceivables;
        private String status;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HRKPI {
        private Long activeEmployees;
        private BigDecimal absenteeismRate;       // porcentaje
        private BigDecimal laborCost;
        private String status;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProductionKPI {
        private Long completedOrders;
        private BigDecimal averageWaste;
        private BigDecimal averageProductionCost;
        private String status;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DailySalesPoint {
        private String date;
        private BigDecimal amount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProductRankingItem {
        private Long productId;
        private String productName;
        private String productSku;
        private BigDecimal totalAmount;
        private BigDecimal quantity;
        private BigDecimal variation;             // vs periodo anterior
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SellerRankingItem {
        private Long employeeId;
        private String employeeName;
        private BigDecimal totalAmount;
        private Long transactionCount;
        private BigDecimal variation;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DashboardAlert {
        private String type;                      // "CRITICAL_STOCK" | "OVERDUE_RECEIVABLE" | "EXCESSIVE_WASTE" | "PENDING_PAYROLL"
        private String severity;                  // "HIGH" | "MEDIUM" | "LOW"
        private String message;
        private String actionLink;
        private Integer count;
    }
}
