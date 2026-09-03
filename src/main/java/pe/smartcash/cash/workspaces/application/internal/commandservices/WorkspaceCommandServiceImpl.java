package pe.smartcash.cash.workspaces.application.internal.commandservices;

import java.time.Clock;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.smartcash.cash.workspaces.domain.exception.WorkspaceNotFoundException;
import pe.smartcash.cash.workspaces.domain.model.aggregates.Workspace;
import pe.smartcash.cash.workspaces.domain.model.aggregates.WorkspaceCategory;
import pe.smartcash.cash.workspaces.domain.model.aggregates.WorkspaceRepository;
import pe.smartcash.cash.workspaces.domain.model.commands.AddWorkspaceCategoryCommand;
import pe.smartcash.cash.workspaces.domain.model.commands.ArchiveWorkspaceCategoryCommand;
import pe.smartcash.cash.workspaces.domain.model.commands.ArchiveWorkspaceCommand;
import pe.smartcash.cash.workspaces.domain.model.commands.CreateWorkspaceCommand;
import pe.smartcash.cash.workspaces.domain.model.commands.UpdateWorkspaceCategoryCommand;
import pe.smartcash.cash.workspaces.domain.model.commands.UpdateWorkspaceCommand;
import pe.smartcash.cash.workspaces.domain.model.valueobjects.StarterCategories;
import pe.smartcash.cash.workspaces.domain.model.valueobjects.UserId;
import pe.smartcash.cash.workspaces.domain.model.valueobjects.WorkspaceCategoryId;
import pe.smartcash.cash.workspaces.domain.model.valueobjects.WorkspaceId;
import pe.smartcash.cash.workspaces.domain.services.WorkspaceCommandService;

@Service
class WorkspaceCommandServiceImpl implements WorkspaceCommandService {

  private final WorkspaceRepository workspaceRepository;
  private final Clock clock;

  WorkspaceCommandServiceImpl(WorkspaceRepository workspaceRepository, Clock clock) {
    this.workspaceRepository = workspaceRepository;
    this.clock = clock;
  }

  @Override
  @Transactional
  public void provisionDefaultFor(String userId) {
    UserId ownerId = UserId.parse(userId);
    if (workspaceRepository.existsDefaultForOwner(ownerId)) {
      return;
    }
    workspaceRepository.save(
        Workspace.openDefault(WorkspaceId.newId(), ownerId, StarterCategories.DEFAULT, clock.instant()));
  }

  @Override
  @Transactional
  public WorkspaceId handle(CreateWorkspaceCommand command) {
    UserId ownerId = UserId.parse(command.ownerId());
    Workspace workspace =
        Workspace.open(
            WorkspaceId.newId(),
            ownerId,
            command.name(),
            command.colorHex(),
            command.icon(),
            StarterCategories.DEFAULT,
            clock.instant());
    workspaceRepository.save(workspace);
    return workspace.id();
  }

  @Override
  @Transactional
  public void handle(UpdateWorkspaceCommand command) {
    Workspace workspace = requireOwned(command.workspaceId(), command.ownerId());
    if (command.name() != null && !command.name().isBlank()) {
      workspace.rename(command.name());
    }
    workspace.recustomize(command.colorHex(), command.icon());
    workspaceRepository.save(workspace);
  }

  @Override
  @Transactional
  public void handle(ArchiveWorkspaceCommand command) {
    Workspace workspace = requireOwned(command.workspaceId(), command.ownerId());
    workspace.archive(clock.instant());
    workspaceRepository.save(workspace);
  }

  @Override
  @Transactional
  public WorkspaceCategoryId handle(AddWorkspaceCategoryCommand command) {
    Workspace workspace = requireOwned(command.workspaceId(), command.ownerId());
    String code =
        CategoryCodeFactory.from(
            command.displayName(),
            workspace.categories().stream().map(WorkspaceCategory::code).collect(Collectors.toSet()));
    WorkspaceCategory category =
        workspace.addCategory(WorkspaceCategoryId.newId(), code, command.displayName(), command.icon());
    workspaceRepository.save(workspace);
    return category.id();
  }

  @Override
  @Transactional
  public void handle(UpdateWorkspaceCategoryCommand command) {
    Workspace workspace = requireOwned(command.workspaceId(), command.ownerId());
    workspace.renameCategory(
        WorkspaceCategoryId.parse(command.categoryId()), command.displayName(), command.icon());
    workspaceRepository.save(workspace);
  }

  @Override
  @Transactional
  public void handle(ArchiveWorkspaceCategoryCommand command) {
    Workspace workspace = requireOwned(command.workspaceId(), command.ownerId());
    workspace.archiveCategory(WorkspaceCategoryId.parse(command.categoryId()));
    workspaceRepository.save(workspace);
  }

  private Workspace requireOwned(String workspaceId, String ownerId) {
    UserId owner = UserId.parse(ownerId);
    WorkspaceId id = WorkspaceId.of(UUID.fromString(workspaceId));
    return workspaceRepository
        .findById(id)
        .filter(w -> w.ownerId().equals(owner))
        .orElseThrow(() -> new WorkspaceNotFoundException(id));
  }
}
