CREATE TABLE lotes_produccion (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    production_order_id BIGINT NOT NULL,
    numero_lote VARCHAR(30) NOT NULL UNIQUE,
    fecha_produccion DATE NOT NULL,
    cantidad INT NOT NULL,
    producto_terminado_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_lotes_numero ON lotes_produccion(numero_lote);
CREATE INDEX idx_lotes_producto ON lotes_produccion(producto_terminado_id);
