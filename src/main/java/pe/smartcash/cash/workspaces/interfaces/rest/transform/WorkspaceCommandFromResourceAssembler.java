package pe.smartcash.cash.workspaces.interfaces.rest.transform;

import pe.smartcash.cash.workspaces.domain.model.commands.AddWorkspaceCategoryCommand;
import pe.smartcash.cash.workspaces.domain.model.commands.ArchiveWorkspaceCategoryCommand;
import pe.smartcash.cash.workspaces.domain.model.commands.ArchiveWorkspaceCommand;
import pe.smartcash.cash.workspaces.domain.model.commands.CreateWorkspaceCommand;
import pe.smartcash.cash.workspaces.domain.model.commands.UpdateWorkspaceCategoryCommand;
import pe.smartcash.cash.workspaces.domain.model.commands.UpdateWorkspaceCommand;
import pe.smartcash.cash.workspaces.interfaces.rest.resources.CreateWorkspaceResource;
import pe.smartcash.cash.workspaces.interfaces.rest.resources.UpdateWorkspaceResource;
import pe.smartcash.cash.workspaces.interfaces.rest.resources.WorkspaceCategoryPayload;

public final class WorkspaceCommandFromResourceAssembler {

  private WorkspaceCommandFromResourceAssembler() {}

  public static CreateWorkspaceCommand toCreateCommand(String ownerId, CreateWorkspaceResource resource) {
    return new CreateWorkspaceCommand(ownerId, resource.name(), resource.colorHex(), resource.icon());
  }

  public static UpdateWorkspaceCommand toUpdateCommand(String workspaceId, String ownerId, UpdateWorkspaceResource resource) {
    return new UpdateWorkspaceCommand(workspaceId, ownerId, resource.name(), resource.colorHex(), resource.icon());
  }

  public static ArchiveWorkspaceCommand toArchiveCommand(String workspaceId, String ownerId) {
    return new ArchiveWorkspaceCommand(workspaceId, ownerId);
  }

  public static AddWorkspaceCategoryCommand toAddCategoryCommand(
      String workspaceId, String ownerId, WorkspaceCategoryPayload payload) {
    return new AddWorkspaceCategoryCommand(workspaceId, ownerId, payload.displayName(), payload.icon());
  }

  public static UpdateWorkspaceCategoryCommand toUpdateCategoryCommand(
      String workspaceId, String categoryId, String ownerId, WorkspaceCategoryPayload payload) {
    return new UpdateWorkspaceCategoryCommand(workspaceId, categoryId, ownerId, payload.displayName(), payload.icon());
  }

  public static ArchiveWorkspaceCategoryCommand toArchiveCategoryCommand(
      String workspaceId, String categoryId, String ownerId) {
    return new ArchiveWorkspaceCategoryCommand(workspaceId, categoryId, ownerId);
  }
}
