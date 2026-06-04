-- ============================================================
-- V11: Gestión de turnos de caja
-- ============================================================

-- -----------------------------------------------------------
-- 1. Turnos de caja
-- -----------------------------------------------------------
CREATE TABLE cash_shifts (
    id              BIGSERIAL    PRIMARY KEY,
    cajero_id       BIGINT       NOT NULL REFERENCES users(id),
    sucursal_id     BIGINT       NOT NULL DEFAULT 1,
    estado          VARCHAR(20)  NOT NULL,
    monto_apertura  NUMERIC(12,2) NOT NULL,
    monto_cierre    NUMERIC(12,2),
    diferencia      NUMERIC(12,2),
    version         BIGINT       NOT NULL DEFAULT 0,
    fecha_apertura  TIMESTAMP    NOT NULL DEFAULT NOW(),
    fecha_cierre    TIMESTAMP
);

CREATE INDEX idx_shift_cajero_estado ON cash_shifts(cajero_id, estado);

-- -----------------------------------------------------------
-- 2. Movimientos manuales de caja
-- -----------------------------------------------------------
CREATE TABLE shift_movements (
    id          BIGSERIAL    PRIMARY KEY,
    shift_id    BIGINT       NOT NULL REFERENCES cash_shifts(id),
    usuario_id  BIGINT       NOT NULL REFERENCES users(id),
    tipo        VARCHAR(20)  NOT NULL,
    monto       NUMERIC(12,2) NOT NULL,
    motivo      VARCHAR(255) NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_movement_shift ON shift_movements(shift_id);

-- -----------------------------------------------------------
-- 3. Agregar shift_id a payments (FK nullable)
-- -----------------------------------------------------------
ALTER TABLE payments
    ADD COLUMN IF NOT EXISTS shift_id BIGINT;

ALTER TABLE payments
    ADD CONSTRAINT fk_payment_shift
    FOREIGN KEY (shift_id) REFERENCES cash_shifts(id);
