package pe.smartcash.cash.workspaces.domain.exception;

import pe.smartcash.cash.workspaces.domain.model.valueobjects.WorkspaceId;

/** El módulo no existe, o existe pero es de otro usuario (mismo mensaje a propósito: no
 * confirma la existencia de recursos ajenos). */
public class WorkspaceNotFoundException extends RuntimeException {

  public WorkspaceNotFoundException(WorkspaceId workspaceId) {
    super("Módulo no encontrado: " + workspaceId.value());
  }
}
