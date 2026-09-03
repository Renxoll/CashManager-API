package pe.smartcash.cash.workspaces.domain.model.queries;

import pe.smartcash.cash.workspaces.domain.model.valueobjects.UserId;

public record FindWorkspacesByOwnerQuery(UserId ownerId) {}
