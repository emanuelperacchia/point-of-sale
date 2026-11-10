-- Sprint 8 — Promociones y Cupones (US-018)
-- Tablas para motor de descuentos automáticos y cupones

-- Promotions
CREATE TABLE promotions (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(200) NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    valor DECIMAL(10,2) NOT NULL,
    fecha_desde DATE NOT NULL,
    fecha_hasta DATE NOT NULL,
    prioridad INTEGER NOT NULL,
    alcance VARCHAR(20) NOT NULL,
    activa BOOLEAN NOT NULL DEFAULT TRUE,
    compra_x INTEGER,
    lleva_y INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Join table: promotion → products
CREATE TABLE promotion_products (
    promotion_id BIGINT NOT NULL REFERENCES promotions(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL,
    PRIMARY KEY (promotion_id, product_id)
);

-- Join table: promotion → categories
CREATE TABLE promotion_categories (
    promotion_id BIGINT NOT NULL REFERENCES promotions(id) ON DELETE CASCADE,
    category_id BIGINT NOT NULL,
    PRIMARY KEY (promotion_id, category_id)
);

-- Coupons
CREATE TABLE coupons (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(50) NOT NULL UNIQUE,
    tipo VARCHAR(20) NOT NULL,
    valor DECIMAL(10,2) NOT NULL,
    fecha_vencimiento DATE NOT NULL,
    limite_usos INTEGER NOT NULL DEFAULT 1,
    usos_actuales INTEGER NOT NULL DEFAULT 0,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Coupon usage audit
CREATE TABLE coupon_usages (
    id BIGSERIAL PRIMARY KEY,
    coupon_id BIGINT NOT NULL,
    sale_id BIGINT NOT NULL,
    client_id BIGINT,
    usado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_promotions_activa ON promotions(activa);
CREATE INDEX idx_promotions_fechas ON promotions(fecha_desde, fecha_hasta);
CREATE INDEX idx_promotions_alcance ON promotions(alcance);
CREATE INDEX idx_coupons_codigo ON coupons(codigo);
CREATE INDEX idx_coupons_activo ON coupons(activo);
CREATE INDEX idx_coupon_usages_coupon ON coupon_usages(coupon_id);
CREATE INDEX idx_coupon_usages_sale ON coupon_usages(sale_id);

-- Comments
COMMENT ON TABLE promotions IS 'Reglas de descuento y promociones automáticas';
COMMENT ON COLUMN promotions.tipo IS 'Tipo: PORCENTAJE, MONTO_FIJO, DOSX1, TRESX2, COMPRA_X_LLEVA_Y';
COMMENT ON COLUMN promotions.alcance IS 'Alcance: PRODUCTO, CATEGORIA, CARRITO';
COMMENT ON COLUMN promotions.prioridad IS 'Mayor número = mayor prioridad';
COMMENT ON TABLE coupons IS 'Cupones de descuento';
COMMENT ON TABLE coupon_usages IS 'Registro de uso de cupones para auditoría';
