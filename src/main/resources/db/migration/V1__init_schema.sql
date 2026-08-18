-- Esquema inicial: usuarios, categorías (catálogo fijo del MVP) y transacciones.
-- gen_random_uuid() vive en pgcrypto; se habilita explícitamente en vez de asumir el core.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE users
(
    id           UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    email        VARCHAR(255) NOT NULL UNIQUE,
    display_name VARCHAR(150),
    fcm_token    TEXT,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Catálogo cerrado de categorías: es también el enum que se le exige al LLM en el
-- Structured Output, así que vive en tabla (editable por admin) en vez de en código.
CREATE TABLE categories
(
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code         VARCHAR(30) NOT NULL UNIQUE,
    display_name VARCHAR(60) NOT NULL,
    icon         VARCHAR(30)
);

INSERT INTO categories (code, display_name, icon)
VALUES ('COMIDA', 'Comida', 'utensils'),
       ('TRANSPORTE', 'Transporte', 'car'),
       ('ENTRETENIMIENTO', 'Entretenimiento', 'film'),
       ('SALUD', 'Salud', 'heart-pulse'),
       ('COMPRAS', 'Compras', 'shopping-bag'),
       ('SERVICIOS', 'Servicios', 'receipt'),
       ('EDUCACION', 'Educación', 'book'),
       ('OTROS', 'Otros', 'circle-help');

CREATE TABLE transactions
(
    id                 UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    user_id            UUID         NOT NULL REFERENCES users (id),
    category_id        BIGINT REFERENCES categories (id),
    raw_text           TEXT         NOT NULL,
    amount             NUMERIC(12, 2),
    currency           CHAR(3),
    merchant           VARCHAR(150),
    status             VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'PROCESSED', 'FAILED')),
    -- de dónde salió el (comercio -> categoría): LLM real o el atajo de cache/Redis
    extraction_source VARCHAR(20)  NOT NULL DEFAULT 'LLM'
        CHECK (extraction_source IN ('LLM', 'CACHE')),
    error_message      TEXT,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    processed_at       TIMESTAMPTZ
);

CREATE INDEX idx_transactions_user_created ON transactions (user_id, created_at DESC);
CREATE INDEX idx_transactions_status ON transactions (status) WHERE status = 'FAILED';
