package pe.smartcash.cash.workspaces.application.internal.queryservices;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import pe.smartcash.cash.workspaces.domain.model.aggregates.Workspace;
import pe.smartcash.cash.workspaces.domain.model.aggregates.WorkspaceCategory;
import pe.smartcash.cash.workspaces.domain.model.aggregates.WorkspaceRepository;
import pe.smartcash.cash.workspaces.domain.model.queries.FindWorkspaceByIdQuery;
import pe.smartcash.cash.workspaces.domain.model.queries.FindWorkspacesByOwnerQuery;
import pe.smartcash.cash.workspaces.domain.model.valueobjects.UserId;
import pe.smartcash.cash.workspaces.domain.model.valueobjects.WorkspaceCategoryId;
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

  @Override
  public Optional<WorkspaceDetail> findDefault(UserId ownerId) {
    return workspaceRepository.findDefaultByOwner(ownerId).map(WorkspaceQueryServiceImpl::toDetail);
  }

  @Override
  public List<WorkspaceCategoryDetail> describeCategories(Collection<UUID> categoryIds) {
    return workspaceRepository
        .findCategoriesByIds(categoryIds.stream().map(WorkspaceCategoryId::of).toList())
        .stream()
        .map(WorkspaceQueryServiceImpl::toCategoryDetail)
        .toList();
  }

  private static WorkspaceCategoryDetail toCategoryDetail(WorkspaceCategory c) {
    return new WorkspaceCategoryDetail(
        c.id().value(), c.code(), c.displayName(), c.icon(), c.position(), c.archived());
  }

  private static WorkspaceDetail toDetail(Workspace workspace) {
    List<WorkspaceCategoryDetail> categories =
        workspace.categories().stream().map(WorkspaceQueryServiceImpl::toCategoryDetail).toList();
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
