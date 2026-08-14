-- Sprint 15: Multi-sucursal infrastructure
-- Creates branches, user_branches, adds branch_id to existing tables

-- 1. Branches table
CREATE TABLE branches (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    direccion VARCHAR(250),
    telefono VARCHAR(30),
    email VARCHAR(120),
    responsable_id BIGINT,
    activa BOOLEAN NOT NULL DEFAULT TRUE,
    timezone VARCHAR(50),
    punto_venta_fiscal VARCHAR(5),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 2. Insert default branch for existing data
INSERT INTO branches (id, nombre, direccion, activa, timezone, punto_venta_fiscal)
VALUES (1, 'Sucursal Principal', 'Direccion Principal', TRUE, 'America/Argentina/Buenos_Aires', '00001');

-- 3. User-Branch mapping
CREATE TABLE user_branches (
    user_id BIGINT NOT NULL,
    branch_id BIGINT NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (user_id, branch_id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (branch_id) REFERENCES branches(id)
);

-- 4. Assign all existing users to default branch
INSERT INTO user_branches (user_id, branch_id, activo)
SELECT id, 1, TRUE FROM users;

-- 5. Add branch_id to warehouses
ALTER TABLE warehouses ADD COLUMN branch_id BIGINT;
UPDATE warehouses SET branch_id = 1;
ALTER TABLE warehouses MODIFY branch_id BIGINT NOT NULL;

-- 6. Add branch_id to sales
ALTER TABLE sales ADD COLUMN branch_id BIGINT;
UPDATE sales SET branch_id = 1;
ALTER TABLE sales MODIFY branch_id BIGINT NOT NULL;

-- 7. Add branch_id to expenses
ALTER TABLE expenses ADD COLUMN branch_id BIGINT;
UPDATE expenses SET branch_id = 1;
ALTER TABLE expenses MODIFY branch_id BIGINT NOT NULL;

-- 8. Add branch_id to receivables
ALTER TABLE receivables ADD COLUMN branch_id BIGINT;
UPDATE receivables SET branch_id = 1;
ALTER TABLE receivables MODIFY branch_id BIGINT NOT NULL;

-- 9. Add branch_id to payables
ALTER TABLE payables ADD COLUMN branch_id BIGINT;
UPDATE payables SET branch_id = 1;
ALTER TABLE payables MODIFY branch_id BIGINT NOT NULL;

-- 10. Indexes
CREATE INDEX idx_branches_activa ON branches(activa);
CREATE INDEX idx_user_branches_user ON user_branches(user_id);
CREATE INDEX idx_user_branches_branch ON user_branches(branch_id);
CREATE INDEX idx_warehouses_branch ON warehouses(branch_id);
CREATE INDEX idx_sales_branch ON sales(branch_id);
CREATE INDEX idx_expenses_branch ON expenses(branch_id);
CREATE INDEX idx_receivables_branch ON receivables(branch_id);
CREATE INDEX idx_payables_branch ON payables(branch_id);
