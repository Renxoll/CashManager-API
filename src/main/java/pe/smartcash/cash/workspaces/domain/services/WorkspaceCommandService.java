package pe.smartcash.cash.workspaces.domain.services;

import pe.smartcash.cash.workspaces.domain.model.commands.AddWorkspaceCategoryCommand;
import pe.smartcash.cash.workspaces.domain.model.commands.ArchiveWorkspaceCategoryCommand;
import pe.smartcash.cash.workspaces.domain.model.commands.ArchiveWorkspaceCommand;
import pe.smartcash.cash.workspaces.domain.model.commands.CreateWorkspaceCommand;
import pe.smartcash.cash.workspaces.domain.model.commands.UpdateWorkspaceCategoryCommand;
import pe.smartcash.cash.workspaces.domain.model.commands.UpdateWorkspaceCommand;
import pe.smartcash.cash.workspaces.domain.model.valueobjects.WorkspaceCategoryId;
import pe.smartcash.cash.workspaces.domain.model.valueobjects.WorkspaceId;

/** Contrato de escritura del bounded context: vive en domain, la implementación en application. */
public interface WorkspaceCommandService {

  /**
   * Crea el módulo "General" del usuario si todavía no existe. Idempotente -- lo llama el
   * handler del evento de alta de cuenta, que puede reintentarse.
   */
  void provisionDefaultFor(String userId);

  WorkspaceId handle(CreateWorkspaceCommand command);

  void handle(UpdateWorkspaceCommand command);

  void handle(ArchiveWorkspaceCommand command);

  WorkspaceCategoryId handle(AddWorkspaceCategoryCommand command);

  void handle(UpdateWorkspaceCategoryCommand command);

  void handle(ArchiveWorkspaceCategoryCommand command);
}
