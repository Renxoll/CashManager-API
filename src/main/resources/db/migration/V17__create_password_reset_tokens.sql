-- "Olvidé mi contraseña": IAM emite un token de un solo uso, lo manda por correo dentro de
-- un enlace al frontend, y al confirmarlo cambia el hash de la contraseña. Acá solo se
-- guarda el SHA-256 del token (hex), nunca el token crudo -- mismo principio que
-- credentials.hashed_password: una filtración de esta tabla no debe entregar enlaces de
-- restablecimiento usables.
CREATE TABLE password_reset_tokens
(
    id          UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL,
    token_hash  VARCHAR(64) NOT NULL UNIQUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at  TIMESTAMPTZ NOT NULL,
    redeemed_at TIMESTAMPTZ
);

-- Sin FK hacia credentials: misma autonomía de contexto que el resto del proyecto (V3 ya
-- deja credentials / user_profiles / subscriptions sin FKs entre sí). El lookup del confirm
-- va por el índice único de token_hash; este índice cubre el borrado de tokens previos
-- cuando el usuario pide un enlace nuevo (solo el último queda vivo).
CREATE INDEX idx_password_reset_tokens_user ON password_reset_tokens (user_id);
