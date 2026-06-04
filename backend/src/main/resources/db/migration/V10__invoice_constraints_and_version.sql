-- ============================================================
-- V10: Constraints de integridad y versionado optimista
-- ============================================================

-- -----------------------------------------------------------
-- 1. Agregar columna version para optimistic locking
-- -----------------------------------------------------------
ALTER TABLE invoice_documents
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE digital_certificates
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

-- -----------------------------------------------------------
-- 2. UNIQUE constraint sobre sale_id para idempotencia real
-- -----------------------------------------------------------
ALTER TABLE invoice_documents
    DROP CONSTRAINT IF EXISTS uk_invoice_sale_id;

ALTER TABLE invoice_documents
    ADD CONSTRAINT uk_invoice_sale_id UNIQUE (sale_id);

-- -----------------------------------------------------------
-- 3. Índice único parcial: solo una empresa activa
-- -----------------------------------------------------------
DROP INDEX IF EXISTS idx_company_unique_active;

CREATE UNIQUE INDEX idx_company_unique_active
    ON companies(active)
    WHERE active = TRUE;

-- -----------------------------------------------------------
-- 4. Secuencia para guías de despacho (Sprint 8)
-- -----------------------------------------------------------
CREATE SEQUENCE IF NOT EXISTS seq_dispatch_guide START 1;
