# Point of Sale — POS System

Sistema de punto de venta completo con backend Spring Boot + frontend React (TypeScript) + app Android (Kotlin Compose).
Incluye facturación electrónica AFIP-style, motor de promociones, fidelización, RRHH (asistencia, turnos, evaluaciones), comisiones, nómina, gestión de materia prima y producción, dashboards analíticos, multi-sucursal, webhooks, módulo contable e integración e-commerce.

---

## Stack

| Capa | Tecnología |
|------|-----------|
| **Backend** | Java 17, Spring Boot 3.3, Spring Security, Spring Data JPA, Flyway, PostgreSQL |
| **Frontend** | React 19, TypeScript 5, Vite, Axios, Tailwind CSS 4 |
| **Android** | Kotlin 2.2, Jetpack Compose, Hilt, Retrofit, Room 2.7, CameraX 1.3, ML Kit Barcode, WorkManager 2.9, Vico Charts |
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
│   ├── app/src/main/java/com/pos/android/
│   │   ├── MainActivity.kt       → NavHost + BottomNavBar + badge notificaciones
│   │   ├── PosApplication.kt     → Hilt + WorkManager
│   │   ├── Routes.kt             → rutas centralizadas
│   │   ├── auth/                 → login, JWT refresh, TokenRefreshApi, selector sucursal
│   │   ├── inventory/            → productos, búsqueda, detalle, escáner ML Kit
│   │   ├── pos/                  → carrito persistente, cobro offline, cola sync
│   │   ├── attendance/           → check-in/out, resumen asistencias
│   │   ├── shifts/               → grilla semanal de turnos
│   │   ├── dashboard/            → KPI cards, chart Vico, top 5, alertas, caché offline
│   │   ├── notification/         → polling HTTP 30s, mark-as-read, badge contador
│   │   └── core/
│   │       ├── network/          → Retrofit, AuthInterceptor, TokenAuthenticator, dos OkHttpClients
│   │       ├── database/         → Room (ProductEntity, CartItemEntity, PendingSaleEntity, DashboardCacheEntity, NotificationEntity)
│   │       ├── sync/             → SyncWorker, SyncScheduler (WorkManager)
│   │       ├── security/         → EncryptedSharedPreferences
│   │       └── ui/               → theme, BottomNavBar
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

Abrir con Android Studio y sync Gradle (o generar el wrapper con `gradle wrapper`).

La app apunta a `http://10.0.2.2:8080/api/` en el emulador (debug) y `https://api.pos.com/api/` en release (configurable por buildType en `app/build.gradle.kts`).

**Features Android:**
- Login con JWT + refresh automático (dos OkHttpClients para evitar ciclo Hilt) + selector de sucursal
- POS con carrito persistente (Room), búsqueda de productos, escáner ML Kit (CameraX), cobro multipago con cálculo de vuelto
- Modo offline: ventas se guardan en cola (Room) y se sincronizan con WorkManager (periódico cada 15 min + inmediato)
- Dashboard ejecutivo offline-first: KPI cards, chart de ventas (Vico), top 5 productos, alertas activas, selector de período/sucursal, PullToRefreshBox
- Centro de notificaciones con polling HTTP cada 30s, mark-as-read, badge contador en bottom nav
- Asistencia: check-in/out con resumen de período (asistencias, ausencias, tardanzas, horas totales)
- Turnos: grilla semanal por empleado con cards de turno por día
- Stock: búsqueda con paginación, detalle de producto con precios por sucursal y promociones
- Bottom navigation con badge de ventas pendientes + notificaciones sin leer

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
| **A1** | Android: proyecto Gradle con Hilt, Retrofit, Room, CameraX, ML Kit. Login JWT + refresh + BranchSelector. Productos (búsqueda local+remota, detalle con precio resuelto). Escáner ML Kit. Navegación con rutas centralizadas | AuthResponse, BranchInfo, ProductEntity, ProductSearchResponse, BarcodeScannerScreen |
| **A2** | Android: POS con carrito persistente (Room), cobro multipago (efectivo/tarjeta/mixto) con vuelto, ventas offline con cola de sincronización (WorkManager 15 min + 3 reintentos). Asistencia (check-in/out + resumen). Turnos (grilla semanal). BottomNavBar con badge de pendientes | CartItemEntity, PendingSaleEntity, SyncWorker, SyncScheduler, ConnectivityObserver, PosScreen, PaymentScreen, AttendanceScreen, ShiftScreen, BottomNavBar |
| **A3** | Android: Dashboard offline-first con KPI cards, gráfico Vico, top 5, alertas, PullToRefreshBox. Centro de notificaciones con polling 30s + badge. TokenRefreshApi separada. Hilt 2.58 + Room 2.7.1 (compat Kotlin 2.2.10). Vico 2.0.3 (stable). Compose BOM 2025.01.00. compileSdk 35 | DashboardScreen, DashboardViewModel, DashboardRepository, DashboardApi, DashboardCacheEntity, NotificationScreen, NotificationViewModel, NotificationRepository, NotificationApi, NotificationEntity, TokenRefreshApi |

## Licencia

Proyecto privado — uso interno.
