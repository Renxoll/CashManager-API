package pe.smartcash.cash.workspaces.domain.model.queries;

import pe.smartcash.cash.workspaces.domain.model.valueobjects.UserId;
import pe.smartcash.cash.workspaces.domain.model.valueobjects.WorkspaceId;

public record FindWorkspaceByIdQuery(WorkspaceId workspaceId, UserId ownerId) {}
