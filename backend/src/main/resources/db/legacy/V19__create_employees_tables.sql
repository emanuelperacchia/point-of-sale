CREATE TABLE employees (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre          VARCHAR(100)   NOT NULL,
    apellido        VARCHAR(100)   NOT NULL,
    dni             VARCHAR(20)    NOT NULL UNIQUE,
    cuil            VARCHAR(20),
    fecha_nacimiento DATE          NOT NULL,
    fecha_ingreso   DATE           NOT NULL,
    fecha_baja      DATE,
    cargo           VARCHAR(100)   NOT NULL,
    departamento    VARCHAR(100)   NOT NULL,
    sucursal_id     BIGINT,
    salario_base    DECIMAL(12,2)  NOT NULL,
    modalidad_contrato VARCHAR(20) NOT NULL,
    user_id         BIGINT,
    activo          BOOLEAN        NOT NULL DEFAULT TRUE,
    documento_url   VARCHAR(500),
    created_at      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE employee_history (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id     BIGINT         NOT NULL,
    campo           VARCHAR(20)    NOT NULL,
    valor_anterior  VARCHAR(255)   NOT NULL,
    valor_nuevo     VARCHAR(255)   NOT NULL,
    fecha           DATETIME       NOT NULL,
    modificado_por  BIGINT,
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE
);
