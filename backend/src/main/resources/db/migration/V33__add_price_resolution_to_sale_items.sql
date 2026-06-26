-- Sprint 16: Agrega campos de resolución de precio a sale_items

ALTER TABLE sale_items ADD COLUMN precio_resuelto DECIMAL(10, 2);
ALTER TABLE sale_items ADD COLUMN es_precio_local BOOLEAN NOT NULL DEFAULT FALSE;
