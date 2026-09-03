-- Bounded context nuevo "workspaces": el usuario separa sus gastos en varios "módulos"
-- (p. ej. "Empresa", "Hijo", "Inversiones"), cada uno con su PROPIA lista de categorías,
-- aparte del módulo personal por defecto ("General") al que caen los gastos que Luki lee
-- automáticamente de los correos. Sin FK hacia tablas de otros contextos (mismo criterio
-- que groups / gmail_connections): cada bounded context es dueño de su propio esquema y
-- solo comparte el UUID de usuario por convención.
CREATE TABLE workspaces
(
    id          UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    owner_id    UUID        NOT NULL,
    name        VARCHAR(60) NOT NULL,
    color_hex   CHAR(7)     NOT NULL DEFAULT '#8B5CF6',
    icon        VARCHAR(40) NOT NULL DEFAULT 'wallet',
    is_default  BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    archived_at TIMESTAMPTZ
);

-- Exactamente un módulo "General" (is_default) por usuario: se crea en el onboarding y no
-- se puede archivar. Índice parcial -> los módulos custom (is_default = false) no entran
-- en la restricción, un usuario puede tener los que quiera.
CREATE UNIQUE INDEX idx_workspaces_one_default_per_owner ON workspaces (owner_id) WHERE is_default;
CREATE INDEX idx_workspaces_owner_active ON workspaces (owner_id) WHERE archived_at IS NULL;

-- Categorías por módulo: reemplazan al catálogo global fijo (tabla categories) para todo
-- lo que no sea el módulo General. code es único DENTRO del módulo, no global -- dos
-- módulos distintos pueden tener ambos una categoría "MARKETING" sin colisionar.
CREATE TABLE workspace_categories
(
    id           UUID PRIMARY KEY    DEFAULT gen_random_uuid(),
    workspace_id UUID        NOT NULL REFERENCES workspaces (id) ON DELETE CASCADE,
    code         VARCHAR(40) NOT NULL,
    display_name VARCHAR(60) NOT NULL,
    icon         VARCHAR(40),
    position     INT         NOT NULL DEFAULT 0,
    archived     BOOLEAN     NOT NULL DEFAULT FALSE,
    UNIQUE (workspace_id, code)
);
CREATE INDEX idx_workspace_categories_workspace ON workspace_categories (workspace_id) WHERE NOT archived;

-- Backfill: un módulo "General" por cada usuario que ya existe, sembrado con una copia de
-- las 8 categorías del catálogo global (tabla categories, V1) como punto de partida. A
-- partir de acá cada módulo evoluciona su lista por separado -- editar la de un módulo no
-- toca la de ningún otro.
INSERT INTO workspaces (id, owner_id, name, color_hex, icon, is_default, created_at)
SELECT gen_random_uuid(), up.id, 'General', '#8B5CF6', 'wallet', TRUE, now()
FROM user_profiles up;

INSERT INTO workspace_categories (id, workspace_id, code, display_name, icon, position)
SELECT gen_random_uuid(),
       w.id,
       c.code,
       c.display_name,
       c.icon,
       ROW_NUMBER() OVER (PARTITION BY w.id ORDER BY c.id) - 1
FROM workspaces w
         JOIN categories c ON TRUE
WHERE w.is_default;
