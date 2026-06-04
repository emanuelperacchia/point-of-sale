-- Sales table — core POS transactions
CREATE TABLE sales (
    id BIGSERIAL PRIMARY KEY,
    client_id BIGINT REFERENCES clients(id) ON DELETE SET NULL,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    warehouse_id BIGINT NOT NULL REFERENCES warehouses(id) ON DELETE RESTRICT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    subtotal DECIMAL(10,2) NOT NULL,
    tax_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
    discount DECIMAL(10,2) NOT NULL DEFAULT 0,
    total DECIMAL(10,2) NOT NULL,
    notes VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Sale items — line items for each sale
CREATE TABLE sale_items (
    id BIGSERIAL PRIMARY KEY,
    sale_id BIGINT NOT NULL REFERENCES sales(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    product_name VARCHAR(150) NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    discount DECIMAL(10,2) NOT NULL DEFAULT 0,
    tax_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
    subtotal DECIMAL(10,2) NOT NULL
);

-- Payments — one sale can have multiple payment methods (split payment)
CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    sale_id BIGINT NOT NULL REFERENCES sales(id) ON DELETE CASCADE,
    payment_method VARCHAR(20) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    reference VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_sales_user ON sales(user_id);
CREATE INDEX idx_sales_client ON sales(client_id);
CREATE INDEX idx_sales_status ON sales(status);
CREATE INDEX idx_sales_created_at ON sales(created_at DESC);
CREATE INDEX idx_sale_items_sale ON sale_items(sale_id);
CREATE INDEX idx_sale_items_product ON sale_items(product_id);
CREATE INDEX idx_payments_sale ON payments(sale_id);
CREATE INDEX idx_payments_method ON payments(payment_method);

-- Comments
COMMENT ON TABLE sales IS 'Ventas registradas en el POS';
COMMENT ON COLUMN sales.status IS 'Estado: PENDING, COMPLETED, CANCELLED, REFUNDED';
COMMENT ON COLUMN sales.warehouse_id IS 'Bodega desde la que se descuenta el stock';
COMMENT ON TABLE sale_items IS 'Detalle de productos en cada venta';
COMMENT ON TABLE payments IS 'Pagos asociados a una venta (soporta pago mixto)';
COMMENT ON COLUMN payments.payment_method IS 'Método: CASH, DEBIT_CARD, CREDIT_CARD, TRANSFER';
