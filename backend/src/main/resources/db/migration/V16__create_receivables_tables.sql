-- Sprint 10 — US-025: Cuentas por Cobrar
-- Tablas: receivables, receivable_payments

CREATE TABLE receivables (
    id              BIGSERIAL       PRIMARY KEY,
    client_id       BIGINT          NOT NULL REFERENCES clients(id),
    sale_id         BIGINT          NOT NULL REFERENCES sales(id),
    monto_original  NUMERIC(12,2)   NOT NULL,
    saldo_pendiente NUMERIC(12,2)   NOT NULL,
    fecha_emision   DATE            NOT NULL,
    fecha_vencimiento DATE          NOT NULL,
    estado          VARCHAR(20)     NOT NULL DEFAULT 'PENDIENTE'
                        CHECK (estado IN ('PENDIENTE','PARCIAL','COBRADA','VENCIDA','INCOBRABLE')),
    intereses_acumulados NUMERIC(12,2) NOT NULL DEFAULT 0,
    created_at      TIMESTAMP       NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP
);

CREATE INDEX idx_receivables_client ON receivables(client_id);
CREATE INDEX idx_receivables_estado ON receivables(estado);
CREATE INDEX idx_receivables_vencimiento ON receivables(fecha_vencimiento);

CREATE TABLE receivable_payments (
    id              BIGSERIAL       PRIMARY KEY,
    receivable_id   BIGINT          NOT NULL REFERENCES receivables(id),
    monto           NUMERIC(12,2)   NOT NULL,
    metodo_pago     VARCHAR(20)     NOT NULL,
    fecha           TIMESTAMP       NOT NULL DEFAULT now(),
    registrado_por  BIGINT          NOT NULL REFERENCES users(id),
    created_at      TIMESTAMP       NOT NULL DEFAULT now()
);

CREATE INDEX idx_receivable_payments_receivable ON receivable_payments(receivable_id);
