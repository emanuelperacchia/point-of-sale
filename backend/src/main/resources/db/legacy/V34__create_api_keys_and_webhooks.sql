-- Sprint 17: API Keys, Webhooks y eventos del sistema

-- 1. API Keys
CREATE TABLE api_keys (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    key_hash VARCHAR(64) NOT NULL UNIQUE,
    key_prefix VARCHAR(20) NOT NULL,
    permisos VARCHAR(500) NOT NULL DEFAULT '',
    rate_limit INT NOT NULL DEFAULT 60,
    expiracion DATETIME,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_por BIGINT,
    ultimo_uso DATETIME,
    total_requests BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (creado_por) REFERENCES users(id)
);

-- 2. API Key Usage Logs
CREATE TABLE api_key_usage_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    api_key_id BIGINT NOT NULL,
    endpoint VARCHAR(255) NOT NULL,
    metodo VARCHAR(10) NOT NULL,
    status_code INT NOT NULL,
    ip VARCHAR(45),
    timestamp DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (api_key_id) REFERENCES api_keys(id)
);

-- 3. Webhook Endpoints
CREATE TABLE webhook_endpoints (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    url VARCHAR(500) NOT NULL,
    eventos VARCHAR(500) NOT NULL,
    secreto VARCHAR(100) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_por BIGINT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (creado_por) REFERENCES users(id)
);

-- 4. Webhook Deliveries
CREATE TABLE webhook_deliveries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    webhook_id BIGINT NOT NULL,
    evento VARCHAR(100) NOT NULL,
    payload JSON,
    status_code INT,
    intentos INT NOT NULL DEFAULT 0,
    ultimo_intento DATETIME,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (webhook_id) REFERENCES webhook_endpoints(id)
);

-- Indexes
CREATE INDEX idx_api_keys_hash ON api_keys(key_hash);
CREATE INDEX idx_api_key_usage_key ON api_key_usage_logs(api_key_id);
CREATE INDEX idx_api_key_usage_ts ON api_key_usage_logs(timestamp);
CREATE INDEX idx_webhook_activo ON webhook_endpoints(activo);
CREATE INDEX idx_webhook_delivery_webhook ON webhook_deliveries(webhook_id);
CREATE INDEX idx_webhook_delivery_estado ON webhook_deliveries(estado);
