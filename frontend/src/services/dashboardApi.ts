import api from './api';

export interface ExecutiveDashboardResponse {
  sales: SalesKPI | null;
  inventory: InventoryKPI | null;
  financial: FinancialKPI | null;
  hr: HRKPI | null;
  production: ProductionKPI | null;
  dailySales: DailySalesPoint[];
  topProducts: ProductRankingItem[];
  topSellers: SellerRankingItem[];
  alerts: DashboardAlert[];
}

export interface SalesKPI {
  totalSales: number;
  transactionCount: number;
  averageTicket: number;
  salesVariation: number;
  transactionVariation: number;
  status: string;
}

export interface InventoryKPI {
  totalStockValue: number;
  criticalStockCount: number;
  noMovementCount: number;
  status: string;
}

export interface FinancialKPI {
  income: number;
  expenses: number;
  projectedBalance: number;
  overdueReceivables: number;
  status: string;
}

export interface HRKPI {
  activeEmployees: number;
  absenteeismRate: number;
  laborCost: number;
  status: string;
}

export interface ProductionKPI {
  completedOrders: number;
  averageWaste: number;
  averageProductionCost: number;
  status: string;
}

export interface DailySalesPoint {
  date: string;
  amount: number;
}

export interface ProductRankingItem {
  productId: number;
  productName: string;
  productSku: string;
  totalAmount: number;
  quantity: number;
  variation: number;
}

export interface SellerRankingItem {
  employeeId: number;
  employeeName: string;
  totalAmount: number;
  transactionCount: number;
  variation: number;
}

export interface DashboardAlert {
  type: string;
  severity: string;
  message: string;
  actionLink: string;
  count: number;
}

export const dashboardApi = {
  getExecutive: (periodo = 'MONTH', sucursalId?: number) =>
    api.get<ExecutiveDashboardResponse>('/dashboard/executive', {
      params: { periodo, sucursalId },
    }),
};
