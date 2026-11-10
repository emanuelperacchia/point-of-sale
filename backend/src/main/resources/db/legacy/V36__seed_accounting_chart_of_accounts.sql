-- ============================================================
-- SPRINT 18 — US-055: Plan de cuentas base y templates
-- ============================================================

-- 1. PLAN DE CUENTAS BASE (4 niveles jerárquicos)
-- ACTIVO
INSERT INTO accounting_accounts (codigo, nombre, tipo, nivel) VALUES ('1', 'ACTIVO', 'ACTIVO', 1);
INSERT INTO accounting_accounts (codigo, nombre, tipo, cuenta_padre_id, nivel) VALUES ('1.1', 'ACTIVO CORRIENTE', 'ACTIVO', 1, 2);
INSERT INTO accounting_accounts (codigo, nombre, tipo, cuenta_padre_id, nivel) VALUES ('1.1.1', 'CAJA', 'ACTIVO', (SELECT id FROM accounting_accounts a WHERE a.codigo = '1.1'), 3);
INSERT INTO accounting_accounts (codigo, nombre, tipo, cuenta_padre_id, nivel) VALUES ('1.1.2', 'BANCOS', 'ACTIVO', (SELECT id FROM accounting_accounts a WHERE a.codigo = '1.1'), 3);
INSERT INTO accounting_accounts (codigo, nombre, tipo, cuenta_padre_id, nivel) VALUES ('1.1.3', 'CUENTAS POR COBRAR', 'ACTIVO', (SELECT id FROM accounting_accounts a WHERE a.codigo = '1.1'), 3);
INSERT INTO accounting_accounts (codigo, nombre, tipo, cuenta_padre_id, nivel) VALUES ('1.1.4', 'CRÉDITO FISCAL IVA', 'ACTIVO', (SELECT id FROM accounting_accounts a WHERE a.codigo = '1.1'), 3);
INSERT INTO accounting_accounts (codigo, nombre, tipo, cuenta_padre_id, nivel) VALUES ('1.1.5', 'STOCK MERCADERÍAS', 'ACTIVO', (SELECT id FROM accounting_accounts a WHERE a.codigo = '1.1'), 3);
INSERT INTO accounting_accounts (codigo, nombre, tipo, nivel) VALUES ('1.2', 'ACTIVO NO CORRIENTE', 'ACTIVO', 2);
INSERT INTO accounting_accounts (codigo, nombre, tipo, cuenta_padre_id, nivel) VALUES ('1.2.1', 'BIENES DE USO', 'ACTIVO', (SELECT id FROM accounting_accounts a WHERE a.codigo = '1.2'), 3);

-- PASIVO
INSERT INTO accounting_accounts (codigo, nombre, tipo, nivel) VALUES ('2', 'PASIVO', 'PASIVO', 1);
INSERT INTO accounting_accounts (codigo, nombre, tipo, cuenta_padre_id, nivel) VALUES ('2.1', 'PASIVO CORRIENTE', 'PASIVO', (SELECT id FROM accounting_accounts a WHERE a.codigo = '2'), 2);
INSERT INTO accounting_accounts (codigo, nombre, tipo, cuenta_padre_id, nivel) VALUES ('2.1.1', 'PROVEEDORES', 'PASIVO', (SELECT id FROM accounting_accounts a WHERE a.codigo = '2.1'), 3);
INSERT INTO accounting_accounts (codigo, nombre, tipo, cuenta_padre_id, nivel) VALUES ('2.1.2', 'SUELDOS A PAGAR', 'PASIVO', (SELECT id FROM accounting_accounts a WHERE a.codigo = '2.1'), 3);
INSERT INTO accounting_accounts (codigo, nombre, tipo, cuenta_padre_id, nivel) VALUES ('2.1.3', 'CARGAS SOCIALES A PAGAR', 'PASIVO', (SELECT id FROM accounting_accounts a WHERE a.codigo = '2.1'), 3);
INSERT INTO accounting_accounts (codigo, nombre, tipo, cuenta_padre_id, nivel) VALUES ('2.1.4', 'IVA DÉBITO FISCAL', 'PASIVO', (SELECT id FROM accounting_accounts a WHERE a.codigo = '2.1'), 3);

-- PATRIMONIO
INSERT INTO accounting_accounts (codigo, nombre, tipo, nivel) VALUES ('3', 'PATRIMONIO NETO', 'PATRIMONIO', 1);
INSERT INTO accounting_accounts (codigo, nombre, tipo, cuenta_padre_id, nivel) VALUES ('3.1', 'CAPITAL', 'PATRIMONIO', (SELECT id FROM accounting_accounts a WHERE a.codigo = '3'), 2);
INSERT INTO accounting_accounts (codigo, nombre, tipo, cuenta_padre_id, nivel) VALUES ('3.2', 'RESULTADOS ACUMULADOS', 'PATRIMONIO', (SELECT id FROM accounting_accounts a WHERE a.codigo = '3'), 2);

-- INGRESO
INSERT INTO accounting_accounts (codigo, nombre, tipo, nivel) VALUES ('4', 'INGRESOS', 'INGRESO', 1);
INSERT INTO accounting_accounts (codigo, nombre, tipo, cuenta_padre_id, nivel) VALUES ('4.1', 'VENTAS', 'INGRESO', (SELECT id FROM accounting_accounts a WHERE a.codigo = '4'), 2);
INSERT INTO accounting_accounts (codigo, nombre, tipo, cuenta_padre_id, nivel) VALUES ('4.1.1', 'VENTAS NETAS', 'INGRESO', (SELECT id FROM accounting_accounts a WHERE a.codigo = '4.1'), 3);

-- EGRESO
INSERT INTO accounting_accounts (codigo, nombre, tipo, nivel) VALUES ('5', 'EGRESOS', 'EGRESO', 1);
INSERT INTO accounting_accounts (codigo, nombre, tipo, cuenta_padre_id, nivel) VALUES ('5.1', 'GASTOS ADMINISTRATIVOS', 'EGRESO', (SELECT id FROM accounting_accounts a WHERE a.codigo = '5'), 2);
INSERT INTO accounting_accounts (codigo, nombre, tipo, cuenta_padre_id, nivel) VALUES ('5.2', 'GASTOS DE VENTA', 'EGRESO', (SELECT id FROM accounting_accounts a WHERE a.codigo = '5'), 2);
INSERT INTO accounting_accounts (codigo, nombre, tipo, cuenta_padre_id, nivel) VALUES ('5.3', 'GASTOS DE PERSONAL', 'EGRESO', (SELECT id FROM accounting_accounts a WHERE a.codigo = '5'), 2);
INSERT INTO accounting_accounts (codigo, nombre, tipo, cuenta_padre_id, nivel) VALUES ('5.4', 'COSTO DE MERCADERÍAS VENDIDAS', 'EGRESO', (SELECT id FROM accounting_accounts a WHERE a.codigo = '5'), 2);

-- 2. TEMPLATES DE ASIENTOS AUTOMÁTICOS

-- VENTA: Caja DEBE (TOTAL) / Ventas HABER (NETO) / IVA HABER (IVA)
INSERT INTO accounting_entry_templates (evento_origen, nombre) VALUES ('VENTA', 'Asiento de venta');
SET @tpl_venta = LAST_INSERT_ID();

INSERT INTO accounting_entry_template_lines (template_id, cuenta_id, tipo, formula, orden)
VALUES (@tpl_venta, (SELECT id FROM accounting_accounts WHERE codigo = '1.1.1'), 'DEBE', 'TOTAL', 1);

INSERT INTO accounting_entry_template_lines (template_id, cuenta_id, tipo, formula, orden)
VALUES (@tpl_venta, (SELECT id FROM accounting_accounts WHERE codigo = '4.1.1'), 'HABER', 'NETO', 2);

INSERT INTO accounting_entry_template_lines (template_id, cuenta_id, tipo, formula, orden)
VALUES (@tpl_venta, (SELECT id FROM accounting_accounts WHERE codigo = '2.1.4'), 'HABER', 'IVA', 3);

-- NOMINA: Sueldos DEBE (SUELDOS) / Sueldos a Pagar HABER (NETO) / Cargas Sociales HABER (CARGAS_SOCIALES)
INSERT INTO accounting_entry_templates (evento_origen, nombre) VALUES ('NOMINA', 'Asiento de liquidación de nómina');
SET @tpl_nomina = LAST_INSERT_ID();

INSERT INTO accounting_entry_template_lines (template_id, cuenta_id, tipo, formula, orden)
VALUES (@tpl_nomina, (SELECT id FROM accounting_accounts WHERE codigo = '5.3'), 'DEBE', 'SUELDOS', 1);

INSERT INTO accounting_entry_template_lines (template_id, cuenta_id, tipo, formula, orden)
VALUES (@tpl_nomina, (SELECT id FROM accounting_accounts WHERE codigo = '2.1.2'), 'HABER', 'NETO', 2);

INSERT INTO accounting_entry_template_lines (template_id, cuenta_id, tipo, formula, orden)
VALUES (@tpl_nomina, (SELECT id FROM accounting_accounts WHERE codigo = '2.1.3'), 'HABER', 'CARGAS_SOCIALES', 3);

-- GASTO: Gasto categoría DEBE (TOTAL) / Caja HABER (TOTAL)
INSERT INTO accounting_entry_templates (evento_origen, nombre) VALUES ('GASTO', 'Asiento de gasto');
SET @tpl_gasto = LAST_INSERT_ID();

INSERT INTO accounting_entry_template_lines (template_id, cuenta_id, tipo, formula, orden)
VALUES (@tpl_gasto, (SELECT id FROM accounting_accounts WHERE codigo = '5.1'), 'DEBE', 'TOTAL', 1);

INSERT INTO accounting_entry_template_lines (template_id, cuenta_id, tipo, formula, orden)
VALUES (@tpl_gasto, (SELECT id FROM accounting_accounts WHERE codigo = '1.1.1'), 'HABER', 'TOTAL', 2);
