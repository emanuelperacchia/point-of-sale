CREATE TABLE production_orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipe_id BIGINT NOT NULL,
    cantidad_planificada INT NOT NULL,
    cantidad_producida INT,
    fecha_planificada DATE NOT NULL,
    responsable_id BIGINT NOT NULL,
    sucursal_id BIGINT,
    estado VARCHAR(20) NOT NULL DEFAULT 'PLANIFICADA',
    observaciones TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME
);

CREATE TABLE production_order_components (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    production_order_id BIGINT NOT NULL,
    bom_component_id BIGINT NOT NULL,
    cantidad_planificada DECIMAL(12,4) NOT NULL,
    cantidad_consumida DECIMAL(12,4),
    merma_real DECIMAL(12,4),
    motivo_merma VARCHAR(20)
);

CREATE INDEX idx_production_orders_estado ON production_orders(estado);
CREATE INDEX idx_production_orders_responsable ON production_orders(responsable_id);
CREATE INDEX idx_po_components_order ON production_order_components(production_order_id);
