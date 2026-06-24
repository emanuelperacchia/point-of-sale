-- Sprint 15: Transferencias entre sucursales

-- 1. Stock Transfers
CREATE TABLE stock_transfers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sucursal_origen_id BIGINT NOT NULL,
    sucursal_destino_id BIGINT NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'SOLICITADA',
    motivo VARCHAR(500),
    solicitado_por BIGINT,
    despachado_por BIGINT,
    recibido_por BIGINT,
    fecha_solicitud DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_despacho DATETIME,
    fecha_recepcion DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (sucursal_origen_id) REFERENCES branches(id),
    FOREIGN KEY (sucursal_destino_id) REFERENCES branches(id)
);

-- 2. Stock Transfer Items
CREATE TABLE stock_transfer_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transfer_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    cantidad_solicitada INT NOT NULL,
    cantidad_despachada INT,
    cantidad_recibida INT,
    FOREIGN KEY (transfer_id) REFERENCES stock_transfers(id),
    FOREIGN KEY (product_id) REFERENCES products(id)
);

-- 3. Indexes
CREATE INDEX idx_transfer_origen ON stock_transfers(sucursal_origen_id);
CREATE INDEX idx_transfer_destino ON stock_transfers(sucursal_destino_id);
CREATE INDEX idx_transfer_estado ON stock_transfers(estado);
CREATE INDEX idx_transfer_items_transfer ON stock_transfer_items(transfer_id);
CREATE INDEX idx_transfer_items_product ON stock_transfer_items(product_id);
