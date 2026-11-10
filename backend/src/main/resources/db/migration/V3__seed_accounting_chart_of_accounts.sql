-- ============================================================
-- V3: Plan de cuentas base y templates de asientos
-- Traducción a PostgreSQL del V36 legacy (estaba en MySQL:
-- usaba SET @tpl = LAST_INSERT_ID() que no existe en Postgres).
-- Las columnas activa/created_at/updated_at son NOT NULL en el
-- schema de Hibernate (baseline V1), por eso se setean explícitas.
-- ============================================================

-- 1. PLAN DE CUENTAS BASE (4 niveles jerárquicos)
-- ACTIVO
INSERT INTO accounting_accounts (codigo, nombre, tipo, nivel, activa, created_at, updated_at) VALUES ('1', 'ACTIVO', 'ACTIVO', 1, TRUE, NOW(), NOW());
INSERT INTO accounting_accounts (codigo, nombre, tipo, cuenta_padre_id, nivel, activa, created_at, updated_at) VALUES ('1.1', 'ACTIVO CORRIENTE', 'ACTIVO', (SELECT id FROM accounting_accounts a WHERE a.codigo = '1'), 2, TRUE, NOW(), NOW());
INSERT INTO accounting_accounts (codigo, nombre, tipo, cuenta_padre_id, nivel, activa, created_at, updated_at) VALUES ('1.1.1', 'CAJA', 'ACTIVO', (SELECT id FROM accounting_accounts a WHERE a.codigo = '1.1'), 3, TRUE, NOW(), NOW());
INSERT INTO accounting_accounts (codigo, nombre, tipo, cuenta_padre_id, nivel, activa, created_at, updated_at) VALUES ('1.1.2', 'BANCOS', 'ACTIVO', (SELECT id FROM accounting_accounts a WHERE a.codigo = '1.1'), 3, TRUE, NOW(), NOW());
INSERT INTO accounting_accounts (codigo, nombre, tipo, cuenta_padre_id, nivel, activa, created_at, updated_at) VALUES ('1.1.3', 'CUENTAS POR COBRAR', 'ACTIVO', (SELECT id FROM accounting_accounts a WHERE a.codigo = '1.1'), 3, TRUE, NOW(), NOW());
INSERT INTO accounting_accounts (codigo, nombre, tipo, cuenta_padre_id, nivel, activa, created_at, updated_at) VALUES ('1.1.4', 'CRÉDITO FISCAL IVA', 'ACTIVO', (SELECT id FROM accounting_accounts a WHERE a.codigo = '1.1'), 3, TRUE, NOW(), NOW());
INSERT INTO accounting_accounts (codigo, nombre, tipo, cuenta_padre_id, nivel, activa, created_at, updated_at) VALUES ('1.1.5', 'STOCK MERCADERÍAS', 'ACTIVO', (SELECT id FROM accounting_accounts a WHERE a.codigo = '1.1'), 3, TRUE, NOW(), NOW());
INSERT INTO accounting_accounts (codigo, nombre, tipo, nivel, activa, created_at, updated_at) VALUES ('1.2', 'ACTIVO NO CORRIENTE', 'ACTIVO', 2, TRUE, NOW(), NOW());
INSERT INTO accounting_accounts (codigo, nombre, tipo, cuenta_padre_id, nivel, activa, created_at, updated_at) VALUES ('1.2.1', 'BIENES DE USO', 'ACTIVO', (SELECT id FROM accounting_accounts a WHERE a.codigo = '1.2'), 3, TRUE, NOW(), NOW());

-- PASIVO
INSERT INTO accounting_accounts (codigo, nombre, tipo, nivel, activa, created_at, updated_at) VALUES ('2', 'PASIVO', 'PASIVO', 1, TRUE, NOW(), NOW());
INSERT INTO accounting_accounts (codigo, nombre, tipo, cuenta_padre_id, nivel, activa, created_at, updated_at) VALUES ('2.1', 'PASIVO CORRIENTE', 'PASIVO', (SELECT id FROM accounting_accounts a WHERE a.codigo = '2'), 2, TRUE, NOW(), NOW());
INSERT INTO accounting_accounts (codigo, nombre, tipo, cuenta_padre_id, nivel, activa, created_at, updated_at) VALUES ('2.1.1', 'PROVEEDORES', 'PASIVO', (SELECT id FROM accounting_accounts a WHERE a.codigo = '2.1'), 3, TRUE, NOW(), NOW());
INSERT INTO accounting_accounts (codigo, nombre, tipo, cuenta_padre_id, nivel, activa, created_at, updated_at) VALUES ('2.1.2', 'SUELDOS A PAGAR', 'PASIVO', (SELECT id FROM accounting_accounts a WHERE a.codigo = '2.1'), 3, TRUE, NOW(), NOW());
INSERT INTO accounting_accounts (codigo, nombre, tipo, cuenta_padre_id, nivel, activa, created_at, updated_at) VALUES ('2.1.3', 'CARGAS SOCIALES A PAGAR', 'PASIVO', (SELECT id FROM accounting_accounts a WHERE a.codigo = '2.1'), 3, TRUE, NOW(), NOW());
INSERT INTO accounting_accounts (codigo, nombre, tipo, cuenta_padre_id, nivel, activa, created_at, updated_at) VALUES ('2.1.4', 'IVA DÉBITO FISCAL', 'PASIVO', (SELECT id FROM accounting_accounts a WHERE a.codigo = '2.1'), 3, TRUE, NOW(), NOW());

-- PATRIMONIO
INSERT INTO accounting_accounts (codigo, nombre, tipo, nivel, activa, created_at, updated_at) VALUES ('3', 'PATRIMONIO NETO', 'PATRIMONIO', 1, TRUE, NOW(), NOW());
INSERT INTO accounting_accounts (codigo, nombre, tipo, cuenta_padre_id, nivel, activa, created_at, updated_at) VALUES ('3.1', 'CAPITAL', 'PATRIMONIO', (SELECT id FROM accounting_accounts a WHERE a.codigo = '3'), 2, TRUE, NOW(), NOW());
INSERT INTO accounting_accounts (codigo, nombre, tipo, cuenta_padre_id, nivel, activa, created_at, updated_at) VALUES ('3.2', 'RESULTADOS ACUMULADOS', 'PATRIMONIO', (SELECT id FROM accounting_accounts a WHERE a.codigo = '3'), 2, TRUE, NOW(), NOW());

-- INGRESO
INSERT INTO accounting_accounts (codigo, nombre, tipo, nivel, activa, created_at, updated_at) VALUES ('4', 'INGRESOS', 'INGRESO', 1, TRUE, NOW(), NOW());
INSERT INTO accounting_accounts (codigo, nombre, tipo, cuenta_padre_id, nivel, activa, created_at, updated_at) VALUES ('4.1', 'VENTAS', 'INGRESO', (SELECT id FROM accounting_accounts a WHERE a.codigo = '4'), 2, TRUE, NOW(), NOW());
INSERT INTO accounting_accounts (codigo, nombre, tipo, cuenta_padre_id, nivel, activa, created_at, updated_at) VALUES ('4.1.1', 'VENTAS NETAS', 'INGRESO', (SELECT id FROM accounting_accounts a WHERE a.codigo = '4.1'), 3, TRUE, NOW(), NOW());

-- EGRESO
INSERT INTO accounting_accounts (codigo, nombre, tipo, nivel, activa, created_at, updated_at) VALUES ('5', 'EGRESOS', 'EGRESO', 1, TRUE, NOW(), NOW());
INSERT INTO accounting_accounts (codigo, nombre, tipo, cuenta_padre_id, nivel, activa, created_at, updated_at) VALUES ('5.1', 'GASTOS ADMINISTRATIVOS', 'EGRESO', (SELECT id FROM accounting_accounts a WHERE a.codigo = '5'), 2, TRUE, NOW(), NOW());
INSERT INTO accounting_accounts (codigo, nombre, tipo, cuenta_padre_id, nivel, activa, created_at, updated_at) VALUES ('5.2', 'GASTOS DE VENTA', 'EGRESO', (SELECT id FROM accounting_accounts a WHERE a.codigo = '5'), 2, TRUE, NOW(), NOW());
INSERT INTO accounting_accounts (codigo, nombre, tipo, cuenta_padre_id, nivel, activa, created_at, updated_at) VALUES ('5.3', 'GASTOS DE PERSONAL', 'EGRESO', (SELECT id FROM accounting_accounts a WHERE a.codigo = '5'), 2, TRUE, NOW(), NOW());
INSERT INTO accounting_accounts (codigo, nombre, tipo, cuenta_padre_id, nivel, activa, created_at, updated_at) VALUES ('5.4', 'COSTO DE MERCADERÍAS VENDIDAS', 'EGRESO', (SELECT id FROM accounting_accounts a WHERE a.codigo = '5'), 2, TRUE, NOW(), NOW());

-- 2. TEMPLATES DE ASIENTOS AUTOMÁTICOS
-- Los ids se resuelven por subquery (evento_origen y codigo son UNIQUE).

-- VENTA: Caja DEBE (TOTAL) / Ventas HABER (NETO) / IVA HABER (IVA)
INSERT INTO accounting_entry_templates (evento_origen, nombre, activo, created_at, updated_at) VALUES ('VENTA', 'Asiento de venta', TRUE, NOW(), NOW());
INSERT INTO accounting_entry_template_lines (template_id, cuenta_id, tipo, formula, orden) VALUES
((SELECT id FROM accounting_entry_templates WHERE evento_origen = 'VENTA'), (SELECT id FROM accounting_accounts WHERE codigo = '1.1.1'), 'DEBE', 'TOTAL', 1);
INSERT INTO accounting_entry_template_lines (template_id, cuenta_id, tipo, formula, orden) VALUES
((SELECT id FROM accounting_entry_templates WHERE evento_origen = 'VENTA'), (SELECT id FROM accounting_accounts WHERE codigo = '4.1.1'), 'HABER', 'NETO', 2);
INSERT INTO accounting_entry_template_lines (template_id, cuenta_id, tipo, formula, orden) VALUES
((SELECT id FROM accounting_entry_templates WHERE evento_origen = 'VENTA'), (SELECT id FROM accounting_accounts WHERE codigo = '2.1.4'), 'HABER', 'IVA', 3);

-- NOMINA: Sueldos DEBE (SUELDOS) / Sueldos a Pagar HABER (NETO) / Cargas Sociales HABER (CARGAS_SOCIALES)
INSERT INTO accounting_entry_templates (evento_origen, nombre, activo, created_at, updated_at) VALUES ('NOMINA', 'Asiento de liquidación de nómina', TRUE, NOW(), NOW());
INSERT INTO accounting_entry_template_lines (template_id, cuenta_id, tipo, formula, orden) VALUES
((SELECT id FROM accounting_entry_templates WHERE evento_origen = 'NOMINA'), (SELECT id FROM accounting_accounts WHERE codigo = '5.3'), 'DEBE', 'SUELDOS', 1);
INSERT INTO accounting_entry_template_lines (template_id, cuenta_id, tipo, formula, orden) VALUES
((SELECT id FROM accounting_entry_templates WHERE evento_origen = 'NOMINA'), (SELECT id FROM accounting_accounts WHERE codigo = '2.1.2'), 'HABER', 'NETO', 2);
INSERT INTO accounting_entry_template_lines (template_id, cuenta_id, tipo, formula, orden) VALUES
((SELECT id FROM accounting_entry_templates WHERE evento_origen = 'NOMINA'), (SELECT id FROM accounting_accounts WHERE codigo = '2.1.3'), 'HABER', 'CARGAS_SOCIALES', 3);

-- GASTO: Gasto categoría DEBE (TOTAL) / Caja HABER (TOTAL)
INSERT INTO accounting_entry_templates (evento_origen, nombre, activo, created_at, updated_at) VALUES ('GASTO', 'Asiento de gasto', TRUE, NOW(), NOW());
INSERT INTO accounting_entry_template_lines (template_id, cuenta_id, tipo, formula, orden) VALUES
((SELECT id FROM accounting_entry_templates WHERE evento_origen = 'GASTO'), (SELECT id FROM accounting_accounts WHERE codigo = '5.1'), 'DEBE', 'TOTAL', 1);
INSERT INTO accounting_entry_template_lines (template_id, cuenta_id, tipo, formula, orden) VALUES
((SELECT id FROM accounting_entry_templates WHERE evento_origen = 'GASTO'), (SELECT id FROM accounting_accounts WHERE codigo = '1.1.1'), 'HABER', 'TOTAL', 2);