-- ============================================================
-- V9: Infraestructura de Facturación Electrónica
-- ============================================================

-- -----------------------------------------------------------
-- 1. Datos fiscales de la empresa emisora
-- -----------------------------------------------------------
CREATE TABLE companies (
    id              BIGSERIAL    PRIMARY KEY,
    business_name   VARCHAR(200) NOT NULL,
    trading_name    VARCHAR(200),
    document_type   VARCHAR(20),
    document_number VARCHAR(50),
    tax_address     VARCHAR(255),
    phone           VARCHAR(100),
    email           VARCHAR(150),
    punto_venta     INTEGER      NOT NULL DEFAULT 1,
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP
);

-- -----------------------------------------------------------
-- 2. Certificados digitales para firma de XML
-- -----------------------------------------------------------
CREATE TABLE digital_certificates (
    id               BIGSERIAL    PRIMARY KEY,
    alias            VARCHAR(100) NOT NULL,
    store_type       VARCHAR(20)  NOT NULL DEFAULT 'PKCS12',
    store_password   VARCHAR(500) NOT NULL,
    certificate_data BYTEA        NOT NULL,
    valid_from       TIMESTAMP    NOT NULL,
    valid_to         TIMESTAMP    NOT NULL,
    issuer           VARCHAR(255),
    subject          VARCHAR(255),
    serial_number    VARCHAR(100),
    active           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP
);

-- -----------------------------------------------------------
-- 3. Documentos de comprobantes electrónicos
-- -----------------------------------------------------------
CREATE TABLE invoice_documents (
    id                     BIGSERIAL    PRIMARY KEY,
    sale_id                BIGINT       NOT NULL,
    tipo_comprobante       VARCHAR(20)  NOT NULL,
    punto_venta            INTEGER      NOT NULL DEFAULT 1,
    numero                 BIGINT       NOT NULL,
    cae                    VARCHAR(20),
    fecha_cae              TIMESTAMP,
    xml_firmado            TEXT,
    pdf_path               VARCHAR(500),
    qr_code                TEXT,
    estado                 VARCHAR(20)  NOT NULL,
    intentos               INTEGER      NOT NULL DEFAULT 0,
    motivo_rechazo         TEXT,
    comprobante_original_id BIGINT,
    receptor_nombre        VARCHAR(200),
    receptor_documento     VARCHAR(50),
    receptor_condicion_iva VARCHAR(20),
    created_at             TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMP
);

CREATE INDEX idx_invoice_sale        ON invoice_documents(sale_id);
CREATE INDEX idx_invoice_estado      ON invoice_documents(estado);
CREATE INDEX idx_invoice_tipo_numero ON invoice_documents(tipo_comprobante, numero);

-- -----------------------------------------------------------
-- 4. Secuencias correlativas por tipo de comprobante
-- -----------------------------------------------------------
CREATE SEQUENCE seq_boleta       START 1;
CREATE SEQUENCE seq_factura_a    START 1;
CREATE SEQUENCE seq_factura_b    START 1;
CREATE SEQUENCE seq_factura_c    START 1;
CREATE SEQUENCE seq_nota_credito START 1;
CREATE SEQUENCE seq_nota_debito  START 1;

-- -----------------------------------------------------------
-- 5. Extender tabla clients con campos fiscales
-- -----------------------------------------------------------
ALTER TABLE clients
    ADD COLUMN IF NOT EXISTS business_name   VARCHAR(200),
    ADD COLUMN IF NOT EXISTS condicion_iva   VARCHAR(20)  DEFAULT 'CONSUMIDOR_FINAL',
    ADD COLUMN IF NOT EXISTS tax_address     VARCHAR(255);
