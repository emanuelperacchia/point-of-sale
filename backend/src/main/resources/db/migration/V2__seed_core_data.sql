-- ============================================================
-- V2: Seed de datos core (sucursal + bodega)
-- Reemplaza los INSERTs de V6 (warehouse) y V29 (branch) legacy,
-- que estaban escritos para MySQL y nunca se ejecutaron.
-- Corre DESPUÉS del baseline V1 (schema de Hibernate).
-- ============================================================

-- 1. Sucursal principal por defecto (equivalente al INSERT de V29 legacy)
INSERT INTO branches (id, nombre, direccion, activa, timezone, punto_venta_fiscal, created_at, updated_at)
VALUES (1, 'Sucursal Principal', 'Direccion Principal', TRUE, 'America/Argentina/Buenos_Aires', '00001', NOW(), NOW());

-- 2. Bodega principal (equivalente al INSERT de V6 legacy)
-- warehouses.branch_id es NOT NULL en el schema de Hibernate
INSERT INTO warehouses (code, name, type, active, branch_id, created_at, updated_at)
VALUES ('MAIN', 'Bodega Principal', 'PRINCIPAL', TRUE, 1, NOW(), NOW());