-- Sprint 8 — Fidelización de Clientes (US-017)
-- Programa de puntos y segmentación por tiers

-- Add loyalty columns to clients
ALTER TABLE clients
    ADD COLUMN IF NOT EXISTS puntos_acumulados BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS tier VARCHAR(10) NOT NULL DEFAULT 'BRONCE',
    ADD COLUMN IF NOT EXISTS fecha_ultima_transaccion DATE;

-- Points transactions ledger
CREATE TABLE points_transactions (
    id BIGSERIAL PRIMARY KEY,
    client_id BIGINT NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    sale_id BIGINT REFERENCES sales(id) ON DELETE SET NULL,
    tipo VARCHAR(20) NOT NULL,
    puntos BIGINT NOT NULL,
    saldo_previo BIGINT NOT NULL,
    saldo_posterior BIGINT NOT NULL,
    descripcion VARCHAR(255),
    fecha TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_points_client ON points_transactions(client_id);
CREATE INDEX idx_points_fecha ON points_transactions(fecha DESC);

-- Comments
COMMENT ON TABLE points_transactions IS 'Libro diario de puntos: acumulación, canje y vencimiento';
COMMENT ON COLUMN points_transactions.tipo IS 'Tipo: ACUMULACION, CANJE, VENCIMIENTO';
