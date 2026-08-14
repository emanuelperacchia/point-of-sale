CREATE TABLE evaluation_templates (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre   VARCHAR(200) NOT NULL,
    periodo  VARCHAR(20)  NOT NULL,
    activo   BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE evaluation_criteria (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_id BIGINT        NOT NULL,
    nombre      VARCHAR(200)  NOT NULL,
    peso        DECIMAL(5,2)  NOT NULL,
    FOREIGN KEY (template_id) REFERENCES evaluation_templates(id) ON DELETE CASCADE
);

CREATE TABLE performance_evaluations (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id      BIGINT        NOT NULL,
    template_id      BIGINT        NOT NULL,
    periodo          VARCHAR(100)  NOT NULL,
    fecha_evaluacion DATE          NOT NULL,
    puntuacion_final DECIMAL(5,2),
    observaciones    VARCHAR(2000),
    estado           VARCHAR(20)   NOT NULL DEFAULT 'BORRADOR',
    evaluado_por     BIGINT,
    created_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE evaluation_scores (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    evaluation_id BIGINT NOT NULL,
    criterion_id  BIGINT NOT NULL,
    puntuacion    INT    NOT NULL,
    FOREIGN KEY (evaluation_id) REFERENCES performance_evaluations(id) ON DELETE CASCADE,
    FOREIGN KEY (criterion_id) REFERENCES evaluation_criteria(id)
);
