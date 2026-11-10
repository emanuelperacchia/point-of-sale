-- ============================================================
-- V12: Devoluciones de ventas
-- ============================================================

-- -----------------------------------------------------------
-- 1. Devoluciones
-- -----------------------------------------------------------
CREATE TABLE sale_returns (
    id                   BIGSERIAL    PRIMARY KEY,
    sale_id              BIGINT       NOT NULL REFERENCES sales(id),
    estado               VARCHAR(25)  NOT NULL,
    motivo               VARCHAR(500) NOT NULL,
    aprobador_id         BIGINT,
    monto_total          NUMERIC(12,2) NOT NULL,
    metodo_devolucion    VARCHAR(20)  NOT NULL,
    nota_credito_id      BIGINT,
    referencia_devolucion VARCHAR(50),
    version              BIGINT       NOT NULL DEFAULT 0,
    created_at           TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP
);

CREATE INDEX idx_return_sale ON sale_returns(sale_id);
CREATE INDEX idx_return_estado ON sale_returns(estado);

-- -----------------------------------------------------------
-- 2. Items de devolución
-- -----------------------------------------------------------
CREATE TABLE return_items (
    id             BIGSERIAL    PRIMARY KEY,
    return_id      BIGINT       NOT NULL REFERENCES sale_returns(id),
    sale_item_id   BIGINT       NOT NULL,
    cantidad       INTEGER      NOT NULL,
    precio_unitario NUMERIC(12,2) NOT NULL
);

CREATE INDEX idx_return_item_return ON return_items(return_id);
