-- ============================================================
-- SPRINT 18 — US-055: Módulo de Contabilidad
-- Plan de cuentas, asientos automáticos, libro diario
-- ============================================================

-- 1. PLAN DE CUENTAS
CREATE TABLE accounting_accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    codigo VARCHAR(20) NOT NULL,
    nombre VARCHAR(200) NOT NULL,
    tipo VARCHAR(20) NOT NULL COMMENT 'ACTIVO, PASIVO, PATRIMONIO, INGRESO, EGRESO',
    cuenta_padre_id BIGINT NULL,
    nivel INT NOT NULL DEFAULT 1,
    activa BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_accounting_account_padre FOREIGN KEY (cuenta_padre_id) REFERENCES accounting_accounts(id),
    UNIQUE INDEX idx_accounting_account_codigo (codigo),
    INDEX idx_accounting_account_tipo (tipo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. PLANTILLAS DE ASIENTOS (por tipo de evento)
CREATE TABLE accounting_entry_templates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    evento_origen VARCHAR(30) NOT NULL COMMENT 'VENTA, COMPRA, GASTO, NOMINA',
    nombre VARCHAR(200) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX idx_accounting_template_evento (evento_origen),
    INDEX idx_accounting_template_activo (activo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. LÍNEAS DE PLANTILLA (DEBE/HABER con fórmula)
CREATE TABLE accounting_entry_template_lines (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_id BIGINT NOT NULL,
    cuenta_id BIGINT NOT NULL,
    tipo VARCHAR(10) NOT NULL COMMENT 'DEBE / HABER',
    formula VARCHAR(30) NOT NULL COMMENT 'TOTAL, IVA, NETO, SUELDOS, CARGAS_SOCIALES, FIJO',
    monto_fijo DECIMAL(12,2) NULL COMMENT 'Solo si formula=FIJO',
    orden INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_accounting_tpl_line_template FOREIGN KEY (template_id) REFERENCES accounting_entry_templates(id) ON DELETE CASCADE,
    CONSTRAINT fk_accounting_tpl_line_cuenta FOREIGN KEY (cuenta_id) REFERENCES accounting_accounts(id),
    INDEX idx_accounting_tpl_line_template (template_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. ASIENTOS CONTABLES (inmutables una vez generados)
CREATE TABLE accounting_journal_entries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    fecha DATE NOT NULL,
    descripcion VARCHAR(500) NOT NULL,
    referencia_id BIGINT NOT NULL COMMENT 'ID de la venta/nómina/gasto',
    referencia_type VARCHAR(20) NOT NULL COMMENT 'SALE, PAYROLL, EXPENSE',
    estado VARCHAR(20) NOT NULL DEFAULT 'GENERADO' COMMENT 'GENERADO (inmutable)',
    webhook_enviado BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_accounting_entry_referencia (referencia_id, referencia_type),
    INDEX idx_accounting_entry_fecha (fecha),
    INDEX idx_accounting_entry_estado (estado)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5. LÍNEAS DE ASIENTO (DEBE/HABER)
CREATE TABLE accounting_journal_lines (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    entry_id BIGINT NOT NULL,
    cuenta_id BIGINT NOT NULL,
    tipo VARCHAR(10) NOT NULL COMMENT 'DEBE / HABER',
    monto DECIMAL(12,2) NOT NULL,
    CONSTRAINT fk_accounting_jl_entry FOREIGN KEY (entry_id) REFERENCES accounting_journal_entries(id) ON DELETE CASCADE,
    CONSTRAINT fk_accounting_jl_cuenta FOREIGN KEY (cuenta_id) REFERENCES accounting_accounts(id),
    INDEX idx_accounting_jl_entry (entry_id),
    INDEX idx_accounting_jl_cuenta (cuenta_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
