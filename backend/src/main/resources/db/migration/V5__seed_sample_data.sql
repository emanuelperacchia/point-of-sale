-- ============================================================
-- V5: Seed de datos de muestra para dashboard y reportes
-- Genera categorías, productos, clientes, empleados y ventas
-- distribuidas en los últimos 60 días.
-- ============================================================

-- Limpiar datos previos
DELETE FROM sale_items;
DELETE FROM sales;
DELETE FROM payments;
DELETE FROM products;
DELETE FROM employees;
DELETE FROM clients;
DELETE FROM categories;

-- ---------------------------------------------------------
-- 1. CATEGORÍAS
-- ---------------------------------------------------------
INSERT INTO categories (name, description, active) VALUES
  ('Bebidas', 'Gaseosas, aguas, jugos y bebidas varias', TRUE),
  ('Lácteos', 'Leche, yogures, quesos y postres lácteos', TRUE),
  ('Limpieza', 'Detergentes, desinfectantes y artículos de limpieza', TRUE),
  ('Almacén', 'Azúcar, sal, harina, aceites y productos secos', TRUE),
  ('Frutas y Verduras', 'Productos frescos de temporada', TRUE),
  ('Carnicería', 'Carnes vacunas, porcinas y aves', TRUE);

-- ---------------------------------------------------------
-- 2. PRODUCTOS (created_at es NOT NULL)
-- ---------------------------------------------------------
INSERT INTO products (sku, name, description, price, stock, tipo, costo_produccion, stock_reservado, category_id, active, created_at) VALUES
  ('BEB-001', 'Coca Cola 500ml', 'Gaseosa cola envase plástico', 950.00, 120, 'PRODUCTO_TERMINADO', 450.00, 0, (SELECT id FROM categories WHERE name = 'Bebidas'), TRUE, NOW()),
  ('BEB-002', 'Sprite 500ml', 'Gaseosa lima-limón envase plástico', 950.00, 95, 'PRODUCTO_TERMINADO', 450.00, 0, (SELECT id FROM categories WHERE name = 'Bebidas'), TRUE, NOW()),
  ('BEB-003', 'Agua Sin Gas 600ml', 'Agua mineral envase plástico', 650.00, 200, 'PRODUCTO_TERMINADO', 300.00, 0, (SELECT id FROM categories WHERE name = 'Bebidas'), TRUE, NOW()),
  ('LAC-001', 'Leche Entera 1L', 'Leche pasteurizada entera', 1200.00, 60, 'PRODUCTO_TERMINADO', 850.00, 0, (SELECT id FROM categories WHERE name = 'Lácteos'), TRUE, NOW()),
  ('LAC-002', 'Yogur Natural 150g', 'Yogur natural sin sabor', 450.00, 80, 'PRODUCTO_TERMINADO', 250.00, 0, (SELECT id FROM categories WHERE name = 'Lácteos'), TRUE, NOW()),
  ('LAC-003', 'Queso Crema 200g', 'Queso crema untable', 980.00, 40, 'PRODUCTO_TERMINADO', 600.00, 0, (SELECT id FROM categories WHERE name = 'Lácteos'), TRUE, NOW()),
  ('LIM-001', 'Detergente Lavavajillas 750ml', 'Detergente concentrado', 1150.00, 50, 'PRODUCTO_TERMINADO', 700.00, 0, (SELECT id FROM categories WHERE name = 'Limpieza'), TRUE, NOW()),
  ('LIM-002', 'Desinfectante Multiusos 1L', 'Desinfectante con aroma a limón', 1350.00, 45, 'PRODUCTO_TERMINADO', 800.00, 0, (SELECT id FROM categories WHERE name = 'Limpieza'), TRUE, NOW()),
  ('ALM-001', 'Azúcar Común 1Kg', 'Azúcar refinada', 850.00, 70, 'PRODUCTO_TERMINADO', 500.00, 0, (SELECT id FROM categories WHERE name = 'Almacén'), TRUE, NOW()),
  ('ALM-002', 'Aceite Girasol 900ml', 'Aceite de girasol envase plástico', 1650.00, 35, 'PRODUCTO_TERMINADO', 1100.00, 0, (SELECT id FROM categories WHERE name = 'Almacén'), TRUE, NOW()),
  ('ALM-003', 'Harina 000 1Kg', 'Harina de trigo común', 700.00, 60, 'PRODUCTO_TERMINADO', 400.00, 0, (SELECT id FROM categories WHERE name = 'Almacén'), TRUE, NOW()),
  ('FRV-001', 'Manzana Roja 1Kg', 'Manzana roja fresca', 1100.00, 45, 'PRODUCTO_TERMINADO', 750.00, 0, (SELECT id FROM categories WHERE name = 'Frutas y Verduras'), TRUE, NOW()),
  ('FRV-002', 'Banana 1Kg', 'Banana fresca', 950.00, 55, 'PRODUCTO_TERMINADO', 600.00, 0, (SELECT id FROM categories WHERE name = 'Frutas y Verduras'), TRUE, NOW()),
  ('FRV-003', 'Tomate 1Kg', 'Tomate redondo fresco', 1450.00, 30, 'PRODUCTO_TERMINADO', 1000.00, 0, (SELECT id FROM categories WHERE name = 'Frutas y Verduras'), TRUE, NOW()),
  ('CAR-001', 'Asado Tira 1Kg', 'Carne vacuna asado tira', 4200.00, 25, 'PRODUCTO_TERMINADO', 3200.00, 0, (SELECT id FROM categories WHERE name = 'Carnicería'), TRUE, NOW()),
  ('CAR-002', 'Vacío 1Kg', 'Carne vacuna vacío', 4800.00, 20, 'PRODUCTO_TERMINADO', 3800.00, 0, (SELECT id FROM categories WHERE name = 'Carnicería'), TRUE, NOW()),
  ('CAR-003', 'Pollo Entero 1Kg', 'Pollo entero fresco', 2100.00, 35, 'PRODUCTO_TERMINADO', 1600.00, 0, (SELECT id FROM categories WHERE name = 'Carnicería'), TRUE, NOW()),
  ('BEB-004', 'Cerveza Rubia 1L', 'Cerveza rubia envase plástico', 1450.00, 40, 'PRODUCTO_TERMINADO', 900.00, 0, (SELECT id FROM categories WHERE name = 'Bebidas'), TRUE, NOW()),
  ('LAC-004', 'Queso Cremoso 200g', 'Queso cremoso untable', 1050.00, 30, 'PRODUCTO_TERMINADO', 650.00, 0, (SELECT id FROM categories WHERE name = 'Lácteos'), TRUE, NOW());

-- ---------------------------------------------------------
-- 3. CLIENTES (created_at es NOT NULL)
-- ---------------------------------------------------------
INSERT INTO clients (name, document_type, document_number, email, phone, business_name, condicion_iva, active, created_at) VALUES
  ('Consumidor Final', 'DNI', '00000000', NULL, NULL, NULL, 'CONSUMIDOR_FINAL', TRUE, NOW()),
  ('Juan Pérez', 'DNI', '27123456', 'juan.perez@email.com', '11-2345-6789', NULL, 'RESPONSABLE_INSCRIPTO', TRUE, NOW()),
  ('María González', 'DNI', '30654321', 'maria.gonzalez@email.com', '11-3456-7890', NULL, 'MONOTRIBUTISTA', TRUE, NOW()),
  ('Carlos Rodríguez', 'DNI', '33112233', 'carlos.rodriguez@email.com', '11-4567-8901', NULL, 'RESPONSABLE_INSCRIPTO', TRUE, NOW()),
  ('Ana Martínez', 'DNI', '35445566', 'ana.martinez@email.com', '11-5678-9012', NULL, 'CONSUMIDOR_FINAL', TRUE, NOW()),
  ('Luis Fernández', 'CUIT', '20-31223344-5', 'luis.fernandez@empresa.com.ar', '11-6789-0123', 'Fernandez SRL', 'RESPONSABLE_INSCRIPTO', TRUE, NOW()),
  ('Elena Gómez', 'DNI', '38776655', 'elena.gomez@email.com', '11-7890-1234', NULL, 'MONOTRIBUTISTA', TRUE, NOW()),
  ('Diego Sánchez', 'DNI', '40112233', 'diego.sanchez@email.com', '11-8901-2345', NULL, 'CONSUMIDOR_FINAL', TRUE, NOW());

-- ---------------------------------------------------------
-- 4. EMPLEADOS
-- ---------------------------------------------------------
INSERT INTO employees (nombre, apellido, dni, cuil, fecha_nacimiento, fecha_ingreso, cargo, departamento, sucursal_id, salario_base, modalidad_contrato, user_id, activo, documento_url, created_at, updated_at) VALUES
  ('Carlos', 'Lopez', '28111222', '20281112221', '1985-03-15', '2022-01-10', 'Cajero', 'Ventas', 1, 180000.00, 'FULL_TIME', NULL, TRUE, NULL, NOW(), NOW()),
  ('Lucía', 'Díaz', '30333444', '20303334442', '1990-07-22', '2022-03-15', 'Vendedora', 'Ventas', 1, 195000.00, 'FULL_TIME', NULL, TRUE, NULL, NOW(), NOW()),
  ('Martín', 'Ruiz', '32555666', '20325556663', '1988-11-05', '2023-02-01', 'Encargado', 'Gerencia', 1, 320000.00, 'FULL_TIME', NULL, TRUE, NULL, NOW(), NOW()),
  ('Soledad', 'Fernández', '34777888', '20347778884', '1992-04-18', '2023-06-01', 'Cajera', 'Ventas', 1, 185000.00, 'PART_TIME', NULL, TRUE, NULL, NOW(), NOW()),
  ('Roberto', 'Gómez', '36999000', '20369990005', '1987-09-30', '2024-01-10', 'Repositor', 'Almacén', 1, 170000.00, 'FULL_TIME', NULL, TRUE, NULL, NOW(), NOW());

-- ---------------------------------------------------------
-- 5. VENTAS CON ITEMS Y PAGOS (últimos 60 días)
-- Genera ventas con items y pagos usando PL/pgSQL
-- ---------------------------------------------------------
DO $$
DECLARE
    v_user_id BIGINT;
    v_warehouse_id BIGINT;
    v_branch_id BIGINT;
    v_client_id BIGINT;
    v_product_id BIGINT;
    v_product_name VARCHAR(150);
    v_product_price NUMERIC(10,2);
    v_sale_id BIGINT;
    v_item_qty INTEGER;
    v_item_unit_price NUMERIC(10,2);
    v_item_subtotal NUMERIC(10,2);
    v_sale_subtotal NUMERIC(10,2);
    v_sale_tax NUMERIC(10,2);
    v_sale_total NUMERIC(10,2);
    v_payment_method VARCHAR(20);
    v_payment_amount NUMERIC(10,2);
    v_sale_date TIMESTAMP;
    i INTEGER;
    j INTEGER;
    k INTEGER;
    item_count INTEGER;
BEGIN
    SELECT id INTO v_user_id FROM users WHERE email = 'admin@pos.com' LIMIT 1;
    SELECT id INTO v_warehouse_id FROM warehouses WHERE code = 'MAIN' LIMIT 1;
    SELECT id INTO v_branch_id FROM branches WHERE nombre = 'Sucursal Principal' LIMIT 1;

    IF v_user_id IS NULL THEN
        v_user_id := 1;
    END IF;

    FOR i IN 0..60 LOOP
        FOR k IN 1..(FLOOR(RANDOM() * 9)::INTEGER) LOOP
            v_sale_date := (CURRENT_DATE - i) +
                           (FLOOR(RANDOM() * 12 + 8)::INTEGER * INTERVAL '1 hour') +
                           (FLOOR(RANDOM() * 60)::INTEGER * INTERVAL '1 minute');

            IF RANDOM() < 0.7 THEN
                SELECT id INTO v_client_id FROM clients WHERE name = 'Consumidor Final' LIMIT 1;
            ELSE
                SELECT id INTO v_client_id FROM clients WHERE name <> 'Consumidor Final' ORDER BY RANDOM() LIMIT 1;
            END IF;

            v_sale_subtotal := 0;
            v_sale_tax := 0;
            item_count := FLOOR(RANDOM() * 5 + 1)::INTEGER;

            INSERT INTO sales (client_id, user_id, warehouse_id, branch_id, status, subtotal, tax_amount, discount, total, notes, created_at, updated_at)
            VALUES (v_client_id, v_user_id, v_warehouse_id, v_branch_id, 'COMPLETED', 0, 0, 0, 0, 'Venta de muestra', v_sale_date, v_sale_date)
            RETURNING id INTO v_sale_id;

            FOR j IN 1..item_count LOOP
                SELECT id, name, price INTO v_product_id, v_product_name, v_product_price
                FROM products
                WHERE active = TRUE
                ORDER BY RANDOM()
                LIMIT 1;

                v_item_qty := FLOOR(RANDOM() * 3 + 1)::INTEGER;
                v_item_unit_price := v_product_price;
                v_item_subtotal := v_item_qty * v_item_unit_price;

                v_sale_subtotal := v_sale_subtotal + v_item_subtotal;
                v_sale_tax := v_sale_tax + (v_item_subtotal * 0.21);

                INSERT INTO sale_items (sale_id, product_id, product_name, quantity, unit_price, precio_resuelto, es_precio_local, discount, tax_amount, subtotal)
                VALUES (v_sale_id, v_product_id, v_product_name, v_item_qty, v_item_unit_price, v_item_unit_price, FALSE, 0, ROUND(v_item_subtotal * 0.21 / v_item_qty::numeric, 2), v_item_subtotal);
            END LOOP;

            v_sale_total := v_sale_subtotal + v_sale_tax;

            UPDATE sales
            SET subtotal = v_sale_subtotal,
                tax_amount = v_sale_tax,
                total = v_sale_total,
                updated_at = v_sale_date
            WHERE id = v_sale_id;

            IF v_sale_total > 0 THEN
                v_payment_amount := v_sale_total;
                IF RANDOM() < 0.6 THEN
                    v_payment_method := 'CASH';
                ELSE
                    v_payment_method := 'CREDIT_CARD';
                END IF;

                INSERT INTO payments (sale_id, payment_method, amount, reference, created_at)
                VALUES (v_sale_id, v_payment_method, v_payment_amount, NULL, v_sale_date);
            END IF;
        END LOOP;
    END LOOP;
END $$;

-- ---------------------------------------------------------
-- 6. TURNOS DE CAJA (cash_shift)
-- ---------------------------------------------------------
INSERT INTO cash_shifts (cajero_id, sucursal_id, estado, monto_apertura, monto_cierre, diferencia, fecha_apertura, fecha_cierre, version)
SELECT
    u.id,
    1,
    'CERRADO',
    50000.00,
    50000.00 + (FLOOR(RANDOM() * 20000 - 10000))::INTEGER,
    (FLOOR(RANDOM() * 20000 - 10000))::INTEGER,
    (CURRENT_DATE - (FLOOR(RANDOM() * 30)::INTEGER))::DATE + '09:00'::TIME,
    (CURRENT_DATE - (FLOOR(RANDOM() * 30)::INTEGER))::DATE + '18:00'::TIME,
    1
FROM users u
WHERE u.email = 'admin@pos.com'
LIMIT 1;

SELECT 'Seed data V5 insertado correctamente.' AS mensaje;
