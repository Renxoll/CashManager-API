-- Bounded context nuevo "groups": gastos compartidos estilo Splitwise (crear grupos con
-- otros usuarios reales de SmartCash, dividir gastos en partes iguales, saldar cuentas y
-- simplificar deudas). Sin FK hacia tablas de otros contextos (mismo criterio que
-- gmail_connections.user_id): cada bounded context es dueño de su propio esquema.
CREATE TABLE groups
(
    id         UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    name       VARCHAR(120) NOT NULL,
    owner_id   UUID         NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE group_memberships
(
    id            UUID PRIMARY KEY    DEFAULT gen_random_uuid(),
    group_id      UUID        NOT NULL REFERENCES groups (id),
    user_id       UUID        NOT NULL,
    status        VARCHAR(10) NOT NULL CHECK (status IN ('PENDING', 'ACCEPTED', 'DECLINED')),
    invited_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    responded_at  TIMESTAMPTZ
);

-- Una sola membresía activa (pendiente o aceptada) por par grupo/usuario -- una invitación
-- rechazada sí puede volver a mandarse más adelante (cae fuera de este índice parcial en
-- cuanto queda DECLINED, mismo truco que idx_pending_senders_user_domain).
CREATE UNIQUE INDEX idx_group_memberships_active ON group_memberships (group_id, user_id)
    WHERE status IN ('PENDING', 'ACCEPTED');
CREATE INDEX idx_group_memberships_group ON group_memberships (group_id);
CREATE INDEX idx_group_memberships_user ON group_memberships (user_id, status);

CREATE TABLE shared_expenses
(
    id              UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    group_id        UUID         NOT NULL REFERENCES groups (id),
    description     VARCHAR(200) NOT NULL,
    amount          NUMERIC(12, 2) NOT NULL,
    currency        VARCHAR(3)   NOT NULL,
    paid_by_user_id UUID         NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_shared_expenses_group ON shared_expenses (group_id, created_at DESC);

-- Montos ya calculados al momento de crear el gasto (split equitativo, con los centavos
-- sobrantes repartidos entre los primeros participantes -- ver Transaction.splitEqually):
-- inmutables después, no se recalculan si cambia la membresía del grupo más adelante.
CREATE TABLE expense_shares
(
    id          UUID PRIMARY KEY    DEFAULT gen_random_uuid(),
    expense_id  UUID        NOT NULL REFERENCES shared_expenses (id),
    user_id     UUID        NOT NULL,
    amount      NUMERIC(12, 2) NOT NULL,
    UNIQUE (expense_id, user_id)
);

CREATE TABLE settlements
(
    id            UUID PRIMARY KEY    DEFAULT gen_random_uuid(),
    group_id      UUID        NOT NULL REFERENCES groups (id),
    from_user_id  UUID        NOT NULL,
    to_user_id    UUID        NOT NULL,
    amount        NUMERIC(12, 2) NOT NULL,
    currency      VARCHAR(3)  NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_settlements_group ON settlements (group_id, created_at DESC);
