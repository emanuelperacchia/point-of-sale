CREATE TABLE expenses (
    id              BIGSERIAL       PRIMARY KEY,
    monto           DECIMAL(12, 2)  NOT NULL,
    fecha           DATE            NOT NULL,
    categoria       VARCHAR(50)     NOT NULL,
    proveedor_id    BIGINT          REFERENCES suppliers(id) ON DELETE SET NULL,
    descripcion     VARCHAR(500)    NOT NULL,
    estado          VARCHAR(20)     NOT NULL DEFAULT 'PENDIENTE',
    comprobante_url VARCHAR(500),
    recurrente      BOOLEAN         NOT NULL DEFAULT FALSE,
    frecuencia      VARCHAR(20),
    proxima_fecha   DATE,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_expense_monto CHECK (monto > 0),
    CONSTRAINT chk_expense_categoria CHECK (categoria IN (
        'ALQUILER', 'SERVICIOS', 'SUELDOS', 'COMPRAS_MERCADERIA',
        'IMPUESTOS', 'MANTENIMIENTO', 'MARKETING', 'OTROS'
    )),
    CONSTRAINT chk_expense_estado CHECK (estado IN ('PAGADO', 'PENDIENTE')),
    CONSTRAINT chk_expense_frecuencia CHECK (
        recurrente = FALSE OR frecuencia IN ('MENSUAL', 'TRIMESTRAL', 'ANUAL')
    )
);

CREATE INDEX idx_expenses_fecha ON expenses(fecha);
CREATE INDEX idx_expenses_categoria ON expenses(categoria);
CREATE INDEX idx_expenses_estado ON expenses(estado);
CREATE INDEX idx_expenses_proveedor ON expenses(proveedor_id);
CREATE INDEX idx_expenses_recurrentes ON expenses(proxima_fecha)
    WHERE recurrente = TRUE AND estado = 'PENDIENTE';
