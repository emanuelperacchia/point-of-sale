-- Sprint 10 — US-026: Cuentas por Pagar
-- Tablas: payables, payable_payments

CREATE TABLE payables (
    id                  BIGSERIAL       PRIMARY KEY,
    supplier_id         BIGINT          NOT NULL REFERENCES suppliers(id),
    purchase_order_id   BIGINT          REFERENCES purchase_orders(id),
    monto_original      NUMERIC(12,2)   NOT NULL,
    saldo_pendiente     NUMERIC(12,2)   NOT NULL,
    fecha_emision       DATE            NOT NULL,
    fecha_vencimiento   DATE            NOT NULL,
    estado              VARCHAR(20)     NOT NULL DEFAULT 'PENDIENTE'
                            CHECK (estado IN ('PENDIENTE','PARCIAL','PAGADA','VENCIDA')),
    referencia_bancaria VARCHAR(100),
    created_at          TIMESTAMP       NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP
);

CREATE INDEX idx_payables_supplier ON payables(supplier_id);
CREATE INDEX idx_payables_estado ON payables(estado);
CREATE INDEX idx_payables_vencimiento ON payables(fecha_vencimiento);

CREATE TABLE payable_payments (
    id                  BIGSERIAL       PRIMARY KEY,
    payable_id          BIGINT          NOT NULL REFERENCES payables(id),
    monto               NUMERIC(12,2)   NOT NULL,
    metodo_pago         VARCHAR(20)     NOT NULL,
    referencia_bancaria VARCHAR(100),
    fecha               TIMESTAMP       NOT NULL DEFAULT now(),
    registrado_por      BIGINT          NOT NULL REFERENCES users(id),
    created_at          TIMESTAMP       NOT NULL DEFAULT now()
);

CREATE INDEX idx_payable_payments_payable ON payable_payments(payable_id);
