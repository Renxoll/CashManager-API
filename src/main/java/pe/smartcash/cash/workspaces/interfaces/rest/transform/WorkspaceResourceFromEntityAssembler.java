package pe.smartcash.cash.workspaces.interfaces.rest.transform;

import pe.smartcash.cash.workspaces.domain.services.WorkspaceDetail;
import pe.smartcash.cash.workspaces.interfaces.rest.resources.WorkspaceCategoryResource;
import pe.smartcash.cash.workspaces.interfaces.rest.resources.WorkspaceResource;

public final class WorkspaceResourceFromEntityAssembler {

  private WorkspaceResourceFromEntityAssembler() {}

  public static WorkspaceResource toResourceFromEntity(WorkspaceDetail detail) {
    return new WorkspaceResource(
        detail.id(),
        detail.name(),
        detail.colorHex(),
        detail.icon(),
        detail.isDefault(),
        detail.createdAt(),
        detail.categories().stream()
            .map(
                c ->
                    new WorkspaceCategoryResource(
                        c.id(), c.code(), c.displayName(), c.icon(), c.position(), c.archived()))
            .toList());
  }
}
