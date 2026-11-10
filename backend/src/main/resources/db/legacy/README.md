# Migraciones Legacy (V6–V37)

Estos archivos son el resultado de 18 sprints de migraciones escritas para **MySQL**
(sintaxis `AUTO_INCREMENT`, `DATETIME`, `SET @var = LAST_INSERT_ID()`, etc.) y con
`CREATE TABLE` que **duplicaría** las tablas que Hibernate crea.

## ¿Por qué están acá y no en `migration/`?

1. **Nunca se ejecutaron en ninguna base real.** El backend booteaba con
   `ddl-auto: create` (o un bootstrap one-off), generando el schema desde las
   entidades JPA. Las migraciones V6–V37 quedaron como documentación muerta.

2. **Referenciaban V1–V5 que no existían.** Los FKs a `users`/`products`/etc.
   asumían tablas base que nunca se crearon vía Flyway.

3. **Son incompatibles con el baseline actual.** V1 (baseline) crea las 93 tablas
   del schema real (PostgreSQL, generado por Hibernate). Si V6–V37 corrieran,
   fallarían por tablas duplicadas y sintaxis MySQL.

4. **Flyway escanea `classpath:db/migration`.** Al moverlas a `db/legacy`, Flyway
   las ignora. El orden de migración activo es:

   - `V1__baseline_schema.sql` — schema completo (pg_dump del schema de Hibernate)
   - `V2__seed_core_data.sql` — sucursal principal + bodega principal
   - `V3__seed_accounting_chart_of_accounts.sql` — plan de cuentas + templates

## Si algún día se necesita

El contenido de V6–V37 puede servir como **historial de features** (sprints 6–18):
ventas, clientes, inventario, RRHH, contabilidad, e-commerce, etc. Si se quiere
recuperar un seed puntual (ej. el plan de cuentas de V36), el equivalente en
PostgreSQL ya está en `V3`.
