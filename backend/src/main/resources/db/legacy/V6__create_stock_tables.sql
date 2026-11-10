-- Warehouses table
CREATE TABLE warehouses (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    address VARCHAR(255),
    city VARCHAR(50),
    phone VARCHAR(20),
    manager VARCHAR(100),
    type VARCHAR(20) NOT NULL DEFAULT 'PRINCIPAL',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Product stocks table
CREATE TABLE product_stocks (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    warehouse_id BIGINT NOT NULL,
    current_stock DECIMAL(10,2) NOT NULL DEFAULT 0,
    reserved_stock DECIMAL(10,2) NOT NULL DEFAULT 0,
    minimum_stock DECIMAL(10,2) NOT NULL DEFAULT 0,
    maximum_stock DECIMAL(10,2) NOT NULL DEFAULT 0,
    average_cost DECIMAL(10,4) NOT NULL DEFAULT 0,
    total_value DECIMAL(10,2) NOT NULL DEFAULT 0,
    last_movement TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    FOREIGN KEY (warehouse_id) REFERENCES warehouses(id) ON DELETE RESTRICT,
    UNIQUE (product_id, warehouse_id)
);

-- Stock movements table
CREATE TABLE stock_movements (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    warehouse_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL,
    quantity DECIMAL(10,2) NOT NULL,
    previous_stock DECIMAL(10,2),
    current_stock DECIMAL(10,2),
    unit_cost DECIMAL(10,2),
    reason VARCHAR(500),
    reference_document VARCHAR(100),
    created_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE RESTRICT,
    FOREIGN KEY (warehouse_id) REFERENCES warehouses(id) ON DELETE RESTRICT,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
);

-- Indexes for performance
CREATE INDEX idx_product_stocks_product ON product_stocks(product_id);
CREATE INDEX idx_product_stocks_warehouse ON product_stocks(warehouse_id);
CREATE INDEX idx_product_stocks_low_stock ON product_stocks(current_stock) 
    WHERE current_stock < minimum_stock;

CREATE INDEX idx_stock_movements_product ON stock_movements(product_id);
CREATE INDEX idx_stock_movements_warehouse ON stock_movements(warehouse_id);
CREATE INDEX idx_stock_movements_created_at ON stock_movements(created_at DESC);
CREATE INDEX idx_stock_movements_type ON stock_movements(type);

-- Insert default warehouse
INSERT INTO warehouses (code, name, type, active) 
VALUES ('MAIN', 'Bodega Principal', 'PRINCIPAL', TRUE);

-- Comments for documentation
COMMENT ON TABLE warehouses IS 'Almacena las bodegas/almacenes del sistema';
COMMENT ON TABLE product_stocks IS 'Stock actual por producto y bodega';
COMMENT ON TABLE stock_movements IS 'Kardex - Historial de movimientos de inventario';
COMMENT ON COLUMN stock_movements.type IS 'Tipo de movimiento: ENTRADA/SALIDA';
COMMENT ON COLUMN product_stocks.reserved_stock IS 'Stock reservado en ventas pendientes';