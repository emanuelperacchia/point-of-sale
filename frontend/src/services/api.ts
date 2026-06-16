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
  ReceivableResponse,
  ReceivablePaymentResponse,
  ReceivablePaymentRequest,
  AgingReportResponse,
  PayableResponse,
  PayablePaymentResponse,
  PayablePaymentRequest,
  PayableRequest,
  BankReconciliationResponse,
  BankStatementResponse,
  ManualMatchRequest,
  CreateExpenseFromStatementRequest,
  // Sprint 11 — RRHH
  EmployeeRequest,
  EmployeeResponse,
  EmployeeHistoryResponse,
  CheckInRequest,
  CheckOutRequest,
  AttendanceResponse,
  AbsenceRequest,
  AbsenceResponse,
  AttendanceSummaryResponse,
  ShiftDefinitionRequest,
  ShiftDefinitionResponse,
  ShiftAssignmentRequest,
  ShiftAssignmentResponse,
  ShiftScheduleResponse,
  ShiftChangeRequestDto,
  ShiftChangeRequestResponse,
  NotificationResponse,
  EvaluationTemplateRequest,
  EvaluationTemplateResponse,
  CreateEvaluationRequest,
  PerformanceEvaluationResponse,
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

  getAll: (page = 0, size = 10, sortBy = 'createdAt', sortDir = 'desc', search?: string, tipo?: string) =>
    api.get<any>('/products', { params: { page, size, sortBy, sortDir, search, tipo } }),
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

// ---------------------------------------------------------------------------
// Cuentas por Cobrar (Sprint 10 — US-025)
// ---------------------------------------------------------------------------

export const receivableApi = {
  getAll: (params?: {
    clientId?: number;
    estado?: string;
    page?: number;
    size?: number;
  }) => api.get<{ content: ReceivableResponse[]; totalElements: number; totalPages: number }>('/receivables', { params }),

  getById: (id: number) =>
    api.get<ReceivableResponse>(`/receivables/${id}`),

  getPayments: (id: number) =>
    api.get<ReceivablePaymentResponse[]>(`/receivables/${id}/payments`),

  registerPayment: (id: number, data: ReceivablePaymentRequest) =>
    api.post<ReceivablePaymentResponse>(`/receivables/${id}/payments`, data),

  getAgingReport: () =>
    api.get<AgingReportResponse>('/receivables/aging-report'),

  exportAgingReport: () =>
    api.get('/receivables/aging-report/export', { responseType: 'blob' }),
};

// ---------------------------------------------------------------------------
// Cuentas por Pagar (Sprint 10 — US-026)
// ---------------------------------------------------------------------------

export const payableApi = {
  getAll: (params?: {
    supplierId?: number;
    estado?: string;
    purchaseOrderId?: number;
  }) => api.get<PayableResponse[]>('/payables', { params }),

  getById: (id: number) =>
    api.get<PayableResponse>(`/payables/${id}`),

  getPayments: (id: number) =>
    api.get<PayablePaymentResponse[]>(`/payables/${id}/payments`),

  registerPayment: (id: number, data: PayablePaymentRequest) =>
    api.post<PayablePaymentResponse>(`/payables/${id}/payments`, data),

  create: (data: PayableRequest) =>
    api.post<PayableResponse>('/payables', data),

  getUpcoming: (dias: number = 30) =>
    api.get<PayableResponse[]>('/payables/upcoming', { params: { dias } }),
};

// ---------------------------------------------------------------------------
// Conciliación Bancaria (Sprint 10 — US-028)
// ---------------------------------------------------------------------------

export const bankReconciliationApi = {
  importCsv: (file: File) => {
    const form = new FormData();
    form.append('file', file);
    return api.post<BankReconciliationResponse>('/bank-reconciliation/import', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },

  getSummary: (periodo: string) =>
    api.get<BankReconciliationResponse>('/bank-reconciliation/summary', { params: { periodo } }),

  getStatements: (id: number) =>
    api.get<BankStatementResponse[]>(`/bank-reconciliation/${id}/statements`),

  autoMatch: (id: number) =>
    api.post<{ matched: number }>(`/bank-reconciliation/${id}/auto-match`),

  manualMatch: (data: ManualMatchRequest) =>
    api.post<BankStatementResponse>('/bank-reconciliation/statements/manual-match', data),

  createExpense: (data: CreateExpenseFromStatementRequest) =>
    api.post<ExpenseResponse>('/bank-reconciliation/statements/create-expense', data),

  exportSummary: (periodo: string) =>
    api.get('/bank-reconciliation/summary/export', {
      params: { periodo },
      responseType: 'blob',
    }),
};

// ---------------------------------------------------------------------------
// Recursos Humanos — Sprint 11
// ---------------------------------------------------------------------------

export const employeeApi = {
  getAll: (params?: { departamento?: string; cargo?: string; sucursalId?: number; activo?: boolean }) =>
    api.get<EmployeeResponse[]>('/employees', { params }),

  getById: (id: number) =>
    api.get<EmployeeResponse>(`/employees/${id}`),

  create: (data: EmployeeRequest) =>
    api.post<EmployeeResponse>('/employees', data),

  update: (id: number, data: EmployeeRequest) =>
    api.put<EmployeeResponse>(`/employees/${id}`, data),

  deactivate: (id: number) =>
    api.delete(`/employees/${id}`),

  getHistory: (id: number) =>
    api.get<EmployeeHistoryResponse[]>(`/employees/${id}/history`),

  uploadDocument: (id: number, file: File) => {
    const form = new FormData();
    form.append('file', file);
    return api.post<EmployeeResponse>(`/employees/${id}/document`, form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },
};

export const attendanceApi = {
  checkIn: (data: CheckInRequest) =>
    api.post<AttendanceResponse>('/attendance/check-in', data),

  checkOut: (data: CheckOutRequest) =>
    api.post<AttendanceResponse>('/attendance/check-out', data),

  registerAbsence: (data: AbsenceRequest) =>
    api.post<AbsenceResponse>('/attendance/ausencias', data),

  getAll: (params: { employeeId?: number; desde: string; hasta: string }) =>
    api.get<AttendanceResponse[]>('/attendance', { params }),

  getSummary: (params: { employeeId: number; mes: number; anio: number }) =>
    api.get<AttendanceSummaryResponse>('/attendance/summary', { params }),
};

export const shiftApi = {
  getDefinitions: () =>
    api.get<ShiftDefinitionResponse[]>('/shifts/definitions'),

  createDefinition: (data: ShiftDefinitionRequest) =>
    api.post<ShiftDefinitionResponse>('/shifts/definitions', data),

  assign: (data: ShiftAssignmentRequest) =>
    api.post<ShiftAssignmentResponse>('/shifts/assignments', data),

  getSchedule: (params: { semana: string; sucursalId?: number }) =>
    api.get<ShiftScheduleResponse>('/shifts/schedule', { params }),

  getEmployeeShifts: (employeeId: number, semana: string) =>
    api.get<ShiftAssignmentResponse[]>(`/shifts/employees/${employeeId}`, { params: { semana } }),

  requestChange: (data: ShiftChangeRequestDto) =>
    api.post<ShiftChangeRequestResponse>('/shifts/change-requests', data),

  getPendingRequests: () =>
    api.get<ShiftChangeRequestResponse[]>('/shifts/change-requests/pending'),

  resolveRequest: (id: number, aprobado: boolean) =>
    api.put<ShiftChangeRequestResponse>(`/shifts/change-requests/${id}/resolve`, null, {
      params: { aprobado },
    }),
};

export const notificationApi = {
  getAll: () =>
    api.get<NotificationResponse[]>('/notifications'),

  getUnread: () =>
    api.get<NotificationResponse[]>('/notifications/unread'),

  getCount: () =>
    api.get<{ count: number }>('/notifications/count'),

  markRead: (id: number) =>
    api.put(`/notifications/${id}/read`),
};

export const evaluationApi = {
  getTemplates: () =>
    api.get<EvaluationTemplateResponse[]>('/evaluations/templates'),

  createTemplate: (data: EvaluationTemplateRequest) =>
    api.post<EvaluationTemplateResponse>('/evaluations/templates', data),

  create: (data: CreateEvaluationRequest) =>
    api.post<PerformanceEvaluationResponse>('/evaluations', data),

  finalize: (id: number) =>
    api.put<PerformanceEvaluationResponse>(`/evaluations/${id}/finalize`),

  getByEmployee: (employeeId: number) =>
    api.get<PerformanceEvaluationResponse[]>(`/evaluations/employees/${employeeId}`),

  calculateScore: (id: number) =>
    api.get<{ puntuacionFinal: number }>(`/evaluations/${id}/calculate`),
};

// ── Sprint 12: Comisiones ──────────────────────────────────────────

export const commissionApi = {
  listSchemes: () =>
    api.get<CommissionSchemeResponse[]>('/commissions/schemes'),

  createScheme: (data: CommissionSchemeRequest) =>
    api.post<CommissionSchemeResponse>('/commissions/schemes', data),

  calculate: (employeeId: number, mes: number, anio: number) =>
    api.post<CommissionResultResponse>(`/commissions/calculate`, null, {
      params: { employeeId, mes, anio },
    }),

  summary: (employeeId: number, mes: number, anio: number) =>
    api.get<CommissionResultResponse>('/commissions/summary', {
      params: { employeeId, mes, anio },
    }),

  ranking: (mes: number, anio: number) =>
    api.get<CommissionResultResponse[]>('/commissions/ranking', {
      params: { mes, anio },
    }),
};

// ── Sprint 12: Nómina ─────────────────────────────────────────────

export const payrollApi = {
  calculateAndSave: (employeeId: number, mes: number, anio: number) =>
    api.post<PayrollResponse>(`/payroll/calculate/${employeeId}`, null, {
      params: { mes, anio },
    }),

  getById: (id: number) =>
    api.get<PayrollResponse>(`/payroll/${id}`),

  list: (params?: { mes?: number; anio?: number; employeeId?: number }) =>
    api.get<PayrollResponse[]>('/payroll', { params }),

  approve: (id: number, aprobadoPor: number) =>
    api.post<PayrollResponse>(`/payroll/${id}/approve`, null, {
      params: { aprobadoPor },
    }),

  addAdjustment: (payrollId: number, concepto: string, monto: number, creadoPor: number, justificacion?: string) =>
    api.post<PayrollAdjustmentResponse>(`/payroll/${payrollId}/adjustments`, null, {
      params: { concepto, monto, creadoPor, justificacion },
    }),

  listAdjustments: (payrollId: number) =>
    api.get<PayrollAdjustmentResponse[]>(`/payroll/${payrollId}/adjustments`),

  generatePdf: (id: number) =>
    api.get(`/payroll/${id}/pdf`, { responseType: 'blob' }),

  exportCsv: (mes: number, anio: number) =>
    api.post(`/payroll/export/csv`, null, {
      params: { mes, anio },
      responseType: 'blob',
    }),
};

// ── Sprint 13: Produccion ──────────────────────────────────────────

export const recipeApi = {
  list: () => api.get<RecipeResponse[]>('/recipes'),

  getById: (id: number) => api.get<RecipeResponse>(`/recipes/${id}`),

  create: (data: RecipeRequest) =>
    api.post<RecipeResponse>('/recipes', data),

  update: (id: number, data: RecipeRequest) =>
    api.put<RecipeResponse>(`/recipes/${id}`, data),

  delete: (id: number) => api.delete(`/recipes/${id}`),

  getBomExplosion: (id: number, cantidad = 1) =>
    api.get<BomExplosionResponse>(`/recipes/${id}/bom-explosion`, {
      params: { cantidad },
    }),

  getCostEstimate: (id: number, cantidad = 1) =>
    api.get<CostEstimateResponse>(`/recipes/${id}/cost-estimate`, {
      params: { cantidad },
    }),
};

export const productionOrderApi = {
  list: (estado?: string) =>
    api.get<ProductionOrderResponse[]>('/production-orders', {
      params: estado ? { estado } : {},
    }),

  getById: (id: number) =>
    api.get<ProductionOrderResponse>(`/production-orders/${id}`),

  create: (data: ProductionOrderRequest) =>
    api.post<ProductionOrderResponse>('/production-orders', data),

  start: (id: number) =>
    api.post<ProductionOrderResponse>(`/production-orders/${id}/start`),

  complete: (id: number, cantidadProducida: number, mermaEntries?: any[]) =>
    api.post<ProductionOrderResponse>(
      `/production-orders/${id}/complete?cantidadProducida=${cantidadProducida}`,
      mermaEntries || [],
    ),

  cancel: (id: number) =>
    api.post<ProductionOrderResponse>(`/production-orders/${id}/cancel`),

  getCostAnalysis: (id: number) =>
    api.get<CostAnalysisResponse>(`/production-orders/${id}/cost-analysis`),
};

export const lotApi = {
  getTraceability: (loteId: string) =>
    api.get<LoteTraceabilityResponse>(`/lots/${loteId}/traceability`),
};

export default api;
