CREATE TABLE shift_definitions (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre      VARCHAR(100) NOT NULL,
    hora_inicio TIME         NOT NULL,
    hora_fin    TIME         NOT NULL,
    dias_semana INT          NOT NULL,
    color       VARCHAR(7),
    activo      BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE shift_assignments (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id         BIGINT NOT NULL,
    shift_definition_id BIGINT NOT NULL,
    semana              DATE   NOT NULL,
    dias_activos        INT    NOT NULL,
    sucursal_id         BIGINT,
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE shift_change_requests (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id     BIGINT       NOT NULL,
    assignment_id   BIGINT       NOT NULL,
    fecha_original  DATE         NOT NULL,
    motivo          VARCHAR(500) NOT NULL,
    estado          VARCHAR(20)  NOT NULL DEFAULT 'PENDIENTE',
    revisado_por    BIGINT,
    fecha_revision  DATETIME,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE notifications (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id   BIGINT        NOT NULL,
    titulo    VARCHAR(200)  NOT NULL,
    mensaje   VARCHAR(1000) NOT NULL,
    leido     BOOLEAN       NOT NULL DEFAULT FALSE,
    creado_en DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
);
