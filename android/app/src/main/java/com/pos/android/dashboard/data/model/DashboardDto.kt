package com.pos.android.dashboard.data.model

import com.google.gson.annotations.SerializedName

/**
 * Mapea exactamente ExecutiveDashboardResponse del backend.
 */
data class ExecutiveDashboardDto(
    @SerializedName("sales") val sales: SalesKpiDto?,
    @SerializedName("inventory") val inventory: InventoryKpiDto?,
    @SerializedName("financial") val financial: FinancialKpiDto?,
    @SerializedName("hr") val hr: HrKpiDto?,
    @SerializedName("production") val production: ProductionKpiDto?,
    @SerializedName("dailySales") val dailySales: List<DailySalesPointDto>?,
    @SerializedName("topProducts") val topProducts: List<ProductRankingDto>?,
    @SerializedName("topSellers") val topSellers: List<SellerRankingDto>?,
    @SerializedName("alerts") val alerts: List<DashboardAlertDto>?
)

data class SalesKpiDto(
    @SerializedName("totalSales") val totalSales: Double?,
    @SerializedName("transactionCount") val transactionCount: Long?,
    @SerializedName("averageTicket") val averageTicket: Double?,
    @SerializedName("salesVariation") val salesVariation: Double?,
    @SerializedName("transactionVariation") val transactionVariation: Double?,
    @SerializedName("status") val status: String?
)

data class InventoryKpiDto(
    @SerializedName("totalStockValue") val totalStockValue: Double?,
    @SerializedName("criticalStockCount") val criticalStockCount: Long?,
    @SerializedName("noMovementCount") val noMovementCount: Long?,
    @SerializedName("status") val status: String?
)

data class FinancialKpiDto(
    @SerializedName("income") val income: Double?,
    @SerializedName("expenses") val expenses: Double?,
    @SerializedName("projectedBalance") val projectedBalance: Double?,
    @SerializedName("overdueReceivables") val overdueReceivables: Double?,
    @SerializedName("status") val status: String?
)

data class HrKpiDto(
    @SerializedName("activeEmployees") val activeEmployees: Long?,
    @SerializedName("absenteeismRate") val absenteeismRate: Double?,
    @SerializedName("laborCost") val laborCost: Double?,
    @SerializedName("status") val status: String?
)

data class ProductionKpiDto(
    @SerializedName("completedOrders") val completedOrders: Long?,
    @SerializedName("averageWaste") val averageWaste: Double?,
    @SerializedName("averageProductionCost") val averageProductionCost: Double?,
    @SerializedName("status") val status: String?
)

data class DailySalesPointDto(
    @SerializedName("date") val date: String?,
    @SerializedName("amount") val amount: Double?
)

data class ProductRankingDto(
    @SerializedName("productId") val productId: Long?,
    @SerializedName("productName") val productName: String?,
    @SerializedName("productSku") val productSku: String?,
    @SerializedName("totalAmount") val totalAmount: Double?,
    @SerializedName("quantity") val quantity: Double?,
    @SerializedName("variation") val variation: Double?
)

data class SellerRankingDto(
    @SerializedName("employeeId") val employeeId: Long?,
    @SerializedName("employeeName") val employeeName: String?,
    @SerializedName("totalAmount") val totalAmount: Double?,
    @SerializedName("transactionCount") val transactionCount: Long?,
    @SerializedName("variation") val variation: Double?
)

data class DashboardAlertDto(
    @SerializedName("type") val type: String?,
    @SerializedName("severity") val severity: String?,
    @SerializedName("message") val message: String?,
    @SerializedName("actionLink") val actionLink: String?,
    @SerializedName("count") val count: Int?
)
