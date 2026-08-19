-- Ingesta de transacciones por correo (SendGrid Inbound Parse): cada usuario tiene una
-- dirección propia a la que reenviar sus notificaciones bancarias (ver UserProfile.register).
ALTER TABLE user_profiles
    ADD COLUMN inbox_address VARCHAR(150);

-- Backfill para perfiles creados antes de este cambio (p. ej. el seed de dev V2): mismo
-- esquema alias-{hash}@dominio que genera la app, con md5 como equivalente en SQL del
-- SHA-256 truncado que usa UserProfile -- no necesita ser el mismo algoritmo, solo único.
UPDATE user_profiles
SET inbox_address = 'alias-' || substr(md5(id::text), 1, 10) || '@inbox.smartcash.pe'
WHERE inbox_address IS NULL;

ALTER TABLE user_profiles
    ALTER COLUMN inbox_address SET NOT NULL;

CREATE UNIQUE INDEX idx_user_profiles_inbox_address ON user_profiles (inbox_address);
