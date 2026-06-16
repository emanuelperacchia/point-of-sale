# Backend — POS System API

API REST del sistema de punto de venta construida con Spring Boot 3.3 y Java 17.

---

## Stack

| Componente | Tecnología |
|------------|-----------|
| **Framework** | Spring Boot 3.3.7 |
| **Runtime** | Java 17 (OpenJDK) |
| **Database** | PostgreSQL 15+ |
| **ORM** | Spring Data JPA + Hibernate 6 |
| **Migrations** | Flyway (`ddl-auto: validate`) |
| **Security** | Spring Security + JWT (access 15min / refresh 7d) |
| **Validation** | Jakarta Validation |
| **Testing** | JUnit 5 + Mockito |
| **Build** | Maven (`.mvnw` wrapper incluido) |
| **Fiscal** | Mock AFIP-style (factura A/B/C + Boleta, PDF, QR, XML) |

## Estructura

```
backend/
├── src/main/java/com/pos/system/
│   ├── controller/       → 38 controladores REST
│   ├── service/           → 57 servicios + impl/ + promotion/
│   ├── repository/        → 30+ repositorios JPA
│   ├── entity/            → 81 entidades JPA
│   ├── dto/
│   │   ├── request/       → DTOs de entrada
│   │   └── response/      → DTOs de salida
│   ├── config/            → SecurityConfig, CorsConfig, etc.
│   ├── exception/         → BadRequestException, ResourceNotFoundException
│   └── security/          → JwtFilter, UserDetailsServiceImpl
├── src/main/resources/
│   ├── application.yml    → Config principal
│   ├── application-dev.yml
│   └── db/migration/      → 23 migrations Flyway (V6–V28)
├── src/test/              → 461 tests unitarios
├── pom.xml
└── HELP.md
```

## Configuración

### application.yml (valores clave)

| Propiedad | Valor | Nota |
|-----------|-------|------|
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/pos_db` | |
| `spring.datasource.username` | `pos_user` | |
| `spring.datasource.password` | `${DB_PASSWORD}` | Variable de entorno |
| `spring.jpa.hibernate.ddl-auto` | `validate` | Flyway gestiona el schema |
| `spring.jpa.open-in-view` | `false` | Evita LazyInitializationException |
| `server.port` | `8080` | |
| `jwt.secret` | `${JWT_SECRET}` | Variable de entorno |
| `jwt.expiration` | `900000` | 15 min |
| `jwt.refresh-expiration` | `604800000` | 7 días |
| `fiscal.mode` | `mock` | Simulación AFIP |
| `returns.auto-approve-limit` | `5000` | Auto-aprobación ≤ $5000 |

### Ejecución

```bash
# Desarrollo
$env:JWT_SECRET="mi-secreto"
$env:DB_PASSWORD="mi-pass"
./mvnw spring-boot:run

# Tests
./mvnw test

# Build
./mvnw clean package -DskipTests
```

## Endpoints principales

### Auth
| Método | Path | Descripción |
|--------|------|-------------|
| POST | `/api/auth/login` | Login con email + password |
| POST | `/api/auth/register` | Registro de usuario |
| POST | `/api/auth/refresh` | Refrescar token |

### POS
| Método | Path | Descripción |
|--------|------|-------------|
| GET | `/api/products/search?q=` | Búsqueda de productos para POS |
| POST | `/api/sales` | Crear venta |
| GET | `/api/sales/{id}` | Obtener venta |
| POST | `/api/sales/validate-cart` | Validar stock del carrito |

### Producción (Sprint 13)
| Método | Path | Descripción |
|--------|------|-------------|
| GET | `/api/recipes` | Listar recetas |
| POST | `/api/recipes` | Crear receta con BOM |
| GET | `/api/recipes/{id}/bom-explosion` | Explosión recursiva de materiales |
| GET | `/api/recipes/{id}/cost-estimate` | Costo estimado de producción |
| GET | `/api/production-orders` | Listar órdenes de producción |
| POST | `/api/production-orders` | Crear orden |
| POST | `/api/production-orders/{id}/start` | Iniciar (reserva stock) |
| POST | `/api/production-orders/{id}/complete` | Completar (consume stock, genera lote) |
| POST | `/api/production-orders/{id}/cancel` | Cancelar (libera reserva) |
| GET | `/api/production-orders/{id}/cost-analysis` | Análisis de costos (estimado vs real) |
| GET | `/api/lots/{numeroLote}/traceability` | Trazabilidad de lote |

### RRHH (Sprint 11–12)
| Método | Path | Descripción |
|--------|------|-------------|
| GET | `/api/employees` | Listar empleados (con filtros) |
| POST | `/api/attendance/check-in` | Marcar entrada |
| POST | `/api/attendance/check-out` | Marcar salida |
| GET | `/api/shifts/schedule` | Grilla de turnos semanal |
| POST | `/api/evaluations` | Evaluación de desempeño |
| POST | `/api/commissions/calculate` | Calcular comisiones |
| GET | `/api/commissions/summary` | Resumen de comisión |
| GET | `/api/commissions/ranking` | Ranking de vendedores |
| GET | `/api/payroll` | Listar nóminas |
| POST | `/api/payroll/{id}/approve` | Aprobar liquidación |
| GET | `/api/payroll/{id}/pdf` | Descargar recibo PDF |

## Migraciones Flyway

```
V6   → Stock
V7   → Clientes
V8   → Ventas
V9   → Facturación
V10  → Constraints + versión factura
V11  → Turnos de caja
V12  → Devoluciones
V13  → Promociones + cupones
V14  → Fidelización (puntos + tiers)
V15  → Gastos
V16  → Cuentas por cobrar
V17  → Cuentas por pagar
V18  → Conciliación bancaria
V19  → Empleados
V20  → Asistencia
V21  → Turnos RRHH
V22  → Evaluaciones
V23  → Comisiones
V24  → Nómina
V25  → Product.tipo, costo_produccion, stock_reservado
V26  → Recetas + BOM
V27  → Órdenes de producción
V28  → Lotes de producción
```

## Tests

```
461 tests — 0 failures — 0 errors
```

- **Framework:** JUnit 5 + Mockito con `@ExtendWith(MockitoExtension.class)`
- **Patrón:** Mock repositories + `@BeforeEach` setup del service con mocked dependencies
- **Cobertura:** Todos los servicios core tienen test suite completo

### Tests por sprint
| Sprint | Tests | Servicios cubiertos |
|--------|-------|---------------------|
| 1–4 | 30+ | Product, Supplier, PurchaseOrder, GoodsReceipt, StockMovement, Kardex |
| 5 | 25+ | Sale, Client, Tax, Invoice, XmlBuilder, MockFiscalApi |
| 6 | 15+ | Invoice, XmlBuilder, DigitalSignature |
| 7 | 25+ | CashShift, SaleReturn |
| 8 | 30+ | Promotion, Loyalty |
| 9 | 40+ | Expense, SalesBook, CashFlow, ExcelExport |
| 10 | 45+ | Receivable, Payable, Reconciliation, AgingReport, BankStatementImport |
| 11 | 35+ | Employee, Attendance, Shift, Evaluation |
| 12 | 25+ | Commission, PayrollCalculator, Payroll |
| 13 | 35+ | BomExplosion, ProductionOrder, CostAnalysis |
