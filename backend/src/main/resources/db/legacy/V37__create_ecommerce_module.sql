-- ============================================================
-- SPRINT 18 — US-056: Integración con E-commerce
-- Configuración, logs de sync, pedidos web importados
-- ============================================================

CREATE TABLE ecommerce_configs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    base_url VARCHAR(500) NOT NULL,
    api_key VARCHAR(500) NOT NULL,
    branch_id BIGINT NOT NULL,
    sync_frequency_minutes INT NOT NULL DEFAULT 5,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    ultima_sync DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_ecommerce_config_activo (activo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE ecommerce_sync_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_id BIGINT NOT NULL,
    tipo VARCHAR(20) NOT NULL COMMENT 'CATALOG, STOCK, ORDERS',
    resultado VARCHAR(10) NOT NULL COMMENT 'OK, ERROR',
    mensaje VARCHAR(1000) NULL,
    items_processed INT NOT NULL DEFAULT 0,
    timestamp DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ecommerce_sync_log_config FOREIGN KEY (config_id) REFERENCES ecommerce_configs(id) ON DELETE CASCADE,
    INDEX idx_ecommerce_sync_log_config (config_id),
    INDEX idx_ecommerce_sync_log_timestamp (timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE ecommerce_orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_id BIGINT NOT NULL,
    external_order_id VARCHAR(100) NOT NULL,
    external_data JSON NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE' COMMENT 'PENDIENTE, CONFIRMADO, ENVIADO',
    sale_id BIGINT NULL COMMENT 'FK al Sale generado en POS',
    error_message VARCHAR(1000) NULL,
    imported_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ecommerce_order_config FOREIGN KEY (config_id) REFERENCES ecommerce_configs(id) ON DELETE CASCADE,
    UNIQUE INDEX idx_ecommerce_order_external (config_id, external_order_id),
    INDEX idx_ecommerce_order_status (status),
    INDEX idx_ecommerce_order_sale (sale_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
