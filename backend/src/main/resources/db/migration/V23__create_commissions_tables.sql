CREATE TABLE commission_schemes (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre         VARCHAR(200) NOT NULL,
    tipo           VARCHAR(30)  NOT NULL,
    activo         BOOLEAN      NOT NULL DEFAULT TRUE,
    vigencia_desde DATE         NOT NULL,
    vigencia_hasta DATE,
    valor          DECIMAL(5,2)
);

CREATE TABLE commission_tiers (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    scheme_id   BIGINT        NOT NULL,
    monto_desde DECIMAL(12,2) NOT NULL,
    monto_hasta DECIMAL(12,2),
    porcentaje  DECIMAL(5,2)  NOT NULL,
    FOREIGN KEY (scheme_id) REFERENCES commission_schemes(id) ON DELETE CASCADE
);

CREATE TABLE employee_commission_assignments (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id    BIGINT NOT NULL,
    scheme_id      BIGINT NOT NULL,
    vigencia_desde DATE   NOT NULL,
    vigencia_hasta DATE,
    FOREIGN KEY (scheme_id) REFERENCES commission_schemes(id)
);

CREATE TABLE sales_targets (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id         BIGINT        NOT NULL,
    mes                 INT           NOT NULL,
    anio                INT           NOT NULL,
    meta_monto          DECIMAL(12,2) NOT NULL,
    bono_por_superacion VARCHAR(20)   NOT NULL,
    valor_bono          DECIMAL(12,2) NOT NULL
);

CREATE TABLE commission_results (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id       BIGINT        NOT NULL,
    mes               INT           NOT NULL,
    anio              INT           NOT NULL,
    total_ventas      DECIMAL(14,2) NOT NULL,
    comision_calculada DECIMAL(12,2) NOT NULL,
    meta_alcanzada    BOOLEAN,
    bono_aplicado     DECIMAL(12,2),
    esquema_usado     VARCHAR(200)
);
