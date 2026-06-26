# Frontend — POS System SPA

Single Page Application del sistema de punto de venta construida con React 19 + TypeScript + Vite.

---

## Stack

| Componente | Tecnología |
|------------|-----------|
| **Framework** | React 19 |
| **Language** | TypeScript 5 |
| **Build** | Vite 6 |
| **HTTP** | Axios |
| **Routing** | React Router DOM v7 |
| **Auth** | JWT (access + refresh con auto-refresh interceptor) |
| **Estilos** | Tailwind CSS 4 |
| **Gráficos** | Recharts (dashboards) |
| **Linting** | ESLint (TypeScript + React) |

## Estructura

```
frontend/
├── src/
│   ├── pages/               → 15 páginas
│   ├── components/
│   │   ├── common/          → Layout + ProtectedRoute
│   │   ├── dashboard/       → KpiCard, TopProducts, TopSellers, AlertsBanner
│   │   └── pos/             → ProductSearch, CartView, ClientSelector, PaymentModal,
│   │                           InvoiceTypeSelector, ReceptorForm, CouponInput,
│   │                           LoyaltyWidget, ShiftPanel, ReturnModal, CashFlowChart
│   ├── services/
│   │   ├── api.ts           → 24 módulos API (axios instance + interceptors)
│   │   └── dashboardApi.ts  → 1 módulo dashboard
│   ├── types/
│   │   └── index.ts         → 95 interfaces + 16 type aliases + 3 const maps
│   ├── context/
│   │   └── AuthContext.tsx   → Contexto de autenticación JWT
│   ├── App.tsx              → Router + rutas protegidas
│   └── main.tsx             → Entry point
├── package.json
├── vite.config.ts           → Proxy /api → localhost:8080
├── tsconfig.json
└── README.md
```

## Rutas

| Path | Página | Descripción |
|------|--------|-------------|
| `/login` | LoginPage | Login con email + password |
| `/pos` | PosPage | Pantalla principal de venta (carrito + cobro) |
| `/dashboard` | DashboardPage | Dashboard ejecutivo con KPIs y gráficos |
| `/dashboard/consolidated` | ConsolidatedDashboardPage | Dashboard multi-sucursal consolidado |
| `/reports/sales-advanced` | SalesAdvancedReportPage | Reportes avanzados de ventas |
| `/reports/inventory` | InventoryReportPage | Reporte de inventario |
| `/reports/hr` | HRReportPage | Reporte de RRHH |
| `/reports/profitability` | ProfitabilityPage | Análisis de rentabilidad |
| `/reports/product-analysis` | ProductAnalysisPage | Análisis ABC de productos |
| `/commissions` | CommissionPage | Esquemas, cálculo y ranking de comisiones |
| `/payroll` | PayrollListPage | Listado y filtro de nóminas por período |
| `/payroll/:id` | PayrollDetailPage | Detalle de nómina, PDF, ajustes |
| `/recipes` | RecipePage | Recetas con BOM, explosión, costo estimado |
| `/production-orders` | ProductionOrderPage | Órdenes de producción (crear, listar, start/cancel) |
| `/production-orders/:id` | ProductionOrderDetailPage | Detalle OP, completar con merma, cost analysis, trazabilidad |

## Ejecución

```bash
# Instalar dependencias
pnpm install

# Desarrollo (hot reload en :5173)
pnpm dev

# Build producción
pnpm build

# Preview del build
pnpm preview

# Linter
pnpm lint
```

## Proxy API

El `vite.config.ts` configura un proxy automático:

```
/api/* → http://localhost:8080/api/*
```

Esto evita CORS en desarrollo. El frontend asume que el backend corre en `localhost:8080`.

## Tipos

El archivo `src/types/index.ts` contiene tipado completo del dominio:

### Interfaces principales (95)
- **Auth:** `User`, `AuthResponse`, `BranchInfo`, `LoginCredentials`
- **POS:** `SaleRequest`, `SaleResponse`, `CartItem`, `CartValidationResult`
- **RRHH:** `EmployeeRequest/Response`, `AttendanceResponse`, `ShiftAssignmentResponse`, `PerformanceEvaluationResponse`
- **Comisiones/Nómina:** `CommissionSchemeResponse`, `CommissionResultResponse`, `PayrollResponse`, `PayrollAdjustmentResponse`
- **Producción:** `RecipeRequest/Response`, `BomComponentResponse`, `BomExplosionResponse`, `CostEstimateResponse`, `ProductionOrderRequest/Response`, `CostAnalysisResponse`, `LoteTraceabilityResponse`
- **Dashboards:** KPIs, reportes avanzados, análisis ABC, rentabilidad, inventario
- **Contabilidad:** Cuentas, asientos, balance
- **E-commerce:** Estados de sync
- **Webhooks/API Keys:** Configuraciones y deliveries

### Type aliases (16)
`PaymentMethod`, `CondicionIva`, `ShiftStatus`, `ReturnStatus`, `PromotionType`, `PromotionScope`, `ClientTier`, `ExpenseCategory`, `ReceivableEstado`, `PayableEstado`, `ModalidadContrato`, `AttendanceEstado`, `AusenciaTipo`, `ShiftChangeEstado`, `PeriodoEvaluacion`, `EvaluationEstado`

## API Modules

El archivo `src/services/api.ts` encapsula todas las llamadas HTTP con tipado fuerte (24 módulos) + `dashboardApi.ts` (1 módulo):

| Módulo | Endpoints principales |
|--------|----------------------|
| `authApi` | login, register, refresh, switchBranch |
| `productApi` | search, getAll, getPrice |
| `saleApi` | create, validateCart, getById, getMySales |
| `clientApi` | search, create |
| `shiftApi` | open/close, movements, report |
| `returnApi` | create, approve, reject |
| `promotionApi` | list, create, toggleActive |
| `couponApi` | validate |
| `loyaltyApi` | getPoints, history |
| `recipeApi` | list, getById, create, update, delete, getBomExplosion, getCostEstimate |
| `productionOrderApi` | list, getById, create, start, complete, cancel, getCostAnalysis |
| `lotApi` | getTraceability |
| `commissionApi` | getSchemes, createScheme, calculate, getSummary, getRanking |
| `payrollApi` | list, getById, approve, getPdf, addAdjustment, exportCsv |
| `employeeApi` | getAll, getById, create, update, delete |
| `attendanceApi` | checkIn, checkOut, getAttendance, getSummary |
| `notificationApi` | getUnread, markRead |
| `evaluationApi` | getTemplates, createTemplate, createEvaluation, finalize |
| `reportApi` | salesBook, cashFlow, export |
| `expenseApi` | list, create, update, delete, pay, getSummary |
| `receivableApi` | list, getAgingReport, addPayment |
| `payableApi` | list, getUpcoming, addPayment |
| `bankReconciliationApi` | import, getSummary, autoMatch, manualMatch |
| `employeeShiftApi` | getDefinitions, getSchedule, requestChange |
| `dashboardApi` | getExecutive, getConsolidated |
| *(+ módulos de contabilidad y e-commerce)* | |

## Sidebar — Navegación

```
POS
Dashboard
  ├─ Ejecutivo
  └─ Consolidado
Reportes
  ├─ Ventas Avanzado
  ├─ Inventario
  ├─ RRHH
  ├─ Rentabilidad
  └─ Análisis ABC
Comisiones
Nómina
Recetas
Producción
```

Cada item es un `<NavLink>` con highlighting automático. Nuevos items se agregan en `Layout.tsx`.
