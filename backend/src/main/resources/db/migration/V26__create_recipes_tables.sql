CREATE TABLE recipes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(200) NOT NULL,
    descripcion TEXT,
    producto_terminado_id BIGINT NOT NULL,
    cantidad_producida DECIMAL(12,2) NOT NULL,
    unidad_medida VARCHAR(20) NOT NULL,
    tiempo_produccion_minutos INT,
    activa BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME
);

CREATE TABLE bom_components (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipe_id BIGINT NOT NULL,
    componente_id BIGINT NOT NULL,
    cantidad DECIMAL(12,4) NOT NULL,
    unidad_medida VARCHAR(20) NOT NULL,
    es_merma_esperada BOOLEAN NOT NULL DEFAULT FALSE,
    porcentaje_merma_esperado DECIMAL(5,2)
);

CREATE INDEX idx_bom_components_recipe ON bom_components(recipe_id);
CREATE INDEX idx_bom_components_componente ON bom_components(componente_id);
