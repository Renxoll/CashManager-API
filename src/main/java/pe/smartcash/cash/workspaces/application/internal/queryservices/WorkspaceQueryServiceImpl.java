package pe.smartcash.cash.workspaces.application.internal.queryservices;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import pe.smartcash.cash.workspaces.domain.model.aggregates.Workspace;
import pe.smartcash.cash.workspaces.domain.model.aggregates.WorkspaceRepository;
import pe.smartcash.cash.workspaces.domain.model.queries.FindWorkspaceByIdQuery;
import pe.smartcash.cash.workspaces.domain.model.queries.FindWorkspacesByOwnerQuery;
import pe.smartcash.cash.workspaces.domain.services.WorkspaceCategoryDetail;
import pe.smartcash.cash.workspaces.domain.services.WorkspaceDetail;
import pe.smartcash.cash.workspaces.domain.services.WorkspaceQueryService;

@Service
class WorkspaceQueryServiceImpl implements WorkspaceQueryService {

  private final WorkspaceRepository workspaceRepository;

  WorkspaceQueryServiceImpl(WorkspaceRepository workspaceRepository) {
    this.workspaceRepository = workspaceRepository;
  }

  @Override
  public List<WorkspaceDetail> handle(FindWorkspacesByOwnerQuery query) {
    return workspaceRepository.findAllActiveByOwner(query.ownerId()).stream().map(WorkspaceQueryServiceImpl::toDetail).toList();
  }

  @Override
  public Optional<WorkspaceDetail> handle(FindWorkspaceByIdQuery query) {
    return workspaceRepository
        .findById(query.workspaceId())
        .filter(w -> w.ownerId().equals(query.ownerId()))
        .map(WorkspaceQueryServiceImpl::toDetail);
  }

  private static WorkspaceDetail toDetail(Workspace workspace) {
    List<WorkspaceCategoryDetail> categories =
        workspace.categories().stream()
            .map(
                c ->
                    new WorkspaceCategoryDetail(
                        c.id().value(), c.code(), c.displayName(), c.icon(), c.position(), c.archived()))
            .toList();
    return new WorkspaceDetail(
        workspace.id().value(),
        workspace.name(),
        workspace.colorHex(),
        workspace.icon(),
        workspace.isDefault(),
        workspace.createdAt(),
        categories);
  }
}
