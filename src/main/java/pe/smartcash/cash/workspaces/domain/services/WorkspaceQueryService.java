package pe.smartcash.cash.workspaces.domain.services;

import java.util.List;
import java.util.Optional;
import pe.smartcash.cash.workspaces.domain.model.queries.FindWorkspaceByIdQuery;
import pe.smartcash.cash.workspaces.domain.model.queries.FindWorkspacesByOwnerQuery;

public interface WorkspaceQueryService {

  List<WorkspaceDetail> handle(FindWorkspacesByOwnerQuery query);

  Optional<WorkspaceDetail> handle(FindWorkspaceByIdQuery query);
}
