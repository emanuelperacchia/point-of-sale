-- Sprint 16: Precios por sucursal

CREATE TABLE branch_prices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    branch_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    precio DECIMAL(10, 2) NOT NULL,
    vigencia_desde DATE,
    vigencia_hasta DATE,
    creado_por BIGINT,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (branch_id) REFERENCES branches(id),
    FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE UNIQUE INDEX idx_branch_price_unique ON branch_prices(branch_id, product_id);
CREATE INDEX idx_branch_prices_branch ON branch_prices(branch_id);
CREATE INDEX idx_branch_prices_product ON branch_prices(product_id);
CREATE INDEX idx_branch_prices_activo ON branch_prices(activo);
