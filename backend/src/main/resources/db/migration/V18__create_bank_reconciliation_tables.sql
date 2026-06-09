-- Sprint 10 — US-028: Conciliación Bancaria
-- Tablas: bank_reconciliations, bank_statements

CREATE TABLE bank_reconciliations (
    id              BIGSERIAL       PRIMARY KEY,
    periodo         VARCHAR(7)      NOT NULL,  -- YYYY-MM
    total_extracto  NUMERIC(14,2)   NOT NULL,
    total_sistema   NUMERIC(14,2)   NOT NULL,
    diferencia      NUMERIC(14,2)   NOT NULL,
    estado          VARCHAR(10)     NOT NULL DEFAULT 'ABIERTA'
                        CHECK (estado IN ('ABIERTA','CERRADA')),
    created_at      TIMESTAMP       NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP
);

CREATE UNIQUE INDEX idx_bank_reconciliations_periodo ON bank_reconciliations(periodo);

CREATE TABLE bank_statements (
    id                  BIGSERIAL       PRIMARY KEY,
    reconciliation_id   BIGINT          NOT NULL REFERENCES bank_reconciliations(id),
    fecha               DATE            NOT NULL,
    descripcion         VARCHAR(500)    NOT NULL,
    monto               NUMERIC(12,2)   NOT NULL,
    tipo                VARCHAR(10)     NOT NULL CHECK (tipo IN ('CREDITO','DEBITO')),
    estado              VARCHAR(20)     NOT NULL DEFAULT 'PENDIENTE'
                            CHECK (estado IN ('PENDIENTE','CONCILIADO','AJUSTE_MANUAL')),
    payment_id          BIGINT,
    observacion         VARCHAR(500),
    created_at          TIMESTAMP       NOT NULL DEFAULT now()
);

CREATE INDEX idx_bank_statements_reconciliation ON bank_statements(reconciliation_id);
CREATE INDEX idx_bank_statements_estado ON bank_statements(estado);
