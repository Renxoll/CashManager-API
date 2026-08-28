-- Antes solo se podía conectar UNA cuenta de Gmail por usuario (user_id era UNIQUE). Se
-- permite conectar varias: la unicidad pasa a ser por (user_id, email) -- reconectar la
-- MISMA cuenta actualiza la fila existente, conectar una cuenta distinta crea una nueva.
ALTER TABLE gmail_connections DROP CONSTRAINT gmail_connections_user_id_key;

-- Nullable, sin backfill: las filas ya conectadas antes de este cambio no tienen forma de
-- saber su email sin volver a llamar a Google, así que se quedan sin verificar hasta que el
-- usuario reconecte una vez -- con la base de usuarios actual (un puñado), no vale la pena
-- una migración de backfill contra la API de Google.
ALTER TABLE gmail_connections ADD COLUMN email VARCHAR(255);

CREATE UNIQUE INDEX idx_gmail_connections_user_email ON gmail_connections (user_id, email);
