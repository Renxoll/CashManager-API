-- Confianza por usuario sobre dominios de remitente, encima del allowlist global curado en
-- app.inbound-email.trusted-sender-domains (ver AllowlistedBankSenderPolicy). Sin esto, un
-- correo de un dominio nuevo (otra billetera, Google Play, Stripe, lo que sea) se descartaba
-- en silencio -- ahora queda pendiente para que el usuario lo apruebe o rechace él mismo.
CREATE TABLE pending_senders
(
    id               UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    user_id          UUID         NOT NULL,
    from_address     VARCHAR(255) NOT NULL,
    domain           VARCHAR(255) NOT NULL,
    -- Solo para mostrarle contexto al usuario al decidir -- nunca se reingesta como
    -- transacción (aprobar solo habilita correos FUTUROS de ese dominio), así que no hace
    -- falta guardar el texto completo del correo.
    sample_snippet   VARCHAR(500),
    status           VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    occurrence_count INT          NOT NULL DEFAULT 1,
    first_seen_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    last_seen_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    decided_at       TIMESTAMPTZ
);

-- Un remitente pendiente por (usuario, dominio): el mismo dominio no se duplica, solo suma
-- occurrence_count (ver PendingSender.recordAnotherSighting).
CREATE UNIQUE INDEX idx_pending_senders_user_domain ON pending_senders (user_id, domain);
CREATE INDEX idx_pending_senders_status_pending ON pending_senders (status) WHERE status = 'PENDING';

CREATE TABLE user_trusted_senders
(
    id         UUID PRIMARY KEY    DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL,
    domain     VARCHAR(255) NOT NULL,
    trusted_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_user_trusted_senders_user_domain ON user_trusted_senders (user_id, domain);
