# Sprint 3 - Control de Inventario

**Estado:** Completado ✅  
**Story Points:** 19  
**Duración:** 3 semanas (12 días de desarrollo, 2h/día)

---

## Entregado

### DÍA 1 - Modelos de Datos
- Entidad `StockMovement` con tipos de movimiento (ENTRADA_COMPRA, ENTRADA_DEVOLUCION, SALIDA_VENTA, SALIDA_DEVOLUCION_PROVEEDOR, AJUSTE, TRANSFERENCIA_SALIDA, TRANSFERENCIA_ENTRADA, MERMA)
- Entidad `Warehouse` (bodegas/almacenes)
- Entidad `ProductStock` con stock actual, mínimo, máximo, costo promedio
- Repositorios JPA para cada entidad

### DÍA 2 - Servicios de Stock
- `KardexService`: historial de movimientos, stock por producto/bodega, costo promedio ponderado
- `StockMovementService`: registro de entradas/salidas/ajustes/transferencias/mermas
- DTOs: `KardexResponse`, `StockStatusResponse`, `StockMovementRequest`, `StockAdjustmentRequest`

### DÍA 3 - Alertas de Inventario
- Entidad `Alert` con tipos (STOCK_BAJO, STOCK_CRITICO, SIN_STOCK, PRODUCTO_VENCIDO)
- `AlertService`: generación, resolución, reportes
- `StockAlertScheduler`: tarea programada cada 30 minutos

### DÍA 4 - Queries de Stock
- (Cubierto por endpoints de consulta en KardexController y StockController)

### DÍA 5 - Unidades de Medida
- Entidad `UnitOfMeasure` con factores de conversión
- `UnitConversionService`: conversión entre unidades

### DÍA 6 - Configuración Dinámica
- Entidad `SystemConfiguration` con tipos de dato (STRING, INTEGER, BOOLEAN, DECIMAL, JSON)
- `ConfigurationService` con Spring Cache (@Cacheable/@CacheEvict)

### DÍA 7 - Impuestos
- Entidad `Tax` con tipos (ADDED, INCLUDED, EXEMPT, PERCENTAGE, FIXED)
- `TaxService`: cálculo de impuestos, asignación a productos
- Relación ManyToMany Product-Tax

### DÍA 8 - Controladores REST
- `KardexController`: 6 endpoints
- `StockController`: 7 endpoints
- `AlertController`: 6 endpoints
- `ConfigurationController`: 6 endpoints
- `TaxController`: 8 endpoints
- Documentación Swagger/OpenAPI en todos
- Seguridad por método con @PreAuthorize

### DÍA 9 - Tests Unitarios
- `KardexServiceTest`: 5 tests
- `StockMovementServiceTest`: 5 tests
- `AlertServiceTest`: 7 tests
- `UnitConversionServiceTest`: (pendiente)
- `ConfigurationServiceTest`: 6 tests
- `TaxServiceTest`: 7 tests

---

## Número de Tests
- **Antes del Sprint 3:** 25 tests (2 fallando)
- **Después del Sprint 3:** 53 tests (2 fallando - preexistentes)
- **Tests nuevos:** 30
- **Cobertura nueva:** Servicios de inventario, kardex, alertas, configuración, impuestos

---

## Problemas Técnicos

1. **AuthControllerTest login**: 2 tests fallan por configuración de mock exception en Spring Security filter chain. Preexistente, no bloqueante.
2. **springdoc-openapi**: Tuve que actualizar de 2.7.0 a 2.8.0 por compatibilidad con `LiteWebJarsResourceResolver`.  
3. **H2 para tests**: Base de datos en memoria configurada con application-test.yml para evitar depender de PostgreSQL en tests.
4. **BigDecimal en tests**: Escala inconsistente entre `BigDecimal.valueOf(21)` y `BigDecimal.valueOf(21.00)`. Solución: usar `compareTo()` en lugar de `assertEquals()`.

---

## Lecciones Aprendidas

- El patrón de tests con `@ExtendWith(MockitoExtension.class)` funciona bien para servicios sin contexto Spring
- Spring Cache requiere `@EnableCaching` en la aplicación principal
- El scheduler de stock bajo necesita `@EnableScheduling`
- Lombok `@Builder` necesita `@Builder.Default` en campos con valores iniciales

---

## Próximos Pasos

- Sprint 4: Gestión de Compras y Proveedores
- Fix de 2 tests de AuthController (opcional, no bloqueante)
