// Tipos compartidos del POS

export interface User {
  id: number;
  email: string;
  fullName: string;
  firstName: string;
  lastName: string;
  roles: string[];
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  userId: number;
  email: string;
  fullName: string;
}

export interface LoginCredentials {
  email: string;
  password: string;
}

export interface ProductSearchResult {
  id: number;
  name: string;
  sku: string;
  price: number;
  stock: number;
  categoryName: string | null;
  active: boolean;
}

export interface CartItem {
  product: ProductSearchResult;
  quantity: number;
  discount: number;
}

export interface SaleItemRequest {
  productId: number;
  quantity: number;
  discount: number;
}

export interface PaymentRequest {
  paymentMethod: PaymentMethod;
  amount: number;
  reference?: string;
}

export type PaymentMethod = 'CASH' | 'DEBIT_CARD' | 'CREDIT_CARD' | 'TRANSFER' | 'CUENTA_CORRIENTE';

export interface SaleRequest {
  clientId?: number;
  notes?: string;
  couponCode?: string;
  puntosCanje?: number;
  items: SaleItemRequest[];
  payments: PaymentRequest[];
}

export interface SaleResponse {
  id: number;
  status: string;
  client: { id: number; name: string; documentNumber: string } | null;
  user: { id: number; username: string; fullName: string };
  subtotal: number;
  taxAmount: number;
  discount: number;
  total: number;
  items: SaleItemResponse[];
  payments: PaymentResponse[];
  createdAt: string;
}

export interface SaleItemResponse {
  id: number;
  productId: number;
  productName: string;
  productSku: string;
  quantity: number;
  unitPrice: number;
  discount: number;
  taxAmount: number;
  subtotal: number;
}

export interface PaymentResponse {
  id: number;
  paymentMethod: string;
  amount: number;
  reference: string | null;
}

export type CondicionIva = 'RESPONSABLE_INSCRIPTO' | 'MONOTRIBUTISTA' | 'EXENTO' | 'CONSUMIDOR_FINAL';

export const CONDICION_IVA_LABELS: Record<CondicionIva, string> = {
  RESPONSABLE_INSCRIPTO: 'Responsable Inscripto',
  MONOTRIBUTISTA: 'Monotributista',
  EXENTO: 'Exento',
  CONSUMIDOR_FINAL: 'Consumidor Final',
};

/** Mapeo de condición fiscal a tipo de comprobante */
export const TIPO_COMPROBANTE_MAP: Record<CondicionIva, string> = {
  RESPONSABLE_INSCRIPTO: 'Factura A',
  MONOTRIBUTISTA: 'Factura C',
  EXENTO: 'Factura B',
  CONSUMIDOR_FINAL: 'Boleta',
};

export interface ClientSearchResult {
  id: number;
  name: string;
  documentType: string | null;
  documentNumber: string | null;
  email: string | null;
  phone: string | null;
  address: string | null;
  businessName: string | null;
  condicionIva: CondicionIva | null;
  taxAddress: string | null;
}

// ──────────────────────────────────────────────
// Turnos de caja (US-016)
// ──────────────────────────────────────────────

export type ShiftStatus = 'ABIERTO' | 'CERRADO';

export interface ShiftResponse {
  id: number;
  cajeroId: number;
  cajeroNombre: string;
  sucursalId: number;
  estado: ShiftStatus;
  montoApertura: number;
  montoCierre: number | null;
  diferencia: number | null;
  fechaApertura: string;
  fechaCierre: string | null;
}

export interface ShiftMovementResponse {
  id: number;
  shiftId: number;
  tipo: 'RETIRO' | 'INGRESO';
  monto: number;
  motivo: string;
  usuarioNombre: string;
  createdAt: string;
}

export interface ShiftReportResponse {
  shiftId: number;
  estado: ShiftStatus;
  montoApertura: number;
  totalVentasEfectivo: number;
  ventasPorMetodoPago: Record<string, number>;
  totalIngresos: number;
  totalRetiros: number;
  movimientos: ShiftMovementResponse[];
  montoEsperado: number;
  montoCierreDeclarado: number | null;
  diferencia: number | null;
  fechaApertura: string;
  fechaCierre: string | null;
}

// ──────────────────────────────────────────────
// Devoluciones (US-019a)
// ──────────────────────────────────────────────

export type ReturnStatus = 'PENDIENTE_APROBACION' | 'APROBADA' | 'RECHAZADA';

export interface ReturnItemResponse {
  id: number;
  saleItemId: number;
  cantidad: number;
  precioUnitario: number;
  subtotal: number;
}

export interface SaleReturnResponse {
  id: number;
  saleId: number;
  estado: ReturnStatus;
  motivo: string;
  aprobadorId: number | null;
  montoTotal: number;
  metodoDevolucion: string;
  notaCreditoId: number | null;
  referenciaDevolucion: string | null;
  items: ReturnItemResponse[];
  createdAt: string;
}

export interface CreateReturnRequest {
  saleId: number;
  motivo: string;
  items: { saleItemId: number; cantidad: number }[];
}

export interface CartValidationResult {
  valid: boolean;
  items: {
    productId: number;
    productName: string;
    requested: number;
    available: number;
    enoughStock: boolean;
    message: string;
  }[];
}

// ──────────────────────────────────────────────
// Promociones y Cupones (US-018)
// ──────────────────────────────────────────────

export type PromotionType = 'PORCENTAJE' | 'MONTO_FIJO' | 'DOSX1' | 'TRESX2' | 'COMPRA_X_LLEVA_Y';
export type PromotionScope = 'PRODUCTO' | 'CATEGORIA' | 'CARRITO';

export interface Promotion {
  id: number;
  nombre: string;
  tipo: PromotionType;
  valor: number;
  fechaDesde: string;
  fechaHasta: string;
  prioridad: number;
  alcance: PromotionScope;
  activa: boolean;
  compraX: number | null;
  llevaY: number | null;
  productoIds: number[];
  categoriaIds: number[];
  createdAt: string;
}

export interface CouponValidationResponse {
  id: number | null;
  codigo: string;
  tipo: PromotionType | null;
  valor: number;
  valido: boolean;
  mensaje: string;
}

export interface ItemDiscount {
  saleItemIndex: number;
  productId: number | null;
  productName: string;
  promotionName: string;
  promotionId: number;
  discountAmount: number;
  description: string;
}

export interface DiscountResult {
  itemsDiscount: ItemDiscount[];
  totalDiscount: number;
  appliedCouponCode: string | null;
  couponDiscount: number;
}

export interface ValidateCartDiscountsRequest {
  items: {
    productId: number;
    productName: string;
    quantity: number;
    unitPrice: number;
    categoryId: number | null;
  }[];
  couponCode?: string;
}

// ──────────────────────────────────────────────
// Fidelización (US-017)
// ──────────────────────────────────────────────

export type ClientTier = 'BRONCE' | 'PLATA' | 'ORO';

export interface PointsTransactionItem {
  id: number;
  saleId: number | null;
  tipo: string;
  puntos: number;
  saldoPrevio: number;
  saldoPosterior: number;
  descripcion: string;
  fecha: string;
}

export interface PointsResponse {
  clientId: number;
  clientName: string;
  saldoActual: number;
  tier: ClientTier;
  historial: PointsTransactionItem[];
}

// ──────────────────────────────────────────────
// Reportes (Sprint 9)
// ──────────────────────────────────────────────

export interface SalesBookRow {
  fecha: string;
  tipoComprobante: string;
  puntoVenta: number;
  numero: number;
  cuitReceptor: string | null;
  razonSocial: string | null;
  netoGravado: number;
  iva: number;
  otrosImpuestos: number;
  total: number;
  estado: string;
}

export interface SalesBookTotals {
  totalNeto: number;
  totalIva: number;
  totalComprobantes: number;
  cantidad: number;
}

export interface SalesBookResponse {
  filas: SalesBookRow[];
  totales: SalesBookTotals;
  saltosEncontrados: number[];
  pagina: number;
  tamanioPagina: number;
  totalElementos: number;
  totalPaginas: number;
}

export interface ExpenseResponse {
  id: number;
  monto: number;
  fecha: string;
  categoria: string;
  proveedorId: number | null;
  descripcion: string;
  estado: string;
  comprobanteUrl: string | null;
  recurrente: boolean;
  frecuencia: string | null;
  proximaFecha: string | null;
}

export interface ExpenseSummaryResponse {
  desde: string;
  hasta: string;
  categorias: { categoria: string; total: number }[];
  total: number;
}

export interface CashFlowDayRow {
  fecha: string;
  ingresos: number;
  egresos: number;
  saldoDia: number;
  saldoAcumulado: number;
  esProyectado: boolean;
}

export interface CashFlowResponse {
  dias: CashFlowDayRow[];
  alertaSaldoNegativo: boolean;
  primerDiaSaldoNegativo: string | null;
}

export type ExpenseCategory =
  | 'ALQUILER' | 'SERVICIOS' | 'SUELDOS' | 'COMPRAS_MERCADERIA'
  | 'IMPUESTOS' | 'MANTENIMIENTO' | 'MARKETING' | 'OTROS';

export interface ExpenseRequest {
  monto: number;
  fecha: string;
  categoria: string;
  proveedorId?: number;
  descripcion: string;
  recurrente?: boolean;
  frecuencia?: string;
}

export const EXPENSE_CATEGORY_LABELS: Record<ExpenseCategory, string> = {
  ALQUILER: 'Alquiler',
  SERVICIOS: 'Servicios',
  SUELDOS: 'Sueldos',
  COMPRAS_MERCADERIA: 'Compras / Mercadería',
  IMPUESTOS: 'Impuestos',
  MANTENIMIENTO: 'Mantenimiento',
  MARKETING: 'Marketing',
  OTROS: 'Otros',
};

// ──────────────────────────────────────────────
// Cuentas por Cobrar (Sprint 10 — US-025)
// ──────────────────────────────────────────────

export type ReceivableEstado = 'PENDIENTE' | 'PARCIAL' | 'COBRADA' | 'VENCIDA' | 'INCOBRABLE';

export interface ReceivableResponse {
  id: number;
  clientId: number;
  clientName: string | null;
  clientDocument: string | null;
  saleId: number;
  montoOriginal: number;
  saldoPendiente: number;
  fechaEmision: string;
  fechaVencimiento: string;
  estado: ReceivableEstado;
  interesesAcumulados: number;
}

export interface ReceivablePaymentResponse {
  id: number;
  receivableId: number;
  monto: number;
  metodoPago: string;
  fecha: string;
  registradoPor: number;
}

export interface ReceivablePaymentRequest {
  monto: number;
  metodoPago: string;
}

// ──────────────────────────────────────────────
// Reporte de Antigüedad (Sprint 10 — US-025)
// ──────────────────────────────────────────────

export interface AgingReportResponse {
  resumenGeneral: {
    corriente: number;
    tramo1a30: number;
    tramo31a60: number;
    tramo61a90: number;
    masDe90: number;
    total: number;
  };
  porCliente: {
    clientId: number;
    clientName: string;
    clientDocument: string | null;
    corriente: number;
    tramo1a30: number;
    tramo31a60: number;
    tramo61a90: number;
    masDe90: number;
    total: number;
  }[];
}

// ──────────────────────────────────────────────
// Cuentas por Pagar (Sprint 10 — US-026)
// ──────────────────────────────────────────────

export type PayableEstado = 'PENDIENTE' | 'PARCIAL' | 'PAGADA' | 'VENCIDA';

export interface PayableResponse {
  id: number;
  supplierId: number;
  supplierName: string | null;
  purchaseOrderId: number | null;
  montoOriginal: number;
  saldoPendiente: number;
  fechaEmision: string;
  fechaVencimiento: string;
  estado: PayableEstado;
  referenciaBancaria: string | null;
}

export interface PayablePaymentResponse {
  id: number;
  payableId: number;
  monto: number;
  metodoPago: string;
  referenciaBancaria: string | null;
  fecha: string;
  registradoPor: number;
}

export interface PayablePaymentRequest {
  monto: number;
  metodoPago: string;
  referenciaBancaria?: string;
}

export interface PayableRequest {
  supplierId: number;
  purchaseOrderId?: number;
  montoOriginal: number;
  fechaEmision: string;
  fechaVencimiento: string;
  referenciaBancaria?: string;
}

// ──────────────────────────────────────────────
// Conciliación Bancaria (Sprint 10 — US-028)
// ──────────────────────────────────────────────

export interface BankReconciliationResponse {
  id?: number;
  periodo: string;
  totalExtracto: number;
  totalSistema: number;
  diferencia: number;
  estado: string;
  totalLineas: number;
  conciliadas: number;
  pendientes: number;
}

export interface BankStatementResponse {
  id: number;
  reconciliationId: number;
  fecha: string;
  descripcion: string;
  monto: number;
  tipo: 'CREDITO' | 'DEBITO';
  estado: 'PENDIENTE' | 'CONCILIADO' | 'AJUSTE_MANUAL';
  paymentId: number | null;
  observacion: string | null;
}

export interface ManualMatchRequest {
  statementId: number;
  paymentId: number;
  tipo: 'RECEIVABLE_PAYMENT' | 'PAYABLE_PAYMENT';
}

export interface CreateExpenseFromStatementRequest {
  statementId: number;
  monto: number;
  categoria: string;
  descripcion: string;
  fecha?: string;
}

// ── Sprint 11: RRHH ──────────────────────────────────────────────

export type ModalidadContrato = 'FULL_TIME' | 'PART_TIME' | 'CONTRATO';

export interface EmployeeRequest {
  nombre: string;
  apellido: string;
  dni: string;
  cuil?: string;
  fechaNacimiento: string;
  fechaIngreso: string;
  cargo: string;
  departamento: string;
  sucursalId?: number;
  salarioBase: number;
  modalidadContrato: ModalidadContrato;
  userId?: number;
}

export interface EmployeeResponse {
  id: number;
  nombre: string;
  apellido: string;
  dni: string;
  cuil: string | null;
  fechaNacimiento: string;
  fechaIngreso: string;
  fechaBaja: string | null;
  cargo: string;
  departamento: string;
  sucursalId: number | null;
  salarioBase: number;
  modalidadContrato: ModalidadContrato;
  userId: number | null;
  activo: boolean;
  documentoUrl: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface EmployeeHistoryResponse {
  id: number;
  employeeId: number;
  campo: 'CARGO' | 'SALARIO' | 'DEPARTAMENTO';
  valorAnterior: string;
  valorNuevo: string;
  fecha: string;
  modificadoPor: number | null;
}

export interface CheckInRequest {
  employeeId: number;
  timestamp: string;
  observacion?: string;
}

export interface CheckOutRequest {
  employeeId: number;
  timestamp: string;
}

export type AttendanceEstado = 'COMPLETO' | 'INCOMPLETO' | 'AUSENCIA';

export interface AttendanceResponse {
  id: number;
  employeeId: number;
  fecha: string;
  horaEntrada: string;
  horaSalida: string | null;
  horasTrabajadasMinutos: number | null;
  horasExtraMinutos: number | null;
  estado: AttendanceEstado;
  observacion: string | null;
  createdAt: string;
}

export type AusenciaTipo = 'JUSTIFICADA' | 'INJUSTIFICADA' | 'LICENCIA' | 'VACACIONES';

export interface AbsenceRequest {
  employeeId: number;
  fecha: string;
  tipo: AusenciaTipo;
  descripcion?: string;
}

export interface AbsenceResponse {
  id: number;
  employeeId: number;
  fecha: string;
  tipo: AusenciaTipo;
  descripcion: string | null;
  aprobadoPor: number | null;
  createdAt: string;
}

export interface AttendanceSummaryResponse {
  employeeId: number;
  mes: number;
  anio: number;
  diasTrabajados: number;
  horasTotalesMinutos: number;
  horasExtraMinutos: number;
  ausenciasJustificadas: number;
  ausenciasInjustificadas: number;
  licencias: number;
  vacaciones: number;
}

export interface ShiftDefinitionRequest {
  nombre: string;
  horaInicio: string;
  horaFin: string;
  diasSemana: number;
  color?: string;
}

export interface ShiftDefinitionResponse {
  id: number;
  nombre: string;
  horaInicio: string;
  horaFin: string;
  diasSemana: number;
  color: string | null;
  activo: boolean;
}

export interface ShiftAssignmentRequest {
  employeeId: number;
  shiftDefinitionId: number;
  semana: string;
  diasActivos: number[];
  sucursalId?: number;
}

export interface ShiftAssignmentResponse {
  id: number;
  employeeId: number;
  employeeName: string;
  shiftDefinitionId: number;
  shiftName: string;
  horaInicio: string;
  horaFin: string;
  semana: string;
  diasActivos: number[];
  sucursalId: number | null;
}

export interface ShiftScheduleResponse {
  semana: string;
  sucursalId: number | null;
  empleados: EmployeeSchedule[];
}

export interface EmployeeSchedule {
  employeeId: number;
  employeeName: string;
  turnos: Record<number, ShiftInfo[]>;
}

export interface ShiftInfo {
  assignmentId: number;
  shiftDefinitionId: number;
  shiftName: string;
  horaInicio: string;
  horaFin: string;
  color: string | null;
}

export interface ShiftChangeRequestDto {
  assignmentId: number;
  fechaOriginal: string;
  motivo: string;
}

export type ShiftChangeEstado = 'PENDIENTE' | 'APROBADO' | 'RECHAZADO';

export interface ShiftChangeRequestResponse {
  id: number;
  employeeId: number;
  assignmentId: number;
  fechaOriginal: string;
  motivo: string;
  estado: ShiftChangeEstado;
  revisadoPor: number | null;
  fechaRevision: string | null;
  createdAt: string;
}

export interface NotificationResponse {
  id: number;
  userId: number;
  titulo: string;
  mensaje: string;
  leido: boolean;
  creadoEn: string;
}

export type PeriodoEvaluacion = 'MENSUAL' | 'TRIMESTRAL' | 'SEMESTRAL' | 'ANUAL';

export interface EvaluationTemplateRequest {
  nombre: string;
  periodo: PeriodoEvaluacion;
  criterios: CriterionRequest[];
}

export interface CriterionRequest {
  nombre: string;
  peso: number;
}

export interface EvaluationTemplateResponse {
  id: number;
  nombre: string;
  periodo: PeriodoEvaluacion;
  activo: boolean;
  criterios: CriterionResponse[];
}

export interface CriterionResponse {
  id: number;
  nombre: string;
  peso: number;
}

export interface CreateEvaluationRequest {
  employeeId: number;
  templateId: number;
  periodo: string;
  fechaEvaluacion: string;
  observaciones?: string;
  scores: ScoreRequest[];
}

export interface ScoreRequest {
  criterionId: number;
  puntuacion: number;
}

export type EvaluationEstado = 'BORRADOR' | 'FINALIZADA';

export interface PerformanceEvaluationResponse {
  id: number;
  employeeId: number;
  templateId: number;
  templateName: string;
  periodo: string;
  fechaEvaluacion: string;
  puntuacionFinal: number | null;
  observaciones: string | null;
  estado: EvaluationEstado;
  evaluadoPor: number | null;
  scores: ScoreResponse[];
  createdAt: string;
}

export interface ScoreResponse {
  id: number;
  criterionId: number;
  criterionName: string;
  puntuacion: number;
}
