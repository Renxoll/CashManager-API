-- Conexión OAuth de Gmail por usuario: alternativa a reenviar/forwardear correos a mano
-- (ver flujo de SendGrid Inbound Parse) -- acá el backend mismo consulta la bandeja del
-- usuario por los remitentes de banco/billetera ya confiables (TrustedBankSenderPolicy) vía
-- un job programado (GmailPollingJob), reusando el mismo pipeline de extracción por LLM.
--
-- access_token/refresh_token viajan siempre cifrados a nivel de aplicación (AES-GCM, ver
-- infrastructure.crypto.TokenCipher) -- la tabla nunca los ve en texto plano.
CREATE TABLE gmail_connections
(
    id                       UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    user_id                  UUID         NOT NULL UNIQUE,
    access_token             TEXT         NOT NULL,
    refresh_token            TEXT         NOT NULL,
    access_token_expires_at  TIMESTAMPTZ  NOT NULL,
    -- NULL hasta el primer poll exitoso; el job lo usa como cursor "after:" en la búsqueda
    -- de Gmail para no reprocesar correos ya vistos.
    last_synced_at           TIMESTAMPTZ,
    connected_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at               TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_gmail_connections_user ON gmail_connections (user_id);
