package pe.smartcash.cash.workspaces.domain.services;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import pe.smartcash.cash.workspaces.domain.model.queries.FindWorkspaceByIdQuery;
import pe.smartcash.cash.workspaces.domain.model.queries.FindWorkspacesByOwnerQuery;
import pe.smartcash.cash.workspaces.domain.model.valueobjects.UserId;

public interface WorkspaceQueryService {

  List<WorkspaceDetail> handle(FindWorkspacesByOwnerQuery query);

  Optional<WorkspaceDetail> handle(FindWorkspaceByIdQuery query);

  /** Módulo "General" del usuario, con sus categorías. */
  Optional<WorkspaceDetail> findDefault(UserId ownerId);

  /** Resuelve categorías de módulo por id (para ACLs de otros contextos). */
  List<WorkspaceCategoryDetail> describeCategories(Collection<UUID> categoryIds);
}
