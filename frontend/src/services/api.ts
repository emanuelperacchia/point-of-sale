import axios from 'axios';
import type {
  AuthResponse,
  LoginCredentials,
  ProductSearchResult,
  SaleRequest,
  SaleResponse,
  ClientSearchResult,
  CartValidationResult,
  ShiftResponse,
  ShiftMovementResponse,
  ShiftReportResponse,
  ShiftStatus,
  User,
  CondicionIva,
  SaleReturnResponse,
  CreateReturnRequest,
  CouponValidationResponse,
  DiscountResult,
  ValidateCartDiscountsRequest,
  PointsResponse,
  SalesBookResponse,
  CashFlowResponse,
  ExpenseResponse,
  ExpenseSummaryResponse,
  ExpenseRequest,
} from '../types';

// ---------------------------------------------------------------------------
// Axios instance
// ---------------------------------------------------------------------------

const api = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
});

// ---------------------------------------------------------------------------
// Request interceptor — attach Bearer token
// ---------------------------------------------------------------------------

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// ---------------------------------------------------------------------------
// Response interceptor — auto‑refresh on 401
// ---------------------------------------------------------------------------

let isRefreshing = false;
let failedQueue: Array<{
  resolve: (token: string) => void;
  reject: (err: unknown) => void;
}> = [];

const processQueue = (error: unknown, token: string | null) => {
  failedQueue.forEach((prom) => {
    if (error) prom.reject(error);
    else prom.resolve(token!);
  });
  failedQueue = [];
};

api.interceptors.response.use(
  (res) => res,
  async (error) => {
    const originalRequest = error.config;

    if (error.response?.status === 401 && !originalRequest._retry) {
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        }).then((token) => {
          originalRequest.headers.Authorization = `Bearer ${token}`;
          return api(originalRequest);
        });
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        const refreshToken = localStorage.getItem('refreshToken');
        const { data } = await axios.post<AuthResponse>('/api/auth/refresh', { refreshToken });

        localStorage.setItem('accessToken', data.accessToken);
        localStorage.setItem('refreshToken', data.refreshToken);

        processQueue(null, data.accessToken);
        originalRequest.headers.Authorization = `Bearer ${data.accessToken}`;
        return api(originalRequest);
      } catch (refreshError) {
        processQueue(refreshError, null);
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        window.location.href = '/login';
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  },
);

// ---------------------------------------------------------------------------
// Auth endpoints
// ---------------------------------------------------------------------------

export const authApi = {
  login: (credentials: LoginCredentials) =>
    api.post<AuthResponse>('/auth/login', credentials),

  refresh: (refreshToken: string) =>
    api.post<AuthResponse>('/auth/refresh', { refreshToken }),

  logout: (refreshToken: string) =>
    api.post('/auth/logout', { refreshToken }),

  me: () => api.get<User>('/auth/me'),
};

// ---------------------------------------------------------------------------
// Product endpoints
// ---------------------------------------------------------------------------

export const productApi = {
  search: (q: string, limit = 10) =>
    api.get<ProductSearchResult[]>('/products/search', { params: { q, limit } }),
};

// ---------------------------------------------------------------------------
// Sale endpoints
// ---------------------------------------------------------------------------

export const saleApi = {
  create: (data: SaleRequest) =>
    api.post<SaleResponse>('/sales', data),

  validateCart: (warehouseId: number, items: { productId: number; quantity: number }[]) =>
    api.post<CartValidationResult>('/sales/validate-cart', { warehouseId, items }),

  getById: (id: number) =>
    api.get<SaleResponse>(`/sales/${id}`),

  getMySales: (page = 0, size = 20) =>
    api.get('/sales', { params: { page, size } }),
};

// ---------------------------------------------------------------------------
// Client endpoints
// ---------------------------------------------------------------------------

export const clientApi = {
  search: (q: string) =>
    api.get<ClientSearchResult[]>('/clients/search', { params: { q } }),

  getById: (id: number) =>
    api.get<ClientSearchResult>(`/clients/${id}`),

  create: (data: {
    name: string;
    documentType?: string;
    documentNumber?: string;
    email?: string;
    phone?: string;
    businessName?: string;
    condicionIva?: CondicionIva;
    taxAddress?: string;
  }) =>
    api.post<ClientSearchResult>('/clients', data),

  update: (id: number, data: {
    name: string;
    documentType?: string;
    documentNumber?: string;
    email?: string;
    phone?: string;
    address?: string;
    businessName?: string;
    condicionIva?: CondicionIva;
    taxAddress?: string;
  }) =>
    api.put<ClientSearchResult>(`/clients/${id}`, data),
};

// ---------------------------------------------------------------------------
// Shift (turnos de caja) endpoints
// ---------------------------------------------------------------------------

export const shiftApi = {
  open: (sucursalId: number, montoApertura: number) =>
    api.post<ShiftResponse>('/shifts/open', { sucursalId, montoApertura }),

  close: (id: number, montoCierre: number) =>
    api.post<ShiftResponse>(`/shifts/${id}/close`, { montoCierre }),

  addMovement: (id: number, tipo: 'RETIRO' | 'INGRESO', monto: number, motivo: string) =>
    api.post<ShiftMovementResponse>(`/shifts/${id}/movements`, { tipo, monto, motivo }),

  getReport: (id: number) =>
    api.get<ShiftReportResponse>(`/shifts/${id}/report`),

  getById: (id: number) =>
    api.get<ShiftResponse>(`/shifts/${id}`),

  findByFilters: (cajeroId?: number, estado?: ShiftStatus) =>
    api.get<ShiftResponse[]>('/shifts', { params: { cajeroId, estado } }),
};

// ---------------------------------------------------------------------------
// Return (devoluciones) endpoints
// ---------------------------------------------------------------------------

export const returnApi = {
  create: (data: CreateReturnRequest) =>
    api.post<SaleReturnResponse>('/returns', data),

  approve: (id: number) =>
    api.post<SaleReturnResponse>(`/returns/${id}/approve`),

  reject: (id: number) =>
    api.post<SaleReturnResponse>(`/returns/${id}/reject`),

  getById: (id: number) =>
    api.get<SaleReturnResponse>(`/returns/${id}`),

  findBySaleId: (saleId: number) =>
    api.get<SaleReturnResponse[]>('/returns', { params: { saleId } }),
};

// ---------------------------------------------------------------------------
// Promotion (descuentos automáticos) endpoints
// ---------------------------------------------------------------------------

export const promotionApi = {
  validateCart: (data: ValidateCartDiscountsRequest) =>
    api.post<DiscountResult>('/promotions/validate-cart', data),
};

// ---------------------------------------------------------------------------
// Coupon endpoints
// ---------------------------------------------------------------------------

export const couponApi = {
  validate: (codigo: string) =>
    api.post<CouponValidationResponse>('/coupons/validate', { codigo }),
};

// ---------------------------------------------------------------------------
// Loyalty (fidelización) endpoints
// ---------------------------------------------------------------------------

export const loyaltyApi = {
  getPoints: (clientId: number) =>
    api.get<PointsResponse>(`/clients/${clientId}/points`),
};

// ---------------------------------------------------------------------------
// Reportes (Sprint 9)
// ---------------------------------------------------------------------------

export const reportApi = {
  getSalesBook: (params: {
    desde: string;
    hasta: string;
    tipo?: string;
    page?: number;
    size?: number;
  }) => api.get<SalesBookResponse>('/reports/sales-book', { params }),

  exportSalesBook: (params: {
    desde: string;
    hasta: string;
    tipo?: string;
    format: 'xlsx' | 'csv';
  }) => api.get('/reports/sales-book/export', {
    params,
    responseType: 'blob',
  }),

  getCashFlow: (params: {
    desde: string;
    hasta: string;
    incluirProyeccion?: boolean;
    diasProyeccion?: number;
  }) => api.get<CashFlowResponse>('/reports/cash-flow', { params }),

  exportCashFlow: (params: {
    desde: string;
    hasta: string;
    incluirProyeccion?: boolean;
    diasProyeccion?: number;
  }) => api.get('/reports/cash-flow/export', {
    params,
    responseType: 'blob',
  }),
};

// ---------------------------------------------------------------------------
// Gastos (Sprint 9)
// ---------------------------------------------------------------------------

export const expenseApi = {
  getAll: (params?: {
    categoria?: string;
    estado?: string;
    desde?: string;
    hasta?: string;
    proveedorId?: number;
  }) => api.get<ExpenseResponse[]>('/expenses', { params }),

  getById: (id: number) =>
    api.get<ExpenseResponse>(`/expenses/${id}`),

  create: (data: ExpenseRequest, comprobante?: File) => {
    const form = new FormData();
    form.append('data', new Blob([JSON.stringify(data)], { type: 'application/json' }));
    if (comprobante) form.append('comprobante', comprobante);
    return api.post<ExpenseResponse>('/expenses', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },

  update: (id: number, data: ExpenseRequest, comprobante?: File) => {
    const form = new FormData();
    form.append('data', new Blob([JSON.stringify(data)], { type: 'application/json' }));
    if (comprobante) form.append('comprobante', comprobante);
    return api.put<ExpenseResponse>(`/expenses/${id}`, form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },

  delete: (id: number) =>
    api.delete(`/expenses/${id}`),

  marcarPagado: (id: number) =>
    api.patch(`/expenses/${id}/pagar`),

  getSummary: (params: {
    desde: string;
    hasta: string;
  }) => api.get<ExpenseSummaryResponse>('/expenses/summary', { params }),
};

export default api;
