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
| **Linting** | ESLint (TypeScript + React) |

## Estructura

```
frontend/
├── src/
│   ├── pages/               → 8 páginas
│   │   ├── LoginPage.tsx
│   │   ├── PosPage.tsx
│   │   ├── CommissionPage.tsx
│   │   ├── PayrollListPage.tsx
│   │   ├── PayrollDetailPage.tsx
│   │   ├── RecipePage.tsx
│   │   ├── ProductionOrderPage.tsx
│   │   └── ProductionOrderDetailPage.tsx
│   ├── components/
│   │   ├── common/
│   │   │   ├── Layout.tsx          → Sidebar + header + main
│   │   │   └── ProtectedRoute.tsx  → Guard de autenticación
│   │   └── pos/
│   │       ├── ProductSearch.tsx
│   │       ├── CartView.tsx
│   │       ├── ClientSelector.tsx
│   │       ├── PaymentModal.tsx
│   │       ├── CouponInput.tsx
│   │       ├── InvoiceTypeSelector.tsx
│   │       ├── ReceptorForm.tsx
│   │       ├── LoyaltyWidget.tsx
│   │       ├── ShiftPanel.tsx
│   │       ├── ReturnModal.tsx
│   │       └── CashFlowChart.tsx
│   ├── services/
│   │   ├── api.ts           → 24 módulos API (axios instance + interceptors)
│   │   └── api.ts           → authApi, productApi, saleApi, clientApi,
│   │                           shiftApi, returnApi, promotionApi, couponApi,
│   │                           loyaltyApi, reportApi, expenseApi, receivableApi,
│   │                           payableApi, bankReconciliationApi, employeeApi,
│   │                           attendanceApi, notificationApi, evaluationApi,
│   │                           commissionApi, payrollApi, recipeApi,
│   │                           productionOrderApi, lotApi
│   ├── types/
│   │   └── index.ts         → 93 interfaces + 16 type aliases + 3 const maps
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

### Interfaces principales
- **Auth:** `User`, `AuthResponse`, `LoginCredentials`
- **POS:** `SaleRequest`, `SaleResponse`, `CartItem`, `CartValidationResult`
- **RRHH:** `EmployeeRequest/Response`, `AttendanceResponse`, `ShiftAssignmentResponse`, `PerformanceEvaluationResponse`
- **Comisiones/Nómina:** `CommissionSchemeResponse`, `CommissionResultResponse`, `PayrollResponse`, `PayrollAdjustmentResponse`
- **Producción:** `RecipeRequest/Response`, `BomComponentResponse`, `BomExplosionResponse`, `CostEstimateResponse`, `ProductionOrderRequest/Response`, `CostAnalysisResponse`, `LoteTraceabilityResponse`

### Type aliases
`PaymentMethod`, `CondicionIva`, `ShiftStatus`, `ReturnStatus`, `PromotionType`, `PromotionScope`, `ClientTier`, `ExpenseCategory`, `ModalidadContrato`, `AttendanceEstado`, `AusenciaTipo`, `EvaluationEstado`, entre otros.

## API Modules

El archivo `src/services/api.ts` encapsula todas las llamadas HTTP con tipado fuerte:

| Módulo | Endpoints principales |
|--------|----------------------|
| `authApi` | login, register, refresh |
| `productApi` | search, getAll |
| `saleApi` | create, validateCart, getById, getMySales |
| `clientApi` | search, create |
| `promotionApi` | list, create, toggleActive |
| `couponApi` | validate |
| `loyaltyApi` | getPoints, history |
| `recipeApi` | list, getById, create, update, delete, getBomExplosion, getCostEstimate |
| `productionOrderApi` | list, getById, create, start, complete, cancel, getCostAnalysis |
| `lotApi` | getTraceability |
| `commissionApi` | getSchemes, createScheme, calculate, getSummary, getRanking |
| `payrollApi` | list, getById, approve, getPdf, addAdjustment, exportCsv |
| `employeeApi` | getAll, getById, create, update, delete |
| *(+12 módulos más)* | |

## Sidebar — Navegación

```
POS
Comisiones
Nómina
Recetas
Producción
```

Cada item es un `<NavLink>` con highlighting automático. Nuevos items se agregan en `Layout.tsx`.
