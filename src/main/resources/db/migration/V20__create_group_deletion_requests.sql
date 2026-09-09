-- Borrar un grupo de gasto compartido cuando TODOS sus miembros aceptados están de acuerdo.
-- Un miembro abre la solicitud (y cuenta como primera aprobación), el resto aprueba; cuando
-- están todos, el grupo y todos sus datos se borran en cascada (bloqueado si hay saldos != 0).
-- Cualquier miembro puede cancelar mientras siga PENDING.
CREATE TABLE group_deletion_requests
(
    id           UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    group_id     UUID        NOT NULL REFERENCES groups (id),
    requested_by UUID        NOT NULL,
    status       VARCHAR(10) NOT NULL CHECK (status IN ('PENDING', 'COMPLETED', 'CANCELLED')),
    requested_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    resolved_at  TIMESTAMPTZ
);

-- Una sola solicitud PENDING por grupo -- una CANCELLED cae fuera del índice y deja abrir otra
-- (mismo truco de índice parcial que idx_group_memberships_active en V13).
CREATE UNIQUE INDEX idx_group_deletion_requests_pending ON group_deletion_requests (group_id)
    WHERE status = 'PENDING';
CREATE INDEX idx_group_deletion_requests_group ON group_deletion_requests (group_id);

CREATE TABLE group_deletion_approvals
(
    id          UUID PRIMARY KEY    DEFAULT gen_random_uuid(),
    request_id  UUID        NOT NULL REFERENCES group_deletion_requests (id),
    user_id     UUID        NOT NULL,
    approved_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (request_id, user_id)
);
CREATE INDEX idx_group_deletion_approvals_request ON group_deletion_approvals (request_id);
