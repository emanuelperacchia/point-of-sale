CREATE TABLE attendance_records (
    id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id            BIGINT       NOT NULL,
    fecha                  DATE         NOT NULL,
    hora_entrada           TIME         NOT NULL,
    hora_salida            TIME,
    horas_trabajadas_minutos INT,
    horas_extra_minutos    INT,
    estado                 VARCHAR(20)  NOT NULL DEFAULT 'COMPLETO',
    observacion            VARCHAR(500),
    created_at             DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE absences (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id     BIGINT       NOT NULL,
    fecha           DATE         NOT NULL,
    tipo            VARCHAR(20)  NOT NULL,
    descripcion     VARCHAR(500),
    aprobado_por    BIGINT,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);
