-- Sprint 15: Backups y dispositivos

-- 1. System backups table
CREATE TABLE system_backups (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    filename VARCHAR(255) NOT NULL,
    file_size BIGINT,
    ubicacion VARCHAR(500) NOT NULL,
    tipo_storage VARCHAR(20) NOT NULL DEFAULT 'LOCAL',
    creado_por BIGINT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. Device tokens (FCM push notifications)
CREATE TABLE device_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    fcm_token VARCHAR(500) NOT NULL,
    plataforma VARCHAR(20) NOT NULL DEFAULT 'ANDROID',
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_device_tokens_user ON device_tokens(user_id);
CREATE INDEX idx_system_backups_created ON system_backups(created_at);
