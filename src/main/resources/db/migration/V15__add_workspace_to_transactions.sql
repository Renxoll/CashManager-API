-- Cada transacción vive ahora en un "módulo" (tabla workspaces, V14). La ingesta automática
-- por correo siempre cae en el módulo "General" del usuario; desde la app el usuario puede
-- mover una transacción a otro módulo y, si es un gasto, reasignarle una categoría propia de
-- ese módulo (workspace_category_id). Para el módulo General se sigue usando category_id ->
-- categories (el catálogo cerrado que conoce el LLM), sin tocar el pipeline de extracción.
--
-- Sin FK hacia workspaces / workspace_categories: mismo criterio que el resto del proyecto
-- (transactions.user_id tampoco tiene FK hacia otros contextos salvo user_profiles, que es
-- histórica). La integridad la garantiza la capa de aplicación (ACL WorkspaceDirectory).
ALTER TABLE transactions
    ADD COLUMN workspace_id UUID;

ALTER TABLE transactions
    ADD COLUMN workspace_category_id UUID;

-- Backfill: toda transacción existente pasa al módulo "General" de su dueño (creado por
-- V14 para todos los usuarios que ya existían). workspace_category_id queda NULL -- estas
-- filas siguen clasificadas por category_id, que es lo correcto para el módulo General.
UPDATE transactions t
SET workspace_id = w.id
FROM workspaces w
WHERE w.owner_id = t.user_id
  AND w.is_default;

ALTER TABLE transactions
    ALTER COLUMN workspace_id SET NOT NULL;

CREATE INDEX idx_transactions_workspace_created ON transactions (workspace_id, created_at DESC);
