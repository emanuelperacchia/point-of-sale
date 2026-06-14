CREATE TABLE payroll (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    mes INT NOT NULL,
    anio INT NOT NULL,
    dias_trabajados INT,
    horas_normales_minutos INT,
    horas_extra_minutos INT,
    sueldo_basico DECIMAL(12,2),
    plus_horas_extra DECIMAL(12,2),
    comisiones DECIMAL(12,2),
    bono_desempeno DECIMAL(12,2),
    total_haberes DECIMAL(12,2),
    desc_jubilacion DECIMAL(12,2),
    desc_obra_social DECIMAL(12,2),
    desc_anses DECIMAL(12,2),
    desc_ausencias DECIMAL(12,2),
    desc_embargos DECIMAL(12,2),
    total_descuentos DECIMAL(12,2),
    neto_a_pagar DECIMAL(12,2),
    estado VARCHAR(20) NOT NULL DEFAULT 'BORRADOR',
    aprobado_por BIGINT,
    fecha_aprobacion DATE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE payroll_adjustments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    payroll_id BIGINT NOT NULL,
    concepto VARCHAR(100) NOT NULL,
    monto DECIMAL(12,2) NOT NULL,
    justificacion VARCHAR(500),
    creado_por BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_payroll_employee_period ON payroll(employee_id, mes, anio);
CREATE INDEX idx_payroll_adjustments_payroll ON payroll_adjustments(payroll_id);
