-- El bounded context "users" se dividió en tres: IAM (credenciales + login), Profile
-- (nombre visible, token de notificación) y Subscription (planes). Cada uno es dueño de
-- su propia tabla; no hay FKs cruzando bounded contexts (solo comparten el UUID por
-- convención), salvo transactions -> user_profiles, que ya existía y se preserva vía el
-- RENAME (Postgres reapunta automáticamente la constraint existente a la tabla renombrada).

ALTER TABLE users RENAME TO user_profiles;
-- El email pasa a ser propiedad exclusiva de IAM (es el identificador de login);
-- Postgres dropea junto con la columna la constraint UNIQUE que dependía de ella.
ALTER TABLE user_profiles DROP COLUMN email;

CREATE TABLE credentials
(
    id              UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    email           VARCHAR(255) NOT NULL UNIQUE,
    hashed_password VARCHAR(100) NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE subscriptions
(
    id          UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL,
    plan_code   VARCHAR(20)  NOT NULL CHECK (plan_code IN ('FREE', 'PREMIUM')),
    status      VARCHAR(20)  NOT NULL CHECK (status IN ('ACTIVE', 'CANCELED', 'EXPIRED')),
    started_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    renews_at   TIMESTAMPTZ,
    canceled_at TIMESTAMPTZ
);

-- Refuerza a nivel de BD la misma regla que ya valida el caso de uso: un usuario no
-- puede tener dos suscripciones ACTIVE al mismo tiempo (defensa en profundidad).
CREATE UNIQUE INDEX idx_subscriptions_one_active_per_user ON subscriptions (user_id) WHERE status = 'ACTIVE';
CREATE INDEX idx_subscriptions_user ON subscriptions (user_id);
