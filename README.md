# Point of Sale — POS System

Sistema de punto de venta completo con backend Spring Boot + frontend React (TypeScript) + app Android (Kotlin Compose).
Incluye facturación electrónica AFIP-style, motor de promociones, fidelización, RRHH (asistencia, turnos, evaluaciones), comisiones, nómina, gestión de materia prima y producción, dashboards analíticos, multi-sucursal, webhooks, módulo contable e integración e-commerce.

---

## Stack

| Capa | Tecnología |
|------|-----------|
| **Backend** | Java 17, Spring Boot 3.3, Spring Security, Spring Data JPA, Flyway, PostgreSQL |
| **Frontend** | React 19, TypeScript 5, Vite, Axios, Tailwind CSS 4 |
| **Android** | Kotlin 1.9+, Jetpack Compose, Hilt, Retrofit, Room, CameraX, ML Kit |
| **Build** | Maven Wrapper (backend), pnpm (frontend), Gradle (Android) |
| **Testing** | JUnit 5, Mockito, Spring MockMvc (back: 484 tests) |
| **Auth** | JWT (access 15 min + refresh 7 días) + API Key (pública) |

## Estructura

```
point-of-sale/
├── backend/           → API REST (Spring Boot)
│   ├── src/
│   │   ├── main/java/com/pos/system/
│   │   │   ├── controller/     → 58 endpoints REST
│   │   │   ├── service/        → 103 servicios + subpaquetes
│   │   │   ├── repository/     → 84 repositorios JPA
│   │   │   ├── entity/         → 100 entidades JPA
│   │   │   ├── dto/            → request/response DTOs
│   │   │   ├── config/         → seguridad, CORS, webhooks, etc.
│   │   │   └── exception/      → manejo de errores
│   │   ├── main/resources/
│   │   │   └── db/migration/   → 32 migrations Flyway (V6–V37)
│   │   └── test/               → 484 tests unitarios
│   ├── pom.xml
│   └── HELP.md
├── frontend/          → SPA (React + Vite)
│   ├── src/
│   │   ├── pages/              → 15 páginas
│   │   ├── components/         → 17 componentes (POS + comunes + dashboards)
│   │   ├── services/           → 25 módulos API (Axios)
│   │   ├── types/              → 95 interfaces + 16 type aliases
│   │   └── context/            → AuthContext (JWT)
│   ├── package.json
│   └── vite.config.ts
├── android/           → App nativa (Kotlin + Compose)
├── .gitignore
└── README.md
```

## Requisitos

- **Java 17+** (OpenJDK recomendado)
- **Node.js 18+** y **pnpm** (`npm install -g pnpm`)
- **PostgreSQL 15+**
- **Maven** (o usar `./mvnw` incluido)
- **Android Studio Hedgehog+** (para Android)

## Configuración rápida

### 1. Base de datos

```bash
createdb pos_db
```

### 2. Backend

```bash
cd backend

# Configurar variables de entorno
$env:JWT_SECRET="tu-secreto-jwt"
$env:DB_PASSWORD="tu-contraseña"

# Iniciar (con Flyway: ddl-auto=validate)
./mvnw spring-boot:run
```

La API corre en `http://localhost:8080`.

### 3. Frontend

```bash
cd frontend
pnpm install
pnpm dev
```

El frontend corre en `http://localhost:5173` con proxy automático al backend (`/api → localhost:8080`).

### 4. Android

```bash
cd android
```

Abrir con Android Studio y sync Gradle. La app apunta a `http://10.0.2.2:8080/api/` en el emulador (configurable en `build.gradle.kts`).

## Variables de Entorno

| Variable | Obligatoria | Default | Descripción |
|----------|------------|---------|-------------|
| `JWT_SECRET` | ✅ | — | Clave secreta para firmar tokens JWT |
| `DB_PASSWORD` | ✅ | — | Contraseña de PostgreSQL |
| `CORS_ORIGINS` | ❌ | `http://localhost:5173` | Orígenes CORS permitidos |

## Roles del Sistema

| Rol | Acceso |
|-----|--------|
| `ADMIN` | Administración completa del sistema |
| `GERENTE` | Reportes, promociones, aprobar devoluciones, RRHH |
| `CAJERO` | POS, cobros, devoluciones, turnos de caja |
| `VENDEDOR` | Consultas, clientes, comisiones |
| `INVENTARIO` | Gestión de stock, productos, recetas, producción |
| `CONTADOR` | Reportes fiscales, libro IVA, cuentas contables |
| `COMPRAS_*` | Permisos granulares para ordenes de compra y proveedores |

## Funcionalidades por Sprint

| Sprint | Features | Entidades principales |
|--------|----------|----------------------|
| **1–4** | Fundación: auth JWT, productos, categorías, proveedores, compras, recepción de mercadería, stock, kárdex | User, Role, Product, Category, Supplier, PurchaseOrder, GoodsReceipt, StockMovement |
| **5** | POS core: carrito, cobro multipago, selección de cliente, cálculo de IVA, facturación electrónica AFIP-style (PDF + QR), turno de caja | Sale, SaleItem, Payment, InvoiceDocument, CashShift |
| **6** | Facturación: Factura A/B/C y Boleta, CAE mock, PDF, QR, XML AFIP, certificados digitales, reintentos | InvoiceDocument, DigitalCertificate, Tax |
| **7** | Turnos de caja (apertura/cierre, movimientos, diferencia), devoluciones (auto-aprobación ≤$5000, aprobación manual) | ShiftMovement, SaleReturn, ReturnItem |
| **8** | Promociones automáticas (% fijo, monto fijo, 2x1, 3x2, compra X lleva Y), cupones descuento, fidelización (puntos + tiers BRONCE/PLATA/ORO) | Promotion, Coupon, CouponUsage, PointsTransaction, ClientTier |
| **9** | Reportes fiscales: libro IVA ventas, flujo de caja, gastos, exportación Excel | SalesBookReport, Expense, CashFlow |
| **10** | Cuentas por cobrar (intereses, vencimiento), cuentas por pagar, conciliación bancaria, reporte de antigüedad | Receivable, Payable, BankReconciliation |
| **11** | RRHH: empleados, asistencia, turnos (solapamiento), solicitudes de cambio, evaluaciones de desempeño | Employee, AttendanceRecord, ShiftAssignment, ShiftChangeRequest, PerformanceEvaluation |
| **12** | Comisiones por venta (porcentaje/escalonado), nómina (cálculo con descuentos, ajustes, PDF recibo, exportación bancaria) | CommissionScheme, CommissionTier, CommissionResult, Payroll, PayrollAdjustment |
| **13** | Materia prima y producción: recetas con BOM, explosión recursiva de materiales, detección de ciclos, órdenes de producción (planificar/iniciar/completar/cancelar), reserva y consumo de stock, cálculo de costos (estimado vs real), trazabilidad por lote | Recipe, BomComponent, ProductionOrder, ProductionOrderComponent, LoteProduccion |
| **14** | Analytics: dashboard ejecutivo (CompletableFuture + @Cacheable), reportes avanzados de ventas (comparativa período anterior, por hora/día/pago), análisis ABC con Pareto 80/20 dinámico, reporte de inventario (valorización + movimientos), rentabilidad (márgenes + punto equilibrio), RRHH (ausentismo + productividad vendedores). Frontend con Recharts. | DashboardService, SalesReportService, ProductAnalysisService, InventoryReportService, ProfitabilityService, HRReportService |
| **15** | Multi-sucursal: BranchContextHolder, BranchContextFilter, BranchPriceService, stock por sucursal + transferencias, consolidación, backups programados, WebSocket STOMP, exportación masiva, frontend multi-sucursal | Branch, BranchPrice, Transfer, StockTransfer, BulkExportJob |
| **16** | Precios por sucursal refinados (precioResuelto en sale_items), alertas de stock configurables, logs del sistema con Sentry | PriceResolutionResponse, BranchAlert, SystemLog |
| **17** | API pública para terceros (API Key SHA-256 + rate limiting Bucket4j), webhooks (eventos VENTA_CREADA, STOCK_ACTUALIZADO, etc.), endpoints públicos de productos/ventas/clientes | ApiKey, WebhookConfig, WebhookDelivery, PublicProductController, PublicSaleController, PublicClientController |
| **18** | Módulo contable (plan de cuentas jerárquico, asientos automáticos por venta/nómina/gasto, balance de comprobación, exportación Excel), integración e-commerce (EcommerceAdapter REST genérico, sincronización stock/catálogo/pedidos @Scheduled cada 5 min) | AccountingAccount, AccountingEntryTemplate, AccountingJournalEntry, AccountingJournalLine, EcommerceConfig, EcommerceOrder, EcommerceSyncLog, PaymentMethod.ONLINE |

## Licencia

Proyecto privado — uso interno.
